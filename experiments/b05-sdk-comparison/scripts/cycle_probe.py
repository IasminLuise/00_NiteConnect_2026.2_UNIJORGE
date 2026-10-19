#!/usr/bin/env python3
"""Independent body-text proof; never a structural/visual fidelity approval."""
import argparse,json
from pathlib import Path
from ooxml_gate import inventory,parse,W

def body_text(xml):
 """Paragraph text with explicit control tokens; NUL cannot occur in XML text.

 Keep controls distinct from literal characters, without normalizing the DOCX.
 This is a conservative body-text comparison, not a formatting/field approval.
 """
 result=[]
 for paragraph in parse(xml).iter(W+'p'):
  chunks=[]
  for element in paragraph.iter():
   if element.tag in (W+'t',W+'instrText',W+'delText',W+'delInstrText'):chunks.append(element.text or '')
   elif element.tag in (W+'tab',W+'br',W+'cr',W+'noBreakHyphen',W+'softHyphen',W+'sym'):
    chunks.append('\0'+element.tag+json.dumps(sorted(element.attrib.items()))+'\0')
  result.append(''.join(chunks))
 return result

def prove(original,edited,reverted):
 expected=original.copy()
 target=next((i for i,text in enumerate(expected) if text.strip()),None)
 if target is not None:expected[target]='XYZ'+expected[target]
 insertion=target is not None and edited==expected
 reversal=target is not None and reverted==original
 return {'exact_insertion':insertion,'exact_reversal':reversal,'complete_body_cycle':insertion and reversal,'scope':'body_text_only_not_fidelity_approval'}

def main():
 p=argparse.ArgumentParser();p.add_argument('--source',type=Path,required=True);p.add_argument('--edited',type=Path,required=True);p.add_argument('--reverted',type=Path,required=True);p.add_argument('--out',type=Path,required=True);a=p.parse_args()
 if '.private' not in a.out.resolve().parts:raise ValueError('Private output required')
 values=[inventory(path) for path in (a.source,a.edited,a.reverted)]
 result=prove(*[body_text(value['main_xml']) for value in values]);result['sha256']=[value['sha256'] for value in values]
 a.out.parent.mkdir(parents=True,exist_ok=True)
 with a.out.open('x',encoding='utf-8') as f:json.dump(result,f,indent=2)
 print(json.dumps({key:result[key] for key in ('exact_insertion','exact_reversal','complete_body_cycle')}))
if __name__=='__main__':main()
