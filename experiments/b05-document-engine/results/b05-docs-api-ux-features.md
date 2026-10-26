# B05.0.3 — experiência observada e limites

Apenas o wrapper mínimo necessário a A/B foi exercitado: embed, pt-BR, toolbar
compacta, abrir/encerrar, readonly, estado externo, download de candidata rejeitada
e reabertura por chave nova. Screenshots privados acompanham os checkpoints.
Readonly abriu e sua configuração negou edição; callbacks de escrita indevida
são recusados nos testes de host. Isso não é homologação completa de permissões.

“Salvo no NITE CONNECT” depende do estado persistido pelo host, nunca de
`onDocumentStateChange(false)` nem do indicador nativo. A candidata recusada
continua identificada como não confirmada, inclusive após reinício.

Gate D **NOT-RUN**, por B reprovado: plugin, histórico visual completo, temas,
mobile, comentários/revisão, dois coeditores, revogação, acessibilidade completa
e UX de biblioteca. O editor pronto continua com interface/marca do fornecedor;
não foi prometido canvas React substituível. A
[tabela oficial API/WOPI](https://api.onlyoffice.com/docs/docs-api/using-wopi/api-vs-wopi)
lista capacidades diferentes; sua existência documental não comprova licença ou
execução desses recursos no laboratório. Nenhuma conclusão de “equivalência ao
SharePoint” é autorizada por estes ensaios.
