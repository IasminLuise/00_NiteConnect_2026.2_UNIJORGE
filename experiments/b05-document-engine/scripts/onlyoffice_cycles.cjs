/* ONLYOFFICE comparison: physical editor input; save confirmation is a new WOPI version. */
const fs = require('node:fs');
const path = require('node:path');
const {createRequire} = require('node:module');
const {spawnSync} = require('node:child_process');
const args = Object.fromEntries(process.argv.slice(2).reduce((pairs,value,i,values)=>i%2?pairs:[...pairs,[value.replace(/^--/,''),values[i+1]]],[]));
const run=path.resolve(args.run), host='http://localhost:18086';
if(!run.startsWith(path.resolve(__dirname,'../.runtime')+path.sep)) throw new Error('Private run directory required');
const pw=createRequire(path.join(args.playwright,'package.json'))('playwright');
async function current(id){const r=await fetch(`${host}/lab/documents/${id}/current`);if(!r.ok)throw new Error('Version unavailable');return(await r.json()).version;}
async function saved(id,previous){for(let i=0;i<150;i++){const v=await current(id);if(v!==previous)return v;await new Promise(r=>setTimeout(r,500));}throw new Error('No persisted version after closing editor');}
async function main(){
 const scenarios=JSON.parse(fs.readFileSync(path.join(run,'scenarios.json'),'utf8'));
 const browser=await pw.chromium.launch({headless:true,executablePath:args.chrome,args:['--no-proxy-server']});
 const context=await browser.newContext({viewport:{width:1440,height:1000}});const page=await context.newPage();const results=[];
 try{
  await page.goto(host);await page.locator('option').last().waitFor({state:'attached',timeout:15000});await page.getByRole('checkbox').check();
  for(const s of scenarios.filter(s=>s.scenario!=='original')){
   const operations=s.scenario==='noop'?['insert','remove']:['1','2','3'];
   for(const operation of operations){
    const previous=await current(s.document);await page.locator('select').selectOption(s.document,{timeout:5000});
    const pp=page.waitForEvent('popup');await page.getByRole('button',{name:'Abrir no editor'}).click();const editor=await pp;
    try{
     const frame=editor.frameLocator('iframe[name=office]').frameLocator('iframe[name=frameEditor]');
     await frame.locator('#statusbar').getByText(/Page 1 of/).first().waitFor({timeout:60000});
     await frame.locator('#area_id').focus();await editor.keyboard.press('Control+Home');
     if(operation==='remove'){
      for(let k=0;k<3;k++)await editor.keyboard.press('Shift+ArrowRight');
      await editor.keyboard.press('Backspace');
     }else{
      if(operation!=='insert')await editor.keyboard.press('End');
      await editor.keyboard.type(operation==='insert'?'XYZ':` B${operation}`,{delay:80});
     }
     const inner=editor.frames().find(f=>f.name()==='frameEditor');
     await page.waitForTimeout(1000);await inner.evaluate(()=>window.Asc.editor.asc_Save());
     await page.waitForTimeout(1500);
    }finally{await editor.close();}
    const version=await saved(s.document,previous);const out=path.join(run,'outputs',`${s.alias.slice(0,3)}-${s.scenario}-${operation}`);
    const r=spawnSync(args.python,[path.join(__dirname,'fidelity.py'),'capture','--host',host,'--document',s.document,'--previous',previous,'--version',version,'--out',out,'--no-pdf'],{encoding:'utf8',timeout:90000});
    if(r.status!==0)throw new Error('Capture failed');if(JSON.parse(r.stdout).version!==version)throw new Error('Captured version differs');results.push({alias:s.alias,scenario:s.scenario,operation,previous,version});
    fs.writeFileSync(path.join(run,'browser-cycles.json'),JSON.stringify(results,null,2));console.log(`${s.alias} ${s.scenario} ${operation}: persisted`);
   }
  }
 }finally{await context.close();await browser.close();}
}
main().catch(e=>{fs.writeFileSync(path.join(run,'browser-error-private.txt'),String(e));console.error('ONLYOFFICE scenario failed; private details recorded:',e.name);process.exitCode=1;});
