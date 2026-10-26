"""OPC/OOXML diagnostics; never pronounce semantic equality from ID normalization."""
from __future__ import annotations

import hashlib
import posixpath
import zipfile
from collections import Counter
from difflib import SequenceMatcher
from pathlib import Path
from urllib.parse import unquote
from xml.etree import ElementTree as ET

W = "{http://schemas.openxmlformats.org/wordprocessingml/2006/main}"
R = "{http://schemas.openxmlformats.org/officeDocument/2006/relationships}"
PKG = "{http://schemas.openxmlformats.org/package/2006/relationships}"
CT = "{http://schemas.openxmlformats.org/package/2006/content-types}"
MAX_PARTS = 5000
MAX_RAW_BYTES = 200 * 1024 * 1024
MAX_PARAGRAPH_CHARS = 400000


def _sha(value: bytes) -> str:
    return hashlib.sha256(value).hexdigest()


def _xml(data: bytes, name: str) -> ET.Element:
    # Strict on purpose. No embedded entity declarations or DTDs from documents.
    if b"<!DOCTYPE" in data.upper() or b"<!ENTITY" in data.upper():
        raise ValueError("DTD/ENTITY recusado na parte OOXML")
    try:
        return ET.fromstring(data)
    except ET.ParseError as exc:
        raise ValueError("XML OOXML inválido") from exc


def _part_owner(relpath: str) -> str:
    if relpath == "_rels/.rels":
        return ""
    folder, basename = posixpath.split(relpath)
    if not folder.endswith("/_rels") or not basename.endswith(".rels"):
        raise ValueError("Localização .rels inválida")
    return posixpath.join(folder[:-len("/_rels")], basename[:-len(".rels")])


def _destination(owner: str, target: str) -> str | None:
    target = unquote(target).replace("\\", "/")
    if not target or "?" in target or "#" in target or "\x00" in target:
        return None
    if target.startswith("/"):
        target = target.lstrip("/")
    else:
        target = posixpath.join(posixpath.dirname(owner), target)
    candidate = posixpath.normpath(target)
    if candidate == ".." or candidate.startswith("../") or candidate.startswith("/"):
        return None
    return candidate


def _files(path: Path) -> dict[str, bytes]:
    if not zipfile.is_zipfile(path):
        raise ValueError("Não é DOCX/ZIP")
    with zipfile.ZipFile(path) as package:
        infos = package.infolist()
        if len(infos) > MAX_PARTS or sum(i.file_size for i in infos) > MAX_RAW_BYTES:
            raise ValueError("DOCX fora de limites")
        names = [i.filename for i in infos if not i.is_dir()]
        if len(names) != len(set(names)) or any(n.startswith("/") or "\\" in n or
                                                   posixpath.normpath(n) != n or n.startswith("../") for n in names):
            raise ValueError("Nomes inseguros/duplicados no ZIP")
        if "word/document.xml" not in names or "[Content_Types].xml" not in names:
            raise ValueError("DOCX sem partes essenciais")
        if package.testzip() is not None:
            raise ValueError("DOCX com CRC inválido")
        return {name: package.read(name) for name in names}


def _opc(parts: dict[str, bytes]) -> dict:
    content = _xml(parts["[Content_Types].xml"], "[Content_Types].xml")
    defaults = {n.get("Extension") for n in content.findall(CT + "Default")}
    overrides = {n.get("PartName", "").lstrip("/") for n in content.findall(CT + "Override")}
    no_type = []
    for name in parts:
        if name == "[Content_Types].xml":
            continue
        extension = name.rsplit(".", 1)[-1] if "." in name else ""
        if name not in overrides and extension not in defaults:
            no_type.append(name)
    rels: dict[str, set[str]] = {}
    dangling = []
    external = 0
    owner_missing = []
    for relpath, raw in sorted(parts.items()):
        if not relpath.endswith(".rels"):
            continue
        owner = _part_owner(relpath)
        if owner and owner not in parts:
            owner_missing.append(relpath)
        root = _xml(raw, relpath)
        ids: set[str] = set()
        for elt in root.findall(PKG + "Relationship"):
            identity = elt.get("Id", "")
            if not identity or identity in ids:
                dangling.append("duplicate_or_missing_relationship_id")
            ids.add(identity)
            target = elt.get("Target", "")
            if elt.get("TargetMode") == "External":
                external += 1
                continue
            resolved = _destination(owner, target)
            if resolved is None or resolved not in parts:
                dangling.append("unresolved_internal_target")
        rels[owner] = ids
    # XML reference attributes must point to relationships belonging to the exact part.
    missing_ref = []
    for name, raw in parts.items():
        if not name.startswith("word/") or not name.endswith(".xml"):
            continue
        root = _xml(raw, name)
        for node in root.iter():
            for key in (R + "id", R + "embed", R + "link"):
                relationship = node.get(key)
                if relationship and relationship not in rels.get(name, set()):
                    missing_ref.append("dangling_xml_relationship_reference")
    return {"missing_content_types": len(no_type), "broken_relations": len(dangling),
            "missing_owners": len(owner_missing), "missing_xml_references": len(missing_ref),
            "external_relations_declared": external,
            "diagnostics": dict(Counter(dangling + missing_ref)),
            "part_count": len(parts)}


