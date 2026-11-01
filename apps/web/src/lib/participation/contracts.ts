import {
  parseParticipationWeeklySummary,
  type ParticipationWeeklySummary,
} from "../dashboard/contracts";

export type { ParticipationWeeklySummary };

export type Page<T> = { items: T[]; page: number; size: number; total: number };
export type ParticipationModality = "PRESENCIAL" | "REMOTA";
export type PeriodStatus = "ABERTO" | "CANCELADO" | "VALIDADO" | "RECUSADO" | "DECLARADO" | "PENDENTE";

export type ExpectedEvent = {
  id: string; title: string; description: string; kind: string; modality: ParticipationModality;
  startsAt: string; endsAt: string; status: "AGENDADO" | "ABERTO" | "ENCERRADO" | "CANCELADO";
  expected: boolean; checkedIn: boolean; attendanceId: string | null;
};
export type TimelineItem = {
  id: string; eventId: string | null; eventTitle: string | null; eventKind: string | null;
  eventStatus: string | null; kind: "ATTENDANCE" | "WORK_LOG"; modality: ParticipationModality;
  startsAt: string; endsAt: string | null; durationSeconds: number | null; description: string | null;
  activityId: string | null; evidenceId: string | null; origin: string; reviewStatus: string;
  periodStatus: PeriodStatus; replacementAdjustmentId: string | null; reviewedAt: string | null;
  reviewReason: string | null; version: number;
};
export type AttendancePeriod = {
  id: string; kind: "ATTENDANCE" | "WORK_LOG"; modality: ParticipationModality;
  startsAt: string; endsAt: string | null; reviewStatus: string; version: number;
};
export type AttendanceView = { id: string; eventId: string; version: number; periods: AttendancePeriod[] };
export type Justification = {
  id: string; eventId: string; reason: string; status: string; decisionReason: string | null;
  decidedAt: string | null; version: number;
};
export type Correction = {
  id: string; periodId: string | null; eventId: string | null; proposedStart: string;
  proposedEnd: string; reason: string; status: string; decisionReason: string | null;
  decidedAt: string | null; version: number;
};
export type WorkLogReviewRequest = {
  id: string; periodId: string; weekStart: string; weekEnd: string; reason: string;
  createdAt: string; status: string;
  currentWorkLog: { id: string; startsAt: string; endsAt: string; description: string;
    modality: string; activityId: string | null; evidenceId: string | null; reviewStatus: string; version: number };
  response: { proposedStart: string; proposedEnd: string; proposedDescription: string; reason: string;
    respondedAt: string; decision: string | null; decisionReason: string | null; decidedAt: string | null } | null;
};
export type Adjustment = {
  id: string; sourceWeekStart: string; sourceWeekEnd: string; kind: string; requestedModality: ParticipationModality;
  requestedHours: number; authorizedModality: ParticipationModality | null; authorizedHours: number | null;
  dueOn: string | null; status: string; requestedAt: string; decidedAt: string | null;
  decisionReason: string | null; reason: string; version: number;
};
export type Replacement = { adjustment: Adjustment; periods: Array<{ periodId: string; kind: string;
  modality: ParticipationModality; reviewStatus: string; startsAt: string; endsAt: string | null;
  eventTitle: string | null; description: string | null; recognizedSeconds: number; assignedAt: string }>;
  recognizedSeconds: number; remainingSeconds: number | null };
export type PersonalCalendarException = { id: string; weekStart: string; weekEnd: string; version: number;
  type: string; presentialHoursRelieved: number; remoteHoursRelieved: number; affectedEventTitle: string | null;
  replacementEventTitle: string | null; validFrom: string; validUntil: string | null };
export type Attachment = { id: string; context: string; filename: string; contentType: string; sizeBytes: number | null;
  status: string; createdAt: string; retentionExpiresOn: string | null; deletionDeadlineOn: string | null;
  retentionHeld: boolean; downloadable: boolean };
