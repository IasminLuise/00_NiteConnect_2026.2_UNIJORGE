"""B05.0.2 explicit pre-editor transformation; never an output repair.

Only exact hash/version and ordered field declarations authorize a copy.
The supported cache consists of plain text runs with optional run properties.
All output/manifests containing real models belong in the ignored lab runtime.
"""
import copy
import hashlib
import io
import posixpath
import zipfile
from urllib.parse import urlsplit, unquote
from xml.dom import minidom, Node
import xml.etree.ElementTree as ET

from compatibility import inventory, normalize_instruction, canonical
from fidelity import W

VERSION = 'b0502-savedate-1'
WNS = W[1:-1]
RNS = 'http://schemas.openxmlformats.org/package/2006/relationships'
CTNS = 'http://schemas.openxmlformats.org/package/2006/content-types'
MAIN = 'application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml'


def digest(data):
    return hashlib.sha256(data).hexdigest()


def checked_package(data):
    """Bounded existing inventory plus explicit conservative OPC policy."""
    if len(data) > 8 * 1024 * 1024:
        raise ValueError('Package exceeds host byte limit')
    try:
        inv = inventory(data)  # Limits, CRC reads, duplicate parts and safe XML.
        with zipfile.ZipFile(io.BytesIO(data)) as z:
            parts = {entry.filename: z.read(entry) for entry in z.infolist()}
        if not {'[Content_Types].xml', '_rels/.rels', 'word/document.xml'} <= parts.keys():
            raise ValueError('Required OPC parts absent')
        for name in parts:
            if (name.startswith('/') or '\\' in name or ':' in name or
                    any(p in ('', '.', '..') for p in name.split('/'))):
                raise ValueError('Unsafe OPC name')
            lower = name.lower()
            if any(x in lower for x in ('vba', 'embeddings/', 'activex/', '_xmlsignatures/')) or lower.endswith('.bin'):
                raise ValueError('Active/signed package unsupported')
        types = ET.fromstring(parts['[Content_Types].xml'])
        if types.tag != '{' + CTNS + '}Types':
            raise ValueError('Unknown content types namespace')
        if not any(n.get('PartName') == '/word/document.xml' and n.get('ContentType') == MAIN for n in types):
            raise ValueError('Not DOCX main content type')
        type_keys = set()
        for n in types:
            if n.tag not in ('{' + CTNS + '}Override', '{' + CTNS + '}Default'):
                raise ValueError('Unknown content type declaration')
            key = (n.tag, n.get('PartName', n.get('Extension')))
            if key in type_keys:
                raise ValueError('Duplicate content type declaration')
            type_keys.add(key)
            if any(x in n.get('ContentType', '').lower() for x in ('macroenabled', 'activex', 'oleobject')):
                raise ValueError('Active content type')
            if n.get('PartName') and (not n.get('PartName').startswith('/') or n.get('PartName')[1:] not in parts):
                raise ValueError('Content type target absent')
        relationship_ids = {}
        has_main = False
        for name, blob in parts.items():
            if not name.endswith('.rels'):
                continue
            rels = ET.fromstring(blob)
            if rels.tag != '{' + RNS + '}Relationships':
                raise ValueError('Unknown relationships namespace')
            ids = set()
            for rel in rels:
                if rel.tag != '{' + RNS + '}Relationship' or rel.get('TargetMode', 'Internal') not in ('Internal', 'External'):
                    raise ValueError('Unknown relationship structure/mode')
                rid, target = rel.get('Id'), rel.get('Target', '')
                if not rid or rid in ids or not target:
                    raise ValueError('Ambiguous OPC relationship')
                ids.add(rid)
                if rel.get('TargetMode', 'Internal') == 'External':
                    uri = urlsplit(target)
                    if not (rel.get('Type') == 'http://schemas.openxmlformats.org/officeDocument/2006/relationships/hyperlink' and
                            uri.scheme == 'https' and uri.hostname and not uri.username and not uri.password):
                        raise ValueError('External relationship unsupported')
                else:
                    uri = urlsplit(target)
                    if uri.scheme or uri.netloc or '\\' in target:
                        raise ValueError('Invalid internal relationship')
                    base = '' if name == '_rels/.rels' else posixpath.dirname(posixpath.dirname(name))
                    resolved = posixpath.normpath(posixpath.join(base, unquote(uri.path).lstrip('/'))) if not uri.path.startswith('/') else unquote(uri.path)[1:]
                    if resolved not in parts:
                        raise ValueError('Relationship target absent')
                    if name == '_rels/.rels' and rel.get('Type') == 'http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument':
                        if resolved != 'word/document.xml' or has_main:
                            raise ValueError('Ambiguous main document relationship')
                        has_main = True
            source_part = '' if name == '_rels/.rels' else posixpath.join(posixpath.dirname(posixpath.dirname(name)), posixpath.basename(name)[:-5])
            if source_part and source_part not in parts:
                raise ValueError('Relationship source absent')
            relationship_ids[source_part] = ids
        if not has_main:
            raise ValueError('Main document relationship absent')
        reference_ns = '{http://schemas.openxmlformats.org/officeDocument/2006/relationships}'
        for name, blob in parts.items():
            if not name.endswith('.xml'):
                continue
            for element in ET.fromstring(blob).iter():
                for attr, value in element.attrib.items():
                    if attr.startswith(reference_ns) and value not in relationship_ids.get(name, set()):
                        raise ValueError('Dangling relationship reference')
        return parts, inv
    except (zipfile.BadZipFile, ET.ParseError, KeyError, RuntimeError) as error:
        raise ValueError('Invalid bounded DOCX package') from error


