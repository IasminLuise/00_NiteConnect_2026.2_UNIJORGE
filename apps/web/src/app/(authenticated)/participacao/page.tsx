import Link from "next/link";
import type { ReactNode } from "react";
import { Badge, Button, Field, InlineFeedback, Select, Surface, Textarea, TextField } from "../../../components/ui";
import { QrCheckIn } from "../../../components/participation/qr-check-in";
import { getAuthenticatedNiteSession } from "../../../lib/auth/server-session";
import {
  getCalendarExceptions, getExpectedEvents, getJustificationAttachments, getMyAdjustments,
  getMyAttendance, getMyCorrections, getMyJustifications, getMyReplacements,
  getMyWorkLogReviewRequests, getTimeline, getWeeklySummary,
} from "../../../lib/participation/data";
import type { ApiResult } from "../../../lib/api/contracts";
import type {
  AttachmentCollection, AttendanceView, Correction, ExpectedEvent, Justification,
  Page, PersonalCalendarException, Replacement, TimelineItem,
  WorkLogReviewRequest,
} from "../../../lib/participation/contracts";
import { participationAction, uploadJustificationAction } from "../../../lib/participation/actions";
import { addDays, dateAtTimeZone, instantToLocalDateTime, startOfWeek } from "../../../lib/participation/time";
import { formatDuration } from "../../../lib/dashboard/contracts";

export const dynamic = "force-dynamic";
export const metadata = { title: "Minha participação" };

type Params = Record<string, string | string[] | undefined>;
type RequestData<T> = ApiResult<Page<T>>;

const control = "min-h-11 w-full min-w-0 rounded-control border border-nite-border-control bg-nite-surface px-3 py-2 text-type-body-compact text-nite-text-primary placeholder:text-nite-placeholder focus-visible:outline-3 focus-visible:outline-offset-2 focus-visible:outline-nite-focus";
const subtleLink = "inline-flex min-h-10 items-center justify-center rounded-control px-3 text-type-body-compact font-medium text-nite-text-secondary hover:bg-nite-surface-subtle hover:text-nite-text-primary focus-visible:outline-3 focus-visible:outline-offset-2 focus-visible:outline-nite-focus";

function one(value: string | string[] | undefined): string | undefined { return typeof value === "string" ? value : undefined; }
function pageNumber(value: string | string[] | undefined): number {
  const parsed = Number(one(value)); return Number.isInteger(parsed) && parsed >= 0 && parsed < 10_000 ? parsed : 0;
}
function dateValue(value: string | string[] | undefined, fallback: string): string {
  const raw = one(value);
  if (!raw || !/^\d{4}-\d{2}-\d{2}$/.test(raw)) return fallback;
  const date = new Date(`${raw}T00:00:00Z`);
  return !Number.isNaN(date.valueOf()) && date.toISOString().slice(0, 10) === raw ? raw : fallback;
}
function formatDate(value: string, options: Intl.DateTimeFormatOptions = { day: "2-digit", month: "short", year: "numeric" }, zone = "America/Bahia"): string {
  const date = new Date(value.length === 10 ? `${value}T12:00:00` : value);
  return Number.isNaN(date.valueOf()) ? "Data indisponível" : new Intl.DateTimeFormat("pt-BR", { ...options, timeZone: zone }).format(date);
}
function formatTime(value: string, zone: string): string {
  const date = new Date(value);
  return Number.isNaN(date.valueOf()) ? "" : new Intl.DateTimeFormat("pt-BR", { hour: "2-digit", minute: "2-digit", timeZone: zone }).format(date);
}
function formatHours(seconds: number): string { return `${(seconds / 3600).toLocaleString("pt-BR", { maximumFractionDigits: 1 })} h`; }
function modalityLabel(value: string): string { return value === "PRESENCIAL" ? "Presencial" : value === "REMOTA" ? "Remota" : value; }
function statusLabel(value: string): string {
  const labels: Record<string, string> = {
    AGENDADO: "Agendado", ABERTO: "Em andamento", ENCERRADO: "Encerrado", CANCELADO: "Cancelado",
    PENDENTE: "Pendente de revisão", DECLARADO: "Declarado", VALIDADO: "Validado", RECUSADO: "Recusado",
    APROVADA: "Validado", RECUSADA: "Recusado", ACEITA: "Aceita", COMPLEMENTACAO_SOLICITADA: "Complementação solicitada",
    AGUARDANDO_RESPOSTA: "Aguardando sua resposta", SOLICITADA: "Solicitada", EM_ANALISE: "Em análise",
    AUTORIZADA: "Autorizada", ACEITO: "Aceita", RECUSADO_ADMIN: "Recusada",
  };
  return labels[value] ?? value.replaceAll("_", " ").toLocaleLowerCase("pt-BR").replace(/^./, (letter) => letter.toLocaleUpperCase("pt-BR"));
}
function statusVariant(value: string): "neutral" | "info" | "success" | "warning" | "error" {
  if (["VALIDADO", "APROVADA", "ACEITA", "AUTORIZADA"].includes(value)) return "success";
  if (["RECUSADO", "RECUSADA", "CANCELADO"].includes(value)) return "error";
  if (["PENDENTE", "DECLARADO", "AGUARDANDO_RESPOSTA", "COMPLEMENTACAO_SOLICITADA", "EM_ANALISE"].includes(value)) return "warning";
  if (["ABERTO", "AGENDADO"].includes(value)) return "info";
  return "neutral";
}
function apiIssueMessage(issue: string): string {
  if (issue === "access") return "A API não autorizou esta consulta para sua conta.";
  if (issue === "not-found") return "Este conteúdo não está disponível para sua conta.";
  if (issue === "invalid") return "A resposta recebida não corresponde ao contrato esperado.";
  return "Não foi possível consultar este conteúdo agora.";
}
function requestFeedback(value: string | undefined): { title: string; description: string; variant: "success" | "error" } | null {
  if (!value) return null;
  const messages: Record<string, [string, string, "success" | "error"]> = {
    created: ["Registro enviado", "A API recebeu a ação. Consulte a situação atual nesta página.", "success"],
    updated: ["Comprovante enviado", "A API confirmou o envio do arquivo privado.", "success"],
    forbidden: ["Ação indisponível", "A autorização ou a condição exigida não foi confirmada pelo servidor.", "error"],
    "not-found": ["Registro indisponível", "A API não encontrou um registro pessoal elegível para esta ação.", "error"],
    conflict: ["O registro mudou", "Atualize a página e confira a situação antes de tentar novamente.", "error"],
    invalid: ["Confira os dados", "A API recusou os dados enviados. Revise os campos e as condições do registro.", "error"],
    "too-large": ["Arquivo acima do limite", "O tamanho excede o limite de 5.000.000 bytes.", "error"],
    unsupported: ["Formato não aceito", "Use somente PDF, JPG/JPEG ou PNG.", "error"],
    unavailable: ["Serviço indisponível", "Não recebemos confirmação. Consulte o estado atual antes de repetir uma ação.", "error"],
  };
  const message = messages[value];
  return message ? { title: message[0], description: message[1], variant: message[2] } : null;
}
function hrefWith(params: URLSearchParams, key: string, value: string | number): string {
  const next = new URLSearchParams(params); next.set(key, String(value)); return `/participacao?${next}`;
}
function Pagination({ query, keyName, page, total, size }: { query: URLSearchParams; keyName: string; page: number; total: number; size: number }) {
  const pages = Math.max(1, Math.ceil(total / size));
  if (pages <= 1) return null;
  return <nav aria-label="Paginação" className="mt-4 flex items-center justify-between gap-3 border-t border-nite-border-soft pt-3">
    <span className="text-type-metadata text-nite-text-secondary">Página {page + 1} de {pages} · {total} registros</span>
    <div className="flex gap-1">
      {page > 0 ? <Link className={subtleLink} href={hrefWith(query, keyName, page - 1)}>Anterior</Link> : <span className="px-3 py-2 text-type-metadata text-nite-text-muted">Anterior</span>}
      {page + 1 < pages ? <Link className={subtleLink} href={hrefWith(query, keyName, page + 1)}>Próxima</Link> : <span className="px-3 py-2 text-type-metadata text-nite-text-muted">Próxima</span>}
    </div>
  </nav>;
}
function requestContent<T>(result: RequestData<T>, emptyText: string, content: (data: Page<T>) => ReactNode, staleHref?: string): ReactNode {
  if (result.issue) return <InlineFeedback variant={result.issue === "access" ? "warning" : "error"} title="Consulta indisponível" description={apiIssueMessage(result.issue)} />;
  if (result.data.items.length === 0 && result.data.total > 0 && result.data.page > 0) return <InlineFeedback variant="info" title="Esta página mudou" description="A lista foi atualizada. Volte à primeira página para continuar consultando os registros." action={staleHref ? <Link className={subtleLink} href={staleHref}>Primeira página</Link> : undefined} />;
  if (result.data.items.length === 0) return <Empty>{emptyText}</Empty>;
  return content(result.data);
}
function Empty({ children }: { children: ReactNode }) {
  return <p className="rounded-control bg-nite-surface-subtle px-4 py-5 text-type-body-compact text-nite-text-secondary">{children}</p>;
}

