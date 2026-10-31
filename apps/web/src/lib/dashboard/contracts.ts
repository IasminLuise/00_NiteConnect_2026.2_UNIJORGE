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

export type LeadershipProject = {
  projectId: string;
  title: string;
};

export type ParticipationModalitySummary = {
  requiredSeconds: number;
  registeredSeconds: number;
  pendingSeconds: number;
  validatedSeconds: number;
  refusedSeconds: number;
  openPeriods: number;
  remainingSeconds: number;
};

export type ParticipationWeeklySummary = {
  zone: string;
  weekStart: string;
  weekEnd: string;
  modalities: { presential: ParticipationModalitySummary; remote: ParticipationModalitySummary };
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

export function parseLeadershipProjects(value: unknown): LeadershipProject[] | null {
  if (!isRecord(value) || !Array.isArray(value.items)) return null;
  const projects: LeadershipProject[] = [];
  for (const item of value.items) {
    if (
      !isRecord(item) ||
      typeof item.projectId !== "string" ||
      typeof item.title !== "string" ||
      !(item.endedAt === null || typeof item.endedAt === "string")
    ) {
      return null;
    }
    if (item.endedAt === null) projects.push({ projectId: item.projectId, title: item.title });
  }
  return projects;
}

function parseModalitySummary(value: unknown): ParticipationModalitySummary | null {
  if (!isRecord(value)) return null;
  const read = (key: string): number | null => {
    const metric = value[key];
    return typeof metric === "number" && Number.isSafeInteger(metric) && metric >= 0 ? metric : null;
  };
  const requiredSeconds = read("requiredSeconds");
  const registeredSeconds = read("registeredSeconds");
  const pendingSeconds = read("pendingSeconds");
  const validatedSeconds = read("validatedSeconds");
  const refusedSeconds = read("refusedSeconds");
  const openPeriods = read("openPeriods");
  const remainingSeconds = read("remainingSeconds");
  if (
    requiredSeconds === null || registeredSeconds === null || pendingSeconds === null ||
    validatedSeconds === null || refusedSeconds === null || openPeriods === null || remainingSeconds === null
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

export function parseParticipationWeeklySummary(value: unknown): ParticipationWeeklySummary | null {
  if (!isRecord(value) || !isRecord(value.modalities)) return null;
  if (typeof value.zone !== "string" || typeof value.weekStart !== "string" || typeof value.weekEnd !== "string") return null;
  const presential = parseModalitySummary(value.modalities.presential);
  const remote = parseModalitySummary(value.modalities.remote);
  if (!presential || !remote) return null;
  return { zone: value.zone, weekStart: value.weekStart, weekEnd: value.weekEnd, modalities: { presential, remote } };
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
