export const THEME_STORAGE_KEY = "nite-connect-theme";
export const THEME_CHANGE_EVENT = "nite-connect-theme-change";

export const themePreferences = ["system", "light", "dark"] as const;
export type ThemePreference = (typeof themePreferences)[number];
export type ResolvedTheme = Exclude<ThemePreference, "system">;

export const themePreferenceLabels: Record<ThemePreference, string> = {
  system: "Sistema",
  light: "Claro",
  dark: "Escuro",
};

export function normalizeThemePreference(value: string | null): ThemePreference {
  if (value === "system" || value === "light" || value === "dark") return value;
  return "system";
}

export function resolveThemePreference(
  preference: ThemePreference,
  systemTheme: ResolvedTheme,
): ResolvedTheme {
  return preference === "system" ? systemTheme : preference;
}

export function getSystemTheme(): ResolvedTheme {
  return typeof window !== "undefined" && window.matchMedia?.("(prefers-color-scheme: light)").matches
    ? "light"
    : "dark";
}

export function subscribeToSystemTheme(listener: (theme: ResolvedTheme) => void): () => void {
  if (typeof window === "undefined" || typeof window.matchMedia !== "function") return () => {};

  const mediaQuery = window.matchMedia("(prefers-color-scheme: light)");
  const handleChange = () => listener(mediaQuery.matches ? "light" : "dark");
  mediaQuery.addEventListener("change", handleChange);

  return () => mediaQuery.removeEventListener("change", handleChange);
}

export function applyThemePreference(preference: ThemePreference): void {
  if (typeof document === "undefined") return;

  const root = document.documentElement;
  const resolvedTheme = resolveThemePreference(preference, getSystemTheme());
  root.dataset.theme = resolvedTheme;
  root.dataset.themePreference = preference;
}

export function readStoredThemePreference(): ThemePreference {
  if (typeof window === "undefined") return "system";
  try {
    return normalizeThemePreference(window.localStorage.getItem(THEME_STORAGE_KEY));
  } catch {
    return "system";
  }
}

export function persistThemePreference(preference: ThemePreference): void {
  if (typeof window === "undefined") return;
  try {
    window.localStorage.setItem(THEME_STORAGE_KEY, preference);
  } catch {
    // A preferência continua valendo na aba atual quando o storage está indisponível.
  }
}

export function buildThemeBootstrapScript(): string {
  return `
(() => {
  const key = ${JSON.stringify(THEME_STORAGE_KEY)};
  const allowed = ${JSON.stringify(themePreferences)};
  let preference = "system";
  try {
    const stored = window.localStorage.getItem(key);
    if (allowed.includes(stored)) preference = stored;
  } catch {}
  const systemTheme = window.matchMedia?.("(prefers-color-scheme: light)").matches ? "light" : "dark";
  const theme = preference === "system" ? systemTheme : preference;
  document.documentElement.dataset.theme = theme;
  document.documentElement.dataset.themePreference = preference;
})();
`;
}
