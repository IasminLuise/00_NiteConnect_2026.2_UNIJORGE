# NITE Connect — fundação tipográfica

## Objetivo e escopo

Esta fundação define as famílias e os papéis tipográficos compartilhados pelo Connect. Ela complementa a fundação cromática sem alterar sua paleta, os temas ou os papéis de cor. Os valores concretos foram consolidados na Etapa 2.2 a partir da proposta tipográfica da Etapa 2.1 e dos refinamentos aprovados.

## Famílias e responsabilidades

| Família | Responsabilidade | Uso |
| --- | --- | --- |
| Sora | Voz institucional | Display, títulos de página e títulos de seção principais. Não usar em títulos de cards, menus ou painéis menores. |
| Geist | Interface e leitura | Corpo, navegação, labels operacionais, controles, tabelas, componentes e indicadores. É a família sans padrão. |
| Geist Mono | Identificadores técnicos | IDs, códigos, versões e conteúdo em que a largura monoespaçada tenha benefício de leitura ou cópia. |

Inter não faz parte da fundação do Connect. A exceção tipográfica que existe em algumas páginas do Portal não justifica uma quarta família nesta aplicação.

## Carregamento

As três famílias são carregadas por `next/font/google` no layout raiz, em subset `latin`, com `display: "swap"` e variáveis CSS (`--font-sora`, `--font-geist`, `--font-geist-mono`). O Next inclui os arquivos de fonte no build e os serve pela própria aplicação; o navegador não depende de uma fonte instalada no computador nem solicita os arquivos ao Google durante a navegação.

Geist é aplicado ao texto geral. Sora e Geist Mono são expostos como aliases Tailwind `font-display` e `font-mono`. Os fallbacks seguem os do Portal: Bahnschrift/Segoe UI para sans e Cascadia Mono/Consolas para mono. O fallback métrico gerado pelo Next reduz variação de métricas durante `font-display: swap`; o impacto real em CLS deve ser medido na validação de produção.

## Tokens semânticos

Os tokens são publicados em `@theme inline` no CSS global, combinando tamanho, line-height, peso e letter-spacing no mesmo utilitário `text-type-*`. As variáveis responsivas residem em `:root` e mudam a partir de 640px; os estilos independem do tema de cor.

| Token/utilitário | Família | Desktop | Mobile | Uso |
| --- | --- | --- | --- | --- |
| `text-type-display` | Sora | 40px / 1.10 / 600 / −0.02em | 32px / 1.125 / 600 / −0.02em | Autenticação ou abertura excepcional |
| `text-type-page-title` | Sora | 28px / 1.21 / 600 / −0.015em | 24px / 1.25 / 600 / −0.015em | Título principal da rota |
| `text-type-section-title` | Sora | 20px / 1.30 / 600 / −0.01em | 18px / 1.33 / 600 / −0.01em | Seções principais |
| `text-type-component-title` | Geist | 16px / 1.375 / 600 / 0 | Igual | Títulos de cards, menus e painéis |
| `text-type-body` | Geist | 16px / 1.50 / 400 / 0 | Igual | Leitura e conteúdo explicativo |
| `text-type-body-compact` | Geist | 14px / 1.43 / 400 / 0 | Igual | Conteúdo secundário e denso |
| `text-type-label` | Geist | 14px / 1.38 / 500 / 0 | Igual | Labels operacionais |
| `text-type-table-header` | Geist | 12px / 1.33 / 600 / 0 | Igual | Cabeçalhos de tabela concisos |
| `text-type-table-cell` | Geist | 14px / 1.43 / 400 / 0 | Igual | Células padrão |
| `text-type-table-cell-compact` | Geist | 13px / 1.38 / 400 / 0 | Igual | Apenas quando uma densidade compacta for justificada |
| `text-type-metadata` | Geist | 12px / 1.33 / 400 / 0 | Igual | Metadados auxiliares |
| `text-type-metric` | Geist | 32px / 1.19 / 600 / 0 | 28px / 1.21 / 600 / 0 | Indicadores numéricos |
| `text-type-technical` | Geist Mono | 12px / 1.33 / 400 / 0 | Igual | Identificadores e dados técnicos |

