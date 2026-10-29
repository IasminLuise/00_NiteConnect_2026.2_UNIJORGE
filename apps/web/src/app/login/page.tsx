import type { Metadata } from "next";
import { auth } from "../../../auth";
import { InstitutionalLogin } from "../../components/auth/institutional-login";
import { isAuthConfigured } from "../../lib/auth/config";

export const metadata: Metadata = {
  title: "Entrar",
  description: "Acesso institucional ao NITE Connect.",
};

export const dynamic = "force-dynamic";

type LoginPageProps = {
  searchParams: Promise<{ error?: string }>;
};

export default async function LoginPage({ searchParams }: LoginPageProps) {
  const [{ error }, session] = await Promise.all([
    searchParams,
    isAuthConfigured() ? auth() : Promise.resolve(null),
  ]);

  return (
    <InstitutionalLogin
      member={session?.error ? null : session?.niteMember}
      roles={session?.error ? [] : session?.niteRoles}
      error={session?.error ?? error ?? null}
    />
  );
}
