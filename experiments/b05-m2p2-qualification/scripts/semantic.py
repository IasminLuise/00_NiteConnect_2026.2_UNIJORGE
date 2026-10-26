#!/usr/bin/env python3
"""Read-only, fail-closed OOXML semantic inventory for B05-M2P.2.

No mutation, repair, implicit part-name allowlist, publication, or official-model approval.
"""
from __future__ import annotations
import posixpath
import sys
from collections import Counter
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[2] / 'b05-m2p-editorial-fidelity' / 'scripts'))
from ooxml_fidelity import (load_package, safe_xml, paragraphs, expectation,
                            tree_key, W, R, PKG, C, MAIN)

STYLE_TAGS = {'pStyle', 'rStyle', 'tblStyle', 'numStyle', 'basedOn', 'next', 'link'}
NOTE_TYPES = {'separator', 'continuationSeparator'}
SEPARATOR_NODES = {W+x for x in ('p', 'r', 'pPr', 'rPr', 'spacing', 'ind', 'separator', 'continuationSeparator')}
FIELD_PARTS = ('word/document.xml', 'word/footnotes.xml', 'word/endnotes.xml')


def _xml(root):
    return tuple(_canonical(n) for n in root)


def _canonical(e, mapping=None, ignore_text=False):
    mapping = mapping or {}
    if not isinstance(e.tag, str):
        return ('non_element', e.tag)
    attrs = dict(e.attrib)
    if e.tag == W+'style':
        # The style identity may have changed; declared user-visible name/type and
        # exact style properties are compared separately.
        attrs.pop(W+'styleId', None)
    if e.tag in {W+x for x in STYLE_TAGS} and W+'val' in attrs:
        attrs[W+'val'] = mapping.get(attrs[W+'val'], attrs[W+'val'])
    txt = None if ignore_text and e.tag == W+'t' else e.text
    return (e.tag, tuple(sorted(attrs.items())), txt, e.tail,
            tuple(_canonical(c, mapping, ignore_text) for c in e))


def style_map(parts):
    if 'word/styles.xml' not in parts:
        return {}, ('MISSING_STYLES',)
    root = safe_xml(parts['word/styles.xml'])
    result, keys = {}, set()
    for e in root.findall(W+'style'):
        identity = e.get(W+'styleId')
        name = e.find(W+'name')
        key = (e.get(W+'type'), name.get(W+'val') if name is not None else None)
        if not identity or key[1] is None or key in keys or identity in result:
            return {}, ('AMBIGUOUS_STYLE_IDENTITY',)
        keys.add(key)
        result[identity] = key
    return result, ()


def compare_styles(a, b):
    amap, ae = style_map(a)
    bmap, be = style_map(b)
    if ae or be: return {'state':'INCONCLUSIVE','reason':'STYLE_IDENTITY_AMBIGUOUS', 'mappingCount':0}
    asigned = {i:'style:'+key[0]+':'+key[1] for i,key in amap.items()}
    bsigned = {i:'style:'+key[0]+':'+key[1] for i,key in bmap.items()}
    aroot = safe_xml(a['word/styles.xml']);broot=safe_xml(b['word/styles.xml'])
    def entries(root, mapping):
        record={}
        for style in root.findall(W+'style'):
            key = mapping[style.get(W+'styleId')]
            record[key] = _canonical(style, mapping)
        # docDefaults/latentStyles/other definitions remain protected.
        other=tuple(_canonical(e, mapping) for e in root if e.tag != W+'style')
        return record,other
    old,other_a=entries(aroot,asigned);new,other_b=entries(broot,bsigned)
    same=old==new and other_a==other_b
    pairs=sorted((i,j) for i,k in amap.items() for j,h in bmap.items() if k==h and i!=j)
    return {'state':'EQUIVALENT_BY_EXACT_STYLE_DEFINITION' if same else 'DIFFERENT_OR_UNPROVEN',
            'mappingCount':len(pairs), 'styleIdsRenamed':len(pairs),
            'styleSetEqual':set(old)==set(new),
            'styleDefinitionsEqualAfterIdResolution':same,
            'stylePropertiesDiffer':sum(old.get(k)!=new.get(k) for k in set(old)|set(new))}


def _rels_source(name):
    if name=='_rels/.rels':return ''
    path, fname=posixpath.split(name)
    if not path.endswith('/_rels'): return None
    return posixpath.join(path[:-6],fname[:-5]) if fname.endswith('.rels') else None


