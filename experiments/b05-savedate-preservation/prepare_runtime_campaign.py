"""Fresh synthetic integrated campaign for the source-built native SDK."""
from __future__ import annotations
import argparse
import hashlib
import json
from pathlib import Path
from docx import Document
from docx.oxml import OxmlElement
from docx.oxml.ns import qn
from prepare_local import prepare

CASES=[
 ('NATIVE_PTBR_SIMPLE','SAVEDATE \\* MERGEFORMAT','pt-BR',True),
 ('NATIVE_EN_SIMPLE','SAVEDATE \\* MERGEFORMAT','en-US',True),
 ('NATIVE_DE_COMPLEX','SAVEDATE \\* MERGEFORMAT','de-DE',False),
 ('NATIVE_FR_COMPLEX','SAVEDATE \\* MERGEFORMAT','fr-FR',False),
 ('NATIVE_UPPER','SAVEDATE \\@ "MMMM" \\* Upper \\* MERGEFORMAT','pt-BR',False),
 ('NATIVE_MERGE','SAVEDATE \\@ "yyyy-MM-dd HH:mm:ss" \\* MERGEFORMAT','pt-BR',False),
 ('NATIVE_CHAR','SAVEDATE \\@ "yyyy-MM-dd HH:mm:ss" \\* CHARFORMAT','pt-BR',False),
 ('NATIVE_UNSUPPORTED','SAVEDATE \\h','pt-BR',False),
]


def run(text,language,style=None,tag='w:t'):
    result=OxmlElement('w:r');pr=OxmlElement('w:rPr');lang=OxmlElement('w:lang');lang.set(qn('w:val'),language);pr.append(lang)
    if style:
        flag=OxmlElement('w:b' if style=='code' else 'w:i');pr.append(flag)
        color=OxmlElement('w:color');color.set(qn('w:val'),'FF0000' if style=='code' else '0000FF');pr.append(color)
    result.append(pr);value=OxmlElement(tag);value.text=text;result.append(value);return result


def add_matrix(source: Path,output: Path):
    document=Document(source);normal=document.styles['Normal'].element.get_or_add_rPr()
    language=OxmlElement('w:lang');language.set(qn('w:val'),'pt-BR');normal.append(language)
    anchor=document.paragraphs[1]
    for label,instruction,language,simple in CASES:
        paragraph=anchor.insert_paragraph_before(label+': ')
        styled=label in ('NATIVE_CHAR','NATIVE_MERGE')
        if simple:
            field=OxmlElement('w:fldSimple');field.set(qn('w:instr'),instruction)
            field.append(run('CACHE_'+label,language));paragraph._p.append(field)
        else:
            for kind in ['begin','code','separate','cache','end']:
                if kind=='code':element=run(instruction,language,'code' if styled else None,'w:instrText')
                elif kind=='cache':element=run('CACHE_'+label,language,'cached' if styled else None)
                else:
                    element=run('',language,'code' if styled else None);element.remove(element[-1])
                    char=OxmlElement('w:fldChar');char.set(qn('w:fldCharType'),kind);element.append(char)
                paragraph._p.append(element)
    with output.open('xb') as stream: document.save(stream)


def build_campaign(folder: Path):
    folder=folder.resolve()
    if '.private' not in folder.parts:raise ValueError('Private campaign required')
    folder.mkdir(parents=True,exist_ok=False)
    construction=folder/'construction';plan=prepare(construction)
    for name in ['original','compatible']:
        target=folder/(name+'.docx');add_matrix(construction/(name+'.docx'),target)
        plan[name]={'path':target.name,'sha256':hashlib.sha256(target.read_bytes()).hexdigest()}
    for item in plan['cases']:
        destination=folder/'cases'/item['name'];destination.mkdir(parents=True)
        file=destination/'compatible-source.docx'
        with file.open('xb') as stream:stream.write((folder/'compatible.docx').read_bytes())
        item['source']={'path':file.relative_to(folder).as_posix(),'sha256':plan['compatible']['sha256']}
    plan['native_source_build']={'required':True,'body_fields':[{'label':v[0],'instruction':v[1],'language':v[2],'simple':v[3]} for v in CASES],
                                 'header_compatibility_retained_for_continuity':True,'expected_f9_before_first_save':'2020-01-02T00:04:05-03:00'}
    (folder/'plan-private.json').write_text(json.dumps(plan,ensure_ascii=False,indent=2),encoding='utf-8')
    return plan


if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('--campaign',required=True,type=Path)
    args=parser.parse_args();plan=build_campaign(args.campaign)
    print(json.dumps({'cases_planned':len(plan['cases']),'native_body_fields':len(CASES),'engine_executed':False}))
