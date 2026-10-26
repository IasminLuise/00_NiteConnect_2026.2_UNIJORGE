"""Opt-in experimental representation adapter. Does not repair REF or change any gate."""
from __future__ import annotations

import argparse
from copy import deepcopy
from io import BytesIO
import json
from pathlib import Path
import re
from zipfile import BadZipFile, ZipFile
from lxml import etree as ET

W = 'http://schemas.openxmlformats.org/wordprocessingml/2006/main'
XML = 'http://www.w3.org/XML/1998/namespace'
LIMIT = 8 * 1024 * 1024
REQUIRED = {'[Content_Types].xml', '_rels/.rels', 'word/document.xml'}


def _xml(data: bytes):
    parser = ET.XMLParser(resolve_entities=False, no_network=True, load_dtd=False,
                          recover=False, huge_tree=False, remove_blank_text=False)
    root = ET.fromstring(data, parser)
    if root.getroottree().docinfo.doctype:
        raise ValueError('DTD unsupported')
    return root


def _run(kind: str, props=None, instruction=None):
    run = ET.Element(f'{{{W}}}r')
    if props is not None: run.append(deepcopy(props))
    if kind == 'instruction':
        node = ET.SubElement(run, f'{{{W}}}instrText')
        node.set(f'{{{XML}}}space', 'preserve'); node.text = instruction
    else:
        node = ET.SubElement(run, f'{{{W}}}fldChar')
        node.set(f'{{{W}}}fldCharType', kind)
    return run


def convert_savedate(data: bytes) -> tuple[bytes, list[dict]]:
    """Convert only original simple SAVEDATE fields; retain instruction and cached runs.

    Unknown attributes, nesting, active content and unsafe archives fail closed.
    Existing complex fields, REF, DATE and all unrelated ZIP parts stay untouched.
    This is not validation for publication and never accepts a gate rejection.
    """
    if not data or len(data) > LIMIT: raise ValueError('DOCX exceeds 8 MiB or is empty')
    try:
        with ZipFile(BytesIO(data)) as archive:
            infos = archive.infolist(); names = [i.filename for i in infos]
            if len(names) > 1024 or len(names) != len(set(names)) or not REQUIRED.issubset(names):
                raise ValueError('Invalid DOCX entries')
            total = 0; contents = {}; roots = {}
            for info in infos:
                name = info.filename; lower = name.lower()
                if (not name or name.startswith('/') or '\\' in name or ':' in name
                        or any(p in ('.', '..') for p in name.split('/')) or info.flag_bits & 1
                        or lower.startswith('_xmlsignatures/') or 'vbaproject' in lower):
                    raise ValueError('Signed, active or unsafe package unsupported')
                total += info.file_size
                if info.file_size > LIMIT or total > 4 * LIMIT: raise ValueError('Expanded package exceeds limit')
                raw = archive.read(info)
                if len(raw) != info.file_size: raise ValueError('Entry length mismatch')
                contents[name] = raw
                if not info.is_dir() and (lower.endswith('.xml') or lower.endswith('.rels')):
                    root = _xml(raw); roots[name] = root
                    if lower.endswith('.rels'):
                        for rel in root:
                            target = rel.get('Target', '')
                            if (rel.get('TargetMode', '').lower() == 'external'
                                    or ':' in target or target.startswith(('/', '\\'))):
                                raise ValueError('External relationships unsupported in this bench')
            changes = []
            for name, root in roots.items():
                if not name.startswith('word/') or not name.endswith('.xml'): continue
                depth = 0; field_depths = {}; invalid_scope = False
                for element in root.iter():
                    if element.tag == f'{{{W}}}fldChar':
                        kind = element.get(f'{{{W}}}fldCharType')
                        if kind == 'begin': depth += 1
                        elif kind == 'end':
                            if depth == 0: invalid_scope = True
                            else: depth -= 1
                        elif kind != 'separate' or depth == 0: invalid_scope = True
                    elif element.tag == f'{{{W}}}fldSimple': field_depths[element] = depth
                invalid_scope = invalid_scope or depth != 0
                changed = False
                for node in list(root.iter(f'{{{W}}}fldSimple')):
                    instruction = node.get(f'{{{W}}}instr', '')
                    if not re.match(r'^\s*SAVEDATE(?:\s|$)', instruction, re.IGNORECASE): continue
                    if len(instruction) > 16384: raise ValueError('Field instruction exceeds limit')
                    allowed = {f'{{{W}}}{a}' for a in ('instr', 'fldLock', 'dirty')}
                    if set(node.attrib) - allowed: raise ValueError('Unknown simple-field attributes')
                    if (invalid_scope or field_depths[node] != 0
                            or node.getparent().tag != f'{{{W}}}p'
                            or any(a.tag == f'{{{W}}}fldSimple' for a in node.iterancestors())
                            or any(c.tag in {f'{{{W}}}fldSimple', f'{{{W}}}fldChar', f'{{{W}}}instrText'} for c in node.iterdescendants())
                            or (node.text and node.text.strip())):
                        raise ValueError('Nested or unsupported SAVEDATE structure')
                    props = node.find(f'{{{W}}}r/{{{W}}}rPr')
                    begin = _run('begin', props)
                    for attribute in ('dirty', 'fldLock'):
                        value = node.get(f'{{{W}}}{attribute}')
                        if value is not None: begin[-1].set(f'{{{W}}}{attribute}', value)
                    replacement = [begin, _run('instruction', props, instruction), _run('separate', props),
                                   *(deepcopy(child) for child in node), _run('end', props)]
                    replacement[-1].tail = node.tail
                    parent = node.getparent(); index = parent.index(node); parent.remove(node)
                    for offset, child in enumerate(replacement): parent.insert(index + offset, child)
                    changes.append({'part': name, 'instruction': instruction, 'cached_result_preserved': True})
                    changed = True
                if changed:
                    raw = contents[name]; declaration = bool(re.match(br'\s*<\?xml\s', raw))
                    standalone = re.search(br'<\?xml[^?]*standalone=[\'"](yes|no)[\'"]', raw)
                    options = {'standalone': standalone.group(1) == b'yes'} if standalone else {}
                    contents[name] = ET.tostring(root, encoding='UTF-8', xml_declaration=declaration, **options)
            if not changes: return data, []
            output = BytesIO()
            with ZipFile(output, 'w') as result:
                result.comment = archive.comment
                for info in infos: result.writestr(info, contents[info.filename])
            fixed = output.getvalue()
            if len(fixed) > LIMIT: raise ValueError('Converted DOCX exceeds 8 MiB')
            return fixed, changes
    except (BadZipFile, ET.XMLSyntaxError, RuntimeError, NotImplementedError) as error:
        raise ValueError('Invalid or unsupported DOCX') from error


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--source', type=Path, required=True)
    parser.add_argument('--out', type=Path, required=True)
    args = parser.parse_args()
    source, output = args.source.resolve(strict=True), args.out.resolve()
    if '.private' not in source.parts or '.private' not in output.parts or source == output:
        raise ValueError('Use distinct input/output beneath .private')
    if source.suffix.lower() != '.docx' or output.suffix.lower() != '.docx' or output.exists():
        raise ValueError('Use a new DOCX output; never overwrite')
    if source.stat().st_size > LIMIT: raise ValueError('Source exceeds 8 MiB')
    fixed, changes = convert_savedate(source.read_bytes())
    output.parent.mkdir(parents=True, exist_ok=True)
    with output.open('xb') as stream: stream.write(fixed)
    print(json.dumps({'converted_fields': len(changes), 'gate_modified': False, 'experimental_only': True}))


if __name__ == '__main__': main()
