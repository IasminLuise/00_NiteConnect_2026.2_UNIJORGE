import "server-only";

import { cache } from "react";
import { getApiJson } from "../api/server";
import { parseLeadershipProjects, type LeadershipProject } from "./contracts";

export const getLeadershipProjects = cache(async (bearer: string) => getApiJson(
  "/api/v1/me/projects/leadership/history?page=0&size=100",
  bearer,
  parseLeadershipProjects,
));

export type { LeadershipProject };
