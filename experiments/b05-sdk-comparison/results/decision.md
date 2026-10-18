# B05.0.4 — resultado da pré-qualificação SuperDoc × Apryse

Execução em 18/10/2026, America/Bahia. Base d03c495 do PR #12; branch
spike/b05-sdk-prequalificacao. O [relatório recebido](initial-delivery.md) foi
preservado como histórico. A falta de rede/npm daquele ambiente não se reproduziu:
as dependências existem, foram instaladas e o código foi compilado.

**Decisão: nenhum SDK adotado. SuperDoc não aprovado nesta configuração para os
modelos oficiais; Apryse INCONCLUSIVE em avaliação.** Há evidência positiva de
preservação DOCX/campos em ambos, mas não há base para afirmar fidelidade completa,
paridade SharePoint ou descarte universal dos dois produtos.

## Ambiente e correções de integração

- SuperDoc 2.23.0, licença declarada AGPL-3.0; Apryse WebViewer 12.1.0. Integridades
  do registry e versões instaladas verificadas; package-lock versionado.
- Node 24.19.0, Vite 7.3.7, TypeScript 5.9.3, python-docx 1.2.0, Chromium 1243.
  Word 16.0 build 16.0.17932 é a referência. Sem Collabora/LibreOffice novos.
- O TypeScript rejeitou initialMode em loadDocumentOptions do Apryse. Foi movido
  para o construtor suportado. Houve também falha de Blob URL e corrida com o
  documento vazio inicial. O host passou a usar initialDoc com URL própria,
  mantida até destruir o SDK. Exports vazios e timeouts de setup foram excluídos
  da avaliação de fidelidade; não são falhas comprovadas de conteúdo do motor.
- Instâncias anteriores são descartadas na reabertura. Controles ficam bloqueados
  durante operações e o nome de export usa o SDK realmente aberto, não a opção
  posteriormente selecionada. O runner comprovou reabertura na mesma página.
- SuperDoc com telemetria desativada; origem loopback, CSP e bloqueio de egress
  HTTP/WebSocket nos ensaios. Não foram observadas tentativas externas nos casos
  válidos. Isso não substitui G1 ou avaliação jurídica/operacional.

## Gate e evidências sintéticas

O gate inicial tinha oito testes e rejeitava mudanças lexicais/instrumentais.
A investigação identificou numeração XML reserializada com árvore idêntica,
adição de IDs de parágrafo e segmentação de texto em runs sem formatação direta.

A [política documentada](../scripts/comparison-policy.md) conserva o default
estrito e oferece duas equivalências opt-in, com fonte normativa, caso mínimo
independente e negativas. IDs existentes, referências, duplicatas, intervalos
inválidos, formatação, campos, tabelas e mídias continuam protegidos. A candidata
não é reescrita. Não há regra que aceite todas as diferenças exportadas.

Os testes cresceram para 17, todos aprovados após as correções da revisão. O novo controle
estendido tem SAVEDATE simples/complexo, PAGE, NUMPAGES, tabela, logo fonte-livre,
8 pt e quebra explícita para duas páginas. As fixtures e seus hashes ficam privados.

| Ensaio | SuperDoc | Apryse | Limite da evidência |
|---|---|---|---|
| Instalar e compilar | PASS | PASS | Versões fixadas, sem suppressões de tipo |
| Abrir e exportar no-op básico | PASS | PASS de transporte | Candidatas reais; export não significa persistência NITE |
| Inserir XYZ, reabrir e remover | PASS | NOT-CONFIRMED | No Apryse a automação não alterou o texto; não confundir com incapacidade de edição |
| Gate estrutural básico/ciclo | PASS com políticas opt-in | FAIL de triagem | Apryse altera estrutura e acrescenta marca de avaliação; sem diagnóstico universal de corrupção |
| SAVEDATE simples/complexo no Word | PASS | PASS no no-op | Tipos 22 preservados e Update válido; cálculo ao vivo no navegador não homologado |
| PAGE / NUMPAGES no Word | PASS | PASS no no-op | Tipos 33/26; controle estendido repagina em duas páginas |
| 8 pt, tabela, logo sintético | PASS | Parcial | 8 pt/páginas/campos conferidos em Apryse, mas layout/estrutura completos não aprovados |
| Edição/reversão estendida | PASS | NOT-RUN | SuperDoc com export e reabertura, sem reparo de XML |

Apryse emitiu aviso de trial de sete dias, sem limitações de recursos além da
marca de avaliação. Não foi fornecida nem adquirida chave. As tentativas de
digitação por teclado ficaram sem efeito no arquivo; o problema pode ser do
alvo da automação/integração. Não atribuir isso ao fornecedor sem reprodução
interativa adequada. Os testes Apryse foram exclusivamente sintéticos.

