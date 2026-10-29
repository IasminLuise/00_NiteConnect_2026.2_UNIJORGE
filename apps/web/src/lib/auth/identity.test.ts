import { describe, expect, it } from "vitest";
import { parseNiteIdentity } from "./identity";

const activeIdentity = {
  member: {
    id: "db4bd5de-d9b3-4565-8340-066d32c08e11",
    name: "Pessoa Integrante",
    institutionalEmail: "integrante@unijorge.com.br",
    status: "ATIVO",
  },
  roles: ["INTEGRANTE"],
};

describe("parseNiteIdentity", () => {
  it("accepts the active-member projection returned by GET /api/v1/me", () => {
    expect(parseNiteIdentity(activeIdentity)).toEqual(activeIdentity);
  });

  it("rejects a Microsoft identity that the API has not activated", () => {
    expect(
      parseNiteIdentity({
        ...activeIdentity,
        member: { ...activeIdentity.member, status: "PENDENTE_INGRESSO" },
      }),
    ).toBeNull();
  });

  it("rejects malformed API responses rather than trusting claims from the browser", () => {
    expect(parseNiteIdentity({ member: activeIdentity.member, roles: "GESTOR_GERAL" })).toBeNull();
    expect(parseNiteIdentity(null)).toBeNull();
  });
});
