import { apiIssueFromStatus, type ApiIssue } from "../api/contracts";

export type AttendanceMutationIssue = ApiIssue | "conflict";

export async function postAttendanceRequest(
  url: URL | string | null,
  bearer: string,
  payload: unknown,
): Promise<{ issue: AttendanceMutationIssue | null }> {
  if (!url) return { issue: "unavailable" };
  try {
    const response = await fetch(url, {
      method: "POST",
      headers: { Authorization: `Bearer ${bearer}`, Accept: "application/json", "Content-Type": "application/json" },
      body: JSON.stringify(payload),
      cache: "no-store",
      redirect: "error",
      signal: AbortSignal.timeout(12_000),
    });
    if (response.ok) return { issue: null };
    if (response.status === 409) return { issue: "conflict" };
    if (response.status === 400 || response.status === 422) return { issue: "invalid" };
    return { issue: apiIssueFromStatus(response.status) };
  } catch {
    return { issue: "unavailable" };
  }
}
