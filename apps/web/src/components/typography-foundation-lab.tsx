const typeTokens = [
  {
    name: "display",
    family: "Sora",
    familyClass: "font-display",
    sizeClass: "text-type-display",
    desktop: "40px · 600 · 1.10 · −0.02em",
    mobile: "32px · 600 · 1.125 · −0.02em",
    sample: "Acompanhe o NITE",
  },
  {
    name: "page-title",
    family: "Sora",
    familyClass: "font-display",
    sizeClass: "text-type-page-title",
    desktop: "28px · 600 · 1.21 · −0.015em",
    mobile: "24px · 600 · 1.25 · −0.015em",
    sample: "Gestão do núcleo",
  },
  {
    name: "section-title",
    family: "Sora",
    familyClass: "font-display",
    sizeClass: "text-type-section-title",
    desktop: "20px · 600 · 1.30 · −0.01em",
    mobile: "18px · 600 · 1.33 · −0.01em",
    sample: "Projetos em acompanhamento",
  },
  {
    name: "component-title",
    family: "Geist",
    familyClass: "font-sans",
    sizeClass: "text-type-component-title",
    desktop: "16px · 600 · 1.375 · 0",
    mobile: "16px · 600 · 1.375 · 0",
    sample: "Atividades recentes",
  },
  {
    name: "body",
    family: "Geist",
    familyClass: "font-sans",
    sizeClass: "text-type-body",
    desktop: "16px · 400 · 1.50 · 0",
    mobile: "16px · 400 · 1.50 · 0",
    sample: "Consulte as atualizações e acompanhe o andamento das atividades do núcleo.",
  },
  {
    name: "body-compact",
    family: "Geist",
    familyClass: "font-sans",
    sizeClass: "text-type-body-compact",
    desktop: "14px · 400 · 1.43 · 0",
    mobile: "14px · 400 · 1.43 · 0",
    sample: "Atualizado por Marina Costa · hoje às 14:32",
  },
  {
    name: "label",
    family: "Geist",
    familyClass: "font-sans",
    sizeClass: "text-type-label",
    desktop: "14px · 500 · 1.38 · 0",
    mobile: "14px · 500 · 1.38 · 0",
    sample: "Nome do projeto",
  },
  {
    name: "table-header",
    family: "Geist",
    familyClass: "font-sans",
    sizeClass: "text-type-table-header",
    desktop: "12px · 600 · 1.33 · 0",
    mobile: "12px · 600 · 1.33 · 0",
    sample: "ÚLTIMA ATUALIZAÇÃO",
  },
  {
    name: "table-cell",
    family: "Geist",
    familyClass: "font-sans",
    sizeClass: "text-type-table-cell",
    desktop: "14px · 400 · 1.43 · 0",
    mobile: "14px · 400 · 1.43 · 0",
    sample: "Oficina de robótica",
  },
  {
    name: "table-cell-compact",
    family: "Geist",
    familyClass: "font-sans",
    sizeClass: "text-type-table-cell-compact",
    desktop: "13px · 400 · 1.38 · 0",
    mobile: "13px · 400 · 1.38 · 0",
    sample: "Variante compacta justificada",
  },
  {
    name: "metadata",
    family: "Geist",
    familyClass: "font-sans",
    sizeClass: "text-type-metadata",
    desktop: "12px · 400 · 1.33 · 0",
    mobile: "12px · 400 · 1.33 · 0",
    sample: "Criado em 12 out 2026",
  },
  {
    name: "metric",
    family: "Geist",
    familyClass: "font-sans tabular-nums",
    sizeClass: "text-type-metric",
    desktop: "32px · 600 · 1.19 · 0",
    mobile: "28px · 600 · 1.21 · 0",
    sample: "18.204",
  },
  {
    name: "technical",
    family: "Geist Mono",
    familyClass: "font-mono",
    sizeClass: "text-type-technical",
    desktop: "12px · 400 · 1.33 · 0",
    mobile: "12px · 400 · 1.33 · 0",
    sample: "proj_01J9Q7NITE",
  },
] as const;

const fontFamilies = [
  {
    name: "Sora",
    role: "Títulos principais",
    familyClass: "font-display",
    sample: "Gestão do núcleo",
  },
  {
    name: "Geist",
    role: "Interface e leitura",
    familyClass: "font-sans",
    sample: "Projetos, filtros e atividades",
  },
  {
    name: "Geist Mono",
    role: "Dados técnicos",
    familyClass: "font-mono",
    sample: "proj_01J9Q7NITE",
  },
] as const;

