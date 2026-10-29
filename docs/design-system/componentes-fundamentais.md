# NITE Connect — componentes fundamentais

## Escopo

Este documento registra os contratos implementados para `Button`, `TextField`, `Textarea`, `Field`, `Select`, `Checkbox`, `Badge`, `InlineFeedback`, `Surface`, `Dialog` e `AlertDialog` em `apps/web/src/components/ui/`, além de `FilterToolbar`, `LoadingState`, `EmptyState`, `ErrorState`, `DataTable` e `Pagination` em `apps/web/src/components/patterns/`. Todos são reutilizáveis fora do Design System Lab. O Lab em `/dev/design-system/components` usa os mesmos componentes e padrões; seus dados e ações continuam demonstrativos.

Os lotes não implementam formulários de domínio, gerenciamento de formulário, validação de regras de negócio, sistema global de notificações, componentes de domínio ou operações de back-end.

## Organização

```text
src/components/
  ui/
    badge.tsx
    button.tsx
    checkbox.tsx
    control-styles.ts
    cx.ts
    alert-dialog.tsx
    dialog-frame.tsx
    dialog.tsx
    field.tsx
    index.ts
    inline-feedback.tsx
    select.tsx
    surface.tsx
    text-field.tsx
    textarea.tsx
  patterns/
    data-table.tsx
    filter-toolbar.tsx
    index.ts
    pagination.tsx
    query-states.tsx
```

## Padrões compostos de consulta

Os padrões administrativos do lote 5.5 ficam em `apps/web/src/components/patterns/` e reutilizam os componentes de `ui/`. Eles cuidam de apresentação e composição; o consumidor mantém valores dos campos, aplicação de filtros, obtenção dos dados e transições assíncronas.

### `FilterToolbar`

Renderiza um formulário responsivo que aceita qualquer composição React como campos e ações. Os campos usam uma coluna em telas estreitas e colunas autoajustáveis a partir de 14rem quando há espaço; na mesma linha, ações ficam ao lado dos campos quando couberem e reorganizam-se abaixo quando necessário. Não define quantidade de filtros, consulta, debounce ou integração com API.

| Propriedade | Contrato |
|---|---|
| `children` | Campos e agrupamentos fornecidos pelo consumidor; use `Field` com `TextField`, `Select` ou outros controles compatíveis. |
| `actions` | Ações contextuais fornecidas pelo consumidor, normalmente um botão explícito `type="submit"`. |
| `hasActiveFilters` | Indica se a consulta atual tem filtros aplicados; exibe o Badge “Filtros aplicados”. |
| `onClear` | Callback opcional para limpar valores controlados pelo consumidor; o botão aparece quando há filtros ativos. |
| `clearLabel`, `activeLabel` | Texto contextual opcional para a ação de limpeza e o Badge. |
| Propriedades de `form` | Incluem `onSubmit`, `aria-label`, `id`, `className` e atributos HTML de formulário. O consumidor cancela a submissão padrão quando a consulta é local ou assíncrona. |

```tsx
<FilterToolbar
  aria-label="Filtros de registros"
  onSubmit={applyFilters}
  hasActiveFilters={hasFilters}
  onClear={clearFilters}
  actions={<Button type="submit">Aplicar filtros</Button>}
>
  <Field id="query" label="Pesquisar">
    <TextField type="search" value={query} onChange={updateQuery} />
  </Field>
  <Field id="period" label="Período">
    <Select value={period} onChange={updatePeriod}>{/* opções do consumidor */}</Select>
  </Field>
</FilterToolbar>
```

### Estados de consulta

| Componente | Responsabilidade e contrato |
|---|---|
| `LoadingState` | Indica espera com spinner decorativo, rótulo visível, `role="status"`, `aria-live="polite"`, `aria-busy="true"` e altura mínima para reduzir mudanças abruptas. Aceita `label` e `className`. Não assume formato de skeleton ou duração da consulta. |
| `EmptyState` | Requer `kind="initial"` ou `kind="filtered"`, distinguindo coleção sem registros de consulta sem correspondências. Cada tipo tem título/descrição genéricos padrão; `title`, `description`, `visual`, `action` e `className` permitem contextualização. `visual` é decorativo e fica oculto de tecnologias assistivas. |
| `ErrorState` | Requer `description`, compõe `InlineFeedback` na variante `error` com anúncio educado e oferece botão secundário somente quando `onRetry` é informado. Aceita `title`, `retryLabel` e `className`; a nova tentativa e o estado pending pertencem ao consumidor. |

