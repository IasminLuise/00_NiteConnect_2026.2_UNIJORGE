"use client";

import { useEffect, useState } from "react";
import { ThemeControl } from "./theme-control";
import { useTheme } from "./theme-provider";
import type { ResolvedTheme } from "../lib/theme";

const institutionalTokens = [
  ["Fundo principal", "--nite-background"],
  ["Fundo de seção", "--nite-section"],
  ["Azul institucional", "--nite-brand-primary"],
  ["Acento ciano", "--nite-brand-accent"],
  ["Texto principal", "--nite-text-primary"],
  ["Texto secundário", "--nite-text-secondary"],
  ["Texto auxiliar", "--nite-text-muted"],
] as const;

const surfaceTokens = [
  ["Canvas", "--nite-background"],
  ["Navegação", "--nite-section"],
  ["Padrão", "--nite-surface"],
  ["Elevada · candidata", "--nite-surface-elevated"],
  ["Sutil", "--nite-surface-subtle"],
] as const;

const statusFamilies = [
  ["Rascunho", "draft"],
  ["Em andamento", "progress"],
  ["Validado", "validated"],
  ["Finalizado", "done"],
  ["Atenção", "warning"],
  ["Erro", "error"],
] as const;

const dataSeries = [
  ["Série 01", "--nite-data-1", 72],
  ["Série 02", "--nite-data-2", 48],
  ["Série 03", "--nite-data-3", 88],
  ["Série 04", "--nite-data-4", 61],
  ["Série 05", "--nite-data-5", 36],
  ["Série 06", "--nite-data-6", 54],
] as const;

function TokenSwatch({ label, token }: { label: string; token: string }) {
  const { ready, resolvedTheme } = useTheme();
  const [snapshot, setSnapshot] = useState<{ theme: ResolvedTheme; value: string | null } | null>(null);

  useEffect(() => {
    if (!ready || !resolvedTheme) return;
    const theme = resolvedTheme;
    const frame = window.requestAnimationFrame(() => {
      const value = getComputedStyle(document.documentElement).getPropertyValue(token).trim() || null;
      setSnapshot({ theme, value });
    });
    return () => window.cancelAnimationFrame(frame);
  }, [ready, resolvedTheme, token]);

  const value = snapshot?.theme === resolvedTheme ? snapshot.value : null;

  return (
    <article className="lab-token">
      <span className="lab-token-color" style={{ backgroundColor: `var(${token})` }} aria-hidden="true" />
      <span className="lab-token-copy">
        <strong>{label}</strong>
        <code>{token}</code>
        <small>{value ?? (!ready ? "Aguardando tema…" : "Lendo token…")}</small>
      </span>
    </article>
  );
}

