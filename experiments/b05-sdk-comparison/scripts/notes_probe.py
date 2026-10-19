#!/usr/bin/env python3
"""Read-only note inventory. This never authorizes removing or rewriting OOXML."""
import argparse,json
from pathlib import Path
from ooxml_gate import load,parse,W,tree_signature

def inventory_notes(path):
 raw=load(Path(path));notes=[];references=[];properties=[];relations=[];content_types=[]
 for name,data in raw.items():
  if not name.endswith(('.xml','.rels')):continue
  root=parse(data)
  if name in ('word/footnotes.xml','word/endnotes.xml'):
   for e in root:
    notes.append({'part':name,'id':e.get(W+'id'),'type':e.get(W+'type','normal'),'text':''.join(e.itertext()),'signature':tree_signature(e)})
  for e in root.iter():
   local=e.tag.split('}')[-1]
   if e.tag in tuple(W+t for t in ('footnoteReference','endnoteReference','footnoteRef','endnoteRef')):
    references.append({'part':name,'element':local,'id':e.get(W+'id')})
   if e.tag in (W+'footnotePr',W+'endnotePr'):properties.append({'part':name,'signature':tree_signature(e)})
   if local=='Relationship' and any(s in e.get('Type','') for s in ('/footnotes','/endnotes')):relations.append({'part':name,**e.attrib})
   if local=='Override' and e.get('PartName') in ('/word/footnotes.xml','/word/endnotes.xml'):content_types.append(e.attrib)
 return {'notes':notes,'regular_note_count':sum(n['type']=='normal' for n in notes),'references':references,'properties':properties,'relations':relations,'content_types':content_types,'classification':'INVENTORY_ONLY_NOT_EQUIVALENCE_CERTIFICATION'}

def main():
 p=argparse.ArgumentParser();p.add_argument('--before',type=Path,required=True);p.add_argument('--after',type=Path);p.add_argument('--out',type=Path,required=True);a=p.parse_args()
 if '.private' not in a.out.resolve().parts:raise ValueError('Private output required')
 result={'source':inventory_notes(a.before)}
 if a.after:result['candidate']=inventory_notes(a.after)
 a.out.parent.mkdir(parents=True,exist_ok=True)
 with a.out.open('x',encoding='utf-8') as f:json.dump(result,f,indent=2,ensure_ascii=False)
 print('Note inventory recorded privately; no equivalence approval')
if __name__=='__main__':main()
