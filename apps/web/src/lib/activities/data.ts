import "server-only";
import { getApiJson } from "../api/server";
import { parseActivityPage } from "./contracts";
export function getMyActivities(bearer: string, query: URLSearchParams) {
  return getApiJson(`/api/v1/me/activities?${query}`, bearer, parseActivityPage);
}
