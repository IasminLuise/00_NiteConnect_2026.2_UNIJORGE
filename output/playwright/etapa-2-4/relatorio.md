# NITE Connect — Etapa 2.4

## Escopo e ambiente

Verificação complementar executada em 27/10/2026 com Playwright CLI/Chromium. Não houve alteração nos arquivos do produto. O build de produção foi atualizado somente na cópia isolada em `%LOCALAPPDATA%/Temp/nite-connect-typography-final-ed5baa576d084fba9a17a0e924d80c20/web`; `npm run build` concluiu e listou as rotas `/_`, `/dev/design-system/typography` e `/dev/design-system/colors`. As rotas do laboratório chamam `notFound()` em `NODE_ENV=production`, então a comparação de tema foi feita em `/`, que contém o `ThemeControl` da aplicação real.

## Verificado e aprovado

### Espaçamento WCAG 1.4.12

No navegador, uma folha de estilos temporária aplicou `line-height: 1.5`, espaçamento de parágrafo de `2em`, `letter-spacing: 0.12em` e `word-spacing: 0.16em`. Os cenários cobriram Light e Dark em 1280 px e 320 CSS px.

- Largura do documento permaneceu igual à largura do viewport nos quatro cenários: 1280/1280 e 320/320.
- A inspeção geométrica encontrou zero sobreposições entre retângulos de texto e zero cortes de conteúdo. A única regra de clipping detectada era `.sr-only`, usada intencionalmente para o nome acessível do seletor.
- Formulário, seletor de tema e navegação permaneceram habilitados; o teste preencheu o campo e alterou a opção do formulário e do tema.
- Em 320 px, a tabela conserva rolagem interna própria (`258 px` de área visível, `410 px` de conteúdo, `overflow-x: auto`).
- Os parâmetros impostos foram aplicados aos textos de 12 px, tabelas, labels, títulos e conteúdo. Textos longos quebraram em várias linhas sem cortar conteúdo.

Capturas reais após a aplicação dos estilos temporários:

- [Light — desktop, 1280 px](wcag-spacing-light-1280x900.png)
- [Light — viewport 320 px](wcag-spacing-light-320x720.png)
- [Dark — desktop, 1280 px](wcag-spacing-dark-1280x900.png)
- [Dark — viewport 320 px](wcag-spacing-dark-320x720.png)

O roteiro reproduzível dos cenários de espaçamento está em [wcag-spacing.js](wcag-spacing.js). Ele injeta estilos apenas na página do navegador e salva as capturas; não altera fontes do projeto.

### Fontes, carregamento e CLS

- No laboratório tipográfico em desenvolvimento, `document.fonts.status` ficou `loaded`; `document.fonts.check()` confirmou Sora, Geist e Geist Mono. Os três arquivos WOFF2 são servidos pelos assets locais `/_next/static/media/`, com tamanhos transferidos de aproximadamente 23,4 KB, 29,6 KB e 34,0 KB. Não há requisição de fonte para um serviço externo durante o uso observado.
- As declarações geradas pelo Next.js apontam as fontes principais para assets empacotados. Os fallbacks ajustados usam `local(Arial)` (`Sora Fallback`, `Geist Fallback` e `Geist Mono Fallback`); portanto, a renderização principal não depende de uma fonte instalada, mas o fallback métrico preferencial depende de Arial local. Se Arial não existir, o navegador ainda pode recorrer ao seu fallback genérico, cuja métrica não foi validada.
- Em cache frio e rede normal, o CLS medido na rota `/` foi `0,002198` tanto em desenvolvimento quanto na build de produção. O deslocamento foi registrado na marca e na barra superior junto à habilitação do seletor de tema; não foram identificados deslocamentos de texto nesse perfil.
- Sob rede simulada lenta (700 ms de latência, 50.000 B/s de download e 20.000 B/s de upload), a página de produção registrou o carregamento de Geist aos ~3.650 ms e um deslocamento de `0,000180` em texto no mesmo instante. A coincidência temporal sugere contribuição da troca de fonte, mas não prova causalidade exclusiva. O maior deslocamento (`0,002198`) veio depois, na marca/barra superior, junto à habilitação do seletor. O CLS da janela máxima continuou `0,002198`.

### Hidratação e disponibilidade do tema

Na rota real `/`, com cache frio e rede normal, o controle ficou disponível em ~402 ms no dev e ~86 ms na build de produção. O estado transitório decorre de `ThemeControl` permanecer desabilitado enquanto `ThemeProvider` ainda não leu a preferência persistida no `useEffect`.

Na rede lenta simulada, o controle habilitou em ~20,8 s no dev e ~6,5 s em produção; em ambos, habilitou depois que os recursos da página carregaram. A demora extrema é sensível à carga de JavaScript e ao perfil de rede, e o servidor dev acrescenta custo de desenvolvimento. Não reproduzi uma regressão de vários segundos em produção sob rede normal. O laboratório em si não pode ser aberto na build porque suas páginas se ocultam deliberadamente em produção; a comparação foi feita no seletor da aplicação em `/`.

## Falha reproduzível

Nenhuma falha de conteúdo, sobreposição, corte, rolagem horizontal da página ou interação do laboratório foi reproduzida nos cenários de espaçamento. O seletor de tema produz deslocamento mensurável durante hidratação (`CLS 0,002198`), mas pequeno; não impediu uso nem persistiu após a habilitação.

## Não verificado

- Zoom nativo de 200% e 400%: o Playwright CLI controla a página, não a interface do navegador. Atalhos `Control+Plus`, `Control+Equal` e `Control+Shift+Equal` não alteraram `innerWidth`, `devicePixelRatio` ou `visualViewport.scale` (`320`, `1`, `1` antes e depois). Não trato a viewport de 320 CSS px como equivalente a zoom. O reflow em 320 px foi validado nesta etapa e permanece evidência separada.
- Disponibilidade de Arial em dispositivos sem essa fonte e aparência do fallback genérico: não foi simulada.
## Conclusão

Os resultados permitem encerrar a fundação tipográfica quanto ao reflow em 320 CSS px, aos testes de espaçamento, ao carregamento das fontes e ao funcionamento do tema em rede normal. Não encontrei problema corretivo que impeça a conclusão. Zoom nativo de 200%/400% segue como limitação explícita a validar em navegador com controle de zoom real; a demora do seletor sob rede simulada lenta fica registrada como comportamento de hidratação, sem regressão reproduzida no uso normal de produção.