export default async function ParticipationPage({ searchParams }: { searchParams: Promise<Params> }) {
  const params = await searchParams;
  const session = await getAuthenticatedNiteSession();
  if (!session) return null;
  const zone = "America/Bahia";
  const today = dateAtTimeZone(new Date(), zone);
  const weekStart = startOfWeek(dateValue(params.week, today));
  const weekEnd = addDays(weekStart, 6);
  const timelineFrom = dateValue(params.from, weekStart);
  const timelineTo = dateValue(params.to, weekEnd);
  const validRange = timelineFrom <= timelineTo;
  const from = validRange ? timelineFrom : weekStart;
  const to = validRange ? timelineTo : weekEnd;
  const timelinePage = pageNumber(params.timelinePage);
  const eventPage = pageNumber(params.eventPage);
  const justificationPage = pageNumber(params.justificationPage);
  const correctionPage = pageNumber(params.correctionPage);
  const reviewPage = pageNumber(params.reviewPage);
  const replacementPage = pageNumber(params.replacementPage);
  const adjustmentPage = pageNumber(params.adjustmentPage);
  const query = new URLSearchParams();
  query.set("week", weekStart); query.set("from", from); query.set("to", to);

  const [weekly, events, timeline, attendance, justifications, corrections, reviewRequests, replacements, adjustments, exceptions] = await Promise.all([
    getWeeklySummary(session.bearer, weekStart, weekEnd),
    getExpectedEvents(session.bearer, weekStart, weekEnd, eventPage),
    getTimeline(session.bearer, from, to, timelinePage),
    getMyAttendance(session.bearer),
    getMyJustifications(session.bearer, justificationPage),
    getMyCorrections(session.bearer, correctionPage),
    getMyWorkLogReviewRequests(session.bearer, reviewPage),
    getMyReplacements(session.bearer, replacementPage),
    getMyAdjustments(session.bearer, adjustmentPage),
    getCalendarExceptions(session.bearer, weekStart, weekEnd),
  ]);
  const attachmentResults = await Promise.all((justifications.data?.items ?? []).map(async (item) =>
    [item.id, await getJustificationAttachments(session.bearer, item.id)] as const));
  const attachmentMap = new Map<string, ApiResult<AttachmentCollection>>(attachmentResults);
  const feedback = requestFeedback(one(params.feedback));
  const policyWeek = weekly.data?.loadPolicy.applicableToWeek ?? null;
  const loadMessage = weekly.data ? policyWeek
    ? `Política ${policyWeek.version} · vigente desde ${formatDate(policyWeek.effectiveFrom, { day: "2-digit", month: "long", year: "numeric" }, weekly.data.zone)}`
    : "Sem obrigação vigente nesta semana. Horas registradas continuam visíveis."
    : "A situação da política não pôde ser consultada.";
  const openAttendance = attendance.data?.items.flatMap((item) => item.periods.map((period) => ({ attendance: item, period })))
    .find(({ period }) => period.endsAt === null) ?? null;
  const currentInstant = new Date();
  const eligibleEvents = (events.data?.items ?? []).filter((event) => event.status !== "CANCELADO" && new Date(event.endsAt) < currentInstant);

  return <main className="mx-auto w-full max-w-7xl min-w-0 space-y-8 overflow-x-clip px-4 py-7 sm:px-7 sm:py-9 lg:px-10">
    {feedback ? <InlineFeedback variant={feedback.variant} announcement={feedback.variant === "success" ? "polite" : "assertive"} title={feedback.title} description={feedback.description} /> : null}

    <header className="grid min-w-0 gap-5 border-b border-nite-border-soft pb-6 lg:grid-cols-[minmax(0,1fr)_auto] lg:items-end">
      <div className="min-w-0 space-y-2">
        <p className="text-type-metadata font-medium uppercase tracking-[0.12em] text-nite-brand-accent">NITE · PARTICIPAÇÃO</p>
        <h1 className="font-display text-type-page-title text-nite-text-primary">Minha participação</h1>
        <p className="max-w-2xl text-type-body-compact text-nite-text-secondary">Uma leitura pessoal dos encontros, registros e solicitações que fazem parte da sua trajetória no núcleo.</p>
      </div>
      <nav aria-label="Navegar entre semanas" className="flex min-w-0 items-center justify-between gap-2 rounded-panel border border-nite-border-soft bg-nite-surface px-2 py-1.5 sm:justify-end">
        <Link aria-label="Semana anterior" className={`${subtleLink} gap-1 px-2 sm:px-3`} href={hrefWith(query, "week", addDays(weekStart, -7))}><span aria-hidden="true">←</span><span className="hidden sm:inline">Anterior</span></Link>
        <span className="min-w-0 whitespace-nowrap text-center text-type-label font-medium text-nite-text-primary">{formatDate(weekStart, { day: "2-digit", month: "short" })} — {formatDate(weekEnd, { day: "2-digit", month: "short", year: "numeric" })}</span>
        <Link aria-label="Próxima semana" className={`${subtleLink} gap-1 px-2 sm:px-3`} href={hrefWith(query, "week", addDays(weekStart, 7))}><span className="hidden sm:inline">Próxima</span><span aria-hidden="true">→</span></Link>
      </nav>
    </header>

    <section aria-labelledby="weekly-heading" className="grid min-w-0 gap-5 lg:grid-cols-[minmax(0,1.35fr)_minmax(17rem,.65fr)]">
      <Surface className="min-w-0" padding="comfortable">
        <div className="flex flex-wrap items-start justify-between gap-3 border-b border-nite-border-soft pb-4">
          <div>
            <p className="text-type-metadata font-medium uppercase tracking-[0.1em] text-nite-text-muted">Visão da semana</p>
            <h2 id="weekly-heading" className="mt-1 font-display text-type-section-title text-nite-text-primary">Seu tempo, por modalidade</h2>
          </div>
          <Badge variant={policyWeek ? "institutional" : "neutral"}>{policyWeek ? "Política aplicável" : "Sem exigência vigente"}</Badge>
        </div>
        {weekly.issue ? <div className="pt-4"><InlineFeedback variant="error" title="Resumo semanal indisponível" description={apiIssueMessage(weekly.issue)} /></div> : <>
          <div className="divide-y divide-nite-border-soft">
            {([ ["presential", "Presencial", "Espaço do núcleo"], ["remote", "Remota", "Atividades a distância"] ] as const).map(([key, title, note]) => {
              const summary = weekly.data.modalities[key];
              return <article key={key} className="grid min-w-0 gap-4 py-4 xl:grid-cols-[minmax(9rem,.7fr)_minmax(0,1.3fr)] xl:items-center">
                <div className="flex min-w-0 items-start gap-3">
                  <span aria-hidden="true" className={`mt-1 inline-flex size-9 shrink-0 items-center justify-center rounded-control border text-type-body-compact font-semibold ${key === "presential" ? "border-status-progress-border bg-status-progress-soft text-status-progress" : "border-nite-brand-primary/25 bg-nite-brand-soft text-nite-brand-accent"}`}>{key === "presential" ? "P" : "R"}</span>
                  <div className="min-w-0"><h3 className="font-medium text-nite-text-primary">{title}</h3><p className="mt-0.5 text-type-metadata text-nite-text-secondary">{note}</p></div>
                </div>
                <div className="grid min-w-0 grid-cols-2 gap-x-4 gap-y-3 sm:grid-cols-3">
                  <Metric label="Registradas" value={formatHours(summary.registeredSeconds)} />
                  <Metric label="Validadas" value={formatHours(summary.validatedSeconds)} />
                  <Metric label="Pendentes" value={formatHours(summary.pendingSeconds)} />
                  <Metric label="Recusadas" value={formatHours(summary.refusedSeconds)} />
                  <Metric label="Em andamento" value={`${summary.openPeriods} ${summary.openPeriods === 1 ? "período" : "períodos"}`} />
                  <Metric label="Exigência" value={summary.requiredSeconds === null ? "Sem vigência" : formatHours(summary.requiredSeconds)} />
                </div>
              </article>;
            })}
          </div>
          <div className="flex flex-col gap-3 border-t border-nite-border-soft pt-4 sm:flex-row sm:items-start sm:justify-between">
            <p className="max-w-2xl text-type-body-compact text-nite-text-secondary">{loadMessage}</p>
            {policyWeek && weekly.data.modalities.presential.remainingSeconds !== null && weekly.data.modalities.remote.remainingSeconds !== null ? <p className="text-type-body-compact font-medium text-nite-text-primary">Restante exigível: {formatHours(weekly.data.modalities.presential.remainingSeconds)} presencial · {formatHours(weekly.data.modalities.remote.remainingSeconds)} remoto</p> : null}
          </div>
          {weekly.data?.loadPolicy.institutionalConfirmation ? <p className="mt-3 text-type-metadata text-nite-text-muted">Parâmetros institucionais confirmados: {weekly.data.loadPolicy.institutionalConfirmation.presentialHours} h presenciais e {weekly.data.loadPolicy.institutionalConfirmation.remoteHours} h remotas por semana. A confirmação não ativa uma vigência.</p> : null}
          {weekly.data?.loadPolicy.status && weekly.data.loadPolicy.status !== "HOMOLOGADA_NO_PERIODO" && weekly.data.loadPolicy.status !== "SEM_POLITICA_APLICAVEL" ? <p className="mt-3 text-type-metadata text-status-warning">A configuração da política exige atenção institucional; esta tela não calcula um saldo fora de uma vigência aplicável.</p> : null}
        </>}
      </Surface>

      <Surface className="grid content-start gap-4" padding="comfortable" variant="subtle">
        <div><p className="text-type-metadata font-medium uppercase tracking-[0.1em] text-nite-text-muted">Agenda pessoal</p><h2 className="mt-1 font-display text-type-section-title text-nite-text-primary">Encontros e presença</h2></div>
        {attendance.issue ? <InlineFeedback variant="warning" title="Ações de presença limitadas" description="Não foi possível confirmar a versão dos seus registros. Atualize a página antes de registrar saída ou retorno." /> : null}
        {events.issue ? <InlineFeedback variant="error" title="Agenda indisponível" description={apiIssueMessage(events.issue)} /> : events.data.items.length === 0 && events.data.total > 0 && events.data.page > 0 ? <InlineFeedback variant="info" title="A agenda mudou" description="Volte à primeira página para continuar consultando os encontros." action={<Link className={subtleLink} href={hrefWith(query, "eventPage", 0)}>Primeira página</Link>} /> : events.data.items.length === 0 ? <Empty>Nenhum encontro esperado nesta semana.</Empty> : <div className="divide-y divide-nite-border-soft">
          {events.data.items.map((event) => <EventRow key={event.id} event={event} zone={weekly.data?.zone ?? zone} attendance={attendance.data?.items.find((item) => item.id === event.attendanceId)} hasOpenAttendance={openAttendance !== null} />)}
        </div>}
        {events.data ? <Pagination query={query} keyName="eventPage" page={events.data.page} total={events.data.total} size={events.data.size} /> : null}
        {openAttendance ? <div className="border-t border-nite-border-soft pt-3"><p className="mb-2 text-type-metadata text-nite-text-secondary">Há um período presencial aberto nos seus registros.</p><form action={participationAction}><input type="hidden" name="intent" value="check-out" /><input type="hidden" name="expectedVersion" value={openAttendance.attendance.version} /><Button type="submit" variant="secondary">Registrar saída</Button></form></div> : null}
      </Surface>
    </section>

    <section id="historico" aria-labelledby="timeline-heading" className="grid min-w-0 scroll-mt-24 gap-5 lg:grid-cols-[minmax(0,1.4fr)_minmax(16rem,.6fr)]">
      <Surface className="min-w-0" padding="comfortable">
        <div className="flex flex-wrap items-end justify-between gap-3 border-b border-nite-border-soft pb-4">
          <div><p className="text-type-metadata font-medium uppercase tracking-[0.1em] text-nite-text-muted">Memória de participação</p><h2 id="timeline-heading" className="mt-1 font-display text-type-section-title text-nite-text-primary">Histórico pessoal</h2></div>
          <span className="text-type-metadata text-nite-text-secondary">Mais recente primeiro · fuso {weekly.data?.zone ?? zone}</span>
        </div>
        <form action="/participacao#historico" method="get" className="grid min-w-0 gap-3 border-b border-nite-border-soft py-4 sm:grid-cols-[1fr_1fr_auto] sm:items-end">
          <input type="hidden" name="week" value={weekStart} />
          <label className="grid min-w-0 gap-1.5 text-type-metadata text-nite-text-secondary">De <TextField name="from" type="date" defaultValue={from} className={control} /></label>
          <label className="grid min-w-0 gap-1.5 text-type-metadata text-nite-text-secondary">Até <TextField name="to" type="date" defaultValue={to} className={control} /></label>
          <Button type="submit" variant="secondary">Aplicar período</Button>
        </form>
        {timeline.issue ? <div className="pt-4"><InlineFeedback variant="error" title="Histórico indisponível" description={apiIssueMessage(timeline.issue)} /></div> : timeline.data.items.length === 0 && timeline.data.total > 0 && timeline.data.page > 0 ? <div className="pt-4"><InlineFeedback variant="info" title="O histórico mudou" description="Volte à primeira página para continuar consultando os registros." action={<Link className={subtleLink} href={hrefWith(query, "timelinePage", 0)}>Primeira página</Link>} /></div> : timeline.data.items.length === 0 ? <div className="pt-4"><Empty>Nenhum registro de participação neste período.</Empty></div> : <ol className="divide-y divide-nite-border-soft">
          {timeline.data.items.map((item) => <TimelineRow key={item.id} item={item} zone={weekly.data?.zone ?? zone} />)}
        </ol>}
        {timeline.data ? <Pagination query={query} keyName="timelinePage" page={timeline.data.page} total={timeline.data.total} size={timeline.data.size} /> : null}
      </Surface>

      <aside className="grid content-start gap-5">
        <Surface padding="comfortable" variant="subtle">
          <p className="text-type-metadata font-medium uppercase tracking-[0.1em] text-nite-text-muted">Registro remoto</p>
          <h2 className="mt-1 font-display text-type-component-title text-nite-text-primary">Adicionar atividade remota</h2>
          <p className="mt-2 text-type-body-compact text-nite-text-secondary">O envio cria um período declarado e pendente de revisão. Ele não é contabilizado como validado antes da revisão.</p>
          <form action={participationAction} className="mt-4 grid min-w-0 gap-3">
            <input type="hidden" name="intent" value="work-log" /><input type="hidden" name="zone" value={weekly.data?.zone ?? zone} />
            <Field id="participation-work-start" label="Início" required><TextField name="startsAt" type="datetime-local" required className={control} /></Field>
            <Field id="participation-work-end" label="Fim" required><TextField name="endsAt" type="datetime-local" required className={control} /></Field>
            <Field id="participation-work-description" label="O que você realizou?" required><Textarea name="description" maxLength={10_000} required /></Field>
            <Button type="submit">Registrar período remoto</Button>
          </form>
        </Surface>
        <CalendarExceptions result={exceptions} />
      </aside>
    </section>

    <section id="solicitacoes" aria-labelledby="requests-heading" className="grid min-w-0 scroll-mt-24 gap-5">
      <div className="flex flex-wrap items-end justify-between gap-3 border-b border-nite-border-soft pb-4">
        <div><p className="text-type-metadata font-medium uppercase tracking-[0.1em] text-nite-text-muted">Acompanhamento pessoal</p><h2 id="requests-heading" className="mt-1 font-display text-type-section-title text-nite-text-primary">Solicitações e decisões</h2></div>
        <p className="max-w-xl text-type-body-compact text-nite-text-secondary">As solicitações são enviadas ao backend e permanecem sujeitas à análise institucional.</p>
      </div>
      <div className="grid min-w-0 gap-5 xl:grid-cols-2">
        <Surface padding="comfortable"><section aria-labelledby="justifications-heading"><h3 id="justifications-heading" className="font-display text-type-component-title text-nite-text-primary">Justificativas</h3>
          <form action={participationAction} className="mt-4 grid min-w-0 gap-3 border-b border-nite-border-soft pb-4">
            <input type="hidden" name="intent" value="justification" />
            <Field id="participation-justification-event" label="Encontro" required><Select name="eventId" placeholder="Selecione um encontro encerrado" required>{eligibleEvents.map((event) => <option key={event.id} value={event.id}>{event.title} · {formatDate(event.startsAt, { day: "2-digit", month: "short" }, zone)}</option>)}</Select></Field>
            <Field id="participation-justification-reason" label="Contexto da ausência" required><Textarea name="reason" maxLength={1_000} required /></Field>
            <p className="text-type-metadata text-nite-text-secondary">Comprovantes são opcionais e só aparecem quando o backend informa disponibilidade privada.</p>
            <Button type="submit" disabled={eligibleEvents.length === 0}>Enviar justificativa</Button>
          </form>
          <div className="pt-4">{requestContent(justifications, "Você ainda não enviou justificativas.", (data) => <ol className="divide-y divide-nite-border-soft">{data.items.map((item) => <JustificationRow key={item.id} item={item} event={eligibleEvents.find((event) => event.id === item.eventId)} attachments={attachmentMap.get(item.id) ?? null} />)}</ol>, hrefWith(query, "justificationPage", 0))}</div>
          {justifications.data ? <Pagination query={query} keyName="justificationPage" page={justifications.data.page} total={justifications.data.total} size={justifications.data.size} /> : null}
        </section></Surface>

        <Surface padding="comfortable"><section aria-labelledby="corrections-heading"><h3 id="corrections-heading" className="font-display text-type-component-title text-nite-text-primary">Correções de registro</h3>
          <form action={participationAction} className="mt-4 grid min-w-0 gap-3 border-b border-nite-border-soft pb-4">
            <input type="hidden" name="intent" value="correction" /><input type="hidden" name="zone" value={weekly.data?.zone ?? zone} />
            <Field id="participation-correction-target" label="Registro relacionado" required><Select name="target" placeholder="Selecione um registro" required>{(timeline.data?.items ?? []).filter((item) => item.periodStatus !== "ABERTO" && item.periodStatus !== "CANCELADO").map((item) => <option key={item.id} value={`period:${item.id}`}>{item.eventTitle ?? item.description ?? "Registro de participação"} · {formatDate(item.startsAt, { day: "2-digit", month: "short" }, zone)}</option>)}</Select></Field>
            <div className="grid min-w-0 gap-3 sm:grid-cols-2"><Field id="participation-correction-start" label="Início proposto" required><TextField name="startsAt" type="datetime-local" required className={control} /></Field><Field id="participation-correction-end" label="Fim proposto" required><TextField name="endsAt" type="datetime-local" required className={control} /></Field></div>
            <Field id="participation-correction-reason" label="O que precisa ser corrigido?" required><Textarea name="reason" maxLength={1_000} required /></Field>
            <Button type="submit" disabled={(timeline.data?.items.filter((item) => item.periodStatus !== "ABERTO" && item.periodStatus !== "CANCELADO").length ?? 0) === 0}>Solicitar correção</Button>
          </form>
          <div className="pt-4">{requestContent(corrections, "Nenhuma solicitação de correção registrada.", (data) => <ol className="divide-y divide-nite-border-soft">{data.items.map((item) => <CorrectionRow key={item.id} item={item} zone={zone} />)}</ol>, hrefWith(query, "correctionPage", 0))}</div>
          {corrections.data ? <Pagination query={query} keyName="correctionPage" page={corrections.data.page} total={corrections.data.total} size={corrections.data.size} /> : null}
        </section></Surface>

        <Surface padding="comfortable"><section aria-labelledby="replacements-heading"><h3 id="replacements-heading" className="font-display text-type-component-title text-nite-text-primary">Reposições e dispensas</h3>
          <form action={participationAction} className="mt-4 grid min-w-0 gap-3 border-b border-nite-border-soft pb-4">
            <input type="hidden" name="intent" value="replacement" />
            <Field id="participation-replacement-week" label="Semana de origem" description="Informe a segunda-feira da semana; o intervalo solicitado terá sete dias consecutivos." required><TextField name="weekStart" type="date" defaultValue={weekStart} required className={control} /></Field>
            <div className="grid min-w-0 gap-3 sm:grid-cols-2"><Field id="participation-replacement-modality" label="Modalidade" required><Select name="modality" required defaultValue="PRESENCIAL"><option value="PRESENCIAL">Presencial</option><option value="REMOTA">Remota</option></Select></Field><Field id="participation-replacement-hours" label="Horas solicitadas" required><TextField name="hours" type="number" min="0.25" step="0.25" required className={control} /></Field></div>
            <Field id="participation-replacement-reason" label="Contexto" required><Textarea name="reason" maxLength={1_000} required /></Field>
            <Button type="submit">Solicitar reposição</Button>
          </form>
          <div className="pt-4">{requestContent(replacements, "Nenhuma reposição registrada.", (data) => <ol className="divide-y divide-nite-border-soft">{data.items.map((item) => <ReplacementRow key={item.adjustment.id} item={item} zone={zone} />)}</ol>, hrefWith(query, "replacementPage", 0))}</div>
          {replacements.data ? <Pagination query={query} keyName="replacementPage" page={replacements.data.page} total={replacements.data.total} size={replacements.data.size} /> : null}
        </section></Surface>

        <Surface padding="comfortable"><section aria-labelledby="review-requests-heading"><h3 id="review-requests-heading" className="font-display text-type-component-title text-nite-text-primary">Revisões de lançamentos remotos</h3>
          <div className="mt-4">{requestContent(reviewRequests, "Nenhuma revisão solicita sua resposta.", (data) => <ol className="divide-y divide-nite-border-soft">{data.items.map((item) => <ReviewRequestRow key={item.id} item={item} zone={weekly.data?.zone ?? zone} />)}</ol>, hrefWith(query, "reviewPage", 0))}</div>
          {reviewRequests.data ? <Pagination query={query} keyName="reviewPage" page={reviewRequests.data.page} total={reviewRequests.data.total} size={reviewRequests.data.size} /> : null}
        </section></Surface>
      </div>
      <Surface padding="comfortable" variant="subtle"><h3 className="font-display text-type-component-title text-nite-text-primary">Resultados e dispensas reconhecidos</h3>
        <div className="mt-3">{adjustments.issue ? <InlineFeedback variant={adjustments.issue === "access" ? "warning" : "error"} title="Consulta indisponível" description={apiIssueMessage(adjustments.issue)} /> : adjustments.data.items.length === 0 ? <Empty>Nenhum ajuste ou dispensa foi registrado para você.</Empty> : <ol className="grid min-w-0 gap-3 md:grid-cols-2">{adjustments.data.items.map((item) => <li key={item.id} className="min-w-0 border-s-2 border-nite-border-strong ps-3"><div className="flex flex-wrap items-center gap-2"><Badge variant={statusVariant(item.status)}>{statusLabel(item.status)}</Badge><span className="text-type-metadata text-nite-text-secondary">{item.kind === "DISPENSA" ? "Dispensa" : "Reposição"} · {formatDate(item.sourceWeekStart, { day: "2-digit", month: "short" }, zone)}</span></div><p className="mt-2 text-type-body-compact text-nite-text-primary">{item.kind === "DISPENSA" ? `${item.authorizedHours ?? item.requestedHours} h dispensadas${item.authorizedModality ? ` · ${modalityLabel(item.authorizedModality)}` : ""}` : `${item.requestedHours} h solicitadas · ${modalityLabel(item.requestedModality)}`}</p><p className="mt-1 text-type-metadata text-nite-text-secondary">{item.decisionReason ?? item.reason}</p></li>)}</ol>}</div>
        {adjustments.data ? <Pagination query={query} keyName="adjustmentPage" page={adjustments.data.page} total={adjustments.data.total} size={adjustments.data.size} /> : null}
      </Surface>
    </section>
  </main>;
}

