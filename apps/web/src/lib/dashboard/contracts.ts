export type DashboardResponse = {
  from: string;
  to: string;
  generatedAt: string;
  scope: string;
  indicators: Record<string, number>;
  columns: string[];
  rows: string[][];
  unavailableMetrics: string[];
};

export type ParticipationModalitySummary = {
  requiredSeconds: number | null;
  registeredSeconds: number;
  pendingSeconds: number;
  validatedSeconds: number;
  refusedSeconds: number;
  openPeriods: number;
  remainingSeconds: number | null;
};

export type ParticipationLoadPolicyVersion = {
  id: string;
  version: number;
  effectiveFrom: string;
  effectiveUntil: string | null;
  presentialHours: number;
  remoteHours: number;
};

export type PlannedParticipationLoadPolicy = {
  id: string;
  version: number;
  presentialHours: number;
  remoteHours: number;
  effectiveFrom: string | null;
  intendedSemester: string | null;
  effectiveWindow: "PRIMEIRA_SEMANA_OFICIAL_DE_ATIVIDADES" | null;
  status: "SEM_VIGENCIA_DEFINIDA" | "AGUARDANDO_ATIVACAO" | "ATIVADA_PARA_FUTURO";
};

export type ParticipationLoadPolicy = {
  status: "HOMOLOGADA_NO_PERIODO" | "SEM_POLITICA_APLICAVEL" | "CONFIGURACAO_PARCIAL_NO_PERIODO" | "MULTIPLAS_CONFIGURACOES_NO_PERIODO" | "CONFIGURACOES_SOBREPOSTAS";
  currentConfiguration: ParticipationLoadPolicyVersion | null;
  applicableToWeek: ParticipationLoadPolicyVersion | null;
  institutionalConfirmation: { presentialHours: number; remoteHours: number; status: "CONFIRMADA" };
  plannedConfiguration: PlannedParticipationLoadPolicy | null;
};

export type ParticipationWeeklySummary = {
  zone: string;
  weekStart: string;
  weekEnd: string;
  modalities: { presential: ParticipationModalitySummary; remote: ParticipationModalitySummary };
  loadPolicy: ParticipationLoadPolicy;
};

export type DashboardHistory = { columns: string[]; rows: string[][]; total: number };

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null && !Array.isArray(value);
}

export function parseDashboard(value: unknown): DashboardResponse | null {
  if (!isRecord(value) || !isRecord(value.indicators)) return null;
  const { from, to, generatedAt, scope, columns, rows, unavailableMetrics } = value;
  if (
    typeof from !== "string" ||
    typeof to !== "string" ||
    typeof generatedAt !== "string" ||
    typeof scope !== "string" ||
    !Array.isArray(columns) ||
    !columns.every((column): column is string => typeof column === "string") ||
    !Array.isArray(rows) ||
    !rows.every((row): row is string[] => Array.isArray(row) && row.every((cell) => typeof cell === "string")) ||
    !Array.isArray(unavailableMetrics) ||
    !unavailableMetrics.every((metric): metric is string => typeof metric === "string")
  ) {
    return null;
  }

  const indicators: Record<string, number> = {};
  for (const [key, metric] of Object.entries(value.indicators)) {
    if (typeof metric !== "number" || !Number.isSafeInteger(metric) || metric < 0) return null;
    indicators[key] = metric;
  }

  return {
    from,
    to,
    generatedAt,
    scope,
    indicators,
    columns,
    rows,
    unavailableMetrics,
  };
}

function parseModalitySummary(value: unknown): ParticipationModalitySummary | null {
  if (!isRecord(value)) return null;
  const read = (key: string): number | null | undefined => {
    const metric = value[key];
    if (metric === null) return null;
    return typeof metric === "number" && Number.isSafeInteger(metric) && metric >= 0 ? metric : undefined;
  };
  const requiredSeconds = read("requiredSeconds");
  const registeredSeconds = read("registeredSeconds");
  const pendingSeconds = read("pendingSeconds");
  const validatedSeconds = read("validatedSeconds");
  const refusedSeconds = read("refusedSeconds");
  const openPeriods = read("openPeriods");
  const remainingSeconds = read("remainingSeconds");
  if (
    requiredSeconds === undefined || remainingSeconds === undefined ||
    registeredSeconds === null || registeredSeconds === undefined ||
    pendingSeconds === null || pendingSeconds === undefined ||
    validatedSeconds === null || validatedSeconds === undefined ||
    refusedSeconds === null || refusedSeconds === undefined ||
    openPeriods === null || openPeriods === undefined
  ) return null;
  return {
    requiredSeconds,
    registeredSeconds,
    pendingSeconds,
    validatedSeconds,
    refusedSeconds,
    openPeriods,
    remainingSeconds,
  };
}

