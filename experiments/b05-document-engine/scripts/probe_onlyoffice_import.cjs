/* Read-only diagnostic for the pinned 10.0.0 build. Internal names are NOT a product API. */
const fs = require('node:fs');
const path = require('node:path');
const crypto = require('node:crypto');
const {createRequire} = require('node:module');
const args = Object.fromEntries(process.argv.slice(2).reduce((a,v,i,x)=>i%2?a:[...a,[v.replace(/^--/,''),x[i+1]]],[]));
const run = path.resolve(args.run);
if (!run.startsWith(path.resolve(__dirname,'../.runtime')+path.sep)) throw new Error('Private run required');
const out = path.join(run, 'import-probe.json');
if (fs.existsSync(out)) throw new Error('Fresh probe output required');
const pw = createRequire(path.join(args.playwright,'package.json'))('playwright');
const host = 'http://localhost:18086';
async function current(id) {
  const response = await fetch(`${host}/lab/documents/${id}/current`);
  if (!response.ok) throw new Error('Version unavailable');
  return (await response.json()).version;
}
async function main() {
  const scenario = JSON.parse(fs.readFileSync(path.join(run,'scenarios.json'),'utf8'))[0];
  const version = await current(scenario.document);
  const response = await fetch(`${host}/lab/documents/${scenario.document}/versions/${version}`);
  if (!response.ok) throw new Error('Immutable source unavailable');
  const sha256 = crypto.createHash('sha256').update(Buffer.from(await response.arrayBuffer())).digest('hex');
  const browser = await pw.chromium.launch({headless:true,executablePath:args.chrome,args:['--no-proxy-server']});
  const context = await browser.newContext();
  try {
    const page = await context.newPage();
    await page.goto(host);
    await page.locator('option').last().waitFor({state:'attached'});
    await page.locator('select').selectOption(scenario.document);
    await page.getByRole('checkbox').check();
    const popup = page.waitForEvent('popup');
    await page.getByRole('button',{name:'Abrir no editor'}).click();
    const editor = await popup;
    await editor.frameLocator('iframe[name=office]').frameLocator('iframe[name=frameEditor]')
      .locator('#statusbar').getByText(/Page 1 of/).first().waitFor({timeout:90000});
    const frame = editor.frames().find(f=>f.name()==='frameEditor');
    // The status bar appears before all header objects are attached to the model.
    await editor.waitForTimeout(2000);
    const model = await frame.evaluate(()=>{
      const queue=[window.Asc.editor.ye()], seen=new Set(), fields=[];
      let visited=0;
      for(let i=0;i<queue.length && i<30000;i++) {
        const obj=queue[i];
        if(!obj || typeof obj!=='object' || seen.has(obj)) continue;
        seen.add(obj); visited++;
        // xZb is GetInstructionLine in the exact installed sdk-all.js.
        if(typeof obj.xZb==='function') {
          const line=obj.xZb();
          if(typeof line==='string') fields.push({instruction:line,type:obj.kH});
        }
        for(const item of Object.values(Object.getOwnPropertyDescriptors(obj))) {
          const value=item.value; // Do not invoke arbitrary getters.
          if(value && typeof value==='object' && !seen.has(value) && queue.length<60000) queue.push(value);
        }
      }
      return {fields,visited,bounded:true};
    });
    if(model.fields.length===0) throw new Error('Pinned internal field probe unsupported; absence is not proof');
    await editor.close();
    const after = await current(scenario.document);
    fs.writeFileSync(out,JSON.stringify({source_version:version,source_sha256:sha256,
      version_after_close:after,source_unchanged:after===version,stage:'loaded-before-user-edit-or-save',model},null,2),{flag:'wx'});
    console.log('Private ONLYOFFICE import-state probe recorded');
  } finally { await context.close(); await browser.close(); }
}
main().catch(error=>{fs.writeFileSync(path.join(run,'import-probe-error-private.txt'),String(error));
  console.error('Import probe failed; private details recorded:',error.name);process.exitCode=1;});
