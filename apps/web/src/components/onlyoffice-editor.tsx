"use client";

import { useEffect, useId, useState } from "react";
import type { EditorSession } from "@/lib/diary-api";

declare global {
  interface Window {
    DocsAPI?: { DocEditor: new (containerId: string, config: Record<string, unknown>) => { destroyEditor(): void } };
  }
}

type Props = { session: EditorSession; trustedOrigin: string; onClose: () => void };

export function OnlyOfficeEditor({ session, trustedOrigin, onClose }: Props) {
  const generatedId = useId().replaceAll(":", "");
  const containerId = `onlyoffice-${generatedId}`;
  const [error, setError] = useState<string | null>(null);
  let scriptUrl: string | null = null;
  let validationError: string | null = null;
  try {
    const parsedScriptUrl = new URL(session.editor.apiScriptUrl);
    const expectedOrigin = trustedOrigin ? new URL(trustedOrigin).origin : "";
    if (!expectedOrigin || parsedScriptUrl.origin !== expectedOrigin) {
      validationError = "A origem do editor não está configurada ou não corresponde ao ONLYOFFICE autorizado.";
    } else if (parsedScriptUrl.protocol !== "https:" && !["localhost", "127.0.0.1", "::1"].includes(parsedScriptUrl.hostname)) {
      validationError = "O script do editor exige HTTPS fora do laboratório local.";
    } else {
      scriptUrl = parsedScriptUrl.href;
    }
  } catch {
    validationError = "A origem do editor está malformada.";
  }

  useEffect(() => {
    let editor: { destroyEditor(): void } | undefined;
    if (validationError || !scriptUrl) return;

    const script = document.createElement("script");
    script.src = scriptUrl;
    script.async = true;
    script.onload = () => {
      if (!window.DocsAPI) {
        setError("O ONLYOFFICE não disponibilizou a API de edição.");
        return;
      }
      editor = new window.DocsAPI.DocEditor(containerId, session.editor.config);
    };
    script.onerror = () => setError("Não foi possível carregar a API do ONLYOFFICE.");
    document.head.append(script);
    return () => {
      editor?.destroyEditor();
      script.remove();
    };
  }, [containerId, session, scriptUrl, validationError]);

  return (
    <section className="editor-overlay" aria-label="Editor de documentos">
      <div className="editor-toolbar"><span>Diário contínuo</span><button type="button" onClick={onClose}>Fechar editor</button></div>
      {(validationError ?? error) && <p className="editor-error" role="alert">{validationError ?? error}</p>}
      <div id={containerId} className="editor-frame" />
    </section>
  );
}
