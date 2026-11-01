import Link from "next/link";
import { notFound } from "next/navigation";
import { ActivityDetail } from "../../../../components/activities/activity-detail";
import { requireAuthenticatedNiteSession } from "../../../../lib/auth/server-session";
import {
  getActivity,
  getActivityAssignees,
  getActivityCandidates,
  getActivityCapabilities,
  getActivityEvidence,
  getActivityHistory,
  getAllMyProjects,
  getEvidenceCapabilities,
} from "../../../../lib/activities/data";
import type { ApiIssue } from "../../../../lib/api/contracts";

export const dynamic = "force-dynamic";
export const metadata = { title: "Atividade" };

type Params = Record<string, string | string[] | undefined>;
type PageProps = { params: Promise<{ id: string }>; searchParams: Promise<Params> };
const uuidPattern = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

function one(value: string | string[] | undefined): string | undefined {
  return typeof value === "string" ? value : undefined;
}

function pageIndex(value: string | undefined): number {
  if (!value || !/^\d{1,6}$/.test(value)) return 0;
  const page = Number(value);
  return Number.isSafeInteger(page) ? page : 0;
}

function safeReturnTo(value: string | undefined): string {
  if (!value || !value.startsWith("/atividades") || value.startsWith("//") || value.includes("\\")) return "/atividades";
  try {
    const target = new URL(value, "https://nite-connect.invalid");
    return target.origin === "https://nite-connect.invalid" && target.pathname === "/atividades" ? `${target.pathname}${target.search}` : "/atividades";
  } catch {
    return "/atividades";
  }
}

function problem(issue: ApiIssue, id: string) {
  const access = issue === "access";
  const unavailable = issue === "unavailable" || issue === "invalid";
  return <main className="mx-auto w-full max-w-3xl space-y-5 px-4 py-12 sm:px-7 sm:py-16">
    <Link href="/atividades" className="text-type-body-compact font-medium text-nite-brand-primary underline underline-offset-4 focus-visible:outline-3 focus-visible:outline-offset-2 focus-visible:outline-nite-focus">Voltar para minhas atividades</Link>
    <section aria-live="polite" className="border-y border-nite-border-soft py-8">
      <h1 className="font-display text-type-section-title text-nite-text-primary">{access ? "Você não tem acesso a esta atividade." : unavailable ? "Não foi possível carregar a atividade." : "A atividade não está disponível."}</h1>
      <p className="mt-2 text-type-body-compact text-nite-text-secondary">{access ? "A consulta não está autorizada para sua identidade neste contexto." : unavailable ? "Tente atualizar a página em alguns instantes." : "O registro pode ter sido removido ou não estar mais disponível."}</p>
      {unavailable ? <Link href={`/atividades/${encodeURIComponent(id)}`} className="mt-4 inline-flex min-h-11 items-center rounded-control border border-nite-border-soft px-3 text-type-body-compact font-medium text-nite-text-primary hover:bg-nite-surface-subtle focus-visible:outline-3 focus-visible:outline-offset-2 focus-visible:outline-nite-focus">Tentar novamente</Link> : null}
    </section>
  </main>;
}

export default async function ActivityDetailPage({ params, searchParams }: PageProps) {
  const [{ id }, query, session] = await Promise.all([params, searchParams, requireAuthenticatedNiteSession()]);
  if (!uuidPattern.test(id)) notFound();
  const result = await getActivity(session.bearer, id);
  if (result.issue) {
    if (result.issue === "not-found") notFound();
    return problem(result.issue, id);
  }

  const activity = result.data;
  const returnTo = safeReturnTo(one(query.from));
  const evidencePage = pageIndex(one(query.evidencePage));
  const historyPage = pageIndex(one(query.historyPage));
  const candidateInput = one(query.candidateQ) ?? "";
  const candidateQuery = candidateInput.trim();
  const candidateSearchInvalid = candidateInput.length > 160 || (candidateInput.length > 0 && (Array.from(candidateQuery).length < 2 || /[\u0000-\u001f\u007f-\u009f]/.test(candidateInput)));
  const candidatePage = pageIndex(one(query.candidatePage));
  const participantsOpen = one(query.participants) === "1";
  const candidatesRequested = participantsOpen || candidateInput.length > 0 || candidatePage > 0;
  const candidateSearchReady = !candidateSearchInvalid && Array.from(candidateQuery).length >= 2;
  const [capabilityResult, assigneeResult, evidenceResult, projectsResult] = await Promise.all([
    getActivityCapabilities(session.bearer, id),
    getActivityAssignees(session.bearer, id),
    getActivityEvidence(session.bearer, id, evidencePage),
    activity.projectId ? getAllMyProjects(session.bearer) : Promise.resolve({ data: [], issue: null } as const),
  ]);
  const capabilities = capabilityResult.issue ? null : capabilityResult.data;
  const historyAllowed = capabilities?.history === true;
  const [historyResult, candidateResult, withdrawResults] = await Promise.all([
    historyAllowed ? getActivityHistory(session.bearer, id, historyPage) : Promise.resolve(null),
    candidatesRequested && candidateSearchReady && capabilities?.manageAssignees ? getActivityCandidates(session.bearer, id, { page: candidatePage, q: candidateQuery }) : Promise.resolve(null),
    evidenceResult.data ? Promise.all(evidenceResult.data.items.map(async (evidence) => [evidence.id, await getEvidenceCapabilities(session.bearer, id, evidence.id)] as const)) : Promise.resolve([]),
  ]);
  const evidenceWithdraw = Object.fromEntries(withdrawResults.flatMap(([evidenceId, capability]) => capability.issue === null && capability.data.withdraw ? [[evidenceId, true]] : []));
  const projectTitle = activity.projectId ? (projectsResult.data ?? []).find((project) => project.id === activity.projectId)?.title ?? null : null;

  return <ActivityDetail
    activity={activity}
    projectTitle={projectTitle}
    memberId={session.member.id}
    returnTo={returnTo}
    capabilities={capabilities}
    assignees={assigneeResult.issue ? null : assigneeResult.data}
    assigneeIssue={assigneeResult.issue}
    candidates={candidateResult?.issue ? null : candidateResult?.data ?? null}
    candidateIssue={candidateResult?.issue ?? null}
    candidateQuery={candidateQuery}
    candidatePage={candidateResult?.data?.page ?? candidatePage}
    candidateSearchInvalid={candidateSearchInvalid}
    participantsOpen={participantsOpen}
    evidence={evidenceResult.issue ? null : evidenceResult.data}
    evidenceIssue={evidenceResult.issue}
    evidenceWithdraw={evidenceWithdraw}
    history={historyResult?.issue ? null : historyResult?.data ?? null}
    historyIssue={historyResult?.issue ?? null}
    historyAllowed={historyAllowed}
    evidencePage={evidenceResult.data?.page ?? evidencePage}
    historyPage={historyResult?.data?.page ?? historyPage}
    feedback={one(query.feedback)}
  />;
}
