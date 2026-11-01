import Link from "next/link";
import { notFound } from "next/navigation";
import { AuthenticatedShell } from "../../../components/shell/authenticated-shell";
import { Button } from "../../../components/ui";
import { ActivityDetail } from "../../../components/activities/activity-detail";
import { ActivityEmptyState, ActivityRows } from "../../../components/activities/activity-list";
import { previewActivities, previewActivity, previewAssignees, previewEvidence, previewHistory, previewProjectTitles } from "../../../lib/activities/preview-fixtures";
import type { Activity } from "../../../lib/activities/contracts";
import type { NiteMember } from "../../../lib/auth/identity";

export const dynamic = "force-dynamic";
export const metadata = { title: "Prévia sintética de atividades" };

type SearchParams = Promise<Record<string, string | string[] | undefined>>;
const member: NiteMember = {
  id: "83000000-0000-4000-8000-000000000001",
  name: "Integrante sintético",
  institutionalEmail: "fixture@example.invalid",
  status: "ATIVO",
};
const mainReturnTo = "/dev-preview/atividades?state=list";

function banner() {
  return <div role="note" className="border-b border-status-warning-border bg-status-warning-soft px-4 py-2 text-center text-type-metadata font-medium text-status-warning sm:pl-[calc(250px+1rem)]">
    Prévia de desenvolvimento — dados sintéticos, sem persistência institucional.
  </div>;
}

function PreviewList({ state }: { state: "list" | "empty" | "filtered" }) {
  const ordered = [...previewActivities].sort((left, right) => right.createdAt.localeCompare(left.createdAt));
  return <main className="mx-auto w-full max-w-6xl min-w-0 space-y-7 px-4 py-7 sm:px-7 sm:py-10 lg:px-9">
    <nav aria-label="Estados da prévia" className="flex flex-wrap gap-2 text-type-metadata">
      {([["list", "Lista preenchida"], ["empty", "Estado vazio"], ["filtered", "Sem resultados"], ["detail", "Detalhe e histórico"]] as const).map(([value, label]) => <Link key={value} href={`/dev-preview/atividades?state=${value}`} aria-current={state === value ? "page" : undefined} className={`inline-flex min-h-9 items-center rounded-control px-2.5 focus-visible:outline-3 focus-visible:outline-offset-2 focus-visible:outline-nite-focus ${state === value ? "bg-nite-brand-soft font-medium text-nite-brand-primary" : "text-nite-text-secondary hover:bg-nite-surface-subtle"}`}>{label}</Link>)}
    </nav>
    <header className="flex min-w-0 flex-col gap-5 sm:flex-row sm:items-end sm:justify-between">
      <div className="min-w-0 space-y-2"><h1 className="font-display text-type-page-title text-nite-text-primary">Minhas atividades</h1><p className="max-w-prose text-type-body-compact text-nite-text-secondary">Acompanhe o que está sob sua responsabilidade.</p></div>
      <span className="text-type-metadata text-nite-text-muted">Prévia sintética</span>
    </header>
    <nav aria-label="Situação sintética" className="-mx-1 flex min-w-0 snap-x gap-1 overflow-x-auto border-b border-nite-border-soft px-1 pb-2 [scrollbar-width:none] [&::-webkit-scrollbar]:hidden">
      {["Todas", "Planejadas", "Em execução", "Concluídas", "Canceladas"].map((label, index) => <span key={label} aria-current={index === 0 ? "page" : undefined} className={`inline-flex min-h-10 shrink-0 snap-start items-center rounded-control px-3 text-type-body-compact ${index === 0 ? "bg-nite-brand-soft font-medium text-nite-brand-primary" : "text-nite-text-secondary"}`}>{label}</span>)}
    </nav>
    <div className="grid min-w-0 gap-3 lg:grid-cols-[minmax(16rem,1fr)_minmax(14rem,auto)_auto] lg:items-end" aria-label="Filtros sintéticos">
      <label className="grid min-w-0 gap-1.5 text-type-metadata text-nite-text-secondary">Buscar atividades<input className="min-h-11 w-full min-w-0 rounded-control border border-nite-border-control bg-nite-surface px-3 py-2 text-type-body-compact text-nite-text-primary placeholder:text-nite-placeholder" disabled type="search" placeholder="Título ou descrição" /></label>
      <details className="group relative min-w-0 lg:w-56">
        <summary className="flex min-h-11 cursor-pointer list-none items-center justify-between gap-3 rounded-control border border-nite-border-soft bg-nite-surface px-3 text-type-body-compact font-medium text-nite-text-secondary transition-colors hover:bg-nite-surface-subtle hover:text-nite-text-primary group-open:border-nite-brand-primary group-open:bg-nite-surface-subtle group-open:text-nite-text-primary focus-visible:outline-3 focus-visible:outline-offset-2 focus-visible:outline-nite-focus motion-reduce:transition-none [&::-webkit-details-marker]:hidden"><span>Filtros avançados</span><span className="flex shrink-0 items-center gap-2"><span className="text-type-metadata text-nite-text-muted">Filtrar</span><svg aria-hidden="true" viewBox="0 0 16 16" fill="none" className="size-4 transition-transform group-open:rotate-180 motion-reduce:transition-none"><path d="m3.5 6 4.5 4 4.5-4" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" /></svg></span></summary>
        <div className="grid min-w-0 gap-3 border-t border-nite-border-soft px-1 pb-2 pt-3 sm:grid-cols-2 lg:absolute lg:right-0 lg:top-full lg:z-20 lg:mt-2 lg:w-[min(42rem,calc(100vw-19rem))] lg:grid-cols-[minmax(12rem,1fr)_minmax(10rem,.8fr)_minmax(10rem,.8fr)_auto] lg:items-end lg:rounded-panel lg:border lg:border-nite-border-soft lg:bg-nite-surface lg:p-4 lg:shadow-nite-floating">
          <label className="grid min-w-0 gap-1.5 text-type-metadata text-nite-text-secondary">Projeto<select className="min-h-11 w-full min-w-0 rounded-control border border-nite-border-control bg-nite-surface px-3 py-2 text-type-body-compact text-nite-text-primary" disabled><option>Todos os projetos</option></select></label>
          <label className="grid min-w-0 gap-1.5 text-type-metadata text-nite-text-secondary">Prazo a partir de<input className="min-h-11 w-full min-w-0 rounded-control border border-nite-border-control bg-nite-surface px-3 py-2 text-type-body-compact text-nite-text-primary" disabled type="date" /></label>
          <label className="grid min-w-0 gap-1.5 text-type-metadata text-nite-text-secondary">Prazo até<input className="min-h-11 w-full min-w-0 rounded-control border border-nite-border-control bg-nite-surface px-3 py-2 text-type-body-compact text-nite-text-primary" disabled type="date" /></label>
          <label className="flex min-h-11 min-w-0 items-center gap-2 text-type-body-compact text-nite-text-secondary"><input className="size-4 accent-nite-brand-primary" disabled type="checkbox" /> Em atraso</label>
        </div>
      </details>
      <div className="flex min-w-0 flex-wrap items-center gap-3 lg:col-start-3"><Button type="button" variant="secondary" disabled>Aplicar filtros</Button><span className="text-type-metadata text-nite-text-muted">Controles indisponíveis nesta prévia.</span></div>
    </div>
    {state === "list" ? <>
      <p className="text-type-metadata text-nite-text-secondary">{ordered.length} atividades</p>
      <ActivityRows activities={ordered} projectTitles={previewProjectTitles} returnTo={mainReturnTo} detailHrefTemplate="/dev-preview/atividades?state=detail&activity={id}" />
      <p className="text-center text-type-metadata text-nite-text-muted">A lista real usa a paginação da API; esta composição mostra itens sintéticos.</p>
    </> : <ActivityEmptyState
      filtered={state === "filtered"}
      canCreate={state === "empty"}
      clearHref="/dev-preview/atividades?state=list"
      actionHref="/dev-preview/atividades?state=list"
      actionLabel="Ver lista preenchida"
    />}
  </main>;
}

