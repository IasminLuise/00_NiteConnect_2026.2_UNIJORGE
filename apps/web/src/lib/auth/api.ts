import { parseNiteIdentity, type NiteIdentity } from "./identity";

export class NiteApiUnauthorizedError extends Error {
  constructor() {
    super("A API não autorizou esta identidade.");
    this.name = "NiteApiUnauthorizedError";
  }
}

export async function getNiteIdentity(
  accessToken: string,
  apiBaseUrl: string,
  fetcher: typeof fetch = fetch,
): Promise<NiteIdentity> {
  const base = new URL(apiBaseUrl);
  if (base.protocol !== "https:" && base.hostname !== "localhost" && base.hostname !== "127.0.0.1") {
    throw new TypeError("API_BASE_URL deve usar HTTPS fora do ambiente local.");
  }

  const endpoint = new URL("/api/v1/me", base);
  const response = await fetcher(endpoint, {
    method: "GET",
    headers: { Authorization: `Bearer ${accessToken}`, Accept: "application/json" },
    cache: "no-store",
    redirect: "error",
    signal: AbortSignal.timeout(5_000),
  });

  if (response.status === 401 || response.status === 403) throw new NiteApiUnauthorizedError();
  if (!response.ok) throw new Error("A API do NITE não pôde validar a identidade.");

  const identity = parseNiteIdentity(await response.json());
  if (!identity) throw new NiteApiUnauthorizedError();
  return identity;
}
