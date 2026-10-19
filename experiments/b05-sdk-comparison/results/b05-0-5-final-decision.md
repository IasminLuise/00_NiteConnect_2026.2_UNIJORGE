# B05.0.5 — decisão focal e matriz F01–F28

19/10/2026. Base PR #13, SHA `4606fe5`. Branch `spike/b05-sdk-diagnostico`,
PR empilhado contra `spike/b05-sdk-prequalificacao`, sem merge. Commits de
instrumentação `149b60d` e regressões iniciais `38c1edf`; SHA final e CI constam
no PR. Não há alteração em produto, API, Flyway, infra, .env ou versões dos SDKs.

| Candidato | Decisão | Motivo |
|---|---|---|
| SuperDoc 2.23.0 | **NO_GO_NA_CONFIGURACAO_ENSAIADA** | página 2 sem conteúdo editorial desloca título para página 3; seis contra cinco; equivalência das notas não certificada |
| Apryse 12.1.0 | **INCONCLUSIVO / INCONCLUSIVE_AUTOMATION** | inserção XYZ e persistência comprovadas, reversão deixa YZ; trial/estrutura e licença continuam bloqueando fidelidade |

Nenhum `APTO_PARA_HOMOLOGACAO_APROFUNDADA`. Conclusão da investigação não
seleciona SDK. G1-F/G1 continuam não aprovados e G2 fechado.

Raiz privada dos caminhos abaixo: `experiments/b05-sdk-comparison/.private/b0505/`.
Hashes dos documentos, screenshots, textos e manifestos permanecem locais.

| ID | Estado | Resultado / evidência privada |
|---|---|---|
| F01 | PASS | Git limpo inicial, head PR13, lock inalterado; baseline-freeze/session-manifest |
| F02 | PASS focal | seis sessões, nenhuma tentativa externa observada; browser/*/result.json |
| F03 | PASS de medição | seis páginas estáveis; av3-diagnostic-1/pagination.json |
| F04 | PASS de reprodução | nova sessão confirma seis e fronteira inicial; av3-diagnostic-2 |
| F05 | PASS focal | parágrafo OOXML15 / Word22, título página3 vs2; av3-text-position.json |
| F06 | INCONCLUSIVE | papel/margens/fontes CSS inventariados; arquivo efetivo por glifo/causa interna não demonstrados |
| F07 | NOT_RUN | nenhum ajuste suportado isolado demonstrado; nenhuma variante A/B |
| F08 | FAIL | sexta página e distribuição incompatível, independente da exportação Word |
| F09 | FAIL estrutural | no-op Word5/campos22,33,26 atualizáveis; remoção de partes mantém bloqueio |
| F10 | PASS de inventário | apenas separadores, zero notas regulares/referências; notes-inventory/notes-av3-v2 |
| F11 | INCONCLUSIVE | scanner sintético positivo/negativo; sem certificação Word de equivalência ou preservação de notas reais pelo SDK |
| F12 | PASS de proteção | gate não afrouxado; testes anteriores e negativa de conteúdo executados |
| F13 | PASS focal / licença limitada | OfficeEditor=true/readOnly=false, UI Editing; apryse-inspected-noop |
| F14 | PASS transporte | no-op texto original e hash; apryse-inspected-noop/candidate.docx |
| F15 | PASS inserção | correção de foco gera alteração real; apryse-cycle-2-edit |
| F16 | PASS corpo focal | prefixoXYZ só no primeiro alvo, restantes textos iguais; apryse-body-proof.json / Word |
| F17 | INCONCLUSIVE_AUTOMATION | A reabre, mas B contém YZ; Word e prova de corpo recusam reversão |
| F18 | NOT_RUN positivo | primeiro ciclo falhou na interação; só segundo inseriu; sem dois ciclos positivos |
| F19 | PASS parcial / NOT_RUN | SAVEDATE/PAGE funcionais; NUMPAGES não ensaiado novamente na fixture básica |
| F20 | FAIL global | candidatas trial continuam reescrevendo estrutura; nenhum PASS de fidelidade |
| F21 | PASS de identificação | marca d'água/mídias/partes extras identificadas, sem bypass/equivalência |
| F22 | PASS regressões | 19 testes Python + cinco controles Node; residualYZ não é reversão válida |
| F23 | PASS local | tsc+vite build; CI final vinculada no PR, não homologação Word |
| F24 | PASS local | ignore/diff sem privados, binários ou chave; revisão independente no PR |
| F25 | BLOCKED produto | licenças comerciais/direitos institucionais não aprovados; trial não autoriza produção |
| F26 | PASS registro | baseline, relatórios focais e adendo ADR; pressupostos separados dos fatos |
| F27 | PASS decisão | pareceres separados acima; nenhum ranking ou descarte universal |
| F28 | PASS encerramento local | originais/histórico verificados, servidor encerrado; PR aberto sem merge |

F03/F04/F10/F21 são PASS de **observação**, não de compatibilidade. F17 não é
falha nativa comprovada do motor. F23/F24/F28 devem ser lidos com os resultados
finais de revisão/CI no PR, sem confundir revisão readonly com review formal GitHub.

Orçamento consumido: dois C0 SuperDoc; nenhum ajuste candidato. Um no-op
Apryse inspecionado, primeiro ciclo interrompido por clique interceptado, um
ajuste de foco e segundo ciclo com inserção/reabertura/remoção parcial. Nenhum
terceiro ciclo, modelo oficial Apryse, C1/C2 oficial, fornecedor antigo ou upgrade.

Recomendação: encerrar SuperDoc nesta configuração. Apryse demonstrou edição,
mas requer decisão institucional sobre avaliação/licença e autorização de um
protocolo de reversão controlada antes de qualquer homologação oficial.
Custos da próxima decisão: licença/termos de avaliação e uma investigação de
foco/temporização com controles independentes; valores comerciais não cotados.
Não iniciar B05.0.6 ou outra shortlist automaticamente.

Validação nova: 19 testes Python, cinco controles Node, sintaxe dos runners,
TypeScript/Vite e Word readonly focal. Build final é reutilizável após mudanças
apenas em relatórios/scripts Python/PowerShell. Não foram repetidos os testes
Java/Flyway/Blocos01–04: contratos e backend não mudaram. Nenhuma suíte
estrutural comprova, sozinha, fidelidade visual ou direitos de distribuição.

[Baseline](b05-0-5-baseline.md), [paginação](b05-0-5-superdoc-pagination.md),
[notas](b05-0-5-superdoc-notes.md), [Apryse](b05-0-5-apryse-editing.md),
[protocolo](../scripts/B0505-PROTOCOL.md).

