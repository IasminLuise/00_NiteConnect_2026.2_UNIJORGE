"""Conservative OOXML/OPC comparisons; differing syntax is never auto-equivalent."""
from __future__ import annotations

import hashlib
import zipfile
from collections import Counter
from pathlib import Path
from xml.etree import ElementTree as ET

W = "{http://schemas.openxmlformats.org/wordprocessingml/2006/main}"
R = "{http://schemas.openxmlformats.org/package/2006/relationships}"
LIMIT_BYTES = 200 * 1024 * 1024
LIMIT_PARTS = 5000


def h(data: bytes | str) -> str:
    return hashlib.sha256(data.encode("utf-8") if isinstance(data, str) else data).hexdigest()


def parse(data: bytes, part: str) -> ET.Element:
    try:
        return ET.fromstring(data)
    except ET.ParseError as exc:
        raise ValueError(f"XML inválido em {part}") from exc


def ptext(node: ET.Element) -> str:
    bits = []
    for item in node.iter():
        if item.tag == W + "t":
            bits.append(item.text or "")
        elif item.tag == W + "tab":
            bits.append("\t")
        elif item.tag == W + "br":
            bits.append("\f" if item.get(W + "type") == "page" else "\n")
    return "".join(bits)


def collect_fields(parts: dict[str, bytes]) -> list[dict]:
    """Collect pre-Word instruction/result independently; simple and complex fields."""
    found = []
    for name in sorted(parts):
        if not name.startswith("word/") or not name.endswith(".xml"):
            continue
        if not any(x in name for x in ("document.xml", "header", "footer", "footnotes.xml", "endnotes.xml")):
            continue
        root = parse(parts[name], name)
        for paragraph in root.iter(W + "p"):
            stack = []
            for elt in paragraph.iter():
                if elt.tag == W + "fldSimple":
                    instr = elt.get(W + "instr", "")
                    result = ptext(elt)
                    found.append(_field(name, instr, result, "simple"))
                elif elt.tag == W + "fldChar":
                    action = elt.get(W + "fldCharType")
                    if action == "begin":
                        stack.append({"instr": [], "result": [], "separate": False})
                    elif action == "separate" and stack:
                        stack[-1]["separate"] = True
                    elif action == "end" and stack:
                        state = stack.pop()
                        found.append(_field(name, "".join(state["instr"]),
                                            "".join(state["result"]), "complex"))
                elif elt.tag == W + "instrText" and stack:
                    stack[-1]["instr"].append(elt.text or "")
                elif elt.tag == W + "t" and stack and stack[-1]["separate"]:
                    stack[-1]["result"].append(elt.text or "")
    return found


def _field(part: str, instruction: str, result: str, kind: str) -> dict:
    return {"part": part, "field": instruction.strip().split(maxsplit=1)[0].upper()
            if instruction.strip() else "", "kind": kind, "instruction_hash": h(instruction),
            "result_hash": h(result), "has_result": bool(result)}


def _notes(parts: dict[str, bytes]) -> dict:
    out = {}
    for flavor in ("footnote", "endnote"):
        name = f"word/{flavor}s.xml"
        entries = []
        if name in parts:
            tree = parse(parts[name], name)
            for element in tree.iter(W + flavor):
                entries.append({"id": element.get(W + "id"), "type": element.get(W + "type"),
                                "text_hash": h("\n".join(ptext(p) for p in element.iter(W + "p")))})
        out[flavor] = entries
    tree = parse(parts["word/document.xml"], "word/document.xml")
    for flavor in ("footnote", "endnote"):
        out[f"{flavor}_refs"] = [n.get(W + "id") for n in tree.iter(W + flavor + "Reference")]
    return out


def _styles(parts: dict[str, bytes]) -> dict:
    if "word/styles.xml" not in parts:
        return {"count": 0, "definitions": {}}
    tree = parse(parts["word/styles.xml"], "word/styles.xml")
    items = {}
    for node in tree.iter(W + "style"):
        identity = node.get(W + "styleId", "")
        items[identity] = h(ET.tostring(node, encoding="utf-8"))
    return {"count": len(items), "definitions": items}


def _rels(parts: dict[str, bytes]) -> dict:
    raw, semantic = {}, {}
    for name, data in parts.items():
        if name.endswith(".rels"):
            tree = parse(data, name)
            relationships = [dict(child.attrib) for child in tree.iter(R + "Relationship")]
            raw[name] = sorted(tuple(sorted(item.items())) for item in relationships)
            # This is diagnostic only. Ignoring Id is NOT proof that all XML refs resolve.
            semantic[name] = sorted(tuple(sorted((k, v) for k, v in item.items() if k != "Id"))
                                    for item in relationships)
    return {"raw": raw, "without_ids": semantic}


