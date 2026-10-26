/**
 * B05-M2P5 — probe opcional, instalado somente em página/editor autorizados.
 * Não lê o conteúdo do clipboard e não grava localmente, em rede ou em disco.
 * A disponibilidade de text/html NÃO prova que o editor colou HTML formatado.
 * Não consegue observar eventos dentro de iframes de origem diferente.
 */
(function installNitePasteMetadataProbe() {
  'use strict';
  if (Object.prototype.hasOwnProperty.call(window, '__nitePasteMetadataProbe')) {
    throw new Error('Probe já instalado; não duplicar observações');
  }
  const records = [];
  let active = true;
  const handle = (event) => {
    if (!active) return;
    records.push({
      event: 'paste_event_observed',
      at: new Date().toISOString(),
      isTrusted: Boolean(event.isTrusted),
      clipboard_formats_offered: [...(event.clipboardData?.types ?? [])].sort(),
      actual_inserted_format: 'NOT_PROVEN_BY_EVENT',
      clipboard_payload_collected: false
    });
  };
  document.addEventListener('paste', handle, true);
  Object.defineProperty(window, '__nitePasteMetadataProbe', {
    configurable: false,
    value: Object.freeze({
      snapshot: () => records.map((r) => ({...r, clipboard_formats_offered: [...r.clipboard_formats_offered]})),
      stop: () => {
        document.removeEventListener('paste', handle, true);
        active = false;
        return records.map((r) => ({...r, clipboard_formats_offered: [...r.clipboard_formats_offered]}));
      }
    })
  });
})();
