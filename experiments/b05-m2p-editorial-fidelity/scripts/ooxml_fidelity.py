#!/usr/bin/env python3
"""B05-M2P: local, fail-closed OOXML diagnostics for a QUARANTINED candidate.

NO automatic publication, document repair, normalization, or official approval.
Only share sanitized findings, never documents, text, source paths or hashes.
"""
from __future__ import annotations
import argparse
import json
from pathlib import Path
import zipfile
from collections import Counter
from dataclasses import dataclass
from lxml import etree as ET

W = '{http://schemas.openxmlformats.org/wordprocessingml/2006/main}'
R = '{http://schemas.openxmlformats.org/officeDocument/2006/relationships}'
PKG = '{http://schemas.openxmlformats.org/package/2006/relationships}'
C = '{http://schemas.openxmlformats.org/package/2006/content-types}'
MAIN = 'word/document.xml'
REQUIRED = {MAIN, '_rels/.rels', '[Content_Types].xml'}
MAX_ENTRY = 8*1024*1024
MAX_TOTAL = 32*1024*1024
MAX_FILES = 1024
MAX_INPUT = 8*1024*1024

class UnsafeInput(ValueError):
    pass

def private_path(path: Path):
    if '.private' not in path.resolve().parts:
        raise UnsafeInput('Input/output must remain in a .private directory')
    return path

def load_package(path: Path) -> dict[str, bytes]:
    private_path(path)
    if not path.is_file() or path.stat().st_size > MAX_INPUT:
        raise UnsafeInput('Missing or oversized DOCX')
    result = {}
    try:
        with zipfile.ZipFile(path) as z:
            infos = z.infolist()
            if len(infos) > MAX_FILES: raise UnsafeInput('Too many ZIP entries')
            total = 0
            for i in infos:
                name = i.filename
                if (not name or name in result or name.startswith('/') or '\\' in name or ':' in name
                    or any(seg in ('', '.', '..') for seg in name.rstrip('/').split('/'))
                    or 'vbaproject' in name.casefold()):
                    raise UnsafeInput('Unsafe OOXML part name')
                if i.is_dir(): continue
                if i.file_size > MAX_ENTRY or i.compress_size > MAX_ENTRY or i.flag_bits & 1:
                    raise UnsafeInput('Unsafe OOXML entry')
                total += i.file_size
                if total > MAX_TOTAL: raise UnsafeInput('Expanded OOXML too large')
                data=z.read(i)
                if len(data)!=i.file_size: raise UnsafeInput('Unexpected ZIP size')
                if name.lower().endswith(('.xml','.rels')) or name == '[Content_Types].xml':
                    safe_xml(data)
                result[name]=data
            if not REQUIRED.issubset(result): raise UnsafeInput('Required OOXML parts absent')
    except (zipfile.BadZipFile,RuntimeError,EOFError) as exc:
        raise UnsafeInput('Invalid DOCX container') from exc
    return result

def safe_xml(data: bytes):
    try:
        parser=ET.XMLParser(resolve_entities=False,no_network=True,load_dtd=False,huge_tree=False,remove_blank_text=False)
        tree=ET.fromstring(data,parser)
        if tree.getroottree().docinfo.doctype: raise UnsafeInput('DTD in OOXML')
        return tree
    except ET.XMLSyntaxError as exc:
        raise UnsafeInput('Invalid OOXML XML') from exc

def tree_key(root, allow_text=False):
    """Semantic tree signature; namespace URI/expanded tag, attrs, text and child order.
    Ignore w:t payload only, never ignore structural attributes, fields or comments.
    """
    def recurse(node):
        if not isinstance(node.tag,str):
            return ('non_element',str(node))
        text = '__TEXT_VALUE__' if allow_text and node.tag==W+'t' else node.text
        return (node.tag,tuple(sorted(node.attrib.items())),text,node.tail,tuple(recurse(c) for c in node))
    return recurse(root)

def is_word_xml(name):
    return name.startswith('word/') and name.endswith('.xml')

def parts_kind(parts):
    return {name: safe_xml(data) for name,data in parts.items() if name.endswith(('.xml','.rels')) or name == '[Content_Types].xml'}

