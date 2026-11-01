export const activityStatuses = ["PLANEJADA", "EM_EXECUCAO", "CONCLUIDA", "CANCELADA"] as const;
export type ActivityStatus = typeof activityStatuses[number];
export type Activity = {
  id: string; title: string; description: string; projectId: string | null; status: ActivityStatus;
  dueOn: string | null; primaryExecutorId: string; primaryExecutorName: string | null;
  executorAvailable: boolean; createdBy: string; version: number; createdAt: string; updatedAt: string;
  completedAt: string | null; deadlinePassed: boolean; overdue: boolean; attentionRequired: boolean;
};
export type Page<T> = { items: T[]; page: number; size: number; total: number };
export type ActivityCapabilities = { edit: boolean; transitions: ActivityStatus[]; history: boolean; manageAssignees: boolean; addEvidence: boolean; upload: boolean };
export type PersonalActivityCapabilities = { createIndependent: boolean };
export type ActivityAssignee = { id: string; memberId: string; memberName: string | null; role: "EXECUTOR" | "COLLABORATOR"; startedAt: string; available: boolean };
export type ActivityCandidate = { id: string; name: string };
export type ActivityHistoryEvent = { id: string; activityId: string; actorId: string; action: string; reason: string; occurredAt: string; activityVersion: number; before: string | null; after: string | null };
export type ActivityEvidence = {
  id: string; activityId: string; authorId: string; authorName: string | null; assignmentId: string | null;
  type: "TEXT" | "TECHNICAL" | "LINK" | "FILE" | "IMAGE"; content: string | null; url: string | null;
  correctsEvidenceId: string | null; createdAt: string; withdrawnAt: string | null;
  object: { filename: string; contentType: string; size: number; sha256: string } | null;
};
export type EvidenceCapabilities = { withdraw: boolean };
export type ProjectSummary = { id: string; title: string };
const record = (v: unknown): v is Record<string, unknown> => typeof v === "object" && v !== null && !Array.isArray(v);
const nullableString = (v: unknown): v is string | null => v === null || typeof v === "string";
const count = (v: unknown): v is number => typeof v === "number" && Number.isSafeInteger(v) && v >= 0;
export function parseActivityCapabilities(value: unknown): ActivityCapabilities | null {
  if (!record(value)) return null;
  const { edit, transitions, history, manageAssignees, addEvidence, upload } = value;
  if (typeof edit !== "boolean" || typeof history !== "boolean" || typeof manageAssignees !== "boolean" || typeof addEvidence !== "boolean" || typeof upload !== "boolean" || !Array.isArray(transitions)) return null;
  const valid = (v: unknown): v is ActivityStatus => v === "PLANEJADA" || v === "EM_EXECUCAO" || v === "CONCLUIDA" || v === "CANCELADA";
  if (!transitions.every(valid)) return null;
  return { edit, transitions, history, manageAssignees, addEvidence, upload };
}
export function parsePersonalActivityCapabilities(value: unknown): PersonalActivityCapabilities | null {
  return record(value) && typeof value.createIndependent === "boolean" ? { createIndependent: value.createIndependent } : null;
}
export function parseEvidenceCapabilities(value: unknown): EvidenceCapabilities | null {
  return record(value) && typeof value.withdraw === "boolean" ? { withdraw: value.withdraw } : null;
}
export function parseActivity(value: unknown): Activity | null {
  if (!record(value)) return null;
  const { id, title, description, projectId, status, dueOn, primaryExecutorId, primaryExecutorName, executorAvailable, createdBy, version, createdAt, updatedAt, completedAt, deadlinePassed, overdue, attentionRequired } = value;
  if (typeof id !== "string" || typeof title !== "string" || typeof description !== "string" || !nullableString(projectId) || !activityStatuses.some((s) => s === status) || !nullableString(dueOn) || typeof primaryExecutorId !== "string" || !nullableString(primaryExecutorName) || typeof executorAvailable !== "boolean" || typeof createdBy !== "string" || !count(version) || typeof createdAt !== "string" || typeof updatedAt !== "string" || !nullableString(completedAt) || typeof deadlinePassed !== "boolean" || typeof overdue !== "boolean" || typeof attentionRequired !== "boolean") return null;
  if (status !== "PLANEJADA" && status !== "EM_EXECUCAO" && status !== "CONCLUIDA" && status !== "CANCELADA") return null;
  return { id, title, description, projectId, status, dueOn, primaryExecutorId, primaryExecutorName, executorAvailable, createdBy, version, createdAt, updatedAt, completedAt, deadlinePassed, overdue, attentionRequired };
}
export function parseActivityPage(value: unknown): Page<Activity> | null {
  return parsePage(value, parseActivity);
}