export type AttachmentCollection = { uploadAvailability: string; items: Attachment[] };

function record(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null && !Array.isArray(value);
}
function string(value: unknown): value is string { return typeof value === "string"; }
function nullableString(value: unknown): value is string | null { return value === null || string(value); }
function integer(value: unknown): value is number { return typeof value === "number" && Number.isSafeInteger(value); }
function nonnegative(value: unknown): value is number { return integer(value) && value >= 0; }
function modality(value: unknown): value is ParticipationModality { return value === "PRESENCIAL" || value === "REMOTA"; }
function parsePage<T>(value: unknown, parse: (item: unknown) => T | null): Page<T> | null {
  if (!record(value) || !Array.isArray(value.items) || !integer(value.page) || value.page < 0 ||
      !integer(value.size) || value.size <= 0 || !integer(value.total) || value.total < 0) return null;
  const items = value.items.map(parse);
  if (items.some((item) => item === null)) return null;
  return { items: items as T[], page: value.page, size: value.size, total: value.total };
}

export function parsePageResult<T>(value: unknown, parse: (item: unknown) => T | null): Page<T> | null {
  return parsePage(value, parse);
}

export function parseExpectedEvent(value: unknown): ExpectedEvent | null {
  if (!record(value) || !string(value.id) || !string(value.title) || !string(value.description) ||
      !string(value.kind) || !modality(value.modality) || !string(value.startsAt) || !string(value.endsAt) ||
      !["AGENDADO", "ABERTO", "ENCERRADO", "CANCELADO"].includes(String(value.status)) ||
      typeof value.expected !== "boolean" || typeof value.checkedIn !== "boolean" || !nullableString(value.attendanceId)) return null;
  return { id: value.id, title: value.title, description: value.description, kind: value.kind, modality: value.modality,
    startsAt: value.startsAt, endsAt: value.endsAt, status: value.status as ExpectedEvent["status"],
    expected: value.expected, checkedIn: value.checkedIn, attendanceId: value.attendanceId };
}

export function parseTimelineItem(value: unknown): TimelineItem | null {
  const statuses: PeriodStatus[] = ["ABERTO", "CANCELADO", "VALIDADO", "RECUSADO", "DECLARADO", "PENDENTE"];
  if (!record(value) || !string(value.id) || !nullableString(value.eventId) || !nullableString(value.eventTitle) ||
      !nullableString(value.eventKind) || !nullableString(value.eventStatus) ||
      (value.kind !== "ATTENDANCE" && value.kind !== "WORK_LOG") || !modality(value.modality) ||
      !string(value.startsAt) || !nullableString(value.endsAt) ||
      !(value.durationSeconds === null || nonnegative(value.durationSeconds)) || !nullableString(value.description) ||
      !nullableString(value.activityId) || !nullableString(value.evidenceId) || !string(value.origin) ||
      !string(value.reviewStatus) || !statuses.includes(value.periodStatus as PeriodStatus) ||
      !nullableString(value.replacementAdjustmentId) || !nullableString(value.reviewedAt) ||
      !nullableString(value.reviewReason) || !integer(value.version) || value.version < 0) return null;
  return value as TimelineItem;
}

export function parseAttendanceView(value: unknown): AttendanceView | null {
  if (!record(value) || !string(value.id) || !string(value.eventId) || !integer(value.version) || !Array.isArray(value.periods)) return null;
  const periods = value.periods.map((item): AttendancePeriod | null => {
    if (!record(item) || !string(item.id) || (item.kind !== "ATTENDANCE" && item.kind !== "WORK_LOG") ||
        !modality(item.modality) || !string(item.startsAt) || !nullableString(item.endsAt) ||
        !string(item.reviewStatus) || !integer(item.version)) return null;
    return { id: item.id, kind: item.kind, modality: item.modality, startsAt: item.startsAt,
      endsAt: item.endsAt, reviewStatus: item.reviewStatus, version: item.version };
  });
  if (periods.some((item) => item === null)) return null;
  return { id: value.id, eventId: value.eventId, version: value.version, periods: periods as AttendancePeriod[] };
}