function parseLoadPolicyVersion(value: unknown): ParticipationLoadPolicyVersion | null | undefined {
  if (value === null) return null;
  if (!isRecord(value) || typeof value.id !== "string" ||
      typeof value.version !== "number" || !Number.isSafeInteger(value.version) || value.version <= 0 ||
      typeof value.effectiveFrom !== "string" ||
      value.effectiveUntil !== null && typeof value.effectiveUntil !== "string" ||
      typeof value.presentialHours !== "number" || !Number.isFinite(value.presentialHours) || value.presentialHours < 0 ||
      typeof value.remoteHours !== "number" || !Number.isFinite(value.remoteHours) || value.remoteHours < 0) return undefined;
  return {
    id: value.id,
    version: value.version,
    effectiveFrom: value.effectiveFrom,
    effectiveUntil: value.effectiveUntil,
    presentialHours: value.presentialHours,
    remoteHours: value.remoteHours,
  };
}

function parsePlannedLoadPolicy(value: unknown): PlannedParticipationLoadPolicy | null | undefined {
  if (value === undefined || value === null) return null;
  if (!isRecord(value) || typeof value.id !== "string" ||
      typeof value.version !== "number" || !Number.isSafeInteger(value.version) || value.version <= 0 ||
      typeof value.presentialHours !== "number" || !Number.isFinite(value.presentialHours) || value.presentialHours < 0 ||
      typeof value.remoteHours !== "number" || !Number.isFinite(value.remoteHours) || value.remoteHours < 0 ||
      value.effectiveFrom !== null && typeof value.effectiveFrom !== "string" ||
      value.intendedSemester !== null && typeof value.intendedSemester !== "string" ||
      value.effectiveWindow !== null && value.effectiveWindow !== "PRIMEIRA_SEMANA_OFICIAL_DE_ATIVIDADES") return undefined;
  const statuses: PlannedParticipationLoadPolicy["status"][] = [
    "SEM_VIGENCIA_DEFINIDA", "AGUARDANDO_ATIVACAO", "ATIVADA_PARA_FUTURO",
  ];
  const status = statuses.find((candidate) => candidate === value.status);
  if (!status) return undefined;
  return {
    id: value.id,
    version: value.version,
    presentialHours: value.presentialHours,
    remoteHours: value.remoteHours,
    effectiveFrom: value.effectiveFrom,
    intendedSemester: value.intendedSemester,
    effectiveWindow: value.effectiveWindow,
    status,
  };
}

function parseLoadPolicy(value: unknown): ParticipationLoadPolicy | null {
  if (!isRecord(value) || typeof value.status !== "string" || !isRecord(value.institutionalConfirmation)) return null;
  const statuses: ParticipationLoadPolicy["status"][] = [
    "HOMOLOGADA_NO_PERIODO",
    "SEM_POLITICA_APLICAVEL",
    "CONFIGURACAO_PARCIAL_NO_PERIODO",
    "MULTIPLAS_CONFIGURACOES_NO_PERIODO",
    "CONFIGURACOES_SOBREPOSTAS",
  ];
  const status = statuses.find((candidate) => candidate === value.status);
  const currentConfiguration = parseLoadPolicyVersion(value.currentConfiguration);
  const applicableToWeek = parseLoadPolicyVersion(value.applicableToWeek);
  const plannedConfiguration = parsePlannedLoadPolicy(value.plannedConfiguration);
  const confirmation = value.institutionalConfirmation;
  if (!status ||
      currentConfiguration === undefined || applicableToWeek === undefined ||
      plannedConfiguration === undefined ||
      typeof confirmation.presentialHours !== "number" || confirmation.presentialHours < 0 ||
      typeof confirmation.remoteHours !== "number" || confirmation.remoteHours < 0 ||
      confirmation.status !== "CONFIRMADA") return null;
  return {
    status,
    currentConfiguration,
    applicableToWeek,
    institutionalConfirmation: {
      presentialHours: confirmation.presentialHours,
      remoteHours: confirmation.remoteHours,
      status: "CONFIRMADA",
    },
    plannedConfiguration,
  };
}

export function parseParticipationWeeklySummary(value: unknown): ParticipationWeeklySummary | null {
  if (!isRecord(value) || !isRecord(value.modalities)) return null;
  if (typeof value.zone !== "string" || typeof value.weekStart !== "string" || typeof value.weekEnd !== "string") return null;
  const presential = parseModalitySummary(value.modalities.presential);
  const remote = parseModalitySummary(value.modalities.remote);
  const loadPolicy = parseLoadPolicy(value.loadPolicy);
  if (!presential || !remote || !loadPolicy) return null;
  return { zone: value.zone, weekStart: value.weekStart, weekEnd: value.weekEnd, modalities: { presential, remote }, loadPolicy };
}

export function parseDashboardHistory(value: unknown): DashboardHistory | null {
  if (!isRecord(value) || !Array.isArray(value.columns) || !Array.isArray(value.rows) ||
      typeof value.total !== "number" || !Number.isSafeInteger(value.total) || value.total < 0) return null;
  if (!value.columns.every((column): column is string => typeof column === "string") ||
      !value.rows.every((row): row is string[] => Array.isArray(row) && row.every((cell) => typeof cell === "string"))) return null;
  return { columns: value.columns, rows: value.rows, total: value.total };
}

export function formatDuration(seconds: number): string {
  const hours = Math.floor(seconds / 3_600);
  const minutes = Math.floor((seconds % 3_600) / 60);
  return `${hours}h ${String(minutes).padStart(2, "0")}min`;
}