def relationships(parts):
    result, risks={},[]
    for name, bytes_ in parts.items():
        if not name.endswith('.rels'):continue
        source=_rels_source(name)
        if source is None:
            risks.append('RELATIONSHIP_PATH_INVALID');continue
        xml=safe_xml(bytes_)
        if xml.tag!=PKG+'Relationships':
            risks.append('RELATIONSHIP_ROOT_INVALID');continue
        seen=set();entries=[]
        for el in xml:
            if el.tag!=PKG+'Relationship':
                risks.append('RELATIONSHIP_NODE_UNKNOWN');continue
            rid=el.get('Id');target=el.get('Target');mode=el.get('TargetMode','Internal');rtype=el.get('Type')
            if not rid or rid in seen or not target or not rtype:
                risks.append('RELATIONSHIP_MALFORMED');continue
            seen.add(rid)
            if mode!='Internal':
                risks.append('EXTERNAL_RELATIONSHIP');entries.append((rtype,'EXTERNAL',mode));continue
            if target.startswith('/') or ':' in target or '\\' in target or '#' in target or '?' in target:
                risks.append('RELATIONSHIP_TARGET_UNSAFE');continue
            dest=posixpath.normpath(posixpath.join(posixpath.dirname(source),target))
            if dest.startswith('../') or dest not in parts:
                risks.append('RELATIONSHIP_TARGET_MISSING');continue
            entries.append((rtype,dest,'Internal'))
        result[source]=(tuple(sorted(entries)),{el.get('Id'):el for el in xml if el.tag==PKG+'Relationship'})
    return result,sorted(set(risks))


def ref_targets(parts):
    rels, risks=relationships(parts)
    resolved=[]
    for part,data in parts.items():
        if not part.endswith('.xml') or part=='[Content_Types].xml':continue
        root=safe_xml(data)
        ridmap=rels.get(part,((),{}))[1]
        for el in root.iter():
            for name,value in el.attrib.items():
                if name.startswith(R) and name.removeprefix(R) in ('id','embed','link'):
                    rel=ridmap.get(value)
                    if rel is None: risks.append('OOXML_DANGLING_RELATIONSHIP_REFERENCE');continue
                    resolved.append((part,el.tag,name,rel.get('Type'),rel.get('Target'),rel.get('TargetMode','Internal')))
    return Counter(resolved),sorted(set(risks))


def _fields(parts):
    """Compare actual field tokens without conflating empty notes-part addition with field loss."""
    records=[]
    for n in sorted(parts):
        if not (n in FIELD_PARTS or n.startswith('word/header') and n.endswith('.xml')
                or n.startswith('word/footer') and n.endswith('.xml')):continue
        root=safe_xml(parts[n]);tokens=[]
        for e in root.iter():
            if e.tag==W+'fldSimple':tokens.append(('simple',e.get(W+'instr','')))
            elif e.tag==W+'instrText':tokens.append(('instruction',e.text or ''))
            elif e.tag==W+'fldChar':tokens.append(('boundary',e.get(W+'fldCharType','')))
        if tokens:records.append((n,tuple(tokens)))
    return tuple(records)


def note_parts(parts):
    notes=[];risk=[];xml_signatures={}
    for name in ('word/footnotes.xml','word/endnotes.xml'):
        if name not in parts:continue
        root=safe_xml(parts[name]);xml_signatures[name]=tree_key(root)
        expected=W+('footnotes' if 'footnotes' in name else 'endnotes')
        if root.tag!=expected: risk.append('NOTES_ROOT_INVALID');continue
        children=list(root); ids=set(); separator_only=True
        for note in children:
            id_=note.get(W+'id');ty=note.get(W+'type')
            if id_ is None or id_ in ids:risk.append('NOTE_ID_MISSING_OR_DUPLICATE')
            ids.add(id_)
            # Only the known separator grammar proves absence of user content.
            # Unknown nodes, symbols and legacy VML must remain inconclusive.
            if ty not in NOTE_TYPES or any(e.tag not in SEPARATOR_NODES for e in note.iterdescendants()):
                separator_only=False
        notes.append({'part':name,'entries':len(children),'separatorOnly':separator_only})
    refs=[]
    for name,blob in parts.items():
        if not name.startswith('word/') or not name.endswith('.xml') or name in ('word/footnotes.xml','word/endnotes.xml'):continue
        for el in safe_xml(blob).iter():
            if el.tag in (W+'footnoteReference',W+'endnoteReference'):refs.append((name,el.tag,el.get(W+'id')))
    return {'notes':notes,'references':tuple(refs),'xmlSignatures':xml_signatures,'riskCodes':sorted(set(risk))}


def content_types(parts):
    root=safe_xml(parts['[Content_Types].xml'])
    if root.tag!=C+'Types':return {},['CONTENT_TYPES_ROOT_INVALID']
    defaults={};overrides={};risk=[]
    for el in root:
        if el.tag==C+'Default':
            k=el.get('Extension','').lower();v=el.get('ContentType')
            if not k or not v or k in defaults:risk.append('CONTENT_TYPES_DUPLICATE')
            defaults[k]=v
        elif el.tag==C+'Override':
            k=el.get('PartName','').lstrip('/');v=el.get('ContentType')
            if not k or not v or k in overrides:risk.append('CONTENT_TYPES_DUPLICATE')
            overrides[k]=v
        else:risk.append('CONTENT_TYPES_UNKNOWN_NODE')
    effective={}
    for name in parts:
        if name=='[Content_Types].xml':continue
        media=overrides.get(name) or defaults.get(name.rsplit('.',1)[-1].lower())
        if not media:risk.append('MISSING_CONTENT_TYPE')
        strict_types={
            'word/footnotes.xml':'application/vnd.openxmlformats-officedocument.wordprocessingml.footnotes+xml',
            'word/endnotes.xml':'application/vnd.openxmlformats-officedocument.wordprocessingml.endnotes+xml',
            'word/document.xml':'application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml',
        }
        if name in strict_types and media!=strict_types[name]:risk.append('WRONG_PROTECTED_PART_CONTENT_TYPE')
        effective[name]=media
    for name in overrides:
        if name not in parts:risk.append('ORPHAN_CONTENT_TYPE_OVERRIDE')
    return effective,sorted(set(risk))