export function parseJustification(value: unknown): Justification | null {
  if (!record(value) || !string(value.id) || !string(value.eventId) || !string(value.reason) || !string(value.status) ||
      !nullableString(value.decisionReason) || !nullableString(value.decidedAt) || !integer(value.version)) return null;
  return { id: value.id, eventId: value.eventId, reason: value.reason, status: value.status,
    decisionReason: value.decisionReason, decidedAt: value.decidedAt, version: value.version };
}

export function parseCorrection(value: unknown): Correction | null {
  if (!record(value) || !string(value.id) || !nullableString(value.periodId) || !nullableString(value.eventId) ||
      !string(value.proposedStart) || !string(value.proposedEnd) || !string(value.reason) || !string(value.status) ||
      !nullableString(value.decisionReason) || !nullableString(value.decidedAt) || !integer(value.version)) return null;
  return { id: value.id, periodId: value.periodId, eventId: value.eventId, proposedStart: value.proposedStart,
    proposedEnd: value.proposedEnd, reason: value.reason, status: value.status,
    decisionReason: value.decisionReason, decidedAt: value.decidedAt, version: value.version };
}

export function parseWorkLogReviewRequest(value: unknown): WorkLogReviewRequest | null {
  if (!record(value) || !string(value.id) || !string(value.periodId) || !string(value.weekStart) || !string(value.weekEnd) ||
      !string(value.reason) || !string(value.createdAt) || !string(value.status) || !record(value.currentWorkLog)) return null;
  const current = value.currentWorkLog;
  if (!string(current.id) || !string(current.startsAt) || !string(current.endsAt) || !string(current.description) ||
      !string(current.modality) || !nullableString(current.activityId) || !nullableString(current.evidenceId) ||
      !string(current.reviewStatus) || !integer(current.version)) return null;
  let response: WorkLogReviewRequest["response"] = null;
  if (value.response !== null) {
    if (!record(value.response) || !string(value.response.proposedStart) || !string(value.response.proposedEnd) ||
        !string(value.response.proposedDescription) || !string(value.response.reason) || !string(value.response.respondedAt) ||
        !nullableString(value.response.decision) || !nullableString(value.response.decisionReason) || !nullableString(value.response.decidedAt)) return null;
    response = { proposedStart: value.response.proposedStart, proposedEnd: value.response.proposedEnd,
      proposedDescription: value.response.proposedDescription, reason: value.response.reason,
      respondedAt: value.response.respondedAt, decision: value.response.decision,
      decisionReason: value.response.decisionReason, decidedAt: value.response.decidedAt };
  }
  return { id: value.id, periodId: value.periodId, weekStart: value.weekStart, weekEnd: value.weekEnd,
    reason: value.reason, createdAt: value.createdAt, status: value.status,
    currentWorkLog: { id: current.id, startsAt: current.startsAt, endsAt: current.endsAt,
      description: current.description, modality: current.modality, activityId: current.activityId,
      evidenceId: current.evidenceId, reviewStatus: current.reviewStatus, version: current.version }, response };
}

function parseAdjustmentValue(value: unknown): Adjustment | null {
  if (!record(value) || !string(value.id) || !string(value.sourceWeekStart) || !string(value.sourceWeekEnd) ||
      !string(value.kind) || !modality(value.requestedModality) || typeof value.requestedHours !== "number" ||
      !nullableString(value.authorizedModality) || !(value.authorizedHours === null || typeof value.authorizedHours === "number") ||
      !nullableString(value.dueOn) || !string(value.status) || !string(value.requestedAt) || !nullableString(value.decidedAt) ||
      !nullableString(value.decisionReason) || !string(value.reason) || !integer(value.version)) return null;
  if (value.authorizedModality !== null && !modality(value.authorizedModality)) return null;
  return { id: value.id, sourceWeekStart: value.sourceWeekStart, sourceWeekEnd: value.sourceWeekEnd,
    kind: value.kind, requestedModality: value.requestedModality, requestedHours: value.requestedHours,
    authorizedModality: value.authorizedModality as ParticipationModality | null,
    authorizedHours: value.authorizedHours as number | null, dueOn: value.dueOn, status: value.status,
    requestedAt: value.requestedAt, decidedAt: value.decidedAt, decisionReason: value.decisionReason,
    reason: value.reason, version: value.version };
}

