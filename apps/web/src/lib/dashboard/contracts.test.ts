import { describe, expect, it } from "vitest";
import { parseDashboard, parseDashboardHistory } from "./contracts";

describe("dashboard contracts", () => {
  it("accepts activity metrics and preserves explicit zero values", () => {
    const dashboard = parseDashboard({
      from: "2026-10-24",
      to: "2026-10-30",
      generatedAt: "2026-10-30T10:00:00Z",
      scope: "integrante",
      indicators: { overdueActivitiesNow: 0, "activities.em_execucao": 0 },
      columns: ["activityId", "activity"],
      rows: [["1", "Revisar proposta"]],
      unavailableMetrics: [],
    });
    expect(dashboard?.indicators.overdueActivitiesNow).toBe(0);
  });

  it("rejects malformed dashboard payloads", () => {
    expect(parseDashboard({ indicators: { projects: "0" } })).toBeNull();
    expect(parseDashboard({ from: "2026-10-24", to: "2026-10-30", generatedAt: "2026-10-30T10:00:00Z", scope: "integrante", indicators: { projects: -1 }, columns: [], rows: [], unavailableMetrics: [] })).toBeNull();
  });

  it("parses historical events without inventing state snapshots", () => {
    expect(parseDashboardHistory({ columns: ["action", "occurredAt"], rows: [["STATUS_CHANGED", "2026-10-30T10:00:00Z"]], total: 1 }))
      .toEqual({ columns: ["action", "occurredAt"], rows: [["STATUS_CHANGED", "2026-10-30T10:00:00Z"]], total: 1 });
    expect(parseDashboardHistory({ columns: [], rows: [], total: -1 })).toBeNull();
  });
});
