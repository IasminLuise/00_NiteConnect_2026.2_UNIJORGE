"""One pass, two answers: semantic bounds (M2P6), institutional readiness (M2P7)."""
from __future__ import annotations

import json
from collections import Counter
from pathlib import Path

from .evidence import CHECKS, REVIEW_ROLES, load_manifest, reverify, _entry
from .semantic import compare_docs, inspect_docx


def _adjudications(raw: dict, previous: dict, root: Path,
                   seen_inputs: list[tuple[Path, str]]) -> list[dict]:
    """Read written adjudication receipts, not grant approval. Validates hashes."""
    # This intentionally permits sharing evidence files with other witness roles only
    # when they are distinct physical files; provenance cannot be borrowed silently.
    decisions = raw.get("adjudications")
    if not isinstance(decisions, list):
        raise ValueError("adjudications deve ser lista")
    expected = previous.get("m2p5", {}).get("unproven")
    if not isinstance(expected, list) or not all(isinstance(x, str) for x in expected):
        raise ValueError("Relatório M2P5 não contém enumeração de UNPROVEN")
    if len(decisions) != len(expected) or sorted(x.get("criterion") for x in decisions if isinstance(x, dict)) != sorted(expected):
        raise ValueError("Todos os UNPROVEN anteriores requerem adjudicação independente, mesmo que pendente")
    occupied = {p for p, _ in seen_inputs}
    answer = []
    for item in decisions:
        if not isinstance(item, dict) or item.get("status") not in {"RESOLVED_ATTESTED", "PENDING", "REJECTED_ATTESTED"}:
            raise ValueError("Status de adjudicação inválido")
        proof = _entry(root, item["evidence"], occupied, seen_inputs) if item.get("evidence") is not None else None
        if item["status"] != "PENDING" and proof is None:
            raise ValueError("Adjudicação sem evidência")
        answer.append({"criterion": item["criterion"], "status": item["status"],
                       "evidence_present": proof is not None,
                       "validation": "ATTESTED_REVIEW_NOT_INDEPENDENTLY_CERTIFIED"})
    return answer