export function TypographyFoundationLab() {
  return (
    <div className="typography-lab-content">
      <section className="typography-lab-section" aria-labelledby="font-families-heading">
        <div className="typography-lab-section-heading">
          <div>
            <p className="typography-lab-eyebrow">01 · FAMÍLIAS</p>
            <h2 id="font-families-heading" className="font-display text-type-section-title">Papéis das fontes</h2>
          </div>
          <p className="text-type-metadata">As fontes abaixo são carregadas pelo Next.js e aplicadas pelos tokens globais.</p>
        </div>
        <div className="typography-family-grid">
          {fontFamilies.map((family) => (
            <article className="typography-family-card" key={family.name}>
              <div className="typography-family-meta">
                <h3 className="font-sans text-type-label">{family.name}</h3>
                <span className="text-type-metadata">{family.role}</span>
              </div>
              <p className={`${family.familyClass} text-type-component-title`}>{family.sample}</p>
            </article>
          ))}
        </div>
      </section>

      <section className="typography-lab-section" aria-labelledby="type-tokens-heading">
        <div className="typography-lab-section-heading">
          <div>
            <p className="typography-lab-eyebrow">02 · ESCALA SEMÂNTICA</p>
            <h2 id="type-tokens-heading" className="font-display text-type-section-title">Tokens tipográficos</h2>
          </div>
          <p className="text-type-metadata">O exemplo de cada papel usa o token CSS correspondente.</p>
        </div>
        <div className="typography-token-list">
          {typeTokens.map((token) => (
            <article className="typography-token-card" data-type-token={token.name} key={token.name}>
              <div className="typography-token-properties">
                <code className="font-mono text-type-technical">{token.name}</code>
                <span>{token.family}</span>
                <span>{token.desktop}</span>
                <span className="typography-token-mobile">Mobile · {token.mobile}</span>
              </div>
              <p className={`${token.familyClass} ${token.sizeClass}`}>{token.sample}</p>
            </article>
          ))}
        </div>
      </section>

      <section className="typography-lab-section" aria-labelledby="type-interface-heading">
        <div className="typography-lab-section-heading">
          <div>
            <p className="typography-lab-eyebrow">03 · USO OPERACIONAL</p>
            <h2 id="type-interface-heading" className="font-display text-type-section-title">Navegação, formulário e tabela</h2>
          </div>
          <p className="text-type-metadata">Exemplos demonstrativos usando a escala proposta.</p>
        </div>
        <div className="typography-example-grid">
          <nav className="typography-example-card typography-sample-nav" aria-label="Exemplo de navegação do Connect">
            <span className="typography-example-title font-sans text-type-label">NITE Connect</span>
            <a className="font-sans text-type-label" href="#overview" aria-current="page">Visão geral</a>
            <a className="font-sans text-type-label" href="#projects">Projetos</a>
            <a className="font-sans text-type-label" href="#members">Integrantes</a>
          </nav>

          <form className="typography-example-card typography-sample-form" aria-label="Exemplo de filtro de projetos">
            <h3 className="font-sans text-type-component-title">Filtrar projetos</h3>
            <label htmlFor="type-project-name" className="text-type-label">Nome do projeto</label>
            <input className="font-sans text-type-body" id="type-project-name" name="project" placeholder="Ex.: Oficina de robótica" />
            <label htmlFor="type-project-status" className="text-type-label">Situação</label>
            <select className="font-sans text-type-body" id="type-project-status" name="status" defaultValue="all">
              <option value="all">Todas as situações</option>
              <option value="progress">Em andamento</option>
            </select>
            <p className="text-type-metadata">Filtros aplicados apenas nesta demonstração.</p>
          </form>

          <section className="typography-example-card typography-sample-table" aria-labelledby="type-table-heading">
            <div className="typography-table-heading">
              <h3 id="type-table-heading" className="font-sans text-type-component-title">Projetos atualizados</h3>
              <div className="typography-metrics">
                <span className="text-type-metric tabular-nums">18</span>
                <span className="text-type-metadata">ativos</span>
              </div>
            </div>
            <div className="typography-table-scroll" role="region" aria-label="Projetos atualizados; tabela demonstrativa" tabIndex={0}>
              <table>
                <thead>
                  <tr>
                    <th className="text-type-table-header" scope="col">Projeto</th>
                    <th className="text-type-table-header" scope="col">Situação</th>
                    <th className="text-type-table-header typography-numeric" scope="col">Conclusão</th>
                  </tr>
                </thead>
                <tbody>
                  <tr>
                    <td className="text-type-table-cell">Oficina de robótica</td>
                    <td className="text-type-table-cell">Em andamento</td>
                    <td className="text-type-table-cell typography-numeric tabular-nums">78%</td>
                  </tr>
                  <tr>
                    <td className="text-type-table-cell">Portal de inscrições</td>
                    <td className="text-type-table-cell">Em revisão</td>
                    <td className="text-type-table-cell typography-numeric tabular-nums">100%</td>
                  </tr>
                  <tr>
                    <td className="text-type-table-cell-compact">Relatório de extensão</td>
                    <td className="text-type-table-cell-compact">Planejado · compacto</td>
                    <td className="text-type-table-cell-compact typography-numeric tabular-nums">24%</td>
                  </tr>
                </tbody>
              </table>
            </div>
            <p className="text-type-technical typography-table-note">ID da consulta · qry_2026_10_26_014</p>
          </section>
        </div>
      </section>

      <section className="typography-lab-section" aria-labelledby="type-reflow-heading">
        <div className="typography-lab-section-heading">
          <div>
            <p className="typography-lab-eyebrow">04 · RESPONSIVIDADE</p>
            <h2 id="type-reflow-heading" className="font-display text-type-section-title">Reflow em telas estreitas</h2>
          </div>
          <p className="text-type-metadata">Texto longo quebra sem cortar; tabelas mantêm rolagem contida.</p>
        </div>
        <article className="typography-reflow-card">
          <h3 className="font-display text-type-page-title">Acompanhamento de projetos e entregas</h3>
          <p className="text-type-body">
            Quando uma atividade depende de validação ou de uma atualização de outra equipe, a descrição deve continuar legível em telas estreitas, com quebra natural das linhas e sem esconder o contexto necessário para a tomada de decisão.
          </p>
          <p className="text-type-technical">integrante_2026_01 · atualizado em 26/10/2026</p>
        </article>
      </section>
    </div>
  );
}
