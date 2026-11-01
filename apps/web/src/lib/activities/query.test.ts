import { describe, expect, it } from "vitest";
import { activityQuery } from "./query";

describe("personal activity filters", () => {
  it("sends supported filters and pagination to the API", () => {
    expect(activityQuery({ status: "EM_EXECUCAO", dueTo: "2026-11-30", overdue: "true", page: "2" }).toString()).toBe("page=2&size=20&status=EM_EXECUCAO&dueTo=2026-11-30&overdue=true");
  });
  it("ignores invalid input instead of forwarding unsupported filters", () => {
    expect(activityQuery({ status: "ATRASADA", page: "-1", q: "atividade", dueFrom: "invalid" }).toString()).toBe("page=0&size=20");
  });
});