Os fallbacks sans e mono são declarados nos aliases `--font-sans`, `--font-display` e `--font-mono`. O alias sans usa Geist; display usa Sora; mono usa Geist Mono.

## Densidade, numerais e conteúdo técnico

- A leitura convencional usa 16px; a interface secundária usa 14px. Rótulos operacionais também permanecem em 14px.
- Células usam 14px. A variante 13px existe somente para tabelas cuja densidade a justifique; não reduzir tabelas indiscriminadamente.
- Metadados de 12px são auxiliares, com contraste da fundação cromática e comprimento controlado. Não usar 9–10px como padrão.
- Valores numéricos comparáveis usam Geist com `tabular-nums` e alinhamento à direita quando apropriado. Geist Mono não é um estilo geral para números.
- Atributos e conteúdo técnico copiável podem usar `text-type-technical`; descrições longas nunca devem ser forçadas a uma única linha.
- Campos digitáveis usam pelo menos 16px no mobile para evitar zoom automático do navegador.

## Temas, responsividade e acessibilidade

Os mesmos tokens tipográficos são válidos em Light, Dark e System. O contraste é responsabilidade dos papéis de cor já existentes; esta fundação não redefine cores. A rota de tipografia do Design System Lab oferece o controle global dos temas para verificar as combinações.

Títulos e textos longos devem quebrar naturalmente. Tabelas largas ficam em uma região com rolagem horizontal própria, preservando o restante da página. Usar zoom do navegador sem restrição e validar 320 CSS px, 200% e 400% de zoom, espaçamento de texto ampliado, navegação por teclado e leitor de tela. Esta documentação não substitui essa validação em telas de produto.

## Exemplos

```tsx
<h1 className="font-display text-type-page-title">Gestão do núcleo</h1>
<h2 className="font-display text-type-section-title">Projetos em acompanhamento</h2>
<p className="font-sans text-type-body">Acompanhe as atividades do núcleo.</p>
<label className="font-sans text-type-label">Nome do projeto</label>
<strong className="font-sans text-type-metric tabular-nums">18.204</strong>
<code className="font-mono text-type-technical">proj_01J9Q7NITE</code>
```

Preferir os utilitários de papéis em vez de compor tamanho, peso e line-height localmente. Use Sora apenas nos níveis principais; títulos de componentes continuam em Geist.

## Laboratório de validação

- `/dev/design-system/colors` mantém a inspeção cromática existente.
- `/dev/design-system/typography` demonstra os tokens, famílias, hierarquia, navegação, formulário, tabela, numerais tabulares e reflow.
- As duas rotas chamam `notFound()` quando `NODE_ENV` é `production`; não são incluídas na navegação de produto.
- A página tipográfica usa os estilos e fontes reais da aplicação. O seletor já existente alterna Light, Dark e System.

## Validação e pendências

Validação realizada nesta etapa:

- `npm run lint`, `npm run typecheck` e `npm run test` passaram; os testes cobrem três arquivos e 12 casos. O Vitest exibiu um aviso preexistente sobre `configLoader: 'native'` no `vitest.config.ts`, sem falhas.
- A prancha foi inspecionada no Browser nativo em Light e Dark. A hierarquia e as famílias reais apareceram corretamente em desktop (1440 × 900); também foram inspecionadas larguras de tablet (768 × 1024), mobile (390 × 844) e estreita (320 × 700), incluindo quebras de títulos e reflow do texto.
- A árvore de acessibilidade expôs títulos, campos, tabela e navegação; não houve erros ou avisos no console durante a inspeção.
- `npm run build` passou em uma cópia isolada com Next.js 16.4.3/Turbopack. No servidor de produção, `/` respondeu 200 e as rotas `/dev/design-system/colors` e `/dev/design-system/typography` responderam 404.

Não foram verificados zoom do navegador em 200%/400%, espaçamento de texto ampliado, medição de CLS em produção, nem navegação completa com teclado e leitor de tela. O Browser permitiu inspecionar capturas durante a validação, mas não exportá-las como arquivos de imagem nesta etapa. O fallback métrico do Next reduz variações entre fontes, porém não substitui a medição real de CLS.