export function ColorFoundationLab() {
  const { resolvedTheme } = useTheme();
  const themeLabel = resolvedTheme === "light" ? "Claro" : resolvedTheme === "dark" ? "Escuro" : "Aguardando";

  return (
    <main className="lab-shell">
      <header className="lab-header">
        <div>
          <p className="lab-eyebrow">NITE CONNECT · FUNDAÇÃO CROMÁTICA</p>
          <h1>Design System Lab</h1>
          <p className="lab-intro">Inspeção dos tokens reais da aplicação. Valores de superfície elevada e séries de dados seguem como candidatos.</p>
        </div>
        <div className="lab-header-controls">
          <span className="lab-theme-state" aria-live="polite" aria-atomic="true">Tema aplicado: <strong>{themeLabel}</strong></span>
          <ThemeControl />
        </div>
      </header>

      <section className="lab-section" aria-labelledby="institution-heading">
        <div className="lab-section-heading"><div><p className="lab-eyebrow">01 · IDENTIDADE INSTITUCIONAL</p><h2 id="institution-heading">Cores herdadas do Portal NITE</h2></div><p>Tokens ativos no tema selecionado.</p></div>
        <div className="lab-token-grid">{institutionalTokens.map(([label, token]) => <TokenSwatch key={token} label={label} token={token} />)}</div>
      </section>

      <section className="lab-section" aria-labelledby="surfaces-heading">
        <div className="lab-section-heading"><div><p className="lab-eyebrow">02 · HIERARQUIA</p><h2 id="surfaces-heading">Superfícies e elevação</h2></div><p>A amostra elevada escura é uma hipótese visual.</p></div>
        <div className="lab-token-grid lab-token-grid-five">{surfaceTokens.map(([label, token]) => <TokenSwatch key={token} label={label} token={token} />)}</div>
        <div className="lab-composition">
          <aside className="lab-nav-sample"><span className="lab-mini-mark">N</span><strong>NITE Connect</strong><span className="lab-nav-item lab-nav-item-active">Visão geral</span><span className="lab-nav-item">Projetos</span><span className="lab-nav-item">Integrantes</span></aside>
          <div className="lab-canvas-sample"><div className="lab-demo-panel"><p className="lab-eyebrow">PAINEL DE EXEMPLO</p><h3>Camadas de interface</h3><p>Uma superfície padrão sobre o canvas; o menu contextual demonstra elevação.</p><div className="lab-floating-menu"><strong>Menu contextual</strong><span>Ação disponível</span><span className="lab-menu-selected">Item selecionado <b aria-hidden="true">✓</b></span></div></div></div>
        </div>
      </section>

      <section className="lab-section" aria-labelledby="status-heading">
        <div className="lab-section-heading"><div><p className="lab-eyebrow">03 · SEMÂNTICA OPERACIONAL</p><h2 id="status-heading">Famílias de status</h2></div><p>Rótulo e marcador acompanham a cor.</p></div>
        <div className="lab-status-grid">{statusFamilies.map(([label, family]) => <span className="lab-status" data-family={family} key={family}><span className="lab-status-dot" aria-hidden="true" />{label}</span>)}</div>
      </section>

      <section className="lab-section" aria-labelledby="interaction-heading">
        <div className="lab-section-heading"><div><p className="lab-eyebrow">04 · INTERAÇÃO</p><h2 id="interaction-heading">Repouso, hover, seleção e foco</h2></div><p>Use Tab para alcançar o controle focável.</p></div>
        <div className="lab-interaction-row">
          <button className="lab-action" type="button">Controle em repouso</button>
          <button className="lab-action lab-action-selected" type="button" aria-pressed="true">Selecionado <span aria-hidden="true">✓</span></button>
          <button className="lab-action" type="button">Foque com teclado</button>
          <button className="lab-action" type="button" disabled>Desabilitado</button>
        </div>
      </section>

      <section className="lab-section" aria-labelledby="data-heading">
        <div className="lab-section-heading"><div><p className="lab-eyebrow">05 · VISUALIZAÇÃO DE DADOS</p><h2 id="data-heading">Séries categóricas candidatas</h2></div><p>Dados demonstrativos, sem semântica de desempenho.</p></div>
        <div className="lab-series-grid">{dataSeries.map(([label, token]) => <TokenSwatch key={token} label={label} token={token} />)}</div>
        <div className="lab-chart" role="img" aria-label="Gráfico demonstrativo com seis séries categóricas e valores de exemplo">
          <h3>Comparação demonstrativa</h3>
          <p>Valores fictícios para avaliar distinção entre cores.</p>
          <div className="lab-bars">{dataSeries.map(([label, token, value]) => <div className="lab-bar-row" key={token}><span>{label}</span><span className="lab-bar-track"><span className="lab-bar-fill" style={{ width: `${value}%`, backgroundColor: `var(${token})` }} /></span><strong>{value}</strong></div>)}</div>
          <div className="lab-chart-legend">{dataSeries.map(([label, token]) => <span key={token}><i style={{ backgroundColor: `var(${token})` }} />{label}</span>)}</div>
        </div>
      </section>
      <footer className="lab-footer">Tema ativo: {themeLabel} · Preferência e valores candidatos devem ser revistos antes de uso em produção.</footer>
    </main>
  );
}
