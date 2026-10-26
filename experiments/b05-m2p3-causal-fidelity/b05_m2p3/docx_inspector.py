"""Read-only DOCX/OOXML comparison for B05-M2P3.

Checks are intentionally conservative. Differences are not declared equivalent
by normalization, and effective formatting is not inferred from run XML alone.
"""
from __future__ import annotations

import hashlib
import re
import xml.etree.ElementTree as ET
import zipfile
from pathlib import Path

W = "{http://schemas.openxmlformats.org/wordprocessingml/2006/main}"
PKG_REL = "{http://schemas.openxmlformats.org/package/2006/relationships}"
MAX_UNCOMPRESSED_BYTES = 200 * 1024 * 1024


def sha256_file(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(block)
    return digest.hexdigest()


def _digest(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def _xml(data: bytes, part: str) -> ET.Element:
    try:
        return ET.fromstring(data)
    except ET.ParseError as exc:
        raise ValueError(f"XML malformado em parte OOXML: {part}") from exc


def _paragraph_text(para: ET.Element) -> str:
    pieces = []
    for node in para.iter():
        if node.tag == W + "t":
            pieces.append(node.text or "")
        elif node.tag == W + "tab":
            pieces.append("\t")
        elif node.tag == W + "br":
            pieces.append("\f" if node.attrib.get(W + "type") == "page" else "\n")
    return "".join(pieces)


def _run_format(r: ET.Element) -> dict:
    props = r.find(W + "rPr")
    if props is None:
        return {}
    result = {}
    for element in props.iter():
        if element is props:
            continue
        key = element.tag.replace(W, "w:")
        if len(element):
            continue
        result.setdefault(key, []).append({
            k.replace(W, "w:"): v for k, v in sorted(element.attrib.items())
        } | ({"text": element.text} if element.text and element.text.strip() else {}))
    return result


def _paragraphs(root: ET.Element) -> list[dict]:
    body = root.find(W + "body")
    if body is None:
        raise ValueError("word/document.xml sem w:body")
    result = []
    for para in body.iter(W + "p"):
        runs = []
        breaks = []
        for run in para.iter(W + "r"):
            runs.append({"text": _paragraph_text(run), "format": _run_format(run)})
            for br in run.iter(W + "br"):
                breaks.append({k.replace(W, "w:"): v for k, v in sorted(br.attrib.items())})
        ppr = para.find(W + "pPr")
        result.append({
            "text": _paragraph_text(para),
            "runs": runs,
            "breaks": breaks,
            "paragraph_properties": ET.tostring(ppr, encoding="unicode") if ppr is not None else "",
        })
    return result


def _fields(data_by_part: dict[str, bytes]) -> list[dict]:
    results = []
    for part in sorted(data_by_part):
        if not (part.startswith("word/") and part.endswith(".xml")):
            continue
        if not re.search(r"/(document|header\d+|footer\d+|footnotes|endnotes)\.xml$", part):
            continue
        root = _xml(data_by_part[part], part)
        for node in root.iter():
            if node.tag == W + "fldSimple":
                value = node.attrib.get(W + "instr", "")
                results.append({"part": part, "instruction_sha256": _digest(value.encode()),
                                "instruction_length": len(value),
                                "field": value.strip().split(" ")[0].upper() if value.strip() else ""})
        # Field instructions are built by per-paragraph XML order, including
        # split w:instrText nodes. This is an inventory, not full Word semantics.
        for para in root.iter(W + "p"):
            stack = []
            for node in para.iter():
                if node.tag == W + "fldChar":
                    kind = node.attrib.get(W + "fldCharType")
                    if kind == "begin":
                        stack.append([])
                    elif kind == "end" and stack:
                        parts = stack.pop()
                        instruction = "".join(parts)
                        if instruction:
                            results.append({"part": part, "instruction_sha256": _digest(instruction.encode()),
                                            "instruction_length": len(instruction),
                                            "field": instruction.strip().split(" ")[0].upper()
                                            if instruction.strip() else ""})
                elif node.tag == W + "instrText" and stack:
                    stack[-1].append(node.text or "")
    return sorted(results, key=lambda x: (x["part"], x["field"], x["instruction_sha256"]))


def _notes(data_by_part: dict[str, bytes]) -> dict:
    out = {}
    for kind in ("footnote", "endnote"):
        part = f"word/{kind}s.xml"
        entries = []
        if part in data_by_part:
            root = _xml(data_by_part[part], part)
            entries = [{"id": n.attrib.get(W + "id"), "type": n.attrib.get(W + "type"),
                        "text_digest": _digest("\n".join(_paragraph_text(p) for p in n.iter(W + "p")).encode())}
                       for n in root.iter(W + kind)]
        out[kind] = entries
    root = _xml(data_by_part["word/document.xml"], "word/document.xml")
    for kind in ("footnote", "endnote"):
        out[kind + "_references"] = [n.attrib.get(W + "id") for n in root.iter(W + kind + "Reference")]
    return out


def _relationships(data_by_part: dict[str, bytes]) -> dict:
    rels = {}
    for part in sorted(data_by_part):
        if part.endswith(".rels"):
            root = _xml(data_by_part[part], part)
            rels[part] = sorted([tuple(sorted(n.attrib.items())) for n in root.iter(PKG_REL + "Relationship")])
    return rels


def inspect_docx(path: Path) -> dict:
    if not zipfile.is_zipfile(path):
        raise ValueError("Arquivo de entrada não é um contêiner ZIP/DOCX válido")
    with zipfile.ZipFile(path) as zf:
        entries = zf.infolist()
        if sum(e.file_size for e in entries) > MAX_UNCOMPRESSED_BYTES:
            raise ValueError("DOCX ultrapassa o limite de análise de 200 MiB descompactados")
        names = [e.filename for e in entries]
        if len(names) != len(set(names)):
            raise ValueError("DOCX contém entradas ZIP duplicadas")
        if "word/document.xml" not in names or "[Content_Types].xml" not in names:
            raise ValueError("Partes OOXML obrigatórias ausentes")
        if zf.testzip() is not None:
            raise ValueError("Falha de integridade CRC no pacote DOCX")
        content = {part: zf.read(part) for part in names if not part.endswith("/")}
    doc = _xml(content["word/document.xml"], "word/document.xml")
    return {
        "hash": sha256_file(path),
        "parts": {part: _digest(payload) for part, payload in sorted(content.items())},
        "paragraphs": _paragraphs(doc),
        "fields": _fields(content),
        "notes": _notes(content),
        "relationships": _relationships(content),
        "media": {part: _digest(payload) for part, payload in sorted(content.items())
                  if part.startswith("word/media/")},
    }


def _issue(issues: list, code: str, **details) -> None:
    issues.append({"code": code, **details})


def compare_docx(source: dict, candidate: dict, expected_edit: dict | None) -> dict:
    """Compare inventories without disclosing user document text."""
    issues = []
    a, b = source["paragraphs"], candidate["paragraphs"]
    source_text = "\n".join(p["text"] for p in a)
    candidate_text = "\n".join(p["text"] for p in b)
    expected_match = False
    if expected_edit is None:
        expected_match = source_text == candidate_text
    else:
        before, after = expected_edit["before"], expected_edit["after"]
        expected_match = (bool(before) and before != after and source_text.count(before) == 1
                          and candidate_text == source_text.replace(before, after, 1))
    if not expected_match:
        _issue(issues, "UNEXPECTED_VISIBLE_TEXT_CHANGE")
    if len(a) != len(b):
        _issue(issues, "PARAGRAPH_COUNT_CHANGED", before=len(a), after=len(b))
    for idx, (left, right) in enumerate(zip(a, b)):
        if left["breaks"] != right["breaks"]:
            _issue(issues, "BREAK_ATTR_CHANGED", paragraph=idx,
                   before=left["breaks"], after=right["breaks"])
        if left["paragraph_properties"] != right["paragraph_properties"]:
            _issue(issues, "PARAGRAPH_PROPERTIES_CHANGED", paragraph=idx)
        lr, rr = left["runs"], right["runs"]
        if len(lr) != len(rr):
            _issue(issues, "RUN_STRUCTURE_CHANGED", paragraph=idx, before=len(lr), after=len(rr))
        else:
            for j, (lrun, rrun) in enumerate(zip(lr, rr)):
                if lrun["format"] != rrun["format"]:
                    _issue(issues, "RUN_FORMAT_CHANGED", paragraph=idx, run=j,
                           before=lrun["format"], after=rrun["format"])
    ap, bp = source["parts"], candidate["parts"]
    added, removed = sorted(bp.keys() - ap.keys()), sorted(ap.keys() - bp.keys())
    modified = sorted(k for k in ap.keys() & bp.keys() if ap[k] != bp[k])
    if added or removed:
        _issue(issues, "OOXML_PARTS_CHANGED", added=added, removed=removed)
    if modified:
        _issue(issues, "OOXML_PART_CONTENT_CHANGED", parts=modified)
    if source["media"] != candidate["media"]:
        _issue(issues, "MEDIA_CHANGED")
    if source["notes"] != candidate["notes"]:
        _issue(issues, "NOTE_CONTENT_OR_REFS_CHANGED")
    if source["relationships"] != candidate["relationships"]:
        _issue(issues, "RELATIONSHIP_CHANGED")
    if source["fields"] != candidate["fields"]:
        _issue(issues, "FIELD_INSTRUCTION_CHANGED",
               field_names_before=[f["field"] for f in source["fields"]],
               field_names_after=[f["field"] for f in candidate["fields"]],
               # No private instructions or instruction hashes in shareable output.
               instructions_differ=True)
    return {
        "expected_text_only_edit_confirmed": expected_match,
        "paragraph_counts": [len(a), len(b)],
        "part_counts": [len(ap), len(bp)],
        "parts_added": added, "parts_removed": removed, "parts_modified": modified,
        "field_counts": [len(source["fields"]), len(candidate["fields"])],
        "issues": issues,
    }
