/** Local, version-pinned native SDK experiment; never patches a running production service. */
import crypto from 'node:crypto';
import fs from 'node:fs';
import path from 'node:path';
import {fileURLToPath} from 'node:url';

const SUPPORTED='1367619eca9d6348997ab46b67ee18d5389028af78832b12b43b656259e3d25c';

// The SDK formatter consumes UTC components. Encode the saved instant's local
// civil components once; never substitute the F9 wall clock for missing metadata.
export function savedateTimestamp(value){
 if(Object.prototype.toString.call(value)!=='[object Date]')return null;
 const time=value.getTime();if(!Number.isFinite(time))return null;
 return Date.UTC(value.getFullYear(),value.getMonth(),value.getDate(),value.getHours(),value.getMinutes(),value.getSeconds());
}

function nativeCalculateSavedate(field){
 const core=field.Qa&&field.Qa.Ix;
 const value=core&&typeof core.UBe==='function'?core.UBe():null;
 const time=savedateTimestamp(value);if(time===null)return '';
 let language=1033;const separator=field.nJ;
 if(separator&&separator.xq())language=separator.xq().xh(false).hg.$e;
 let picture=field.Xu.MO();
 if(!picture){
  // Explicitly bounded defaults; unsupported locales preserve the existing cache.
  picture=language===1033?'M/d/yyyy h:mm:ss AM/PM':language===1046?'dd/MM/yyyy HH:mm:ss':null;
  if(!picture)return '';
 }
 const format=AscCommon.b3.get(picture,AscCommon.IJa.Src);if(!format)return '';
 // Already encoded as UTC civil components. The SDK's getExcelDate(true)
 // converts through local midnight again and shifts the day at UTC-03:00.
 // Excel's 1900 date system includes its fictitious 1900-02-29.
 const serial=time/86400000+25569-(time<Date.UTC(1900,2,1)?1:0);
 return format.l6c(serial,15,AscCommon.Yra[language]);
}

export function patchBundle(source){
 if(crypto.createHash('sha256').update(source).digest('hex')!==SUPPORTED)throw Error('Unsupported SDK bundle; no patch applied');
 function replace(before,after){
  if(source.split(before).length!==2)throw Error('Unsupported SDK anchor');
  source=source.replace(before,after);
 }
 // New native type in the vendor parser, separate from DATE/TIME and REF.
 const nativeInstruction = String.raw`function B05SavedateInstruction(){Iv.call(this);this.ha=80;}
B05SavedateInstruction.prototype=Object.create(Iv.prototype);
B05SavedateInstruction.prototype.constructor=B05SavedateInstruction;
B05SavedateInstruction.prototype.rJ=function(){return this.uma||('SAVEDATE'+(this.yi?' \\@ "'+this.yi.replace(/"/g,'\\"')+'"':''));};`;
 replace('function le(){AscWord.JN.call(this);',nativeInstruction+'function le(){AscWord.JN.call(this);');
 replace('else if("DATE"===b)for(a.Tn=new Iv;', 'else if("SAVEDATE"===b)for(a.Tn=new B05SavedateInstruction;Ov(a);)Pv(a)&&"@"===a.Ew.charAt(1)&&(b=Rv(a),0<b.length&&a.Tn.Fv(b[0]));else if("DATE"===b)for(a.Tn=new Iv;');
 // Keep SAVEDATE when a simple field is reconstructed and exported.
 replace('case AscWord.P5c:b="DATE";break;', 'case 80:b="SAVEDATE";break;case AscWord.P5c:b="DATE";break;');
 // Both explicit F9 updates and calculated field-result queries use metadata.
 replace('case AscWord.P5c:(d=rw(this))&&this.Qa.Ye(d);break;', 'case AscWord.P5c:(d=rw(this))&&this.Qa.Ye(d);break;case 80:(d=nativeCalculateSavedate(this))&&this.Qa.Ye(d);break;');
 replace('case AscWord.P5c:b=rw(a);break;', 'case AscWord.P5c:b=rw(a);break;case 80:b=nativeCalculateSavedate(a);break;');
 replace('z.We=function(){if(this.Fa||this.Fa.Ba){var a=null;this.kH===AscWord.lkb?', 'z.We=function(){if(this.Fa||this.Fa.Ba){var a=null;if(this.kH===80)a=nativeCalculateSavedate({Qa:this.Fa.le(),Xu:this.SK(),nJ:null});else this.kH===AscWord.lkb?');
 replace('function rw(a,b){',savedateTimestamp.toString()+'\n'+nativeCalculateSavedate.toString()+'\nfunction rw(a,b){');
 return source;
}

if(process.argv[1]&&path.resolve(process.argv[1])===fileURLToPath(import.meta.url)){
 const [input,output]=process.argv.slice(2);
 if(!input||!output)throw Error('Usage: native_sdk_patch.mjs private-input private-output');
 const src=fs.realpathSync(input),dest=path.resolve(output),parent=fs.realpathSync(path.dirname(dest));
 if(!src.split(path.sep).includes('.private')||!parent.split(path.sep).includes('.private')||src===dest)throw Error('Distinct private paths required');
 const patched=patchBundle(fs.readFileSync(src,'utf8'));
 fs.writeFileSync(dest,patched,{flag:'wx'});
 console.log('Pinned SDK copy patched for local native SAVEDATE experiment.');
}
