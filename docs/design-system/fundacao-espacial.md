# NITE Connect — fundação espacial

## Escopo

Esta fundação descreve o uso de espaçamento e densidade no Connect. Ela complementa as fundações cromática e tipográfica sem redefinir cores, famílias, tamanhos tipográficos ou geometria de componentes. A escala espacial primitiva vem do Tailwind CSS 4 instalado na aplicação; não foram criadas variáveis CSS paralelas para os mesmos valores.

## Escala primitiva

Tailwind define `--spacing: 0.25rem` (4px na escala base do navegador). As utilities existentes calculam seus valores como múltiplos dessa base. Use-as diretamente para padding, margin, gap e dimensões espaciais:

| Utility representativa | Valor | Uso recomendado |
|---|---:|---|
| `1` | 4px | Microespaço, por exemplo ícone e rótulo |
| `2` | 8px | Elementos diretamente relacionados |
| `3` | 12px | Distância curta entre campos ou itens |
| `4` | 16px | Padding compacto e gutter mobile |
| `5` | 20px | Passo intermediário quando 16/24 não se ajustam |
| `6` | 24px | Agrupamentos, painéis e gutter tablet |
| `8` | 32px | Gutter desktop e separação mobile entre regiões |
| `12` | 48px | Separação entre regiões principais em telas maiores |
| `16` | 64px | Separação excepcional entre fluxos distintos |
| `20` | 80px | Exceção rara em layouts operacionais |
| `24` | 96px | Uso editorial, evitar em dashboards |

Não adicionar tokens ou aliases customizados quando a utility Tailwind existente expressar a necessidade. Um nome semântico em componente ou documentação pode descrever um padrão sem criar outro valor de escala.

## Convenções de layout

| Papel | Convenção implementada no Lab | Aplicação |
|---|---|---|
| Largura máxima do conteúdo | `max-w-7xl` = 80rem / 1280px | Região principal de conteúdo, não inclui eventual sidebar da aplicação |
| Gutter mobile | `px-4` = 16px | Base até o breakpoint `sm` |
| Gutter tablet | `sm:px-6` = 24px | A partir de 640px |
| Gutter desktop | `lg:px-8` = 32px | A partir de 1024px |
| Gap entre painéis irmãos | `gap-4` base; `sm:gap-6` | 16px em mobile, 24px em larguras a partir de 640px |
| Separação de regiões principais | `space-y-8` base; `md:space-y-12` | 32px em mobile e 48px a partir de 768px |
| Gap de grupos | `space-y-4` base; `md:space-y-6` | 16px em mobile e 24px a partir de 768px |
| Campos relacionados | `gap-3` | 12px; label e controle ficam em uma pilha com `gap-2` = 8px |
| Padding confortável de painel | `p-4 sm:p-6` | 16px em mobile, 24px a partir de 640px |
| Padding compacto de painel tabular | 16px | Local à região explicitamente marcada como compacta |

As faixas `sm`, `md` e `lg` são os breakpoints padrão existentes do Tailwind (640, 768 e 1024 CSS px). Para grids e filtros, o reflow também é baseado na capacidade do conteúdo de caber: grids usam `auto-fit`/`minmax`, filtros usam flex-wrap e campos têm uma largura mínima compatível com seus rótulos e controles. Os breakpoints acima adaptam gutters e ritmo geral; não se deve adicionar breakpoint só para acomodar uma composição rígida.

Uma região de dados pode solicitar mais largura que o conteúdo principal quando a comparação de colunas exigir. Em mobile, essa rolagem permanece confinada ao contêiner tabular, focável por teclado e identificado para tecnologias assistivas; a página não deve ganhar overflow horizontal.

## Densidade operacional

### Confortável

É o estado padrão da aplicação e da tabela no Lab. Usar em dashboards, formulários, indicadores, revisão de entregas e qualquer operação de decisão. Linhas de tabela usam `px-4 py-3` (16px horizontal × 12px vertical). Painéis usam 24px em telas a partir de 640px e 16px em mobile.

### Compacta

Usar somente em listas/tabelas longas ou regiões informativas quando a maior quantidade de linhas visíveis ajudar a consulta. As células usam 12px horizontal × 8px vertical e o painel da tabela usa 16px. Não reduzir a tipografia quando a densidade muda: a tabela continua usando `text-type-table-cell`; a opção `text-type-table-cell-compact` permanece disponível apenas quando uma necessidade legível e validada justificar seu uso pontual.

Nenhuma preferência global de densidade existe. A região responsável declara sua variante local (`data-density="comfortable|compact"` no Lab). Não compactar altura de campos, botões, alvo de toque, foco, ou espaçamento exigido para compreender um grupo. Linhas e painéis crescem com textos longos, zoom e reflow; não fixar alturas para compensar conteúdo.

## Agrupamento e composição

