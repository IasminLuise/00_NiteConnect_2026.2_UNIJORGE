# NITE Connect — fundação cromática

## Objetivo e escopo

Esta fundação oferece tokens de cor compartilhados entre as telas do Connect e um laboratório local para avaliá-los. Os valores institucionais vêm da implementação canônica do Portal NITE (`packages/ui/src/theme.css`). O Connect herda a paleta e os significados cromáticos, mas mantém a própria composição e os próprios componentes administrativos.

O escopo aqui é cromático: não define tipografia, layout de produto, navegação, tabelas ou uma biblioteca de componentes. O CSS atual do Diário foi ligado aos tokens para validar os dois temas sem alterar sua estrutura.

## Organização dos tokens

`apps/web/src/app/globals.css` contém três níveis:

1. Valores por tema: `--nite-light-*` e `--nite-dark-*`.
2. Papéis ativos: `--nite-background`, `--nite-surface`, `--nite-text-primary`, `--nite-brand-primary`, `--nite-status-warning` e demais `--nite-*`.
3. Aliases do Tailwind 4 em `@theme inline`, que apontam para os papéis ativos sem manter uma segunda paleta.

Use os papéis ativos nos novos componentes, por exemplo `var(--nite-surface)` ou as classes Tailwind `bg-nite-surface`, `text-nite-text-primary`, `border-nite-border-soft` e `bg-status-warning`. Evite usar os valores `--nite-light-*` e `--nite-dark-*` diretamente em componentes: a seleção do tema pertence aos tokens semânticos.

## Tokens institucionais herdados

Os valores abaixo são os extraídos do CSS do Portal:

| Papel | Claro | Escuro |
| --- | --- | --- |
| Fundo principal | `#F4F7FA` | `#09090A` |
| Fundo de seção | `#E8EEF5` | `#0D111C` |
| Texto principal | `#0B1220` | `#F0F0F0` |
| Texto secundário | `#334155` | `#A1A4A5` |
| Texto auxiliar | `#5C6878` | `#878D8F` |
| Azul institucional | `#1D4ED8` | `#2563EB` |
| Acento ciano | `#0369A1` | `#38BDF8` |

Superfícies, bordas e estados também seguem a base do Portal. O papel de placeholder reutiliza o texto secundário opaco (`#334155` / `#A1A4A5`) para manter legibilidade; os valores originais translúcidos do Portal seriam fracos como texto de formulário. `--nite-focus` usa uma cor opaca de identidade (`#1D4ED8` / `#38BDF8`) como contorno de teclado, em vez dos anéis translúcidos do Portal. Isto evita depender de uma indicação com baixo contraste. Texto sobre ação azul usa branco; no tema escuro, a cor de texto de marca deve preferir o ciano quando o azul institucional não alcançar contraste de texto comum.

## Famílias operacionais herdadas

As seis famílias são cores semânticas, não regras de domínio. Fundos suaves e bordas são calculados com `color-mix()` a partir da cor da família e da superfície atual.

| Família | Claro | Escuro |
| --- | --- | --- |
| `draft` | `#475569` | `#94A3B8` |
| `progress` | `#0369A1` | `#38BDF8` |
| `validated` | `#6D28D9` | `#A78BFA` |
| `done` | `#047857` | `#34D399` |
| `warning` | `#92400E` | `#FBBF24` |
| `error` | `#BE123C` | `#FB7185` |

Aliases semânticos podem ser criados quando fizerem sentido, reutilizando essas famílias. Um estado do produto precisa de texto explícito e não deve ser inferido apenas pela cor.

## Séries categóricas candidatas

`--nite-data-1` a `--nite-data-6` têm os seguintes valores, ainda sujeitos a validação de design:

| Série | Claro | Escuro |
| --- | --- | --- |
| 01 | `#1D4ED8` | `#60A5FA` |
| 02 | `#007C91` | `#22D3EE` |
| 03 | `#9A6700` | `#FACC15` |
| 04 | `#6B4C9A` | `#C4B5FD` |
| 05 | `#8C4A5F` | `#F472B6` |
| 06 | `#475569` | `#CBD5E1` |

