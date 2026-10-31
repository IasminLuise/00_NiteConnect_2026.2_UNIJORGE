import "server-only";

import type { ApiResult } from "./contracts";

export async function getApiJson<T>(path: string, bearer: string, parse: (value: unknown) => T | null): Promise<ApiResult<T>> {
  const baseUrl = process.env.API_BASE_URL;
  if (!baseUrl) return { data: null, issue: "unavailable" };

  let url: URL;
  try {
    url = new URL(path, baseUrl);
    if (url.protocol !== "https:" && !(url.protocol === "http:" && ["localhost", "127.0.0.1"].includes(url.hostname))) {
      return { data: null, issue: "unavailable" };
    }
  } catch {
    return { data: null, issue: "unavailable" };
  }

  try {
    const response = await fetch(url, {
      method: "GET",
      headers: { Authorization: `Bearer ${bearer}`, Accept: "application/json" },
      cache: "no-store",
      redirect: "error",
      signal: AbortSignal.timeout(8_000),
    });
    if (response.status === 401 || response.status === 403) return { data: null, issue: "access" };
    if (!response.ok) return { data: null, issue: "unavailable" };
    const data = parse(await response.json());
    return data ? { data, issue: null } : { data: null, issue: "invalid" };
  } catch {
    return { data: null, issue: "unavailable" };
  }
}
