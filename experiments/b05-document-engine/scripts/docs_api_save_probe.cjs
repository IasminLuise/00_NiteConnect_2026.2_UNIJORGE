/* B05.0.3 real Docs API browser checkpoints. Every output is private. */
const fs=require('node:fs'),path=require('node:path'),crypto=require('node:crypto');
const {createRequire}=require('node:module');
const args=Object.fromEntries(process.argv.slice(2).reduce((a,v,i,x)=>i%2?a:[...a,[v.replace(/^--/,''),x[i+1]]],[]));
const run=path.resolve(args.run),host=args.host||'http://localhost:18087';
if(!run.startsWith(path.resolve(__dirname,'../.runtime')+path.sep))throw Error('Private runtime required');
const output=path.join(run,'browser',args.tag||args.phase);fs.mkdirSync(output,{recursive:false});
const operator=fs.readFileSync(path.join(run,'operator'),'utf8');
const pw=createRequire(path.join(args.playwright,'package.json'))('playwright');
const timeline=[];const sleep=ms=>new Promise(r=>setTimeout(r,ms));
const alias=args.alias||'F01.docx';
if(!/^F0[1-9]\.docx$/.test(alias))throw Error('Unsupported private fixture alias');
async function create(mode,writable=true){const r=await fetch(host+'/docs-api/sessions',{method:'POST',headers:{Origin:host,'Content-Type':'application/json','X-Lab-Operator':operator},body:JSON.stringify({alias,mode,writable})});if(!r.ok)throw Error('Session creation rejected');const w=await r.json();fs.writeFileSync(path.join(output,'session.json'),JSON.stringify(w,null,2),{mode:0o600});return w;}
async function state(w){const r=await fetch(`${host}/docs-api/state/${w.id}?cap=${w.cap}`);if(!r.ok)throw Error('Host state unavailable');const s=await r.json();timeline.push({at:new Date().toISOString(),...s});return s;}
async function wait(w,predicate){for(let i=0;i<90;i++){const s=await state(w);if(predicate(s))return s;await sleep(1000);}throw Error('Expected host state not observed');}
async function capture(w,s,recovery=false){const suffix=recovery?`recovery/${w.id}`:`version/${w.id}/${s.headVersion}`;const r=await fetch(`${host}/docs-api/${suffix}?cap=${w.cap}`);if(!r.ok)throw Error('Immutable/recovery bytes unavailable');const data=Buffer.from(await r.arrayBuffer()),hash=crypto.createHash('sha256').update(data).digest('hex');if(hash!==r.headers.get('x-content-sha256'))throw Error('Host checksum mismatch');if(hash!==(recovery?s.candidateHash:s.lastHash))throw Error('State checksum mismatch');fs.writeFileSync(path.join(output,recovery?'candidate.docx':'saved.docx'),data);return hash;}
async function main(){
 const browser=await pw.chromium.launch({headless:true,executablePath:args.chrome,args:['--no-proxy-server']});const context=await browser.newContext({viewport:{width:1440,height:1000}});const page=await context.newPage();let w;
 try{
  const access=args.phase==='readonly';w=await create(args.phase==='strict'?'strict':args.phase==='refusal'?'reject':'structural',!access);
  await page.goto(`${host}/docs-api/editor/${w.id}?cap=${w.cap}`);
  const frame=()=>page.frames().find(f=>f.url().includes('/documenteditor/'));
  for(let i=0;i<90&&!frame();i++)await sleep(500);
  if(!frame())throw Error('Editor iframe unavailable');const inner=frame();
  await inner.locator('#statusbar').waitFor({state:'visible',timeout:90000});
  if(access){await page.screenshot({path:path.join(output,'readonly.png')});const r=await fetch(`${host}/docs-api/config/${w.id}?cap=${w.cap}`);const c=await r.json();if(c.document.permissions.edit!==false||c.editorConfig.mode!=='view')throw Error('Readonly config violated');await page.getByRole('button',{name:'Encerrar editor',exact:true}).click();await sleep(2000);await state(w);console.log('Docs API readonly opened');return;}
  await inner.locator('#area_id').focus();await page.keyboard.press('Control+Home');await page.keyboard.type('XYZ',{delay:80});await page.keyboard.press('Control+s');await sleep(4000);
  let s=await state(w);if(s.commits!==0||s.state==='committed')throw Error('Native save incorrectly confirmed by host before callback');
  await page.screenshot({path:path.join(output,'pending.png')});
  fs.writeFileSync(path.join(output,'editor-before-close.txt'),await inner.locator('body').innerText());
  if(args.phase==='force'){
   await page.getByRole('button',{name:'Salvar no NITE',exact:true}).click();s=await wait(w,s=>s.state==='committed'||s.state==='rejected'||s.state==='recovery-required');
  }else{await page.getByRole('button',{name:'Encerrar editor',exact:true}).click();s=await wait(w,s=>s.state==='committed'||s.state==='rejected'||s.state==='recovery-required');}
  await page.screenshot({path:path.join(output,'result.png')});
  fs.writeFileSync(path.join(output,'host-result.json'),JSON.stringify(s,null,2));
  if(args.phase==='control'||args.phase==='force'||args['expect-commit']==='true'){
   if(s.state!=='committed'||s.commits<1)throw Error('Control was not committed');await capture(w,s);
  }else{if(s.state!=='rejected'||s.commits!==0)throw Error('Rejection did not preserve host version');await capture(w,s,true);if(await page.locator('#host-state').innerText().then(t=>t.startsWith('Salvo no NITE')))throw Error('Wrapper falsely confirmed rejection');}
  console.log(`Docs API ${args.phase}: ${s.state}; bytes/checksum verified`);
 }finally{fs.writeFileSync(path.join(output,'timeline.json'),JSON.stringify(timeline,null,2));await context.close();await browser.close();}
}
main().catch(e=>{fs.writeFileSync(path.join(output,'error-private.txt'),String(e));console.error('Docs API browser probe failed; private diagnostics retained');process.exitCode=1;});
