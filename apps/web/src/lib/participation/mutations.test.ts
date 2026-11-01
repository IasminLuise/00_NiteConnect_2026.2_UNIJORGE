import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

const { resolveApiUrl } = vi.hoisted(() => ({ resolveApiUrl: vi.fn((path: string) => new URL(path, "http://localhost:8080")) }));
vi.mock("server-only", () => ({}));
vi.mock("../api/server", () => ({ resolveApiUrl }));

import { downloadJustificationAttachment, postParticipation } from "./mutations";

describe("personal participation requests", () => {
  const originalFetch = globalThis.fetch;
  beforeEach(() => resolveApiUrl.mockClear());
  afterEach(() => { globalThis.fetch = originalFetch; });

  it("posts a personal payload with the server-side bearer", async () => {
    const fetchMock = vi.fn().mockResolvedValue(new Response(null, { status: 201 }));
    globalThis.fetch = fetchMock;
    const result = await postParticipation("/api/v1/me/attendance/check-in", "server-bearer", { eventId: "event", payload: "qr" });
    expect(result).toEqual({ issue: null });
    expect(fetchMock).toHaveBeenCalledWith(expect.any(URL), expect.objectContaining({
      method: "POST", headers: expect.objectContaining({ Authorization: "Bearer server-bearer" }),
      body: JSON.stringify({ eventId: "event", payload: "qr" }),
    }));
  });

  it("maps optimistic-version conflicts and invalid fields without reporting success", async () => {
    globalThis.fetch = vi.fn().mockResolvedValue(new Response(null, { status: 409 }));
    expect(await postParticipation("/api/v1/me/attendance/check-out", "server-bearer", { expectedVersion: 1 })).toEqual({ issue: "conflict" });
    globalThis.fetch = vi.fn().mockResolvedValue(new Response(null, { status: 400 }));
    expect(await postParticipation("/api/v1/me/attendance/check-out", "server-bearer", { expectedVersion: -1 })).toEqual({ issue: "invalid" });
  });

  it("downloads only the authenticated personal attachment route and rejects unsafe content types", async () => {
    const bytes = new Uint8Array([37, 80, 68, 70]).buffer;
    globalThis.fetch = vi.fn().mockResolvedValue(new Response(bytes, { status: 200, headers: { "content-type": "application/pdf" } }));
    const result = await downloadJustificationAttachment("own-justification", "own-file", "server-bearer");
    expect(result.issue).toBeNull();
    expect(resolveApiUrl).toHaveBeenLastCalledWith("/api/v1/me/absence-justifications/own-justification/attachments/own-file/download");
    globalThis.fetch = vi.fn().mockResolvedValue(new Response("not a file", { status: 200, headers: { "content-type": "text/html" } }));
    expect((await downloadJustificationAttachment("own-justification", "own-file", "server-bearer")).issue).toBe("invalid");
  });
});