function Metric({ label, value }: { label: string; value: string }) {
  return <div className="min-w-0"><p className="text-type-metadata text-nite-text-secondary">{label}</p><p className="mt-0.5 break-words text-type-body-compact font-medium tabular-nums text-nite-text-primary">{value}</p></div>;
}

function EventRow({ event, zone, attendance, hasOpenAttendance }: { event: ExpectedEvent; zone: string; attendance?: AttendanceView; hasOpenAttendance: boolean }) {
  const isOpen = event.status === "ABERTO";
  const attendanceIsOpen = attendance?.periods.some((period) => period.endsAt === null) ?? false;
  return <article className="grid min-w-0 gap-3 py-3">
    <div className="min-w-0"><div className="flex flex-wrap items-center gap-2"><h3 className="min-w-0 break-words text-type-body-compact font-medium text-nite-text-primary">{event.title}</h3><Badge variant={statusVariant(event.status)}>{statusLabel(event.status)}</Badge></div>
      <p className="mt-1 text-type-metadata text-nite-text-secondary">{formatDate(event.startsAt, { weekday: "short", day: "2-digit", month: "short" }, zone)} · {formatTime(event.startsAt, zone)}–{formatTime(event.endsAt, zone)} · {modalityLabel(event.modality)}</p>
      {event.description ? <p className="mt-1 max-w-prose text-type-metadata text-nite-text-secondary">{event.description}</p> : null}
      {event.expected ? <p className="mt-1 text-type-metadata text-nite-text-muted">Encontro previsto para você</p> : null}
    </div>
    <div className="flex flex-wrap items-center gap-2">
      {isOpen && !event.checkedIn && !hasOpenAttendance ? <QrCheckIn intent="check-in" targetId={event.id} /> : null}
      {isOpen && event.checkedIn && attendance && !attendanceIsOpen && !hasOpenAttendance ? <QrCheckIn intent="return" targetId={attendance.id} expectedVersion={attendance.version} /> : null}
      {isOpen && event.checkedIn && !attendance ? <span className="text-type-metadata text-nite-text-secondary">Registro confirmado</span> : null}
      {isOpen && !event.checkedIn && hasOpenAttendance ? <span className="max-w-40 text-type-metadata text-nite-text-secondary">Encerre o período aberto para iniciar outra presença.</span> : null}
      {event.checkedIn ? <Badge variant="success">Presença iniciada</Badge> : null}
      {attendanceIsOpen ? <Badge variant="info">Período aberto</Badge> : null}
    </div>
  </article>;
}

