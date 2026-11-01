import { describe, expect, it } from "vitest";
import { parseExpectedEvent, parsePageResult, parseTimelineItem, parseWeeklySummary } from "./contracts";

const event = {
  id: "a8e0f5a9-6d41-49a8-92d1-42a47b19e9c8", title: "Encontro de tecnologia", description: "",
  kind: "ENCONTRO", modality: "PRESENCIAL", startsAt: "2027-03-01T12:00:00Z", endsAt: "2027-03-01T14:00:00Z",
  status: "ABERTO", expected: true, checkedIn: false, attendanceId: null,
};

describe("participation contracts", () => {
  it("accepts personal expected event rows and rejects missing payload fields", () => {
    expect(parseExpectedEvent(event)?.status).toBe("ABERTO");
    expect(parseExpectedEvent({ ...event, startsAt: undefined })).toBeNull();
  });

  it("preserves null hours for a week without an applicable policy", () => {
    const modality = { requiredSeconds: null, registeredSeconds: 0, pendingSeconds: 0, validatedSeconds: 0,
      refusedSeconds: 0, openPeriods: 0, remainingSeconds: null };
    const summary = parseWeeklySummary({ zone: "America/Bahia", weekStart: "2026-11-02", weekEnd: "2026-11-08",
      modalities: { presential: modality, remote: modality }, loadPolicy: { status: "SEM_POLITICA_APLICAVEL",
        currentConfiguration: null, applicableToWeek: null,
        institutionalConfirmation: { presentialHours: 5, remoteHours: 4, status: "CONFIRMADA" }, plannedConfiguration: null } });
    expect(summary?.modalities.presential.requiredSeconds).toBeNull();
    expect(summary?.modalities.remote.remainingSeconds).toBeNull();
  });

  it("rejects a timeline response that conflates duration or status", () => {
    const item = { id: "a8e0f5a9-6d41-49a8-92d1-42a47b19e9c8", eventId: null, eventTitle: null, eventKind: null,
      eventStatus: null, kind: "WORK_LOG", modality: "REMOTA", startsAt: "2027-03-01T12:00:00Z", endsAt: null,
      durationSeconds: null, description: "Atividade", activityId: null, evidenceId: null, origin: "MANUAL",
      reviewStatus: "PENDENTE", periodStatus: "DECLARADO", replacementAdjustmentId: null, reviewedAt: null,
      reviewReason: null, version: 0 };
    expect(parseTimelineItem(item)?.durationSeconds).toBeNull();
    expect(parseTimelineItem({ ...item, periodStatus: "HORAS_CONFIRMADAS" })).toBeNull();
  });

  it("uses actual zero-based page metadata rather than local slicing", () => {
    expect(parsePageResult({ items: [event], page: 2, size: 20, total: 43 }, parseExpectedEvent)).toMatchObject({ page: 2, total: 43 });
    expect(parsePageResult({ items: [event], page: -1, size: 20, total: 43 }, parseExpectedEvent)).toBeNull();
  });
});
