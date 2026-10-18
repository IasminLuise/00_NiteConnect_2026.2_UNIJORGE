#!/usr/bin/env python3
"""B05.0.4 conservative, local-only DOCX read-only prequalification gate.
Does NOT establish Word visual parity or automatically homologate a vendor.
"""
from __future__ import annotations
import argparse
from collections import Counter
from hashlib import sha256
import json
import re
from pathlib import Path
from zipfile import ZipFile
import xml.etree.ElementTree as ET

W = '{http://schemas.openxmlformats.org/wordprocessingml/2006/main}'
PARA_ID = '{http://schemas.microsoft.com/office/word/2010/wordml}paraId'
WORD = 'word/'
MAX_DOCX = 8 * 1024 * 1024
MAX_TOTAL = 64 * 1024 * 1024
MAX_PART = 16 * 1024 * 1024

def die(message):
    raise ValueError(message)

def parse(data: bytes):
    if b'<!DOCTYPE' in data.upper() or b'<!ENTITY' in data.upper():
        die('DTD/entities unsupported')
    return ET.fromstring(data)

def load(path: Path):
    if path.stat().st_size > MAX_DOCX:
        die('DOCX oversized')
    result = {}
    with ZipFile(path) as zin:
        entries = zin.infolist()
        if len(entries) > 512 or sum(x.file_size for x in entries) > MAX_TOTAL:
            die('Unsupported archive size/count')
        for entry in entries:
            name = entry.filename
            if (name.startswith('/') or '\\' in name or ':' in name or
                any(s in ('', '.', '..') for s in name.split('/')) or
                entry.file_size > MAX_PART or name in result):
                die('Unsafe or duplicate archive entry')
            result[name] = zin.read(entry)
    if not {'[Content_Types].xml', 'word/document.xml'} <= result.keys():
        die('Missing DOCX main parts')
    for name, data in result.items():
        if name.endswith(('.xml', '.rels')):
            parse(data)
    return result

def paragraphs(root):
    values=[]
    for p in root.iter(W+'p'):
        txt=''.join((el.text or '') for el in p.iter() if el.tag in (W+'t',W+'tab',W+'instrText'))
        paragraph_props=p.find(W+'pPr')
        # A strict XML signature catches hidden spacing/borders/indent/keep changes.
        props=ET.tostring(paragraph_props,encoding='unicode') if paragraph_props is not None else ''
        values.append({'text':txt,'properties':props})
    return values

def fields(root):
    entries=[]
    for field in root.iter(W+'fldSimple'):
        entries.append({'form':'simple','instruction':field.get(W+'instr','').strip()})
    instr=''.join((e.text or '') for e in root.iter(W+'instrText'))
    if instr.strip():
        entries.append({'form':'complex','instruction':instr.strip()})
    return entries

def tree_signature(element):
    """Prefix/order independent, but preserves OOXML content and element structure."""
    text = (element.text or '') if element.tag in (W+'t',W+'instrText',W+'delText',W+'delInstrText') or element.get('{http://www.w3.org/XML/1998/namespace}space')=='preserve' else (element.text if element.text and element.text.strip() else '')
    tail = element.tail if element.tail and element.tail.strip() else ''
    return (element.tag, tuple(sorted(element.attrib.items())), text,
            tuple(tree_signature(child) for child in element), tail)

def normalize_plain_runs(root):
    """Only adjacent, attribute-free plain-text runs without any direct formatting.

    Never crosses fields, bookmarks, hyperlinks, revisions or other children.
    Whitespace-bearing edge text and xml:space remain conservatively exact.
    """
    for paragraph in root.iter(W+'p'):
        previous=None
        for child in list(paragraph):
            clean_run=not (child.text or '').strip() and not (child.tail or '').strip()
            if child.tag==W+'r' and not child.attrib and len(child)==0 and clean_run:
                paragraph.remove(child);continue
            if child.tag==W+'r' and not child.attrib and len(child)==1 and clean_run:
                text=child[0];value=text.text or ''
                if text.tag==W+'t' and not text.attrib and len(text)==0 and not (text.tail or '').strip() and value and value.strip()==value and not any(c in value for c in '\r\n\t'):
                    if previous is not None:
                        previous.text=(previous.text or '')+value;paragraph.remove(child)
                    else:previous=text
                    continue
            previous=None

