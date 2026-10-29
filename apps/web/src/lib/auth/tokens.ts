type TokenEndpointResponse = {
  access_token: string;
  expires_in: number;
  refresh_token?: string;
};

export type RefreshedToken = {
  accessToken: string;
  refreshToken: string;
  expiresAt: number;
};

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null && !Array.isArray(value);
}

function isTokenEndpointResponse(value: unknown): value is TokenEndpointResponse {
  return (
    isRecord(value) &&
    typeof value.access_token === "string" &&
    typeof value.expires_in === "number" &&
    Number.isFinite(value.expires_in) &&
    value.expires_in > 0 &&
    (value.refresh_token === undefined || typeof value.refresh_token === "string")
  );
}

export async function refreshEntraToken(
  input: {
    refreshToken: string;
    tenantId: string;
    clientId: string;
    clientSecret: string;
    apiScope: string;
  },
  fetcher: typeof fetch = fetch,
): Promise<RefreshedToken> {
  const endpoint = new URL(
    `https://login.microsoftonline.com/${encodeURIComponent(input.tenantId)}/oauth2/v2.0/token`,
  );
  const response = await fetcher(endpoint, {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded", Accept: "application/json" },
    body: new URLSearchParams({
      client_id: input.clientId,
      client_secret: input.clientSecret,
      grant_type: "refresh_token",
      refresh_token: input.refreshToken,
      scope: `openid profile email offline_access ${input.apiScope}`,
    }),
    cache: "no-store",
    redirect: "error",
    signal: AbortSignal.timeout(5_000),
  });

  if (!response.ok) throw new Error("A sessão Microsoft expirou.");

  const payload: unknown = await response.json();
  if (!isTokenEndpointResponse(payload)) {
    throw new Error("A Microsoft retornou uma resposta de token inválida.");
  }

  return {
    accessToken: payload.access_token,
    refreshToken: typeof payload.refresh_token === "string" && payload.refresh_token.length > 0
      ? payload.refresh_token
      : input.refreshToken,
    expiresAt: Date.now() + payload.expires_in * 1_000,
  };
}