def _elements(node):
    result = []
    for child in node.childNodes:
        if child.nodeType == Node.ELEMENT_NODE:
            result.append(child)
        elif child.nodeType != Node.TEXT_NODE or child.data.strip():
            raise ValueError('Mixed/non-element field structure unsupported')
    return result


def _qualified(node, local):
    return node.namespaceURI == WNS and node.localName == local


def _check_simple(field):
    allowed = {'instr', 'dirty', 'fldLock'}
    for attr in field.attributes.values():
        if attr.namespaceURI != WNS or attr.localName not in allowed:
            raise ValueError('Unknown simple field attribute')
    lock = field.getAttributeNS(WNS, 'fldLock')
    dirty = field.getAttributeNS(WNS, 'dirty')
    if lock not in ('', 'false', '0', 'off') or dirty not in ('', 'true', 'false', '0', '1', 'on', 'off'):
        raise ValueError('Protected/unknown field flag')
    parent = field.parentNode
    if not _qualified(parent, 'p'):
        raise ValueError('Only direct paragraph fields supported')
    depth = 0
    for sibling in parent.childNodes:
        if sibling is field:
            break
        if sibling.nodeType == Node.ELEMENT_NODE:
            for marker in sibling.getElementsByTagNameNS(WNS, 'fldChar'):
                kind = marker.getAttributeNS(WNS, 'fldCharType')
                depth += 1 if kind == 'begin' else -1 if kind == 'end' else 0
    if depth != 0:
        raise ValueError('Nested complex/simple field unsupported')
    ancestor = parent
    while ancestor.nodeType == Node.ELEMENT_NODE:
        if ancestor.namespaceURI == 'http://schemas.openxmlformats.org/markup-compatibility/2006' or ancestor.localName in ('sdt', 'ins', 'del'):
            raise ValueError('Conditional/protected field container unsupported')
        ancestor = ancestor.parentNode
    runs = _elements(field)
    if not runs:
        raise ValueError('Explicit cached text required')
    for run in runs:
        if not _qualified(run, 'r'):
            raise ValueError('Only cached text runs supported')
        for attr in run.attributes.values():
            if attr.namespaceURI != WNS or attr.localName not in ('rsidR', 'rsidRPr', 'rsidDel'):
                raise ValueError('Unknown cache run attribute')
        children = _elements(run)
        if not children or any(not (_qualified(c, 't') or _qualified(c, 'rPr') and i == 0) for i, c in enumerate(children)):
            raise ValueError('Only text and initial run properties supported')
        for props in run.getElementsByTagNameNS(WNS, 'rPr'):
            if any(n.localName in ('fldChar', 'instrText', 'drawing', 'object', 'fldSimple') for n in props.getElementsByTagName('*')):
                raise ValueError('Active cache properties unsupported')
        for text in run.getElementsByTagNameNS(WNS, 't'):
            if any(c.nodeType != Node.TEXT_NODE for c in text.childNodes):
                raise ValueError('Nested cache content unsupported')
    return runs


