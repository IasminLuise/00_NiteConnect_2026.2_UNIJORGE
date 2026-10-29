import type { NiteMember } from "./identity";

type AuthSessionError = "AccessDenied" | "SessionExpired";

declare module "next-auth" {
  interface Session {
    niteMember?: NiteMember;
    niteRoles?: string[];
    error?: AuthSessionError;
  }

  interface User {
    niteMember?: NiteMember;
    niteRoles?: string[];
  }
}

declare module "next-auth/jwt" {
  interface JWT {
    accessToken?: string;
    refreshToken?: string;
    accessTokenExpiresAt?: number;
    niteMember?: NiteMember;
    niteRoles?: string[];
    authError?: AuthSessionError;
  }
}

export type { AuthSessionError };
