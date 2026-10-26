import fs from 'node:fs';
import path from 'node:path';
import http from 'node:http';
import crypto from 'node:crypto';

const privateRoot=path.resolve(process.env.B05_PRIVATE_ROOT||'');
const controlRoot=path.resolve(process.env.B05_CONTROL_ROOT||'');
const outputRoot=path.resolve(process.env.B05_OUTPUT_ROOT||'');
const documentServer=(process.env.B05_DOCSERVER_ORIGIN||'http://127.0.0.1:18888').replace(/\/+$/,'');
const internalDocumentServer=(process.env.B05_INTERNAL_DOCSERVER_ORIGIN||documentServer).replace(/\/+$/,'');
const selfOrigin=(process.env.B05_SELF_ORIGIN||'http://host.docker.internal:'+Number(process.env.B05_HOST_PORT||5186)).replace(/\/+$/,'');
const port=Number(process.env.B05_HOST_PORT||5186);
const listenHost=process.env.B05_LISTEN_HOST||'0.0.0.0';
const documents=new Map([
 ['plano',{file:'plano-seed.docx',title:'Plano AV3 — controle SAVEDATE local'}],
 ['relatorio',{file:'relatorio-seed.docx',title:'Relatório semanal — controle SAVEDATE local'}],
]);
if(![privateRoot,controlRoot,outputRoot].every(value=>value.split(path.sep).includes('.private')))throw Error('Private roots required');
if(!Number.isInteger(port)||port<1024||port>65535)throw Error('Invalid host port');
for(const [id,item] of documents){
 const file=path.resolve(controlRoot,item.file);
 if(!file.startsWith(controlRoot+path.sep)||!fs.statSync(file).isFile())throw Error('Control document path invalid: '+id);
}
fs.mkdirSync(outputRoot,{recursive:false});
const evidenceFile=path.join(outputRoot,'callback-events-private.jsonl');
const sha=bytes=>crypto.createHash('sha256').update(bytes).digest('hex');
const safeRemote=address=>{
 const normalized=(address||'').replace(/^::ffff:/,'');
 return normalized==='127.0.0.1'||normalized==='::1'||normalized.startsWith('192.168.65.');
};
const json=(res,status,value)=>{const body=Buffer.from(JSON.stringify(value));res.writeHead(status,{'Content-Type':'application/json','Cache-Control':'no-store','Content-Length':body.length});res.end(body);};
const page=id=>{
 const config=JSON.stringify(documents.get(id));
 const key=JSON.stringify(crypto.randomUUID());
 return '<!doctype html><html><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>B05 SAVEDATE official local control</title><style>html,body{height:100%;width:100%;margin:0;overflow:hidden}body{display:flex;flex-direction:column}header{flex:0 0 40px;height:40px;background:#f4f6f8;padding:8px 12px;font:14px Arial;box-sizing:border-box}#editor{flex:1 1 auto;min-height:0;width:100%;position:relative}#editor>iframe{display:block;width:100%;height:100%;border:0}</style><script src="'+documentServer+'/web-apps/apps/api/documents/api.js"></script></head><body><header>B05 SAVEDATE — controle privado local: '+id+'</header><div id="editor"></div><script>const id='+JSON.stringify(id)+';const config='+config+';new DocsAPI.DocEditor("editor",{documentType:"word",type:"desktop",width:"100%",height:"100%",document:{fileType:"docx",key:'+key+',title:config.title,url:'+JSON.stringify(selfOrigin+'/file/'+id)+',permissions:{edit:true,download:true,print:true,copy:true,comment:false,review:false}},editorConfig:{mode:"edit",callbackUrl:"'+selfOrigin+'/callback/'+id+'",user:{id:"b05-local-check",name:"B05 Local Validation"}}});</script></body></html>';
};
const server=http.createServer(async(req,res)=>{
 if(!safeRemote(req.socket.remoteAddress)){json(res,403,{error:'loopback_and_local_container_only'});return;}
 try{
  const url=new URL(req.url||'/','http://'+(req.headers.host||'localhost'));
  const parts=url.pathname.split('/').filter(Boolean);
  if(req.method==='GET'&&url.pathname==='/'){res.writeHead(302,{Location:'/editor/plano'});res.end();return;}
  if(req.method==='GET'&&parts[0]==='editor'&&parts.length===2&&documents.has(parts[1])){
   const body=Buffer.from(page(parts[1]));res.writeHead(200,{'Content-Type':'text/html; charset=utf-8','Cache-Control':'no-store','Content-Length':body.length});res.end(body);return;
  }
  if(req.method==='GET'&&parts[0]==='file'&&parts.length===2&&documents.has(parts[1])){
   const item=documents.get(parts[1]);const bytes=fs.readFileSync(path.join(controlRoot,item.file));
   res.writeHead(200,{'Content-Type':'application/vnd.openxmlformats-officedocument.wordprocessingml.document','Content-Length':bytes.length,'Cache-Control':'no-store','X-B05-SHA256':sha(bytes)});res.end(bytes);return;
  }
  if(req.method==='POST'&&parts[0]==='callback'&&parts.length===2&&documents.has(parts[1])){
   let size=0;const chunks=[];
   for await(const chunk of req){size+=chunk.length;if(size>65536){json(res,413,{error:'callback_too_large'});return;}chunks.push(chunk);}
   const payload=JSON.parse(Buffer.concat(chunks).toString('utf8'));
   const event={document:parts[1],status:Number(payload.status),key:String(payload.key||''),hasUrl:typeof payload.url==='string',receivedAt:new Date().toISOString()};
   fs.appendFileSync(evidenceFile,JSON.stringify(event)+'\n',{flag:'a',mode:0o600});
   if([2,6].includes(event.status)&&typeof payload.url==='string'){
    const provided=new URL(payload.url);
    if(!['localhost','127.0.0.1','host.docker.internal','documentserver'].includes(provided.hostname)||!provided.pathname.startsWith('/'))throw Error('Callback download host rejected');
    const fetchUrl=new URL(provided.pathname+provided.search,internalDocumentServer);
    const response=await fetch(fetchUrl,{signal:AbortSignal.timeout(30000)});
    if(!response.ok)throw Error('Callback source HTTP_'+response.status);
    const bytes=Buffer.from(await response.arrayBuffer());
    if(bytes.length<22||bytes.length>8*1024*1024)throw Error('Callback DOCX size rejected');
    const out=path.join(outputRoot,parts[1]+'-'+String(Date.now())+'-'+crypto.randomBytes(3).toString('hex')+'.docx');
    fs.writeFileSync(out,bytes,{flag:'wx',mode:0o600});
    fs.appendFileSync(evidenceFile,JSON.stringify({...event,savedFile:path.basename(out),sha256:sha(bytes),size:bytes.length})+'\n',{flag:'a',mode:0o600});
   }
   json(res,200,{error:0});return;
  }
  if(req.method==='GET'&&url.pathname==='/health'){json(res,200,{ok:true,documents:documents.size});return;}
  json(res,404,{error:'not_found'});
 }catch(error){
  fs.appendFileSync(evidenceFile,JSON.stringify({error:String(error?.message||'callback_error'),receivedAt:new Date().toISOString()})+'\n',{flag:'a',mode:0o600});
  json(res,500,{error:'local_callback_failed'});
 }
});
server.listen(port,listenHost,()=>process.stdout.write(JSON.stringify({port,listenHost,documents:documents.size,privateOnly:true})+'\n'));
process.on('SIGINT',()=>server.close(()=>process.exit(0)));
process.on('SIGTERM',()=>server.close(()=>process.exit(0)));