export function parseAdjustment(value: unknown): Adjustment | null {
  return parseAdjustmentValue(value);
}

export function parseReplacement(value: unknown): Replacement | null {
  if (!record(value) || !Array.isArray(value.periods) || !nonnegative(value.recognizedSeconds) ||
      !(value.remainingSeconds === null || nonnegative(value.remainingSeconds))) return null;
  const adjustment = parseAdjustment(value.adjustment);
  if (!adjustment) return null;
  const periods = value.periods.map((item) => {
    if (!record(item) || !string(item.periodId) || !string(item.kind) || !modality(item.modality) ||
        !string(item.reviewStatus) || !string(item.startsAt) || !nullableString(item.endsAt) ||
        !nullableString(item.eventTitle) || !nullableString(item.description) || !nonnegative(item.recognizedSeconds) ||
        !string(item.assignedAt)) return null;
    return { periodId: item.periodId, kind: item.kind, modality: item.modality,
      reviewStatus: item.reviewStatus, startsAt: item.startsAt, endsAt: item.endsAt,
      eventTitle: item.eventTitle, description: item.description, recognizedSeconds: item.recognizedSeconds,
      assignedAt: item.assignedAt };
  });
  if (periods.some((item) => item === null)) return null;
  return { adjustment, periods: periods as Replacement["periods"], recognizedSeconds: value.recognizedSeconds,
    remainingSeconds: value.remainingSeconds };
}

export function parseCalendarException(value: unknown): PersonalCalendarException | null {
  if (!record(value) || !string(value.id) || !string(value.weekStart) || !string(value.weekEnd) ||
      !integer(value.version) || !string(value.type) || typeof value.presentialHoursRelieved !== "number" ||
      typeof value.remoteHoursRelieved !== "number" || !nullableString(value.affectedEventTitle) ||
      !nullableString(value.replacementEventTitle) || !string(value.validFrom) || !nullableString(value.validUntil)) return null;
  return value as PersonalCalendarException;
}

export function parseAttachmentCollection(value: unknown): AttachmentCollection | null {
  if (!record(value) || !string(value.uploadAvailability) || !Array.isArray(value.items)) return null;
  const items = value.items.map((item): Attachment | null => {
    if (!record(item) || !string(item.id) || !string(item.context) || !string(item.filename) ||
        !string(item.contentType) || !(item.sizeBytes === null || nonnegative(item.sizeBytes)) ||
        !string(item.status) || !string(item.createdAt) || !nullableString(item.retentionExpiresOn) ||
        !nullableString(item.deletionDeadlineOn) || typeof item.retentionHeld !== "boolean" || typeof item.downloadable !== "boolean") return null;
    return { id: item.id, context: item.context, filename: item.filename, contentType: item.contentType,
      sizeBytes: item.sizeBytes, status: item.status, createdAt: item.createdAt,
      retentionExpiresOn: item.retentionExpiresOn, deletionDeadlineOn: item.deletionDeadlineOn,
      retentionHeld: item.retentionHeld, downloadable: item.downloadable };
  });
  if (items.some((item) => item === null)) return null;
  return { uploadAvailability: value.uploadAvailability, items: items as Attachment[] };
}

export function parseWeeklySummary(value: unknown): ParticipationWeeklySummary | null {
  return parseParticipationWeeklySummary(value);
}