def analyze_semantics(source:Path,candidate:Path,expected:dict):
    a=load_package(source);b=load_package(candidate)
    text=expectation(paragraphs(a),paragraphs(b),expected)
    style=compare_styles(a,b)
    rel_a,risk_a=relationships(a);rel_b,risk_b=relationships(b)
    refs_a,ref_risk_a=ref_targets(a);refs_b,ref_risk_b=ref_targets(b)
    types_a,type_risk_a=content_types(a);types_b,type_risk_b=content_types(b)
    fields_a=_fields(a);fields_b=_fields(b)
    notes_a=note_parts(a);notes_b=note_parts(b)
    used_parts=set(a)|set(b)
    risks=sorted(set(risk_a+risk_b+ref_risk_a+ref_risk_b+type_risk_a+type_risk_b+notes_a['riskCodes']+notes_b['riskCodes']))
    # Compare relationship semantics for shared source parts; differences may be
    # legitimate but are NOT automatically authorized.
    common=set(rel_a)&set(rel_b)
    rel_differences=sum(rel_a[p][0]!=rel_b[p][0] for p in common)
    common_type_diff=sum(types_a[p]!=types_b[p] for p in set(types_a)&set(types_b))
    extra_notes=set(b)-set(a)
    new_note_names=extra_notes & {'word/footnotes.xml','word/endnotes.xml'}
    new_refs=[r for r in notes_b['references'] if (r[1]==W+'footnoteReference' and 'word/footnotes.xml' in new_note_names)
              or (r[1]==W+'endnoteReference' and 'word/endnotes.xml' in new_note_names)]
    only_empty_added=all(
        next((n['separatorOnly'] for n in notes_b['notes'] if n['part']==name), False)
        for name in new_note_names)
    empty_notes_added=bool(new_note_names) and only_empty_added and not new_refs and not notes_b['riskCodes']
    return {
      'schema':'b05-m2p2-semantics-v1', 'scope':'SYNTHETIC_ONLY_READ_ONLY',
      'expectedEditExact':text['exact'],
      'parts':{'original':len(a),'candidate':len(b),'added':len(set(b)-set(a)),'removed':len(set(a)-set(b)),
               'changedCommon':sum(a[p]!=b[p] for p in set(a)&set(b))},
      'styles':style,
      'fields':{'sourceFieldTokens':sum(len(x[1]) for x in fields_a),
                'candidateFieldTokens':sum(len(x[1]) for x in fields_b),
                'state':'NOT_EXERCISED' if not fields_a and not fields_b else 'EQUAL' if fields_a==fields_b else 'DIFFERENT'},
      'notes':{'sourceReferences':len(notes_a['references']),'candidateReferences':len(notes_b['references']),
               'candidateSeparatorOnly':empty_notes_added,'status':'NEW_EMPTY_NOTES_NOT_APPROVED' if empty_notes_added else 'REQUIRES_REVIEW' if (notes_a['references']!=notes_b['references'] or notes_a['xmlSignatures']!=notes_b['xmlSignatures']) else 'UNCHANGED'},
      'relationships':{'changedSharedSources':rel_differences,'sourceSetsChanged':set(rel_a)!=set(rel_b),
                       'resolvedReferencesEqual':refs_a==refs_b},
      'contentTypes':{'changedCommon':common_type_diff,'partTypesAddedOrRemoved':bool(set(types_a)!=set(types_b))},
      'protectedParts':{'stylesWithEffectsRemoved':'word/stylesWithEffects.xml' in a and 'word/stylesWithEffects.xml' not in b,
                        'thumbnailRemoved':'docProps/thumbnail.jpeg' in a and 'docProps/thumbnail.jpeg' not in b,
                        'mediaChanged':any(a.get(p)!=b.get(p) for p in used_parts if p.startswith(('word/media/','word/embeddings/')))},
      'risks':risks,
      'classification':'EXACT_TEXT_ONLY' if set(a)==set(b) and all(a[k]==b[k] for k in a if k not in (MAIN,'docProps/core.xml'))
                       and tree_key(safe_xml(a[MAIN]),allow_text=True)==tree_key(safe_xml(b[MAIN]),allow_text=True) and text['exact']
                       else 'OOXML_TRANSFORMATIONS_REQUIRE_JUSTIFICATION',
      'mayPromote':False
    }
