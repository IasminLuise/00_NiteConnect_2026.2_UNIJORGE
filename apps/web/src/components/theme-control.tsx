"use client";

import { normalizeThemePreference, themePreferenceLabels, themePreferences } from "../lib/theme";
import { useTheme } from "./theme-provider";

type ThemeControlProps = {
  className?: string;
};

export function ThemeControl({ className }: ThemeControlProps) {
  const { preference, setPreference } = useTheme();

  return (
    <label className={className ?? "theme-control"}>
      <span className="sr-only">Tema da interface</span>
      <select
        aria-label="Tema da interface"
        value={preference}
        onChange={(event) => setPreference(normalizeThemePreference(event.currentTarget.value))}
      >
        {themePreferences.map((option) => (
          <option key={option} value={option}>
            {themePreferenceLabels[option]}
          </option>
        ))}
      </select>
    </label>
  );
}