São séries sem semântica de aprovação, erro, risco ou desempenho. A legenda e os rótulos devem tornar os gráficos interpretáveis sem discriminação cromática. As cores são usadas nos marcadores/barras; não sobreponha automaticamente texto branco às amostras.

## Superfícies e estados de interação

As superfícies têm papéis distintos: fundo, seção/navegação, superfície padrão, superfície em foco e superfície sutil. O Connect também define `--nite-surface-elevated` para menus e camadas sobrepostas. No tema escuro, `#20242D` é uma hipótese para essa elevação, marcada como candidata e exibida no laboratório contra a superfície padrão. Ainda não é uma aprovação final.

Foco visível usa `--nite-focus` com contorno sólido de 3 px e deslocamento. Seleção é persistente e combina fundo tonal com marcador/linha; hover é transitório e altera a superfície ou borda. Elementos desabilitados usam texto e fundo próprios; bordas sutis não são o único sinal de um controle. A combinação medida do foco sólido é 6,70:1 no Light e 7,25:1 no Dark contra as superfícies de demonstração.

## Temas

O controle seleciona **Sistema**, **Claro** ou **Escuro**. Sem preferência salva, o Connect começa em **Sistema** e, quando o sistema não oferece preferência clara, resolve para escuro, alinhado ao Portal. A preferência é armazenada em `localStorage` com a chave própria `nite-connect-theme`, sem sincronização entre origens.

O script síncrono de bootstrap em `apps/web/src/lib/theme.ts` define `data-theme` antes da hidratação. O provedor React mantém o controle sincronizado, acompanha mudanças de `prefers-color-scheme` enquanto Sistema estiver selecionado e recebe eventos `storage` de outras abas. Valor persistido inválido ou storage indisponível resulta em Sistema; sem storage, a escolha continua ativa na aba atual.

## Laboratório visual

Com as dependências já instaladas, inicie a aplicação em `apps/web` com `npm run dev` e abra `http://localhost:3000/dev/design-system/colors`. O laboratório usa os tokens globais reais e mostra cores institucionais, hierarquia de superfícies, badges semânticos, hover/seleção/foco/desabilitado, cores de séries e um gráfico com dados fictícios. Tab alcança o controle de foco por teclado.

A rota executa `notFound()` quando `NODE_ENV` é `production`; não é apenas omitida da navegação. O build local de produção foi gerado e o servidor `next start` respondeu 404 para a rota do laboratório.

## Acessibilidade e pendências

As combinações de texto institucional foram auditadas previamente contra os fundos institucionais: os tons auxiliares superam 4,5:1 nos fundos correspondentes. Uma verificação numérica das badges nesta implementação, com tonal a 10% sobre a superfície, encontrou mínimo de 4,78:1 no Light e 5,62:1 no Dark. Os marcadores das séries contra fundo de seção variam de 4,17:1 a 6,49:1 no Light e de 7,12:1 a 12,70:1 no Dark, acima de 3:1 para os elementos gráficos testados. As cores de status e as séries devem ser recalculadas se suas superfícies ou opacidades mudarem. Gráficos demonstrativos não atribuem texto branco às barras.

Pendências explícitas para validação:

- Confirmar se `#20242D` oferece a elevação visual adequada no modo escuro; a comparação numérica aproximada com a superfície Portal efetiva `#17181A` é de apenas 1,14:1, então sua separação é sutil e não deve ser o único indicador de hierarquia.
- Avaliar distinção das seis séries e simulações de visão cromática antes de adotar gráficos de produção.
- Revisar estados de foco e seleção em componentes reais, pois o laboratório demonstra apenas estados pequenos.
- Testar contraste no navegador, inclusive as composições de superfície translúcida.

Próxima fundação sugerida: tipografia do Connect (Sora, Geist e Geist Mono), a ser discutida e implementada em outra etapa.