def _node_sig(node: ET.Element | None) -> str:
    return _sha(ET.tostring(node, encoding="utf-8")) if node is not None else "NONE"


def _paragraph_data(parts: dict[str, bytes]) -> list[dict]:
    root = _xml(parts["word/document.xml"], "word/document.xml")
    body = root.find(W + "body")
    if body is None:
        raise ValueError("DOCX sem corpo")
    values = []
    for p in body.iter(W + "p"):
        prefix = _node_sig(p.find(W + "pPr"))
        fragments = []
        for run in p.iter(W + "r"):
            rp = _node_sig(run.find(W + "rPr"))
            for n in run.iter():
                if n.tag == W + "t":
                    fragments.append((n.text or "", rp))
                elif n.tag == W + "tab":
                    fragments.append(("\t", rp))
                elif n.tag == W + "br":
                    fragments.append(("\f" if n.get(W + "type") == "page" else "\n", rp))
        text = "".join(fragment for fragment, _ in fragments)
        if len(text) > MAX_PARAGRAPH_CHARS:
            raise ValueError("Parágrafo excede limite da comparação de estilo")
        # Note: the signature encodes *explicit* formatting and paragraph properties, not inherited render style.
        trace = [prefix + ":" + rp for fragment, rp in fragments for _ in fragment]
        values.append({"text": text, "trace": trace})
    return values


def _fields(parts: dict[str, bytes]) -> list[tuple[str, str, str]]:
    result = []
    for name, raw in sorted(parts.items()):
        if not name.startswith("word/") or not name.endswith(".xml"):
            continue
        if not any(part in name for part in ("document.xml", "header", "footer", "footnotes", "endnotes")):
            continue
        for paragraph in _xml(raw, name).iter(W + "p"):
            frames = []
            for elt in paragraph.iter():
                if elt.tag == W + "fldSimple":
                    instruction = elt.get(W + "instr", "")
                    result.append((name, _sha(instruction.encode()), "simple"))
                elif elt.tag == W + "fldChar":
                    action = elt.get(W + "fldCharType")
                    if action == "begin":
                        frames.append([])
                    elif action == "end" and frames:
                        instruction = "".join(frames.pop())
                        result.append((name, _sha(instruction.encode()), "complex"))
                elif elt.tag == W + "instrText" and frames:
                    frames[-1].append(elt.text or "")
    return result


def _notes(parts: dict[str, bytes]) -> dict:
    notes = {}
    for typ in ("footnote", "endnote"):
        path = f"word/{typ}s.xml"
        by_id = {}
        if path in parts:
            tree = _xml(parts[path], path)
            for note in tree.iter(W + typ):
                if note.get(W + "type") in {"separator", "continuationSeparator", "continuationNotice"}:
                    continue
                text = "".join(n.text or "" for n in note.iter(W + "t"))
                by_id[note.get(W + "id")] = _sha(text.encode())
        refs = []
        for part, raw in parts.items():
            if not part.startswith("word/") or not part.endswith(".xml") or part == path:
                continue
            for node in _xml(raw, part).iter(W + typ + "Reference"):
                refs.append(by_id.get(node.get(W + "id")))
        notes[typ] = {"reference_count": len(refs), "unresolved": refs.count(None),
                      "reference_text_signatures": sorted(x for x in refs if x is not None)}
    return notes


def _styles(parts: dict[str, bytes]) -> dict:
    if "word/styles.xml" not in parts:
        return {}
    root = _xml(parts["word/styles.xml"], "word/styles.xml")
    return {n.get(W + "styleId", ""): _node_sig(n) for n in root.iter(W + "style")}


