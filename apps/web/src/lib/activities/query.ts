import { activityStatuses } from "./contracts";
type Params = Record<string, string | string[] | undefined>;
const one = (v: string | string[] | undefined) => typeof v === "string" ? v : undefined;
const date = (v: string | undefined) => {
  if (!v || !/^\d{4}-\d{2}-\d{2}$/.test(v)) return undefined;
  const d = new Date(`${v}T00:00:00Z`);
  return Number.isNaN(d.valueOf()) || d.toISOString().slice(0, 10) !== v ? undefined : v;
};
export function activityQuery(params: Params): URLSearchParams {
  const rawPage = one(params.page);
  const page = rawPage && /^\d{1,6}$/.test(rawPage) ? Number(rawPage) : 0;
  const query = new URLSearchParams({ page: String(page), size: "20" });
  const rawSearch = one(params.q);
  const search = rawSearch?.trim();
  if (rawSearch && search && rawSearch.length <= 160 && Array.from(search).length >= 2 && !/[\u0000-\u001f\u007f-\u009f]/.test(rawSearch)) query.set("q", search);
  const projectId = one(params.projectId);
  if (projectId && /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(projectId)) query.set("projectId", projectId);
  const status = one(params.status);
  if (status && activityStatuses.some((item) => item === status)) query.set("status", status);
  const from = date(one(params.dueFrom));
  const to = date(one(params.dueTo));
  if (!from || !to || from <= to) {
    if (from) query.set("dueFrom", from);
    if (to) query.set("dueTo", to);
  }
  if (one(params.overdue) === "true") query.set("overdue", "true");
  return query;
}

export function activityCandidateQuery(params: { page?: number; q?: string }): URLSearchParams {
  const page = params.page !== undefined && Number.isSafeInteger(params.page) && params.page >= 0 ? params.page : 0;
  const query = new URLSearchParams({ page: String(page), size: "20" });
  const rawSearch = params.q ?? "";
  const search = rawSearch.trim();
  if (search && rawSearch.length <= 160 && Array.from(search).length >= 2 && !/[\u0000-\u001f\u007f-\u009f]/.test(rawSearch)) query.set("query", search);
  return query;
}
