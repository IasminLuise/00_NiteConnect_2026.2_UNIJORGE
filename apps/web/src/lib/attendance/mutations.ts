import "server-only";

import { resolveApiUrl } from "../api/server";
import { postAttendanceRequest } from "./mutations-core";
export type { AttendanceMutationIssue } from "./mutations-core";

export async function postAttendance(
  path: string,
  bearer: string,
  payload: unknown,
): ReturnType<typeof postAttendanceRequest> {
  return postAttendanceRequest(resolveApiUrl(path), bearer, payload);
}
