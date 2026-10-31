import "server-only";

import { cache } from "react";
import { redirect } from "next/navigation";
import { headers } from "next/headers";
import { getToken } from "next-auth/jwt";
import { auth, authSessionCookieName } from "../../../auth";
import type { NiteMember } from "./identity";

export type AuthenticatedNiteSession = {
  member: NiteMember;
  roles: string[];
  bearer: string;
};

export const getAuthenticatedNiteSession = cache(async (): Promise<AuthenticatedNiteSession | null> => {
  const [session, token] = await Promise.all([
    auth(),
    process.env.AUTH_SECRET
      ? getToken({
          req: { headers: await headers() },
          secret: process.env.AUTH_SECRET,
          cookieName: authSessionCookieName,
          secureCookie: process.env.NODE_ENV === "production",
        })
      : Promise.resolve(null),
  ]);
  if (!session?.niteMember || session.error) return null;
  const bearer = token?.accessToken;
  if (typeof bearer !== "string" || bearer.length === 0) return null;
  return { member: session.niteMember, roles: session.niteRoles ?? [], bearer };
});

export async function requireAuthenticatedNiteSession(): Promise<AuthenticatedNiteSession> {
  const session = await getAuthenticatedNiteSession();
  if (!session) redirect("/login");
  return session;
}
