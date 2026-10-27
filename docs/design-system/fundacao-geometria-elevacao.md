# NITE Connect — fundação de geometria e elevação

## Escopo

Esta fundação complementa os sistemas cromático, tipográfico e espacial do Connect. Ela define os papéis de raio e a distinção visual entre conteúdo em fluxo e camadas sobrepostas. Não cria APIs definitivas para componentes nem altera valores cromáticos existentes.

## Geometria

Os papéis são publicados no namespace de radius do Tailwind CSS 4 em `apps/web/src/app/globals.css`. Assim, as utilities `rounded-subtle`, `rounded-control`, `rounded-panel`, `rounded-dialog` e `rounded-pill` são geradas pela configuração CSS existente; não se mantém uma segunda escala em variáveis semânticas paralelas.

| Utility | Valor | Uso |
|---|---:|---|
| `rounded-subtle` | 4px | detalhe compacto e controle interno de popover |
| `rounded-control` | 8px | botão, campo, seletor e menu |
| `rounded-panel` | 12px | card, painel e agrupamento |
| `rounded-dialog` | 16px | superfície modal |
| `rounded-pill` | 9999px | badge ou chip |

Controles demonstrativos usam altura mínima de 44px; itens informativos compactos podem manter alturas menores. O CSS não fixa a altura de painéis nem corta conteúdo. Bordas padrão usam 1px com os tokens cromáticos já existentes. Foco usa outline sólida de 3px com offset de 2px e `--nite-focus`. Hover, seleção, foco e disabled têm sinais distintos; a seleção combina superfície tonal e marcador interno, sem depender apenas da cor.

## Elevação

| Nível | Papel | Tratamento |
|---|---|---|
| 0 — Base | página e navegação estrutural | canvas e separação espacial, sem sombra |
| 1 — Conteúdo | cards e painéis | surface e borda, sem sombra permanente |
| 2 — Flutuante | menus, dropdowns e popovers | surface elevada, borda e `shadow-nite-floating` |
| 3 — Modal | diálogos | surface elevada, backdrop nativo e `shadow-nite-modal` |

Os tokens semânticos de sombra usam a namespace shadow do Tailwind e resolvem para variáveis de tema: Light `0 8px 24px rgb(15 23 42 / .14)` e `0 20px 56px rgb(15 23 42 / .20)`; Dark `0 8px 24px rgb(0 0 0 / .36)` e `0 20px 56px rgb(0 0 0 / .48)`. São valores de elevação funcional; não aplicar aos cards comuns.

O popover de demonstração usa disclosure com `aria-expanded` e ações nativas focáveis em sequência. Escape fecha a região e devolve o foco ao acionador. Não usa `role="menu"`, pois não implementa o padrão de teclado com setas esperado por um menu ARIA.

O diálogo usa o elemento nativo `<dialog>` em `showModal()`, que fornece modalidade, backdrop e comportamento nativo de Escape. Os botões de fechamento chamam `close()`. Esta demonstração não substitui validação de um componente de produção com foco, ciclos de abertura/fechamento e conteúdo real.

A ordem demonstrada é conteúdo-base < popover < dialog top-layer. Não há escala global de z-index: o popover usa uma camada local dentro do estágio; o diálogo modal nativo é promovido à top layer do navegador. O editor imersivo existente (`z-index: 20`) continua sendo uma implementação local fora desta nova convenção.

## Superfície elevada escura

`--nite-dark-surface-elevated: #20242D` foi preservada sem alteração. O Lab compara a surface com o contorno em dois exemplos, sem sombra e com `shadow-nite-floating`, usando o token real por tema. A comparação permite avaliação visual direta; ainda não declara a cor aprovada. A documentação cromática anterior registra separação tonal aproximada de 1,14:1 ante a superfície escura base, portanto a cor sozinha não deve sustentar a hierarquia.

## Design System Lab

A rota interna `/dev/design-system/geometry` demonstra raios, controles, estados, níveis de conteúdo/flutuante, comparação A/B de superfície, popover e diálogo nativo. Ela utiliza `FoundationLabNavigation`, `ThemeControl` e tokens Tailwind reais. Assim como as outras páginas do Lab, chama `notFound()` em `NODE_ENV=production`.

O controle de sombra altera somente a amostra flutuante. A alternância do tema segue o seletor Light/Dark/System compartilhado. O componente não persiste dados nem representa operações reais do produto.

## Acessibilidade e limites

- Controles do Lab têm pelo menos 44px, foco explícito e labels associados.
- Elementos disabled permanecem legíveis e indisponíveis ao teclado.
- Popover é disclosure comum com botões; Escape fecha e retorna foco ao acionador.
- O diálogo é um modal demonstrativo nativo, com título, descrição, fechamento por Escape e botões explícitos.
- Respeita `prefers-reduced-motion`; as transições locais são desligadas nessa condição.
- Contraste de texto e borda deve ser observado em tema real, sobretudo sobre `#20242D`. Cor e borda cromáticas não foram redefinidas nesta etapa.
- A rota não implementa componentes definitivos, roving tabindex para menu ARIA nem lógica de negócio.

## Validação

Verificações executadas nesta revisão:

- `npm run lint`, `npm run typecheck`, `npm run test` e `npm run build` passaram. A suíte final executou 5 arquivos e 16 testes. O Vitest ainda mostrou o aviso preexistente sobre `configLoader: 'native'` em `vitest.config.ts`; sem falhas.
- Playwright inspecionou Light e Dark a 320, 360, 768, 1024 e 1440 CSS px. Nas dez combinações `documentElement.scrollWidth` foi igual a `clientWidth`; não houve overflow horizontal. Altura computada dos controles e campos: 44px. Raios computados: 8px para controles e 12px para painéis.
- O tema Dark resolveu `--nite-surface-elevated` para `rgb(32, 36, 45)` (#20242D) e shadow flutuante para `0 8px 24px rgb(0 0 0 / .36)`. Light usou a superfície elevada já existente `rgba(235, 241, 248, .94)` e shadow `0 8px 24px rgb(15 23 42 / .14)`. O controle A/B desliga a sombra sem alterar a superfície.
- Popover: abrir/fechar, sequência por Tab, Escape, retorno do foco ao acionador e ativação de uma ação verificados. O item escolhido atualiza a mensagem demonstrativa.
- Dialog: `showModal()` abriu diálogo modal nativo; computed style confirmou raio 16px, shadow modal e backdrop temático. Escape fechou o diálogo e devolveu foco ao acionador.
- `prefers-reduced-motion: reduce` e `forced-colors: active` foram emulados. O controle manteve altura mínima 44px, transição ficou em `0s` com redução de movimento, e o documento não apresentou overflow na emulação.
- Em servidor de produção após build, `/` respondeu 200 e `/dev/design-system/geometry` respondeu 404.
- Capturas reais e script reproduzível estão em `apps/web/output/playwright/etapa-4-2-geometry-20261027/` (`geometry-light-1440.png`, `geometry-dark-1440.png`, versões 360px, estados de menu/dialog e `validate-geometry.txt`).

Zoom nativo do navegador em 200%/400% não foi validado: o Playwright CLI usado não expõe esse controle de zoom. As larguras CSS testadas não são apresentadas como equivalentes a zoom. Leitor de tela também não foi testado. O reflow estreito foi corrigido durante a inspeção: em mobile o contexto do popover ocupa a linha inteira antes do acionador, evitando quebra excessiva de palavras.
