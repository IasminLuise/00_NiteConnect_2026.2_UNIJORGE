"""Two-pass evaluation: M2P4 factual diagnosis, then M2P5 conservative decision."""
from __future__ import annotations

import json
from collections import Counter
from pathlib import Path

from .evidence import DIGEST, reverify, validate_manifest
from .ooxml import compare, inspect_docx
from .pdf import glyph_probes, raster_compare


def _word_inventory(path: Path | None) -> dict:
    if path is None:
        return {"status": "NOT_COLLECTED"}
    obj = json.loads(path.read_text(encoding="utf-8"))
    if not isinstance(obj, dict):
        raise ValueError("Inventário Word deve ser objeto JSON")
    status = {}
    for name in ("source", "sdk", "callback"):
        data = obj.get(name)
        if data is None:
            continue
        if not isinstance(data, dict):
            raise ValueError("Inventário Word inválido")
        mandatory = ("source_unmodified", "layout_changed_during_pdf", "page_count_before",
                     "page_count_after", "fields_before", "fields_after")
        if any(x not in data for x in mandatory):
            raise ValueError("Inventário Word incompleto")
        if (type(data["source_unmodified"]) is not bool or
                type(data["layout_changed_during_pdf"]) is not bool or
                any(type(data[x]) is not int or data[x] < 1 for x in ("page_count_before", "page_count_after")) or
                not isinstance(data["fields_before"], list) or
                not isinstance(data["fields_after"], list)):
            raise ValueError("Inventário Word possui valores inválidos")
        for key in ("fields_before", "fields_after"):
            if not all(isinstance(f, dict) and isinstance(f.get("name"), str)
                       and isinstance(f.get("instruction_sha256"), str)
                       and isinstance(f.get("result_sha256"), str)
                       and DIGEST.fullmatch(f["instruction_sha256"]) and DIGEST.fullmatch(f["result_sha256"])
                       for f in data[key]):
                raise ValueError("Campos Word exigem nome e hashes para comparar sem vazar conteúdo")
        before, after = data["fields_before"], data["fields_after"]
        status[name] = {"source_unmodified_reported": data["source_unmodified"],
                        "layout_changed_during_pdf_reported": data["layout_changed_during_pdf"],
                        "page_counts": [data["page_count_before"], data["page_count_after"]],
                        "field_counts": [len(before), len(after)],
                        "field_instructions_changed_during_export":
                        [f["instruction_sha256"] for f in before] !=
                        [f["instruction_sha256"] for f in after],
                        "field_results_changed_during_export":
                        [f["result_sha256"] for f in before] !=
                        [f["result_sha256"] for f in after],
                        "instrument_strength": "OPERATOR_PROBE_REPORT_ONLY"}
    if not {"source", "sdk"}.issubset(status):
        raise ValueError("Inventário Word deve conter source e sdk")
    return {"status": "COLLECTED_DECLARED_NOT_INDEPENDENTLY_CERTIFIED", "stages": status}


def _pair(name: str, a: dict, b: dict, edit: dict | None, pa: Path | None,
          pb: Path | None, dpi: int, probes: list[dict]) -> dict:
    result = {"pair": name, "docx": compare(a, b, expected_edit=edit)}
    if pa is None or pb is None:
        result["pdf"] = {"status": "NOT_COLLECTED"}
        result["glyph_probes"] = {"status": "NOT_COLLECTED"}
    else:
        result["pdf"] = raster_compare(pa, pb, dpi=dpi)
        result["glyph_probes"] = glyph_probes(pa, pb, probes) if probes else []
    return result


