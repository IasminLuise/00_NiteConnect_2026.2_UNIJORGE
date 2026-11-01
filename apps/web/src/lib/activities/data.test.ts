import { describe, expect, it, vi } from "vitest";

const { getApiJson } = vi.hoisted(() => ({ getApiJson: vi.fn() }));
vi.mock("server-only", () => ({}));
vi.mock("../api/server", () => ({ getApiJson }));

import { getActivityCandidates } from "./data";

describe("consulta de candidatos de atividade", () => {
  it("envia busca e página nos parâmetros aceitos pelo backend", async () => {
    getApiJson.mockResolvedValue({ data: null, issue: "unavailable" });

    await getActivityCandidates("bearer-server", "activity-id", { page: 2, q: "  Ana Lima  " });

    expect(getApiJson).toHaveBeenCalledWith(
      "/api/v1/activities/activity-id/assignee-candidates?page=2&size=20&query=Ana+Lima",
      "bearer-server",
      expect.any(Function),
    );
  });
});
