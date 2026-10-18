# B05.0.4 — Estado da pré-qualificação SuperDoc × Apryse

**Decisão: `BLOCKED_ENVIRONMENT` para ambos os SDKs; nenhum editor selecionado.**

Esta entrega inclui instrumentação e análises privadas da origem, mas não realiza homologação. O ambiente executor não possui os SDKs instalados, e as tentativas de consultar `registry.npmjs.org` falharam por indisponibilidade de resolução de rede. A edição/exportação em navegador fica **NOT-RUN**, nunca `PASS` ou `NO-GO` dos motores.

## Evidências executadas

- Fonte empacotada extraída com histórico `.git`, checkout limpo em `d03c495`; branch nova derivada sem modificar branches anteriores. Nenhum código de produto, Flyway, Entra, backend real ou modelo original modificado.
- Inventário privado dos dois DOCX recebidos: relatório semanal com uma tabela longa e mídia; AV3 com campos `SAVEDATE`, `NUMPAGES` e `PAGE`, sete tabelas e mídia. O PDF de referência do AV3 tem 5 páginas e data legível; a origem DOCX oferece uma divergência de renderização no `SAVEDATE` já observada no corpus. Nenhum desses achados é resultado de motor novo.
- Verificador OOXML conservador detecta campo, alteração em `w:pPr` de 8 para 0 pt, ausência de parte estrutural, alteração textual fora do roteiro e ZIP duplicado. **8 testes unitários aprovados no ambiente local**, inclusive regressões de tabela e relacionamentos OOXML.
- Vite shell implementado por interfaces oficiais documentadas (SuperDoc/Apryse), não executado nem compilado por falta de download dos pacotes. Não se atribuem capacidades experimentais a essa implementação não validada.

## Matriz de execução

| Gate | SuperDoc | Apryse | Evidência |
|---|---|---|---|
| A — dependências efetivas e licença | BLOCKED | BLOCKED | npm sem rede; Apryse add-on/trial não verificado |
| B1 — abrir/exportar no-op | NOT-RUN | NOT-RUN | Shell preparado, motor ausente |
| B2 — edição XYZ e reabertura | NOT-RUN | NOT-RUN | Sem editor instalado |
| B3 — SAVEDATE/PAGE/NUMPAGES | NOT-RUN | NOT-RUN | Somente inventário offline |
| B4 — espaçamento e tabelas | NOT-RUN | NOT-RUN | Regressão do verificador, não SDK |
| C — DOCX oficiais + controle Word | BLOCKED | BLOCKED | Exige Gate B aprovado; privados intocados |
| D — UX, colaboração, CAS, direitos | BLOCKED | BLOCKED | Exige Gate C aprovado |
| E — seleção de motor | INCONCLUSIVE | INCONCLUSIVE | Não há prova experimental de compatibilidade |

## Critério para desbloquear

1. Em máquina autorizada com internet, instalar versões fixadas de `superdoc` e `@pdftron/webviewer`; conferir versão e build, compilar `web`.
2. Obter trial/licença Apryse DOCX Editor e esclarecer telemetria para os documentos institucionais **antes do teste C**.
3. Executar B1–B4 com arquivos sintéticos, registrar exportações privadas e aferir os resultados no Word; caso aprovado, executar C em cópias oficiais e os controles editoriais.
4. Emitir novo relatório/ADR de comparação com bytes e screenshots conferidos, sem integrar automaticamente o produto.

**Conclusão:** a infraestrutura de comparação e a caracterização local dos modelos foram entregues; o ensaio de motores ainda não foi executado. Não transformar falta de ferramenta em veredito técnico falso.
