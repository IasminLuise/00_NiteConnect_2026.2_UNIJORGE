"use server";

import { redirect } from "next/navigation";
import { signIn, signOut } from "../../../auth";
import { isAuthConfigured } from "./config";

export async function signInWithMicrosoft(): Promise<void> {
  if (!isAuthConfigured()) redirect("/login?error=Configuration");
  await signIn("microsoft-entra-id", { redirectTo: "/login" });
}

export async function signOutOfNite(): Promise<void> {
  await signOut({ redirectTo: "/login" });
}
