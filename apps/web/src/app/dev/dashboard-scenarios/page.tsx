import { notFound } from "next/navigation";
import { AuthenticatedShell } from "../../../components/shell/authenticated-shell";
import { ViewSelector } from "../../../components/dashboard/view-selector";
import { DashboardView } from "../../../components/dashboard/dashboard-view";
import type { DashboardHistory, DashboardResponse } from "../../../lib/dashboard/contracts";

export const dynamic = "force-dynamic";

const from = "2026-10-25";
const to = "2026-10-31";
const generatedAt = "2026-10-31T12:00:00Z";
const projectId = "11111111-1111-4111-8111-111111111111";
const activityColumns = ["activityId", "project", "activity", "executor", "status", "createdAt", "dueOn", "completedAt", "overdueNow"];
const projectColumns = ["projectId", "project", "officialStatus", "activities", "overdueActivitiesNow", "completedActivities", "activeMembers", "openActivities", "healthClassification", "healthReasons"];

const activityRows = [
  ["a1", "Projeto Aurora", "Revisar proposta", "Integrante A", "PLANEJADA", "2026-10-25", "2026-10-28", "", "true"],
  ["a2", "Projeto Aurora", "Preparar oficina", "Integrante B", "EM_EXECUCAO", "2026-10-26", "2026-11-03", "", "false"],
  ["a3", "Projeto Aurora", "Publicar resultado", "Integrante A", "CONCLUIDA", "2026-10-27", "2026-10-30", "2026-10-29", "false"],
  ["a4", "Projeto Aurora", "Reunir equipe", "Integrante C", "CANCELADA", "2026-10-28", "", "", "false"],
];
const projectRows = [
  [projectId, "Projeto Aurora", "ATIVO", "5", "1", "2", "4", "2", "ATENCAO", "Há atividade atrasada"],
  ["22222222-2222-4222-8222-222222222222", "Projeto Horizonte", "PLANEJAMENTO", "2", "0", "0", "2", "2", "DADOS_INSUFICIENTES", ""],
  ["33333333-3333-4333-8333-333333333333", "Projeto Viver", "CONCLUIDO", "3", "0", "3", "3", "0", "NORMAL", ""],
];
const baseIndicators = {
  recordsInPeriod: 0,
  "activities.planejada": 0,
  "activities.em_execucao": 0,
  "activities.concluida": 0,
  "activities.cancelada": 0,
  overdueActivitiesNow: 0,
};

function dashboard(scenario: string, scope: "personal" | "project" | "institutional"): DashboardResponse {
  const projects = scope !== "personal";
  const activeRows = scenario === "empty" ? [] : projects ? projectRows : activityRows;
  const activityIndicators: Record<string, number> = ["activities", "partial", "pending", "projects"].includes(scenario) && !projects ? {
    "activities.planejada": 1, "activities.em_execucao": 1, "activities.concluida": 1, "activities.cancelada": 1,
    overdueActivitiesNow: 1, recordsInPeriod: 4,
  } : {};
  const projectIndicators: Record<string, number> = projects && activeRows.length > 0 ? scope === "project" ? {
    projects: 1, activities: 5, openActivities: 2, completedActivities: 2,
    overdueActivitiesNow: 1, recordsInPeriod: 1,
  } : {
    projects: activeRows.length, activities: 10, openActivities: 4, completedActivities: 5,
    overdueActivitiesNow: 1, recordsInPeriod: activeRows.length,
  } : projects ? { projects: 0, activities: 0, openActivities: 0, completedActivities: 0, overdueActivitiesNow: 0 } : {};
  const indicators: Record<string, number> = {
    ...baseIndicators, ...activityIndicators, ...projectIndicators,
  };
  if (projects) {
    delete indicators["activities.planejada"];
    delete indicators["activities.em_execucao"];
    delete indicators["activities.concluida"];
    delete indicators["activities.cancelada"];
  }
  if (scenario === "partial") {
    delete indicators["activities.cancelada"];
  }
  return {
    from, to, generatedAt, scope, indicators,
    columns: projects ? projectColumns : activityColumns,
    rows: scope === "project" ? activeRows.slice(0, 1) : activeRows,
    unavailableMetrics: scenario === "partial" ? ["Situação cancelada indisponível nesta consulta."] : [],
  };
}

