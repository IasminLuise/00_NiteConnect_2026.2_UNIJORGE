import { describe, expect, it } from "vitest";
import { activityCandidateQuery, activityQuery } from "./query";

describe("personal activity filters", () => {
  it("passes valid search and project filters to the API", () => {
    expect(activityQuery({ q: "  revisão  ", projectId: "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa" }).toString()).toBe("page=0&size=20&q=revis%C3%A3o&projectId=aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa");
  });
  it("rejects malformed projects and control characters in search", () => {
    expect(activityQuery({ q: "ab\ncd", projectId: "invalid" }).toString()).toBe("page=0&size=20");
  });
  it("sends supported filters and pagination to the API", () => {
    expect(activityQuery({ status: "EM_EXECUCAO", dueTo: "2026-11-30", overdue: "true", page: "2" }).toString()).toBe("page=2&size=20&status=EM_EXECUCAO&dueTo=2026-11-30&overdue=true");
  });
  it("ignores invalid input instead of forwarding unsupported filters", () => {
    expect(activityQuery({ status: "ATRASADA", page: "-1", q: "x", dueFrom: "invalid" }).toString()).toBe("page=0&size=20");
  });
  it("paginas a busca de candidatos apenas para consultas válidas", () => {
    expect(activityCandidateQuery({ page: 2, q: "  Ana Lima  " }).toString()).toBe("page=2&size=20&query=Ana+Lima");
    expect(activityCandidateQuery({ page: -1, q: "x" }).toString()).toBe("page=0&size=20");
    expect(activityCandidateQuery({ page: 1, q: `ab\ncd` }).toString()).toBe("page=1&size=20");
  });
});