function TimelineRow({ item, zone }: { item: TimelineItem; zone: string }) {
  return <li className="grid min-w-0 gap-3 py-4 sm:grid-cols-[minmax(7rem,.25fr)_minmax(0,1fr)]">
    <div className="text-type-metadata text-nite-text-secondary"><p className="font-medium text-nite-text-primary">{formatDate(item.startsAt, { weekday: "short", day: "2-digit", month: "short" }, zone)}</p><p className="mt-1 tabular-nums">{formatTime(item.startsAt, zone)}{item.endsAt ? `–${formatTime(item.endsAt, zone)}` : " · sem saída"}</p></div>
    <div className="min-w-0 border-s-2 border-nite-border-strong ps-3">
      <div className="flex min-w-0 flex-wrap items-center gap-2"><h3 className="min-w-0 break-words text-type-body-compact font-medium text-nite-text-primary">{item.eventTitle ?? item.description ?? (item.kind === "WORK_LOG" ? "Atividade remota" : "Registro de presença")}</h3><Badge variant={statusVariant(item.periodStatus)}>{statusLabel(item.periodStatus)}</Badge></div>
      <div className="mt-1 flex flex-wrap gap-x-3 gap-y-1 text-type-metadata text-nite-text-secondary"><span>{modalityLabel(item.modality)}</span><span>{item.kind === "WORK_LOG" ? "Atividade remota" : "Encontro"}</span>{item.durationSeconds !== null ? <span>{formatDuration(item.durationSeconds)}</span> : null}</div>
      {item.periodStatus === "RECUSADO" && item.reviewReason ? <p className="mt-2 text-type-body-compact text-status-error">Motivo da revisão: {item.reviewReason}</p> : null}
      {item.periodStatus === "CANCELADO" ? <p className="mt-2 text-type-metadata text-nite-text-secondary">Registro mantido no histórico para contextualizar o encontro cancelado; sem duração contabilizada.</p> : null}
      {item.replacementAdjustmentId ? <p className="mt-2 text-type-metadata text-nite-text-secondary">Este período está associado a uma reposição reconhecida.</p> : null}
    </div>
  </li>;
}

