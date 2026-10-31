import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

const { authMock, getTokenMock, headersMock, redirectMock } = vi.hoisted(() => ({
  authMock: vi.fn(),
  getTokenMock: vi.fn(),
  headersMock: vi.fn(),
  redirectMock: vi.fn((path: string): never => { throw new Error(`redirect:${path}`); }),
}));

vi.mock("server-only", () => ({}));
vi.mock("../../../auth", () => ({ auth: authMock, authSessionCookieName: "nite-connect.session" }));
vi.mock("next-auth/jwt", () => ({ getToken: getTokenMock }));
vi.mock("next/headers", () => ({ headers: headersMock }));
vi.mock("next/navigation", () => ({ redirect: redirectMock }));

import { getAuthenticatedNiteSession, requireAuthenticatedNiteSession } from "./server-session";

describe("sessão autenticada do NITE", () => {
  beforeEach(() => {
    authMock.mockReset();
    getTokenMock.mockReset();
    headersMock.mockReset();
    redirectMock.mockClear();
    vi.stubEnv("AUTH_SECRET", "test-secret");
    vi.stubEnv("NODE_ENV", "development");
    headersMock.mockResolvedValue(new Headers({ cookie: "nite-connect.session=encrypted" }));
  });

  afterEach(() => {
    vi.unstubAllEnvs();
  });

  it("obtém o bearer do JWE no servidor sem ampliar session.user", async () => {
    const member = { id: "member-1", name: "Pessoa de teste", institutionalEmail: "member@example.invalid", status: "ATIVO" as const };
    authMock.mockResolvedValue({ niteMember: member, niteRoles: ["INTEGRANTE"], user: { name: member.name, email: member.institutionalEmail } });
    getTokenMock.mockResolvedValue({ accessToken: "server-only-bearer" });

    await expect(getAuthenticatedNiteSession()).resolves.toEqual({ member, roles: ["INTEGRANTE"], bearer: "server-only-bearer" });
    expect(getTokenMock).toHaveBeenCalledWith(expect.objectContaining({
      cookieName: "nite-connect.session",
      secret: "test-secret",
      secureCookie: false,
    }));
  });

  it("nega sessão sem integrante, com erro da sessão ou sem bearer", async () => {
    authMock.mockResolvedValue(null);
    getTokenMock.mockResolvedValue({ accessToken: "server-only-bearer" });
    await expect(getAuthenticatedNiteSession()).resolves.toBeNull();

    authMock.mockResolvedValue({ niteMember: { id: "member-1" }, niteRoles: ["INTEGRANTE"], error: "RefreshAccessTokenError" });
    await expect(getAuthenticatedNiteSession()).resolves.toBeNull();

    authMock.mockResolvedValue({ niteMember: { id: "member-1" }, niteRoles: ["INTEGRANTE"] });
    getTokenMock.mockResolvedValue({});
    await expect(getAuthenticatedNiteSession()).resolves.toBeNull();
  });

  it("nega a sessão sem segredo de descriptografia", async () => {
    vi.stubEnv("AUTH_SECRET", "");
    authMock.mockResolvedValue({ niteMember: { id: "member-1" }, niteRoles: ["INTEGRANTE"] });

    await expect(getAuthenticatedNiteSession()).resolves.toBeNull();
    expect(getTokenMock).not.toHaveBeenCalled();
  });

  it("redireciona para login quando a sessão não está autenticada", async () => {
    authMock.mockResolvedValue(null);
    getTokenMock.mockResolvedValue(null);

    await expect(requireAuthenticatedNiteSession()).rejects.toThrow("redirect:/login");
    expect(redirectMock).toHaveBeenCalledWith("/login");
  });
});
