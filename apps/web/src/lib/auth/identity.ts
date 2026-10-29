export type NiteMember = {
  id: string;
  name: string;
  institutionalEmail: string;
  status: "ATIVO";
};

export type NiteIdentity = {
  member: NiteMember;
  roles: string[];
};

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null && !Array.isArray(value);
}

export function parseNiteIdentity(value: unknown): NiteIdentity | null {
  if (!isRecord(value) || !isRecord(value.member) || !Array.isArray(value.roles)) return null;

  const { id, name, institutionalEmail, status } = value.member;
  if (
    typeof id !== "string" ||
    typeof name !== "string" ||
    typeof institutionalEmail !== "string" ||
    status !== "ATIVO" ||
    value.roles.length === 0 ||
    !value.roles.every((role): role is string => typeof role === "string" && role.length > 0)
  ) {
    return null;
  }

  return {
    member: { id, name, institutionalEmail, status },
    roles: value.roles,
  };
}
