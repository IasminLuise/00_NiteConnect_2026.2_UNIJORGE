import Link from "next/link";
import { requireAuthenticatedNiteSession } from "../../../lib/auth/server-session";
import { getAllMyProjects, getMyActivities, getMyActivityCapabilities } from "../../../lib/activities/data";
import { activityQuery } from "../../../lib/activities/query";
import { ActivityEmptyState, ActivityRows, formatActivityDate } from "../../../components/activities/activity-list";
import { ActivityFeedback } from "../../../components/activities/activity-detail";
import { Button } from "../../../components/ui";

export const dynamic = "force-dynamic";
export const metadata = { title: "Minhas atividades" };

const tabs: readonly [string, string][] = [
  ["", "Todas"],
  ["PLANEJADA", "Planejadas"],
  ["EM_EXECUCAO", "Em execução"],
  ["CONCLUIDA", "Concluídas"],
  ["CANCELADA", "Canceladas"],
];
const controlClass = "min-h-11 w-full min-w-0 rounded-control border border-nite-border-control bg-nite-surface px-3 py-2 text-type-body-compact text-nite-text-primary placeholder:text-nite-placeholder focus-visible:outline-3 focus-visible:outline-offset-2 focus-visible:outline-nite-focus";
type Params = Record<string, string | string[] | undefined>;

function one(value: string | string[] | undefined): string | undefined {
  return typeof value === "string" ? value : undefined;
}

function pageHref(query: URLSearchParams, page: number): string {
  const next = new URLSearchParams(query);
  next.set("page", String(page));
  return `/atividades?${next}`;
}

