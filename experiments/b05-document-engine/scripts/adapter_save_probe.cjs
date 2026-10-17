/* B05.0.2 synthetic save rejection UX; evidence remains private. */
const fs=require('node:fs'),path=require('node:path');
const {createRequire}=require('node:module');
const args=Object.fromEntries(process.argv.slice(2).reduce((p,v,i,a)=>i%2?p:[...p,[v.replace(/^--/,''),a[i+1]]],[]));
const pw=createRequire(path.join(args.playwright,'package.json'))('playwright');
const r=path.resolve(args.run),host='http://localhost:18086';
if(!r.startsWith(path.resolve(__dirname,'../.runtime')+path.sep))throw Error('Private run required');
if(fs.existsSync(path.join(r,'save-ux.json')))throw Error('Fresh output required');
const scenario=JSON.parse(fs.readFileSync(path.join(r,'scenarios.json')))[0];
async function current(){const x=await fetch(`${host}/lab/documents/${scenario.document}/current`);if(!x.ok)throw Error('Host not ready');return(await x.json()).version;}
async function main(){
 const browser=await pw.chromium.launch({headless:true,executablePath:args.chrome,args:['--no-proxy-server']});
 const context=await browser.newContext({viewport:{width:1440,height:1000}});let record={};
 try{
  const page=await context.newPage();await page.goto(host);await page.locator('option').last().waitFor({state:'attached'});await page.getByRole('checkbox').check();await page.locator('select').selectOption(scenario.document);
  async function open(){const p=page.waitForEvent('popup');await page.getByRole('button',{name:'Abrir no editor'}).click();const editor=await p;const frame=editor.frameLocator('iframe[name=office]').frameLocator('iframe[name=frameEditor]');await frame.locator('#statusbar').getByText(/Page 1 of/).first().waitFor({timeout:90000});return{editor,frame,inner:editor.frames().find(f=>f.name()==='frameEditor')};}
  record.before=await current();let {editor,frame,inner}=await open();
  await page.waitForTimeout(2000);record.ui_initial=await inner.locator('body').innerText();
  if(record.ui_initial.includes('not saved to storage'))throw Error('Fresh dataset required: engine restored a previous unsaved backup');
  await frame.locator('#area_id').focus();await editor.keyboard.press('Control+Home');await editor.keyboard.type('XYZ',{delay:80});await page.waitForTimeout(1000);await inner.evaluate(()=>window.Asc.editor.asc_Save());
  await page.waitForTimeout(10000);record.while_open=await current();record.ui_before_close=await inner.locator('body').innerText();await editor.screenshot({path:path.join(r,'save-before-close.png')});
  await editor.close();await page.waitForTimeout(10000);record.after_close=await current();
  ({editor,frame,inner}=await open());await page.waitForTimeout(5000);record.ui_after_reopen=await inner.locator('body').innerText();await editor.screenshot({path:path.join(r,'save-after-reopen.png')});
  // Read-only SVG/text layer and visible UI, no clipboard or additional edits.
  record.rendered_text=await inner.locator('body').innerText();record.after_reopen=await current();
  await editor.close();
 }finally{fs.writeFileSync(path.join(r,'save-ux.json'),JSON.stringify(record,null,2));await context.close();await browser.close();}
}
main().catch(e=>{fs.writeFileSync(path.join(r,'save-ux-error.txt'),String(e));console.error('Synthetic save UX failed; private error retained');process.exitCode=1;});