```tsx
{loading ? <LoadingState label="Carregando projetos…" /> : null}
{records.length === 0 ? <EmptyState kind={hasFilters ? "filtered" : "initial"} /> : null}
{error ? <ErrorState description={errorMessage} onRetry={reload} /> : null}
```

Os estados são regiões inline para listas, tabelas, painéis ou conteúdo composto. Não implementam cache, requisições globais, política de retry, persistência nem regras de domínio. `FilterToolbar` e os estados podem ser usados independentemente do Design System Lab.

### `DataTable`

Tabela semântica genérica para conjuntos tabulares. Recebe `caption`, `columns`, `rows` e `getRowId`; a aplicação fornece a identificação estável de cada linha, o conteúdo das células e qualquer ação. Cada coluna declara `id`, `header` e `cell`, podendo também indicar alinhamento, cabeçalho de linha e classes. A legenda existe no HTML e fica visualmente oculta. Cabeçalhos de coluna usam `scope="col"`; uma coluna marcada `rowHeader` gera `<th scope="row">`.

| Propriedade | Contrato |
|---|---|
| `caption` | Nome acessível que descreve a tabela. |
| `columns`, `rows` | Dados e apresentação controlados pelo consumidor; não há ordenação, seleção ou busca implícita. |
| `getRowId` | Identificador estável de linha, usado como chave React. |
| `density` | `comfortable` (padrão) usa `px-4 py-3`; `compact` usa `px-3 py-2`. A escala tipográfica permanece igual. |
| `emptyContent` | Conteúdo opcional fornecido pelo consumidor, renderizado em linha que ocupa todas as colunas. O estado de consulta continua responsabilidade do consumidor. |

A região da tabela pode rolar horizontalmente quando a largura mínima tabular não cabe. Esse overflow fica contido no próprio wrapper; a página não precisa alargar. Não há altura fixa de linha, paginação, virtualização, seleção, ordenação ou regra de domínio embutida.

```tsx
<DataTable
  caption="Integrantes do núcleo"
  columns={columns}
  rows={members}
  getRowId={(member) => member.id}
  density="compact"
/>
```

### `Pagination`

Controle independente e controlado de navegação entre páginas. Recebe `currentPage`, `totalPages` e `onPageChange`; botões Anterior/Próxima respeitam os limites e fornecem nomes acessíveis. A página exibida é normalizada para o intervalo válido; quando `totalPages` é zero, apresenta “Nenhuma página” e não oferece navegação. Em viewports menores que o breakpoint `sm`, o indicador visual é abreviado (`1/4`), mas a live region mantém a leitura completa (“Página 1 de 4”). O consumidor continua responsável por atualizar a fonte de dados, limitar resultados e redefinir sua página quando filtros ou totais mudarem. O componente não faz requisições nem mantém estado próprio.

```tsx
<Pagination
  currentPage={page}
  totalPages={pageCount}
  onPageChange={setPage}
/>
```

### Composição de consulta, tabela e paginação

`FilterToolbar` organiza os controles e ações. O consumidor coordena filtros aplicados, estado assíncrono, conteúdo da tabela e página. `LoadingState`, `EmptyState` e `ErrorState` ocupam a região de resultados; em estado de conteúdo, `DataTable` e `Pagination` são compostos sem dependência entre si. Ao mudar filtros, o consumidor deve decidir e aplicar o reset da página, como faz a demonstração do Lab.

```tsx
<FilterToolbar onSubmit={applyFilters} actions={<Button type="submit">Aplicar</Button>}>
  {/* Field e controles controlados pelo consumidor */}
</FilterToolbar>
{loading ? <LoadingState /> : error ? <ErrorState description={error} onRetry={reload} /> : (
  <>
    <DataTable caption="Registros" columns={columns} rows={pageRows} getRowId={getId} />
    <Pagination currentPage={page} totalPages={pageCount} onPageChange={setPage} />
  </>
)}
```

