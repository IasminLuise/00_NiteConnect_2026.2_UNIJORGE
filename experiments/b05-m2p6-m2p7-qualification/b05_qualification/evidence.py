"""Read-only input validation with immutable, contained evidence paths."""
from __future__ import annotations

import hashlib
import json
import re
from pathlib import Path

SHA256 = re.compile(r"[0-9a-f]{64}\Z")
MAX_JSON_BYTES = 4 * 1024 * 1024
FEATURES = frozenset({
    "tables", "images", "footnotes", "endnotes", "page_breaks", "fields",
    "headers_footers", "numbered_lists", "section_breaks", "styles",
    "hyperlinks", "comments", "tracked_changes"
})
CHECKS = frozenset({
    "normal_api_permissions", "entra_authentication", "role_authorization",
    "signed_callbacks", "quarantine_and_head", "word_pdf_export",
    "sdk_callback_provenance", "recovery_and_audit"
})
REVIEW_ROLES = frozenset({"technical", "editorial", "institutional"})


def digest(path: Path) -> str:
    h = hashlib.sha256()
    with path.open("rb") as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b""):
            h.update(chunk)
    return h.hexdigest()


def read_json(path: Path) -> dict:
    if path.stat().st_size > MAX_JSON_BYTES:
        raise ValueError("JSON excede tamanho permitido")
    value = json.loads(path.read_text(encoding="utf-8"))
    if not isinstance(value, dict):
        raise ValueError("JSON deve ser um objeto")
    return value


def _entry(root: Path, ref: object, seen: set[Path], inputs: list[tuple[Path, str]]) -> Path:
    if not isinstance(ref, dict) or set(ref) != {"path", "sha256"}:
        raise ValueError("Cada evidência exige path e sha256")
    name, value = ref["path"], ref["sha256"]
    if not isinstance(name, str) or not name or not isinstance(value, str) or not SHA256.fullmatch(value):
        raise ValueError("Referência ou SHA-256 inválido")
    candidate = (root / name).resolve()
    if not candidate.is_relative_to(root) or not candidate.is_file():
        raise ValueError("Evidência ausente ou fora da campanha")
    if candidate in seen:
        raise ValueError("Evidência física reutilizada: cada registro deve ter sua própria cópia")
    seen.add(candidate)
    if digest(candidate) != value:
        raise ValueError("Hash incorreto para evidência")
    inputs.append((candidate, value))
    return candidate


