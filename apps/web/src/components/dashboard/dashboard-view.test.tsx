import { renderToStaticMarkup } from "react-dom/server";
import { describe, expect, it } from "vitest";
import { DashboardView, priorityEmptyMessage, recentEventLabel } from "./dashboard-view";
import type { DashboardResponse } from "../../lib/dashboard/contracts";

describe("mensagem de prioridades do painel", () => {
  it("não interpreta indicadores ausentes como zero", () => {
    expect(priorityEmptyMessage("personal", {})).toBe("Não foi possível verificar atividades atrasadas nesta consulta.");
    expect(priorityEmptyMessage("personal", { overdueActivitiesNow: 0 })).toBe("Nenhuma pendência identificada nos dados consultados.");
  });

  it("separa ausência confirmada de ausência de registros", () => {
    expect(priorityEmptyMessage("personal", { overdueActivitiesNow: 0 }, false)).toContain("Não há atividades exibidas neste recorte");
    expect(priorityEmptyMessage("project", { overdueActivitiesNow: 0 })).toBe("Nenhuma pendência identificada nos dados consultados.");
  });
});

describe("rótulos de eventos recentes", () => {
  it("distingue atualização de projeto e de atividade", () => {
    expect(recentEventLabel("PROJECT", "UPDATED")).toBe("Projeto atualizado");
    expect(recentEventLabel("ACTIVITY", "UPDATED")).toBe("Atividade atualizada");
  });
});

describe("painel pessoal", () => {
  it("mantém os filtros de atividade e não consulta nem exibe horas de participação", () => {
    const result: DashboardResponse = {
      from: "2026-10-25",
      to: "2026-10-31",
      generatedAt: "2026-10-31T12:00:00Z",
      scope: "integrante",
      indicators: { recordsInPeriod: 1, overdueActivitiesNow: 0, "activities.em_execucao": 1 },
      columns: ["activityId", "project", "activity", "executor", "status", "createdAt", "dueOn", "completedAt", "overdueNow"],
      rows: [["a1", "Projeto Aurora", "Preparar oficina", "Pessoa A", "EM_EXECUCAO", "2026-10-27", "", "", "false"]],
      unavailableMetrics: [],
    };
    const markup = renderToStaticMarkup(<DashboardView
      result={{ data: result, issue: null }} historyResult={null}
      scope="personal" scopeLabel="Meu espaço" contextValue="personal"
      from="2026-10-25" to="2026-10-31" search="oficina"
    />);

    expect(markup).toContain('aria-label="Filtros do painel"');
    expect(markup).toContain('type="date"');
    expect(markup).toContain('name="from" value="2026-10-25"');
    expect(markup).toContain('name="to" value="2026-10-31"');
    expect(markup).toContain('name="q" value="oficina"');
    expect(markup).toContain("Preparar oficina");
    expect(markup).not.toContain("participation");
    expect(markup).not.toContain("h 00min");
    expect(markup).not.toContain("Saldo");
  });
});