export function parseActivityAssignee(value: unknown): ActivityAssignee | null {
  if (!record(value) || typeof value.id !== "string" || typeof value.memberId !== "string" || !nullableString(value.memberName) || (value.role !== "EXECUTOR" && value.role !== "COLLABORATOR") || typeof value.startedAt !== "string" || typeof value.available !== "boolean") return null;
  return { id: value.id, memberId: value.memberId, memberName: value.memberName, role: value.role, startedAt: value.startedAt, available: value.available };
}
export function parseActivityCandidate(value: unknown): ActivityCandidate | null {
  return record(value) && typeof value.id === "string" && typeof value.name === "string" ? { id: value.id, name: value.name } : null;
}
export function parseActivityHistoryEvent(value: unknown): ActivityHistoryEvent | null {
  if (!record(value) || typeof value.id !== "string" || typeof value.activityId !== "string" || typeof value.actorId !== "string" || typeof value.action !== "string" || typeof value.reason !== "string" || typeof value.occurredAt !== "string" || !count(value.activityVersion) || !nullableString(value.before) || !nullableString(value.after)) return null;
  return { id: value.id, activityId: value.activityId, actorId: value.actorId, action: value.action, reason: value.reason, occurredAt: value.occurredAt, activityVersion: value.activityVersion, before: value.before, after: value.after };
}
export function parseActivityEvidence(value: unknown): ActivityEvidence | null {
  if (!record(value) || typeof value.id !== "string" || typeof value.activityId !== "string" || typeof value.authorId !== "string" || !nullableString(value.authorName) || !nullableString(value.assignmentId) || (value.type !== "TEXT" && value.type !== "TECHNICAL" && value.type !== "LINK" && value.type !== "FILE" && value.type !== "IMAGE") || !nullableString(value.content) || !nullableString(value.url) || !nullableString(value.correctsEvidenceId) || typeof value.createdAt !== "string" || !nullableString(value.withdrawnAt)) return null;
  let object: ActivityEvidence["object"] = null;
  if (value.object !== null) {
    if (!record(value.object) || typeof value.object.filename !== "string" || typeof value.object.contentType !== "string" || !count(value.object.size) || typeof value.object.sha256 !== "string") return null;
    object = { filename: value.object.filename, contentType: value.object.contentType, size: value.object.size, sha256: value.object.sha256 };
  }
  return { id: value.id, activityId: value.activityId, authorId: value.authorId, authorName: value.authorName, assignmentId: value.assignmentId, type: value.type, content: value.content, url: value.url, correctsEvidenceId: value.correctsEvidenceId, createdAt: value.createdAt, withdrawnAt: value.withdrawnAt, object };
}
export function parseProjectPage(value: unknown): Page<ProjectSummary> | null {
  return parsePage(value, (item) => record(item) && typeof item.id === "string" && typeof item.title === "string" ? { id: item.id, title: item.title } : null);
}

export function parsePage<T>(value: unknown, parseItem: (item: unknown) => T | null): Page<T> | null {
  if (!record(value) || !Array.isArray(value.items) || !count(value.page) || !count(value.size) || value.size < 1 || value.size > 100 || !count(value.total)) return null;
  const items = value.items.map(parseItem);
  if (items.some((item) => item === null)) return null;
  return { items: items.filter((item): item is T => item !== null), page: value.page, size: value.size, total: value.total };
}
