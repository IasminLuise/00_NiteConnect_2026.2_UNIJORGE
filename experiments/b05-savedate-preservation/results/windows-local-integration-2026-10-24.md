# B05 — SAVEDATE no fluxo real da bancada local

Compatibilidade integrada e três controles reais executados. A instrução SAVEDATE
foi preservada; suporte nativo à atualização por F9 continua pendente, como próxima
etapa. Não há liberação de produção, homologação institucional ou exceção ao gate.

## Escopo e implementação

- Checkout de execução: worktree `b05-core-docs-api`, branch `feat/b05-core-docs-api`,
  base `017281a1830724cd66eee6ee505a704e9fa37b4d`, com alterações locais anteriores
  preservadas. O checkout de auditoria em `D:\nite-connect` não foi modificado.
- `prepare_local.py` prepara uma fixture sintética nova e cópia compatível privada,
  com três páginas, cabeçalhos primeira/par/principal, três SAVEDATE, três PAGE,
  três NUMPAGES, tabelas e logotipo fictício. Registra hashes e plano sem fabricar
  saídas ou marcar controles como executados.
- `local_provision.mjs` usa a API real em loopback e administrador autenticado por
  Entra. Cria versões imutáveis distintas do modelo: original e compatível.
  O documento deriva explicitamente da versão compatível. Confere o download do
  head inicial, inclusive identificador da versão e hash, antes de emitir recibo.
- Adaptação ocorre antes do provisionamento. Não reescreve resposta de fonte,
  objeto armazenado, histórico ou bytes originais. Não transforma REF em SAVEDATE.
- Não houve alteração nesta rodada em Java, autenticação, migrações, configuração,
  gate ou permissões. Reutilizou-se o fluxo administrativo de inspeção anterior.

## Execução real e procedência

Windows; Word 16.0; ONLYOFFICE Document Server `10.0.0.1`; PostgreSQL 17.11;
API Spring Boot local, armazenamento local. Autenticação Entra validada pela API,
com `/api/v1/me` retornando 200. Token somente em memória/ambiente dos processos;
nenhum token no relatório ou nos recibos. Nenhum túnel externo foi aberto.

Cada caso recebeu duas versões de modelo e documento próprios. Consultas ao banco
confirmaram hashes das duas versões, vínculo com a versão compatível e manutenção
do head inicial. Não se reutilizou uma candidata como origem de outro caso.

| Caso | Operação no editor real | Callback terminal normal | Persistência |
| --- | --- | --- | --- |
| `no_edit` | Abrir e fechar sem edição | 4 | Sem candidata normal; snapshot de inspeção importado como `INSPECTION_ONLY`, em quarentena |
| `edit_text` | `MARCADOR_EDITAVEL_LOCAL_1` → `MARCADOR_EDITADO_LOCAL_1` | 2 | Candidata normal em quarentena; head inalterado |
| `edit_table` | `Célula sintética 0-1-0` → `Célula editada LOCAL 0-1-0` | 2 | Candidata normal em quarentena; head inalterado |

Os callbacks foram confirmados em `document_callback_receipts`, não inferidos
apenas de nomes de eventos. Edições registram `EDITOR_VERSION_QUARANTINED`.
O editor normal manteve `download:false` nos três casos.

Nos casos editados, `engine.docx` foi adquirido da URL assinada do callback após
ACK do backend; `callback.docx` veio da versão persistida, por API administrativa.
Os dois arquivos são byte a byte iguais. Não são duas exportações independentes
antes do callback. A reabertura usa inspeção administrativa somente leitura,
com permissão própria de exportação e trilha de auditoria. No caso sem edição,
o snapshot também depende dessa inspeção. Isso não equivale a uma campanha de
aceitação completa sob permissões normais de produção.

## Campos, conteúdo e PDFs

Todas as oito saídas DOCX adquiridas — duas sem edição e três em cada caso editado —
mantiveram os três SAVEDATE reconhecidos pelo Word como tipo 22, sem erro de campo.
PAGE e NUMPAGES foram reconhecidos como tipos 33 e 26, respectivamente, nos três
rodapés. Instruções e resultados foram inventariados antes da atualização explícita,
depois dela e depois da geração do PDF. Originais permaneceram byte a byte intactos.

Corpo e tabelas coincidiram com a edição única esperada em cada caso; 29 parágrafos
no inventário semântico de origem e candidata. Cada saída manteve três tabelas,
duas quebras explícitas de página e bytes da imagem idênticos aos da origem.
Todas as saídas mantiveram três
páginas. Inspeção visual das páginas completas e comparação de todos os pixels,
sem máscaras, não identificaram deslocamento de tabela, imagem ou paginação.

