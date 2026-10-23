#!/usr/bin/env node
/** Optional, deliberate preparation of a synthetic, nonofficial document in a disposable local database. */
import fs from 'node:fs';
import path from 'node:path';
import {webOrigin,checkDocument} from './core.mjs';

function options(argv){const x={};for(let i=0;i<argv.length;i+=2){if(!argv[i]?.startsWith('--')||!argv[i+1])throw Error('Expected --name value');x[argv[i].slice(2)]=argv[i+1];}return x;}
async function main(){
 const a=options(process.argv.slice(2));
 if(a['confirm-disposable-dev']!=='YES')throw Error('--confirm-disposable-dev YES is mandatory');
 const api=webOrigin(a['api-base'],a['allow-http-loopback']==='YES');
 const key=a['token-env']||'NITE_M2R_ACCESS_TOKEN';
 if(!/^[A-Z][A-Z0-9_]{4,80}$/.test(key)||!process.env[key])throw Error('Use an external Entra bearer token from an environment variable');
 const token=process.env[key];
 const fixture=path.resolve(a.fixture||'');const out=path.resolve(a.output||'');
 if(!fixture.split(path.sep).includes('.private')||!out.split(path.sep).includes('.private')||fs.existsSync(out)||!fixture.endsWith('.docx'))throw Error('Fixture and new output must be in .private');
 const bytes=fs.readFileSync(fixture);if(bytes.length<100||bytes.length>8*1024*1024)throw Error('Invalid fixture bytes');
 const auth={Authorization:`Bearer ${token}`};
 async function request(pathname,body,headers={},multipart=false){
  const res=await fetch(api+pathname,{method:'POST',headers:{...auth,...(multipart?{}:{'Content-Type':'application/json'}),...headers},body:multipart?body:JSON.stringify(body),redirect:'manual',signal:AbortSignal.timeout(25000)});
  if(!res.ok)throw Error(`Provisioning rejected with HTTP ${res.status}; check private environment and permissions`);
  return res.json();
 }
 const t=await request('/api/v1/document-templates',{title:'B05 M2R — Modelo totalmente sintético',official:false,reason:'Ensaio local descartável M2R'});
 if(t.official!==false||!t.id||!Number.isSafeInteger(t.version))throw Error('Invalid created template');
 const form=new FormData();form.set('expectedVersion',String(t.version));form.set('reason','Fixture sintética para runtime ONLYOFFICE');form.set('file',new Blob([bytes],{type:'application/vnd.openxmlformats-officedocument.wordprocessingml.document'}),'m2r-synthetic.docx');
 const v=await request(`/api/v1/document-templates/${t.id}/versions`,form,{},true);
 if(!v.id)throw Error('Template version absent');
 const doc=checkDocument(await request('/api/v1/documents',{title:'B05 M2R — Edição sintética',context:'ADMINISTRATIVE',ownerId:null,projectId:null,eventId:null,demandReference:'M2R-LOCAL',templateVersionId:v.id,reason:'Teste isolado de motor real'}));
 fs.mkdirSync(path.dirname(out),{recursive:true});fs.writeFileSync(out,JSON.stringify({schema:'b05-m2r-provision-private-v1',documentId:doc.id,initialVersion:doc.currentVersionId,documentRecordVersion:doc.version},null,2)+'\n',{flag:'wx',mode:0o600});
 console.log(`Documento fictício criado na base local: ${doc.id}`);
 console.log('Anotação PRIVADA: '+out);
}
main().catch(()=>{console.error('Provisionamento interrompido. Conferir ambiente local; nenhum rollback automático do que já foi persistido.');process.exitCode=1;});
