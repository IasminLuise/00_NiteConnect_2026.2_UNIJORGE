import { afterEach, describe, expect, it, vi } from "vitest";
import {
  buildThemeBootstrapScript,
  normalizeThemePreference,
  persistThemePreference,
  resolveThemePreference,
  THEME_STORAGE_KEY,
} from "./theme";

afterEach(() => vi.unstubAllGlobals());

describe("preferência de tema do NITE Connect", () => {
  it("normaliza preferências persistidas desconhecidas para system", () => {
    expect(normalizeThemePreference("unknown")).toBe("system");
    expect(normalizeThemePreference(null)).toBe("system");
    expect(normalizeThemePreference("light")).toBe("light");
  });

  it("resolve system conforme a preferência do dispositivo e preserva escolhas explícitas", () => {
    expect(resolveThemePreference("system", "light")).toBe("light");
    expect(resolveThemePreference("system", "dark")).toBe("dark");
    expect(resolveThemePreference("light", "dark")).toBe("light");
    expect(resolveThemePreference("dark", "light")).toBe("dark");
  });

  it("gera bootstrap síncrono usando uma chave de armazenamento própria", () => {
    const script = buildThemeBootstrapScript();
    expect(THEME_STORAGE_KEY).toBe("nite-connect-theme");
    expect(script).toContain(THEME_STORAGE_KEY);
    expect(script).toContain("document.documentElement.dataset.theme");
    expect(script).toContain("prefers-color-scheme: light");
  });

  it("persiste a escolha na chave exclusiva do Connect", () => {
    const setItem = vi.fn();
    vi.stubGlobal("window", { localStorage: { setItem } });

    persistThemePreference("dark");

    expect(setItem).toHaveBeenCalledOnce();
    expect(setItem).toHaveBeenCalledWith("nite-connect-theme", "dark");
  });
});
