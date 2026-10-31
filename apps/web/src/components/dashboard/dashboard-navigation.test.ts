import { describe, expect, it } from "vitest";
import { buildDashboardNavigationGroups } from "./dashboard-navigation";

const base = {
  contextValue: "personal",
  projects: [],
  canAdminister: false,
  canCoordinate: false,
  from: "2026-10-25",
  to: "2026-10-31",
  search: "reunião",
};

describe("navegação do painel", () => {
  it("mantém itens pessoais funcionais e sinaliza módulos sem tela", () => {
    const groups = buildDashboardNavigationGroups(base);
    const personal = groups.find(({ label }) => label === "Meu espaço");

    expect(groups.map(({ label }) => label)).toEqual(["Início", "Meu espaço"]);
    expect(personal?.items.find(({ label }) => label === "Minhas atividades")?.href).toContain("#records");
    expect(personal?.items.find(({ label }) => label === "Minha participação")?.href).toContain("q=reuni%C3%A3o");
    const agenda = personal?.items.find(({ label }) => label === "Agenda");
    expect(agenda?.status).toBe("development");
    expect(agenda?.href).toBeUndefined();
    expect(groups.flatMap(({ items }) => items).some(({ label }) => label === "Prioridades")).toBe(false);
  });

  it("mostra liderança somente para projetos autorizados", () => {
    const groups = buildDashboardNavigationGroups({
      ...base,
      contextValue: "project:project-1",
      projects: [{ projectId: "project-1", title: "Projeto de teste" }],
    });
    const leadership = groups.find(({ label }) => label === "Projetos que lidero");

    expect(leadership?.items[0]).toMatchObject({
      label: "Acompanhamento",
      href: expect.stringContaining("scope=project%3Aproject-1"),
    });
    expect(leadership?.items[1]).toMatchObject({ label: "Atividades do projeto", status: "development" });
    expect(groups.some(({ label }) => label === "Administração")).toBe(false);
  });

  it("expõe navegação institucional e acadêmica conforme os papéis recebidos", () => {
    const groups = buildDashboardNavigationGroups({
      ...base,
      canAdminister: true,
      canCoordinate: true,
    });
    const institutional = groups.find(({ label }) => label === "Acompanhamento do NITE");
    const academic = groups.find(({ label }) => label === "Acompanhamento acadêmico");

    expect(institutional?.items.find(({ label }) => label === "Projetos")?.href).toContain("scope=institutional");
    expect(institutional?.items.find(({ label }) => label === "Frequência e revisões")?.href).toContain("#participation");
    const reports = institutional?.items.find(({ label }) => label === "Relatórios");
    expect(reports?.status).toBe("development");
    expect(reports?.href).toBeUndefined();
    expect(academic?.items.map(({ label }) => label)).toEqual(["Avaliações", "Revisões e homologações"]);
  });
});