def _features(parts: dict[str, bytes]) -> list[str]:
    doc = _xml(parts["word/document.xml"], "word/document.xml")
    tags = {n.tag for n in doc.iter()}
    features = set()
    if W + "tbl" in tags: features.add("tables")
    if any(x.startswith("word/media/") for x in parts): features.add("images")
    if W + "footnoteReference" in tags: features.add("footnotes")
    if W + "endnoteReference" in tags: features.add("endnotes")
    if any(n.tag == W + "br" and n.get(W + "type") == "page" for n in doc.iter()): features.add("page_breaks")
    if W + "fldSimple" in tags or W + "instrText" in tags or any(
        b"<w:instrText" in x for k, x in parts.items() if k.startswith("word/header") or k.startswith("word/footer")
    ): features.add("fields")
    if any(x.startswith("word/header") or x.startswith("word/footer") for x in parts): features.add("headers_footers")
    if W + "numPr" in tags: features.add("numbered_lists")
    if W + "sectPr" in tags: features.add("section_breaks")
    if "word/styles.xml" in parts: features.add("styles")
    if W + "hyperlink" in tags: features.add("hyperlinks")
    if W + "commentReference" in tags: features.add("comments")
    if W + "ins" in tags or W + "del" in tags: features.add("tracked_changes")
    return sorted(features)


def inspect_docx(path: Path) -> dict:
    parts = _files(path)
    return {"parts": {key: _sha(data) for key, data in parts.items()},
            "opc": _opc(parts), "paragraphs": _paragraph_data(parts),
            "notes": _notes(parts), "styles": _styles(parts),
            "fields": _fields(parts), "features": _features(parts)}


def compare_docs(source: dict, candidate: dict, expected_edit: dict | None = None) -> dict:
    a, b = source["paragraphs"], candidate["paragraphs"]
    equal_count = len(a) == len(b)
    original_text = "\n".join(p["text"] for p in a)
    candidate_text = "\n".join(p["text"] for p in b)
    if expected_edit is None:
        expected_visible_text_only = original_text == candidate_text
    else:
        expected_visible_text_only = (original_text.count(expected_edit["before"]) == 1
                                      and candidate_text == original_text.replace(
                                          expected_edit["before"], expected_edit["after"], 1))
    # Exact aligned paragraphs only. Match same visible characters via sequence matching,
    # but do not infer effective style inheritance or equivalence from the matched blocks.
    mismatched_format_chars = 0
    matched_chars = 0
    for left, right in zip(a, b):
        matcher = SequenceMatcher(None, left["text"], right["text"], autojunk=False)
        for i, j, size in matcher.get_matching_blocks():
            matched_chars += size
            mismatched_format_chars += sum(x != y for x, y in zip(
                left["trace"][i:i+size], right["trace"][j:j+size]))
    sa, sb = source["styles"], candidate["styles"]
    pa, pb = source["parts"], candidate["parts"]
    added = sorted(pb.keys() - pa.keys())
    removed = sorted(pa.keys() - pb.keys())
    changed = sorted(k for k in pa.keys() & pb.keys() if pa[k] != pb[k])
    notes_equal = source["notes"] == candidate["notes"]
    return {
        "paragraph_counts": [len(a), len(b)], "paragraphs_positionally_aligned": equal_count,
        "expected_visible_text_only": expected_visible_text_only,
        "matching_text_characters_compared": matched_chars,
        "explicit_format_mismatch_chars_on_matched_text": mismatched_format_chars,
        "effective_style_equivalence_proven": False,
        "parts_added": added, "parts_removed": removed, "parts_modified": changed,
        "opcs_before_after": [source["opc"], candidate["opc"]],
        "candidate_new_broken_relationships": max(0, candidate["opc"]["broken_relations"] - source["opc"]["broken_relations"])
            + max(0, candidate["opc"]["missing_xml_references"] - source["opc"]["missing_xml_references"])
            # Even an empty .rels part must not hide an absent source part.
            # This is a structural review signal, not proof of functional loss.
            + max(0, candidate["opc"]["missing_owners"] - source["opc"]["missing_owners"]),
        "candidate_new_missing_content_types": max(0, candidate["opc"]["missing_content_types"] - source["opc"]["missing_content_types"]),
        "notes_referenced_text_signatures_equal": notes_equal,
        "notes_semantic_equivalence_proven": False,
        "field_instruction_occurrences": [len(source["fields"]), len(candidate["fields"])],
        "field_instructions_equal_in_order": source["fields"] == candidate["fields"],
        "field_effective_results_equivalence_proven": False,
        "styles_ids_added": len(sb.keys() - sa.keys()),
        "styles_ids_removed": len(sa.keys() - sb.keys()),
        "style_definitions_changed_same_id": sum(sa[k] != sb[k] for k in sa.keys() & sb.keys()),
        "style_equivalence_proven": sa == sb,
        "noted_limit": "Explicit run/paragraph properties, not computed Word font; sequence matching can be ambiguous; all OPC/field/note transforms remain unapproved."
    }