def expected_document_signature(data, expected_prefix, allow_plain_text_run_segmentation=False):
    root = parse(data)
    if expected_prefix is not None:
        for paragraph in root.iter(W+'p'):
            texts = [e for e in paragraph.iter(W+'t') if (e.text or '').strip()]
            if texts:
                texts[0].text = expected_prefix + (texts[0].text or '')
                break
        else:
            die('No editable paragraph target')
    if allow_plain_text_run_segmentation:normalize_plain_runs(root)
    return tree_signature(root)

def inventory(path:Path):
    raw=load(path)
    stories={};allfields=[];tables=0
    for name,data in raw.items():
        if not name.startswith(WORD) or not name.endswith('.xml'):
            continue
        root=parse(data)
        p=paragraphs(root)
        if name=='word/document.xml' or name.startswith(('word/header','word/footer')):
            stories[name]=p
        for f in fields(root):
            allfields.append({'part':name,**f})
        if name=='word/document.xml':
            tables=sum(1 for e in root.iter(W+'tbl'))
    doc=raw['word/document.xml'];root=parse(doc)
    topologies=[tuple(len(tr.findall(W+'tc')) for tr in tbl.iter(W+'tr')) for tbl in root.iter(W+'tbl')]
    media={name:sha256(data).hexdigest() for name,data in raw.items() if name.startswith('word/media/')}
    # All structural parts kept; fingerprints do not expose text but remain private.
    protected_names=('word/styles.xml','word/settings.xml','word/numbering.xml','word/fontTable.xml','word/theme/theme1.xml')
    protection={name:sha256(json.dumps(tree_signature(parse(data)),ensure_ascii=False).encode()).hexdigest() for name,data in raw.items() if name in protected_names}
    signatures={name:tree_signature(parse(data)) for name,data in raw.items() if name.endswith(('.xml','.rels'))}
    xml_parts={name:data for name,data in raw.items() if name.endswith(('.xml','.rels'))}
    binary_parts={name:sha256(data).hexdigest() for name,data in raw.items() if name not in xml_parts}
    references=any(key.split('}')[-1] in ('paraId','paraIdParent') and not (element.tag==W+'p' and key==PARA_ID)
                   for data in xml_parts.values() for element in parse(data).iter() for key in element.attrib)
    return {'sha256':sha256(path.read_bytes()).hexdigest(),'parts':set(raw),'stories':stories,'fields':allfields,
            'signatures':signatures,'main_xml':raw['word/document.xml'],
            'media':media,'tables':tables,'topologies':topologies,'protected_parts':protection,
            'part_count':len(raw),'paragraph_count':len(stories.get('word/document.xml',[])),
            'xml_parts':xml_parts,'binary_parts':binary_parts,'paragraph_id_references':references}

def paragraph_identity_comparison(source, candidate, has_references):
    """Opt-in: only valid unreferenced additions; never remap or discard existing IDs.

    MS-DOCX 2.6.2.3: unique within a part, >0 and <0x80000000. This changes
    only the in-memory comparison tree, never the candidate DOCX bytes.
    """
    if has_references:
        return tree_signature(candidate)
    old=list(source.iter(W+'p'));new=list(candidate.iter(W+'p'))
    if len(old)!=len(new):
        return tree_signature(candidate)
    ids=[p.get(PARA_ID) for p in new if p.get(PARA_ID) is not None]
    if any(not re.fullmatch(r'[0-9A-Fa-f]{8}',value) or not 0<int(value,16)<0x80000000 for value in ids):
        return tree_signature(candidate)
    if len({int(value,16) for value in ids})!=len(ids):
        return tree_signature(candidate)
    for previous,current in zip(old,new):
        if previous.get(PARA_ID) is None:
            current.attrib.pop(PARA_ID,None)
    return tree_signature(candidate)

