import { describe, expect, it } from "vitest";
import { formatDuration, parseDashboard, parseDashboardHistory, parseParticipationWeeklySummary } from "./contracts";

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

  it("formats seconds without treating missing metrics as zero", () => {
    expect(formatDuration(19_800)).toBe("5h 30min");
  });

  it("keeps weekly modalities and review states separate", () => {
    const modality = { requiredSeconds: 18_000, registeredSeconds: 7_200, pendingSeconds: 3_600, validatedSeconds: 3_600, refusedSeconds: 0, openPeriods: 0, remainingSeconds: 14_400 };
    const loadPolicy = {
      status: "HOMOLOGADA_NO_PERIODO",
      currentConfiguration: null,
      applicableToWeek: { id: "policy-1", version: 1, effectiveFrom: "2026-10-24", effectiveUntil: null, presentialHours: 5, remoteHours: 4 },
      institutionalConfirmation: { presentialHours: 5, remoteHours: 4, status: "CONFIRMADA" },
      plannedConfiguration: null,
    };
    const summary = parseParticipationWeeklySummary({ zone: "America/Bahia", weekStart: "2026-10-24", weekEnd: "2026-10-30", modalities: { presential: modality, remote: { ...modality, requiredSeconds: 14_400 } }, loadPolicy });
    expect(summary?.modalities.presential.pendingSeconds).toBe(3_600);
    expect(summary?.modalities.remote.requiredSeconds).toBe(14_400);
    expect(summary?.loadPolicy.applicableToWeek?.version).toBe(1);
  });

  it("preserves unavailable weekly requirements without turning them into zero", () => {
    const modality = { requiredSeconds: null, registeredSeconds: 0, pendingSeconds: 0, validatedSeconds: 0, refusedSeconds: 0, openPeriods: 0, remainingSeconds: null };
    const summary = parseParticipationWeeklySummary({
      zone: "America/Bahia", weekStart: "2026-10-24", weekEnd: "2026-10-30",
      modalities: { presential: modality, remote: modality },
      loadPolicy: {
        status: "SEM_POLITICA_APLICAVEL", currentConfiguration: null, applicableToWeek: null,
        institutionalConfirmation: { presentialHours: 5, remoteHours: 4, status: "CONFIRMADA" },
        plannedConfiguration: null,
      },
    });
    expect(summary?.modalities.presential.requiredSeconds).toBeNull();
    expect(summary?.modalities.remote.remainingSeconds).toBeNull();
    expect(summary?.loadPolicy.institutionalConfirmation).toEqual({ presentialHours: 5, remoteHours: 4, status: "CONFIRMADA" });
  });

  it("rejects non-integer weekly counters and invalid history totals", () => {
    const modality = { requiredSeconds: 18_000, registeredSeconds: 7_200.5, pendingSeconds: 3_600, validatedSeconds: 3_600, refusedSeconds: 0, openPeriods: 0, remainingSeconds: 14_400 };
    expect(parseParticipationWeeklySummary({
      zone: "America/Bahia", weekStart: "2026-10-24", weekEnd: "2026-10-30",
      modalities: { presential: modality, remote: modality },
      loadPolicy: {
        status: "HOMOLOGADA_NO_PERIODO", currentConfiguration: null, applicableToWeek: null,
        institutionalConfirmation: { presentialHours: 5, remoteHours: 4, status: "CONFIRMADA" },
        plannedConfiguration: null,
      },
    })).toBeNull();
    expect(parseDashboardHistory({ columns: [], rows: [], total: -1 })).toBeNull();
  });

  it("preserves a future official policy with no invented effective date", () => {
    const modality = { requiredSeconds: null, registeredSeconds: 0, pendingSeconds: 0, validatedSeconds: 0, refusedSeconds: 0, openPeriods: 0, remainingSeconds: null };
    const summary = parseParticipationWeeklySummary({
      zone: "America/Bahia", weekStart: "2026-10-24", weekEnd: "2026-10-30",
      modalities: { presential: modality, remote: modality },
      loadPolicy: {
        status: "SEM_POLITICA_APLICAVEL", currentConfiguration: null, applicableToWeek: null,
        institutionalConfirmation: { presentialHours: 5, remoteHours: 4, status: "CONFIRMADA" },
        plannedConfiguration: {
          id: "policy-2027", version: 2, presentialHours: 5, remoteHours: 4,
          effectiveFrom: null, intendedSemester: "2027.1",
          effectiveWindow: "PRIMEIRA_SEMANA_OFICIAL_DE_ATIVIDADES", status: "SEM_VIGENCIA_DEFINIDA",
        },
      },
    });
    expect(summary?.loadPolicy.plannedConfiguration?.effectiveFrom).toBeNull();
    expect(summary?.modalities.presential.requiredSeconds).toBeNull();
    expect(summary?.modalities.remote.remainingSeconds).toBeNull();
  });

  it("parses historical events without inventing state snapshots", () => {
    expect(parseDashboardHistory({ columns: ["action", "occurredAt"], rows: [["STATUS_CHANGED", "2026-10-30T10:00:00Z"]], total: 1 }))
      .toEqual({ columns: ["action", "occurredAt"], rows: [["STATUS_CHANGED", "2026-10-30T10:00:00Z"]], total: 1 });
  });
});