function CalendarExceptions({ result }: { result: ApiResult<Page<PersonalCalendarException>> }) {
  return <Surface padding="comfortable"><h2 className="font-display text-type-component-title text-nite-text-primary">Exceções oficiais da semana</h2>
    {result.issue ? <p className="mt-3 text-type-body-compact text-nite-text-secondary">{apiIssueMessage(result.issue)}</p> : result.data.items.length === 0 ? <p className="mt-3 text-type-body-compact text-nite-text-secondary">Nenhuma exceção oficial neste período.</p> : <ul className="mt-3 divide-y divide-nite-border-soft">{result.data.items.map((item) => <li key={item.id} className="py-3"><p className="text-type-body-compact font-medium text-nite-text-primary">{statusLabel(item.type)}</p><p className="mt-1 text-type-metadata text-nite-text-secondary">{formatDate(item.weekStart, { day: "2-digit", month: "short" })}–{formatDate(item.weekEnd, { day: "2-digit", month: "short" })}{item.affectedEventTitle ? ` · ${item.affectedEventTitle}` : ""}</p><p className="mt-1 text-type-metadata text-nite-text-secondary">Dispensa de obrigação: {item.presentialHoursRelieved} h presenciais · {item.remoteHoursRelieved} h remotas. Não representa horas trabalhadas.</p></li>)}</ul>}
  </Surface>;
}

