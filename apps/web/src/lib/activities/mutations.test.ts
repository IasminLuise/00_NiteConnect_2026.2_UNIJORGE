import { describe, expect, it, vi } from "vitest";
import { activityMutationIssueFromStatus } from "./mutations";

vi.mock("server-only", () => ({}));

describe("classificação das respostas de mutação de atividade", () => {
  it("classifica HTTP 400 como entrada inválida", () => {
    expect(activityMutationIssueFromStatus(400)).toBe("invalid");
  });
});
