"use client";

import { useRef, useState } from "react";

const radiusTokens = [
  { token: "rounded-subtle", value: "4px", role: "Indicadores compactos" },
  { token: "rounded-control", value: "8px", role: "Controles e menus" },
  { token: "rounded-panel", value: "12px", role: "Painéis e agrupamentos" },
  { token: "rounded-dialog", value: "16px", role: "Diálogos" },
  { token: "rounded-pill", value: "9999px", role: "Badges e chips" },
] as const;

export function GeometryFoundationLab() {
  const [selected, setSelected] = useState(false);
  const [withShadow, setWithShadow] = useState(true);
  const [menuOpen, setMenuOpen] = useState(false);
  const [menuMessage, setMenuMessage] = useState("Nenhuma ação escolhida.");
  const dialogRef = useRef<HTMLDialogElement>(null);
  const menuTriggerRef = useRef<HTMLButtonElement>(null);

  function closeMenu() {
    setMenuOpen(false);
    menuTriggerRef.current?.focus();
  }

  return (
    <div className="geometry-lab-content space-y-8 md:space-y-12">
      <section className="geometry-lab-section space-y-4 md:space-y-6" aria-labelledby="geometry-radius-heading">
        <div className="flex flex-wrap items-end justify-between gap-4">
          <div>
            <p className="lab-eyebrow">01 · ESCALA DE RAIOS</p>
            <h2 id="geometry-radius-heading" className="font-display text-type-section-title">Papéis semânticos</h2>
          </div>
          <p className="max-w-prose text-type-metadata text-nite-text-muted">Utilities geradas por tokens semânticos na escala Tailwind existente.</p>
        </div>
        <div className="geometry-radius-grid grid gap-3">
          {radiusTokens.map(({ token, value, role }) => (
            <article className="geometry-radius-sample rounded-panel border border-nite-border-soft bg-nite-surface p-4" key={token}>
              <div className={`geometry-radius-shape ${token}`} aria-hidden="true" />
              <code className="font-mono text-type-technical">{token}</code>
              <strong className="font-sans text-type-label">{value}</strong>
              <span className="text-type-metadata text-nite-text-secondary">{role}</span>
            </article>
          ))}
        </div>
      </section>

      <section className="geometry-lab-section space-y-4 md:space-y-6" aria-labelledby="geometry-controls-heading">
        <div>
          <p className="lab-eyebrow">02 · CONTROLES E ESTADOS</p>
          <h2 id="geometry-controls-heading" className="font-display text-type-section-title">Contorno e interação</h2>
        </div>
        <div className="geometry-panel rounded-panel border border-nite-border-soft bg-nite-surface p-4 sm:p-6">
          <p className="text-type-body-compact text-nite-text-secondary">Campos e ações usam borda de 1px e raio de 8px. Use Tab para verificar o anel de foco; passe o ponteiro sobre ações para conferir hover.</p>
          <div className="geometry-control-row mt-4 flex flex-wrap items-center gap-3">
            <button className="geometry-demo-control geometry-demo-primary font-sans text-type-label" type="button">Salvar alterações</button>
            <button className="geometry-demo-control font-sans text-type-label" type="button">Ação secundária</button>
            <button
              className="geometry-demo-control geometry-demo-toggle font-sans text-type-label"
              type="button"
              aria-pressed={selected}
              onClick={() => setSelected((current) => !current)}
            >
              {selected ? "Selecionado" : "Selecionar"}
              {selected ? <span aria-hidden="true"> ✓</span> : null}
            </button>
            <button className="geometry-demo-control font-sans text-type-label" type="button" disabled>Desabilitado</button>
          </div>
          <div className="geometry-field-grid mt-4 grid gap-4 sm:grid-cols-2">
            <label className="geometry-demo-field grid gap-2 font-sans text-type-label" htmlFor="geometry-project-name">
              Nome do projeto
              <input id="geometry-project-name" className="font-sans text-type-body" defaultValue="Acompanhamento do núcleo" />
            </label>
            <label className="geometry-demo-field grid gap-2 font-sans text-type-label" htmlFor="geometry-project-state">
              Situação
              <select id="geometry-project-state" className="font-sans text-type-body" defaultValue="review">
                <option value="progress">Em andamento</option>
                <option value="review">Aguardando revisão</option>
                <option value="done">Concluído</option>
              </select>
            </label>
          </div>
          <p className="mt-3 text-type-metadata text-nite-text-muted">Altura mínima dos exemplos: 44px para manter área de toque. Estados disabled mantêm o rótulo legível.</p>
        </div>
      </section>

      <section className="geometry-lab-section space-y-4 md:space-y-6" aria-labelledby="geometry-elevation-heading">
        <div className="flex flex-wrap items-end justify-between gap-4">
          <div>
            <p className="lab-eyebrow">03 · NÍVEIS 0–2</p>
            <h2 id="geometry-elevation-heading" className="font-display text-type-section-title">Superfície e elevação</h2>
          </div>
          <p className="max-w-prose text-type-metadata text-nite-text-muted">Cards de conteúdo não têm sombra permanente; elevação é reservada a camadas flutuantes.</p>
        </div>
        <div className="geometry-elevation-grid grid gap-4 lg:grid-cols-3">
          <article className="geometry-level geometry-level-base rounded-panel border border-nite-border-soft p-4 sm:p-5">
            <p className="lab-eyebrow">NÍVEL 0 · BASE</p>
            <h3 className="font-sans text-type-component-title">Canvas e navegação</h3>
            <p className="mt-2 text-type-body-compact text-nite-text-secondary">Fundo e separação espacial, sem sombra.</p>
          </article>
          <article className="geometry-level geometry-level-content rounded-panel border border-nite-border-soft bg-nite-surface p-4 sm:p-5">
            <p className="lab-eyebrow">NÍVEL 1 · CONTEÚDO</p>
            <h3 className="font-sans text-type-component-title">Painel administrativo</h3>
            <p className="mt-2 text-type-body-compact text-nite-text-secondary">Surface e borda. Sem elevação permanente.</p>
          </article>
          <article className="geometry-level geometry-level-floating rounded-panel border border-nite-border-strong bg-nite-surface-elevated p-4 sm:p-5" data-shadow={withShadow ? "on" : "off"}>
            <p className="lab-eyebrow">NÍVEL 2 · FLUTUANTE</p>
            <h3 className="font-sans text-type-component-title">Superfície elevada</h3>
            <p className="mt-2 text-type-body-compact text-nite-text-secondary">Token cromático real com contorno e sombra candidata.</p>
            <button
              className="geometry-shadow-toggle mt-4 font-sans text-type-label"
              type="button"
              aria-pressed={withShadow}
              onClick={() => setWithShadow((current) => !current)}
            >
              Sombra {withShadow ? "ativada" : "desativada"}
            </button>
          </article>
        </div>
        <div className="geometry-elevated-comparison grid gap-4 sm:grid-cols-2" aria-label="Comparação da superfície elevada sem e com sombra">
          <article className="geometry-elevation-compare rounded-panel border border-nite-border-strong bg-nite-surface-elevated p-4 sm:p-5">
            <p className="lab-eyebrow">SEM SOMBRA</p>
            <h3 className="font-sans text-type-component-title">Superfície + contorno</h3>
            <code className="mt-2 block font-mono text-type-technical text-nite-text-secondary">--nite-surface-elevated</code>
          </article>
          <article className="geometry-elevation-compare geometry-elevation-shadow rounded-panel border border-nite-border-strong bg-nite-surface-elevated p-4 sm:p-5" data-shadow={withShadow ? "on" : "off"}>
            <p className="lab-eyebrow">COM SOMBRA</p>
            <h3 className="font-sans text-type-component-title">Surface + contorno + lift</h3>
            <code className="mt-2 block font-mono text-type-technical text-nite-text-secondary">shadow-nite-floating</code>
          </article>
        </div>
        <p className="text-type-metadata text-nite-text-muted">No Dark, a surface elevada usa #20242D. O controle alterna a sombra real do token sem modificar a cor nem a borda.</p>
      </section>

      <section className="geometry-lab-section space-y-4 md:space-y-6" aria-labelledby="geometry-overlay-heading">
        <div>
          <p className="lab-eyebrow">04 · CAMADAS SOBREPOSTAS</p>
          <h2 id="geometry-overlay-heading" className="font-display text-type-section-title">Popover e diálogo demonstrativos</h2>
        </div>
        <div className="geometry-overlay-grid grid gap-4 lg:grid-cols-2">
          <article className="geometry-overlay-panel rounded-panel border border-nite-border-soft bg-nite-surface p-4 sm:p-5">
            <h3 className="font-sans text-type-component-title">Menu contextual</h3>
            <p className="mt-1 text-type-body-compact text-nite-text-secondary">Disclosure acessível com ações navegáveis por Tab.</p>
            <div className="geometry-popover-stage mt-4 rounded-control border border-nite-border-soft bg-nite-background p-4">
              <div className="geometry-popover-context rounded-panel border border-nite-border-soft bg-nite-surface p-4">
                <strong className="font-sans text-type-label">Projetos em acompanhamento</strong>
                <p className="mt-1 text-type-metadata text-nite-text-secondary">Tabela e filtros permanecem no fluxo.</p>
              </div>
              <div className="geometry-popover-anchor">
                <button
                  className="geometry-demo-control font-sans text-type-label"
                  type="button"
                  aria-expanded={menuOpen}
                  aria-controls="geometry-demo-popover"
                  onClick={() => setMenuOpen((current) => !current)}
                  onKeyDown={(event) => {
                    if (event.key === "Escape" && menuOpen) closeMenu();
                  }}
                  ref={menuTriggerRef}
                >
                  Ações do projeto
                </button>
                <div
                  className="geometry-popover rounded-control border border-nite-border-strong bg-nite-surface-elevated p-2 shadow-nite-floating"
                  id="geometry-demo-popover"
                  hidden={!menuOpen}
                  role="group"
                  aria-label="Ações do projeto"
                  onKeyDown={(event) => {
                    if (event.key === "Escape") closeMenu();
                  }}
                >
                  {(["Abrir projeto", "Revisar entrega", "Arquivar"] as const).map((action) => (
                    <button
                      className="geometry-popover-action rounded-subtle font-sans text-type-label"
                      type="button"
                      key={action}
                      onClick={() => {
                        setMenuMessage(`Ação escolhida: ${action}.`);
                        closeMenu();
                      }}
                    >
                      {action}
                    </button>
                  ))}
                </div>
              </div>
            </div>
            <p className="mt-3 text-type-metadata text-nite-text-muted" aria-live="polite">{menuMessage}</p>
          </article>

          <article className="geometry-overlay-panel rounded-panel border border-nite-border-soft bg-nite-surface p-4 sm:p-5">
            <h3 className="font-sans text-type-component-title">Diálogo modal</h3>
            <p className="mt-1 text-type-body-compact text-nite-text-secondary">Elemento nativo dialog com backdrop do tema, Escape e fechamento acessíveis.</p>
            <button
              className="geometry-demo-control geometry-demo-primary mt-4 font-sans text-type-label"
              type="button"
              onClick={() => dialogRef.current?.showModal()}
            >
              Abrir diálogo demonstrativo
            </button>
            <dialog className="geometry-demo-dialog rounded-dialog border border-nite-border-strong bg-nite-surface-elevated p-5 shadow-nite-modal" ref={dialogRef} aria-labelledby="geometry-dialog-title" aria-describedby="geometry-dialog-description">
              <h3 id="geometry-dialog-title" className="font-sans text-type-component-title">Confirmar alteração</h3>
              <p id="geometry-dialog-description" className="mt-2 text-type-body-compact text-nite-text-secondary">Esta amostra valida a camada modal. Nenhum dado é salvo.</p>
              <div className="mt-5 flex flex-wrap justify-end gap-2">
                <button className="geometry-demo-control font-sans text-type-label" type="button" onClick={() => dialogRef.current?.close()}>Voltar</button>
                <button className="geometry-demo-control geometry-demo-primary font-sans text-type-label" type="button" onClick={() => dialogRef.current?.close()}>Entendi</button>
              </div>
            </dialog>
          </article>
        </div>
      </section>
    </div>
  );
}
