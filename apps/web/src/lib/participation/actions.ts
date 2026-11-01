"use server";

import { revalidatePath } from "next/cache";
import { redirect } from "next/navigation";
import { getAuthenticatedNiteSession } from "../auth/server-session";
import { localDateTimeToInstant, addDays } from "./time";
import { getJustificationAttachments } from "./data";
import { postParticipation, uploadJustificationAttachment, type ParticipationMutationIssue } from "./mutations";

const uuid = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;
type Feedback = "created" | "updated" | "forbidden" | "not-found" | "conflict" | "invalid" | "too-large" | "unsupported" | "unavailable";

function value(form: FormData, key: string): string {
  const raw = form.get(key);
  return typeof raw === "string" ? raw.trim() : "";
}
function integer(form: FormData, key: string): number | null {
  const raw = value(form, key);
  if (!/^\d{1,12}$/.test(raw)) return null;
  const parsed = Number(raw);
  return Number.isSafeInteger(parsed) ? parsed : null;
}
function safeText(input: string, max: number): boolean { return input.length > 0 && input.length <= max; }
function feedbackFor(issue: ParticipationMutationIssue | null): Feedback {
  if (issue === "access") return "forbidden";
  if (issue === "not-found") return "not-found";
  if (issue === "conflict") return "conflict";
  if (issue === "too-large") return "too-large";
  if (issue === "unsupported") return "unsupported";
  if (issue === "invalid") return "invalid";
  return "unavailable";
}
function finish(feedback: Feedback): never {
  revalidatePath("/participacao");
  redirect(`/participacao?feedback=${feedback}`);
}
function parseInstant(raw: string, zone: string): string | null {
  return localDateTimeToInstant(raw, zone);
}

