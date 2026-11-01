import { beforeEach, describe, expect, it, vi } from "vitest";

const mocks = vi.hoisted(() => ({
  getSession: vi.fn(), getAttachments: vi.fn(), post: vi.fn(), upload: vi.fn(), revalidate: vi.fn(),
}));
vi.mock("next/cache", () => ({ revalidatePath: mocks.revalidate }));
vi.mock("next/navigation", () => ({ redirect: (path: string) => { throw new Error(`REDIRECT:${path}`); } }));
vi.mock("../auth/server-session", () => ({ getAuthenticatedNiteSession: mocks.getSession }));
vi.mock("./data", () => ({ getJustificationAttachments: mocks.getAttachments }));
vi.mock("./mutations", () => ({ postParticipation: mocks.post, uploadJustificationAttachment: mocks.upload }));

import { participationAction, uploadJustificationAction } from "./actions";

const session = { bearer: "server-only-test-token", member: { id: "member" }, roles: ["INTEGRANTE"] };
const redirectMessage = (promise: Promise<void>, path: string) => expect(promise).rejects.toThrow(`REDIRECT:${path}`);

describe("participation actions", () => {
  beforeEach(() => {
    mocks.getSession.mockResolvedValue(session);
    mocks.getAttachments.mockReset();
    mocks.post.mockReset().mockResolvedValue({ issue: null });
    mocks.upload.mockReset().mockResolvedValue({ issue: null });
    mocks.revalidate.mockReset();
  });

  it("sends check-in only to the personal endpoint with the scanned QR payload", async () => {
    const form = new FormData();
    form.set("intent", "check-in"); form.set("eventId", "a8e0f5a9-6d41-49a8-92d1-42a47b19e9c8"); form.set("payload", "temporary-qr-payload");
    await redirectMessage(participationAction(form), "/participacao?feedback=created");
    expect(mocks.post).toHaveBeenCalledWith("/api/v1/me/attendance/check-in", session.bearer, {
      eventId: "a8e0f5a9-6d41-49a8-92d1-42a47b19e9c8", payload: "temporary-qr-payload",
    });
  });

  it.each([
    {
      name: "checkout uses the current attendance version",
      fields: [["intent", "check-out"], ["expectedVersion", "7"]],
      path: "/api/v1/me/attendance/check-out", payload: { expectedVersion: 7 },
    },
    {
      name: "return sends the QR to the personal attendance record",
      fields: [["intent", "return"], ["attendanceId", "a8e0f5a9-6d41-49a8-92d1-42a47b19e9c8"], ["expectedVersion", "8"], ["payload", "session-qr"]],
      path: "/api/v1/me/attendance/a8e0f5a9-6d41-49a8-92d1-42a47b19e9c8/return", payload: { expectedVersion: 8, payload: "session-qr" },
    },
    {
      name: "remote log timestamps use the returned API timezone",
      fields: [["intent", "work-log"], ["zone", "America/Bahia"], ["startsAt", "2027-03-01T09:00"], ["endsAt", "2027-03-01T10:15"], ["description", "Documentação técnica"]],
      path: "/api/v1/me/work-logs", payload: { startsAt: "2027-03-01T12:00:00.000Z", endsAt: "2027-03-01T13:15:00.000Z", description: "Documentação técnica" },
    },
    {
      name: "absence justification is contextual to an event",
      fields: [["intent", "justification"], ["eventId", "a8e0f5a9-6d41-49a8-92d1-42a47b19e9c8"], ["reason", "Conflito acadêmico"]],
      path: "/api/v1/me/absence-justifications", payload: { eventId: "a8e0f5a9-6d41-49a8-92d1-42a47b19e9c8", reason: "Conflito acadêmico" },
    },
    {
      name: "justification complement carries its expected version",
      fields: [["intent", "complement"], ["id", "a8e0f5a9-6d41-49a8-92d1-42a47b19e9c8"], ["expectedVersion", "2"], ["reason", "Informação adicional"]],
      path: "/api/v1/me/absence-justifications/a8e0f5a9-6d41-49a8-92d1-42a47b19e9c8/complements", payload: { expectedVersion: 2, reason: "Informação adicional" },
    },
    {
      name: "correction proposal identifies one personal period",
      fields: [["intent", "correction"], ["target", "period:a8e0f5a9-6d41-49a8-92d1-42a47b19e9c8"], ["zone", "America/Bahia"], ["startsAt", "2027-03-01T09:00"], ["endsAt", "2027-03-01T10:00"], ["reason", "Ajuste do intervalo"]],
      path: "/api/v1/me/attendance/corrections", payload: { periodId: "a8e0f5a9-6d41-49a8-92d1-42a47b19e9c8", startsAt: "2027-03-01T12:00:00.000Z", endsAt: "2027-03-01T13:00:00.000Z", reason: "Ajuste do intervalo" },
    },
    {
      name: "replacement request sends the exact seven-day week",
      fields: [["intent", "replacement"], ["weekStart", "2027-03-01"], ["modality", "REMOTA"], ["hours", "1.5"], ["reason", "Reposição contextual"]],
      path: "/api/v1/me/participation-replacements", payload: { weekStart: "2027-03-01", weekEnd: "2027-03-07", modality: "REMOTA", hours: 1.5, reason: "Reposição contextual" },
    },
    {
      name: "work-log response is a proposal tied to the current version",
      fields: [["intent", "work-log-response"], ["id", "a8e0f5a9-6d41-49a8-92d1-42a47b19e9c8"], ["expectedPeriodVersion", "4"], ["zone", "America/Bahia"], ["startsAt", "2027-03-01T09:00"], ["endsAt", "2027-03-01T10:00"], ["description", "Revisão de protótipo"], ["reason", "Faixa correta"]],
      path: "/api/v1/me/work-logs/review-requests/a8e0f5a9-6d41-49a8-92d1-42a47b19e9c8/responses", payload: { expectedPeriodVersion: 4, startsAt: "2027-03-01T12:00:00.000Z", endsAt: "2027-03-01T13:00:00.000Z", description: "Revisão de protótipo", reason: "Faixa correta" },
    },
  ])("$name", async ({ fields, path, payload }) => {
    const form = new FormData();
    for (const [key, value] of fields) form.set(key, value);
    await redirectMessage(participationAction(form), "/participacao?feedback=created");
    expect(mocks.post).toHaveBeenCalledWith(path, session.bearer, payload);
    expect(path).toContain("/api/v1/me/");
  });

  it("keeps optional receipt upload disabled unless the API says it is available", async () => {
    mocks.getAttachments.mockResolvedValue({ data: { uploadAvailability: "RETENCAO_AGUARDANDO_VALIDACAO", items: [] }, issue: null });
    const form = new FormData();
    form.set("id", "a8e0f5a9-6d41-49a8-92d1-42a47b19e9c8"); form.set("context", "JUSTIFICATIVA");
    form.set("file", new File(["synthetic-pdf"], "proof.pdf", { type: "application/pdf" }));
    await redirectMessage(uploadJustificationAction(form), "/participacao?feedback=forbidden");
    expect(mocks.upload).not.toHaveBeenCalled();
  });

  it("uploads an optional receipt only after availability and file checks", async () => {
    mocks.getAttachments.mockResolvedValue({ data: { uploadAvailability: "DISPONIVEL", items: [] }, issue: null });
    const form = new FormData();
    form.set("id", "a8e0f5a9-6d41-49a8-92d1-42a47b19e9c8"); form.set("context", "JUSTIFICATIVA");
    form.set("file", new File(["synthetic-pdf"], "proof.pdf", { type: "application/pdf" }));
    await redirectMessage(uploadJustificationAction(form), "/participacao?feedback=updated");
    expect(mocks.upload).toHaveBeenCalledWith("a8e0f5a9-6d41-49a8-92d1-42a47b19e9c8", session.bearer, expect.any(File), "JUSTIFICATIVA");
  });
});