def evaluate(manifest: Path) -> dict:
    raw, records, inputs = load_manifest(manifest)
    # Detect any changes during document comparisons, and after the adjudication reads.
    root = manifest.resolve().parent
    adjudicated = _adjudications(raw, records["previous"], root, inputs)
    corpus_scope = raw.get("corpus_scope_receipt")
    if corpus_scope is not None:
        from .evidence import _entry as checked_entry
        _ = checked_entry(root, corpus_scope, {p for p, _ in inputs}, inputs)
    docs = []
    features_observed = set()
    high_risk = []
    pending_semantics = []
    for document in records["documents"]:
        values = {name: inspect_docx(path) for name, path in document["files"].items()}
        source = values["source_docx"]
        features_observed.update(source["features"])
        pairs = {}
        for name in ("sdk_docx", "callback_docx"):
            if name not in values:
                continue
            result = compare_docs(source, values[name], document["expected_edit"])
            pairs["SOURCE_TO_" + name.removesuffix("_docx").upper()] = result
            if not result["expected_visible_text_only"]:
                high_risk.append("UNEXPECTED_VISIBLE_TEXT_CHANGE_IN_" + name.removesuffix("_docx").upper())
            if result["candidate_new_broken_relationships"] or result["candidate_new_missing_content_types"]:
                high_risk.append("NEW_BROKEN_OPC_IN_" + name.removesuffix("_docx").upper())
        if "callback_docx" in values:
            pairs["SDK_TO_CALLBACK"] = compare_docs(values["sdk_docx"], values["callback_docx"])
        semantic_changes = any(
            pair["explicit_format_mismatch_chars_on_matched_text"] > 0 or
            not pair["notes_referenced_text_signatures_equal"] or
            not pair["field_instructions_equal_in_order"] or
            not pair["style_equivalence_proven"] or
            bool(pair["parts_added"] or pair["parts_removed"] or pair["parts_modified"])
            for pair in pairs.values())
        if semantic_changes and not document["semantic_review_receipt_present"]:
            pending_semantics.append(document["id"])
        docs.append({"id": document["id"], "origin": document["origin"], "operation": document["operation"],
                     "authorization_receipt_present": document["authorization_receipt_present"],
                     "semantic_changes_require_review": semantic_changes,
                     "semantic_review_receipt_present": document["semantic_review_receipt_present"],
                     "features_claimed_not_proven": document["declared_features"],
                     "features_observed_source": source["features"], "pairs": pairs})
    reverify(inputs)
    required = set(raw["required_features"])
    missing = sorted(required - features_observed)
    prev = records["previous"]
    prior_fail = list(prev["m2p5"].get("failures", []))
    if not isinstance(prev["m2p5"].get("failures"), list):
        raise ValueError("Relatório M2P5 sem enumeração de falhas")
    if prior_fail:
        high_risk.append("UNRESOLVED_PRIOR_M2P5_FAIL")
    checks = records["integration_checks"]
    reviews = records["reviews"]
    missing_checks = sorted(item["id"] for item in checks if item["result"] != "PASS_ATTESTED")
    missing_reviews = sorted(REVIEW_ROLES - {r["role"] for r in reviews if r["decision"] == "ACCEPT_ATTESTED"})
    missing_adjudications = sorted(item["criterion"] for item in adjudicated if item["status"] != "RESOLVED_ATTESTED")
    corpus_official = raw["corpus_kind"] == "official_controlled" and all(
        d["origin"] == "official_controlled" and d["authorization_receipt_present"] for d in docs)
    held_reasons = []
    if {d["operation"] for d in docs} != {"no_edit", "edited"}:
        held_reasons.append("EDITED_AND_NO_EDIT_CONTROLS_REQUIRED_FOR_READINESS")
    if pending_semantics:
        held_reasons.append("NEW_STRUCTURAL_DIFFERENCES_WITHOUT_SEMANTIC_REVIEW")
    if not corpus_official: held_reasons.append("OFFICIAL_AUTHORIZED_CORPUS_NOT_PROVEN")
    if not raw.get("corpus_scope_receipt"): held_reasons.append("CORPUS_SCOPE_ACCEPTANCE_NOT_PROVIDED")
    if missing: held_reasons.append("REQUIRED_FEATURE_COVERAGE_MISSING")
    if missing_checks: held_reasons.append("NORMAL_BACKEND_INTEGRATION_NOT_FULLY_ATTESTED")
    if missing_reviews: held_reasons.append("INDEPENDENT_BOARD_ROLES_MISSING")
    if missing_adjudications: held_reasons.append("PRIOR_UNPROVEN_NOT_ADJUDICATED")
    if any(r["decision"] == "REJECT_ATTESTED" for r in reviews): high_risk.append("REVIEW_REJECTED")
    if any(item["status"] == "REJECTED_ATTESTED" for item in adjudicated): high_risk.append("ADJUDICATION_REJECTED")
    if any(item["result"] == "FAIL_ATTESTED" for item in checks): high_risk.append("BACKEND_CHECK_FAILED_ATTESTED")
    # M2P6 observations only: a matching visible text segment with different explicit
    # OOXML formatting is not necessarily a functional regression (inheritance/runs).
    prior_baseline = []
    for case in prev.get("m2p4", {}).get("baseline_no_edit", []):
        source_sdk = case.get("source_to_sdk_raster") or {}
        original_repeat = case.get("source_repeat_raster") or {}
        prior_baseline.append({"case_id": case.get("id"),
                               "source_to_sdk_total_different_pixels": source_sdk.get("total_different_pixels"),
                               "source_reexport_total_different_pixels": original_repeat.get("total_different_pixels"),
                               "raster_recomputed_in_this_patch": False})
    m2p6 = {"scope": "STRUCTURE_AND_SEMANTIC_BOUNDS", "documents": docs,
             "prior_m2p4_no_edit_raster": prior_baseline,
             "baseline_m2p5_report_used": True,
             "objective_broken_candidate_opc": bool(any(x.startswith("NEW_BROKEN_OPC") for x in high_risk)),
             "new_equivalence_rules": [],
             "limits": [
                 "A análise de estilos é explícita, não computa formatação efetiva do Word.",
                 "Matching por offset de caracteres não estabelece equivalência de todos os runs.",
                 "Notas são alinhadas somente pelo texto referenciado, não por ciclo de vida e semântica completa.",
                 "Campos verificam instruções armazenadas, não semântica de recálculo ou resultados visuais.",
                 "Sem reexecutar Word/PDF, os 1452 pixels observados antes permanecem não resolvidos.",
                 "Partes OPC, links externos e relacionamentos não são normalizados para forçar aprovação.",
             ]}
    if high_risk:
        recommendation = "STOP_AND_REVIEW_DEMONSTRATED_OR_ATTESTED_RISKS"
    elif held_reasons:
        recommendation = "HOLD_DECISION_NOT_ENOUGH_EVIDENCE"
    else:
        recommendation = "READY_FOR_FORMAL_GATE_REVIEW_NOT_APPROVAL"
    m2p7 = {"scope": "INSTITUTIONAL_FINAL_ADOPTION_READINESS", "recommendation": recommendation,
             "consultative_only": True, "definitive_engine_approved": False,
             "backend_final_integration_authorized": False,
             "required_features": sorted(required), "observed_features": sorted(features_observed),
             "feature_gaps": missing, "official_corpus_and_authorization_proven_by_receipt": corpus_official,
             "corpus_representativeness_independently_proven": False,
             "corpus_scope_receipt_present": bool(corpus_scope),
             "integration_checks": checks, "missing_integration_checks": missing_checks,
             "reviews": reviews, "missing_review_roles": missing_reviews,
             "prior_unproven_adjudications": adjudicated,
             "missing_adjudications": missing_adjudications,
             "hold_reasons": sorted(held_reasons), "risk_flags": sorted(set(high_risk)),
             "documents_pending_semantic_review": len(pending_semantics),
             "gate_controls_untouched": True,
             "formal_followup": "Independent board verifies raw evidence, corpus scope, official models and reviews; this kit cannot approve adoption."}
    return {"schema_version": 1, "campaign_id": raw["campaign_id"],
            "source_m2p5_campaign_id": prev["campaign_id"],
            "all_inputs_hash_verified_before_and_after": True,
            "m2p6": m2p6, "m2p7": m2p7,
            "status": "HOLD_QUARANTINE", "canPromote": False, "officialApproved": False,
            "backend_final_integration_authorized": False, "blocked_gates": ["G1", "G2", "M3"]}