`control-styles.ts` concentra a base visual compartilhada entre `TextField` e `Textarea`; `cx.ts` combina classes sem adicionar dependência de merge de Tailwind. Os componentes não introduzem estado global e, salvo o Lab, não exigem Client Components.

## Tokens e fundações

- A cor vem dos papéis ativos: `nite-*` e `status-error-*`, sem referências a valores de tema Light/Dark nos componentes.
- Tipografia: `font-sans`, `text-type-label`, `text-type-body`, `text-type-metadata` e `text-type-body-compact`.
- Espaçamento: utilities da escala Tailwind existente (`gap-2`, `px-3`, `px-4`, `py-2`).
- Geometria: `rounded-control`, `min-h-11` (44px) e foco com outline visível de 3px e offset de 2px.
- Os controles de entrada e seleção usam `text-type-body` (16px), inclusive em mobile. A Textarea pode ser redimensionada verticalmente; não define altura fixa.
- `Checkbox` usa `accent-nite-brand-primary`, foco visível e área de rótulo clicável via `Field`.
- `Badge` usa `rounded-pill` e famílias neutra, institucional ou semânticas existentes, sem valores cromáticos locais.
- `Surface` usa `rounded-panel`, borda e superfície; apenas `elevated` aplica `shadow-nite-floating`.
- `InlineFeedback` usa bordas/fundos semânticos existentes e texto principal de alto contraste.
- `Dialog` usa `rounded-dialog`, `shadow-nite-modal` e o backdrop transitório do elemento nativo; não adiciona tokens persistentes.

## `Button`

Usa o elemento nativo `<button>` e aceita suas propriedades, atributos ARIA e `ref`.

| Propriedade | Contrato |
|---|---|
| `variant` | `primary` (padrão), `secondary`, `quiet` ou `destructive`. `secondary` é a variante de contorno. `destructive` usa a família semântica de erro em tratamento suave e deve ficar reservada a uma ação crítica explícita; a confirmação da operação pertence ao fluxo que a compõe. |
| `loading` | Mantém `children` visível, exibe indicador decorativo, define `aria-busy="true"` e desabilita o botão para evitar acionamento duplicado. |
| `leadingIcon`, `trailingIcon` | Conteúdo opcional decorativo, sem biblioteca de ícones; o rótulo continua sendo fornecido pelo conteúdo do botão ou por `aria-label` em controle apenas com ícone. |
| Propriedades HTML | Incluem `type`, `disabled`, `name`, `value`, `form`, `onClick`, `aria-pressed` e `ref`. O componente não força `type`; ao usá-lo em formulário, informe `type="button"`, `"submit"` ou `"reset"` explicitamente. |

Estados visuais: repouso, hover, foco visível, pressionado, desabilitado e carregando. Respeita `prefers-reduced-motion` na transição e no indicador de carregamento.

```tsx
<Button type="submit" variant="primary">Salvar</Button>
<Button type="button" variant="secondary">Voltar</Button>
<Button type="button" loading>Salvando</Button>
```

Links devem usar `next/link` ou `<a>` e não receber papel de botão por meio deste componente.

## `TextField`

Renderiza um `<input>` nativo e repassa as propriedades HTML, incluindo `type`, `name`, `value`, `defaultValue`, `required`, `disabled`, `readOnly`, validação nativa, eventos e `ref`. O `type` padrão é `text`; tipos como `email`, `password` e `search` permanecem sob decisão do consumidor.

Controles usam a mesma classe-base de `Textarea`, contemplando foco, `aria-invalid`, desabilitado e somente leitura. Não valida regras de domínio nem altera seleção, teclado ou submissão nativa.

```tsx
<TextField type="email" name="email" autoComplete="email" />
```

## `Textarea`

Renderiza `<textarea>` nativo e repassa atributos, conteúdo controlado ou não controlado, `rows`, eventos e `ref`. Compartilha cor, borda, tipografia, foco e estados de `TextField`; acrescenta altura mínima, redimensionamento vertical e alinhamento pelo topo. Não incorpora editor rico.

```tsx
<Textarea name="description" rows={4} placeholder="Descreva o contexto" />
```