function JustificationRow({ item, event, attachments }: { item: Justification; event?: ExpectedEvent; attachments: ApiResult<AttachmentCollection> | null }) {
  const attachmentAvailability = attachments?.data?.uploadAvailability;
  return <li className="grid min-w-0 gap-3 py-4">
    <div className="flex flex-wrap items-center gap-2"><Badge variant={statusVariant(item.status)}>{statusLabel(item.status)}</Badge><span className="min-w-0 break-words text-type-body-compact font-medium text-nite-text-primary">{event?.title ?? "Justificativa de ausência"}</span></div>
    <p className="text-type-body-compact text-nite-text-secondary">{item.reason}</p>
    {item.decisionReason ? <p className="border-s-2 border-status-warning-border ps-3 text-type-body-compact text-nite-text-secondary">Retorno: {item.decisionReason}</p> : null}
    {item.status === "COMPLEMENTACAO_SOLICITADA" ? <form action={participationAction} className="grid min-w-0 gap-3 rounded-control bg-nite-surface-subtle p-3"><input type="hidden" name="intent" value="complement" /><input type="hidden" name="id" value={item.id} /><input type="hidden" name="expectedVersion" value={item.version} /><Field id={`justification-complement-${item.id}`} label="Complementar justificativa" required><Textarea name="reason" maxLength={1_000} required /></Field><Button type="submit" variant="secondary">Enviar complemento</Button></form> : null}
    <AttachmentSection justificationId={item.id} context={item.status === "COMPLEMENTACAO_SOLICITADA" ? "COMPLEMENTACAO" : "JUSTIFICATIVA"} collection={attachments} availability={attachmentAvailability} />
  </li>;
}