def compare(before:dict,after:dict,expected_prefix:str|None=None,*,allow_paragraph_id_additions=False,allow_plain_text_run_segmentation=False):
    issues=[]
    for key in ('parts','media','binary_parts','tables','topologies','fields','protected_parts'):
        if before[key]!=after[key]: issues.append('CHANGED_'+key.upper())
    if before['stories'].keys()!=after['stories'].keys(): issues.append('CHANGED_STORIES')
    for name in before['signatures'].keys() & after['signatures'].keys():
        if name in ('docProps/core.xml','docProps/app.xml'):
            continue  # Volatile metadata. SAVEDATE validation in Word remains mandatory.
        expected=(expected_document_signature(before['main_xml'],expected_prefix,allow_plain_text_run_segmentation) if name=='word/document.xml'
                  else before['signatures'][name])
        actual=after['signatures'][name]
        if allow_paragraph_id_additions or name=='word/document.xml' and allow_plain_text_run_segmentation:
            candidate=parse(after['xml_parts'][name])
            if allow_paragraph_id_additions:
                paragraph_identity_comparison(parse(before['xml_parts'][name]),candidate,before['paragraph_id_references'] or after['paragraph_id_references'])
            if name=='word/document.xml' and allow_plain_text_run_segmentation:normalize_plain_runs(candidate)
            actual=tree_signature(candidate)
        if expected!=actual:
            issues.append('CHANGED_XML_STRUCTURE:'+name)
    for name in before['stories'].keys()&after['stories'].keys():
        old=before['stories'][name];new=after['stories'][name]
        if len(old)!=len(new): issues.append('CHANGED_PARAGRAPH_COUNT:'+name);continue
        first_target=next((i for i,p in enumerate(old) if p['text'].strip()),None) if name=='word/document.xml' else None
        for i,(a,b) in enumerate(zip(old,new)):
            if a['properties']!=b['properties']: issues.append(f'CHANGED_PARAGRAPH_PROPERTIES:{name}:{i}')
            expected=a['text']
            if expected_prefix is not None and i==first_target:
                expected=expected_prefix+expected
            if expected!=b['text']:issues.append(f'CHANGED_TEXT:{name}:{i}')
    return {'status':'PASS_STRUCTURAL' if not issues else 'FAIL_STRUCTURAL',
            'issues':issues[:100], 'issue_count':len(issues),
            'comparison_policy':'valid_unreferenced_para_id_additions' if allow_paragraph_id_additions else 'preserve_ids',
            'plain_text_run_segmentation':allow_plain_text_run_segmentation,
            'note':'Strict structural screen only. Requires Word/PDF visual, fields and multi-save acceptance.'}

def stats(result):
    types=Counter(f['instruction'].split(' ')[0].upper() for f in result['fields'])
    return {'parts':result['part_count'],'body_paragraphs':result['paragraph_count'],'tables':result['tables'],
            'fields_by_type':dict(types),'media_count':len(result['media'])}

def main():
    arg=argparse.ArgumentParser(description='Private offline DOCX inventory/comparison')
    arg.add_argument('--before',required=True,type=Path);arg.add_argument('--after',type=Path)
    arg.add_argument('--expected-prefix',default=None);arg.add_argument('--out',type=Path,required=True)
    arg.add_argument('--allow-paragraph-id-additions',action='store_true',help='Only valid unreferenced paragraph ID additions; existing IDs remain protected')
    arg.add_argument('--allow-plain-text-run-segmentation',action='store_true',help='Adjacent unformatted plain-text runs in body only; no fields or formatting ignored')
    args=arg.parse_args()
    if '.private' not in args.out.resolve().parts:die('Output must be within .private/ directory')
    old=inventory(args.before)
    result={'original_sha256':old['sha256'],'original_summary':stats(old)}
    if args.after:
        new=inventory(args.after);result.update({'candidate_sha256':new['sha256'],'candidate_summary':stats(new),
                                                   'comparison':compare(old,new,args.expected_prefix,allow_paragraph_id_additions=args.allow_paragraph_id_additions,allow_plain_text_run_segmentation=args.allow_plain_text_run_segmentation)})
    args.out.parent.mkdir(parents=True,exist_ok=True)
    if args.out.exists():die('Never overwrite an existing inspection result')
    args.out.write_text(json.dumps(result,indent=2,ensure_ascii=False)+'\n',encoding='utf-8')
    print(json.dumps({'result_file':str(args.out),'comparison':result.get('comparison',{}).get('status','INVENTORY_ONLY')},ensure_ascii=False))
if __name__=='__main__':main()
