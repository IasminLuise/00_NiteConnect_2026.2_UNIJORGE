import "server-only";

import { getApiJson } from "../api/server";
import type { ApiResult } from "../api/contracts";
import {
  parseAdjustment, parseAttachmentCollection, parseAttendanceView, parseCalendarException,
  parseCorrection, parseExpectedEvent, parseJustification, parsePageResult, parseReplacement,
  parseTimelineItem, parseWeeklySummary, parseWorkLogReviewRequest,
  type Adjustment, type AttachmentCollection, type AttendanceView, type Correction, type ExpectedEvent,
  type Justification, type Page, type PersonalCalendarException, type Replacement, type TimelineItem,
  type ParticipationWeeklySummary, type WorkLogReviewRequest,
} from "./contracts";

export type { ApiResult };

function listPath(path: string, page: number, size: number, from?: string, to?: string): string {
  const query = new URLSearchParams({ page: String(page), size: String(size) });
  if (from && to) { query.set("from", from); query.set("to", to); }
  return `${path}?${query}`;
}

export function getWeeklySummary(bearer: string, from: string, to: string): Promise<ApiResult<ParticipationWeeklySummary>> {
  const query = new URLSearchParams({ from, to });
  return getApiJson(`/api/v1/me/participation-weekly-summary?${query}`, bearer, parseWeeklySummary);
}

export function getExpectedEvents(bearer: string, from: string, to: string, page = 0): Promise<ApiResult<Page<ExpectedEvent>>> {
  return getApiJson(listPath("/api/v1/me/participation/expected-events", page, 20, from, to), bearer,
    (value) => parsePageResult(value, parseExpectedEvent));
}

export function getTimeline(bearer: string, from: string, to: string, page = 0): Promise<ApiResult<Page<TimelineItem>>> {
  return getApiJson(listPath("/api/v1/me/participation/timeline", page, 20, from, to), bearer,
    (value) => parsePageResult(value, parseTimelineItem));
}

export function getMyAttendance(bearer: string): Promise<ApiResult<Page<AttendanceView>>> {
  return getApiJson(listPath("/api/v1/me/attendance", 0, 100), bearer,
    (value) => parsePageResult(value, parseAttendanceView));
}

export function getMyJustifications(bearer: string, page = 0): Promise<ApiResult<Page<Justification>>> {
  return getApiJson(listPath("/api/v1/me/absence-justifications", page, 10), bearer,
    (value) => parsePageResult(value, parseJustification));
}

export function getMyCorrections(bearer: string, page = 0): Promise<ApiResult<Page<Correction>>> {
  return getApiJson(listPath("/api/v1/me/attendance/corrections", page, 10), bearer,
    (value) => parsePageResult(value, parseCorrection));
}

export function getMyWorkLogReviewRequests(bearer: string, page = 0): Promise<ApiResult<Page<WorkLogReviewRequest>>> {
  return getApiJson(listPath("/api/v1/me/work-logs/review-requests", page, 10), bearer,
    (value) => parsePageResult(value, parseWorkLogReviewRequest));
}

export function getMyReplacements(bearer: string, page = 0): Promise<ApiResult<Page<Replacement>>> {
  return getApiJson(listPath("/api/v1/me/participation-replacements", page, 10), bearer,
    (value) => parsePageResult(value, parseReplacement));
}

export function getMyAdjustments(bearer: string, page = 0): Promise<ApiResult<Page<Adjustment>>> {
  return getApiJson(listPath("/api/v1/me/participation-adjustments", page, 10), bearer,
    (value) => parsePageResult(value, parseAdjustment));
}

export function getCalendarExceptions(bearer: string, from: string, to: string): Promise<ApiResult<Page<PersonalCalendarException>>> {
  return getApiJson(listPath("/api/v1/me/participation/calendar-exceptions", 0, 50, from, to), bearer,
    (value) => parsePageResult(value, parseCalendarException));
}

export function getJustificationAttachments(bearer: string, id: string): Promise<ApiResult<AttachmentCollection>> {
  return getApiJson(`/api/v1/me/absence-justifications/${encodeURIComponent(id)}/attachments`, bearer, parseAttachmentCollection);
}
