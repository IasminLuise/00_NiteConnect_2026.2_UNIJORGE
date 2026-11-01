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
