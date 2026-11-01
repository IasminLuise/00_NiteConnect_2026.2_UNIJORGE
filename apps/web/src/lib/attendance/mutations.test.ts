import { afterEach, describe, expect, it, vi } from "vitest";

import { postAttendanceRequest } from "./mutations-core";

describe("mutação de presença autenticada", () => {
  afterEach(() => vi.unstubAllGlobals());

  it("envia bearer, payload JSON e não armazena a resposta em cache", async () => {
    const fetchMock = vi.fn().mockResolvedValue(new Response(null, { status: 201 }));
    vi.stubGlobal("fetch", fetchMock);
    const payload = { eventId: "event-1", payload: "temporary-qr" };
    const url = "http://api.local/api/v1/me/attendance/check-in";
    expect(await postAttendanceRequest(url, "server-token", payload)).toEqual({ issue: null });
    expect(fetchMock).toHaveBeenCalledWith(url, expect.objectContaining({
      method: "POST",
      headers: expect.objectContaining({ Authorization: "Bearer server-token", "Content-Type": "application/json" }),
      body: JSON.stringify(payload),
      cache: "no-store",
      redirect: "error",
    }));
  });

  it("diferencia conflito de permissão e falha de rede", async () => {
    const fetchMock = vi.fn()
      .mockResolvedValueOnce(new Response(null, { status: 409 }))
      .mockResolvedValueOnce(new Response(null, { status: 403 }))
      .mockRejectedValueOnce(new Error("network"));
    vi.stubGlobal("fetch", fetchMock);
    expect(await postAttendanceRequest("http://api.local/check-in", "token", {})).toEqual({ issue: "conflict" });
    expect(await postAttendanceRequest("http://api.local/check-in", "token", {})).toEqual({ issue: "access" });
    expect(await postAttendanceRequest("http://api.local/check-in", "token", {})).toEqual({ issue: "unavailable" });
  });
});
