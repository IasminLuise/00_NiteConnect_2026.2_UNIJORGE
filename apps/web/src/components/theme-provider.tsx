"use client";

import { createContext, useContext, useEffect, useState } from "react";
import {
  applyThemePreference,
  getSystemTheme,
  normalizeThemePreference,
  persistThemePreference,
  readStoredThemePreference,
  subscribeToSystemTheme,
  THEME_STORAGE_KEY,
  type ResolvedTheme,
  type ThemePreference,
} from "../lib/theme";

type ThemeContextValue = {
  preference: ThemePreference | null;
  resolvedTheme: ResolvedTheme | null;
  ready: boolean;
  setPreference: (preference: ThemePreference) => void;
};

const ThemeContext = createContext<ThemeContextValue | null>(null);

export function ThemeProvider({ children }: Readonly<{ children: React.ReactNode }>) {
  const [preference, setPreferenceState] = useState<ThemePreference | null>(null);
  const [resolvedTheme, setResolvedTheme] = useState<ResolvedTheme | null>(null);

  useEffect(() => {
    const syncPreference = (
      nextPreference: ThemePreference,
      systemTheme: ResolvedTheme = getSystemTheme(),
    ) => {
      setPreferenceState(nextPreference);
      const nextTheme = nextPreference === "system" ? systemTheme : nextPreference;
      setResolvedTheme(nextTheme);
      applyThemePreference(nextPreference);
    };

    syncPreference(readStoredThemePreference());

    const unsubscribeSystem = subscribeToSystemTheme((systemTheme) => {
      if (readStoredThemePreference() === "system") syncPreference("system", systemTheme);
    });
    const handleStorage = (event: StorageEvent) => {
      if (event.key === THEME_STORAGE_KEY || event.key === null) {
        syncPreference(normalizeThemePreference(event.newValue));
      }
    };

    window.addEventListener("storage", handleStorage);
    return () => {
      unsubscribeSystem();
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

  const ready = preference !== null && resolvedTheme !== null;

  return (
    <ThemeContext.Provider value={{ preference, resolvedTheme, ready, setPreference }}>
      {children}
    </ThemeContext.Provider>
  );
}

export function useTheme() {
  const context = useContext(ThemeContext);
  if (!context) throw new Error("useTheme deve ser usado dentro de ThemeProvider.");
  return context;
}
