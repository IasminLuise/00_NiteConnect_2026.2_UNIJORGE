import { randomUUID } from 'node:crypto';

export function webOrigin(raw, allowHttpLoopback = false) {
  const u = new URL(raw);
  if (u.username || u.password || u.search || u.hash || !['/', ''].includes(u.pathname)) throw Error('Origin must not include path, credentials, query or fragment');
  const local = ['127.0.0.1', 'localhost', '[::1]'].includes(u.hostname.toLowerCase());
  if (u.protocol !== 'https:' && !(allowHttpLoopback && local && u.protocol === 'http:')) throw Error('HTTPS required; HTTP permitted only for explicit loopback development');
  return u.origin;
}

export function checkDocument(doc) {
  if (!doc || !/^[0-9a-f-]{36}$/i.test(doc.id || '') || !Number.isSafeInteger(doc.version) || doc.version < 0 || !/^[0-9a-f-]{36}$/i.test(doc.currentVersionId || '')) throw Error('Unexpected document contract');
  if (doc.official !== false) throw Error('Official documents are prohibited');
  if (doc.archivedAt) throw Error('Archived documents are prohibited');
  return doc;
}

export function checkSession(response, now = Date.now()) {
  const config = response?.editor?.config;
  const script = response?.editor?.apiScriptUrl;
  if (!/^[0-9a-f-]{36}$/i.test(response?.id || '') || !config || typeof config !== 'object' || typeof script !== 'string') throw Error('Invalid Docs API session response');
  if (!config?.token || config.documentType !== 'word' || config.document?.fileType !== 'docx' || config.editorConfig?.mode !== 'edit') throw Error('Signed edit configuration missing');
  if (!Number.isFinite(Date.parse(response.expiresAt)) || Date.parse(response.expiresAt) <= now) throw Error('Expired edit session');
  const api = new URL(script);
  if (!api.pathname.endsWith('/web-apps/apps/api/documents/api.js') || api.search || api.hash) throw Error('Unexpected Docs API script URL');
  const source = new URL(config.document.url), callback = new URL(config.editorConfig.callbackUrl);
  if (!source.pathname.startsWith('/api/v1/document-provider/sources/') || !source.searchParams.get('token') || !callback.pathname.endsWith(`/cycles/${response.id}/callback`)) throw Error('Unexpected source/callback contract');
  if (config.document.permissions?.edit !== true) throw Error('Editor is not in edit mode');
  return { config, script, source, callback };
}

export function checkReadOnlySession(response, now = Date.now()) {
  const config = response?.editor?.config;
  if (!/^[0-9a-f-]{36}$/i.test(response?.id || '') || !config || config.editorConfig?.mode !== 'view' || config.document?.permissions?.edit !== false || !Number.isFinite(Date.parse(response.expiresAt)) || Date.parse(response.expiresAt) <= now) throw Error('Invalid read-only session');
  const source = new URL(config.document.url);
  if (!source.pathname.startsWith('/api/v1/document-provider/sources/') || !source.searchParams.get('token')) throw Error('Invalid read-only source');
  return source;
}

export function classify(before, after, versionsBefore, versionsAfter, ready, edited) {
  const known = new Set(versionsBefore.map(v=>v.id));
  const added = versionsAfter.filter(v=>!known.has(v.id));
  const available = added.filter(v=>v.status === 'AVAILABLE');
  const quarantined = added.filter(v=>v.status === 'QUARANTINED');
  let status = 'INCONCLUSIVE';
  if (!ready) status = 'EDITOR_NOT_READY';
  else if (!edited && !added.length) status = 'NO_EDIT_CONFIRMED';
  else if (added.length === 1 && available.length === 1 && after.currentVersionId === added[0].id) status = 'VERSION_AVAILABLE_NOT_EDITORIAL_APPROVAL';
  else if (added.length === 1 && quarantined.length === 1 && after.currentVersionId === before.currentVersionId) status = 'QUARANTINED_GUARD_ENFORCED';
  else if (added.length === 0) status = 'NO_PERSISTED_CALLBACK';
  else status = 'AMBIGUOUS_VERSION_RESULT';
  return { status, versionsCreated: added.length, available:available.length, quarantined:quarantined.length, headChanged:before.currentVersionId!==after.currentVersionId,
    quarantineCodes: quarantined.map(v=>v.rejectionCode).filter(x=>typeof x==='string' && /^[A-Z_]{1,48}$/.test(x)) };
}

export function operatorApiEndpoint(publicUrl, operatorOrigin) {
  // The public callback hostname may resolve inside Docker but not on Windows.
  // Send local security probes to the same backend via the operator's verified origin.
  const u = new URL(publicUrl);
  if (!u.pathname.startsWith('/api/v1/document-provider/') || u.username || u.password || u.hash) throw Error('Not a provider endpoint');
  return new URL(u.pathname + u.search, operatorOrigin);
}

export function shareableFailure(error) {
  // Never serialize exceptions carrying source URLs, callbacks, JWTs or bearer tokens.
  if (error?.name === 'AbortError') return 'TIMEOUT';
  if (/^HTTP_(400|401|403|404|409|422|429|500|502|503)$/.test(error?.code||'')) return error.code;
  return 'CHECK_PRIVATE_LOG_LOCALLY';
}

export function safeSignal(type) {return ['app_ready','document_ready','document_modified','editor_error','finish'].includes(type);}
export function newRunId() {return randomUUID();}

export function campaignResult(report, status) {
  const c=report.checks;
  const basic=report.editorRuntimeReal===true&&c.editorError===false&&c.anonymousDenied&&c.signedSession&&c.docsApiScriptReachable&&c.tamperedCapabilityDenied&&c.unsignedCallbackDenied&&c.documentReady&&c.operatorFinished&&c.editEventObserved&&c.sourceValidBeforeRevocation&&c.explicitRevocationCompleted&&c.sourceDeniedAfterRevocation;
  const pdf=c.pdfMagic&&c.pdfVersionHeader&&c.pdfRepeatByteIdentical;
  return basic&&pdf&&['VERSION_AVAILABLE_NOT_EDITORIAL_APPROVAL','QUARANTINED_GUARD_ENFORCED'].includes(status)?'M2R_INTEGRATION_DEMONSTRATED_NOT_EDITORIAL_APPROVAL':'M2R_NOT_CONCLUDED';
}
export function allowLocalRequest(host, origin, port, method) {
  if (host!==`127.0.0.1:${port}`) return false;
  return method==='GET' || (method==='POST' && origin===`http://127.0.0.1:${port}`);
}
