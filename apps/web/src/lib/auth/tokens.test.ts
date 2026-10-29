import { describe, expect, it } from "vitest";
import { refreshEntraToken } from "./tokens";

describe("refreshEntraToken", () => {
  it("uses the tenant endpoint, asks for the API scope, and retains an unrotated refresh token", async () => {
    let requestedUrl: URL | undefined;
    let requestBody: URLSearchParams | undefined;
    const fetcher: typeof fetch = async (input, init) => {
      requestedUrl = new URL(input.toString());
      requestBody = new URLSearchParams(init?.body?.toString());
      return Response.json({ access_token: "new-access-token", expires_in: 3600 });
    };

    const result = await refreshEntraToken(
      {
        refreshToken: "old-refresh-token",
        tenantId: "11111111-1111-4111-8111-111111111111",
        clientId: "22222222-2222-4222-8222-222222222222",
        clientSecret: "server-secret",
        apiScope: "api://33333333-3333-4333-8333-333333333333/access_as_user",
      },
      fetcher,
    );

    expect(requestedUrl?.href).toBe(
      "https://login.microsoftonline.com/11111111-1111-4111-8111-111111111111/oauth2/v2.0/token",
    );
    expect(requestBody?.get("scope")).toContain("api://33333333-3333-4333-8333-333333333333/access_as_user");
    expect(requestBody?.get("client_secret")).toBe("server-secret");
    expect(result).toMatchObject({ accessToken: "new-access-token", refreshToken: "old-refresh-token" });
    expect(result.expiresAt).toBeGreaterThan(Date.now());
  });

  it("rejects malformed token responses", async () => {
    const fetcher: typeof fetch = async () => Response.json({ access_token: "token", expires_in: 0 });

    await expect(
      refreshEntraToken(
        {
          refreshToken: "refresh",
          tenantId: "tenant",
          clientId: "client",
          clientSecret: "secret",
          apiScope: "scope",
        },
        fetcher,
      ),
    ).rejects.toThrow("resposta de token inválida");
  });
});
