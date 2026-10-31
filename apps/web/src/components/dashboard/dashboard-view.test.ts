import { describe, expect, it } from "vitest";
import { priorityEmptyMessage, recentEventLabel } from "./dashboard-view";

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
