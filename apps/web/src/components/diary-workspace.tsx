"use client";

import { useMemo, useState } from "react";
import Link from "next/link";
import { DiaryApi, type DiaryActivityPanel, type DiaryView, type EditorSession } from "@/lib/diary-api";
import { OnlyOfficeEditor } from "@/components/onlyoffice-editor";

type Props = { accessToken: string | null; apiBaseUrl: string };

function mondayOf(date: Date): Date {
  const result = new Date(Date.UTC(date.getFullYear(), date.getMonth(), date.getDate()));
  const day = date.getDay() || 7;
  result.setUTCDate(result.getUTCDate() - day + 1);
  return result;
}

function dateLabel(value: Date): string {
  return new Intl.DateTimeFormat("pt-BR", { day: "2-digit", month: "short", timeZone: "UTC" }).format(value);
}

function isoDate(value: Date): string {
  return value.toISOString().slice(0, 10);
}

export function DiaryWorkspace({ accessToken, apiBaseUrl }: Props) {
  const [weekStart, setWeekStart] = useState(() => mondayOf(new Date()));
  const weekEnd = useMemo(() => {
    const end = new Date(weekStart);
    end.setUTCDate(end.getUTCDate() + 6);
    return end;
  }, [weekStart]);
  const [status, setStatus] = useState("A sessão institucional ainda não foi conectada a esta interface.");
  const connected = Boolean(accessToken && apiBaseUrl);
  const api = useMemo(() => accessToken && apiBaseUrl ? new DiaryApi(apiBaseUrl, accessToken) : null, [apiBaseUrl, accessToken]);
  const [diary, setDiary] = useState<DiaryView | null>(null);
  const [panel, setPanel] = useState<DiaryActivityPanel | null>(null);
  const [editorSession, setEditorSession] = useState<EditorSession | null>(null);

  function moveWeek(direction: -1 | 1) {
    setWeekStart((current) => {
      const next = new Date(current);
      next.setUTCDate(next.getUTCDate() + direction * 7);
      return next;
    });
  }

  async function loadWeek() {
    if (!api) return;
    setStatus("Carregando atividades e evidências da semana…");
    try {
      const [currentDiary, currentPanel] = await Promise.all([
        api.getMine(), api.getActivities(isoDate(weekStart), isoDate(weekEnd)),
      ]);
      setDiary(currentDiary);
      setPanel(currentPanel);
      setStatus("Diário e painel semanal carregados.");
    } catch (error) {
      setStatus(error instanceof Error ? error.message : "Não foi possível carregar o Diário.");
    }
  }

  async function openDiary() {
    if (!api || !diary) return;
    setStatus("Abrindo o editor documental…");
    try {
      setEditorSession(await api.openEditor(diary.documentId, diary.documentVersion));
      setStatus("Editor pronto.");
    } catch (error) {
      setStatus(error instanceof Error ? error.message : "Não foi possível abrir o Diário.");
    }
  }

  async function closeDiary() {
    if (!api || !diary || !editorSession) return;
    try {
      await api.closeEditor(diary.documentId, editorSession.id);
      setEditorSession(null);
      setStatus("Sessão do editor encerrada.");
      await loadWeek();
    } catch (error) {
      setStatus(error instanceof Error ? error.message : "Não foi possível encerrar o editor.");
    }
  }

  async function downloadDiary(format: "docx" | "pdf") {
    if (!api || !diary) return;
    try {
      const blob = await api.downloadVersion(diary.documentId, diary.currentVersionId, format);
      const objectUrl = URL.createObjectURL(blob);
      const link = document.createElement("a");
      link.href = objectUrl;
      link.download = `diario-continuo.${format}`;
      link.click();
      window.setTimeout(() => URL.revokeObjectURL(objectUrl), 1_000);
    } catch (error) {
      setStatus(error instanceof Error ? error.message : "Não foi possível baixar a versão atual.");
    }
  }

  return (
    <main className="shell">
      <header className="topbar">
        <Link className="brand" href="/" aria-label="NITE Connect, início"><span className="brand-mark">N</span><span>NITE <b>CONNECT</b></span></Link>
        <span className="topbar-label">DOCUMENTOS</span>
      </header>

      <div className="workspace">
        <nav className="breadcrumbs" aria-label="Navegação estrutural"><span>Início</span><span aria-hidden="true">/</span><strong>Diário contínuo</strong></nav>

        <section className="heading">
          <div>
            <p className="eyebrow">REGISTRO INDIVIDUAL</p>
            <h1>Diário contínuo</h1>
            <p className="subtitle">Escreva seu resumo semanal no documento. Atividades e evidências ficam ao lado para consulta.</p>
          </div>
          <span className="status-pill"><span className="status-dot" />Documento pessoal</span>
        </section>

        <section className="weekbar" aria-label="Selecionar semana">
          <div className="week-title"><span>SEMANA</span><strong>{dateLabel(weekStart)} — {dateLabel(weekEnd)}</strong></div>
          <div className="week-actions">
            <button className="icon-button" type="button" onClick={() => moveWeek(-1)} aria-label="Semana anterior">←</button>
            <button className="today-button" type="button" onClick={() => setWeekStart(mondayOf(new Date()))}>Semana atual</button>
            <button className="icon-button" type="button" onClick={() => moveWeek(1)} aria-label="Próxima semana">→</button>
          </div>
        </section>

        {!connected && <div className="notice" role="status"><span className="notice-icon">i</span><p>{status} A autenticação Entra e o provedor de sessão ainda precisam ser conectados.</p></div>}
        {connected && <div className="notice" role="status"><span className="notice-icon">i</span><p>{status}</p><button type="button" className="load-button" onClick={loadWeek}>Carregar semana</button></div>}

        <div className="columns">
          <section className="document-card" aria-labelledby="document-heading">
            <div className="card-heading">
              <div><p className="eyebrow">DOCUMENTO WORD</p><h2 id="document-heading">Meu Diário</h2></div>
              <span className="revision">Contínuo</span>
            </div>
            <div className="editor-placeholder">
              <div className="doc-icon" aria-hidden="true">W</div>
              <h3>{diary?.title ?? "Seu documento aparecerá aqui"}</h3>
              <p>{diary ? `Versão ${diary.currentRevision} · atualizado ${new Intl.DateTimeFormat("pt-BR", { dateStyle: "medium", timeStyle: "short" }).format(new Date(diary.updatedAt))}` : "Depois de conectar sua sessão institucional, você poderá abrir, editar e salvar seu Diário no editor documental."}</p>
              <button type="button" className="primary-button" disabled={!connected || !diary} onClick={openDiary}>Abrir Diário</button>
              <div className="download-actions">
                <button type="button" disabled={!connected || !diary} onClick={() => downloadDiary("docx")}>Baixar DOCX</button>
                <button type="button" disabled={!connected || !diary} onClick={() => downloadDiary("pdf")}>Baixar PDF</button>
              </div>
            </div>
            <div className="document-meta"><span>Salvamentos geram versões no histórico.</span><span>DOCX</span></div>
          </section>

          <aside className="activity-card" aria-labelledby="activity-heading">
            <div className="card-heading">
              <div><p className="eyebrow">CONSULTA SEMANAL</p><h2 id="activity-heading">Atividades e evidências</h2></div>
              <span className="count-badge">{dateLabel(weekStart)}–{dateLabel(weekEnd)}</span>
            </div>
            <p className="panel-hint">Use estas informações para redigir seu resumo. Elas não são inseridas automaticamente no documento.</p>
            <div className="empty-state">
              <div className="empty-mark" aria-hidden="true">↗</div>
              {!panel && <><h3>Nenhuma atividade carregada</h3><p>As atividades atribuídas e as evidências enviadas no período serão exibidas aqui.</p></>}
              {panel && <div className="activity-list">
                <h3>Atividades ({panel.activities.total})</h3>
                {panel.activities.items.length === 0 ? <p>Nenhuma atividade com prazo nesta semana.</p> : panel.activities.items.map((item) => <article className="activity-item" key={item.id}><strong>{item.title}</strong><span>{item.status}{item.dueOn ? ` · prazo ${new Intl.DateTimeFormat("pt-BR", { dateStyle: "medium", timeZone: "UTC" }).format(new Date(`${item.dueOn}T00:00:00Z`))}` : ""}</span></article>)}
                <h3>Evidências ({panel.evidence.total})</h3>
                {panel.evidence.items.length === 0 ? <p>Nenhuma evidência enviada nesta semana.</p> : panel.evidence.items.map((item) => <article className="activity-item" key={item.id}><strong>{item.type}</strong><span>{item.object?.filename ?? item.content ?? item.url ?? "Registro de evidência"}</span></article>)}
              </div>}
            </div>
          </aside>
        </div>
      </div>
      <footer className="footer"><span>NITE Connect</span><span>Seu Diário é privado e versionado.</span></footer>
      {editorSession && <OnlyOfficeEditor session={editorSession} trustedOrigin={process.env.NEXT_PUBLIC_ONLYOFFICE_ORIGIN ?? ""} onClose={closeDiary} />}
    </main>
  );
}
