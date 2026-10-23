import test from 'node:test';
import assert from 'node:assert/strict';
import {webOrigin,checkDocument,checkSession,checkReadOnlySession,classify,safeSignal,shareableFailure,operatorApiEndpoint,campaignResult,allowLocalRequest} from '../scripts/core.mjs';
const id='00000000-0000-4000-8000-000000000001';const other='00000000-0000-4000-8000-000000000002';
const doc=()=>({id,version:2,official:false,archivedAt:null,currentVersionId:other});
function session(ro=false){return {id,expiresAt:'2099-01-01T00:00:00Z',editor:{apiScriptUrl:'https://docs.example.test/web-apps/apps/api/documents/api.js',config:{token:'SIGNED',documentType:'word',document:{key:'m2r',fileType:'docx',title:'Synthetic',permissions:{edit:!ro},url:`https://api.example.test/api/v1/document-provider/sources/${other}?token=SECRET`},editorConfig:{mode:ro?'view':'edit',callbackUrl:`https://api.example.test/api/v1/document-provider/cycles/${id}/callback`}}}};}
test('origin rejects creds/query/fragment/subpaths and HTTP private networks',()=>{
 assert.equal(webOrigin('https://api.example.test'),'https://api.example.test');
 for(const x of ['https://u:p@api.example.test','https://api.example.test/path','https://api.example.test/?token=a','http://api.example.test','http://192.168.5.6'])assert.throws(()=>webOrigin(x,true));
 assert.equal(webOrigin('http://127.0.0.1:8090',true),'http://127.0.0.1:8090');
 assert.throws(()=>webOrigin('http://127.0.0.1:8090'));
});
test('reject official, archived and invalid documents',()=>{
 assert.equal(checkDocument(doc()).version,2);
 assert.throws(()=>checkDocument({...doc(),official:true}));
 assert.throws(()=>checkDocument({...doc(),archivedAt:'2026-10-23'}));
 assert.throws(()=>checkDocument({...doc(),currentVersionId:null}));
});
test('session requires actual signed Docs API, source, callback, mode and TTL',()=>{
 assert.equal(checkSession(session()).config.documentType,'word');
 assert.throws(()=>checkSession(session(true)));
 const missing=session();delete missing.editor.config.token;assert.throws(()=>checkSession(missing));
 const tampered=session();tampered.editor.apiScriptUrl='https://evil.test/a.js';assert.throws(()=>checkSession(tampered));
 const expired=session();expired.expiresAt='2001-01-01T00:00:00Z';assert.throws(()=>checkSession(expired));
 const wrong=session();wrong.editor.config.editorConfig.callbackUrl='https://api.example.test/not-source';assert.throws(()=>checkSession(wrong));
});
test('read-only source requires mode=view and signed capability',()=>{
 assert.ok(checkReadOnlySession(session(true)) instanceof URL);
 assert.throws(()=>checkReadOnlySession(session()));
});
test('persisted available vs quarantined vs absent are distinct',()=>{
 const b=doc(),head='00000000-0000-4000-8000-000000000003';
 const old=[{id:other,status:'AVAILABLE'}];const add={id:head,status:'AVAILABLE'};
 assert.equal(classify(b,{...b,currentVersionId:head},old,[...old,add],true,true).status,'VERSION_AVAILABLE_NOT_EDITORIAL_APPROVAL');
 assert.equal(classify(b,b,old,[...old,{id:head,status:'QUARANTINED',rejectionCode:'OOXML_PROTECTED_CHANGE'}],true,true).status,'QUARANTINED_GUARD_ENFORCED');
 assert.equal(classify(b,b,old,old,true,true).status,'NO_PERSISTED_CALLBACK');
 assert.equal(classify(b,b,old,old,false,false).status,'EDITOR_NOT_READY');
});
test('never leak throwable containing bearer/source URL and signals are allowlisted',()=>{
 assert.equal(shareableFailure(Error('https://site/?token=SECRET')),'CHECK_PRIVATE_LOG_LOCALLY');
 assert.equal(shareableFailure({code:'HTTP_403'}),'HTTP_403');
 assert.equal(safeSignal('document_ready'),true);assert.equal(safeSignal('leakToken'),false);
});

test('operator probes use only the real backend path when Docker-only hostname is not resolvable',()=>{
 const src=`https://host.docker.internal:9443/api/v1/document-provider/sources/${other}?token=SECRET`;
 assert.equal(operatorApiEndpoint(src,'http://127.0.0.1:8080').href,`http://127.0.0.1:8080/api/v1/document-provider/sources/${other}?token=SECRET`);
 assert.throws(()=>operatorApiEndpoint('https://evil.test/other?token=SECRET','http://127.0.0.1:8080'));
});

test('campaign cannot approve a late editor error or an unproven real runtime',()=>{
 const checks=Object.fromEntries(['anonymousDenied','signedSession','docsApiScriptReachable','tamperedCapabilityDenied','unsignedCallbackDenied','documentReady','operatorFinished','editEventObserved','sourceValidBeforeRevocation','explicitRevocationCompleted','sourceDeniedAfterRevocation','pdfMagic','pdfVersionHeader','pdfRepeatByteIdentical'].map(k=>[k,true]));
 const report={checks:{...checks,editorError:false},editorRuntimeReal:true};
 const status='QUARANTINED_GUARD_ENFORCED';
 assert.equal(campaignResult(report,status),'M2R_INTEGRATION_DEMONSTRATED_NOT_EDITORIAL_APPROVAL');
 assert.equal(campaignResult({...report,checks:{...checks,editorError:true}},status),'M2R_NOT_CONCLUDED');
 assert.equal(campaignResult({...report,editorRuntimeReal:false},status),'M2R_NOT_CONCLUDED');
 assert.equal(campaignResult(report,'NO_PERSISTED_CALLBACK'),'M2R_NOT_CONCLUDED');
});
test('local page and signals reject rebinding hosts and foreign origins',()=>{
 assert.equal(allowLocalRequest('127.0.0.1:5179',undefined,5179,'GET'),true);
 assert.equal(allowLocalRequest('127.0.0.1:5179','http://127.0.0.1:5179',5179,'POST'),true);
 for(const host of ['attacker.example:5179','127.0.0.1:5180','127.0.0.1:5179.attacker.example',undefined])
  assert.equal(allowLocalRequest(host,undefined,5179,'GET'),false);
 for(const origin of ['https://attacker.example','null','http://localhost:5179',undefined])
  assert.equal(allowLocalRequest('127.0.0.1:5179',origin,5179,'POST'),false);
});
