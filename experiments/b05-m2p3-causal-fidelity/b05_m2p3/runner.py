"""B05-M2P3 campaign orchestration. Never writes to inputs or enables promotion."""
from __future__ import annotations

import json
import re
from collections import Counter
from datetime import datetime
from pathlib import Path

from .docx_inspector import compare_docx, inspect_docx, sha256_file
from .pdf_inspector import compare_pdfs

MODES = {"no_edit", "typed", "plain_paste", "rich_paste"}
ACTIONS = {"no_edit": ("opened", "closed"), "typed": ("typed_text",),
           "plain_paste": ("paste_plain",), "rich_paste": ("paste_rich",)}
STRENGTHS = {"instrumented", "observed", "attested"}
DIGEST = re.compile(r"^[0-9a-f]{64}$")


def _load_json(path: Path):
    return json.loads(path.read_text(encoding="utf-8"))


def _safe_path(base: Path, relative: str) -> Path:
    if not isinstance(relative, str) or not relative.strip():
        raise ValueError("Um caminho de evidência está ausente")
    p = (base / relative).resolve()
    if not p.is_file():
        raise ValueError(f"Evidência ausente: {Path(relative).name}")
    return p


def _check_digest(path: Path, expected: str) -> str:
    if not isinstance(expected, str) or DIGEST.fullmatch(expected) is None:
        raise ValueError("Manifesto contém SHA-256 inválido ou não preenchido")
    actual = sha256_file(path)
    if actual != expected:
        raise ValueError(f"SHA-256 divergente para entrada: {path.name}")
    return actual


def _log_confirmed(path: Path, mode: str) -> dict:
    data = _load_json(path)
    if data.get("confirmed_mode") != mode:
        raise ValueError("Modo confirmado no log não corresponde ao manifesto")
    strength = data.get("evidence_strength")
    if strength not in STRENGTHS:
        raise ValueError("Log sem força de evidência declarada")
    if not data.get("editor_version") or not data.get("session_label"):
        raise ValueError("Log sem versão do editor ou rótulo local da sessão")
    events = data.get("events")
    if not isinstance(events, list) or not events:
        raise ValueError("Log sem sequência de eventos")
    kinds = [event.get("event") for event in events if isinstance(event, dict)]
    if any(kind not in kinds for kind in ACTIONS[mode]):
        raise ValueError(f"Log não comprova a operação declarada no modo {mode}")
    for event in events:
        try:
            datetime.fromisoformat(event["at"].replace("Z", "+00:00"))
        except (TypeError, KeyError, ValueError, AttributeError) as exc:
            raise ValueError("Evento sem timestamp ISO-8601 válido") from exc
    return {"evidence_strength": strength, "event_count": len(events),
            "editor_version": data["editor_version"]}


def _validate_manifest(manifest: dict) -> None:
    if manifest.get("schema_version") != 1:
        raise ValueError("schema_version precisa ser 1")
    trials = manifest.get("trials")
    if not isinstance(trials, list) or len(trials) < 4:
        raise ValueError("Campanha precisa de pelo menos quatro sessões independentes")
    modes = [t.get("mode") for t in trials]
    if set(modes) != MODES:
        raise ValueError("A campanha deve conter no_edit, typed, plain_paste e rich_paste")
    ids = [t.get("id") for t in trials]
    if any(not isinstance(i, str) or not i for i in ids) or len(ids) != len(set(ids)):
        raise ValueError("IDs de sessão precisam ser únicos e não vazios")
    if not isinstance(manifest.get("campaign_id"), str) or not manifest["campaign_id"]:
        raise ValueError("campaign_id obrigatório")
    for item in trials:
        edit = item.get("expected_edit")
        if item["mode"] == "no_edit" and edit is not None:
            raise ValueError("Controle no_edit não pode declarar edição esperada")
        if item["mode"] != "no_edit" and (not isinstance(edit, dict)
                or not isinstance(edit.get("before"), str)
                or not isinstance(edit.get("after"), str)
                or not edit["before"] or edit["before"] == edit["after"]):
            raise ValueError("Modo editado requer expected_edit.before/after diferentes")
        roi = item.get("edit_region")
        if roi is not None and (not isinstance(roi, dict) or not isinstance(roi.get("page"), int)
                               or not isinstance(roi.get("bbox_pt"), list)
                               or len(roi["bbox_pt"]) != 4
                               or not all(isinstance(n, (int, float)) for n in roi["bbox_pt"])):
            raise ValueError("edit_region requer page e bbox_pt [x0,y0,x1,y1]")


