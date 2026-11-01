import "server-only";
import { getApiJson } from "../api/server";
import { activityCandidateQuery } from "./query";
import {
  parseActivity,
  parseActivityAssignee,
  parseActivityCandidate,
  parseActivityCapabilities,
  parseActivityEvidence,
  parseActivityHistoryEvent,
  parseActivityPage,
  parsePage,
  parseEvidenceCapabilities,
  parsePersonalActivityCapabilities,
  parseProjectPage,
} from "./contracts";
export function getMyActivities(bearer: string, query: URLSearchParams) {
  return getApiJson(`/api/v1/me/activities?${query}`, bearer, parseActivityPage);
}
export function getActivity(bearer: string, id: string) {
  return getApiJson(`/api/v1/activities/${encodeURIComponent(id)}`, bearer, parseActivity);
}
export function getActivityCapabilities(bearer: string, id: string) {
  return getApiJson(`/api/v1/activities/${encodeURIComponent(id)}/capabilities`, bearer, parseActivityCapabilities);
}
export function getMyActivityCapabilities(bearer: string) {
  return getApiJson("/api/v1/me/activities/capabilities", bearer, parsePersonalActivityCapabilities);
}
export function getMyProjects(bearer: string, page = 0) {
  const query = new URLSearchParams({ page: String(page), size: "100" });
  return getApiJson(`/api/v1/me/projects?${query}`, bearer, parseProjectPage);
}
export async function getAllMyProjects(bearer: string) {
  const first = await getMyProjects(bearer, 0);
  if (first.issue) return first;
  const pageCount = Math.ceil(first.data.total / first.data.size);
  if (pageCount <= 1) return { data: first.data.items, issue: null } as const;
  const rest = await Promise.all(Array.from({ length: pageCount - 1 }, (_, index) => getMyProjects(bearer, index + 1)));
  const failed = rest.find((result) => result.issue !== null);
  if (failed?.issue) return { data: null, issue: failed.issue } as const;
  return { data: [...first.data.items, ...rest.flatMap((result) => result.data?.items ?? [])], issue: null } as const;
}
export function getActivityHistory(bearer: string, id: string, page = 0) {
  const query = new URLSearchParams({ page: String(page), size: "20" });
  return getApiJson(`/api/v1/activities/${encodeURIComponent(id)}/history?${query}`, bearer, (value) => parsePage(value, parseActivityHistoryEvent));
}
export function getActivityAssignees(bearer: string, id: string) {
  const query = new URLSearchParams({ page: "0", size: "100" });
  return getApiJson(`/api/v1/activities/${encodeURIComponent(id)}/assignees?${query}`, bearer, (value) => parsePage(value, parseActivityAssignee));
}
export function getActivityCandidates(bearer: string, id: string, params: { page?: number; q?: string } = {}) {
  const query = activityCandidateQuery(params);
  return getApiJson(`/api/v1/activities/${encodeURIComponent(id)}/assignee-candidates?${query}`, bearer, (value) => parsePage(value, parseActivityCandidate));
}
export function getActivityEvidence(bearer: string, id: string, page = 0) {
  const query = new URLSearchParams({ page: String(page), size: "20" });
  return getApiJson(`/api/v1/activities/${encodeURIComponent(id)}/evidence?${query}`, bearer, (value) => parsePage(value, parseActivityEvidence));
}
export function getActivityEvidenceById(bearer: string, id: string, evidenceId: string) {
  return getApiJson(`/api/v1/activities/${encodeURIComponent(id)}/evidence/${encodeURIComponent(evidenceId)}`, bearer, parseActivityEvidence);
}
export function getEvidenceCapabilities(bearer: string, id: string, evidenceId: string) {
  return getApiJson(`/api/v1/activities/${encodeURIComponent(id)}/evidence/${encodeURIComponent(evidenceId)}/capabilities`, bearer, parseEvidenceCapabilities);
}
