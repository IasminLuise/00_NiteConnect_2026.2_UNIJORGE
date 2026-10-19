/* Real SDK prequalification. Synthetic inputs and detailed evidence stay private. */
const fs = require('node:fs'), path = require('node:path'), crypto = require('node:crypto');
const { createRequire } = require('node:module');
const args = Object.fromEntries(process.argv.slice(2).reduce((out, value, i, all) => i % 2 ? out : [...out, [value.replace(/^--/, ''), all[i + 1]]], []));
const run = path.resolve(args.run), root = path.resolve(__dirname, '../.private');
if (!run.startsWith(root + path.sep) || !['superdoc', 'apryse'].includes(args.engine)) throw Error('Private run and known engine required');
const out = path.join(run, 'browser', args.tag || `${args.engine}-noop`);
fs.mkdirSync(path.dirname(out), { recursive: true }); fs.mkdirSync(out);
const source = path.resolve(args.source || path.join(run, 'fixtures/compat-synthetic.docx'));
if (!source.startsWith(run + path.sep)) throw Error('Input must belong to the private synthetic run');
const phase = args.phase || 'noop';
if (!['noop', 'edit', 'remove'].includes(phase)) throw Error('Unknown phase');
const settle = Number(args.settle || 1500);
if (!Number.isInteger(settle) || settle < 0 || settle > 10000) throw Error('Settle must be 0..10000 ms');
const record = { engine: args.engine, sourceHash: crypto.createHash('sha256').update(fs.readFileSync(source)).digest('hex'), requests: [], errors: [], status: 'STARTED' };
const origin = 'http://127.0.0.1:5173';
const pw = createRequire(path.join(args.playwright, 'package.json'))('playwright');
async function main() {
  const browser = await pw.chromium.launch({ headless: true, executablePath: args.chrome });
  const context = await browser.newContext({ viewport: { width: 1440, height: 1100 }, acceptDownloads: true });
  let page;
  try {
    await context.routeWebSocket('**/*', socket => {
      const url = new URL(socket.url()); const allowed = url.origin === 'ws://127.0.0.1:5173';
      record.requests.push({ origin: url.origin, path: url.pathname, method: 'WEBSOCKET', allowed });
      if (allowed) socket.connectToServer(); else socket.close({ code: 1008, reason: 'Local laboratory only' });
    });
    await context.route('**/*', route => {
      const url = new URL(route.request().url());
      const allowed = url.origin === origin;
      record.requests.push({ origin: url.origin, path: url.pathname, method: route.request().method(), allowed });
      return allowed ? route.continue() : route.abort('blockedbyclient');
    });
    context.on('requestfailed', request => {
      const url = new URL(request.url());
      record.errors.push(`REQUEST_FAILED ${url.origin}${url.pathname} ${request.failure()?.errorText || ''}`);
    });
    context.on('response', response => {
      if (response.status() >= 400) { const url = new URL(response.url()); record.errors.push(`HTTP_${response.status()} ${url.origin}${url.pathname}`); }
    });
    page = await context.newPage();
    page.on('pageerror', e => record.errors.push(e.stack || String(e)));
    page.on('console', message => { if (['warning', 'error'].includes(message.type())) record.errors.push(message.text() + ' at ' + JSON.stringify(message.location())); });
    const diagnostic = args['diagnostic-pagination'] === 'true' || args['diagnostic-focus'] === 'true';
    await page.goto(origin + (diagnostic ? '/?b0505=1' : ''), { waitUntil: 'domcontentloaded', timeout: 60000 });
    await page.locator('#engine').selectOption(args.engine);
    await page.locator('#file').setInputFiles(source);
    await page.locator('#start').click();
    if (!await page.locator('#engine').isDisabled() || !await page.locator('#file').isDisabled()) throw Error('Input controls not locked during initialization');
    await page.waitForFunction(() => /^(Pronto:|Falha na abertura:)/.test(document.querySelector('#status')?.textContent || ''), undefined, { timeout: 160000 });
    record.openStatus = await page.locator('#status').innerText();
    if (diagnostic && record.requests.some(request=>!request.allowed)) throw Error('Unauthorized egress attempt; focal session stopped');
    // Document-loaded is an I/O event. Give the first visual layout a bounded settle interval.
    await page.waitForTimeout(settle);
    record.editorText = await page.locator('#editor').innerText();
    record.nativePageCount = await page.locator('#editor').getAttribute('data-page-count');
    record.editControls = await page.locator('#editor').locator('input,textarea,[contenteditable]').evaluateAll(nodes => nodes.map(n => ({ tag: n.tagName, type: n.getAttribute('type'), editable: n.getAttribute('contenteditable'), className: n.className })));
    await page.screenshot({ path: path.join(out, 'opened.png'), fullPage: true });
    if (!record.openStatus.startsWith('Pronto:')) throw Error('SDK did not open; inspect private evidence');
    if (args['diagnostic-pagination'] === 'true') record.pagination = await require('./focal_diagnostics.cjs').pagination(page,out);
    if (args['diagnostic-focus'] === 'true') {
      record.focus = await require('./focal_diagnostics.cjs').focusSnapshot(page);
      record.sdkState = await page.locator('#editor').evaluate(el=>({...el.dataset}));
    }
    if (phase !== 'noop') {
      const title = (phase === 'remove' ? 'XYZ' : '') + 'NITE — fixture sintética de compatibilidade';
      if (args.engine === 'superdoc') {
        await page.locator('#editor').getByText(title, { exact: true }).first().click();
        await page.keyboard.press('Home');
      } else {
        if (args['diagnostic-focus'] === 'true') {
          // Inspected 12.1.0 surface inside the Web Component, not the outer page.
          const canvas=page.locator('#editor canvas.hacc.canvas1').first();
          // locator.click auto-scrolls this oversized canvas under the SDK ribbon.
          // Use its inspected current viewport box, without mutating editor state.
          await page.waitForTimeout(1000);
          const box=await canvas.boundingBox();if(!box)throw Error('No visible DOCX canvas');
          await page.mouse.click(box.x+125,box.y+140);
          await page.keyboard.press('Control+Home');
          record.focusAfterClick=await require('./focal_diagnostics.cjs').focusSnapshot(page);
        } else {
        // Fixed synthetic/desktop viewport: observed first body heading, below the header.
        // Canvas text has no DOM text selector. The exported text is checked independently.
        await page.mouse.click(445, 550);
        await page.keyboard.press('Control+Home');
        }
      }
      if (phase === 'edit') await page.keyboard.type('XYZ', { delay: 80 });
      else for (let i = 0; i < 3; i++) await page.keyboard.press('Delete');
      await page.waitForTimeout(500);
    }
    if (args.lifecycle === 'true') await page.locator('#engine').selectOption(args.engine === 'superdoc' ? 'apryse' : 'superdoc');
    if (diagnostic && record.requests.some(request=>!request.allowed)) throw Error('Unauthorized egress attempt; export stopped');
    const downloaded = page.waitForEvent('download', { timeout: 120000 });
    await page.locator('#export').click();
    const download = await downloaded;
    if (download.suggestedFilename() !== `b0504-${args.engine}-candidate.docx`) throw Error('Export mislabeled after SDK selection changed');
    await download.saveAs(path.join(out, 'candidate.docx'));
    record.exportStatus = await page.locator('#status').innerText();
    record.candidateHash = crypto.createHash('sha256').update(fs.readFileSync(path.join(out, 'candidate.docx'))).digest('hex');
    record.status = 'EXPORTED_NOT_VALIDATED';
    await page.screenshot({ path: path.join(out, 'exported.png'), fullPage: true });
    if (args.lifecycle === 'true') {
      await page.locator('#engine').selectOption(args.engine);
      await page.locator('#file').setInputFiles(path.join(out, 'candidate.docx'));
      await page.locator('#start').click();
      await page.waitForFunction(() => /^(Pronto:|Falha na abertura:)/.test(document.querySelector('#status')?.textContent || ''), undefined, { timeout: 160000 });
      if (!(await page.locator('#status').innerText()).startsWith('Pronto:')) throw Error('Same-page reinitialization failed');
      const next = page.waitForEvent('download', { timeout: 120000 });await page.locator('#export').click();
      await (await next).saveAs(path.join(out, 'reopened-candidate.docx'));record.samePageReopened = true;
    }
    console.log(`${args.engine}: browser export captured; fidelity not approved`);
  } catch (e) {
    record.status = 'EXECUTION_FAILED'; record.failure = String(e);
    if (page) { record.openStatus = await page.locator('#status').innerText().catch(() => 'unavailable'); await page.screenshot({ path: path.join(out, 'failed.png'), fullPage: true }).catch(() => {}); }
    throw e;
  }
  finally { fs.writeFileSync(path.join(out, 'result.json'), JSON.stringify(record, null, 2)); await context.close(); await browser.close(); }
}
main().catch(() => { console.error('SDK probe failed; inspect private result.json'); process.exitCode = 1; });
