export type DiaryView = {
  memberId: string;
  documentId: string;
  title: string;
  createdAt: string;
  updatedAt: string;
  currentVersionId: string;
  currentRevision: number;
  documentVersion: number;
  importedAt: string;
  sourceCreatedAt: string | null;
  sourceModifiedAt: string | null;
  sourceMetadataOrigin: "SHAREPOINT" | null;
};

export type Page<T> = { items: T[]; page: number; size: number; total: number };
export type ActivitySummary = { id: string; title: string; status: string; dueOn: string | null };
export type EvidenceSummary = { id: string; activityId: string; type: string; createdAt: string; content: string | null; url: string | null; object: { filename: string } | null };
export type DiaryActivityPanel = {
  from: string;
  to: string;
  activities: Page<ActivitySummary>;
  evidence: Page<EvidenceSummary>;
};
export type EditorSession = { id: string; expiresAt: string; editor: { apiScriptUrl: string; config: Record<string, unknown> } };

function object(value: unknown): Record<string, unknown> {
  if (typeof value !== "object" || value === null || Array.isArray(value)) throw new Error("A API retornou um objeto inválido.");
  return value as Record<string, unknown>;
}

function string(value: unknown): string {
  if (typeof value !== "string") throw new Error("A API retornou um texto inválido.");
  return value;
}

function nullableString(value: unknown): string | null {
  return value === null ? null : string(value);
}

function page<T>(value: unknown, decode: (item: unknown) => T): Page<T> {
  const data = object(value);
  if (!Array.isArray(data.items) || typeof data.page !== "number" || typeof data.size !== "number" || typeof data.total !== "number") {
    throw new Error("A API retornou uma página inválida.");
  }
  return { items: data.items.map(decode), page: data.page, size: data.size, total: data.total };
}

function decodeDiary(value: unknown): DiaryView {
  const data = object(value);
  if (typeof data.currentRevision !== "number" || typeof data.documentVersion !== "number") throw new Error("A API retornou a versão do Diário inválida.");
  const origin = data.sourceMetadataOrigin;
  if (origin !== null && origin !== "SHAREPOINT") throw new Error("A API retornou a origem do Diário inválida.");
  return {
    memberId: string(data.memberId), documentId: string(data.documentId), title: string(data.title),
    createdAt: string(data.createdAt), updatedAt: string(data.updatedAt), currentVersionId: string(data.currentVersionId),
    currentRevision: data.currentRevision, documentVersion: data.documentVersion, importedAt: string(data.importedAt),
    sourceCreatedAt: nullableString(data.sourceCreatedAt), sourceModifiedAt: nullableString(data.sourceModifiedAt),
    sourceMetadataOrigin: origin,
  };
}

function decodeActivity(value: unknown): ActivitySummary {
  const data = object(value);
  return { id: string(data.id), title: string(data.title), status: string(data.status), dueOn: nullableString(data.dueOn) };
}

function decodeEvidence(value: unknown): EvidenceSummary {
  const data = object(value);
  const objectValue = data.object === null ? null : { filename: string(object(data.object).filename) };
  return { id: string(data.id), activityId: string(data.activityId), type: string(data.type), createdAt: string(data.createdAt),
    content: nullableString(data.content), url: nullableString(data.url), object: objectValue };
}

function decodePanel(value: unknown): DiaryActivityPanel {
  const data = object(value);
  return { from: string(data.from), to: string(data.to), activities: page(data.activities, decodeActivity), evidence: page(data.evidence, decodeEvidence) };
}

function decodeEditorSession(value: unknown): EditorSession {
  const data = object(value);
  const editor = object(data.editor);
  const config = object(editor.config);
  return { id: string(data.id), expiresAt: string(data.expiresAt), editor: { apiScriptUrl: string(editor.apiScriptUrl), config } };
}

export class DiaryApi {
  constructor(private readonly baseUrl: string, private readonly accessToken: string) {}

  async getMine(signal?: AbortSignal): Promise<DiaryView> {
    return this.request("/api/v1/diaries/me", decodeDiary, signal);
  }

  async getActivities(from: string, to: string, signal?: AbortSignal): Promise<DiaryActivityPanel> {
    const query = new URLSearchParams({ from, to });
    return this.request(`/api/v1/diaries/me/activities?${query}`, decodePanel, signal);
  }

  async downloadVersion(documentId: string, versionId: string, format: "docx" | "pdf"): Promise<Blob> {
    const suffix = format === "pdf" ? "pdf" : "download";
    const method = format === "pdf" ? "POST" : "GET";
    const response = await fetch(new URL(`/api/v1/documents/${encodeURIComponent(documentId)}/versions/${encodeURIComponent(versionId)}/${suffix}`, this.baseUrl || window.location.origin), {
      method,
      cache: "no-store",
      headers: { Authorization: `Bearer ${this.accessToken}` },
    });
    if (!response.ok) throw new Error(`A API respondeu ${response.status} ao preparar o download.`);
    return response.blob();
  }

  async openEditor(documentId: string, expectedVersion: number, signal?: AbortSignal): Promise<EditorSession> {
    return this.request(`/api/v1/documents/${encodeURIComponent(documentId)}/editor-sessions`, decodeEditorSession, signal, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ expectedVersion, readOnly: false }),
    });
  }

  async closeEditor(documentId: string, sessionId: string, signal?: AbortSignal): Promise<void> {
    const response = await fetch(new URL(`/api/v1/documents/${encodeURIComponent(documentId)}/editor-sessions/${encodeURIComponent(sessionId)}/close`, this.baseUrl || window.location.origin), {
      method: "POST",
      signal,
      cache: "no-store",
      headers: { Authorization: `Bearer ${this.accessToken}` },
    });
    if (!response.ok) throw new Error(`A API respondeu ${response.status} ao encerrar o editor.`);
  }

  private async request<T>(path: string, decode: (value: unknown) => T, signal?: AbortSignal, init: RequestInit = {}): Promise<T> {
    const response = await fetch(new URL(path, this.baseUrl || window.location.origin), {
      ...init,
      signal,
      cache: "no-store",
      headers: { ...init.headers, Authorization: `Bearer ${this.accessToken}` },
    });
    if (!response.ok) throw new Error(`A API respondeu ${response.status}.`);
    return decode(await response.json());
  }
}
