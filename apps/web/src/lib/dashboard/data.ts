import "server-only";

import { headers } from "next/headers";
import { getToken, type JWT } from "next-auth/jwt";
import { auth, authSessionCookieName } from "../../../auth";
import {
  parseDashboard,
  parseDashboardHistory,
  parseLeadershipProjects,
  parseParticipationWeeklySummary,
  type DashboardHistory,
  type DashboardResponse,
  type LeadershipProject,
  type ParticipationWeeklySummary,
} from "./contracts";

export type DashboardIssue = "access" | "unavailable" | "invalid";
export type ApiResult<T> = { data: T; issue: null } | { data: null; issue: DashboardIssue };

function accessToken(token: JWT | null): string | null {
  const bearer = token?.accessToken;
  return typeof bearer === "string" && bearer.length > 0 ? bearer : null;
}

export async function getDashboardSession() {
  const [session, token] = await Promise.all([
    auth(),
    process.env.AUTH_SECRET
      ? getToken({
          req: { headers: await headers() },
          secret: process.env.AUTH_SECRET,
          cookieName: authSessionCookieName,
          secureCookie: process.env.NODE_ENV === "production",
        })
      : Promise.resolve(null),
  ]);
  if (!session?.niteMember || session.error) return { session: null, issue: "access" as const };
  const bearer = accessToken(token);
  if (!bearer) return { session: null, issue: "access" as const };
  return { session: { member: session.niteMember, roles: session.niteRoles ?? [], bearer }, issue: null };
}

async function getApiJson<T>(path: string, bearer: string, parse: (value: unknown) => T | null): Promise<ApiResult<T>> {
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

export async function getDashboard(
  bearer: string,
  scope: "personal" | "institutional" | "project",
  from: string,
  to: string,
  projectId?: string,
): Promise<ApiResult<DashboardResponse>> {
  const query = new URLSearchParams({ from, to });
  const path = scope === "personal"
    ? `/api/v1/me/dashboard?${query}`
    : scope === "institutional"
      ? `/api/v1/analytics/dashboard?${query}`
      : `/api/v1/projects/${encodeURIComponent(projectId ?? "")}/dashboard?${query}`;
  return getApiJson(path, bearer, parseDashboard);
}

export async function getLeadershipProjects(bearer: string): Promise<ApiResult<LeadershipProject[]>> {
  return getApiJson(
    "/api/v1/me/projects/leadership/history?page=0&size=100",
    bearer,
    parseLeadershipProjects,
  );
}

export async function getParticipationWeeklySummary(bearer: string, from: string, to: string): Promise<ApiResult<ParticipationWeeklySummary>> {
  const query = new URLSearchParams({ from, to });
  return getApiJson(`/api/v1/me/participation-weekly-summary?${query}`, bearer, parseParticipationWeeklySummary);
}

export async function getDashboardHistory(
  bearer: string,
  scope: "project" | "institutional",
  from: string,
  to: string,
  projectId?: string,
): Promise<ApiResult<DashboardHistory>> {
  const pathForPage = (page: number) => {
    const query = new URLSearchParams({ from, to, page: String(page), size: "200" });
    return scope === "institutional"
      ? `/api/v1/analytics/history?${query}`
      : `/api/v1/projects/${encodeURIComponent(projectId ?? "")}/analytics/history?${query}`;
  };
  const firstPage = await getApiJson(pathForPage(0), bearer, parseDashboardHistory);
  if (!firstPage.data || firstPage.data.total <= firstPage.data.rows.length) return firstPage;
  if (firstPage.data.total > 2_000) return { data: null, issue: "unavailable" };
  const lastPage = Math.ceil(firstPage.data.total / 200) - 1;
  return getApiJson(pathForPage(lastPage), bearer, parseDashboardHistory);
}