## `Field`

Compõe label, controle filho, texto auxiliar e erro. Requer `id` único, label e exatamente um elemento de controle compatível (`TextField` ou `Textarea`). Injeta o `id` no controle, associa o label com `htmlFor` e combina os IDs de descrição/erro com referências `aria-describedby` já informadas no controle. Quando há `error`, marca `aria-invalid`; `required` é aplicado ao controle e sinalizado também por texto. `optional` exibe indicação textual quando o controle não é obrigatório.

`Field` não executa validação. O consumidor controla valor, eventos e regra do campo; o erro apresentado deve vir do fluxo apropriado. Não use o mesmo `id` mais de uma vez na página.

`layout="checkbox"` posiciona o checkbox antes do rótulo em uma linha com altura mínima de 44px e mantém descrição/erro abaixo do conjunto. Use essa disposição somente com um controle checkbox; a propriedade não altera a semântica do controle.

```tsx
<Field
  id="project-name"
  label="Nome do projeto"
  description="Use um nome que identifique o projeto."
  required
>
  <TextField name="projectName" />
</Field>
```

## `Select`

Renderiza `<select>` nativo para seleção simples. Repassa atributos HTML, eventos e `ref`; suporta `value` ou `defaultValue`, `name`, `required`, `disabled`, `aria-invalid` e `aria-describedby`. A API tipa valores como string e omite `multiple` e `size`, pois a biblioteca ainda não oferece multisseleção nem lista expandida.

Quando `placeholder` é informado, ele cria uma opção inicial vazia e desabilitada. Para um controle não controlado, o valor inicial é `""`; em um controle controlado, o consumidor define `value` e atualiza o valor em `onChange`. Com `required`, o placeholder vazio mantém a validação nativa do navegador.

```tsx
<Field id="team" label="Equipe" description="Escolha uma equipe." required>
  <Select name="team" placeholder="Selecione uma equipe">
    <option value="research">Pesquisa</option>
    <option value="extension">Extensão</option>
  </Select>
</Field>
```

O controle mantém a aparência e tipografia compartilhadas com os demais campos, incluindo fonte de 16px, altura mínima de 44px, foco visível e opções nativas do navegador. Não implementa busca, combobox ou dropdown customizado.

## `Checkbox`

Renderiza `<input type="checkbox">` e mantém as propriedades nativas de formulário, estado controlado (`checked`/`onChange`) ou não controlado (`defaultChecked`), nome, valor, desabilitado, validação e `ref`.

`indeterminate` sincroniza a propriedade DOM homônima para apresentar um estado misto. Essa propriedade é apenas visual: a submissão continua determinada por `checked` e pelo `name`/`value` nativos. O componente não cria um terceiro valor nem substitui a regra de formulário do consumidor.

```tsx
<Field
  id="include-archived"
  label="Incluir itens arquivados"
  description="A opção pode ser alterada depois."
  layout="checkbox"
>
  <Checkbox name="includeArchived" />
</Field>
```

O rótulo associado é clicável, o controle responde ao teclado nativo e tem outline de foco. O estado `disabled` permanece nativo. Para erros, use `Field` com `error`; a mensagem é vinculada por `aria-describedby` e o controle recebe `aria-invalid`.

## `Badge`

Elemento `<span>` exclusivamente informativo, sem ação, papel de botão ou foco de teclado. Aceita conteúdo textual, `className`, propriedades não interativas de `span` e `ref`.

| Variante | Apresentação | Tokens de cor |
|---|---|---|
| `neutral` (padrão) | Identificação neutra | `nite-border-soft`, `nite-surface-subtle`, `nite-text-secondary` |
| `institutional` | Ênfase da identidade do Connect | Texto `nite-brand-accent`, fundo `nite-brand-soft`, borda `nite-brand-primary` |
| `info` | Informação | `status-progress` |
| `success` | Resultado positivo | `status-done` |
| `warning` | Atenção | `status-warning` |
| `error` | Erro | `status-error` |

```tsx
<Badge variant="institutional">NITE Connect</Badge>
<Badge variant="warning">Atenção necessária</Badge>
```

O texto deve comunicar o significado sem depender apenas da cor. O componente não define nomes de status do produto, regras de projetos/integrantes/atividades, ícones ou interatividade.