def analyze_campaign(manifest_path: Path, *, dpi: int = 144) -> dict:
    manifest_path = manifest_path.resolve()
    manifest = _load_json(manifest_path)
    _validate_manifest(manifest)
    base = manifest_path.parent
    all_files = []
    identities = set()
    trials = []
    for trial in manifest["trials"]:
        evidence = trial.get("evidence", {})
        if not isinstance(evidence, dict):
            raise ValueError("evidence precisa ser um objeto")
        selected = {}
        for suffix in ("source_docx", "candidate_docx", "source_pdf", "candidate_pdf"):
            obj = _safe_path(base, trial.get(suffix))
            selected[suffix] = obj
            all_files.append((obj, _check_digest(obj, evidence.get(suffix + "_sha256"))))
            # Exact path cannot be reused across controls. Equal bytes are
            # legitimate when copied as separate immutable baselines.
            if obj in identities:
                raise ValueError("Mesmo arquivo reutilizado em sessões independentes")
            identities.add(obj)
        log_path = _safe_path(base, evidence.get("operator_log"))
        if log_path in identities:
            raise ValueError("Log de operador reutilizado entre sessões")
        identities.add(log_path)
        all_files.append((log_path, sha256_file(log_path)))
        provenance = _log_confirmed(log_path, trial["mode"])
        src = inspect_docx(selected["source_docx"])
        dst = inspect_docx(selected["candidate_docx"])
        structural = compare_docx(src, dst, trial.get("expected_edit"))
        visual = compare_pdfs(selected["source_pdf"], selected["candidate_pdf"], dpi=dpi,
                              edit_region=trial.get("edit_region"))
        trials.append({"id": trial["id"], "mode": trial["mode"],
                       "provenance": provenance, "docx": structural, "pdf": visual})
    # A post-examination check detects unintended input changes. It does not
    # certify Windows/Word actions which occurred prior to collection.
    for path, prior in all_files:
        if sha256_file(path) != prior:
            raise ValueError("Entrada mudou durante execução; relatório não produzido")
    observed = Counter()
    for trial in trials:
        for issue in trial["docx"]["issues"]:
            if issue["code"] == "RUN_FORMAT_CHANGED":
                observed[trial["mode"]] += 1
    modes_with_runs = sorted(m for m in MODES if observed[m] > 0)
    if len(modes_with_runs) == 1:
        causal_note = ("Mudança explícita de formato de run foi observada apenas no modo "
                       f"{modes_with_runs[0]} desta campanha. Associação, não causa demonstrada.")
    elif modes_with_runs:
        causal_note = ("Mudanças explícitas de formato de run ocorreram em: "
                       + ", ".join(modes_with_runs) + ". Causa não isolada.")
    else:
        causal_note = ("Nenhuma mudança explícita de formato detectada entre runs alinhados; "
                       "isso não prova fidelidade de estilos efetivos nem isola causa.")
    return {
        "schema_version": 1,
        "campaign_id": manifest["campaign_id"],
        "status": "HOLD_QUARANTINE",
        "canPromote": False, "officialApproved": False,
        "blocked_gates": ["G1", "G2", "M3"],
        "scope": "instrumentacao_local_sem_autorizacao_editorial",
        "inputs_verified_before_and_after": True,
        "mode_counts": dict(sorted(Counter(t["mode"] for t in trials).items())),
        "conclusion_kind": "OBSERVATIONAL_ASSOCIATION_ONLY",
        "causal_note": causal_note,
        "trials": trials,
        "limitations": [
            "Logs declaram operações e proveniência, mas não provam causalidade sozinhos.",
            "Run XML explícito não substitui resolução de estilos herdados nem inspeção Word.",
            "Inventário de campos não interpreta todos os resultados/dinâmicas Word.",
            "Diferenças em partes OPC não foram classificadas como equivalentes.",
            "Rasters PDF não isolam o efeito do SaveAs2 ou do estado de campos.",
            "Nenhuma avaliação automática concede G1/G2/M3 ou publicação.",
        ],
    }


def as_markdown(report: dict) -> str:
    out = ["# B05-M2P3 — qualificação causal (instrumentação)", "",
           f"Campanha: `{report['campaign_id']}`", "",
           "**Decisão: `HOLD_QUARANTINE`.** `canPromote=false`; "
           "`officialApproved=false`; G1/G2/M3 bloqueados.", "",
           "| Sessão | Modalidade | Proveniência | Alertas OOXML | Pixels diferentes (144 dpi) |",
           "|---|---|---|---:|---:|"]
    for t in report["trials"]:
        alerts = len(t["docx"]["issues"])
        pixels = t["pdf"]["total_different_pixels"]
        out.append(f"| {t['id']} | {t['mode']} | {t['provenance']['evidence_strength']} | {alerts} | {pixels:,} |")
    out += ["", "## Leitura causal", "", report["causal_note"], "",
            "**Isto não é homologação editorial ou prova isolada do mecanismo.**", "",
            "## Limitações", ""]
    out += [f"- {value}" for value in report["limitations"]]
    return "\n".join(out) + "\n"


def write_reports(report: dict, out_dir: Path) -> list[Path]:
    out_dir = out_dir.resolve()
    if out_dir.exists():
        raise FileExistsError("Destino já existe; escolha outro diretório para não sobrescrever evidências")
    out_dir.mkdir(parents=True, exist_ok=False)
    artifacts = [out_dir / "qualification.json", out_dir / "qualification.md"]
    artifacts[0].write_text(json.dumps(report, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
    artifacts[1].write_text(as_markdown(report), encoding="utf-8")
    return artifacts
