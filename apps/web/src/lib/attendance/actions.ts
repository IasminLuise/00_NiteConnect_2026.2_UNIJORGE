"use server";

import { redirect } from "next/navigation";
import { getAuthenticatedNiteSession } from "../auth/server-session";
import { postAttendance, type AttendanceMutationIssue } from "./mutations";

export type AttendanceActionState = { status: "idle" | "success" | "error"; message?: string };

const uuid = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

function value(form: FormData, key: string): string {
  const raw = form.get(key);
  return typeof raw === "string" ? raw.trim() : "";
}

function version(form: FormData): number | null {
  const raw = value(form, "expectedVersion");
  if (!/^(0|[1-9]\d{0,11})$/.test(raw)) return null;
  const parsed = Number(raw);
  return Number.isSafeInteger(parsed) ? parsed : null;
}

function issueMessage(issue: AttendanceMutationIssue): string {
  if (issue === "access") return "Sua permissão para registrar presença não está disponível.";
  if (issue === "not-found") return "O encontro ou registro não está disponível para esta conta.";
  if (issue === "conflict") return "A sessão mudou ou o QR expirou. Consulte o encontro e tente novamente.";
  if (issue === "invalid") return "Os dados do registro não passaram pela validação.";
  return "Não foi possível concluir o registro agora.";
}

export async function registerAttendance(_previous: AttendanceActionState, form: FormData): Promise<AttendanceActionState> {
  const session = await getAuthenticatedNiteSession();
  if (!session) redirect("/login");
  const intent = value(form, "intent");
  const payloadValue = value(form, "payload");
  let path: string;
  let payload: Record<string, string | number>;

  if (intent === "check-in") {
    const eventId = value(form, "eventId");
    if (!uuid.test(eventId) || payloadValue.length === 0 || payloadValue.length > 64) {
      return { status: "error", message: "O QR não contém um código válido para este encontro." };
    }
    path = "/api/v1/me/attendance/check-in";
    payload = { eventId, payload: payloadValue };
  } else if (intent === "check-out") {
    const expectedVersion = version(form);
    if (expectedVersion === null) return { status: "error", message: "O registro mudou. Atualize o encontro e tente novamente." };
    path = "/api/v1/me/attendance/check-out";
    payload = { expectedVersion };
  } else if (intent === "return") {
    const attendanceId = value(form, "attendanceId");
    const expectedVersion = version(form);
    if (!uuid.test(attendanceId) || expectedVersion === null || payloadValue.length === 0 || payloadValue.length > 64) {
      return { status: "error", message: "Não foi possível validar o QR e o registro de presença." };
    }
    path = `/api/v1/me/attendance/${encodeURIComponent(attendanceId)}/return`;
    payload = { expectedVersion, payload: payloadValue };
  } else {
    return { status: "error", message: "Ação de presença inválida." };
  }

  const result = await postAttendance(path, session.bearer, payload);
  return result.issue
    ? { status: "error", message: issueMessage(result.issue) }
    : { status: "success", message: intent === "check-out" ? "Saída registrada." : "Presença registrada." };
}