## `InlineFeedback`

Mensagem contextual para informação, sucesso, atenção e erro. Requer `variant` e `description`; aceita `title`, `action` e `className`. Combina um símbolo decorativo (`aria-hidden`), texto visível, famílias semânticas já existentes e uma superfície suave. A descrição usa texto primário para manter contraste; o título e o símbolo reforçam a variante.

| Variante | Família de tokens |
|---|---|
| `info` | `status-progress` |
| `success` | `status-done` |
| `warning` | `status-warning` |
| `error` | `status-error` |

Mensagens persistentes não recebem `role` ou `aria-live` por padrão. Para conteúdo atualizado dinamicamente, `announcement="polite"` expõe `role="status"`; `announcement="assertive"` expõe `role="alert"`. `announcement="none"` mantém o conteúdo fora de live regions. Escolha o anúncio no ponto em que a mensagem muda, sem transformar todo feedback persistente em notificação.

```tsx
<InlineFeedback
  variant="warning"
  title="Revisão necessária"
  description="Confira os campos indicados antes de continuar."
  action={<Button type="button" variant="secondary">Revisar</Button>}
/>
```

O componente não agenda mensagens, não some automaticamente e não é um Toast ou serviço global. A ação é uma composição fornecida pelo consumidor.

## `Surface`

Wrapper estrutural `<div>` sem comportamento de botão/link. Aceita as propriedades nativas de `div`, `className`, `ref`, `variant` e `padding`.

| Propriedade | Valores | Comportamento |
|---|---|---|
| `variant` | `default` (padrão) | Superfície comum com borda suave, sem sombra. |
| `variant` | `subtle` | Fundo sutil com a mesma borda, sem sombra. |
| `variant` | `elevated` | Superfície elevada, borda forte e sombra flutuante. Reserve para uma hierarquia que precise de separação. |
| `padding` | `comfortable` (padrão) | `p-4 sm:p-6`. |
| `padding` | `compact` | `p-3 sm:p-4`. |
| `padding` | `none` | Sem padding; o conteúdo decide o espaçamento. |

```tsx
<Surface>
  <h2>Conteúdo agrupado</h2>
</Surface>
```

Prefira um Surface por agrupamento. Não empilhe superfícies para simular camadas; painéis comuns não recebem sombra.

## `Dialog`

Modal controlado sobre o elemento nativo `<dialog>`. Requer `open`, `onOpenChange` e `title`; aceita `description`, `children`, `footer`, `initialFocusRef`, `closeOnBackdrop` (padrão `true`) e `className`.

- A abertura chama `showModal()` e mantém o conteúdo externo inerte pela modalidade nativa do navegador.
- O foco inicial vai ao `initialFocusRef` quando informado; caso contrário, ao título. O navegador restaura o foco ao elemento que abriu o diálogo quando ele fecha.
- Escape e o botão Fechar solicitam `onOpenChange(false)`. Clique no backdrop também fecha por padrão; desative por `closeOnBackdrop={false}` se o contexto exigir uma ação explícita.
- O consumidor controla o estado e compõe as ações de rodapé. Para um formulário no corpo, associe o botão de rodapé com o atributo HTML `form`.
- O cabeçalho e as ações permanecem fixos; o corpo usa rolagem vertical interna quando excede a altura disponível. A largura e a altura máxima adaptam-se à viewport.

```tsx
<Dialog
  open={open}
  onOpenChange={setOpen}
  title="Editar item"
  description="Atualize os dados necessários."
  footer={<Button type="button" onClick={() => setOpen(false)}>Concluir</Button>}
>
  <Field id="item-name" label="Nome" required><TextField /></Field>
</Dialog>
```

## `AlertDialog`

Confirmação específica para uma ação sensível; não é um alias visual de `Dialog`. Requer `open`, `onOpenChange`, `title`, `description` e `onConfirm`; aceita `children`, `cancelLabel`, `confirmLabel`, `pending` e `className`.

