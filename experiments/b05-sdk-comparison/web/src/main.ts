import './style.css';
import type { SuperDoc as SuperDocType } from 'superdoc';

const el = <T extends HTMLElement>(id: string) => document.getElementById(id) as T;
const engine = el<HTMLSelectElement>('engine');
const input = el<HTMLInputElement>('file');
const license = el<HTMLInputElement>('license');
const start = el<HTMLButtonElement>('start');
const save = el<HTMLButtonElement>('export');
const status = el<HTMLElement>('status');
const viewer = el<HTMLElement>('editor');
const toolbar = el<HTMLElement>('toolbar');
type EditorHandle = { exportDocx: () => Promise<Blob>; destroy: () => Promise<void> };
let active: { editor: EditorHandle; engine: string } | undefined;
let exporting = false;
const report = (message: string) => { status.textContent = message; };

async function openSuperDoc(file: File): Promise<EditorHandle> {
  const { SuperDoc } = await import('superdoc');
  await import('superdoc/style.css');
  let ready: () => void;
  let fail: (reason: Error) => void;
  const loaded = new Promise<void>((resolve, reject) => { ready = resolve; fail = reject; });
  const timeout = window.setTimeout(() => fail(new Error('SuperDoc onReady timeout')), 120000);
  let doc: SuperDocType | undefined;
  try {
  doc = new SuperDoc({
    selector: '#editor', toolbar: '#toolbar', document: file, documentMode: 'editing',
    telemetry: { enabled: false },
    onPaginationUpdate: ({ totalPages }) => {
      viewer.dataset.pageCount = String(totalPages);
      if (new URLSearchParams(location.search).has('b0505')) {
        const events: { at: number; totalPages: number }[] = JSON.parse(viewer.dataset.paginationEvents || '[]');
        events.push({ at: performance.now(), totalPages });
        viewer.dataset.paginationEvents = JSON.stringify(events.slice(-100));
      }
    },
    onReady: () => ready(),
    onContentError: ({error}) => fail(error instanceof Error ? error : new Error(String(error))),
    onException: ({error}) => fail(error instanceof Error ? error : new Error(String(error))),
  });
  await loaded;
  } catch (error) { doc?.destroy(); throw error; }
  finally { window.clearTimeout(timeout); }
  const opened = doc;
  return { destroy: async () => opened.destroy(), exportDocx: async () => {
    const result = await opened.export({ exportType: ['docx'], triggerDownload: false });
    if (!(result instanceof Blob)) throw new Error('SuperDoc não retornou DOCX Blob');
    return result;
  }};
}
async function openApryse(file: File): Promise<EditorHandle> {
  const { default: WebViewer } = await import('@pdftron/webviewer');
  const key = license.value.trim();
  // No trial key is recorded in source, URL, localStorage or logs.
  const sourceUrl = URL.createObjectURL(file);
  let instance: Awaited<ReturnType<typeof WebViewer>>;
  try { instance = await WebViewer({ path:'/lib', ...(key ? {licenseKey:key} : {}), initialDoc: sourceUrl, extension: 'docx', filename: file.name, enableOfficeEditing:true, initialMode: WebViewer.Modes.DOCX_EDITOR }, viewer); }
  catch (error) { URL.revokeObjectURL(sourceUrl); throw error; }
  const loaded = new Promise<void>((resolve, reject) => {
    const timeout = window.setTimeout(() => reject(new Error('Apryse documentLoaded timeout')), 120000);
    instance.Core.documentViewer.addEventListener('documentLoaded', () => {
      window.clearTimeout(timeout); resolve();
    }, { once: true });
  });
  try { await loaded; }
  catch (error) { await instance.UI.dispose(); URL.revokeObjectURL(sourceUrl); throw error; }
  if (new URLSearchParams(location.search).has('b0505')) {
    viewer.dataset.officeEditor = String(Boolean(instance.Core.documentViewer.getDocument().getOfficeEditor()));
    viewer.dataset.readOnly = String(instance.Core.annotationManager.isReadOnlyModeEnabled());
  }
  return { destroy: async () => { await instance.UI.dispose(); URL.revokeObjectURL(sourceUrl); }, exportDocx: async () => {
    const document = instance.Core.documentViewer.getDocument();
    const binary = await document.getFileData({ downloadType: 'office' });
    return new Blob([new Uint8Array(binary)], { type: file.type || 'application/vnd.openxmlformats-officedocument.wordprocessingml.document' });
  }};
}
start.addEventListener('click', async () => {
  const file = input.files?.[0];
  if (!file || !file.name.toLowerCase().endsWith('.docx')) { report('Escolha um arquivo DOCX local.'); return; }
  if (file.size > 8 * 1024 * 1024) { report('DOCX excede o limite experimental de 8 MiB.'); return; }
  const selectedEngine = engine.value;
  const previous = active;active = undefined;save.disabled=true;start.disabled=true;engine.disabled=true;input.disabled=true;license.disabled=true;
  report(`Abrindo cópia em ${selectedEngine}; nenhuma versão foi salva.`);
  try {
    await previous?.editor.destroy();viewer.replaceChildren();toolbar.replaceChildren();delete viewer.dataset.pageCount;delete viewer.dataset.paginationEvents;delete viewer.dataset.officeEditor;delete viewer.dataset.readOnly;
    const editor = selectedEngine === 'superdoc' ? await openSuperDoc(file) : await openApryse(file);
    active = { editor, engine: selectedEngine };
    save.disabled=false;report(`Pronto: ${selectedEngine}. Edite, exporte e valide o arquivo fora do navegador.`);
  } catch (error) {report(`Falha na abertura: ${String(error)}`);} finally { start.disabled=false;engine.disabled=false;input.disabled=false;license.disabled=false; }
});
save.addEventListener('click', async () => {
  if (!active || exporting)return;
  const current = active;
  exporting=true;save.disabled=true;start.disabled=true;engine.disabled=true;input.disabled=true;license.disabled=true;report('Exportando candidata local; nenhuma confirmação de integridade ainda.');
  try {
    const file=await current.editor.exportDocx();
    if(file.size===0 || file.size>8*1024*1024)throw new Error('Bytes DOCX inválidos');
    const url=URL.createObjectURL(file);const a=document.createElement('a');
    a.href=url;a.download=`b0504-${current.engine}-candidate.docx`;a.click();
    window.setTimeout(()=>URL.revokeObjectURL(url),10000);
    report('Candidata exportada localmente. Verifique OOXML/Word antes de qualquer aprovação.');
  } catch(error){report(`Falha na exportação: ${String(error)}`);} finally{exporting=false;save.disabled=false;start.disabled=false;engine.disabled=false;input.disabled=false;license.disabled=false;}
});
