import { cp, mkdir, stat } from 'node:fs/promises';
import { fileURLToPath } from 'node:url';
import path from 'node:path';
const current=path.dirname(fileURLToPath(import.meta.url));
const root=path.resolve(current,'..');
const source=path.join(root,'node_modules/@pdftron/webviewer/public');
const target=path.join(root,'public/lib');
await stat(source);await mkdir(path.dirname(target),{recursive:true});await cp(source,target,{recursive:true});
console.log('Apryse assets copied into ignored web/public/lib; verify trial/license before use.');
