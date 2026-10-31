export type ApiIssue = "access" | "unavailable" | "invalid";
export type ApiResult<T> = { data: T; issue: null } | { data: null; issue: ApiIssue };
