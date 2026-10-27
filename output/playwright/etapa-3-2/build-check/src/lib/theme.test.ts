import { afterEach, describe, expect, it, vi } from "vitest";
import { createElement } from "react";
import { renderToString } from "react-dom/server";
import { runInNewContext } from "node:vm";
import { ThemeProvider, useTheme } from "../components/theme-provider";
import * as themeModule from "./theme";
import {
  buildThemeBootstrapScript,
  normalizeThemePreference,
  persistThemePreference,
  readStoredThemePreference,
  resolveThemePreference,
  THEME_STORAGE_KEY,
} from "./theme";

function ThemeStateProbe() {
  const { preference, resolvedTheme } = useTheme();
  return createElement("output", null, `${preference ?? "pendente"}:${resolvedTheme ?? "pendente"}`);
}

afterEach(() => vi.unstubAllGlobals());

describe("preferência de tema do NITE Connect", () => {
  it("renderiza um estado neutro até o navegador resolver a preferência persistida", () => {
    const markup = renderToString(
      createElement(ThemeProvider, null, createElement(ThemeStateProbe)),
    );

    expect(markup).toContain("pendente:pendente");
    expect(markup).not.toContain("system:dark");
  });

  it("acompanha mudanças dinâmicas de prefers-color-scheme e remove a assinatura", () => {
    type ThemeListener = (theme: "light" | "dark") => void;
    type ThemeSubscription = (listener: ThemeListener) => () => void;
    const isThemeSubscription = (value: unknown): value is ThemeSubscription =>
      typeof value === "function";
    let matchesLight = false;
    let handler: (() => void) | undefined;
    const removeEventListener = vi.fn();
    const mediaQuery = {
      get matches() { return matchesLight; },
      addEventListener: (_type: string, listener: () => void) => { handler = listener; },
      removeEventListener,
    };
    vi.stubGlobal("window", { matchMedia: () => mediaQuery });

    const subscription = Reflect.get(themeModule, "subscribeToSystemTheme");
    expect(isThemeSubscription(subscription)).toBe(true);
    if (!isThemeSubscription(subscription)) return;

    const themes: Array<"light" | "dark"> = [];
    const unsubscribe = subscription((theme) => themes.push(theme));
    matchesLight = true;
    handler?.();
    matchesLight = false;
    handler?.();
    unsubscribe();

    expect(themes).toEqual(["light", "dark"]);
    expect(removeEventListener).toHaveBeenCalledOnce();
  });

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

  it("aplica no bootstrap a preferência salva ou resolve System para o tema do dispositivo", () => {
    const resolveBootstrap = (stored: string | null, prefersLight: boolean) => {
      const documentElement = { dataset: {} as Record<string, string> };
      runInNewContext(buildThemeBootstrapScript(), {
        window: {
          localStorage: { getItem: () => stored },
          matchMedia: () => ({ matches: prefersLight }),
        },
        document: { documentElement },
      });
      return documentElement.dataset;
    };

    expect(resolveBootstrap("light", false)).toEqual({ theme: "light", themePreference: "light" });
    expect(resolveBootstrap("dark", true)).toEqual({ theme: "dark", themePreference: "dark" });
    expect(resolveBootstrap("system", true)).toEqual({ theme: "light", themePreference: "system" });
    expect(resolveBootstrap("invalid", false)).toEqual({ theme: "dark", themePreference: "system" });
  });

  it("persiste a escolha na chave exclusiva do Connect", () => {
    const setItem = vi.fn();
    vi.stubGlobal("window", { localStorage: { setItem } });

    persistThemePreference("dark");

    expect(setItem).toHaveBeenCalledOnce();
    expect(setItem).toHaveBeenCalledWith("nite-connect-theme", "dark");
  });

  it("mantém o fallback System quando o armazenamento local está indisponível", () => {
    vi.stubGlobal("window", {
      localStorage: {
        getItem: () => { throw new Error("storage indisponível"); },
        setItem: () => { throw new Error("storage indisponível"); },
      },
    });

    expect(readStoredThemePreference()).toBe("system");
    expect(() => persistThemePreference("light")).not.toThrow();
  });
});