def analyze(manifest_path: Path) -> dict:
    manifest, cases, inputs = validate_manifest(manifest_path)
    dpi = manifest["environment"].get("dpi", 144)
    out = []
    for case in cases:
        files = case["artifacts"]
        a = inspect_docx(files["source_docx"])
        b = inspect_docx(files["sdk_docx"])
        callback = inspect_docx(files["callback_docx"]) if "callback_docx" in files else None
        source_pdf = files.get("source_pdf")
        sdk_pdf = files.get("sdk_pdf")
        callback_pdf = files.get("callback_pdf")
        pairs = [_pair("SOURCE_TO_SDK", a, b, case["expected_edit"], source_pdf,
                       sdk_pdf, dpi, case["probes"])]
        if callback is not None:
            pairs.append(_pair("SOURCE_TO_CALLBACK", a, callback, case["expected_edit"],
                               source_pdf, callback_pdf, dpi, case["probes"]))
            pairs.append(_pair("SDK_TO_CALLBACK", b, callback, None, sdk_pdf,
                               callback_pdf, dpi, case["probes"]))
        repeat = None
        if source_pdf is not None and "source_pdf_repeat" in files:
            repeat = raster_compare(source_pdf, files["source_pdf_repeat"], dpi=dpi)
        out.append({"id": case["id"], "mode": case["mode"],
                    "callback_status": case["callback_status"],
                    "provenance": case["provenance"],
                    "pairs": pairs, "source_pdf_repeat": repeat,
                    "word_inventory": _word_inventory(files.get("word_inventory"))})
    # Fail closed: no report is returned for mutation during analysis.
    reverify(inputs)
    factual = _m2p4(out)
    decision = _m2p5(out, factual, manifest)
    return {"schema_version": 1, "campaign_id": manifest["campaign_id"],
            "environment": {"onlyoffice_build": manifest["environment"]["onlyoffice_build"],
                            "word_build": manifest["environment"]["word_build"], "dpi": dpi,
                            "export_method": manifest["environment"].get("export_method", "not_declared")},
            "corpus_kind": manifest["corpus_kind"], "input_hashes_verified_before_and_after": True,
            "m2p4": factual, "m2p5": decision,
            "status": "HOLD_QUARANTINE", "canPromote": False, "officialApproved": False,
            "blocked_gates": ["G1", "G2", "M3"], "cases": out}


def _m2p4(cases: list[dict]) -> dict:
    no_edit = [x for x in cases if x["mode"] == "no_edit"]
    baseline = []
    counter = Counter()
    for trial in cases:
        for pair in trial["pairs"]:
            dif = pair["docx"]
            if not dif["expected_visible_text_only"]:
                counter["unexpected_visible_text"] += 1
            if dif["break_attributes_changed"]:
                counter["breaks_changed"] += 1
            if not dif["media_unchanged"]:
                counter["media_changed"] += 1
            if not dif["note_content_and_references_unchanged"]:
                counter["notes_changed_or_unaligned"] += 1
            if (dif["opc_added"] or dif["opc_removed"] or dif["opc_modified"]):
                counter["opc_changed_unproven"] += 1
            if (dif["field_analysis"]["field_types_same"] is False or
                dif["field_analysis"]["instruction_changes"]):
                counter["field_changes_unproven"] += 1
            pdf = pair["pdf"]
            if pdf.get("status") == "NOT_COLLECTED":
                counter["missing_pdf_pair"] += 1
            elif not pdf["comparable"]:
                counter["pdf_page_or_size_mismatch"] += 1
        if trial["mode"] == "no_edit":
            first = trial["pairs"][0]
            baseline.append({"id": trial["id"], "source_to_sdk_raster": first["pdf"],
                             "source_repeat_raster": trial["source_pdf_repeat"],
                             "source_to_sdk_changed_opc_parts":
                             len(first["docx"]["opc_added"]) + len(first["docx"]["opc_removed"]) +
                             len(first["docx"]["opc_modified"])})
    return {"scope": "M2P4_ROUNDTRIP_FACTUAL_DIAGNOSIS",
            "control_counts": dict(sorted(Counter(x["mode"] for x in cases).items())),
            "baseline_no_edit": baseline, "alert_counts": dict(counter),
            "causal_bounds": [
                "Diferença no no_edit é anterior a qualquer edição humana, mas não identifica isoladamente o componente que a causou.",
                "PDF de origem repetido mede a estabilidade do caminho Word apenas nessa condição; não autoriza ignorar pixels.",
                "Diferenças entre SDK e callback precisam de comparação antes de atualizar campos Word.",
                "Instruções e resultados de campos armazenados são distintos de resultados após SaveAs2.",
                "Igualdade de relacionamentos sem IDs e de textos de notas não prova equivalência OPC.",
                "Logs de colagem humana atestados não são eventos instrumentados.",
            ], "equivalence_rules_added": []}