export async function participationAction(form: FormData): Promise<void> {
  const session = await getAuthenticatedNiteSession();
  if (!session) redirect("/login");
  const intent = value(form, "intent");
  let path = "";
  let payload: Record<string, unknown> = {};

  if (intent === "check-in") {
    const eventId = value(form, "eventId"); const qrPayload = value(form, "payload");
    if (!uuid.test(eventId) || !safeText(qrPayload, 64)) finish("invalid");
    path = "/api/v1/me/attendance/check-in"; payload = { eventId, payload: qrPayload };
  } else if (intent === "check-out") {
    const expectedVersion = integer(form, "expectedVersion");
    if (expectedVersion === null) finish("invalid");
    path = "/api/v1/me/attendance/check-out"; payload = { expectedVersion };
  } else if (intent === "return") {
    const attendanceId = value(form, "attendanceId"); const expectedVersion = integer(form, "expectedVersion");
    const qrPayload = value(form, "payload");
    if (!uuid.test(attendanceId) || expectedVersion === null || !safeText(qrPayload, 64)) finish("invalid");
    path = `/api/v1/me/attendance/${encodeURIComponent(attendanceId)}/return`; payload = { expectedVersion, payload: qrPayload };
  } else if (intent === "work-log") {
    const startsAt = parseInstant(value(form, "startsAt"), value(form, "zone"));
    const endsAt = parseInstant(value(form, "endsAt"), value(form, "zone"));
    const description = value(form, "description");
    if (!startsAt || !endsAt || new Date(startsAt) >= new Date(endsAt) || !safeText(description, 10_000)) finish("invalid");
    path = "/api/v1/me/work-logs";
    payload = { startsAt, endsAt, description,
      ...(uuid.test(value(form, "activityId")) ? { activityId: value(form, "activityId") } : {}),
      ...(uuid.test(value(form, "evidenceId")) ? { evidenceId: value(form, "evidenceId") } : {}) };
  } else if (intent === "justification") {
    const eventId = value(form, "eventId"); const reason = value(form, "reason");
    if (!uuid.test(eventId) || !safeText(reason, 1_000)) finish("invalid");
    path = "/api/v1/me/absence-justifications"; payload = { eventId, reason };
  } else if (intent === "complement") {
    const id = value(form, "id"); const expectedVersion = integer(form, "expectedVersion"); const reason = value(form, "reason");
    if (!uuid.test(id) || expectedVersion === null || !safeText(reason, 1_000)) finish("invalid");
    path = `/api/v1/me/absence-justifications/${encodeURIComponent(id)}/complements`; payload = { expectedVersion, reason };
  } else if (intent === "correction") {
    const target = value(form, "target");
    const [targetKind, targetId] = target.split(":");
    const periodId = targetKind === "period" ? targetId : "";
    const eventId = targetKind === "event" ? targetId : "";
    const startsAt = parseInstant(value(form, "startsAt"), value(form, "zone"));
    const endsAt = parseInstant(value(form, "endsAt"), value(form, "zone")); const reason = value(form, "reason");
    const hasPeriod = uuid.test(periodId); const hasEvent = uuid.test(eventId);
    if (hasPeriod === hasEvent || !startsAt || !endsAt || new Date(startsAt) >= new Date(endsAt) || !safeText(reason, 1_000)) finish("invalid");
    path = "/api/v1/me/attendance/corrections";
    payload = { ...(hasPeriod ? { periodId } : { eventId }), startsAt, endsAt, reason };
  } else if (intent === "replacement") {
    const weekStart = value(form, "weekStart"); const suppliedWeekEnd = value(form, "weekEnd");
    const weekEnd = suppliedWeekEnd || (/^\d{4}-\d{2}-\d{2}$/.test(weekStart) ? addDays(weekStart, 6) : "");
    const modality = value(form, "modality"); const hours = Number(value(form, "hours")); const reason = value(form, "reason");
    if (!/^\d{4}-\d{2}-\d{2}$/.test(weekStart) || addDays(weekStart, 6) !== weekEnd ||
        !["PRESENCIAL", "REMOTA"].includes(modality) || !Number.isFinite(hours) || hours <= 0 || !safeText(reason, 1_000)) finish("invalid");
    path = "/api/v1/me/participation-replacements"; payload = { weekStart, weekEnd, modality, hours, reason };
  } else if (intent === "work-log-response") {
    const id = value(form, "id"); const expectedPeriodVersion = integer(form, "expectedPeriodVersion");
    const startsAt = parseInstant(value(form, "startsAt"), value(form, "zone"));
    const endsAt = parseInstant(value(form, "endsAt"), value(form, "zone"));
    const description = value(form, "description"); const reason = value(form, "reason");
    if (!uuid.test(id) || expectedPeriodVersion === null || !startsAt || !endsAt || new Date(startsAt) >= new Date(endsAt) ||
        !safeText(description, 10_000) || !safeText(reason, 1_000)) finish("invalid");
    path = `/api/v1/me/work-logs/review-requests/${encodeURIComponent(id)}/responses`;
    payload = { expectedPeriodVersion, startsAt, endsAt, description, reason };
  } else {
    finish("invalid");
  }

  const result = await postParticipation(path, session.bearer, payload);
  if (result.issue) finish(feedbackFor(result.issue));
  finish("created");
}

export async function uploadJustificationAction(form: FormData): Promise<void> {
  const session = await getAuthenticatedNiteSession();
  if (!session) redirect("/login");
  const id = value(form, "id");
  const context = value(form, "context");
  const fileValue = form.get("file");
  if (!uuid.test(id) || !(fileValue instanceof File) || fileValue.size <= 0 || fileValue.size > 5_000_000) finish("invalid");
  if (!["JUSTIFICATIVA", "COMPLEMENTACAO"].includes(context)) finish("invalid");
  const allowed = new Set(["application/pdf", "image/jpeg", "image/png"]);
  const extension = fileValue.name.split(".").pop()?.toLowerCase();
  const validExtension = ["pdf", "jpg", "jpeg", "png"].includes(extension ?? "");
  if (!validExtension || !allowed.has(fileValue.type)) finish("unsupported");
  const availability = await getJustificationAttachments(session.bearer, id);
  if (availability.issue) finish(feedbackFor(availability.issue));
  if (availability.data.uploadAvailability !== "DISPONIVEL") finish("forbidden");
  const result = await uploadJustificationAttachment(id, session.bearer, fileValue, context as "JUSTIFICATIVA" | "COMPLEMENTACAO");
  if (result.issue) finish(feedbackFor(result.issue));
  finish("updated");
}
