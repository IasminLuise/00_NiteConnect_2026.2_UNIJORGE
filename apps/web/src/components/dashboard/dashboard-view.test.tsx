import { renderToStaticMarkup } from "react-dom/server";
import { describe, expect, it } from "vitest";
import { DashboardView, priorityEmptyMessage, recentEventLabel } from "./dashboard-view";
import type { DashboardResponse, ParticipationWeeklySummary } from "../../lib/dashboard/contracts";

describe("mensagem de prioridades do painel", () => {
  it("não interpreta indicadores ausentes como zero", () => {
    expect(priorityEmptyMessage("personal", {})).toBe("Não foi possível verificar atividades atrasadas e registros aguardando revisão nesta consulta.");
    expect(priorityEmptyMessage("personal", { overdueActivitiesNow: 0 })).toBe("Não foi possível verificar registros aguardando revisão nesta consulta.");
  });

  it("informa ausência de atraso quando zero foi retornado para o projeto", () => {
    expect(priorityEmptyMessage("project", { overdueActivitiesNow: 0 })).toBe("Nenhuma pendência identificada nos dados consultados.");
    expect(priorityEmptyMessage("project", {})).toBe("Não foi possível verificar atividades atrasadas nesta consulta.");
  });

  it("separa uma ausência confirmada de um indicador que não veio", () => {
    expect(priorityEmptyMessage("personal", { overdueActivitiesNow: 0, "participation.pendente": 0 })).toBe("Nenhuma pendência identificada nos dados consultados.");
    expect(priorityEmptyMessage("personal", { overdueActivitiesNow: 0, "participation.pendente": 0 }, false)).toContain("Não há atividades exibidas neste recorte");
    expect(priorityEmptyMessage("personal", { overdueActivitiesNow: 0 }, true)).toContain("Não foi possível verificar registros aguardando revisão");
  });
});

describe("rótulos de eventos recentes", () => {
  it("distingue uma atualização de projeto de uma atualização de atividade", () => {
    expect(recentEventLabel("PROJECT", "UPDATED")).toBe("Projeto atualizado");
    expect(recentEventLabel("ACTIVITY", "UPDATED")).toBe("Atividade atualizada");
  });
});

describe("filtros do painel", () => {
  it("mantém o intervalo e a busca restrita aos registros com largura máxima", () => {
    const result: DashboardResponse = {
      from: "2026-10-25",
      to: "2026-10-31",
      generatedAt: "2026-10-31T12:00:00Z",
      scope: "integrante",
      indicators: { recordsInPeriod: 2 },
      columns: ["activityId", "project", "activity", "executor", "status", "createdAt", "dueOn", "completedAt", "overdueNow"],
      rows: [
        ["a1", "Projeto Aurora", "Revisar proposta", "Pessoa A", "PLANEJADA", "2026-10-26", "", "", "false"],
        ["a2", "Projeto Aurora", "Preparar oficina", "Pessoa B", "EM_EXECUCAO", "2026-10-27", "", "", "false"],
      ],
      unavailableMetrics: [],
    };
    const markup = renderToStaticMarkup(
      <DashboardView
        result={{ data: result, issue: null }}
        historyResult={null}
        weeklyResult={null}
        scope="personal"
        scopeLabel="Meu espaço"
        contextValue="personal"
        from="2026-10-25"
        to="2026-10-31"
        search="oficina"
      />,
    );

    expect(markup).toContain('aria-label="Filtros do painel"');
    expect(markup).toContain('type="date"');
    expect(markup).toContain('name="from" value="2026-10-25"');
    expect(markup).toContain('name="to" value="2026-10-31"');
    expect(markup).toContain('type="search"');
    expect(markup).toContain('maxLength="120"');
    expect(markup).toContain('name="q" value="oficina"');
    expect(markup).toContain("Aplicar filtros");
    expect(markup).toContain('type="date" class="h-11');
    expect(markup).toContain('type="search" maxLength="120" placeholder="Atividade ou projeto" class="h-11');
    expect(markup).not.toContain("col-span-2 min-h-10 sm:col-span-1");
    expect(markup).toContain("Preparar oficina");
    expect(markup).not.toContain("Revisar proposta");
    expect(markup).toContain("sm:max-w-[24rem]");
  });
});

