# NITE Connect — componentes fundamentais

## Escopo

Este documento registra os contratos implementados para `Button`, `TextField`, `Textarea` e `Field` em `apps/web/src/components/ui/`. Eles são componentes de produção e podem ser importados fora do Design System Lab. O Lab em `/dev/design-system/components` usa os mesmos componentes; seus dados e ações continuam demonstrativos.

O lote não implementa formulários de domínio, gerenciamento de formulário, validação de regras de negócio, Select, Checkbox, Badge, Dialog, AlertDialog ou componentes administrativos.

## Organização

```text
src/components/ui/
  badge.tsx
  button.tsx
  checkbox.tsx
  control-styles.ts
  cx.ts
  field.tsx
  index.ts
  select.tsx
  text-field.tsx
  textarea.tsx
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

## Acessibilidade

- Manter o rótulo visível para `Button`; ações somente com ícone exigem `aria-label` ou texto acessível equivalente.
- Usar labels associados via `Field`; não depender de placeholder como rótulo.
- `Field` associa descrição e erro com `aria-describedby`; erros também expõem `aria-invalid`.
- Usar `disabled` para indisponibilidade operacional e `readOnly` quando o valor deve continuar acessível/selecionável.
- Loading preserva o nome acessível do botão e informa estado ocupado.
- O foco do botão e dos campos é visível nos dois temas; não remover outline sem alternativa equivalente.
- `Select` e `Checkbox` preservam seleção/teclado nativos; `Field` mantém rótulo, ajuda, erro e obrigatoriedade associados.
- `Badge` é conteúdo complementar; não substitui rótulos textuais de estado nem assume semântica de domínio.
- Texto auxiliar e erro usam tokens de texto já aprovados. As demonstrações não constituem validação institucional de toda combinação de conteúdo/tema.

## Design System Lab

Rota: `/dev/design-system/components`, disponível apenas fora de `NODE_ENV=production`.

O Lab demonstra os componentes reais de todos os lotes: variantes/estados de Button; TextField, Textarea, Select, Checkbox, Field e Badge; composição de erro alternável; estados controlados e não controlados; validação nativa; navegação entre fundações; e seletor Light/Dark/System existente. As interações e dados são demonstrativos e não persistem nem representam fluxos do produto.

## Decisões e limites

- O indicador de loading é CSS, não dependência externa; desabilita o botão até o estado ser removido pelo consumidor.
- A variante destrutiva existe para ações críticas com semântica documentada. Ela não confirma nem executa uma operação sensível sozinha.
- Não há combobox, busca interna, multisseleção, dropdown customizado, tooltip, dialog ou API de formulário.
- Variantes de `Badge` apenas apresentam famílias cromáticas; mapeamento de estados específicos ficará em futuras composições de domínio.
- Os contratos podem evoluir com os consumidores reais, sem migrar telas existentes em massa.