function AttachmentSection({ justificationId, context, collection, availability }: { justificationId: string; context: "JUSTIFICATIVA" | "COMPLEMENTACAO"; collection: ApiResult<AttachmentCollection> | null; availability: string | undefined }) {
  const uploadLabel: Record<string, string> = {
    ARMAZENAMENTO_PRIVADO_NAO_CONFIGURADO: "Envio indisponível: armazenamento privado não configurado.",
    ARMAZENAMENTO_AGUARDANDO_APROVACAO: "Envio indisponível: armazenamento privado aguarda aprovação institucional.",
    RETENCAO_AGUARDANDO_VALIDACAO: "Envio indisponível: política de retenção aguarda validação institucional.",
    ARMAZENAMENTO_INDISPONIVEL: "Arquivo não disponível para consulta neste momento.",
  };
  return <div className="grid min-w-0 gap-2 rounded-control border border-nite-border-soft bg-nite-surface-subtle p-3">
    <p className="text-type-metadata font-medium text-nite-text-secondary">Comprovantes · opcionais</p>
    {collection?.issue ? <p className="text-type-metadata text-nite-text-secondary">Estado de comprovantes não consultável.</p> : null}
    {collection?.data?.items.map((attachment) => <div key={attachment.id} className="flex min-w-0 flex-wrap items-center justify-between gap-2 text-type-metadata">
      <span className="min-w-0 break-words text-nite-text-secondary">{attachment.filename} · {statusLabel(attachment.status)}{attachment.retentionHeld ? " · conservação excepcional" : ""}</span>
      {attachment.downloadable ? <Link className={subtleLink} href={`/api/participacao/justificativas/${encodeURIComponent(justificationId)}/comprovantes/${encodeURIComponent(attachment.id)}`}>Baixar</Link> : null}
    </div>)}
    {availability === "DISPONIVEL" ? <form action={uploadJustificationAction} encType="multipart/form-data" className="grid min-w-0 gap-2 sm:grid-cols-[minmax(0,1fr)_auto] sm:items-end">
      <input type="hidden" name="id" value={justificationId} /><input type="hidden" name="context" value={context} />
      <label className="grid min-w-0 gap-1.5 text-type-metadata text-nite-text-secondary">Adicionar arquivo privado
        <TextField name="file" type="file" accept="application/pdf,image/jpeg,image/png,.pdf,.jpg,.jpeg,.png" required className={control} />
      </label>
      <Button type="submit" variant="secondary">Enviar comprovante</Button>
    </form> : <p className="text-type-metadata text-nite-text-secondary">{uploadLabel[availability ?? ""] ?? "Envio e acesso de comprovantes dependem da configuração privada aprovada."}</p>}
    <p className="text-type-metadata text-nite-text-muted">PDF, JPG/JPEG ou PNG · até 5.000.000 bytes. A API aplica a autorização da justificativa.</p>
  </div>;
}