describe("carga horária do painel pessoal", () => {
  it("não mostra exigência nem saldo sem política aplicável e preserva as horas por situação", () => {
    const dashboard: DashboardResponse = {
      from: "2026-10-25", to: "2026-10-31", generatedAt: "2026-10-31T12:00:00Z", scope: "integrante",
      indicators: {}, columns: [], rows: [], unavailableMetrics: [],
    };
    const weekly: ParticipationWeeklySummary = {
      zone: "America/Bahia", weekStart: "2026-10-25", weekEnd: "2026-10-31",
      modalities: {
        presential: { requiredSeconds: null, registeredSeconds: 3_600, pendingSeconds: 1_800, validatedSeconds: 1_800, refusedSeconds: 0, openPeriods: 1, remainingSeconds: null },
        remote: { requiredSeconds: null, registeredSeconds: 0, pendingSeconds: 0, validatedSeconds: 0, refusedSeconds: 0, openPeriods: 0, remainingSeconds: null },
      },
      loadPolicy: {
        status: "SEM_POLITICA_APLICAVEL", currentConfiguration: null, applicableToWeek: null,
        institutionalConfirmation: { presentialHours: 5, remoteHours: 4, status: "CONFIRMADA" },
        plannedConfiguration: null,
      },
    };
    const markup = renderToStaticMarkup(<DashboardView
      result={{ data: dashboard, issue: null }} historyResult={null} weeklyResult={{ data: weekly, issue: null }}
      scope="personal" scopeLabel="Meu espaço" contextValue="personal" from={weekly.weekStart} to={weekly.weekEnd} search=""
    />);

    expect(markup).toContain("5h presenciais e 4h remotas");
    expect(markup).toContain("Não há política aplicável a esta semana; exigência e saldo não são exibidos.");
    expect(markup).toContain("Registrado");
    expect(markup).toContain("Pendente");
    expect(markup).toContain("Validado");
    expect(markup).not.toContain("Exigido");
    expect(markup).not.toContain("Saldo");
    expect(markup).toContain("1h 00min");
    expect(markup).toContain("30min");
  });

  it("explica que a política aprovada aguarda a data oficial sem exibir saldo", () => {
    const dashboard: DashboardResponse = {
      from: "2026-10-25", to: "2026-10-31", generatedAt: "2026-10-31T12:00:00Z", scope: "integrante",
      indicators: {}, columns: [], rows: [], unavailableMetrics: [],
    };
    const weekly: ParticipationWeeklySummary = {
      zone: "America/Bahia", weekStart: "2026-10-25", weekEnd: "2026-10-31",
      modalities: {
        presential: { requiredSeconds: null, registeredSeconds: 0, pendingSeconds: 0, validatedSeconds: 0, refusedSeconds: 0, openPeriods: 0, remainingSeconds: null },
        remote: { requiredSeconds: null, registeredSeconds: 0, pendingSeconds: 0, validatedSeconds: 0, refusedSeconds: 0, openPeriods: 0, remainingSeconds: null },
      },
      loadPolicy: {
        status: "SEM_POLITICA_APLICAVEL", currentConfiguration: null, applicableToWeek: null,
        institutionalConfirmation: { presentialHours: 5, remoteHours: 4, status: "CONFIRMADA" },
        plannedConfiguration: {
          id: "policy-2027", version: 2, presentialHours: 5, remoteHours: 4,
          effectiveFrom: null, intendedSemester: "2027.1",
          effectiveWindow: "PRIMEIRA_SEMANA_OFICIAL_DE_ATIVIDADES", status: "SEM_VIGENCIA_DEFINIDA",
        },
      },
    };
    const markup = renderToStaticMarkup(<DashboardView
      result={{ data: dashboard, issue: null }} historyResult={null} weeklyResult={{ data: weekly, issue: null }}
      scope="personal" scopeLabel="Meu espaço" contextValue="personal" from={weekly.weekStart} to={weekly.weekEnd} search=""
    />);

    expect(markup).toContain("Política v2 de 5h presenciais e 4h remotas aprovada para 2027.1; aguarda a data oficial.");
    expect(markup).not.toContain("Exigido");
    expect(markup).not.toContain("Saldo");
  });
});
