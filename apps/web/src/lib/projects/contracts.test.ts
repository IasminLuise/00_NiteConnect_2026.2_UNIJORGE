import { describe, expect, it } from "vitest";
import { parseLeadershipProjects } from "./contracts";

describe("contratos dos projetos sob liderança", () => {
  it("mantém apenas projetos com vínculo de liderança atual", () => {
    expect(parseLeadershipProjects({ items: [
      { projectId: "current", title: "Projeto atual", endedAt: null },
      { projectId: "past", title: "Projeto anterior", endedAt: "2026-01-01T00:00:00Z" },
    ] })).toEqual([{ projectId: "current", title: "Projeto atual" }]);
  });

  it("rejeita um contrato inconsistente", () => {
    expect(parseLeadershipProjects({ items: [{ projectId: "current", title: "Projeto atual", endedAt: false }] })).toBeNull();
  });
});
