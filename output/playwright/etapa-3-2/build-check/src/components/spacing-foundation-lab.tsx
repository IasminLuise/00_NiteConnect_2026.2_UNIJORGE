"use client";

import { useState } from "react";

const spacingScale = [
  { token: "1", value: 4, role: "Ícone e texto" },
  { token: "2", value: 8, role: "Elementos próximos" },
  { token: "3", value: 12, role: "Campos e itens" },
  { token: "4", value: 16, role: "Gutter mobile" },
  { token: "6", value: 24, role: "Grupos e painéis" },
  { token: "8", value: 32, role: "Região mobile" },
  { token: "12", value: 48, role: "Região desktop" },
  { token: "16", value: 64, role: "Separação excepcional" },
  { token: "20", value: 80, role: "Uso raro" },
  { token: "24", value: 96, role: "Uso editorial" },
] as const;

const projects = [
  { name: "Oficina de robótica educacional", status: "Em andamento", owner: "Marina Costa", updated: "Hoje, 14:32" },
  { name: "Portal de acompanhamento de bolsas", status: "Em revisão", owner: "Lucas Almeida", updated: "Ontem, 17:08" },
  { name: "Mapeamento de laboratórios parceiros", status: "Planejado", owner: "Ana Ribeiro", updated: "24 out 2026" },
] as const;

type Density = "comfortable" | "compact";