Renderização: Word, atualização explícita de todas as histórias, PDF via `SaveAs2`,
raster RGB a 144 dpi, 1224 × 1584 pixels por página. Esse método atualiza novamente
o resultado SAVEDATE para o horário da exportação PDF, inclusive na referência,
sem salvar o DOCX. Portanto, comparar o cache anterior com o texto do PDF exige
distinguir esses dois momentos. Zero pixels não significa data original preservada.

| Comparação | Pixels diferentes, páginas 1 / 2 / 3 | Explicação |
| --- | --- | --- |
| Original × cópia compatível, antes do motor | 0 / 0 / 0 | Adaptação isolada sem divergência visual nesta fixture |
| `no_edit`: referência pareada × motor | 0 / 0 / 0 | Mesmo resultado visual na exportação pareada |
| `no_edit`: referência pareada de reabertura × reaberto | 0 / 0 / 0 | Mesmo resultado visual na segunda exportação pareada |
| `edit_text`: referência × motor, callback e reaberto | 5942 / 0 / 0 em cada estágio | Substituição do marcador e redistribuição horizontal dos caracteres na mesma linha, caixa `[386,265,948,286]` |
| `edit_table`: referência × motor | 1910 / 0 / 0 | Texto alterado na célula, caixa `[249,341,455,358]` |
| `edit_table`: referência × callback e reaberto | 2041 / 131 / 131 em cada estágio | Mesma alteração de célula mais minuto do SAVEDATE, `7:58` → `7:59` |

Coordenadas são pixels, origem superior esquerda. A diferença temporal do último
caso ocupa `[479,129,490,145]` em cada página: 131 pixels por página, 393 no total.
Motor e reaberto da edição de texto tiveram zero pixels entre si. No caso sem
edição, comparar o reaberto com a referência anterior, em vez da pareada, produz
124 pixels por página, total 372: minuto `7:51` → `7:52`, caixa `[479,129,489,144]`.
Essas diferenças foram conservadas e explicadas; nenhuma região foi excluída.

## Rejeições continuam válidas

Preservação de SAVEDATE não prova equivalência estrutural do pacote inteiro.
O analisador ainda encontra `word/_rels/comments.xml.rels` sem a parte proprietária,
uma nova relação interna quebrada, partes adicionadas/removidas e diferenças de
formatação explícita em 1982 / 1979 / 1976 caracteres, conforme o caso. Não se
concluiu equivalência geral de estilos a partir da aparência desta fixture.

As três instruções SAVEDATE permaneceram exatamente iguais. A igualdade estrita
do conjunto de instruções retorna falsa porque o motor acrescenta `\* MERGEFORMAT`
a PAGE e NUMPAGES, originalmente sem esse switch. Word reconheceu os tipos e os
resultados; essa observação não habilita uma exceção geral no gate.

Controles reais de inspeção nos três casos: fonte válida 200, capability adulterada
403, abertura anônima 401, fonte revogada 403 e tentativa de promover/restaurar
quarentena 409. Auditoria registrada e head preservado em todos.

## Validação e limites

- Python: `python -m unittest discover -s experiments/b05-savedate-preservation/tests -v`:
  15 testes aprovados; inclui preservação, recusa de entradas inseguras, preparação
  privada, origem distinta e proibição de sobrescrita.
- Node: `node --test experiments/b05-savedate-preservation/tests/local_provision.test.mjs`:
  5 testes aprovados; duas versões, restrição loopback, adulteração, head divergente
  e repetição de provisionamento. São testes de transporte; não substituem os
  controles reais descritos acima.
- Sintaxe Python/Node validada. `git diff --check` executado no encerramento.
- Suíte Java anterior: 277 aprovados e 1 ignorado; não repetida, pois o backend e
  seus contratos não foram alterados nesta rodada. Não houve frontend de produto,
  banco ou dependência nova. Nenhuma alegação de nova validação Java.
- F9 nativo não foi alterado nem retestado nesta rodada. O erro anterior continua
  pendente; correção do parser e avaliador exige alteração própria no motor.
- Não houve requalificação de modelos oficiais nem integração de SDKs adicionais.

Evidências em `.private/savedate-local-integration-20261024-01`: plano, hashes,
recibos de provisionamento, callbacks, controle de banco, dump privado, inventários
Word, 14 PDFs, diferenças de todas as páginas, imagens de revisão, logs e estado
anterior do checkout. Binários, identificadores e hashes privados ficam fora do Git.
Ao encerrar, API, proxies, UI e redirect de login foram parados; contêineres da
rodada ficaram parados com volumes preservados. Nenhuma das portas locais da
bancada permaneceu em escuta. O dump privado foi congelado antes do encerramento.
