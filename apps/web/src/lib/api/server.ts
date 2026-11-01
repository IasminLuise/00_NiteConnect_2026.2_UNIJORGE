import "server-only";

import { apiIssueFromStatus, type ApiResult } from "./contracts";

export function resolveApiUrl(path: string): URL | null {
  const baseUrl = process.env.API_BASE_URL;
  if (!baseUrl) return null;

  try {
    const url = new URL(path, baseUrl);
    if (url.protocol !== "https:" && !(url.protocol === "http:" && ["localhost", "127.0.0.1"].includes(url.hostname))) return null;
    return url;
  } catch {
    return null;
  }
}

export async function getApiJson<T>(path: string, bearer: string, parse: (value: unknown) => T | null): Promise<ApiResult<T>> {
  const url = resolveApiUrl(path);
  if (!url) return { data: null, issue: "unavailable" };

  try {
    const response = await fetch(url, {
      method: "GET",
      headers: { Authorization: `Bearer ${bearer}`, Accept: "application/json" },
      cache: "no-store",
      redirect: "error",
      signal: AbortSignal.timeout(8_000),
    });
    if (!response.ok) return { data: null, issue: apiIssueFromStatus(response.status) };
    const data = parse(await response.json());
    return data ? { data, issue: null } : { data: null, issue: "invalid" };
  } catch {
    return { data: null, issue: "unavailable" };
  }
}
