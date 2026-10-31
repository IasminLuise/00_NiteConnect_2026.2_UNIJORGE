import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

const { authMock, getTokenMock, headersMock } = vi.hoisted(() => ({
  authMock: vi.fn(),
  getTokenMock: vi.fn(),
  headersMock: vi.fn(),
}));

vi.mock("server-only", () => ({}));
vi.mock("../../../auth", () => ({ auth: authMock, authSessionCookieName: "nite-connect.session" }));
vi.mock("next-auth/jwt", () => ({ getToken: getTokenMock }));
vi.mock("next/headers", () => ({ headers: headersMock }));

import { getDashboardSession } from "./data";

describe("sessão do dashboard", () => {
  beforeEach(() => {
    vi.stubEnv("AUTH_SECRET", "test-secret");
    vi.stubEnv("NODE_ENV", "development");
    headersMock.mockResolvedValue(new Headers({ cookie: "nite-connect.session=encrypted" }));
  });

  afterEach(() => {
    vi.unstubAllEnvs();
  });

  it("obtém o bearer do JWT do cookie, sem depender da projeção pública de session.user", async () => {
    const member = { id: "member-1", name: "Pessoa de teste", institutionalEmail: "member@example.invalid", status: "ATIVO" as const };
    authMock.mockResolvedValue({ niteMember: member, niteRoles: ["INTEGRANTE"], user: { name: member.name, email: member.institutionalEmail } });
    getTokenMock.mockResolvedValue({ accessToken: "server-only-bearer" });

    const result = await getDashboardSession();

    expect(result).toEqual({ session: { member, roles: ["INTEGRANTE"], bearer: "server-only-bearer" }, issue: null });
    expect(getTokenMock).toHaveBeenCalledWith(expect.objectContaining({
      cookieName: "nite-connect.session",
      secret: "test-secret",
      secureCookie: false,
    }));
  });

  it("nega o contexto se o JWT não contém bearer", async () => {
    authMock.mockResolvedValue({ niteMember: { id: "member-1" }, niteRoles: ["INTEGRANTE"] });
    getTokenMock.mockResolvedValue({});

    await expect(getDashboardSession()).resolves.toEqual({ session: null, issue: "access" });
  });
});