export default async function ActivitiesPage({ searchParams }: { searchParams: Promise<Params> }) {
  const params = await searchParams;
  const query = activityQuery(params);
  const session = await requireAuthenticatedNiteSession();
  const [result, projects, activityCapabilities] = await Promise.all([
    getMyActivities(session.bearer, query),
    getAllMyProjects(session.bearer),
    getMyActivityCapabilities(session.bearer),
  ]);

  const status = query.get("status") ?? "";
  const filtered = ["status", "q", "projectId", "dueFrom", "dueTo", "overdue"].some((key) => query.has(key));
  const returnTo = `/atividades?${query.toString()}`;
  const projectTitles = Object.fromEntries((projects.data ?? []).map((project) => [project.id, project.title]));
  const canCreate = activityCapabilities.issue === null && activityCapabilities.data.createIndependent;
  const selectedProjectId = query.get("projectId");
  const advancedFilters = [
    selectedProjectId ? projectTitles[selectedProjectId] ?? "Projeto selecionado" : null,
    query.get("dueFrom") ? `A partir de ${formatActivityDate(query.get("dueFrom")!)}` : null,
    query.get("dueTo") ? `Até ${formatActivityDate(query.get("dueTo")!)}` : null,
    query.has("overdue") ? "Em atraso" : null,
  ].filter((value): value is string => value !== null);
  const selectedTabHref = (value: string) => {
    const next = new URLSearchParams(query);
    next.set("page", "0");
    if (value) next.set("status", value); else next.delete("status");
    return `/atividades?${next}`;
  };

  return <main className="mx-auto w-full max-w-6xl min-w-0 space-y-7 px-4 py-7 sm:px-7 sm:py-10 lg:px-9">
    <ActivityFeedback code={one(params.feedback)} />
    <header className="flex min-w-0 flex-col gap-5 sm:flex-row sm:items-end sm:justify-between">
      <div className="min-w-0 space-y-2">
        <h1 className="font-display text-type-page-title text-nite-text-primary">Minhas atividades</h1>
        <p className="max-w-prose text-type-body-compact text-nite-text-secondary">Acompanhe o que está sob sua responsabilidade.</p>
      </div>
      {canCreate ? <Link href="/atividades/nova" className="inline-flex min-h-11 shrink-0 items-center justify-center rounded-control border border-nite-brand-primary bg-nite-brand-primary px-4 text-type-body-compact font-medium text-nite-action-text transition-colors hover:bg-nite-brand-primary/90 focus-visible:outline-3 focus-visible:outline-offset-2 focus-visible:outline-nite-focus motion-reduce:transition-none">Nova atividade</Link> : null}
    </header>

    <nav aria-label="Situação das atividades" className="-mx-1 flex min-w-0 snap-x gap-1 overflow-x-auto border-b border-nite-border-soft px-1 pb-2 [scrollbar-width:none] [&::-webkit-scrollbar]:hidden">
      {tabs.map(([value, label]) => <Link key={label} href={selectedTabHref(value)} aria-current={status === value ? "page" : undefined} className={`inline-flex min-h-10 shrink-0 snap-start items-center rounded-control px-3 text-type-body-compact transition-colors focus-visible:outline-3 focus-visible:outline-offset-2 focus-visible:outline-nite-focus motion-reduce:transition-none ${status === value ? "bg-nite-brand-soft font-medium text-nite-brand-primary" : "text-nite-text-secondary hover:bg-nite-surface-subtle hover:text-nite-text-primary"}`}>{label}</Link>)}
    </nav>

    <form action="/atividades" method="get" className="grid min-w-0 gap-3 lg:grid-cols-[minmax(16rem,1fr)_minmax(14rem,auto)_auto] lg:items-end">
      <input type="hidden" name="page" value="0" />
      {status ? <input type="hidden" name="status" value={status} /> : null}
      {selectedProjectId && projects.issue ? <input type="hidden" name="projectId" value={selectedProjectId} /> : null}
      <label className="grid min-w-0 gap-1.5 text-type-metadata text-nite-text-secondary">Buscar atividades<input className={controlClass} type="search" name="q" minLength={2} maxLength={160} defaultValue={query.get("q") ?? ""} placeholder="Título ou descrição" /></label>
      <details className="group relative min-w-0 lg:w-56">
        <summary className="flex min-h-11 cursor-pointer list-none items-center justify-between gap-3 rounded-control border border-nite-border-soft bg-nite-surface px-3 text-type-body-compact font-medium text-nite-text-secondary transition-colors hover:bg-nite-surface-subtle hover:text-nite-text-primary group-open:border-nite-brand-primary group-open:bg-nite-surface-subtle group-open:text-nite-text-primary focus-visible:outline-3 focus-visible:outline-offset-2 focus-visible:outline-nite-focus motion-reduce:transition-none [&::-webkit-details-marker]:hidden">
          <span>Filtros avançados</span>
          <span className="flex shrink-0 items-center gap-2">
            <span className="text-type-metadata text-nite-text-muted">{advancedFilters.length ? `${advancedFilters.length} ${advancedFilters.length === 1 ? "ativo" : "ativos"}` : "Filtrar"}</span>
            <svg aria-hidden="true" viewBox="0 0 16 16" fill="none" className="size-4 transition-transform group-open:rotate-180 motion-reduce:transition-none"><path d="m3.5 6 4.5 4 4.5-4" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" /></svg>
          </span>
        </summary>
        <div className="grid min-w-0 gap-3 border-t border-nite-border-soft px-1 pb-2 pt-3 sm:grid-cols-2 lg:absolute lg:right-0 lg:top-full lg:z-20 lg:mt-2 lg:w-[min(42rem,calc(100vw-19rem))] lg:grid-cols-[minmax(12rem,1fr)_minmax(10rem,.8fr)_minmax(10rem,.8fr)_auto] lg:items-end lg:rounded-panel lg:border lg:border-nite-border-soft lg:bg-nite-surface lg:p-4 lg:shadow-nite-floating">
          <label className="grid min-w-0 gap-1.5 text-type-metadata text-nite-text-secondary">Projeto<select className={controlClass} name="projectId" defaultValue={selectedProjectId ?? ""} disabled={projects.issue !== null}>
            <option value="">Todos os projetos</option>
            {(projects.data ?? []).map((project) => <option key={project.id} value={project.id}>{project.title}</option>)}
          </select></label>
          <label className="grid min-w-0 gap-1.5 text-type-metadata text-nite-text-secondary">Prazo a partir de<input className={controlClass} type="date" name="dueFrom" defaultValue={query.get("dueFrom") ?? ""} /></label>
          <label className="grid min-w-0 gap-1.5 text-type-metadata text-nite-text-secondary">Prazo até<input className={controlClass} type="date" name="dueTo" defaultValue={query.get("dueTo") ?? ""} /></label>
          <label className="flex min-h-11 min-w-0 items-center gap-2 text-type-body-compact text-nite-text-secondary"><input className="size-4 accent-nite-brand-primary focus-visible:outline-3 focus-visible:outline-offset-2 focus-visible:outline-nite-focus" type="checkbox" name="overdue" value="true" defaultChecked={query.has("overdue")} /> Em atraso</label>
        </div>
      </details>
      {advancedFilters.length ? <ul aria-label="Filtros avançados ativos" className="flex min-w-0 flex-wrap gap-2 lg:col-span-3">
        {advancedFilters.map((filter) => <li key={filter} className="max-w-full break-words rounded-pill border border-nite-border-soft bg-nite-surface-subtle px-2.5 py-1 text-type-metadata text-nite-text-secondary">{filter}</li>)}
      </ul> : null}
      <div className="flex min-w-0 flex-wrap items-center gap-3 lg:col-start-3">
        <Button type="submit" variant="secondary">Aplicar filtros</Button>
        {filtered ? <Link href="/atividades" className="inline-flex min-h-11 items-center rounded-control px-3 text-type-body-compact font-medium text-nite-text-secondary underline underline-offset-4 hover:text-nite-text-primary focus-visible:outline-3 focus-visible:outline-offset-2 focus-visible:outline-nite-focus">Limpar filtros</Link> : null}
        {projects.issue ? <span className="text-type-metadata text-nite-text-muted">O filtro por projeto está indisponível no momento.</span> : null}
      </div>
    </form>

    {result.issue ? <section aria-live="polite" className="border-y border-nite-border-soft py-8">
      <p className="font-medium text-nite-text-primary">{result.issue === "access" ? "Você não tem acesso a esta consulta." : result.issue === "not-found" ? "A consulta não está disponível." : "Não foi possível carregar suas atividades."}</p>
      <p className="mt-1 text-type-body-compact text-nite-text-secondary">{result.issue === "access" ? "Confira sua sessão institucional e tente novamente." : "Tente atualizar a página em alguns instantes."}</p>
    </section> : result.data.items.length === 0 && result.data.total > 0 ? <section aria-live="polite" className="flex min-h-64 flex-col items-center justify-center gap-3 border-y border-nite-border-soft px-4 py-12 text-center">
      <h2 className="font-display text-type-section-title text-nite-text-primary">Esta página não contém atividades.</h2>
      <Link href={pageHref(query, Math.max(0, Math.ceil(result.data.total / result.data.size) - 1))} className="inline-flex min-h-11 items-center rounded-control px-3 text-type-body-compact font-medium text-nite-brand-primary underline underline-offset-4 focus-visible:outline-3 focus-visible:outline-offset-2 focus-visible:outline-nite-focus">Ir para a última página disponível</Link>
    </section> : result.data.items.length === 0 ? <ActivityEmptyState filtered={filtered} canCreate={canCreate} clearHref="/atividades" /> : <>
      <div className="flex min-w-0 flex-wrap items-baseline justify-between gap-2 text-type-metadata text-nite-text-secondary">
        <p>{result.data.total} {result.data.total === 1 ? "atividade" : "atividades"}</p>
      </div>
      <ActivityRows activities={result.data.items} projectTitles={projectTitles} returnTo={returnTo} />
      {Math.ceil(result.data.total / result.data.size) > 1 ?
      <nav aria-label="Paginação" className="flex min-w-0 flex-wrap items-center justify-between gap-3 pt-1">
        {result.data.page > 0 ? <Link href={pageHref(query, result.data.page - 1)} className="inline-flex min-h-11 items-center rounded-control border border-nite-border-soft px-3 text-type-body-compact font-medium text-nite-text-primary hover:bg-nite-surface-subtle focus-visible:outline-3 focus-visible:outline-offset-2 focus-visible:outline-nite-focus">Anterior</Link> : <span aria-disabled="true" className="inline-flex min-h-11 items-center rounded-control border border-nite-border-soft px-3 text-type-body-compact text-nite-text-muted">Anterior</span>}
        <span className="text-center text-type-body-compact text-nite-text-secondary">Itens {result.data.total === 0 ? 0 : result.data.page * result.data.size + 1}–{Math.min((result.data.page + 1) * result.data.size, result.data.total)} de {result.data.total}</span>
        {(result.data.page + 1) * result.data.size < result.data.total ? <Link href={pageHref(query, result.data.page + 1)} className="inline-flex min-h-11 items-center rounded-control border border-nite-border-soft px-3 text-type-body-compact font-medium text-nite-text-primary hover:bg-nite-surface-subtle focus-visible:outline-3 focus-visible:outline-offset-2 focus-visible:outline-nite-focus">Próxima</Link> : <span aria-disabled="true" className="inline-flex min-h-11 items-center rounded-control border border-nite-border-soft px-3 text-type-body-compact text-nite-text-muted">Próxima</span>}
      </nav>
      : null}
    </>}
  </main>;
}
