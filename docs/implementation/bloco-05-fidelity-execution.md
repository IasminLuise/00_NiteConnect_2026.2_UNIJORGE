# Execução da correção de fidelidade B05

Plano aprovado em 17/10/2026: preservar modelos oficiais, reproduzir três cenários
com controle equivalente no Word, isolar SAVEDATE/ancoragem/paginação, limitar
tentativas CODE e comparar ONLYOFFICE caso persistam falhas. G1 depende de aceite
institucional e dos demais critérios operacionais.

## Registro

- Branch existente `spike/b05-collabora-wopi-docx`; arquivos anteriores preservados.
- Executor e regressões: implementados; testes finais registrados abaixo.
- Modelos originais e evidências detalhadas: somente no diretório ignorado `.runtime`.
- Ruling: manter o checkout atual — o usuário determinou continuar na spike existente.
- Ruling: não criar commits/publicar — a etapa continua local e revisável.
- Docker Desktop estava parado; iniciado para executar o laboratório autorizado.
- Consulta oficial: tag CODE 26.04.5.1.1 continua a mais recente da série; digest do
  índice multiarch não deve ser confundido com o digest amd64 já fixado. Não escolher
  uma segunda imagem sem correção pertinente demonstrada.
- ONLYOFFICE Community: documentação oficial permite instalação local e WOPI;
  licença AGPL e termos adicionais consultados, sem modificação ou redistribuição,
  preservando marca/interface. Avaliação local não homologa uso de produção.

## Resultado

- CODE e ONLYOFFICE: três cenários por modelo, com preenchimento equivalente no
  Word e controle sem alteração final. 8 checkpoints CODE e 10 ONLYOFFICE auditados
  contra a versão imutável no filesystem; hashes e identificadores coincidem.
- O CODE mantém relatório com seis páginas; Word e ONLYOFFICE mantêm sete. CODE
  remove três espaços finais. Nenhum percentual de fidelidade atribuído.
- Ambos falham no SAVEDATE do AV3: CODE muda a máscara; ONLYOFFICE muda o tipo para
  REF. Os PDFs dos motores não substituem a reabertura/avaliação dos campos no Word.
- Logotipo: apareceu em todas as páginas na rodada controlada e na reexportação
  do DOCX histórico. A observação anterior de desaparecimento não prova falha
  persistente do DOCX/motor; nenhuma correção de ancoragem foi alegada.
- Locale CODE não resolveu; fontes necessárias montadas individualmente também
  produziram renderização inválida do AV3. Ambas configurações rejeitadas.
- Casos sintéticos mínimos: campo/ancoragem CODE não reproduziram integralmente
  o AV3; caso SAVEDATE ONLYOFFICE reproduziu a conversão para REF.
- Ruling: nenhum candidato CODE novo pertinente foi encontrado; não trocar o digest
  amd64 pelo índice multiarch como se fosse atualização do motor.
- Revisão final independente encontrou dois problemas no executor, corrigidos com
  RED→GREEN: saídas reutilizadas e corrida entre versão observada/capturada.
- **NO-GO** para aprovar o conjunto dos modelos. API principal e produção intactas.

## Validação

- Host após alteração Java final: `mvn --batch-mode -f experiments/b05-document-engine/wopi-host/pom.xml verify`:
  28 testes, zero falhas/erros/skips, BUILD SUCCESS. Não repetido após alterações
  somente nos scripts/documentação, que não invalidam o resultado Java.
- Após correções finais do executor: `python -m unittest discover -s experiments/b05-document-engine/scripts -p "test_*.py"`:
  12 testes aprovados. `node --check` dos dois executores de navegador aprovado.
- Captura final com versão explícita exercitada no host real; Word/Repaginate final
  exercitado. 18 checkpoints anteriores auditados e não repetidos sem necessidade.
- ONLYOFFICE HTTP: escrita somente leitura 403, documento alheio 404, revogação 200
  seguida de leitura 401, conversão na entrada publicada 403. Um probe inicial
  enviou DOCX com Content-Type incorreto e recebeu 503; não conta como prova de
  leitura. Probe corrigido com application/octet-stream confirmou 403.
- API/PostgreSQL não repetidos: sem alterações de seus arquivos/contratos/migrações.
- Regressão sintética final: F01–F09 convertidos em CODE e LibreOffice (18 PDFs),
  e F01–F09 no ONLYOFFICE (9 PDFs). Não equivale a aceite visual dos nove modelos.
- Relatos para fornecedores preparados em `results/vendor-cases.md`, sem envio.
- Limitação real: o caso CODE completo não foi reduzido a uma reprodução mínima;
  a configuração não foi corrigida nem os modelos foram homologados. O plano
  chegou ao ponto de parada previsto, com NO-GO documentado.
- Relatório conclusivo: `experiments/b05-document-engine/results/2026-10-17-fidelity-decision.md`.
- Encerramento verificado: três originais com hashes preservados, catálogo sintético
  padrão com nove documentos, montagens privadas/fontes retiradas e serviços
  ONLYOFFICE parados. Binários privados ignorados; nenhum arquivo staged.