export function SpacingFoundationLab() {
  const [density, setDensity] = useState<Density>("comfortable");

  return (
    <div className="spacing-lab-content space-y-8 md:space-y-12">
      <section className="spacing-lab-section space-y-4 md:space-y-6" aria-labelledby="spacing-scale-heading">
        <div className="spacing-lab-section-heading flex flex-wrap items-end justify-between gap-4">
          <div>
            <p className="lab-eyebrow">01 · ESCALA PRIMITIVA</p>
            <h2 id="spacing-scale-heading" className="font-display text-type-section-title">Ritmo de 4 px</h2>
          </div>
          <p className="max-w-prose text-type-metadata text-nite-text-muted">
            Utilities Tailwind padrão; os valores são exemplos de uso, não variáveis CSS novas.
          </p>
        </div>
        <div className="spacing-lab-scale-grid grid gap-4">
          {spacingScale.map(({ token, value, role }) => (
            <div className="spacing-lab-scale-item grid gap-2" key={token}>
              <div className="spacing-lab-scale-meta grid gap-1">
                <code className="font-mono text-type-technical">{`spacing-${token}`}</code>
                <span className="font-sans text-type-metadata">{value} px · {role}</span>
              </div>
              <span className="spacing-lab-scale-bar" style={{ width: `${value}px` }} aria-hidden="true" />
            </div>
          ))}
        </div>
      </section>

      <section className="spacing-lab-section space-y-4 md:space-y-6" aria-labelledby="spacing-container-heading">
        <div className="spacing-lab-section-heading flex flex-wrap items-end justify-between gap-4">
          <div>
            <p className="lab-eyebrow">02 · CONTAINER E GUTTERS</p>
            <h2 id="spacing-container-heading" className="font-display text-type-section-title">Área principal fluida</h2>
          </div>
          <p className="max-w-prose text-type-metadata text-nite-text-muted">Máximo 1280 px · gutters 16 / 24 / 32 px por largura disponível.</p>
        </div>
        <div className="spacing-lab-container-example">
          <div className="spacing-lab-container-inner grid gap-4 p-4 sm:p-6 lg:p-8">
            <span className="spacing-lab-gutter-marker" aria-hidden="true" />
            <div>
              <strong className="font-sans text-type-component-title">Região principal de conteúdo</strong>
              <p className="mt-2 text-type-body-compact text-nite-text-secondary">
                O limite não inclui uma eventual sidebar da aplicação. Uma região tabular pode ocupar largura adicional quando a comparação entre colunas exigir.
              </p>
            </div>
            <span className="spacing-lab-container-value font-mono text-type-technical">max-width: 80rem</span>
          </div>
        </div>
      </section>

      <section className="spacing-lab-section space-y-4 md:space-y-6" aria-labelledby="spacing-grid-heading">
        <div className="spacing-lab-section-heading flex flex-wrap items-end justify-between gap-4">
          <div>
            <p className="lab-eyebrow">03 · GRIDS E AGRUPAMENTOS</p>
            <h2 id="spacing-grid-heading" className="font-display text-type-section-title">Colunas que acompanham o conteúdo</h2>
          </div>
          <p className="max-w-prose text-type-metadata text-nite-text-muted">Painéis irmãos usam gap 16 px em mobile e 24 px em larguras maiores.</p>
        </div>
        <div className="spacing-lab-panel-grid grid gap-4 sm:gap-6">
          <article className="spacing-lab-panel rounded-xl border border-nite-border-soft bg-nite-surface p-4 sm:p-6">
            <p className="lab-eyebrow">AGRUPAMENTO A</p>
            <h3 className="font-sans text-type-component-title">Projetos em acompanhamento</h3>
            <p className="mt-2 text-type-body-compact text-nite-text-secondary">O grid redistribui os painéis sem impor larguras rígidas às colunas.</p>
          </article>
          <article className="spacing-lab-panel rounded-xl border border-nite-border-soft bg-nite-surface p-4 sm:p-6">
            <p className="lab-eyebrow">AGRUPAMENTO B</p>
            <h3 className="font-sans text-type-component-title">Atividades recentes</h3>
            <p className="mt-2 text-type-body-compact text-nite-text-secondary">Conteúdo extenso cresce em altura e mantém a leitura em uma única coluna quando necessário.</p>
          </article>
          <article className="spacing-lab-panel rounded-xl border border-nite-border-soft bg-nite-surface p-4 sm:p-6">
            <p className="lab-eyebrow">AGRUPAMENTO C</p>
            <h3 className="font-sans text-type-component-title">Entregas para revisão</h3>
            <p className="mt-2 text-type-body-compact text-nite-text-secondary">Separações de 48 px são reservadas a regiões principais, não a cada card do dashboard.</p>
          </article>
        </div>
      </section>

      <section className="spacing-lab-section space-y-4 md:space-y-6" aria-labelledby="spacing-hierarchy-heading">
        <div>
          <p className="lab-eyebrow">04 · HIERARQUIA VERTICAL</p>
          <h2 id="spacing-hierarchy-heading" className="font-display text-type-section-title">Proximidade antes de decoração</h2>
        </div>
        <article className="spacing-lab-hierarchy grid gap-3 rounded-xl border border-nite-border-soft bg-nite-surface p-4 sm:p-6">
          <p className="lab-eyebrow">TÍTULO → DESCRIÇÃO → CONTEÚDO</p>
          <h3 className="font-sans text-type-component-title">Acompanhamento de entregas</h3>
          <p className="spacing-lab-long-copy text-type-body text-nite-text-secondary">
            A descrição permanece próxima do título que contextualiza, enquanto a região de conteúdo recebe uma separação maior. Este texto demonstrativo é intencionalmente longo para mostrar quebra natural das linhas, expansão vertical sem altura fixa e manutenção do contexto em larguras reduzidas.
          </p>
          <div className="spacing-lab-hierarchy-content rounded-lg border border-nite-border-subtle bg-nite-background p-3 sm:p-4">
            <strong className="font-sans text-type-label">Próxima ação</strong>
            <p className="mt-1 text-type-body-compact text-nite-text-secondary">Revisar a documentação de acompanhamento e registrar a decisão no histórico do projeto.</p>
          </div>
        </article>
      </section>

      <section className="spacing-lab-section space-y-4 md:space-y-6" aria-labelledby="spacing-density-heading">
        <div className="spacing-lab-section-heading flex flex-wrap items-end justify-between gap-4">
          <div>
            <p className="lab-eyebrow">05 · DENSIDADE CONTEXTUAL</p>
            <h2 id="spacing-density-heading" className="font-display text-type-section-title">Tabela administrativa</h2>
          </div>
          <fieldset className="spacing-lab-density-control">
            <legend className="font-sans text-type-label">Espaçamento das linhas</legend>
            <div className="spacing-lab-density-buttons flex flex-wrap gap-2" role="group" aria-label="Densidade da tabela demonstrativa">
              <button
                className="spacing-lab-density-button font-sans text-type-label"
                type="button"
                aria-pressed={density === "comfortable"}
                onClick={() => setDensity("comfortable")}
              >
                Confortável
              </button>
              <button
                className="spacing-lab-density-button font-sans text-type-label"
                type="button"
                aria-pressed={density === "compact"}
                onClick={() => setDensity("compact")}
              >
                Compacta
              </button>
            </div>
          </fieldset>
        </div>
        <div className="spacing-lab-table-panel rounded-xl border border-nite-border-soft bg-nite-surface p-4 sm:p-6" data-density={density}>
          <div className="spacing-lab-filter-row flex flex-wrap items-end gap-3">
            <div className="spacing-lab-field grid gap-2">
              <label htmlFor="spacing-project-search" className="font-sans text-type-label">Buscar projeto</label>
              <input id="spacing-project-search" className="font-sans text-type-body" type="search" placeholder="Nome ou responsável" />
            </div>
            <div className="spacing-lab-field grid gap-2">
              <label htmlFor="spacing-project-status" className="font-sans text-type-label">Situação</label>
              <select id="spacing-project-status" className="font-sans text-type-body" defaultValue="all">
                <option value="all">Todas as situações</option>
                <option value="progress">Em andamento</option>
                <option value="review">Em revisão</option>
              </select>
            </div>
            <button className="spacing-lab-filter-action font-sans text-type-label" type="button">Filtros</button>
            <button className="spacing-lab-primary-action font-sans text-type-label" type="button">Exportar dados</button>
          </div>
          <p className="spacing-lab-scroll-hint mb-2 mt-4 text-type-metadata text-nite-text-muted" id="spacing-table-hint">
            Em larguras menores, a rolagem horizontal fica contida nesta tabela para preservar a comparação entre colunas.
          </p>
          <div className="spacing-lab-table-scroll" role="region" aria-label="Projetos demonstrativos; tabela com rolagem horizontal" aria-describedby="spacing-table-hint" tabIndex={0}>
            <table className="spacing-lab-table" data-density={density}>
              <thead>
                <tr>
                  <th className="font-sans text-type-table-header" scope="col">Projeto</th>
                  <th className="font-sans text-type-table-header" scope="col">Situação</th>
                  <th className="font-sans text-type-table-header" scope="col">Responsável</th>
                  <th className="font-sans text-type-table-header" scope="col">Atualizado</th>
                </tr>
              </thead>
              <tbody>
                {projects.map((project) => (
                  <tr key={project.name}>
                    <td className="font-sans text-type-table-cell">{project.name}</td>
                    <td className="font-sans text-type-table-cell">{project.status}</td>
                    <td className="font-sans text-type-table-cell">{project.owner}</td>
                    <td className="font-sans text-type-table-cell tabular-nums">{project.updated}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
        <p className="text-type-metadata text-nite-text-muted" aria-live="polite">
          A densidade altera apenas o padding vertical e horizontal das células; a escala tipográfica e a navegação por foco permanecem iguais.
        </p>
      </section>
    </div>
  );
}
