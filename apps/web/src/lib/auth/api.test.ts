import { describe, expect, it } from "vitest";
import { getNiteIdentity, NiteApiUnauthorizedError } from "./api";

const identity = {
  member: {
    id: "db4bd5de-d9b3-4565-8340-066d32c08e11",
    name: "Pessoa Integrante",
    institutionalEmail: "integrante@unijorge.com.br",
    status: "ATIVO",
  },
  roles: ["INTEGRANTE"],
};

describe("getNiteIdentity", () => {
  it("sends the access token only to the configured API and returns its identity", async () => {
    let requestedUrl: URL | undefined;
    let authorization: string | null = null;
    const fetcher: typeof fetch = async (input, init) => {
      requestedUrl = new URL(input.toString());
      authorization = new Headers(init?.headers).get("Authorization");
      return Response.json(identity);
    };

    await expect(getNiteIdentity("secret-access-token", "http://localhost:8080", fetcher)).resolves.toEqual(identity);
    expect(requestedUrl?.href).toBe("http://localhost:8080/api/v1/me");
    expect(authorization).toBe("Bearer secret-access-token");
  });

  it("rejects identities denied by the API", async () => {
    const fetcher: typeof fetch = async () => new Response(null, { status: 403 });

    await expect(getNiteIdentity("token", "http://localhost:8080", fetcher)).rejects.toBeInstanceOf(
      NiteApiUnauthorizedError,
    );
  });

  it("rejects non-HTTPS API URLs outside local development", async () => {
    await expect(getNiteIdentity("token", "http://api.example.com")).rejects.toThrow(
      "API_BASE_URL deve usar HTTPS fora do ambiente local.",
    );
  });
});