def normalized_relationships(root):
    if root.tag != PKG+'Relationships': return None
    # Relationships are compared irrespective of rId assignment only as a diagnostic.
    # Changes in rId referenced by document.xml are separately detected by full tree compare.
    return tuple(sorted((e.get('Type'),e.get('Target'),e.get('TargetMode','Internal')) for e in root if e.tag==PKG+'Relationship'))

def normalize_content_types(root):
    if root.tag!=C+'Types': return None
    return tuple(sorted((e.tag,tuple(sorted(e.attrib.items()))) for e in root))

def relevant_sections(root):
    tags={W+x for x in ('sectPr','tblPr','trPr','tcPr','pPr','rPr','tblGrid','drawing','pict','object','ins','del','sdt')}
    return [tree_key(e) for e in root.iter() if e.tag in tags]

def fields(parts):
    # Field instructions and field boundaries across document/header/footer/notes.
    names=sorted(n for n in parts if is_word_xml(n) and (
        n in ('word/document.xml','word/footnotes.xml','word/endnotes.xml') or
        n.startswith('word/header') or n.startswith('word/footer')))
    result=[]
    for n in names:
        root=safe_xml(parts[n]); ins=[]
        for el in root.iter():
            if el.tag==W+'fldSimple': ins.append(('simple',el.get(W+'instr','')))
            elif el.tag==W+'instrText': ins.append(('complex',el.text or ''))
            elif el.tag==W+'fldChar': ins.append(('boundary',el.get(W+'fldCharType','')))
        result.append((n,tuple(ins)))
    return result

def paragraphs(parts):
    root=safe_xml(parts[MAIN]); body=root.find(W+'body')
    if body is None: raise UnsafeInput('Missing w:body')
    values=[]
    for p in body.iter(W+'p'):
        # all existing displayed w:t, not w:instrText; include explicit control tokens.
        text=[]
        for e in p.iter():
            if e.tag==W+'t':text.append(e.text or '')
            elif e.tag in (W+'br',W+'cr',W+'tab'):text.append('\x00'+e.tag.removeprefix(W)+':'+json.dumps(dict(e.attrib),sort_keys=True))
        values.append(''.join(text))
    return values

def expectation(before,after,expected):
    if expected is None: return {'provided':False,'exact':False,'changedParagraphs':None,'reason':'EXPECTED_CHANGE_NOT_SUPPLIED'}
    basic={'paragraphIndex','oldText','newText'}
    fragment={'paragraphIndex','oldFragment','newFragment'}
    if set(expected) not in (basic,fragment):
        raise UnsafeInput('Expected change: paragraphIndex + oldText/newText or oldFragment/newFragment')
    ix=expected['paragraphIndex']
    if not isinstance(ix,int) or isinstance(ix,bool) or ix<0:
        raise UnsafeInput('Invalid paragraph index')
    is_fragment=set(expected)==fragment
    old=expected['oldFragment' if is_fragment else 'oldText']
    new=expected['newFragment' if is_fragment else 'newText']
    if not isinstance(old,str) or not isinstance(new,str) or not old or old==new:
        raise UnsafeInput('Invalid expected edit')
    if len(before)!=len(after) or ix>=len(before):
        return {'provided':True,'exact':False,'changedParagraphs':None,'reason':'PARAGRAPH_COUNT_MISMATCH'}
    diff=[j for j,(a,b) in enumerate(zip(before,after)) if a!=b]
    if is_fragment:
        edited=before[ix].replace(old,new)
        valid=(before[ix].count(old)==1 and after[ix]==edited and diff==[ix])
    else: valid=(before[ix]==old and after[ix]==new and diff==[ix])
    return {'provided':True,'exact':valid,'changedParagraphs':len(diff),
            'reason':None if valid else 'UNEXPECTED_TEXT_CHANGE'}

