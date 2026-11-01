import { beforeEach, describe, expect, it, vi } from "vitest";

const mocks = vi.hoisted(() => ({
  getSession: vi.fn(),
  postAttendance: vi.fn(),
}));

vi.mock("../auth/server-session", () => ({ getAuthenticatedNiteSession: mocks.getSession }));
vi.mock("./mutations", () => ({ postAttendance: mocks.postAttendance }));
vi.mock("next/navigation", () => ({ redirect: (path: string) => { throw new Error(`redirect:${path}`); } }));

import { registerAttendance } from "./actions";

function form(values: Record<string, string>): FormData {
  const result = new FormData();
  for (const [key, value] of Object.entries(values)) result.set(key, value);
  return result;
}

describe("ações de presença", () => {
  beforeEach(() => {
    mocks.getSession.mockResolvedValue({ bearer: "server-only-token" });
    mocks.postAttendance.mockResolvedValue({ issue: null });
  });

  it("envia check-in com o QR temporário e o encontro selecionado", async () => {
    const result = await registerAttendance({ status: "idle" }, form({
      intent: "check-in", eventId: "a8e0f5a9-6d41-49a8-92d1-42a47b19e9c8", payload: "temporary-qr",
    }));
    expect(mocks.postAttendance).toHaveBeenCalledWith("/api/v1/me/attendance/check-in", "server-only-token", {
      eventId: "a8e0f5a9-6d41-49a8-92d1-42a47b19e9c8", payload: "temporary-qr",
    });
    expect(result.status).toBe("success");
  });

  it("envia saída versionada e retorno vinculado à própria presença", async () => {
    await registerAttendance({ status: "idle" }, form({ intent: "check-out", expectedVersion: "7" }));
    expect(mocks.postAttendance).toHaveBeenLastCalledWith("/api/v1/me/attendance/check-out", "server-only-token", { expectedVersion: 7 });

    await registerAttendance({ status: "idle" }, form({
      intent: "return", attendanceId: "a8e0f5a9-6d41-49a8-92d1-42a47b19e9c8", expectedVersion: "8", payload: "return-qr",
    }));
    expect(mocks.postAttendance).toHaveBeenLastCalledWith(
      "/api/v1/me/attendance/a8e0f5a9-6d41-49a8-92d1-42a47b19e9c8/return", "server-only-token", { expectedVersion: 8, payload: "return-qr" },
    );
  });

  it("não chama a API com QR inválido e traduz conflito sem indicar sucesso", async () => {
    const invalid = await registerAttendance({ status: "idle" }, form({ intent: "check-in", eventId: "x", payload: "qr" }));
    expect(invalid.status).toBe("error");
    expect(mocks.postAttendance).not.toHaveBeenCalled();

    mocks.postAttendance.mockResolvedValueOnce({ issue: "conflict" });
    const conflict = await registerAttendance({ status: "idle" }, form({
      intent: "check-in", eventId: "a8e0f5a9-6d41-49a8-92d1-42a47b19e9c8", payload: "expired-qr",
    }));
    expect(conflict).toMatchObject({ status: "error" });
    expect(conflict.message).toContain("QR expirou");
  });
});