- O rótulo e a descrição devem explicar a ação e suas consequências; Cancelar é explícito e recebe foco inicial.
- Escape solicita cancelamento seguro quando `pending` é falso. O clique no backdrop nunca confirma nem fecha a confirmação.
- Enquanto `pending`, Cancelar e Confirmar ficam desabilitados, Escape não fecha e Confirmar indica estado ocupado para evitar acionamentos repetidos.
- A operação e a decisão de fechar após sucesso pertencem ao consumidor. `onConfirm` não executa regra, autorização, exclusão ou chamada de back-end.

```tsx
<AlertDialog
  open={open}
  onOpenChange={setOpen}
  title="Confirmar ação?"
  description="Esta ação altera o registro selecionado."
  pending={saving}
  onConfirm={confirmAction}
/>
```

Ambos os diálogos compartilham `dialog-frame.tsx`, que centraliza elemento nativo, IDs acessíveis, foco inicial, fechamento e estrutura visual. `Dialog` permite fechamento por backdrop e oferece botão Fechar; `AlertDialog` remove esse botão, mantém o backdrop inerte e coloca Cancelar antes de Confirmar.

## Acessibilidade

- Manter o rótulo visível para `Button`; ações somente com ícone exigem `aria-label` ou texto acessível equivalente.
- Usar labels associados via `Field`; não depender de placeholder como rótulo.
- `Field` associa descrição e erro com `aria-describedby`; erros também expõem `aria-invalid`.
- Usar `disabled` para indisponibilidade operacional e `readOnly` quando o valor deve continuar acessível/selecionável.
- Loading preserva o nome acessível do botão e informa estado ocupado.
- O foco do botão e dos campos é visível nos dois temas; não remover outline sem alternativa equivalente.
- `Select` e `Checkbox` preservam seleção/teclado nativos; `Field` mantém rótulo, ajuda, erro e obrigatoriedade associados.
- `Badge` é conteúdo complementar; não substitui rótulos textuais de estado nem assume semântica de domínio.
- `InlineFeedback` só usa live region quando o consumidor declara que a mensagem é uma atualização dinâmica.
- `Dialog`/`AlertDialog` preservam `<dialog>` modal nativo, Escape, foco inicial, retorno de foco e rolagem interna; operações e estado controlado continuam no consumidor.
- `Surface` não tem sombra por padrão; `elevated` e a elevação modal são escolhas contextuais com tokens aprovados.
- Texto auxiliar e erro usam tokens de texto já aprovados. As demonstrações não constituem validação institucional de toda combinação de conteúdo/tema.

## Design System Lab

Rota: `/dev/design-system/components`, disponível apenas fora de `NODE_ENV=production`.

O Lab demonstra os componentes reais dos lotes 5.2–5.6: variantes/estados de Button; TextField, Textarea, Select, Checkbox, Field, Badge, InlineFeedback, Surface, Dialog e AlertDialog; composição de erro alternável; estados controlados e não controlados; validação nativa; filtros locais combináveis; tabela administrativa com paginação e densidades confortável/compacta; loading, coleção vazia, consulta vazia e falha recuperável; navegação entre fundações; e seletor Light/Dark/System existente. O diálogo contém formulário e conteúdo longo para testar rolagem; a confirmação sensível simula execução e não altera dados. As interações e dados não persistem nem representam fluxos do produto.

## Decisões e limites

- O indicador de loading é CSS, não dependência externa; desabilita o botão até o estado ser removido pelo consumidor.
- A variante destrutiva existe para ações críticas com semântica documentada. Ela não confirma nem executa uma operação sensível sozinha.
- Não há combobox, busca interna, multisseleção, dropdown customizado, Tooltip, Toast, Popover, ordenação, seleção de linhas, virtualização ou API de formulário.
- `FilterToolbar` não filtra nem busca dados; filtros aplicados e limpeza dependem do estado e callbacks fornecidos pelo consumidor. `LoadingState`, `EmptyState` e `ErrorState` não conhecem formatos de resposta nem integram serviços.
- `Dialog` e `AlertDialog` não implementam operações administrativas nem política de autorização; o consumidor fornece estado, ações e fechamento após sucesso.
- Variantes de `Badge` apenas apresentam famílias cromáticas; mapeamento de estados específicos ficará em futuras composições de domínio.
- Os contratos podem evoluir com os consumidores reais, sem migrar telas existentes em massa.