function PreviewDetail({ activityId }: { activityId: string | undefined }) {
  const activity: Activity = previewActivities.find((item) => item.id === activityId) ?? previewActivity;
  const history = { items: previewHistory.map((item) => ({ ...item, activityId: activity.id })), page: 0, size: 20, total: 20 };
  const evidence = { items: previewEvidence.map((item) => ({ ...item, activityId: activity.id })), page: 0, size: 20, total: previewEvidence.length };
  const assignees = { items: previewAssignees.map((item) => item.role === "EXECUTOR" ? { ...item, memberId: activity.primaryExecutorId, memberName: activity.primaryExecutorName } : item), page: 0, size: 100, total: previewAssignees.length };
  return <>
    <nav aria-label="Estados da prévia" className="mx-auto flex w-full max-w-6xl flex-wrap gap-2 px-4 pt-7 text-type-metadata sm:px-7 lg:px-9">
      <Link href="/dev-preview/atividades?state=list" className="inline-flex min-h-9 items-center rounded-control px-2.5 text-nite-brand-primary underline underline-offset-4 focus-visible:outline-3 focus-visible:outline-offset-2 focus-visible:outline-nite-focus">Voltar à prévia da lista</Link>
      <span className="self-center text-nite-text-muted">·</span><span className="self-center text-nite-text-secondary">Detalhe e histórico sintéticos</span>
    </nav>
    <ActivityDetail
      activity={activity}
      projectTitle={previewProjectTitles[activity.projectId ?? ""] ?? null}
      memberId={member.id}
      returnTo={mainReturnTo}
      capabilities={null}
      assignees={assignees}
      assigneeIssue={null}
      candidates={null}
      candidateIssue={null}
      candidateQuery=""
      candidatePage={0}
      candidateSearchInvalid={false}
      participantsOpen={false}
      evidence={evidence}
      evidenceIssue={null}
      evidenceWithdraw={{}}
      history={history}
      historyIssue={null}
      historyAllowed
      evidencePage={0}
      historyPage={0}
    />
  </>;
}

export default async function SyntheticActivitiesPreview({ searchParams }: { searchParams: SearchParams }) {
  if (process.env.NODE_ENV !== "development") notFound();
  const params = await searchParams;
  const rawState = typeof params.state === "string" ? params.state : "list";
  const state = rawState === "empty" || rawState === "filtered" || rawState === "detail" ? rawState : "list";
  return <>
    {banner()}
    <AuthenticatedShell member={member} roles={["INTEGRANTE"]} projects={[]} leadershipProjectsUnavailable={false}>
      {state === "detail" ? <PreviewDetail activityId={typeof params.activity === "string" ? params.activity : undefined} /> : <PreviewList state={state} />}
    </AuthenticatedShell>
  </>;
}