## C0 oficial SuperDoc — resultado parcial e parada

B sintético e contenção de rede permitiram C0 em cópias locais dos dois DOCX
autorizados. Os originais não foram editados. Foi criado congelamento novo por
hash: o manifesto privado histórico B05.0.3 não está disponível neste checkout,
portanto seus 177 hashes não foram revalidados nesta rodada.

| Documento | Word da origem | Word da candidata | Prévia SuperDoc |
|---|---:|---:|---:|
| AV3 | 5 | 5 | 6 |
| Relatório semanal recebido | 7 | 7 | 7 |

A paginação AV3 de seis páginas foi observada em duas sessões novas, a segunda
com oito segundos de estabilização. Não é contagem de PDF nem porcentagem de
fidelidade. C1 e ciclos oficiais seguintes não foram iniciados.

Nos dois C0, texto por parágrafo/célula, mídias e métricas de parágrafo no Word
coincidem. AV3 manteve SAVEDATE/PAGE/NUMPAGES tipos 22/33/26, sem erro após Update.
O relatório manteve seus quatro campos tipo 88. Isso é evidência favorável ao
arquivo exportado, sem equivaler à aprovação da experiência de edição.

Todos os PDFs Word da origem/candidata foram renderizados. As sete páginas do
relatório coincidem pixel a pixel. No AV3, cada página difere apenas na região
da hora do SAVEDATE: as exportações Word ocorreram às 04:12 e 04:13. Isso não é
mudança de layout atribuível ao SDK. As doze páginas de referência foram
inspecionadas; não houve evidência nova de sobreposição/cortes na saída Word.
O PDF AV3 fornecido também foi confirmado como referência estática de cinco
páginas; não representa campo dinâmico executável.

O gate C0 continua FAIL: SuperDoc removeu word/footnotes.xml, word/endnotes.xml,
footnotePr/endnotePr e relações correspondentes. A equivalência dessa remoção
não foi certificada. Não se afirma perda de notas reais. Não se adicionou uma
exceção para fazer esses modelos passarem. Além disso, a prévia de seis páginas
do AV3 impede homologar a fidelidade do editor.

## Matriz final de gates

| Gate | SuperDoc | Apryse |
|---|---|---|
| A — disponibilidade técnica / contenção local | PASS local; licença de produto pendente | PASS de trial local; licença limpa pendente |
| B — conjunto sintético | PASS sob políticas documentadas | INCONCLUSIVE: trial/estrutura e edição sem confirmação |
| C — modelos oficiais | FAIL em C0; Word preservado, preview/estrutura pendentes | BLOCKED; nenhum modelo oficial carregado |
| D — UX completa, colaboração, CAS, direitos/operação | BLOCKED | BLOCKED |
| E — seleção para NITE | NÃO APROVADO nesta configuração | INCONCLUSIVE |

A bancada não possui servidor de versionamento, CAS, autenticação institucional
ou PDF persistido do NITE. Cópia Word preservada não demonstra esses contratos.
O export do SuperDoc aqui é DOCX; a [documentação oficial](https://docs.superdoc.dev/editor/export-options/)
não promete conversão PDF por esse método. Uma futura arquitetura de PDF precisa
de avaliação própria. A disponibilidade de APIs Apryse não foi confundida com
licença ou homologação de todas as suas funções.

## Entrega e próximos passos

Recomendação focal: investigar a página extra AV3 no SuperDoc e certificar as
partes de notas removidas, mantendo os modelos intactos. Em paralelo decisório,
viabilizar Apryse sem as limitações de avaliação e concluir edição/reversão com
prova de texto, antes de qualquer ensaio institucional. Não instalar outro
fornecedor, trocar versões aleatoriamente ou integrar produto por inércia.

Validações locais: 17 testes Python, TypeScript, Vite build e sintaxe do runner;
runners reais e Word/PDF descritos acima. Os resultados Word/gate não viram
aprovação apenas porque o build passa. Testes Java/Flyway/PostgreSQL não se
aplicam: nenhum backend mudou. [Reprodução](../scripts/EXECUTION.md).
Revisão independente identificou três pontos importantes, corrigidos: hashes de todas as partes binárias, espaços significativos em XML e timeout/descarte da instância SuperDoc após falha. Regressões negativas reproduziram os falsos PASS antes das correções. A normalização também preserva mixed content inesperado. A suíte final e a reavaliação das seis candidatas sintéticas confirmaram os PASS anteriores; os dois C0 oficiais continuam FAIL. CI remota será vinculada no PR.
G1/G2 e aceite institucional permanecem pendentes.
