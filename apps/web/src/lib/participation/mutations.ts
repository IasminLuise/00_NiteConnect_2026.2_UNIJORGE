import "server-only";

import { apiIssueFromStatus, type ApiIssue, type ApiResult } from "../api/contracts";
import { resolveApiUrl } from "../api/server";
import type { AttachmentCollection } from "./contracts";

export type ParticipationMutationIssue = ApiIssue | "conflict" | "too-large" | "unsupported";

export async function postParticipation(path: string, bearer: string, payload: unknown): Promise<{ issue: ParticipationMutationIssue | null }> {
  const url = resolveApiUrl(path);
  if (!url) return { issue: "unavailable" };
  try {
    const response = await fetch(url, { method: "POST", headers: {
      Authorization: `Bearer ${bearer}`, Accept: "application/json", "Content-Type": "application/json",
    }, body: JSON.stringify(payload), cache: "no-store", redirect: "error", signal: AbortSignal.timeout(12_000) });
    if (response.ok) return { issue: null };
    if (response.status === 409) return { issue: "conflict" };
    if (response.status === 413) return { issue: "too-large" };
    if (response.status === 415) return { issue: "unsupported" };
    if (response.status === 400 || response.status === 422) return { issue: "invalid" };
    return { issue: apiIssueFromStatus(response.status) };
  } catch {
    return { issue: "unavailable" };
  }
}

export async function uploadJustificationAttachment(
  justificationId: string, bearer: string, file: File, context: "JUSTIFICATIVA" | "COMPLEMENTACAO",
): Promise<{ issue: ParticipationMutationIssue | null }> {
  const url = resolveApiUrl(`/api/v1/me/absence-justifications/${encodeURIComponent(justificationId)}/attachments`);
  if (!url) return { issue: "unavailable" };
  const body = new FormData();
  body.set("file", file, file.name);
  body.set("context", context);
  try {
    const response = await fetch(url, { method: "POST", headers: { Authorization: `Bearer ${bearer}`, Accept: "application/json" },
      body, cache: "no-store", redirect: "error", signal: AbortSignal.timeout(30_000) });
    if (response.ok) return { issue: null };
    if (response.status === 409) return { issue: "conflict" };
    if (response.status === 413) return { issue: "too-large" };
    if (response.status === 415) return { issue: "unsupported" };
    if (response.status === 400 || response.status === 422) return { issue: "invalid" };
    return { issue: apiIssueFromStatus(response.status) };
  } catch {
    return { issue: "unavailable" };
  }
}

export type DownloadedAttachment = { bytes: ArrayBuffer; contentType: string };
export async function downloadJustificationAttachment(
  justificationId: string, attachmentId: string, bearer: string,
): Promise<ApiResult<DownloadedAttachment>> {
  const url = resolveApiUrl(`/api/v1/me/absence-justifications/${encodeURIComponent(justificationId)}/attachments/${encodeURIComponent(attachmentId)}/download`);
  if (!url) return { data: null, issue: "unavailable" };
  try {
    const response = await fetch(url, { headers: { Authorization: `Bearer ${bearer}`, Accept: "application/octet-stream" },
      cache: "no-store", redirect: "error", signal: AbortSignal.timeout(20_000) });
    if (!response.ok) return { data: null, issue: apiIssueFromStatus(response.status) };
    const contentType = response.headers.get("content-type")?.split(";")[0]?.toLowerCase() ?? "";
    if (!["application/pdf", "image/jpeg", "image/png"].includes(contentType)) return { data: null, issue: "invalid" };
    return { data: { bytes: await response.arrayBuffer(), contentType }, issue: null };
  } catch {
    return { data: null, issue: "unavailable" };
  }
}

export async function getAttachmentMetadata(id: string, bearer: string): Promise<ApiResult<AttachmentCollection>> {
  const { getJustificationAttachments } = await import("./data");
  return getJustificationAttachments(bearer, id);
}
