# B05.0.1 A — baseline e instrumentos

17/10/2026. Base `360fc24d6a8d3203e06db5faf219105c3576e2ac`, PR #9 aberto,
CI sintética aprovada nos dois eventos (push e pull_request). Pesquisa em
`spike/b05-compatibilidade-docx`, derivada da spike; o PR novo deve apontar à
branch do PR #9. Nenhum merge autorizado.

## Congelamento verificado

- Três originais autorizados conferidos contra o manifesto de importação: dois
  DOCX e um PDF. Nenhum hash ou caminho pessoal entra neste relatório.
- 29 checkpoints históricos auditados contra `inventory.json` e os bytes da
  respectiva versão imutável no filesystem: 16 CODE e 13 ONLYOFFICE, incluindo
  controles/intervenções complementares. Os 18 checkpoints centrais da auditoria
  anterior estão contidos nesse conjunto; não são 29 ensaios independentes.
- Dez arquivos dos controles Word congelados por hash privado: quatro DOCX e
  seis PDFs. A auditoria detalhada está em `.runtime/b0501/baseline-private.json`.
- Word local 16.0, build 16.0.17932; referências anteriores continuam específicas
  desse ambiente. Os testes da API principal não foram repetidos.
- Imagens e arquitetura em `b05-compatibility-baseline.json`. O digest ONLYOFFICE
  usado originalmente é um índice OCI; o digest amd64 foi resolvido e registrado
  separadamente. Não corresponde a uma atualização do motor.

## Instrumentação

`scripts/compatibility.py` inventaria instrução, tipo, resultado em cache,
representação simples/complexa e lock dos campos; texto por parágrafo e story;
propriedades de parágrafos/runs, tabelas, seções, settings/styles e relações.
Não resolve todos os estilos condicionais de tabela nem executa o layout do Word.
Diferenças estruturais são pistas, não causas automaticamente comprovadas.

Classes: `unchanged`, `expected-edit`, `semantic-regression`,
`layout-regression`, `unverified`. Mudança de cache/máscara exige validação; IDs,
partes adicionais e relações precisam de inspeção de uso. O inventário não busca
links, atualiza campos, executa macros nem modifica pacotes. Saídas são privadas.

Testes sintéticos cobrem equivalência simples/complexa, fragmentação de instrução,
locks equivalentes, preservação de whitespace em pictures, cache, edição esperada,
novas partes, entidades UTF-16 e ZIP com entradas duplicadas. RED observado antes
da implementação; suíte conjunta: 25 testes Python aprovados.

`word_layout_probe.ps1` abre cópias em leitura e exporta PDF/metadados privados.
Atualiza apenas SAVEDATE/REF locais quando solicitado. Registra índices/páginas e
tamanho efetivo de fonte, restaura opções e não salva a fonte.

## Ocorrências de instrumentação excluídas das conclusões

A primeira redução com styles/settings originais reteve referências a separadores
de notas cujas partes foram removidas. O Word rejeitou essa fixture. Retirar apenas
essas referências tornou a redução válida; há regressão unitária e controle Word.
Não é falha atribuída ao motor. A fixture inválida ficou privada e não foi usada
no ensaio CODE.

Uma tentativa de conversão apontou para outro dataset e recebeu 404; outra começou
antes de o host estar pronto. Saídas incompletas foram preservadas e excluídas.
Novas capturas usam destinos novos e a versão imutável do dataset correspondente.
O probe de importação precisa aguardar a anexação dos objetos do cabeçalho; ausência
de match é `inconclusivo`, nunca prova de que o campo não existe.

A auditoria final corrigiu dois fingerprints descritivos em manifests privados,
copiados antes de alterar fixtures diagnósticas (redução sem notas e controle de
docDefaults). Os manifests anteriores foram preservados. Esses campos não eram
consumidos pelo runner; os 23 checkpoints foram conferidos contra os bytes reais
das fixtures/versões pertinentes. Nenhuma captura incorreta foi promovida a evidência.

Aceite A técnico: atendido. Aceite editorial/institucional não concedido por este
inventário. O plano completo está em `../../../docs/implementation/bloco-05-compatibility-plan.md`.
