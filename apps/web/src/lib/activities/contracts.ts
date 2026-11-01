export const activityStatuses = ["PLANEJADA", "EM_EXECUCAO", "CONCLUIDA", "CANCELADA"] as const;
export type ActivityStatus = typeof activityStatuses[number];
export type Activity = {
  id: string; title: string; description: string; projectId: string | null; status: ActivityStatus;
  dueOn: string | null; primaryExecutorId: string; primaryExecutorName: string | null;
  executorAvailable: boolean; createdBy: string; version: number; createdAt: string; updatedAt: string;
  completedAt: string | null; deadlinePassed: boolean; overdue: boolean; attentionRequired: boolean;
};
export type Page<T> = { items: T[]; page: number; size: number; total: number };
const record = (v: unknown): v is Record<string, unknown> => typeof v === "object" && v !== null && !Array.isArray(v);
const nullableString = (v: unknown): v is string | null => v === null || typeof v === "string";
const count = (v: unknown): v is number => typeof v === "number" && Number.isSafeInteger(v) && v >= 0;
export function parseActivity(value: unknown): Activity | null {
  if (!record(value)) return null;
  const { id, title, description, projectId, status, dueOn, primaryExecutorId, primaryExecutorName, executorAvailable, createdBy, version, createdAt, updatedAt, completedAt, deadlinePassed, overdue, attentionRequired } = value;
  if (typeof id !== "string" || typeof title !== "string" || typeof description !== "string" || !nullableString(projectId) || !activityStatuses.some((s) => s === status) || !nullableString(dueOn) || typeof primaryExecutorId !== "string" || !nullableString(primaryExecutorName) || typeof executorAvailable !== "boolean" || typeof createdBy !== "string" || !count(version) || typeof createdAt !== "string" || typeof updatedAt !== "string" || !nullableString(completedAt) || typeof deadlinePassed !== "boolean" || typeof overdue !== "boolean" || typeof attentionRequired !== "boolean") return null;
  if (status !== "PLANEJADA" && status !== "EM_EXECUCAO" && status !== "CONCLUIDA" && status !== "CANCELADA") return null;
  return { id, title, description, projectId, status, dueOn, primaryExecutorId, primaryExecutorName, executorAvailable, createdBy, version, createdAt, updatedAt, completedAt, deadlinePassed, overdue, attentionRequired };
}
export function parseActivityPage(value: unknown): Page<Activity> | null {
  if (!record(value) || !Array.isArray(value.items) || !count(value.page) || !count(value.size) || value.size < 1 || value.size > 100 || !count(value.total)) return null;
  const items = value.items.map(parseActivity);
  if (items.some((item) => item === null)) return null;
  return { items: items.filter((item): item is Activity => item !== null), page: value.page, size: value.size, total: value.total };
}
