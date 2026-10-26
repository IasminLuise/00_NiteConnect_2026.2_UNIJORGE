# B05 — Protocolo de decisão final sobre ONLYOFFICE

Este documento complementa o analisador. Não altera a ADR B05-005 e não substitui avaliação institucional.

## Critério de encerramento: três saídas mutuamente distintas

**1. GO definitivo (somente por revisão formal, nunca por software deste patch):** os tipos de documento oficialmente suportados e o corpus representativo foram delimitados; a fidelidade textual, de formatação, estrutura, campos e conteúdo, além dos requisitos de segurança/integração, foi demonstrada com evidência reprodutível e parecer técnico-editorial independente; o backend pode integrar o motor **após** passar pelos gates pertinentes. Não significa autorização automática para promover ou publicar documentos.

**2. GO condicionado:** existe subconjunto de tipos/fluxos satisfatoriamente qualificado e as restrições operacionais são explicitamente aprovadas pelos responsáveis; requisitos não demonstrados ficam excluídos, com controles de acesso e de promoção intactos. Não equivale à adoção definitiva e não permite publicar a candidata em quarentena. A matriz deste kit não emite GO condicionado por conta própria.

**3. NO-GO para adoção definitiva:** regressão de conteúdo, de segurança, de formatação funcional ou risco estrutural incompatível com os documentos do escopo, ou uma incapacidade demonstrada de qualificar recursos indispensáveis. Documentar alternativa técnica e o custo de migração; não pressupor automaticamente que outro motor passe nos testes.

**HOLD/INCONCLUSIVO é diferente das três saídas acima:** dados pendentes ou equivalência não demonstrada não devem ser transformados em GO condicionado e tampouco automaticamente em defeito comprovado do motor.

## Perguntas que devem ser respondidas, em sequência, pelo mesmo relatório

1. **B05-M2P4 — diagnóstico:** a diferença existe antes de entrar no editor? É gerada na extração SDK, na persistência callback, no round-trip de conversão, no campo atualizado pelo Word ou no raster? Há repetição de PDF de origem obtida por nova exportação? O marcador preserva texto e legibilidade sob digitação simples? Quais transformações seguem sem equivalência?
2. **B05-M2P5 — decisão:** cada diferença é causalmente compreendida ou ainda não demonstrada? Impacta função, legibilidade, conteúdo, rastreabilidade, impressão ou segurança? O corpus representativo está completo? A integração e o gate foram verificados com as configurações *normais* de produção? Há parecer independente validado?

## Itens que não serão aprovados por inferência

- Variação de raster como mera antialiasing sem experimento controlado que a atribua;
- Adição/remoção de partes OPC, endnotes, estilos e rels com comparação superficial de IDs;
- Resultado de campo `PAGE`/`SAVEDATE` após SaveAs2 usado para provar estado preexistente;
- `download:true` experimental tratado como característica da configuração API normal;
- Declaração humana de paste tratada como evento instrumentado;
- Duas páginas de uma fixture sintética tratadas como garantia de preservação geométrica global;
- Critério acadêmico/institucional não aprovado por coordenador ou entidade competente.

## Registro de encerramento a preencher pelo responsável (fora do analisador)

| Campo | Exigência |
|---|---|
| Corpus e escopo | Quais modelos, versões, idiomas e recursos Word foram realmente testados |
| Matriz M2P4 | Resultados por origem, SDK, callback, Word; diferenças não mascaradas |
| Resíduos pendentes | Nome, classe, causalidade, significância e efeito operacional |
| Matriz M2P5 | Critérios aprovados/rejeitados/pendentes e justificativas |
| Responsáveis | Parecer técnico independente e homologação editorial/institucional |
| Decisão | GO definitivo / GO condicionado / NO-GO / HOLD, com motivo verificável |
| Backend | Gates e controles de segurança continuam ativos até aprovação formal |

**Regra de parada:** uma vez classificados os requisitos indispensáveis como não atendidos ou não qualificáveis no escopo, formalizar NO-GO em vez de criar exceções sucessivas. Se estiverem apenas sem evidência, manter HOLD, delimitar a aquisição faltante e não fabricar uma conclusão.