def load_manifest(path: Path) -> tuple[dict, dict, list[tuple[Path, str]]]:
    path = path.resolve()
    root = path.parent
    raw = read_json(path)
    if raw.get("schema_version") != 1 or not isinstance(raw.get("campaign_id"), str) or not raw["campaign_id"]:
        raise ValueError("Versão de manifesto ou campaign_id inválido")
    if raw.get("corpus_kind") not in ("synthetic", "official_controlled"):
        raise ValueError("Corpus inválido")
    required = raw.get("required_features")
    if not isinstance(required, list) or not required or len(required) != len(set(map(str, required))) or not set(required) <= FEATURES:
        raise ValueError("required_features deve conter funcionalidades identificadas e sem duplicatas")
    documents = raw.get("documents")
    if not isinstance(documents, list) or not documents:
        raise ValueError("É necessário ao menos um documento de corpus")
    evidence = raw.get("integration_checks")
    if not isinstance(evidence, list):
        raise ValueError("integration_checks deve ser uma lista")
    if {x.get("id") for x in evidence if isinstance(x, dict)} != CHECKS or len(evidence) != len(CHECKS):
        raise ValueError("Os oito controles de integração precisam ser declarados separadamente")
    reviews = raw.get("reviews")
    if not isinstance(reviews, list):
        raise ValueError("reviews deve ser lista")
    seen: set[Path] = set()
    inputs: list[tuple[Path, str]] = []
    prior = _entry(root, raw.get("m2p5_report"), seen, inputs)
    previous = read_json(prior)
    if previous.get("schema_version") != 1 or previous.get("status") != "HOLD_QUARANTINE" or not isinstance(previous.get("m2p5"), dict):
        raise ValueError("Relatório M2P5 inválido ou não compatível")
    prior_decision = previous["m2p5"]
    if (previous.get("canPromote") is not False or previous.get("officialApproved") is not False
            or previous.get("blocked_gates") != ["G1", "G2", "M3"]
            or prior_decision.get("backend_final_integration_authorized") is not False
            or prior_decision.get("definitive_engine_approved") is not False
            or prior_decision.get("advisory_only") is not True
            or prior_decision.get("recommendation") not in
               {"HOLD_DECISION_NOT_ENOUGH_EVIDENCE", "NO_GO_FOR_DEFINITIVE_ADOPTION"}):
        raise ValueError("Relatório anterior não preserva decisões e bloqueios consultivos")
    by_id: set[str] = set()
    docs = []
    for doc in documents:
        if not isinstance(doc, dict) or not isinstance(doc.get("id"), str) or not doc["id"] or doc["id"] in by_id:
            raise ValueError("Documento sem identificador único")
        by_id.add(doc["id"])
        if not isinstance(doc.get("declared_features"), list) or not set(doc["declared_features"]) <= FEATURES:
            raise ValueError("Features declaradas inválidas")
        if doc.get("origin") not in {"synthetic", "official_controlled"}:
            raise ValueError("Proveniência inválida")
        operation = doc.get("operation")
        if operation not in {"no_edit", "edited"}:
            raise ValueError("Cada documento deve declarar operação no_edit ou edited")
        expected = doc.get("expected_edit")
        if (operation == "no_edit" and expected is not None) or (operation == "edited" and expected is None):
            raise ValueError("no_edit proíbe expected_edit; edited exige expected_edit")
        if operation == "edited" and doc.get("callback_docx") is None:
            raise ValueError("edited exige DOCX persistido pelo callback")
        if expected is not None and (not isinstance(expected, dict) or set(expected) != {"before", "after"}
                                     or not isinstance(expected["before"], str)
                                     or not isinstance(expected["after"], str)
                                     or not expected["before"] or expected["before"] == expected["after"]):
            raise ValueError("Edição esperada exige before/after diferentes")
        paths = {k: _entry(root, doc[k], seen, inputs) for k in ("source_docx", "sdk_docx")}
        if "callback_docx" in doc and doc["callback_docx"] is not None:
            paths["callback_docx"] = _entry(root, doc["callback_docx"], seen, inputs)
        semantic_receipt = _entry(root, doc["semantic_review_receipt"], seen, inputs) if doc.get("semantic_review_receipt") is not None else None
        authorization = None
        if doc["origin"] == "official_controlled":
            if raw["corpus_kind"] != "official_controlled":
                raise ValueError("Documento oficial em campanha sintética")
            authorization = _entry(root, doc.get("authorization_receipt"), seen, inputs)
        docs.append({"id": doc["id"], "origin": doc["origin"], "operation": operation,
                     "declared_features": sorted(set(doc["declared_features"])),
                     "files": paths, "expected_edit": expected,
                     "semantic_review_receipt_present": bool(semantic_receipt),
                     "authorization_receipt_present": bool(authorization)})
    checks = []
    for e in evidence:
        if not isinstance(e, dict) or e.get("result") not in {"PASS_ATTESTED", "FAIL_ATTESTED", "UNVERIFIED"}:
            raise ValueError("Controle de integração inválido")
        proof = _entry(root, e["evidence"], seen, inputs) if e.get("evidence") is not None else None
        if e["result"] != "UNVERIFIED" and proof is None:
            raise ValueError("Declaração de aprovação/falha exige arquivo de evidência")
        checks.append({"id": e["id"], "result": e["result"], "evidence_present": proof is not None,
                       "validation": "FILE_HASH_ONLY_NOT_INDEPENDENT_API_VERIFICATION"})
    reviewers = []
    for review in reviews:
        if not isinstance(review, dict) or review.get("role") not in REVIEW_ROLES or review.get("decision") not in {"ACCEPT_ATTESTED", "REJECT_ATTESTED", "PENDING"}:
            raise ValueError("Revisão inválida")
        proof = _entry(root, review["evidence"], seen, inputs) if review.get("evidence") is not None else None
        if review["decision"] != "PENDING" and proof is None:
            raise ValueError("Parecer declarado exige evidência")
        reviewers.append({"role": review["role"], "decision": review["decision"],
                          "evidence_present": proof is not None,
                          "validation": "REVIEW_ATTESTATION_NOT_IDENTITY_VERIFICATION"})
    return raw, {"previous": previous, "documents": docs, "integration_checks": checks,
                 "reviews": reviewers}, inputs


def reverify(inputs: list[tuple[Path, str]]) -> None:
    if any(not file.is_file() or digest(file) != expected for file, expected in inputs):
        raise ValueError("Evidências mudaram durante avaliação, sem relatório")