def analyze(source: Path,candidate: Path,expect: dict|None=None):
    left=load_package(source);right=load_package(candidate)
    names_a=set(left);names_b=set(right)
    added=sorted(names_b-names_a);removed=sorted(names_a-names_b)
    changed=sorted(n for n in names_a & names_b if left[n]!=right[n]);common=sorted(names_a&names_b)
    before_par=paragraphs(left);after_par=paragraphs(right)
    edit=expectation(before_par,after_par,expect)
    structs_a=parts_kind(left);structs_b=parts_kind(right)
    relevant_a=relevant_sections(safe_xml(left[MAIN]));relevant_b=relevant_sections(safe_xml(right[MAIN]))
    field_equal=fields(left)==fields(right)
    structural_main_equal=tree_key(structs_a[MAIN],allow_text=True)==tree_key(structs_b[MAIN],allow_text=True)
    changed_styles=any((n in added or n in removed or n in changed) for n in ('word/styles.xml','word/stylesWithEffects.xml','word/numbering.xml','word/fontTable.xml','word/settings.xml'))
    protected_changed=sorted(n for n in changed if n!=MAIN and n!='docProps/core.xml')
    # Pure reserialization of other XML parts is not approved by this pilot.
    nontext_modification=bool(added or removed or protected_changed or not structural_main_equal)
    layout_signals={
        'fieldInstructionsEqual':field_equal,
        'mainStructureExceptTextEqual':structural_main_equal,
        'paragraphFormatAndTableGeometryEqual':relevant_a==relevant_b,
        'stylesOrSettingsModified':changed_styles,
        'contentTypesEqual':normalize_content_types(structs_a['[Content_Types].xml'])==normalize_content_types(structs_b['[Content_Types].xml']),
        'relationshipsChanged':any((n in added or n in removed or n in changed) for n in set(left)|set(right) if n.endswith('.rels')),
        'mediaChanged':any((n in added or n in removed or n in changed) for n in set(left)|set(right) if n.startswith(('word/media/','word/embeddings/'))),
    }
    blockers=[]
    if added or removed:blockers.append('PART_SET_CHANGED')
    if protected_changed:blockers.append('NON_MAIN_PART_CHANGED')
    if not structural_main_equal:blockers.append('MAIN_STRUCTURE_CHANGED')
    if not field_equal:blockers.append('FIELD_INSTRUCTIONS_CHANGED')
    if not layout_signals['paragraphFormatAndTableGeometryEqual']:blockers.append('LAYOUT_PROPERTIES_CHANGED')
    if not edit['exact']:blockers.append(edit['reason'] or 'TEXT_CHANGE_NOT_DEMONSTRATED')
    prequalified=not nontext_modification and field_equal and edit['exact']
    return {
        'schema':'b05-m2p-ooxml-v1', 'scope':'SYNTHETIC_ONLY_OFFLINE_NO_PUBLICATION',
        'sourceParts':len(names_a),'candidateParts':len(names_b),
        'addedParts':added,'removedParts':removed,'modifiedParts':changed,
        'modifiedNonMainPartCount':len(protected_changed),
        'sourceParagraphs':len(before_par),'candidateParagraphs':len(after_par),
        'expectedTextEdit':edit,'signals':layout_signals,'blockers':sorted(set(blockers)),
        'outcome':('STRUCTURAL_TEXT_ONLY_PREQUALIFIED_NOT_FIDELITY_APPROVAL' if prequalified else 'QUARANTINE_REQUIRED'),
        'mayPromote':False,
    }

def main():
    p=argparse.ArgumentParser(description=__doc__)
    p.add_argument('--source',type=Path,required=True);p.add_argument('--candidate',type=Path,required=True)
    p.add_argument('--expected',type=Path,required=True);p.add_argument('--out',type=Path,required=True)
    a=p.parse_args()
    for x in (a.source,a.candidate,a.expected,a.out):private_path(x)
    if a.out.exists():raise SystemExit('Refusing to overwrite report')
    expect=json.loads(a.expected.read_text(encoding='utf-8-sig'))
    report=analyze(a.source,a.candidate,expect)
    a.out.parent.mkdir(parents=True,exist_ok=True)
    with a.out.open('x',encoding='utf-8') as f:json.dump(report,f,indent=2,ensure_ascii=False)
    print(json.dumps({'outcome':report['outcome'],'blockers':report['blockers'],'added':len(report['addedParts']),'removed':len(report['removedParts']),'changed':len(report['modifiedParts'])}))
if __name__=='__main__':main()
