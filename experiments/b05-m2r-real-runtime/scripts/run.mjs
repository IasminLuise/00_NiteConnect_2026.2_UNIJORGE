#!/usr/bin/env node
/** B05-M2R: opens the REAL Docs API returned by PR17, no JWT mocking and no official templates.
 * Writes only sanitized, shareable evidence. Browser editing is performed by the operator.
 */
import fs from 'node:fs';
import path from 'node:path';
import http from 'node:http';
import crypto from 'node:crypto';
import { once } from 'node:events';
import { webOrigin, checkDocument, checkSession, checkReadOnlySession, classify, shareableFailure, safeSignal, newRunId, operatorApiEndpoint, campaignResult, allowLocalRequest } from './core.mjs';

function argsOf(values) {
  const opts = {};
  for (let i=0;i<values.length;i+=2) {if(!values[i]?.startsWith('--') || values[i+1] == null) throw Error('Expected --option value pairs');const key=values[i].slice(2);if(key in opts)throw Error('Duplicated option');opts[key]=values[i+1];}
  return opts;
}
function boundedSeconds(raw) {const n=Number(raw||90);if(!Number.isSafeInteger(n)||n<25||n>240)throw Error('wait-seconds must be 25..240');return n;}
function insist(cond, message) {if(!cond)throw Error(message);}
function httpError(status) {const e=Error('Remote request rejected');e.code=`HTTP_${status}`;return e;}
async function fetchApi(api, token, pathname, method='GET', body=undefined, auth=true, timeoutMs=12000) {
  const headers={'Accept':'application/json'};
  if (auth) headers.Authorization=`Bearer ${token}`;
  if (body!==undefined)headers['Content-Type']='application/json';
  const resp=await fetch(api+pathname,{method,headers,body:body===undefined?undefined:JSON.stringify(body),signal:AbortSignal.timeout(timeoutMs),redirect:'manual'});
  if (resp.status>=300&&resp.status<400) throw Error('API redirect rejected');
  return resp;
}
async function jsonApi(api,token,p,method='GET',body) {const r=await fetchApi(api,token,p,method,body);if(!r.ok)throw httpError(r.status);return await r.json();}
async function allVersions(api,token,docId) {
  const all=[];
  for(let page=0;page<30;page++){
    const r=await jsonApi(api,token,`/api/v1/documents/${docId}/versions?page=${page}&size=100`);
    if(!Array.isArray(r.items)||!Number.isInteger(r.total))throw Error('Unexpected version list');
    all.push(...r.items);
    if(all.length>=r.total)return all;
    if(!r.items.length)throw Error('Empty page before total satisfied');
  }
  throw Error('Document has too many versions for bounded audit');
}
function makePage(conf,nonce) {
  const data=JSON.stringify({apiScriptUrl:conf.script,config:conf.config,nonce}).replace(/</g,'\\u003c').replace(/>/g,'\\u003e').replace(/&/g,'\\u0026');
  return `<!doctype html><html lang="pt-BR"><head><meta charset="utf-8"><meta name="referrer" content="no-referrer"><title>B05-M2R — editor sintético</title><style>html,body{height:100%;margin:0;overflow:hidden;font:14px system-ui;background:#f6f7f9;color:#18212f}body{display:flex;flex-direction:column}#top{flex:0 0 auto;padding:12px;display:flex;align-items:center;gap:16px}#editor-host{flex:1 1 auto;min-height:0;width:100%;position:relative}#editor,#editor-host>iframe{height:100%;width:100%;border:0;display:block}button{padding:9px 14px;cursor:pointer}button:disabled{opacity:.5}</style></head><body><div id="top"><strong>ONLYOFFICE real — B05-M2R</strong><span id="state">Carregando...</span><button id="complete" disabled>Terminei a edição — fechar editor</button></div><div id="editor-host"><div id="editor"></div></div><script type="application/json" id="bootstrap">${data}</script><script>
'use strict';const bootstrap=JSON.parse(document.getElementById('bootstrap').textContent);const state=document.getElementById('state');const button=document.getElementById('complete');let editor=null;let done=false;
function signal(type){fetch('/signal',{method:'POST',headers:{'Content-Type':'application/json','X-M2R-Nonce':bootstrap.nonce},body:JSON.stringify({type})}).catch(()=>{});}
function set(s){state.textContent=s;}
function bootstrapSdk(){if(!window.DocsAPI?.DocEditor){set('SDK indisponível');signal('editor_error');return;}
const cfg=bootstrap.config;cfg.events={...(cfg.events||{}),onAppReady(){set('Aplicação carregada');signal('app_ready');},onDocumentReady(){set('Documento aberto — edite apenas texto fictício');button.disabled=false;signal('document_ready');},onDocumentStateChange(e){if(e?.data===true)signal('document_modified');},onError(){set('Erro do motor — consultar evidências privadas');signal('editor_error');},onWarning(){set('Aviso do motor — verificar na tela');}};
try{editor=new DocsAPI.DocEditor('editor',cfg);}catch(e){set('Falha ao criar editor');signal('editor_error');}}
button.addEventListener('click',()=>{if(done)return;done=true;button.disabled=true;try{editor?.destroyEditor();set('Editor encerrado; aguardando callback real no backend');signal('finish');}catch(e){set('Falha ao encerrar editor');signal('editor_error');}});
const js=document.createElement('script');js.src=bootstrap.apiScriptUrl;js.onload=bootstrapSdk;js.onerror=()=>{set('Docs API script inacessível');signal('editor_error');};document.head.appendChild(js);
</script></body></html>`;
}
async function main() {
  const a=argsOf(process.argv.slice(2));
  insist(a['confirm-local-dev']==='YES','Explicit --confirm-local-dev YES required');
  insist(/^[0-9a-f-]{36}$/i.test(a['document-id']||''),'Valid document ID required');
  const tokenEnv=a['token-env']||'NITE_M2R_ACCESS_TOKEN';
  insist(/^[A-Z][A-Z0-9_]{4,80}$/.test(tokenEnv),'Invalid environment variable name');
  const token=process.env[tokenEnv];insist(token&&token.length>100,'Valid external Entra bearer required in env (not CLI)');
  const api=webOrigin(a['api-base'],a['allow-http-loopback']==='YES');
  const out=path.resolve(a.output||'');
  insist(out.split(path.sep).includes('.private') && !fs.existsSync(out),'Use a new output path beneath .private (never overwrite)');
  const seconds=boundedSeconds(a['wait-seconds']);
  const port=Number(a.port||5179);
  insist(Number.isSafeInteger(port)&&port>=1024&&port<=65535,'Invalid loopback port');
  fs.mkdirSync(out,{recursive:true,mode:0o700});
  const docId=a['document-id'];const prefix=`/api/v1/documents/${docId}`;
  const report={schema:'b05-m2r-v1',runId:newRunId(),source:'LIVE_ONLYOFFICE_DOCS_API',editorRuntimeReal:false,wordFidelityApproved:false,officialTemplateUsed:false,createdAt:new Date().toISOString(),checks:{},result:'INCONCLUSIVE'};
  let server;
  try{
    const before=checkDocument(await jsonApi(api,token,prefix));
    insist(before.id.toLowerCase()===docId.toLowerCase(),'Wrong document ID returned');
    const versionsBefore=await allVersions(api,token,docId);
    const noAuth=await fetchApi(api,token,prefix,'GET',undefined,false);
    report.checks.anonymousDenied=[401,403].includes(noAuth.status);
    insist(report.checks.anonymousDenied,'Anonymous document access unexpectedly allowed');
    const session=await jsonApi(api,token,`${prefix}/editor-sessions`,'POST',{expectedVersion:before.version,readOnly:false});
    const conf=checkSession(session);
    report.checks.signedSession=true;
    // Prove that the configured runtime serves the actual browser SDK; keep the script bytes private.
    const sdkResponse=await fetch(conf.script,{redirect:'manual',signal:AbortSignal.timeout(15000)});
    report.checks.docsApiScriptReachable=sdkResponse.status===200;
    await sdkResponse.body?.cancel();
    insist(report.checks.docsApiScriptReachable,'Docs API JavaScript endpoint unavailable');
    report.checks.callbackOriginConsistent=new URL(conf.callback).origin===new URL(conf.source).origin;
    insist(report.checks.callbackOriginConsistent,'Source/callback origins differ');
    // Negative test must not reveal original capability URL or create a document version.
    const tampered=new URL(conf.source);tampered.searchParams.set('token','INVALID');
    const tamperedResponse=await fetch(operatorApiEndpoint(tampered,api),{signal:AbortSignal.timeout(12000),redirect:'manual'});
    report.checks.tamperedCapabilityDenied=[401,403].includes(tamperedResponse.status);
    insist(report.checks.tamperedCapabilityDenied,'Invalid source capability unexpectedly accepted');
    const unauthCallback=await fetch(operatorApiEndpoint(conf.callback,api),{method:'POST',headers:{'Content-Type':'application/json'},body:'{}',signal:AbortSignal.timeout(12000),redirect:'manual'});
    report.checks.unsignedCallbackDenied=[401,403].includes(unauthCallback.status);
    insist(report.checks.unsignedCallbackDenied,'Unsigned callback unexpectedly accepted');
    const eventState={appReady:false,documentReady:false,modified:false,finished:false,error:false};
    const nonce=crypto.randomBytes(24).toString('hex');
    const page=makePage(conf,nonce);
    server=http.createServer(async(req,res)=>{
      if(!allowLocalRequest(req.headers.host,req.headers.origin,port,req.method)){res.statusCode=403;res.end();return;}
      const dest=new URL(req.url||'/', 'http://127.0.0.1');
      res.setHeader('Cache-Control','no-store');res.setHeader('X-Content-Type-Options','nosniff');res.setHeader('Referrer-Policy','no-referrer');
      if(req.method==='GET' && dest.pathname==='/') {res.setHeader('Content-Type','text/html; charset=utf-8');res.end(page);return;}
      if(req.method==='POST'&&dest.pathname==='/signal'){
        if(req.headers['x-m2r-nonce']!==nonce){res.statusCode=403;res.end();return;}
        let raw='';for await(const chunk of req){raw+=chunk;if(raw.length>1024){res.statusCode=413;res.end();return;}}
        let type;try{type=JSON.parse(raw).type;}catch{res.statusCode=400;res.end();return;}
        if(!safeSignal(type)){res.statusCode=400;res.end();return;}
        if(type==='app_ready')eventState.appReady=true;
        if(type==='document_ready')eventState.documentReady=true;
        if(type==='document_modified')eventState.modified=true;
        if(type==='editor_error')eventState.error=true;
        if(type==='finish')eventState.finished=true;
        res.setHeader('Content-Type','application/json');res.end('{"ok":true}');return;
      }
      res.statusCode=404;res.end();
    });
    server.listen(port,'127.0.0.1');await once(server,'listening');
    console.log(`Abrir no navegador local: http://127.0.0.1:${port}/`);
    console.log('Editar SOMENTE conteúdo sintético e clicar em “Terminei a edição”. Não fechar a janela antes de concluir.');
    console.log('Aguardando confirmação na interface (sem capturar documentos, JWT, URLs ou conteúdo)...');
    const maxOpenMs=6*60*1000, first=Date.now();
    while(!eventState.finished&&Date.now()-first<maxOpenMs&&!eventState.error)await new Promise(r=>setTimeout(r,650));
    report.checks.editorAppReady=eventState.appReady;
    report.checks.documentReady=eventState.documentReady;
    report.checks.editEventObserved=eventState.modified;
    report.checks.editorError=eventState.error;
    report.checks.operatorFinished=eventState.finished;
    report.editorRuntimeReal=eventState.documentReady && !eventState.error;
    // Vendor status=2 arrives asynchronously after editor closure; poll versions rather than assume Save is durable.
    let latest,versionsAfter;
    for(let i=0;i<seconds && !eventState.error;i+=2){
      await new Promise(r=>setTimeout(r,2000));
      latest=checkDocument(await jsonApi(api,token,prefix));
      versionsAfter=await allVersions(api,token,docId);
      if(versionsAfter.some(v=>!versionsBefore.some(b=>b.id===v.id)))break;
    }
    latest ??= checkDocument(await jsonApi(api,token,prefix));
    versionsAfter ??= await allVersions(api,token,docId);
    const state=classify(before,latest,versionsBefore,versionsAfter,eventState.documentReady,eventState.modified);
    report.persistence=state;
    // Use a clean read-only session to verify an accessible source becomes inaccessible upon explicit revocation.
    if(eventState.documentReady){
      const current=checkDocument(await jsonApi(api,token,prefix));
      const readonly=await jsonApi(api,token,`${prefix}/editor-sessions`,'POST',{expectedVersion:current.version,readOnly:true});
      const src=checkReadOnlySession(readonly);
      const alive=await fetch(operatorApiEndpoint(src,api),{redirect:'manual',signal:AbortSignal.timeout(15000)});
      report.checks.sourceValidBeforeRevocation=alive.status===200;
      const closed=await fetchApi(api,token,`${prefix}/editor-sessions/${readonly.id}/close`,'POST');
      report.checks.explicitRevocationCompleted=closed.status===204;
      const dead=await fetch(operatorApiEndpoint(src,api),{redirect:'manual',signal:AbortSignal.timeout(12000)});
      report.checks.sourceDeniedAfterRevocation=[401,403].includes(dead.status);
    }
    // PDF must bind to the exact requested available immutable version (not current by assumption).
    const available=versionsAfter.find(v=>v.id===latest.currentVersionId && v.status==='AVAILABLE') || versionsBefore.find(v=>v.id===before.currentVersionId && v.status==='AVAILABLE');
    if(available){
      const url=`${prefix}/versions/${available.id}/pdf`;
      const r=await fetchApi(api,token,url,'POST',undefined,true,45000);
      if(r.ok){
        const bytes=Buffer.from(await r.arrayBuffer());
        const hash=crypto.createHash('sha256').update(bytes).digest('hex');
        const r2=await fetchApi(api,token,url,'POST',undefined,true,45000);
        const bytes2=Buffer.from(await r2.arrayBuffer());
        report.checks.pdfMagic=bytes.subarray(0,5).toString('ascii')==='%PDF-';
        report.checks.pdfVersionHeader=r.headers.get('X-Document-Version')===available.id && r2.headers.get('X-Document-Version')===available.id;
        report.checks.pdfRepeatByteIdentical=r2.ok&&crypto.createHash('sha256').update(bytes2).digest('hex')===hash;
      }else report.checks.pdfHttpStatus=r.status;
    }
    // Errors may arrive after finish while persistence/PDF checks are awaiting I/O.
    report.checks.editorError=eventState.error;
    report.editorRuntimeReal=eventState.documentReady && !eventState.error;
    report.result=campaignResult(report,state.status);
    report.pending=['FULL_OOXML_WORD_PDF_FIDELITY','MULTI_ACTOR_REAL_COEDIT','VENDOR_CALLBACK_EXACT_REPLAY','REVOKED_CONCURRENT_EDITOR','WINDOWS_SYMLINK_SECURITY','LICENSING_OPERATIONS','OFFICIAL_TEMPLATES_PROHIBITED'];
    console.log(`Resultado: ${report.result}; candidato: ${state.status}. Consulte o JSON compartilhável.`);
  }catch(error){report.result='M2R_BLOCKED_OR_FAILED';report.failureCategory=shareableFailure(error);console.error(`Execução interrompida: ${report.failureCategory}. Inspecione configuração/ambiente local sem compartilhar URLs ou tokens.`);process.exitCode=1;}
  finally{
    if(server)await new Promise(resolve=>server.close(resolve));
    const target=path.join(out,'m2r-shareable.json');fs.writeFileSync(target,JSON.stringify(report,null,2)+'\n',{flag:'wx',mode:0o600});
    console.log('Relatório: '+target);
  }
}
main().catch(()=>{console.error('Falha de pré-condição. Verifique parâmetros sem expor credenciais.');process.exitCode=1;});
