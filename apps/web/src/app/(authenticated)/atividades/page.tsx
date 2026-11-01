import Link from "next/link";
import { requireAuthenticatedNiteSession } from "../../../lib/auth/server-session";
import { getMyActivities } from "../../../lib/activities/data";
import { activityQuery } from "../../../lib/activities/query";
import type { ActivityStatus } from "../../../lib/activities/contracts";

export const dynamic = "force-dynamic";
export const metadata = { title: "Minhas atividades" };
const labels: Record<ActivityStatus, string> = { PLANEJADA: "Planejada", EM_EXECUCAO: "Em execução", CONCLUIDA: "Concluída", CANCELADA: "Cancelada" };
const tabs = [["", "Todas"], ["PLANEJADA", "Planejadas"], ["EM_EXECUCAO", "Em execução"], ["CONCLUIDA", "Concluídas"], ["CANCELADA", "Canceladas"]] as const;
const linkClass = "rounded-control px-3 py-2 text-type-body-compact focus-visible:outline-3 focus-visible:outline-offset-2 focus-visible:outline-nite-focus";
type Params = Record<string, string | string[] | undefined>;
export default async function ActivitiesPage({ searchParams }: { searchParams: Promise<Params> }) {
  const params = await searchParams;
  const query = activityQuery(params);
  const session = await requireAuthenticatedNiteSession();
  const result = await getMyActivities(session.bearer, query);
  const status = query.get("status") ?? "";
  const selected = (value: string, page = 0) => {
    const next = new URLSearchParams(query);
    next.set("page", String(page));
    if (value) next.set("status", value); else next.delete("status");
    return `/atividades?${next}`;
  };
  const filtered = Boolean(status || query.has("dueFrom") || query.has("dueTo") || query.has("overdue"));
  return <main className="mx-auto w-full max-w-5xl min-w-0 space-y-8 px-4 py-8 sm:px-8 sm:py-12">
    <header className="space-y-2"><h1 className="font-display text-type-page-title text-nite-text-primary">Minhas atividades</h1><p className="text-type-body text-nite-text-secondary">Acompanhe o que está sob sua responsabilidade.</p></header>
    <nav aria-label="Situação das atividades" className="flex flex-wrap gap-1 border-b border-nite-border-soft pb-2">{tabs.map(([value, label]) => <Link key={label} href={selected(value)} aria-current={status === value ? "page" : undefined} className={`${linkClass} ${status === value ? "bg-nite-brand-soft font-medium text-nite-brand-primary" : "text-nite-text-secondary hover:bg-nite-surface-subtle"}`}>{label}</Link>)}</nav>
    <form className="flex flex-wrap items-end gap-3" action="/atividades">
      {status ? <input type="hidden" name="status" value={status} /> : null}
      <label className="grid gap-1 text-type-metadata text-nite-text-secondary">Prazo a partir de<input type="date" name="dueFrom" defaultValue={query.get("dueFrom") ?? ""} className="rounded-control border border-nite-border-soft bg-nite-surface p-2 text-nite-text-primary" /></label>
      <label className="grid gap-1 text-type-metadata text-nite-text-secondary">Prazo até<input type="date" name="dueTo" defaultValue={query.get("dueTo") ?? ""} className="rounded-control border border-nite-border-soft bg-nite-surface p-2 text-nite-text-primary" /></label>
      <label className="flex items-center gap-2 py-2 text-type-body-compact text-nite-text-secondary"><input type="checkbox" name="overdue" value="true" defaultChecked={query.has("overdue")} /> Em atraso</label>
      <button className={`${linkClass} border border-nite-border-soft text-nite-text-primary`} type="submit">Aplicar filtros</button>
      {filtered ? <Link className={`${linkClass} text-nite-text-secondary`} href="/atividades">Limpar filtros</Link> : null}
    </form>
    {result.issue ? <p role="alert" className="py-8 text-nite-text-secondary">{result.issue === "access" ? "Você não tem acesso a esta consulta." : "Não foi possível carregar suas atividades. Tente novamente."}</p> : result.data.items.length === 0 ? <div className="py-16 text-center"><p className="font-medium text-nite-text-primary">{filtered ? "Nenhuma atividade encontrada" : "Nenhuma atividade atribuída por enquanto"}</p><p className="mt-2 text-type-body-compact text-nite-text-secondary">{filtered ? "Experimente ajustar os filtros." : "Quando houver atividades para você, elas aparecerão aqui."}</p></div> : <>
      <ul className="divide-y divide-nite-border-soft border-y border-nite-border-soft">{result.data.items.map((activity) => <li key={activity.id} className="flex min-w-0 flex-wrap items-center justify-between gap-x-6 gap-y-2 py-4"><div className="min-w-0"><p className="break-words font-medium text-nite-text-primary">{activity.title}</p><p className="mt-1 text-type-metadata text-nite-text-secondary">{activity.projectId ? "Atividade de projeto" : "Atividade independente"}</p></div><div className="flex flex-wrap items-center gap-3 text-type-metadata text-nite-text-secondary"><span>{labels[activity.status]}</span>{activity.dueOn ? <time dateTime={activity.dueOn}>{activity.dueOn.split("-").reverse().join("/")}</time> : null}{activity.overdue ? <span className="font-medium">Em atraso</span> : null}</div></li>)}</ul>
      <nav aria-label="Paginação" className="flex items-center justify-between gap-3 text-type-body-compact">{result.data.page > 0 ? <Link className={linkClass} href={selected(status, result.data.page - 1)}>Anterior</Link> : <span /> }<span className="text-nite-text-secondary">Página {result.data.page + 1}</span>{(result.data.page + 1) * result.data.size < result.data.total ? <Link className={linkClass} href={selected(status, result.data.page + 1)}>Próxima</Link> : <span />}</nav>
    </>}
  </main>;
}
