import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import vm from 'node:vm';

const root=process.env.B05_SDK_SOURCE;
if(!root)throw Error('Set B05_SDK_SOURCE to the pinned sdkjs source checkout');
const extension=new URL('../native/savedate.js',import.meta.url);
function runtime(){
 const c=vm.createContext({Date,console,Asc:{c_oAscNumberingFormat:{}},AscWord:{},AscCommon:{}});
 c.window=c;
 for(const file of ['word/Editor/Paragraph/ComplexFields/types.js','word/Editor/Paragraph/ComplexFields/instruction-base.js','word/Editor/Paragraph/ComplexFields/ref.js','word/Editor/Paragraph/ComplexFieldInstruction.js'])vm.runInContext(fs.readFileSync(path.join(root,file),'utf8'),c);
 vm.runInContext(fs.readFileSync(extension,'utf8'),c);
 return c;
}
test('source parser uses SAVEDATE type and retains picture/general switches',()=>{
 const c=runtime(),code='SAVEDATE \\@ "dd \'de\' MMMM \'de\' yyyy" \\* MERGEFORMAT';
 const f=new c.CFieldInstructionParser().GetInstructionClass(code);
 assert.equal(f.GetType(),80);assert.equal(f.GetFormat(),"dd 'de' MMMM 'de' yyyy");assert.equal(f.isMergeFormat(),true);assert.equal(f.ToString(),code);
});
test('source parser supports CHARFORMAT, case insensitive names and text casing',()=>{
 const c=runtime();
 const f=new c.CFieldInstructionParser().GetInstructionClass('savedate \\* CHARFORMAT \\* Upper');
 assert.equal(f.GetType(),80);assert.equal(f.CharFormat,true);assert.equal(f.TextCase,'UPPER');assert.equal(f.Unsupported,false);
 for(const code of ['SAVEDATE \\h','SAVEDATE \\@','SAVEDATE \\* Unknown','SAVEDATE \\s'])assert.equal(new c.CFieldInstructionParser().GetInstructionClass(code).Unsupported,true,code);
});
test('simple NUMPAGES instruction survives field-to-run conversion and DOCX export',()=>{
 const fieldSource=fs.readFileSync(path.join(root,'word/Editor/Field.js'),'utf8');
 const start=fieldSource.indexOf('ParaField.prototype.GetRunWithPageField = function(paragraph)');
 const end=fieldSource.indexOf('ParaField.prototype.IsValid = function()',start);
 assert.ok(start>=0&&end>start,'NUMPAGES conversion source anchors must exist');
 const c=vm.createContext({ParaField:function(){},AscWord:{fieldtype_PAGENUM:1,fieldtype_PAGECOUNT:3,CRunPagesCount:class{constructor(count){this.count=count;}}},
  ParaRun:class{constructor(){this.Content=[];}Set_Pr(){}AddToContentToEnd(value){this.Content.push(value);}}});
 vm.runInContext(fieldSource.slice(start,end),c);
 const field={FieldType:3,GetFirstRunNonEmpty:()=>null,GetSelectedText:()=> '5',GetInstructionLine:()=> 'NUMPAGES \\* Arabic'};
 const converted=c.ParaField.prototype.GetRunWithPageField.call(field,{});
 assert.equal(converted.Content[0].fieldInstruction,'NUMPAGES \\* Arabic');

 const pages=fs.readFileSync(path.join(root,'word/Editor/Paragraph/RunContent/PagesCount.js'),'utf8');
 const serializer=fs.readFileSync(path.join(root,'word/Editor/Serialize2.js'),'utf8');
 assert.match(pages,/copy\.fieldInstruction\s*=\s*this\.fieldInstruction/);
 assert.match(serializer,/Instr\s*=\s*elem\.pageNum\.fieldInstruction\s*\|\|\s*"NUMPAGES \\\\\* MERGEFORMAT"/);
 assert.match(serializer,/Instr\s*=\s*"PAGE \\\\\* MERGEFORMAT"/,'PAGE retains its previous fallback');
 const copyStart=pages.indexOf('CRunPagesCount.prototype.Copy = function()'),copyEnd=pages.indexOf('CRunPagesCount.prototype.UpdatePageCount =',copyStart);
 assert.ok(copyStart>=0&&copyEnd>copyStart,'page-count copy source anchors must exist');
 const pageRuntime=vm.createContext({CRunPagesCount:function(pageNum){this.pageNum=pageNum;this.graphemes=[];this.widths=[];}});
 pageRuntime.CRunPagesCount.prototype={};
 vm.runInContext(pages.slice(copyStart,copyEnd),pageRuntime);
 const page=Object.assign(new pageRuntime.CRunPagesCount(5),{numFormat:7,numText:'٥',textPr:{font:'Test'},graphemes:['5'],widths:[1],Width:1,WidthVisible:1,fieldInstruction:'NUMPAGES \\* Arabic'});
 const copied=page.Copy();
 assert.equal(copied.fieldInstruction,page.fieldInstruction);
 assert.deepEqual(Array.from(copied.graphemes),['5']);
});
test('native field prefers the explicit field run language, then falls back to the Word document default',()=>{
 const c=runtime(),pictures=[];
 c.AscCommon.g_aCultureInfos={1046:{Name:'pt-BR',DateSeparator:'/',UseAMPM:0},1033:{Name:'en-US',DateSeparator:'/',UseAMPM:1},1031:{Name:'de-DE',DateSeparator:'.',UseAMPM:0}};
 c.AscCommon.getShortDateFormat=culture=>culture.Name==='en-US'?'m/d/yyyy':'dd/mm/yyyy';
 c.AscCommon.getLongTimeFormat=culture=>culture.UseAMPM?'h:mm:ss AM/PM;@':'h:mm:ss;@';
 c.AscCommon.NumFormatType={WordFieldDate:1};c.AscCommon.oNumFormatCache={get(picture){pictures.push(picture);return {formatToWord:()=> 'OUT'};}};
 const instruction=new c.CFieldInstructionParser().GetInstructionClass('SAVEDATE');
 const doc={Core:{asc_getModified:()=>new Date('2020-01-02T03:04:05Z')},GetDefaultLanguage:()=>1033};
 const run=(lang,compiledLang=lang)=>({GetDirectTextPr:()=>({Lang:{Val:lang}}),Get_CompiledPr:()=>({Lang:{Val:compiledLang}})});
 assert.equal(c.AscWord.calculateSavedate(instruction,doc,run(1046)).value,'OUT');assert.equal(pictures.pop(),'dd/MM/yyyy HH:mm:ss');
 assert.equal(c.AscWord.calculateSavedate(instruction,doc,null).value,'OUT');assert.equal(pictures.pop(),'M/d/yyyy h:mm:ss AM/PM');
 c.AscWord.calculateSavedate(instruction,doc,run(1031,1046));assert.equal(pictures.pop(),'dd.MM.yyyy HH:mm:ss');
 const docPt={...doc,GetDefaultLanguage:()=>1046};c.AscWord.calculateSavedate(instruction,docPt,run(1031));assert.equal(pictures.pop(),'dd.MM.yyyy HH:mm:ss');
 const docWithoutDefault={Core:doc.Core};c.AscWord.calculateSavedate(instruction,docWithoutDefault,run(1031));assert.equal(pictures.pop(),'dd.MM.yyyy HH:mm:ss');
 const unsupportedDoc={...doc,GetDefaultLanguage:()=>999999};assert.equal(c.AscWord.calculateSavedate(instruction,unsupportedDoc,run(999999)).reason,'UNSUPPORTED_LANGUAGE');
 assert.equal(c.AscWord.calculateSavedate(instruction,{Core:{asc_getModified:()=>null}},run(1046)).reason,'MISSING_LAST_SAVE');
});
test('F9 resolves language from an instruction run before the field result run',()=>{
 const c=runtime(),codeRun={name:'instruction',GetDirectTextPr:()=>({Lang:{Val:1031}})},resultRun={name:'result',GetDirectTextPr:()=>({Lang:{Val:1046}})};
 const field={InstructionItems:[{GetValue:()=> 'SAVEDATE',GetRun:()=>codeRun}],GetSeparateChar:()=>({GetRun:()=>resultRun})};
 assert.equal(c.AscWord.getSavedateInstructionRun(field),codeRun);
 assert.equal(c.AscWord.getSavedateInstructionRun({InstructionItems:[]}),null);
});
test('SAVEDATE rounds last-save timestamp down to minute precision like Word',()=>{
 const c=runtime();c.AscCommon.g_aCultureInfos={1033:{Name:'en-US'}};c.AscCommon.getShortDateFormat=()=> 'm/d/yyyy';c.AscCommon.getLongTimeFormat=()=> 'h:mm:ss;@';
 c.AscCommon.NumFormatType={WordFieldDate:1};let seen;
 c.AscCommon.oNumFormatCache={get:()=>({formatToWord(serial){seen=serial;return 'OUT';}})};
 const instruction=new c.CFieldInstructionParser().GetInstructionClass('SAVEDATE');
 const doc={Core:{asc_getModified:()=>new Date('2020-01-02T03:04:57Z')},GetDefaultLanguage:()=>1033};
 assert.equal(c.AscWord.calculateSavedate(instruction,doc,null).value,'OUT');
 const seconds=Math.round((seen-Math.floor(seen))*86400);
 assert.equal(seconds%60,0);
});
test('MERGEFORMAT keeps the result token style and uses field-code style after whitespace',()=>{
 const c=runtime();
 const field=parseForSegments(c,'SAVEDATE \\* MERGEFORMAT');
 const resultStyle={Color:'0000FF',Italic:true},codeStyle={Color:'FF0000',Bold:true};
 assert.equal(JSON.stringify(c.AscWord.savedateFormatSegments('2026-10-25 13:02:00',field,resultStyle,codeStyle)),JSON.stringify([
  {text:'2026-10-25',textPr:resultStyle},
  {text:' 13:02:00',textPr:codeStyle}
 ]));
 assert.equal(JSON.stringify(c.AscWord.savedateFormatSegments('JANEIRO',field,resultStyle,codeStyle)),JSON.stringify([
  {text:'JANEIRO',textPr:resultStyle}
 ]));
 const noMerge=parseForSegments(c,'SAVEDATE');
 assert.equal(JSON.stringify(c.AscWord.savedateFormatSegments('2026-10-25 13:02:00',noMerge,resultStyle,codeStyle)),JSON.stringify([
  {text:'2026-10-25 13:02:00',textPr:resultStyle}
 ]));
});
function parseForSegments(c,code){return new c.CFieldInstructionParser().GetInstructionClass(code);}
test('unsupported switches preserve safety and a new save is read on each update',()=>{
 const c=runtime();let instant=new Date('2020-01-02T03:04:05Z'),seen;
 c.AscCommon.g_aCultureInfos={1033:{}};c.AscCommon.NumFormatType={WordFieldDate:1};
 c.AscCommon.oNumFormatCache={get:()=>({formatToWord(serial){seen=serial;return 'january';}})};
 const doc={Core:{asc_getModified:()=>instant},GetDefaultLanguage:()=>1033};
 const parse=text=>new c.CFieldInstructionParser().GetInstructionClass(text);
 assert.equal(c.AscWord.calculateSavedate(parse('SAVEDATE \\h'),doc,null).reason,'UNSUPPORTED_SWITCH');
 const field=parse('SAVEDATE \\@ "MMMM" \\* Upper');
 assert.equal(c.AscWord.calculateSavedate(field,doc,null).value,'JANUARY');const before=seen;
 instant=new Date('2026-10-24T12:00:00Z');c.AscWord.calculateSavedate(field,doc,null);assert.ok(seen>before+2000);
 for(const [code,type] of [['DATE',11],['TIME',10],['REF Bookmark',8],['SAVEDATEX',8]])assert.equal(parse(code).GetType(),type);
});
test('complex update preserves cached formatting and CHARFORMAT uses first code-letter run',()=>{
 const c=runtime();c.CComplexField=function(){};
 const source=fs.readFileSync(path.join(root,'word/Editor/Paragraph/ComplexField.js'),'utf8');
 const start=source.indexOf('CComplexField.prototype.private_GetSavedateRun ='),end=source.indexOf('CComplexField.prototype.private_UpdateTIME =',start);
 vm.runInContext(source.slice(start,end),c);
 c.AscWord.calculateSavedate=()=>({ok:true,value:'DATE_VALUE'});
 const cached={Bold:false,Color:'0000FF'},code={Bold:true,Color:'FF0000'};let applied;
  const field={Instruction:{CharFormat:false,isMergeFormat:()=>true},LogicDocument:{},private_GetSavedateRun:()=>({GetDirectTextPr:()=>code}),GetSeparateChar:()=>({GetRun:()=>({})}),GetFieldValueTextPr:()=>cached,
  InstructionItems:[{GetValue:()=> ' ',GetRun:()=>null},{GetValue:()=> 'S',GetRun:()=>({GetDirectTextPr:()=>code})}],private_InsertSavedateSegments:segments=>{applied=segments;}};
 c.CComplexField.prototype.private_UpdateSAVEDATE.call(field);
 assert.equal(JSON.stringify(applied),JSON.stringify([{text:'DATE_VALUE',textPr:cached}]));
 field.Instruction.CharFormat=true;c.CComplexField.prototype.private_UpdateSAVEDATE.call(field);
 assert.equal(JSON.stringify(applied),JSON.stringify([{text:'DATE_VALUE',textPr:code}]));assert.equal(cached.Bold,false);
});
test('MERGEFORMAT inserts separate formatted runs into the field result',()=>{
 const c=runtime();c.CComplexField=function(){};
 const source=fs.readFileSync(path.join(root,'word/Editor/Paragraph/ComplexField.js'),'utf8');
 const start=source.indexOf('CComplexField.prototype.private_InsertSavedateSegments ='),end=source.indexOf('CComplexField.prototype.private_UpdateSAVEDATE =',start);
 vm.runInContext(source.slice(start,end),c);
 c.AscCommonWord={};c.AscCommonWord.CSelectedContent=function(){this.elements=[];this.Add=e=>this.elements.push(e);};
 c.AscCommonWord.CSelectedElement=function(paragraph,flag){this.paragraph=paragraph;this.flag=flag;};
 c.AscWord.Paragraph=function(){this.runs=[];this.AddToContentToEnd=run=>this.runs.push(run);};
 c.ParaRun=function(paragraph){this.pr=undefined;this.text='';this.Apply_Pr=pr=>{this.pr=pr;};this.AddText=text=>{this.text+=text;};};
 const first={Copy(){return {name:'result'};}},second={Copy(){return {name:'code'};}};let inserted;
 c.CComplexField.prototype.private_InsertSavedateSegments.call({private_InsertContent(content){inserted=content;}},[
  {text:'2026-10-25',textPr:first},{text:' 13:02:00',textPr:second}
 ]);
 assert.deepEqual(inserted.elements.map(element=>element.paragraph.runs.map(run=>({text:run.text,style:run.pr}))),[
  [{text:'2026-10-25',style:{name:'result'}},{text:' 13:02:00',style:{name:'code'}}]
 ]);
});
test('unsupported complex field is declined before selecting or replacing its cache',()=>{
 const c=runtime();c.CComplexField=function(){};
 const source=fs.readFileSync(path.join(root,'word/Editor/Paragraph/ComplexField.js'),'utf8');
 const helperStart=source.indexOf('CComplexField.prototype.private_GetSavedateRun ='),helperEnd=source.indexOf('CComplexField.prototype.private_InsertSavedateSegments =',helperStart);
 vm.runInContext(source.slice(helperStart,helperEnd),c);
 const start=source.indexOf('CComplexField.prototype.Update ='),end=source.indexOf('CComplexField.prototype.CalculateValue =',start);
 vm.runInContext(source.slice(start,end),c);
  const field={Instruction:{GetType:()=>80,Unsupported:true},private_GetSavedateRun:()=>null,private_CheckNestedComplexFields(){},private_UpdateInstruction(){},IsValid:()=>true,HaveValuePart:()=>true,
  GetSeparateChar:()=>({GetRun:()=>({})}),SelectFieldValue(){throw Error('Cache must not be selected');}};
 assert.equal(c.CComplexField.prototype.Update.call(field,false,false),false);
});
