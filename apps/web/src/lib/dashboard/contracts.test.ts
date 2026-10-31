import { describe, expect, it } from "vitest";
import { formatDuration, parseDashboard, parseDashboardHistory, parseLeadershipProjects, parseParticipationWeeklySummary } from "./contracts";

describe("dashboard contracts", () => {
  it("accepts API metrics and preserves explicit zero values", () => {
    const dashboard = parseDashboard({
      from: "2026-10-24",
      to: "2026-10-30",
      generatedAt: "2026-10-30T10:00:00Z",
      scope: "integrante",
      indicators: { "participation.pendente": 0 },
      columns: ["activityId", "activity"],
      rows: [["1", "Revisar proposta"]],
      unavailableMetrics: [],
    });
    expect(dashboard?.indicators["participation.pendente"]).toBe(0);
  });

  it("rejects malformed dashboard payloads", () => {
    expect(parseDashboard({ indicators: { projects: "0" } })).toBeNull();
    expect(parseDashboard({ from: "2026-10-24", to: "2026-10-30", generatedAt: "2026-10-30T10:00:00Z", scope: "integrante", indicators: { projects: -1 }, columns: [], rows: [], unavailableMetrics: [] })).toBeNull();
  });

  it("keeps only current leadership projects", () => {
    expect(parseLeadershipProjects({ items: [
      { projectId: "current", title: "Projeto atual", endedAt: null },
      { projectId: "past", title: "Projeto anterior", endedAt: "2026-01-01T00:00:00Z" },
    ] })).toEqual([{ projectId: "current", title: "Projeto atual" }]);
  });

  it("formats seconds without treating missing metrics as zero", () => {
    expect(formatDuration(19_800)).toBe("5h 30min");
  });

  it("keeps weekly modalities and review states separate", () => {
    const modality = { requiredSeconds: 18_000, registeredSeconds: 7_200, pendingSeconds: 3_600, validatedSeconds: 3_600, refusedSeconds: 0, openPeriods: 0, remainingSeconds: 14_400 };
    const summary = parseParticipationWeeklySummary({ zone: "America/Bahia", weekStart: "2026-10-24", weekEnd: "2026-10-30", modalities: { presential: modality, remote: { ...modality, requiredSeconds: 14_400 } } });
    expect(summary?.modalities.presential.pendingSeconds).toBe(3_600);
    expect(summary?.modalities.remote.requiredSeconds).toBe(14_400);
  });

  it("rejects non-integer weekly counters and invalid history totals", () => {
    const modality = { requiredSeconds: 18_000, registeredSeconds: 7_200.5, pendingSeconds: 3_600, validatedSeconds: 3_600, refusedSeconds: 0, openPeriods: 0, remainingSeconds: 14_400 };
    expect(parseParticipationWeeklySummary({ zone: "America/Bahia", weekStart: "2026-10-24", weekEnd: "2026-10-30", modalities: { presential: modality, remote: modality } })).toBeNull();
    expect(parseDashboardHistory({ columns: [], rows: [], total: -1 })).toBeNull();
  });

  it("parses historical events without inventing state snapshots", () => {
    expect(parseDashboardHistory({ columns: ["action", "occurredAt"], rows: [["STATUS_CHANGED", "2026-10-30T10:00:00Z"]], total: 1 }))
      .toEqual({ columns: ["action", "occurredAt"], rows: [["STATUS_CHANGED", "2026-10-30T10:00:00Z"]], total: 1 });
  });
});
