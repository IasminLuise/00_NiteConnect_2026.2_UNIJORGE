import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import vm from 'node:vm';
import {execFileSync} from 'node:child_process';
import {patchBundle, savedateTimestamp} from '../native_sdk_patch.mjs';

const bundle=fs.readFileSync(process.env.B05_SDK_BUNDLE,'utf8');
function parser(source){
 const context=vm.createContext({AscWord:{JN:function(){},P5c:11,Ewc:10,Fsh:function(){this.ha=8;this.k7a=function(x){this.bookmark=x};this.XP=function(){return this.bookmark};this.mag=function(){};}},Asc:{},AscCommon:{},z:null});
 vm.runInContext(source.slice(source.indexOf('function Hv()'),source.indexOf('function le()')),context);
 vm.runInContext(source.slice(source.indexOf('function Mv()'),source.indexOf(';(function(a)',source.indexOf('function Sv('))),context);
 return text=>context.Nv(new context.Mv(),text);
}
test('stock parser reproduces SAVEDATE as REF; patched parser uses separate native type',()=>{
 assert.equal(parser(bundle)('SAVEDATE \\@ "yyyy-MM-dd HH:mm:ss"').ha,8);
 const field=parser(patchBundle(bundle))('SAVEDATE \\@ "yyyy-MM-dd HH:mm:ss"');
 assert.equal(field.ha,80);assert.equal(field.MO(),'yyyy-MM-dd HH:mm:ss');
 assert.equal(field.rJ(),'SAVEDATE \\@ "yyyy-MM-dd HH:mm:ss"');
});
test('DATE, TIME, REF and lookalikes retain their original semantics',()=>{
 const parse=parser(patchBundle(bundle));
 assert.equal(parse('DATE').ha,11);assert.equal(parse('TIME').ha,10);
 assert.equal(parse('REF Bookmark').ha,8);assert.equal(parse('SAVEDATEX').ha,8);
});
test('timestamp derives only from valid last-save metadata, never current time',()=>{
 const date=new Date('2020-01-02T00:04:05Z');
 const actual=savedateTimestamp(date);
 assert.equal(actual,Date.UTC(date.getFullYear(),date.getMonth(),date.getDate(),date.getHours(),date.getMinutes(),date.getSeconds()));
 for(const value of [null,undefined,'',0,'2020-02-30','invalid',new Date(NaN)])assert.equal(savedateTimestamp(value),null);
});
test('saved instant preserves civil day across UTC, Bahia and fractional-offset zones',()=>{
 const module=new URL('../native_sdk_patch.mjs',import.meta.url).href;
 for(const [zone,expected] of [['UTC','2020-01-02T02:04:05Z'],['America/Bahia','2020-01-01T23:04:05Z'],['Asia/Kathmandu','2020-01-02T07:49:05Z']]){
  const code=`import {savedateTimestamp} from ${JSON.stringify(module)};console.log(savedateTimestamp(new Date('2020-01-02T02:04:05Z')));`;
  const result=Number(execFileSync(process.execPath,['--input-type=module','-e',code],{env:{...process.env,TZ:zone},encoding:'utf8'}).trim());
  assert.equal(result,Date.parse(expected),zone);
 }
});
test('patch is pinned and rejects another or already modified vendor bundle',()=>{
 assert.throws(()=>patchBundle(bundle+' '),/Unsupported/);
 const patched=patchBundle(bundle);assert.throws(()=>patchBundle(patched),/Unsupported/);
 new vm.Script(patched);
});
test('simple-field serialization retains SAVEDATE rather than REF or DATE',()=>{
 const source=patchBundle(bundle),start=source.indexOf('z.xZb=function(){var a="";switch(this.kH)'),end=source.indexOf(';z.SK=function()',start);
 const context=vm.createContext({z:{},AscWord:{}});vm.runInContext(source.slice(start,end),context);
 assert.equal(context.z.xZb.call({kH:80,Arguments:[],AJb:[' \\@ "yyyy-MM-dd"']}),'SAVEDATE \\@ "yyyy-MM-dd"');
});
test('native calculator uses last-save metadata and SDK formatter; refuses absent metadata',()=>{
 const source=patchBundle(bundle);let observed;
 class SdkDate extends Date {C7(){throw Error("SDK local-day conversion must not reinterpret encoded UTC civil date");}}
 const context=vm.createContext({Date,Asc:{FH:SdkDate},AscCommon:{b3:{get(picture){return {l6c(serial){observed={picture,serial};return 'FORMATTED_SAVEDATE';}};}},IJa:{Src:1},Yra:{1033:{}}},AscCommonExcel:{R2c:86400}});
 vm.runInContext(source.slice(source.indexOf('function savedateTimestamp('),source.indexOf('function rw(')),context);
 const field={Qa:{Ix:{UBe:()=>new Date('2020-01-02T00:04:05Z')}},Xu:{MO:()=> 'yyyy-MM-dd HH:mm:ss'}};
 assert.equal(context.nativeCalculateSavedate(field),'FORMATTED_SAVEDATE');assert.equal(observed.picture,'yyyy-MM-dd HH:mm:ss');assert.ok(observed.serial<45000);
 assert.equal(context.nativeCalculateSavedate({...field,Qa:{}}),'');
});
test('F9 dispatch updates a simple SAVEDATE through its existing mapped-run path',()=>{
 const source=patchBundle(bundle),start=source.indexOf('z.We=function(){if(this.Fa||this.Fa.Ba)'),end=source.indexOf('z.xZb=function(){var a="";switch(this.kH)',start);
 let mapped=null;const context=vm.createContext({z:{},AscWord:{},nativeCalculateSavedate:()=> '2020-01-01 21:04:05',Fs:(_field,text)=>text});
 vm.runInContext(source.slice(start,end),context);
 context.z.We.call({Fa:{le:()=>({})},kH:80,SK:()=>({}),aa:[1],Sn(){},Ng(_index,text){mapped=text;}});
 assert.equal(mapped,'2020-01-01 21:04:05');
});
