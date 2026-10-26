# Protocolo obrigatório — B05-M2P8 + M2P9

O executor Windows deve operar com **os DOCX originais autorizados**, sem renderizar um documento artificial no lugar deles. Dois modelos distintos: `Plano de AV3 - MODELO.docx` (referência PDF original pode acompanhar) e `RA_seunome_completo - Relatório Individual Semanal.docx`. **Não alterar originais**; operar exclusivamente sobre cópias isoladas. Dados pessoais presentes no relatório semanal precisam de autorização/controle de acesso; não publicar PDF, DOCX, diffs ou capturas em repositório.

## Preparação

1. Registrar procedência e autorização dos dois modelos em recibos `official_source` (hashes originais conferidos). Documentar edição das cópias permitida e limites de exposição de dados.
2. No Word Desktop, abrir cada DOCX *somente leitura*, gerar PDF de referência e fechar sem salvar. Guardar versão/build, opções de atualização de campos e fontes instaladas. O PDF do AV3 fornecido pelo usuário pode ser **referência auxiliar**, mas não deve ser confundido com a exportação Word da mesma cópia por esta bancada.
3. Rodar `prepare.py` apontando aos **dois DOCX reais** e PDFs produzidos do Word. Registrar SHA-256 das cópias e preencher `official_source_receipt` no manifesto.
4. Em seis sessões separadas (cada uma com sua cópia e identificador de documento), executar no ONLYOFFICE e no backend do NITE CONNECT, respeitando permissões **normais**, sem `download:true` extra e sem force-promote.

## Sessões obrigatórias (três para cada modelo)

| Sessão | Ação humana/automatizada comprovável | Evidências |
|---|---|---|
| `no_edit` | Abrir, inspecionar todas as páginas, fechar sem alterar; callback esperado 4 | Captura de snapshot do motor por caminho *autorizado*, reabertura e PDFs Word de referência/candidata; **não inventar callback** |
| `edit_text` | Editar apenas uma ocorrência inequívoca de texto em campo operacional de uma cópia; registrar `before` / `after` | Saída do editor, callback autenticado status 2, cópia persistida QUARANTINED, reabertura, PDFs dos três estágios |
| `edit_table` | Modificar exatamente uma célula de uma tabela real, sem editar o restante; registrar `before` / `after` | Mesmas evidências, mais verificação visual da tabela completa e quebras de página |

### Particularidades dos modelos

- **Relatório Individual Semanal:** validar a tabela `Projeto / Atividade / Período / Descrição do que foi feito / Evidência / Status / Observações`, observando quebra/continuação da tabela em páginas subsequentes. Se a edição exigir criar linhas, fazer um teste adicional declarado, pois os três cenários obrigatórios são edições textuais simples e não autorizam tratar inserção de nova linha como comparação textual simples.
- **Plano de AV3:** validar identificação, seções, cronograma com colunas semana inicial/final, cabeçalho e paginação `Págs.`; verificar que a referência PDF enviada tem cinco páginas, mas distinguir paginação original da exportação pelo Word nesta mesma campanha.
- **Ambos:** cabeçalhos/rodapés, campos dinâmicos, estilos efetivos em Word, notas, referências OPC, mídia, tabelas, páginas e conteúdo integral. Não mascarar `SAVEDATE`, cabeçalhos ou regiões da edição. Registrar quando a diferença é esperada (revisão) e quando é inesperada.

## Reabertura e proveniência

Para cada sessão, obter `engine_docx` de mecanismo técnico **autorizado** (sem alterar permissões da API), `callback_docx` real para editados, e `reopened_docx` obtido após carregar a candidata persistida novamente no editor. Os três estágios precisam de arquivos físicos independentes, e também de seus PDFs emitidos pelo Word sem salvar os DOCX. No `no_edit` não existe callback de candidata: **não inventar**. Se o ambiente normal não permite exportar snapshot do motor, registrar o bloqueio como pendência; não reexecutar com configuração relaxada para fabricar PASS.

O recibo de aquisição deve conferir os SHA-256 de cada DOCX capturado, identificar o método, versão do motor, operador, callback terminal e `normal_permissions=true`, `permission_override=false`. O recibo de backend precisa confirmar Entra, RBAC, callback assinado, versão em quarentena, head preservado, auditoria e recuperação; não tratar histórico de teste sintético como novo recibo de integração normal.

## Critérios de produto e encerramento

- **Risco impeditivo:** texto não solicitado alterado/perdido, tabela quebrada, campo crítico perdido, corrupção ou impossibilidade de reabrir, alteração visual rejeitada na revisão ou insegurança de acesso/persistência. Mesmo uma falha pode demandar investigação de impacto; reprovações editorial e técnica independentes do mesmo caso permitem recomendação NO-GO.
- **HOLD:** artefato, autorização, PDF Word, revisão, evidência estrutural, snapshot de reabertura ou integração normal faltante; não converter em PASS.
- **Elegível à decisão formal:** todos os seis cenários completos e aceitos, origem íntegra, sem risco pendente, integração normal atestada, pareceres técnico/editorial registrados. **Não equivale a GO institucional automático.**

Somente o órgão institucional poderá homologar e autorizar os gates; o avaliador não altera `canPromote`, `officialApproved`, G1/G2/M3 ou regras de publicação.