def inspect_docx(path: Path) -> dict:
    if not zipfile.is_zipfile(path):
        raise ValueError("Arquivo inválido: esperado DOCX/ZIP")
    with zipfile.ZipFile(path) as zf:
        infos = zf.infolist()
        if len(infos) > LIMIT_PARTS or sum(e.file_size for e in infos) > LIMIT_BYTES:
            raise ValueError("Arquivo excede o limite de segurança OOXML")
        names = [i.filename for i in infos if not i.is_dir()]
        if len(names) != len(set(names)):
            raise ValueError("Entradas ZIP duplicadas")
        if "word/document.xml" not in names or "[Content_Types].xml" not in names:
            raise ValueError("Partes DOCX essenciais ausentes")
        if zf.testzip() is not None:
            raise ValueError("DOCX corrompido: CRC divergente")
        parts = {n: zf.read(n) for n in names}
    doc = parse(parts["word/document.xml"], "word/document.xml")
    body = doc.find(W + "body")
    if body is None:
        raise ValueError("Documento sem body")
    paras = list(body.iter(W + "p"))
    br = []
    for para in paras:
        br.append([tuple(sorted(node.attrib.items())) for node in para.iter(W + "br")])
    runs = [len(list(para.iter(W + "r"))) for para in paras]
    text = "\n".join(ptext(para) for para in paras)
    media = {n: h(b) for n, b in parts.items() if n.startswith("word/media/")}
    return {"parts": {n: h(b) for n, b in parts.items()}, "paragraph_text": text,
            "paragraph_count": len(paras), "break_attributes": br,
            "run_counts": runs, "fields": collect_fields(parts), "notes": _notes(parts),
            "styles": _styles(parts), "relationships": _rels(parts), "media": media}


def _compare_fields(before: list[dict], after: list[dict]) -> dict:
    before_names = Counter((x["part"], x["field"], x["kind"]) for x in before)
    after_names = Counter((x["part"], x["field"], x["kind"]) for x in after)
    # List position is an association heuristic, not a stable field identifier.
    comparable = len(before) == len(after) and list(before_names) == list(after_names)
    instruction_changes = result_changes = 0
    if comparable:
        instruction_changes = sum(a["instruction_hash"] != b["instruction_hash"]
                                  for a, b in zip(before, after))
        result_changes = sum(a["result_hash"] != b["result_hash"]
                             for a, b in zip(before, after))
    return {"count": [len(before), len(after)], "field_types_same": before_names == after_names,
            "occurrences_align_heuristically": comparable,
            "instruction_changes": instruction_changes if comparable else None,
            "stored_result_changes": result_changes if comparable else None,
            "known_fields": sorted(set(f["field"] for f in before + after))}


def compare(before: dict, after: dict, *, expected_edit: dict | None) -> dict:
    a = before["paragraph_text"]
    b = after["paragraph_text"]
    if expected_edit is None:
        expected = a == b
    else:
        original, edited = expected_edit["before"], expected_edit["after"]
        expected = a.count(original) == 1 and b == a.replace(original, edited, 1)
    ap, bp = before["parts"], after["parts"]
    added = sorted(bp.keys() - ap.keys())
    removed = sorted(ap.keys() - bp.keys())
    changed = sorted(k for k in ap.keys() & bp.keys() if ap[k] != bp[k])
    fields = _compare_fields(before["fields"], after["fields"])
    raw_equal = before["relationships"]["raw"] == after["relationships"]["raw"]
    id_free_equal = before["relationships"]["without_ids"] == after["relationships"]["without_ids"]
    styles = before["styles"]["definitions"]
    styles_after = after["styles"]["definitions"]
    return {
        "expected_visible_text_only": expected,
        "paragraph_count": [before["paragraph_count"], after["paragraph_count"]],
        "run_count_changes": sum(x != y for x, y in zip(before["run_counts"], after["run_counts"]))
                             + abs(len(before["run_counts"]) - len(after["run_counts"])),
        "break_attributes_changed": before["break_attributes"] != after["break_attributes"],
        "media_unchanged": before["media"] == after["media"],
        "note_content_and_references_unchanged": before["notes"] == after["notes"],
        "notes_comparison_requires_semantic_review": before["notes"] != after["notes"],
        "field_analysis": fields,
        "opc_added": added, "opc_removed": removed, "opc_modified": changed,
        "opc_differences_have_equivalence_proven": False if added or removed or changed else True,
        "relationships_raw_equal": raw_equal,
        "relationships_ignoring_id_equal_diagnostic_only": id_free_equal,
        "style_definition_counts": [len(styles), len(styles_after)],
        "style_ids_added": len(styles_after.keys() - styles.keys()),
        "style_ids_removed": len(styles.keys() - styles_after.keys()),
        "style_definitions_changed_with_same_id": sum(styles[k] != styles_after[k]
                                                       for k in styles.keys() & styles_after.keys()),
    }
