import type { Metadata } from "next";
import { DashboardView } from "../../../components/dashboard/dashboard-view";
import { requireAuthenticatedNiteSession } from "../../../lib/auth/server-session";
import { getDashboard, getDashboardHistory, getParticipationWeeklySummary } from "../../../lib/dashboard/data";
import { getLeadershipProjects } from "../../../lib/projects/data";

export const metadata: Metadata = { title: "Painel" };
export const dynamic = "force-dynamic";

type SearchParams = Promise<Record<string, string | string[] | undefined>>;
type DashboardPageProps = { searchParams: SearchParams };

function one(value: string | string[] | undefined): string | undefined {
  return Array.isArray(value) ? value[0] : value;
}

function isoDay(date: Date): string {
  return new Intl.DateTimeFormat("en-CA", { timeZone: "America/Bahia" }).format(date);
}

function dateWindow(fromValue?: string, toValue?: string): { from: string; to: string } {
  const today = new Date();
  const defaultTo = isoDay(today);
  const defaultFrom = isoDay(new Date(today.getTime() - 6 * 86_400_000));
  const validDate = (value: string | undefined) => Boolean(
    value && /^\d{4}-\d{2}-\d{2}$/.test(value) &&
    !Number.isNaN(Date.parse(`${value}T00:00:00Z`)) &&
    new Date(`${value}T00:00:00Z`).toISOString().slice(0, 10) === value,
  );
  if (!validDate(fromValue) || !validDate(toValue) || fromValue! > toValue!) return { from: defaultFrom, to: defaultTo };
  const days = (Date.parse(`${toValue}T00:00:00Z`) - Date.parse(`${fromValue}T00:00:00Z`)) / 86_400_000;
  return days > 365 ? { from: defaultFrom, to: defaultTo } : { from: fromValue!, to: toValue! };
}

export default async function DashboardPage({ searchParams }: DashboardPageProps) {
  const params = await searchParams;
  const identity = await requireAuthenticatedNiteSession();

  const canAdminister = identity.roles.some((role) => role === "GESTOR_GERAL" || role === "COORDENADOR");
  const leadershipResult = await getLeadershipProjects(identity.bearer);
  const leadershipProjects = leadershipResult.data ?? [];
  const scopeValue = one(params.scope) ?? "personal";
  const projectId = scopeValue.startsWith("project:") ? scopeValue.slice("project:".length) : undefined;
  const isPersonal = scopeValue === "personal";
  const isInstitutional = scopeValue === "institutional" && canAdminister;
  const selectedProject = projectId ? leadershipProjects.find((project) => project.projectId === projectId) : undefined;
  const allowedScope = isPersonal || isInstitutional || Boolean(selectedProject);
  const range = dateWindow(one(params.from), one(params.to));
  const search = one(params.q)?.slice(0, 120) ?? "";
  const query = allowedScope
      ? await getDashboard(
        identity.bearer,
        isPersonal ? "personal" : isInstitutional ? "institutional" : "project",
        range.from,
        range.to,
        selectedProject?.projectId,
      )
    : { data: null, issue: "access" as const };
  const isSevenDayWindow = Date.parse(`${range.to}T00:00:00Z`) - Date.parse(`${range.from}T00:00:00Z`) === 6 * 86_400_000;
  const [historyResult, weeklyResult] = await Promise.all([
    !isPersonal && allowedScope
      ? getDashboardHistory(identity.bearer, isInstitutional ? "institutional" : "project", range.from, range.to, selectedProject?.projectId)
      : Promise.resolve(null),
    isPersonal && isSevenDayWindow
      ? getParticipationWeeklySummary(identity.bearer, range.from, range.to)
      : Promise.resolve(null),
  ]);
  const scope = isPersonal ? "personal" : isInstitutional ? "institutional" : selectedProject ? "project" : "personal";
  const contextLabel = isPersonal ? "Meu espaço" : isInstitutional ? "Visão geral do NITE" : selectedProject?.title ?? "Meu espaço";
  const contextValue = isPersonal ? "personal" : isInstitutional ? "institutional" : selectedProject ? `project:${selectedProject.projectId}` : "personal";

  return (
    <DashboardView
      result={query}
      scope={scope}
      scopeLabel={contextLabel}
      from={range.from}
      to={range.to}
      search={search}
      contextValue={contextValue}
      historyResult={historyResult}
      weeklyResult={weeklyResult}
    />
  );
}