def _m2p5(cases: list[dict], m2p4: dict, manifest: dict) -> dict:
    criteria = []
    def add(id_: str, status: str, reason: str):
        criteria.append({"id": id_, "result": status, "reason": reason})
    docs = [p["docx"] for trial in cases for p in trial["pairs"]]
    visual = [p["pdf"] for trial in cases for p in trial["pairs"]]
    if all(d["expected_visible_text_only"] for d in docs):
        add("VISIBLE_BODY_TEXT", "PASS_SCOPE_ONLY", "Texto do corpo coincide com troca esperada neste corpus.")
    else:
        add("VISIBLE_BODY_TEXT", "FAIL", "Há mudança textual inesperada em pelo menos um par.")
    if any(not d["media_unchanged"] for d in docs):
        add("EMBEDDED_MEDIA", "FAIL", "Bytes ou presença de mídia divergiram.")
    else:
        add("EMBEDDED_MEDIA", "PASS_SCOPE_ONLY", "Mídia encontrada no corpus não divergiu.")
    if any(not d["note_content_and_references_unchanged"] for d in docs):
        add("NOTES_SEMANTICS", "UNPROVEN", "Notas ou referências mudaram: equivalência não demonstrada.")
    else:
        add("NOTES_SEMANTICS", "PASS_SCOPE_ONLY", "Inventário de notas e referências coincide.")
    if any((d["opc_added"] or d["opc_removed"] or d["opc_modified"] or
            d["style_ids_added"] or d["style_ids_removed"] or
            d["style_definitions_changed_with_same_id"] or d["break_attributes_changed"])
           for d in docs):
        add("OOXML_STRUCTURAL_EQUIVALENCE", "UNPROVEN", "Transformações estruturais não têm prova de equivalência.")
    else:
        add("OOXML_STRUCTURAL_EQUIVALENCE", "PASS_SCOPE_ONLY", "Pacotes inspecionados sem mudanças estruturais detectadas.")
    if any(not d["field_analysis"]["field_types_same"] or
           d["field_analysis"]["instruction_changes"] is None or
           d["field_analysis"]["instruction_changes"] for d in docs):
        add("FIELD_INSTRUCTIONS", "UNPROVEN", "Campos sem pareamento seguro ou instruções divergentes.")
    elif any(d["field_analysis"]["stored_result_changes"] for d in docs):
        add("FIELD_INSTRUCTIONS", "UNPROVEN", "Resultados armazenados mudaram antes do Word; sem equivalência.")
    else:
        add("FIELD_INSTRUCTIONS", "PASS_SCOPE_ONLY", "Instruções e resultados alinhados não divergem nos pares.")
    missing = any(x.get("status") == "NOT_COLLECTED" for x in visual)
    mismatch = any(x.get("status") != "NOT_COLLECTED" and not x["comparable"] for x in visual)
    nonzero = any(x.get("status") != "NOT_COLLECTED" and x["total_different_pixels"] > 0 for x in visual)
    if missing:
        add("VISUAL_FIDELITY", "UNPROVEN", "Pares PDF exigidos ausentes.")
    elif mismatch:
        add("VISUAL_FIDELITY", "FAIL", "Páginas, dimensões ou renderização incompatíveis.")
    elif nonzero:
        add("VISUAL_FIDELITY", "UNPROVEN", "Pixels divergentes presentes; nenhuma tolerância foi concedida.")
    else:
        add("VISUAL_FIDELITY", "PASS_SCOPE_ONLY", "Rasters dos pares fornecidos coincidem integralmente.")
    probes = [p["glyph_probes"] for trial in cases for p in trial["pairs"]]
    if not probes or any(isinstance(p, dict) and p.get("status") == "NOT_COLLECTED" for p in probes) or not any(isinstance(p, list) and p for p in probes):
        add("GLYPH_LOCALIZATION", "UNPROVEN", "Probes direcionados ausentes.")
    elif any(isinstance(p, list) and (not x["comparable"] or x["font_or_color_changed"] or
                                           x["max_dx_pt"] or x["max_dy_pt"]) for p in probes for x in p):
        add("GLYPH_LOCALIZATION", "UNPROVEN", "Diferença de glifo/fonte/cor detectada; requer causalidade e revisão.")
    else:
        add("GLYPH_LOCALIZATION", "PASS_SCOPE_ONLY", "Probes fornecidos sem divergência.")
    word = [c["word_inventory"] for c in cases]
    if any(w["status"] == "NOT_COLLECTED" for w in word):
        add("WORD_PRE_POST_EXPORT", "UNPROVEN", "Inventário pré/pós-Word indisponível em parte do corpus.")
    elif any(not s["source_unmodified_reported"] or s["layout_changed_during_pdf_reported"]
             or s["page_counts"][0] != s["page_counts"][1]
             for w in word for s in w["stages"].values()):
        add("WORD_PRE_POST_EXPORT", "FAIL", "Probe indicou modificação de fonte ou layout após PDF.")
    else:
        add("WORD_PRE_POST_EXPORT", "UNPROVEN", "Estados Word declarados, ainda sem certificação independente de campos.")
    if manifest["corpus_kind"] != "official_controlled":
        add("OFFICIAL_REPRESENTATIVE_CORPUS", "UNPROVEN", "A fixture sintética não substitui corpus institucional representativo.")
    else:
        add("OFFICIAL_REPRESENTATIVE_CORPUS", "UNPROVEN", "Corpus marcado como institucional, sem validação externa de abrangência.")
    add("BACKEND_GATE_INTEGRATION", "UNPROVEN", "Este kit offline não executa suítes Java, segurança, API ou gate institucional.")
    add("INDEPENDENT_EDITORIAL_SIGNOFF", "UNPROVEN", "Aprovação editorial independente não está demonstrada pelo kit.")
    failures = [x["id"] for x in criteria if x["result"] == "FAIL"]
    undecided = [x["id"] for x in criteria if x["result"] == "UNPROVEN"]
    recommendation = "NO_GO_FOR_DEFINITIVE_ADOPTION" if failures else "HOLD_DECISION_NOT_ENOUGH_EVIDENCE"
    return {"scope": "M2P5_ENGINE_ARCHITECTURE_DECISION",
            "recommendation": recommendation,
            "definitive_engine_approved": False, "backend_final_integration_authorized": False,
            "failures": failures, "unproven": undecided, "criteria": criteria,
            "formal_next_decision": "Independent technical/editorial board must adjudicate documented gaps; never auto-GO",
            "advisory_only": True, "policy_relaxations": []}


