import "server-only";

import { getApiJson } from "../api/server";
import type { ApiResult } from "../api/contracts";
import {
  parseDashboard,
  parseDashboardHistory,
  type DashboardHistory,
  type DashboardResponse,
} from "./contracts";

export type { ApiResult };

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
