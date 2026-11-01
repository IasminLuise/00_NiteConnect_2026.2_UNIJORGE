import { describe, expect, it } from "vitest";
import { parseActivityPage, parseActivityCapabilities } from "./contracts";

const activity = { id: "a", title: "Atividade", description: "Descrição", projectId: null, status: "PLANEJADA", dueOn: null, primaryExecutorId: "m", primaryExecutorName: "Pessoa", executorAvailable: true, createdBy: "m", version: 0, createdAt: "2026-10-31T00:00:00Z", updatedAt: "2026-10-31T00:00:00Z", completedAt: null, deadlinePassed: false, overdue: false, attentionRequired: false };

describe("activity API contract", () => {
  it("validates activity capabilities without accepting unknown transitions", () => {
    expect(parseActivityCapabilities({ edit: true, transitions: ["EM_EXECUCAO"], history: true, manageAssignees: false, addEvidence: true, upload: false })?.transitions).toEqual(["EM_EXECUCAO"]);
    expect(parseActivityCapabilities({ edit: true, transitions: ["ATRASADA"], history: true, manageAssignees: false, addEvidence: true, upload: false })).toBeNull();
  });
  it("preserves independent activities and the API overdue condition", () => {
    expect(parseActivityPage({ items: [{ ...activity, overdue: true }], page: 0, size: 20, total: 1 })?.items[0]).toMatchObject({ projectId: null, status: "PLANEJADA", overdue: true });
  });
  it("rejects unknown statuses and malformed pagination", () => {
    expect(parseActivityPage({ items: [{ ...activity, status: "ATRASADA" }], page: 0, size: 20, total: 1 })).toBeNull();
    expect(parseActivityPage({ items: [], page: -1, size: 20, total: 0 })).toBeNull();
  });
});
