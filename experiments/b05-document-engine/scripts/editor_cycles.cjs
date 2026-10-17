/* Browser-driven CODE checkpoints. No traces/HAR or capability URLs are recorded. */
const fs = require('node:fs');
const path = require('node:path');
const {createRequire} = require('node:module');
const {spawnSync} = require('node:child_process');
const args = Object.fromEntries(process.argv.slice(2).reduce((pairs, value, i, values) => i % 2 ? pairs : [...pairs, [value.replace(/^--/, ''), values[i + 1]]], []));
const run = path.resolve(args.run);
const runtime = path.resolve(__dirname, '../.runtime') + path.sep;
if (!run.startsWith(runtime)) throw new Error('Private run directory required');
const pw = createRequire(path.join(args.playwright, 'package.json'))('playwright');
const host = args.host || 'http://localhost:18085';
async function current(id) {
  const response = await fetch(`${host}/lab/documents/${id}/current`);
  if (!response.ok) throw new Error('Current version unavailable');
  return (await response.json()).version;
}
async function saved(id, previous) {
  for (let attempt = 0; attempt < 90; attempt++) {
    const version = await current(id);
    if (version !== previous) return version;
    await new Promise(resolve => setTimeout(resolve, 500));
  }
  throw new Error('Persisted version did not advance');
}
async function main() {
  const scenarios = JSON.parse(fs.readFileSync(path.join(run, 'scenarios.json'), 'utf8'));
  const browser = await pw.chromium.launch({headless:true, executablePath:args.chrome, args:['--no-proxy-server']});
  const context = await browser.newContext({viewport:{width:1440,height:1000}});
  const page = await context.newPage();
  const results = [];
  try {
    await page.goto(host);
    await page.locator('option').last().waitFor({state:'attached', timeout:15000});
    await page.getByRole('checkbox').check();
    for (const scenario of scenarios.filter(s => s.scenario !== 'original' && (!args.scenario || s.scenario === args.scenario))) {
      const count = scenario.scenario === 'noop' ? 1 : 3;
      for (let cycle = 1; cycle <= count; cycle++) {
        const previous = await current(scenario.document);
        await page.locator('select').selectOption(scenario.document, {timeout:5000});
        const popup = page.waitForEvent('popup');
        await page.getByRole('button', {name:'Abrir no editor'}).click({timeout:5000});
        const editor = await popup;
        try {
          const office = editor.frameLocator('iframe[name=office]');
          await office.getByText(/Página 1 de/).first().waitFor({timeout:45000});
          const frame = editor.frames().find(f => f.name() === 'office');
          // Welcome is loaded asynchronously. Close through its visible slideshow buttons.
          const intro = office.frameLocator('iframe[name="iframe-welcome-form"]');
          try {
            await intro.locator('#slide-3-indicator').click({timeout:3000});
            await intro.locator('#slide-3-button').click({timeout:3000});
          } catch { /* A context remembers dismissal. */ }
          // Use the editor's normal composition/UNO transport, avoiding hidden-textarea focus races.
          await frame.evaluate(() => {
            window.app.map.sendUnoCommand('.uno:GoToStartOfDoc');
            window.app.map.sendUnoCommand('.uno:GoToEndOfPara');
          });
          await frame.evaluate(text => window.app.map._textInput._sendText(text), scenario.scenario === 'noop' ? 'XYZ' : ` B${cycle}`);
          // Wait for document state acknowledgement before issuing Undo/Save.
          await page.waitForTimeout(800);
          if (scenario.scenario === 'noop') await frame.evaluate(() => window.app.map.sendUnoCommand('.uno:Undo'));
          // Explicitly request serialization even when Undo returned the document to an unmodified state.
          await frame.evaluate(() => window.app.map.save(false, false));
          const version = await saved(scenario.document, previous);
          const out = path.join(run, 'outputs', `${scenario.alias.slice(0,3)}-${scenario.scenario}-${cycle}`);
          const captured = spawnSync(args.python, [path.join(__dirname, 'fidelity.py'), 'capture', '--host', host,
            '--document', scenario.document, '--previous', previous, '--version', version, '--out', out], {encoding:'utf8', timeout:90000});
          if (captured.status !== 0) throw new Error('Private capture failed');
          if (JSON.parse(captured.stdout).version !== version) throw new Error('Captured version differs from the observed save');
          results.push({alias:scenario.alias, scenario:scenario.scenario, cycle, version, previous, transport:'browser-composition-and-UNO'});
          fs.writeFileSync(path.join(run, 'browser-cycles.json'), JSON.stringify(results, null, 2));
          console.log(`${scenario.alias} ${scenario.scenario} cycle ${cycle}: persisted`);
        } finally { await editor.close(); }
      }
    }
  } finally { await context.close(); await browser.close(); }
}
main().catch(error => {
  fs.writeFileSync(path.join(run, 'browser-error-private.txt'), String(error));
  console.error('Editor scenario failed; details saved privately:', error.name);
  process.exitCode=1;
});
