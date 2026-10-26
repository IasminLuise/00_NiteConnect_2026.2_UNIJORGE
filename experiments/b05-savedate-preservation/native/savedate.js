/* SPDX-License-Identifier: AGPL-3.0-only */
/* NITE local SDK extension. Loaded after ComplexFieldInstruction.js. */
(function(window){
 "use strict";
 const word=window.AscWord;
 word.fieldtype_SAVEDATE=0x0050; // SDK extension value; Word's wdFieldSaveDate is 22.
 function FieldInstructionSAVEDATE(){
  CFieldInstructionDATE.call(this);
  this.Unsupported=false;this.CharFormat=false;this.TextCase=null;
 }
 FieldInstructionSAVEDATE.prototype=Object.create(CFieldInstructionDATE.prototype);
 FieldInstructionSAVEDATE.prototype.constructor=FieldInstructionSAVEDATE;
 FieldInstructionSAVEDATE.prototype.Type=word.fieldtype_SAVEDATE;
 FieldInstructionSAVEDATE.prototype.ToString=function(){return this.InstructionLine||'SAVEDATE';};
 word.FieldInstructionSAVEDATE=FieldInstructionSAVEDATE;
 function getSavedateInstructionRun(field){
  const items=field&&field.InstructionItems;
  if(!items)return null;
  for(let i=0;i<items.length;i++){
   const item=items[i];
   if(item&&item.GetValue&&/[A-Za-z]/.test(item.GetValue())&&item.GetRun())return item.GetRun();
  }
  return null;
 }
 CFieldInstructionParser.prototype.private_ReadSAVEDATE=function(){
  const field=this.Result=new FieldInstructionSAVEDATE();
  field.SetInstructionLine(this.Line);
  while(this.private_ReadNext()){
   if(!this.private_IsSwitch()){field.Unsupported=true;continue;}
   const option=this.Buffer.toLowerCase();
   if(option==='\\@'){
    const args=this.private_ReadArguments();
    if(args.length!==1||!args[0])field.Unsupported=true;else field.SetFormat(args[0]);
   }else if(option==='\\*'){
    const args=this.private_ReadArguments();
    if(args.length!==1){field.Unsupported=true;continue;}
    const flag=args[0].toUpperCase();
    if(flag==='MERGEFORMAT'){field.addGeneralSwitches(['MERGEFORMAT']);field.CharFormat=false;}
    else if(flag==='CHARFORMAT')field.CharFormat=true;
    else if(['UPPER','LOWER','CAPS','FIRSTCAP'].indexOf(flag)>=0)field.TextCase=flag;
    else field.Unsupported=true;
   }else field.Unsupported=true;
  }
 };
 function calculateSavedate(instruction,document,run){
  if(instruction.Unsupported)return {ok:false,reason:'UNSUPPORTED_SWITCH'};
  const core=document&&document.Core;
  const date=core&&typeof core.asc_getModified==='function'?core.asc_getModified():null;
  if(Object.prototype.toString.call(date)!=='[object Date]'||!Number.isFinite(date.getTime()))return {ok:false,reason:'MISSING_LAST_SAVE'};
  const directProperties=run&&run.GetDirectTextPr?run.GetDirectTextPr():null;
  const compiledProperties=run&&run.Get_CompiledPr?run.Get_CompiledPr(false):null;
  const directLanguage=directProperties&&directProperties.Lang?directProperties.Lang.Val:null;
  const compiledLanguage=compiledProperties&&compiledProperties.Lang?compiledProperties.Lang.Val:null;
  const runLanguage=directLanguage!==null&&directLanguage!==undefined?directLanguage:compiledLanguage;
  const documentLanguage=document&&document.GetDefaultLanguage?document.GetDefaultLanguage():null;
  const candidates=[runLanguage,documentLanguage].filter(value=>value!==null&&value!==undefined);
  let language=candidates.find(value=>AscCommon.g_aCultureInfos[value]!==undefined);
  if(language===undefined){
   if(candidates.length)return {ok:false,reason:'UNSUPPORTED_LANGUAGE'};
   language=1033;
  }
  const culture=AscCommon.g_aCultureInfos[language];
  if(!culture)return {ok:false,reason:'UNSUPPORTED_LANGUAGE'};
  let picture=instruction.GetFormat();
  if(!picture){
   const day=AscCommon.getShortDateFormat(culture).replace(/m/g,'M').split('/').join(culture.DateSeparator||'/');
   let clock=AscCommon.getLongTimeFormat(culture).split(';')[0];
   if(!culture.UseAMPM)clock=clock.replace(/h+/g,'HH');
   picture=day+' '+clock;
  }
  const format=AscCommon.oNumFormatCache.get(picture,AscCommon.NumFormatType.WordFieldDate);
  if(!format)return {ok:false,reason:'UNSUPPORTED_PICTURE'};
  const civil=Date.UTC(date.getFullYear(),date.getMonth(),date.getDate(),date.getHours(),date.getMinutes(),0);
  const serial=civil/86400000+25569-(civil<Date.UTC(1900,2,1)?1:0);
  let value=format.formatToWord(serial,15,culture);
  if(typeof value!=='string'||!value)return {ok:false,reason:'EMPTY_FORMATTED_RESULT'};
  if(instruction.TextCase==='UPPER')value=value.toLocaleUpperCase(culture.Name);
  else if(instruction.TextCase==='LOWER')value=value.toLocaleLowerCase(culture.Name);
  else if(instruction.TextCase==='FIRSTCAP')value=value.replace(/\p{L}/u,c=>c.toLocaleUpperCase(culture.Name));
  else if(instruction.TextCase==='CAPS')value=value.replace(/\p{L}[\p{L}\p{M}]*/gu,s=>s[0].toLocaleUpperCase(culture.Name)+s.slice(1).toLocaleLowerCase(culture.Name));
  return {ok:true,value:value,language:language,picture:picture};
 }
 function savedateFormatSegments(value,instruction,resultTextPr,codeTextPr){
  if(instruction&&instruction.isMergeFormat&&instruction.isMergeFormat()&&codeTextPr){
   const separator=value.search(/\s/);
   if(separator>0)return [
    {text:value.slice(0,separator),textPr:resultTextPr},
    {text:value.slice(separator),textPr:codeTextPr}
   ];
  }
  return [{text:value,textPr:resultTextPr}];
 }
 word.calculateSavedate=calculateSavedate;
 word.getSavedateInstructionRun=getSavedateInstructionRun;
 word.savedateFormatSegments=savedateFormatSegments;
})(window);
