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
  ) return null;

  const indicators: Record<string, number> = {};
  for (const [key, metric] of Object.entries(value.indicators)) {
    if (typeof metric !== "number" || !Number.isSafeInteger(metric) || metric < 0) return null;
    indicators[key] = metric;
  }

  return { from, to, generatedAt, scope, indicators, columns, rows, unavailableMetrics };
}

export function parseDashboardHistory(value: unknown): DashboardHistory | null {
  if (!isRecord(value) || !Array.isArray(value.columns) || !Array.isArray(value.rows) ||
      typeof value.total !== "number" || !Number.isSafeInteger(value.total) || value.total < 0) return null;
  if (!value.columns.every((column): column is string => typeof column === "string") ||
      !value.rows.every((row): row is string[] => Array.isArray(row) && row.every((cell) => typeof cell === "string"))) return null;
  return { columns: value.columns, rows: value.rows, total: value.total };
}
