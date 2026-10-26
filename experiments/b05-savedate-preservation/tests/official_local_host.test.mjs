import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const here = path.dirname(fileURLToPath(import.meta.url));
const hostSource = fs.readFileSync(path.join(here, '../scripts/official_local_host.mjs'), 'utf8');

test('official local editor host gives the iframe the remaining viewport height', () => {
  assert.match(hostSource, /body\{display:flex;flex-direction:column\}/);
  assert.match(hostSource, /header\{flex:0 0 40px;height:40px/);
  assert.match(hostSource, /#editor\{flex:1 1 auto;min-height:0;width:100%/);
  assert.match(hostSource, /#editor>iframe\{display:block;width:100%;height:100%;border:0\}/);
  assert.match(hostSource, /width:"100%",height:"100%"/);
  assert.doesNotMatch(hostSource, /height:"calc\(100% - 40px\)"/);
});