function CorrectionRow({ item, zone }: { item: Correction; zone: string }) {
  return <li className="grid min-w-0 gap-2 py-3"><div className="flex flex-wrap items-center gap-2"><Badge variant={statusVariant(item.status)}>{statusLabel(item.status)}</Badge><span className="text-type-metadata text-nite-text-secondary">{formatDate(item.proposedStart, { day: "2-digit", month: "short", year: "numeric" }, zone)} · {formatTime(item.proposedStart, zone)}–{formatTime(item.proposedEnd, zone)}</span></div><p className="text-type-body-compact text-nite-text-secondary">{item.reason}</p>{item.decisionReason ? <p className="text-type-metadata text-nite-text-secondary">Retorno: {item.decisionReason}</p> : null}</li>;
}

function ReplacementRow({ item, zone }: { item: Replacement; zone: string }) {
  const adjustment = item.adjustment;
  return <li className="grid min-w-0 gap-2 py-3"><div className="flex flex-wrap items-center gap-2"><Badge variant={statusVariant(adjustment.status)}>{statusLabel(adjustment.status)}</Badge><span className="text-type-metadata text-nite-text-secondary">Semana {formatDate(adjustment.sourceWeekStart, { day: "2-digit", month: "short" }, zone)} · {modalityLabel(adjustment.requestedModality)}</span></div><p className="text-type-body-compact text-nite-text-primary">{adjustment.requestedHours} h solicitadas · {formatDuration(item.recognizedSeconds)} reconhecidas</p><p className="text-type-metadata text-nite-text-secondary">{adjustment.decisionReason ?? adjustment.reason}{adjustment.dueOn ? ` · prazo ${formatDate(adjustment.dueOn, { day: "2-digit", month: "short", year: "numeric" }, zone)}` : ""}</p>{item.remainingSeconds !== null ? <p className="text-type-metadata text-nite-text-secondary">Saldo autorizado da reposição: {formatDuration(item.remainingSeconds)}</p> : null}{item.periods.length ? <ul className="grid gap-1 border-s border-nite-border-strong ps-3 text-type-metadata text-nite-text-secondary">{item.periods.map((period) => <li key={period.periodId}>{period.eventTitle ?? period.description ?? "Período associado"} · {statusLabel(period.reviewStatus)} · {formatDuration(period.recognizedSeconds)}</li>)}</ul> : null}</li>;
}

function ReviewRequestRow({ item, zone }: { item: WorkLogReviewRequest; zone: string }) {
  const canRespond = item.status === "AGUARDANDO_RESPOSTA" && item.response === null;
  return <li className="grid min-w-0 gap-3 py-4"><div className="flex flex-wrap items-center gap-2"><Badge variant={statusVariant(item.status)}>{statusLabel(item.status)}</Badge><span className="text-type-metadata text-nite-text-secondary">Semana {formatDate(item.weekStart, { day: "2-digit", month: "short" }, zone)}</span></div><div className="rounded-control bg-nite-surface-subtle p-3"><p className="text-type-metadata font-medium text-nite-text-secondary">Lançamento atual</p><p className="mt-1 text-type-body-compact text-nite-text-primary">{item.currentWorkLog.description}</p><p className="mt-1 text-type-metadata text-nite-text-secondary">{formatDate(item.currentWorkLog.startsAt, { day: "2-digit", month: "short", year: "numeric" }, zone)} · {formatTime(item.currentWorkLog.startsAt, zone)}–{formatTime(item.currentWorkLog.endsAt, zone)} · {statusLabel(item.currentWorkLog.reviewStatus)}</p><p className="mt-2 text-type-body-compact text-nite-text-secondary">Solicitação de revisão: {item.reason}</p></div>
    {item.response?.decisionReason ? <p className="text-type-body-compact text-nite-text-secondary">Retorno: {item.response.decisionReason}</p> : null}
    {canRespond ? <details className="rounded-control border border-nite-border-soft p-3"><summary className="min-h-10 cursor-pointer py-2 text-type-body-compact font-medium text-nite-brand-accent focus-visible:outline-3 focus-visible:outline-offset-2 focus-visible:outline-nite-focus">Enviar uma proposta de correção</summary><form action={participationAction} className="mt-3 grid min-w-0 gap-3"><input type="hidden" name="intent" value="work-log-response" /><input type="hidden" name="id" value={item.id} /><input type="hidden" name="expectedPeriodVersion" value={item.currentWorkLog.version} /><input type="hidden" name="zone" value={zone} /><Field id={`review-start-${item.id}`} label="Novo início" required><TextField name="startsAt" type="datetime-local" defaultValue={instantToLocalDateTime(item.currentWorkLog.startsAt, zone)} required className={control} /></Field><Field id={`review-end-${item.id}`} label="Novo fim" required><TextField name="endsAt" type="datetime-local" defaultValue={instantToLocalDateTime(item.currentWorkLog.endsAt, zone)} required className={control} /></Field><Field id={`review-description-${item.id}`} label="Descrição proposta" required><Textarea name="description" defaultValue={item.currentWorkLog.description} maxLength={10_000} required /></Field><Field id={`review-reason-${item.id}`} label="Motivo da proposta" required><Textarea name="reason" maxLength={1_000} required /></Field><p className="text-type-metadata text-nite-text-secondary">A proposta fica versionada para análise; não altera o lançamento até a decisão administrativa.</p><Button type="submit" variant="secondary">Enviar proposta</Button></form></details> : null}
  </li>;
}