def eligible(data, policy):
    if not policy.get('template_version') or digest(data) != policy.get('original_sha256'):
        raise ValueError('Unapproved model bytes/version')
    parts, inv = checked_package(data)
    if any(f['type'] == 'SAVEDATE' and not f['complete'] for f in inv['fields']):
        raise ValueError('Incomplete SAVEDATE')
    expected = policy.get('fields')
    found = []
    parsed = {}
    for name, blob in parts.items():
        if not name.endswith('.xml'):
            continue
        root = ET.fromstring(blob)
        if any(n.tag == W + 'documentProtection' and n.get(W + 'enforcement') not in ('0', 'false', 'off') for n in root.iter()):
            raise ValueError('Protected document unsupported')
        doc = minidom.parseString(blob)
        for field in doc.getElementsByTagNameNS(WNS, 'fldSimple'):
            instruction = field.getAttributeNS(WNS, 'instr')
            if normalize_instruction(instruction).split(' ', 1)[0:1] != ['SAVEDATE']:
                continue
            if not (name == 'word/document.xml' or name.startswith(('word/header', 'word/footer'))):
                raise ValueError('Unexpected SAVEDATE story')
            _check_simple(field)
            found.append({'part': name, 'instruction': instruction})
            parsed.setdefault(name, (doc, []))[1].append(field)
    if found != expected:
        raise ValueError('Exact ordered field declaration mismatch')
    return parts, inv, parsed


def manifest(data, output, policy, changes, count):
    return {'adapter_version': VERSION, 'template_version': policy['template_version'],
            'original_sha256': digest(data), 'working_sha256': digest(output),
            'transform_rules': ['eligible-fldSimple-SAVEDATE-to-complex'],
            'transformed_fields': count, 'changes': changes}


def normalize_copy(data, policy):
    parts, _, parsed = eligible(data, policy)
    changes, replacements, count = [], {}, 0
    for name, (doc, fields) in parsed.items():
        for field in fields:
            prefix = (field.prefix + ':') if field.prefix else ''
            def run(child_name, attr=None, value=None):
                node = doc.createElementNS(WNS, prefix + 'r')
                child = doc.createElementNS(WNS, prefix + child_name)
                if attr:
                    child.setAttributeNS(WNS, prefix + attr, value)
                node.appendChild(child)
                return node, child
            begin, char = run('fldChar', 'fldCharType', 'begin')
            for flag in ('fldLock', 'dirty'):
                if field.hasAttributeNS(WNS, flag):
                    char.setAttributeNS(WNS, prefix + flag, field.getAttributeNS(WNS, flag))
            instruction, text = run('instrText')
            text.setAttributeNS('http://www.w3.org/XML/1998/namespace', 'xml:space', 'preserve')
            text.appendChild(doc.createTextNode(field.getAttributeNS(WNS, 'instr')))
            sequence = [begin, instruction, run('fldChar', 'fldCharType', 'separate')[0]]
            sequence.extend(n.cloneNode(True) for n in _check_simple(field))
            sequence.append(run('fldChar', 'fldCharType', 'end')[0])
            for node in sequence:
                field.parentNode.insertBefore(node, field)
            field.parentNode.removeChild(field)
            count += 1
        blob = doc.toxml(encoding='utf-8')
        changes.append({'part': name, 'before_sha256': digest(parts[name]), 'after_sha256': digest(blob),
                        'before_canonical': canonical(ET.fromstring(parts[name])),
                        'after_canonical': canonical(ET.fromstring(blob))})
        replacements[name] = blob
    if not replacements:
        return data, manifest(data, data, policy, [], 0)
    out = io.BytesIO()
    with zipfile.ZipFile(io.BytesIO(data)) as source, zipfile.ZipFile(out, 'w') as target:
        target.comment = source.comment
        for entry in source.infolist():
            target.writestr(copy.copy(entry), replacements.get(entry.filename, parts[entry.filename]))
    output = out.getvalue()
    checked_package(output)
    return output, manifest(data, output, policy, changes, count)


def verify_idempotence(data, record):
    if digest(data) != record['working_sha256']:
        return False
    policy = {'original_sha256': digest(data), 'template_version': record['template_version'], 'fields': []}
    return normalize_copy(data, policy)[0] == data
