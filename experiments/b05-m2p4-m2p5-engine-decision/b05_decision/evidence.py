"""Validate an offline corpus of immutable evidence. No network or document rewriting."""
from __future__ import annotations

import hashlib
import json
import re
from pathlib import Path

DIGEST = re.compile(r"[a-f0-9]{64}\Z")
MODES = frozenset({"no_edit", "typed", "plain_paste", "rich_paste"})
PROVENANCE = frozenset({"attested", "observed", "instrumented"})
ARTIFACTS = ("source_docx", "sdk_docx", "callback_docx", "source_pdf", "sdk_pdf",
             "callback_pdf", "source_pdf_repeat", "operator_log", "word_inventory")


def sha256(path: Path) -> str:
    h = hashlib.sha256()
    with path.open("rb") as stream:
        for chunk in iter(lambda: stream.read(1 << 20), b""):
            h.update(chunk)
    return h.hexdigest()


def _path(root: Path, relative: str) -> Path:
    if not isinstance(relative, str) or not relative.strip():
        raise ValueError("Caminho de evidência ausente")
    path = (root / relative).resolve()
    # No traversal/symlink escape. Retain all evidence under the same campaign root.
    if not path.is_relative_to(root) or not path.is_file():
        raise ValueError("Caminho de evidência ausente ou fora da campanha")
    return path


def _entry(root: Path, metadata: dict | None, *, required: bool) -> Path | None:
    if metadata is None and not required:
        return None
    if not isinstance(metadata, dict) or set(metadata) != {"path", "sha256"}:
        raise ValueError("Cada evidência exige apenas path e sha256")
    expected = metadata["sha256"]
    if not isinstance(expected, str) or not DIGEST.fullmatch(expected):
        raise ValueError("SHA-256 ausente ou inválido no manifesto")
    path = _path(root, metadata["path"])
    if sha256(path) != expected:
        raise ValueError("SHA-256 divergente em evidência de entrada")
    return path


def _check_log(path: Path, mode: str) -> dict:
    log = json.loads(path.read_text(encoding="utf-8"))
    if log.get("mode") != mode or log.get("strength") not in PROVENANCE:
        raise ValueError("Log de proveniência inconsistente")
    if not isinstance(log.get("events"), list) or not log["events"]:
        raise ValueError("Log de operação sem eventos")
    required = {"no_edit": "closed_without_changes", "typed": "typed_text",
                "plain_paste": "paste_plain", "rich_paste": "paste_rich"}[mode]
    if required not in {e.get("event") for e in log["events"] if isinstance(e, dict)}:
        raise ValueError("Operação declarada não consta no log")
    if not log.get("session") or not log.get("editor_build"):
        raise ValueError("Log sem sessão ou build do editor")
    # The word 'instrumented' is a declaration, not an independent certification.
    return {"strength_declared": log["strength"], "event_count": len(log["events"]),
            "has_external_capture": bool(log.get("external_capture_reference"))}


def validate_manifest(manifest_path: Path) -> tuple[dict, list[dict], list[tuple[Path, str]]]:
    manifest_path = manifest_path.resolve()
    root = manifest_path.parent
    manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
    if manifest.get("schema_version") != 1:
        raise ValueError("schema_version deve ser 1")
    if not isinstance(manifest.get("campaign_id"), str) or not manifest["campaign_id"]:
        raise ValueError("campaign_id obrigatório")
    if manifest.get("corpus_kind") not in {"synthetic", "official_controlled"}:
        raise ValueError("corpus_kind deve ser synthetic ou official_controlled")
    env = manifest.get("environment")
    if not isinstance(env, dict) or not env.get("onlyoffice_build") or not env.get("word_build"):
        raise ValueError("Versões do editor e do Word precisam ser registradas")
    dpi = env.get("dpi", 144)
    if type(dpi) is not int or not 72 <= dpi <= 300:
        raise ValueError("dpi deve ser inteiro entre 72 e 300")
    cases = manifest.get("cases")
    if not isinstance(cases, list) or len(cases) < 4:
        raise ValueError("São exigidos quatro controles independentes")
    modes = [x.get("mode") for x in cases]
    if set(modes) != MODES:
        raise ValueError("Faltam modalidades canônicas")
    labels = [x.get("id") for x in cases]
    if any(not isinstance(x, str) or not x for x in labels) or len(set(labels)) != len(labels):
        raise ValueError("IDs de controles precisam ser únicos")
    used = set()
    files = []
    resolved = []
    for case in cases:
        if not isinstance(case, dict):
            raise ValueError("Controle inválido")
        mode = case["mode"]
        edit = case.get("expected_edit")
        if mode == "no_edit":
            if edit is not None:
                raise ValueError("no_edit não admite edição esperada")
        elif not isinstance(edit, dict) or not isinstance(edit.get("before"), str) or not isinstance(edit.get("after"), str) or not edit["before"] or edit["before"] == edit["after"]:
            raise ValueError("Edição exige before e after diferentes")
        if case.get("callback_status") not in {"not_created", "persisted", "not_captured"}:
            raise ValueError("callback_status obrigatório")
        if case["callback_status"] == "not_created" and mode != "no_edit":
            raise ValueError("Controle editado não pode declarar callback not_created")
        if case["callback_status"] == "not_created" and case.get("callback_docx") is not None:
            raise ValueError("callback not_created não admite DOCX persistido")
        if case["callback_status"] == "persisted" and case.get("callback_docx") is None:
            raise ValueError("Persistência precisa de candidato do callback")
        current = {"id": case["id"], "mode": mode, "expected_edit": edit,
                   "callback_status": case["callback_status"], "artifacts": {},
                   "probes": case.get("probes", [])}
        if not isinstance(current["probes"], list):
            raise ValueError("probes deve ser lista")
        for key in ARTIFACTS:
            necessary = key in ("source_docx", "sdk_docx", "operator_log")
            path = _entry(root, case.get(key), required=necessary)
            if path is None:
                continue
            if path in used:
                raise ValueError("Reuso do mesmo arquivo entre sessões/etapas")
            used.add(path)
            files.append((path, sha256(path)))
            current["artifacts"][key] = path
        current["provenance"] = _check_log(current["artifacts"]["operator_log"], mode)
        if ("source_pdf" in current["artifacts"]) != ("sdk_pdf" in current["artifacts"]):
            raise ValueError("PDFs origem/SDK precisam formar um par")
        if "callback_pdf" in current["artifacts"] and "source_pdf" not in current["artifacts"]:
            raise ValueError("PDF callback precisa de PDF de origem")
        if "source_pdf_repeat" in current["artifacts"] and "source_pdf" not in current["artifacts"]:
            raise ValueError("Repetição de origem requer PDF de origem")
        resolved.append(current)
    return manifest, resolved, files


def reverify(files: list[tuple[Path, str]]) -> None:
    if any(not p.is_file() or sha256(p) != digest for p, digest in files):
        raise ValueError("Evidência alterada durante a avaliação: relatório não emitido")