def markdown(report: dict) -> str:
    result = ["# B05-M2P4 + B05-M2P5 — diagnóstico e decisão", "",
              f"Campanha: `{report['campaign_id']}`", "",
              "**Status: HOLD_QUARANTINE. canPromote=false; officialApproved=false; G1/G2/M3 bloqueados.**", "",
              "## Resposta M2P4 — o que muda sem edição?", "",
              "O inventário por pares está no JSON. O baseline sem edição e a repetição independente de PDF são mantidos sem mascaramento.", "",
              "## Resposta M2P5 — elegibilidade arquitetural", "",
              f"**Recomendação:** `{report['m2p5']['recommendation']}` (consultiva, não altera o backend).", "",
              "| Critério | Resultado | Justificativa |", "|---|---|---|"]
    for c in report["m2p5"]["criteria"]:
        result.append(f"| {c['id']} | {c['result']} | {c['reason']} |")
    result += ["", "## Limites de atribuição", ""]
    result += ["- " + x for x in report["m2p4"]["causal_bounds"]]
    result += ["", "**Nenhuma conclusão automática autoriza GO, exceções, normalização corretiva, merge ou deploy.**", ""]
    return "\n".join(result)


def write(report: dict, folder: Path) -> tuple[Path, Path]:
    folder = folder.resolve()
    if folder.exists():
        raise FileExistsError("Destino existe: evidências anteriores não podem ser sobrescritas")
    folder.mkdir(parents=True, exist_ok=False)
    j = folder / "decision.json"
    m = folder / "decision.md"
    j.write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    m.write_text(markdown(report), encoding="utf-8")
    return j, m