- Manter próximos os itens que formam uma única ação ou ideia: label e campo, título e descrição, controle e resultado.
- Separar grupos relacionados com 16px em mobile e 24px em larguras maiores.
- Reservar 32px/48px para regiões principais, conforme tela; 48px não é gap padrão entre cards de um mesmo dashboard.
- Usar grids `auto-fit` com larguras mínimas baseadas no conteúdo, não colunas fixas que comprimam texto ou botões.
- Preferir alinhamento, espaços e separadores à criação de cartões aninhados ou wrappers puramente decorativos.
- Campos de filtro quebram para uma nova linha quando a largura não permite acomodá-los. Ações preservam largura intrínseca e texto em uma linha — por exemplo, “Filtros” não se divide em várias linhas.
- Não cortar títulos, descrições, nomes, labels ou estados por alturas fixas; permitir quebra e crescimento vertical.

## Design System Lab

A página interna `/dev/design-system/spacing` demonstra a escala, os gutters, o container, grids, grupos, hierarquia vertical, filtros e dados demonstrativos com textos longos. O controle **Confortável/Compacta** altera apenas o padding das células e o painel tabular. A tipografia permanece a mesma. O seletor compartilhado conserva os temas Light, Dark e System. A rota chama `notFound()` em produção, como as páginas de cores e tipografia.

## Validação requerida

Validar a rota em 320, 360, 768, 1024, 1280 e 1440 CSS px, em Light e Dark. Confirmar ausência de overflow no documento, reflow dos controles, rolagem confinada da tabela, texto longo, altura natural, foco de teclado e áreas de toque. Repetir com zoom de 200%/400% e espaçamento textual aumentado quando as ferramentas disponíveis permitirem. A nova rota não valida sozinha a aplicação inteira: aplicar estes padrões a interfaces reais exige validar cada composição no seu contexto.

### Resultados da Etapa 3.2

- Playwright executado em Light e Dark nas seis larguras requeridas. O documento manteve `scrollWidth == clientWidth` em todas as 12 combinações. Gutter computado: 16px em 320/360, 24px em 768 e 32px em 1024/1280/1440. A região principal ficou limitada a 1280px em viewport de 1440px.
- Os painéis refluíram em uma coluna nas larguras 320/360, duas colunas em 768 e três em 1024 ou mais. Os filtros mantiveram o botão “Filtros” em uma linha; os campos e ações reorganizaram-se sem overflow.
- A tabela rolou internamente em 320/360/768px e coube sem rolagem em 1024px ou mais. O restante da página não rolou horizontalmente. Campos de demonstração mediram 44px de altura.
- Densidade confortável: células `12px 16px`, painel 24px no desktop (16px em mobile). Densidade compacta: células `8px 12px`, painel 16px. A célula permaneceu em 14px nas duas densidades; o estado acessível do controle mudou para “Compacta”.
- Com `line-height: 1.5`, espaçamento de parágrafo `2em`, `letter-spacing: 0.12em` e `word-spacing: 0.16em`, os cenários 320/1440 em Light/Dark mantiveram reflow, inputs e seleção operáveis, sem corte do texto longo nem overflow do documento.
- Navegação por teclado: Tab alcançou o link “Cores” primeiro, com outline visível de 3px; seletor de tema habilitado e alternância Light/Dark efetivamente refletida no token de fundo.
- `npm run lint`, `npm run typecheck`, `npm run test` (4 arquivos, 14 testes) e `npm run build` passaram. Vitest ainda apresenta o aviso já existente sobre `configLoader: 'native'` no `vitest.config.ts`; sem falhas de teste.
- Em servidor de produção, `/` respondeu 200; `/dev/design-system/colors`, `/dev/design-system/typography` e `/dev/design-system/spacing` responderam 404.
- Zoom nativo de navegador em 200%/400% não foi medido: o controle Playwright utilizado permite configurar viewport, mas não oferece um comando de zoom nativo. Não se considera a validação em viewport estreita equivalente a zoom.

Capturas Playwright desta etapa ficam em `output/playwright/etapa-3-2/`: `spacing-dark-desktop.png`, `spacing-light-desktop.png`, `spacing-dark-mobile-360.png`, `spacing-light-mobile-360.png` e `spacing-density-compact-dark.png`. O script `validate-spacing.js` reproduz as medições de viewport, tema e densidade; `validate-text-spacing.js` reproduz os overrides de espaçamento textual.

## Implementação e limites

- Primitivos: escala Tailwind nativa, sem duplicar escala em tokens CSS.
- Convenções semânticas: expressas por utilities no container e nos agrupamentos; não foram adicionados aliases globais de layout.
- Variante compacta: restrita à demonstração tabular, sem preferência ou efeito global.
- Referência de implementação: `src/app/dev/design-system/spacing/page.tsx`, `src/components/spacing-foundation-lab.tsx`, `src/components/foundation-lab-navigation.tsx` e regras `spacing-lab-*` em `src/app/globals.css`.
- A composição do workspace/diário existente não foi redesenhada nesta etapa.