def markdown(result: dict) -> str:
    dec = result["m2p7"]
    lines = ["# B05-M2P6 + B05-M2P7 — qualificação conservadora", "",
             "**HOLD_QUARANTINE — canPromote=false — officialApproved=false — G1/G2/M3 bloqueados.**", "",
             "## M2P6 — limites da equivalência documental", "",
             f"Documentos inspecionados: {len(result['m2p6']['documents'])}.", "",
             "A inspeção não prova semântica completa de estilos, notas, campos ou efeitos visuais.", "",
             "## M2P7 — decisão de prontidão", "",
             f"**Recomendação consultiva:** `{dec['recommendation']}`", "",
             "| Verificação | Situação |", "|---|---|"]
    lines.extend([f"| Corpus oficial autorizado | {'declarado e comprovante presente' if dec['official_corpus_and_authorization_proven_by_receipt'] else 'não comprovado'} |",
                  f"| Lacunas de cobertura | {len(dec['feature_gaps'])} |",
                  f"| Controles de backend incompletos | {len(dec['missing_integration_checks'])} |",
                  f"| Papéis de revisão ausentes | {len(dec['missing_review_roles'])} |",
                  f"| Pendências M2P5 não adjudicadas | {len(dec['missing_adjudications'])} |",
                  f"| Sinais de risco | {len(dec['risk_flags'])} |", "",
                  "## Lacunas e restrições", ""])
    lines.extend("- " + reason for reason in dec["hold_reasons"] + dec["risk_flags"])
    lines += ["", "A aprovação definitiva depende de verificação institucional externa e gates formais; ",
              "este relatório não concede GO, não altera o backend e não libera versões em quarentena.", ""]
    return "\n".join(lines)


def write(report: dict, target: Path) -> tuple[Path, Path]:
    path = target.resolve()
    if path.exists():
        raise FileExistsError("Destino já existe: evidências não serão sobrescritas")
    path.mkdir(parents=True, exist_ok=False)
    j = path / "qualification.json"
    m = path / "qualification.md"
    j.write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    m.write_text(markdown(report), encoding="utf-8")
    return j, m