const history: DashboardHistory = {
  columns: ["sourceType", "sourceId", "projectId", "project", "action", "occurredAt", "previousState", "currentState"],
  rows: [
    ["ACTIVITY", "event-1", projectId, "Projeto Aurora", "STATUS_CHANGED", "2026-10-30T16:00:00Z", "PLANEJADA", "EM_EXECUCAO"],
    ["PROJECT", "event-2", projectId, "Projeto Aurora", "UPDATED", "2026-10-29T14:00:00Z", "", ""],
  ],
  total: 2,
};

type Props = { searchParams: Promise<{ scenario?: string; scope?: string }> };

export default async function DashboardScenariosPage({ searchParams }: Props) {
  if (process.env.NODE_ENV !== "development") notFound();
  const params = await searchParams;
  const scenario = ["empty", "activities", "projects", "pending", "partial", "error"].includes(params.scenario ?? "") ? params.scenario! : "empty";
  const scope = params.scope === "institutional" ? "institutional" : params.scope === `project:${projectId}` ? "project" : "personal";
  const contextValue = scope === "project" ? `project:${projectId}` : scope;
  const contextLabel = scope === "personal" ? "Meu espaço" : scope === "project" ? "Projeto Aurora" : "Visão geral do NITE";
  const response = dashboard(scenario, scope);
  const nextContextValue = scope === "personal" ? "institutional" : scope === "institutional" ? `project:${projectId}` : "personal";
  return <AuthenticatedShell
    member={{ id: "fixture", name: "Pessoa de demonstração", institutionalEmail: "demo@example.invalid", status: "ATIVO" }}
    roles={scope === "personal" ? ["INTEGRANTE"] : ["GESTOR_GERAL"]}
    projects={scope === "personal" ? [] : [{ projectId, title: "Projeto Aurora" }]}
    leadershipProjectsUnavailable={false}
  >
    <div role="note" className="mb-4 rounded-control border border-status-warning-border bg-status-warning-soft px-3 py-2 text-type-metadata text-status-warning">
      Prévia local com dados sintéticos. Nenhuma consulta à API ou ao banco institucional.
    </div>
    <nav aria-label="Cenários de validação" className="mb-4 flex flex-wrap gap-2 text-type-metadata">
      {["empty", "activities", "projects", "pending", "partial", "error"].map((item) => <a key={item} href={`/dev/dashboard-scenarios?scenario=${item}&scope=${encodeURIComponent(contextValue)}`} aria-current={scenario === item ? "page" : undefined} className="rounded-control border border-nite-border-soft px-2 py-1 text-nite-text-secondary focus-visible:outline-3 focus-visible:outline-offset-2 focus-visible:outline-nite-focus">{item}</a>)}
      <a href={`/dev/dashboard-scenarios?scenario=${scenario}&scope=${encodeURIComponent(nextContextValue)}`} className="rounded-control border border-nite-border-soft px-2 py-1 text-nite-text-secondary focus-visible:outline-3 focus-visible:outline-offset-2 focus-visible:outline-nite-focus">Trocar contexto de prévia</a>
    </nav>
    <DashboardView
      viewSelector={<ViewSelector id="dashboard-view" projects={scope === "personal" ? [] : [{ projectId, title: "Projeto Aurora" }]} canAdminister={scope !== "personal"} />}
      result={scenario === "error" ? { data: null, issue: "unavailable" } : { data: response, issue: null }}
      historyResult={scope === "personal" ? null : { data: scenario === "empty" ? { ...history, rows: [], total: 0 } : history, issue: null }}
      scope={scope} scopeLabel={contextLabel} contextValue={contextValue} from={from} to={to} search=""
    />
  </AuthenticatedShell>;
}
