import { renderToStaticMarkup } from "react-dom/server";
import { describe, expect, it } from "vitest";
import { DashboardView, priorityEmptyMessage, recentEventLabel } from "./dashboard-view";
import type { DashboardResponse } from "../../lib/dashboard/contracts";

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
