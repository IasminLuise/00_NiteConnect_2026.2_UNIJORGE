"use client";

import { createContext, useContext, useEffect, useState } from "react";
import {
  applyThemePreference,
  getSystemTheme,
  normalizeThemePreference,
  persistThemePreference,
  readStoredThemePreference,
  THEME_STORAGE_KEY,
  type ResolvedTheme,
  type ThemePreference,
} from "../lib/theme";

type ThemeContextValue = {
  preference: ThemePreference;
  resolvedTheme: ResolvedTheme;
  setPreference: (preference: ThemePreference) => void;
};

const ThemeContext = createContext<ThemeContextValue | null>(null);

export function ThemeProvider({ children }: Readonly<{ children: React.ReactNode }>) {
  const [preference, setPreferenceState] = useState<ThemePreference>("system");
  const [resolvedTheme, setResolvedTheme] = useState<ResolvedTheme>("dark");

  useEffect(() => {
    const syncPreference = (nextPreference: ThemePreference) => {
      setPreferenceState(nextPreference);
      const nextTheme = nextPreference === "system" ? getSystemTheme() : nextPreference;
      setResolvedTheme(nextTheme);
      applyThemePreference(nextPreference);
    };

    syncPreference(readStoredThemePreference());

    const mediaQuery = window.matchMedia?.("(prefers-color-scheme: light)");
    const handleSystemChange = () => {
      if (readStoredThemePreference() === "system") syncPreference("system");
    };
    const handleStorage = (event: StorageEvent) => {
      if (event.key === THEME_STORAGE_KEY || event.key === null) {
        syncPreference(normalizeThemePreference(event.newValue));
      }
    };

    mediaQuery?.addEventListener("change", handleSystemChange);
    window.addEventListener("storage", handleStorage);
    return () => {
      mediaQuery?.removeEventListener("change", handleSystemChange);
      window.removeEventListener("storage", handleStorage);
    };
  }, []);

  const setPreference = (nextPreference: ThemePreference) => {
    persistThemePreference(nextPreference);
    setPreferenceState(nextPreference);
    const nextTheme = nextPreference === "system" ? getSystemTheme() : nextPreference;
    setResolvedTheme(nextTheme);
    applyThemePreference(nextPreference);
  };

  return (
    <ThemeContext.Provider value={{ preference, resolvedTheme, setPreference }}>
      {children}
    </ThemeContext.Provider>
  );
}

export function useTheme() {
  const context = useContext(ThemeContext);
  if (!context) throw new Error("useTheme deve ser usado dentro de ThemeProvider.");
  return context;
}
