/** Deliberate synthetic-only local integration. Never transforms an existing immutable source response. */
import fs from 'node:fs';import path from 'node:path';import crypto from 'node:crypto';import {fileURLToPath} from 'node:url';
import {webOrigin,checkDocument} from '../b05-m2r-real-runtime/scripts/core.mjs';

const sha=bytes=>crypto.createHash('sha256').update(bytes).digest('hex');
const uuid=value=>typeof value==='string'&&/^[a-f0-9-]{36}$/i.test(value);
function privateInput(folder,entry){
 if(!entry||typeof entry.path!=='string'||typeof entry.sha256!=='string'||entry.path.includes('\\')||entry.path.includes(':')||entry.path.startsWith('/'))throw Error('Input integrity metadata invalid');
 const candidate=path.resolve(folder,entry.path),real=fs.realpathSync(candidate);
 if(fs.lstatSync(candidate).isSymbolicLink()||!real.startsWith(folder+path.sep)||path.extname(real)!=='.docx')throw Error('Input integrity path invalid');
 const size=fs.statSync(real).size;if(size<22||size>8*1024*1024)throw Error('Input integrity size invalid');
 const bytes=fs.readFileSync(real);if(sha(bytes)!==entry.sha256)throw Error('Input integrity mismatch');return bytes;
}
async function boundedBytes(response){const chunks=[];let size=0;for await(const bytes of response.body){size+=bytes.length;if(size>8*1024*1024)throw Error('Persisted head integrity limit');chunks.push(bytes);}return Buffer.concat(chunks);}

export async function provisionCase({apiBase,campaign,caseName,token,confirmed=false,fetchImpl=fetch}){
 const api=webOrigin(apiBase,true),origin=new URL(api);
 if(!['127.0.0.1','localhost','[::1]'].includes(origin.hostname))throw Error('API must be loopback');
 if(confirmed!==true)throw Error('Explicit local compatibility confirmation required');
 if(typeof token!=='string'||token.length<100)throw Error('External Entra bearer required');
 if(!['no_edit','edit_text','edit_table'].includes(caseName))throw Error('Invalid case');
 const folder=fs.realpathSync(path.resolve(campaign));if(!folder.split(path.sep).includes('.private'))throw Error('Private campaign required');
 const planPath=path.join(folder,'plan-private.json');if(fs.statSync(planPath).size>1024*1024)throw Error('Plan exceeds limit');
 const plan=JSON.parse(fs.readFileSync(planPath,'utf8'));
 if(plan.schema!=='b05-savedate-local-compat-v1'||plan.corpus!=='synthetic_only'||plan.local_only!==true||plan.gate_modified!==false||plan.native_f9_fixed!==false||plan.compatibility?.converted_fields!==3||plan.compatibility?.separate_template_versions_required!==true)throw Error('Unexpected local compatibility plan');
 const selected=plan.cases.find(c=>c.name===caseName);if(!selected)throw Error('Case absent');
 const output=path.join(folder,'cases',caseName,'provision-private.json');if(fs.existsSync(output))throw Error('Existing provisioning evidence; use a new campaign');
 const original=privateInput(folder,plan.original),compatible=privateInput(folder,plan.compatible),source=privateInput(folder,selected.source);
 if(sha(source)!==sha(compatible)||sha(original)===sha(compatible))throw Error('Input integrity lineage mismatch');
 const auth={Authorization:'Bearer '+token};
 async function request(p,method='POST',body){
  const multipart=body instanceof FormData;
  const response=await fetchImpl(api+p,{method,headers:{...auth,...(multipart?{}:{'Content-Type':'application/json'})},body:body===undefined?undefined:multipart?body:JSON.stringify(body),redirect:'manual',signal:AbortSignal.timeout(25000)});
  if(!response.ok)throw Error('HTTP_'+response.status);return response;
 }
 async function json(p,body){return (await request(p,'POST',body)).json();}
 const template=await json('/api/v1/document-templates',{title:'SAVEDATE compat local — '+caseName,official:false,reason:'Bancada sintética isolada; original e compatível em versões próprias'});
 if(!uuid(template.id)||template.official!==false||!Number.isSafeInteger(template.version)||template.version<0)throw Error('Invalid template response');
 async function templateVersion(bytes,expected,label){
  const form=new FormData();form.set('expectedVersion',String(expected));form.set('reason',label+'; fixture sintética; sem homologação ou exceção ao gate');form.set('file',new Blob([bytes],{type:'application/vnd.openxmlformats-officedocument.wordprocessingml.document'}),'synthetic-'+label+'.docx');
  const version=await json('/api/v1/document-templates/'+template.id+'/versions',form);if(!uuid(version.id)||!Number.isSafeInteger(version.revision))throw Error('Invalid template version');return version;
 }
 const originalVersion=await templateVersion(original,template.version,'original');
 const compatibleVersion=await templateVersion(compatible,template.version+1,'compatible');
 if(originalVersion.id===compatibleVersion.id||compatibleVersion.revision!==originalVersion.revision+1)throw Error('Distinct immutable template versions required');
 const doc=checkDocument(await json('/api/v1/documents',{title:'SAVEDATE compat local — '+caseName,context:'ADMINISTRATIVE',ownerId:null,projectId:null,eventId:null,demandReference:'SAVEDATE-COMPAT-LOCAL',templateVersionId:compatibleVersion.id,reason:'Documento sintético derivado explicitamente da versão compatível'}));
 const response=await request(`/api/v1/documents/${doc.id}/versions/${doc.currentVersionId}/download`,'GET');
 const persisted=await boundedBytes(response);
 if(response.headers.get('X-Document-Version')!==doc.currentVersionId||sha(persisted)!==sha(compatible))throw Error('Persisted head integrity mismatch');
 const receipt={schema:'b05-savedate-local-provision-v1',case:caseName,templateId:template.id,
  originalTemplateVersionId:originalVersion.id,compatibleTemplateVersionId:compatibleVersion.id,
  documentId:doc.id,initialVersion:doc.currentVersionId,documentRecordVersion:doc.version,
  originalSha256:sha(original),compatibleSha256:sha(compatible),initialHeadSha256:sha(persisted),
  sourcePreparedBeforeProvisioning:true,immutableSourceResponseTransformed:false,
  gateModified:false,nativeF9Fixed:false,officialApproved:false};
 fs.writeFileSync(output,JSON.stringify(receipt,null,2)+'\n',{flag:'wx',mode:0o600});return receipt;
}

if(process.argv[1]&&path.resolve(process.argv[1])===fileURLToPath(import.meta.url)){
 const args={};for(let i=2;i<process.argv.length;i+=2)args[process.argv[i]?.replace(/^--/,'')]=process.argv[i+1];
 const envName=args['token-env']||'NITE_M2R_ACCESS_TOKEN';
 if(!/^[A-Z][A-Z0-9_]{4,80}$/.test(envName)){console.error('Invalid token environment name');process.exitCode=1;}
 else provisionCase({apiBase:args['api-base'],campaign:args.campaign,caseName:args.case,token:process.env[envName],confirmed:args['confirm-local-compatibility']==='YES'})
  .then(()=>console.log('Original and compatible template versions provisioned locally; immutable head verified.'))
  .catch(()=>{console.error('Local provisioning stopped; inspect private environment and audit. No automatic rollback.');process.exitCode=1;});
}
