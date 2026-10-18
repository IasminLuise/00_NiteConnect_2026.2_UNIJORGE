/* Verify durable bytes after an externally controlled host restart. Private synthetic evidence only. */
const fs=require('node:fs'),path=require('node:path'),crypto=require('node:crypto');
const {createRequire}=require('node:module');
const args=Object.fromEntries(process.argv.slice(2).reduce((a,v,i,x)=>i%2?a:[...a,[v.replace(/^--/,''),x[i+1]]],[]));
const run=path.resolve(args.run),host=args.host||'http://localhost:18087';
if(!run.startsWith(path.resolve(__dirname,'../.runtime')+path.sep))throw Error('Private runtime required');
const out=path.join(run,'browser',args.tag||'recovery-after-restart');fs.mkdirSync(out,{recursive:false});
const read=tag=>JSON.parse(fs.readFileSync(path.join(run,'browser',tag,'session.json')));
const control=read(args.control),rejected=read(args.rejected);
const hash=bytes=>crypto.createHash('sha256').update(bytes).digest('hex');
async function get(url){const r=await fetch(host+url);if(!r.ok)throw Error('Host request rejected');return r;}
async function state(w){return(await get(`/docs-api/state/${w.id}?cap=${w.cap}`)).json();}
async function check(w,recovery){const s=await state(w),route=recovery?`recovery/${w.id}`:`version/${w.id}/${s.headVersion}`;
 const data=Buffer.from(await(await get(`/docs-api/${route}?cap=${w.cap}`)).arrayBuffer());
 const expected=fs.readFileSync(path.join(run,'browser',recovery?args.rejected:args.control,recovery?'candidate.docx':'saved.docx'));
 if(!data.equals(expected)||hash(data)!==(recovery?s.candidateHash:s.lastHash)||s.commits!==(recovery?0:1))throw Error('Durable checkpoint mismatch');return s;}
async function main(){
 const accepted=await check(control,false),refused=await check(rejected,true);
 const response=await fetch(`${host}/docs-api/reopen/${control.id}?cap=${control.cap}`,{method:'POST',headers:{Origin:host}});if(!response.ok)throw Error('Reopen refused');const next=await response.json();
 if(next.key===control.key||next.document!==control.document||next.sourceVersion!==accepted.headVersion)throw Error('Fresh key/version mismatch');
 fs.writeFileSync(path.join(out,'session.json'),JSON.stringify(next,null,2),{mode:0o600});
 const config=await(await get(`/docs-api/config/${next.id}?cap=${next.cap}`)).json();
 const secret=fs.readFileSync(path.join(run,'jwt-secret'),'utf8');
 const encode=o=>Buffer.from(JSON.stringify(o)).toString('base64url');
 const unsigned=encode({alg:'HS256',typ:'JWT'})+'.'+encode({operation:'download',payload:{url:config.document.url},exp:Math.floor(Date.now()/1000)+60});
 const token=unsigned+'.'+crypto.createHmac('sha256',secret).update(unsigned).digest('base64url');
 const source=await fetch(host+new URL(config.document.url).pathname+new URL(config.document.url).search,{headers:{Authorization:'Bearer '+token}});
 if(!source.ok||hash(Buffer.from(await source.arrayBuffer()))!==accepted.lastHash)throw Error('Reopened source differs from immutable version');
 const pw=createRequire(path.join(args.playwright,'package.json'))('playwright');const browser=await pw.chromium.launch({headless:true,executablePath:args.chrome,args:['--no-proxy-server']});
 try{const page=await browser.newPage({viewport:{width:1440,height:1000}});await page.goto(`${host}/docs-api/editor/${next.id}?cap=${next.cap}`);
  await page.frameLocator('iframe[name=frameEditor]').locator('#statusbar').waitFor({state:'visible',timeout:90000});await page.screenshot({path:path.join(out,'reopened.png')});
  await page.getByRole('button',{name:'Encerrar editor',exact:true}).click();
 }finally{await browser.close();}
 fs.writeFileSync(path.join(out,'result.json'),JSON.stringify({accepted,refused,newKey:true,sourceHashMatches:true},null,2));
 console.log('Durable accepted/rejected bytes and fresh-key source/reopening verified');
}
main().catch(e=>{fs.writeFileSync(path.join(out,'error-private.txt'),String(e));console.error('Recovery probe failed; private diagnostics retained');process.exitCode=1;});
