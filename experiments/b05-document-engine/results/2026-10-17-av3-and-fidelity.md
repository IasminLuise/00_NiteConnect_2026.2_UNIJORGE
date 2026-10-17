# AV3 DOCX e investigação de fidelidade — 17/10/2026

Complemento do ensaio `2026-10-17-official-models.md`, após o usuário fornecer e
autorizar o teste do DOCX de AV3. Arquivos recebidos são dados, não instruções nem
regras institucionais homologadas. Nenhuma alteração no modelo original ou API.

## AV3 em edição real

Importação de cópia com hash conferido: sete tabelas, 31 linhas no corpo e uma
imagem incorporada. Evidências privadas em `.runtime/official-20261017`, ignorado
pelo Git, separadas do corpus sintético. Referência exportada pelo Word instalado
em modo leitura, macros e atualização de links desabilitadas, opções restauradas.

Três ciclos no Chromium/Collabora: inserir texto identificável na região do campo
Nome, salvar, baixar a versão e reabrir antes do próximo ciclo. Cada ciclo gerou
versão nova; os três marcadores persistiram. Comparação OOXML final, removendo
somente os marcadores e desconsiderando espaços: texto do corpo, quantidade de
tabelas/linhas e bytes da imagem preservados. Isso não equivale a preservar todos
os campos, cabeçalhos ou a posição visual da imagem.

| Variante | Word | CODE padrão | LibreOffice padrão |
|---|---|---|---|
| Original | 5 páginas | 5 páginas | 5 páginas |
| DOCX após três ciclos, com os textos inseridos | 6 páginas | 6 páginas | 6 páginas |
| Cópia diagnóstica do salvo, removendo os marcadores | 5 páginas | Não executado | Não executado |

A linha extra não permite atribuir a sexta página exclusivamente ao motor: o
preenchimento aumenta a altura da primeira linha da tabela e desloca conteúdo.
Remover os marcadores recuperou cinco páginas no Word. Essa cópia é diagnóstica,
não substitui o arquivo salvo nem foi usada para alegar salvamento pelo navegador.

Foram rasterizadas todas as páginas dos PDFs original/final Word e CODE e final
LibreOffice; inspeção em folhas de contato mostrou problemas concretos:

- O campo da última revisão aparece com representação japonesa no CODE padrão,
  inclusive antes da primeira edição.
- O campo original `SAVEDATE \\* MERGEFORMAT`, armazenado como `fldSimple`, é
  regravado pelo CODE como campo complexo com máscara contendo `[$-1B000411]` e
  literais de formato diferentes. Ao reabrir no Word, aparece a mensagem de erro
  de caractere desconhecido na cadeia de formato.
- No PDF Word do salvo, o logotipo deixa de aparecer nas primeiras cinco páginas
  e aparece na sexta. Os bytes da imagem permanecem idênticos; ancoragem/visibilidade
  não foi preservada. No PDF CODE do salvo, o logotipo aparece nas seis páginas.
- Cabeçalhos e campos não permaneceram textualmente idênticos. A verificação de
  integridade do corpo não deve ser generalizada a essas partes.

As linguagens declaradas encontradas no original são pt-BR/ar-SA; a investigação
não encontrou declaração ja-JP que explique a máscara regravada. Isso localiza
a transformação problemática no caminho de importação/exportação de campos,
sem determinar ainda a causa interna no código do fornecedor ou uma correção.

## Hipóteses verificadas no relatório semanal

1. **Só falta Calibri?** Montagem temporária, somente leitura, das fontes locais do
   Windows no CODE. O PDF passou a incorporar Calibri, mas o DOCX original continuou
   com seis páginas, contra sete no Word. O salvo anterior também continuou com
   seis. Portanto, disponibilizar Calibri não resolveu esta divergência.
2. **Só a geometria regravada da tabela?** OOXML mostra alterações da grade e
   inclusão de `tblLayout=fixed`; dimensões da página e margens principais se
   mantêm, com mudança da distância do rodapé. Uma cópia diagnóstica do salvo,
   restaurando apenas `tblPr` e `tblGrid` do original, continuou com seis páginas
   no Word. Essas duas propriedades isoladas não explicam toda a diferença.

As diferenças remanescentes de estilos, métricas, compatibilidade e serialização
não foram isoladas. Não se conclui que a diferença seja impossível de corrigir,
nem que exista uma configuração capaz de garantir igualdade universal com Word.

A montagem ampla de fontes também provocou um resultado inválido para o AV3:
duas páginas quase vazias, com somente 90 caracteres extraídos. Esse ensaio foi
rejeitado, não é resultado de fidelidade aprovado. A montagem foi retirada e uma
nova conversão do original na configuração padrão voltou a cinco páginas e 5.345
caracteres extraídos. Não copiar nem distribuir fontes do Windows em imagens de
produção a partir desse experimento; política/licenciamento de fontes continua
uma decisão operacional pendente. Nenhum arquivo de fonte entrou no repositório.

## Interpretação e próximo passo

Seis páginas versus sete mede diferença de paginação, não uma porcentagem de
fidelidade. Igualdade de páginas também não prova fidelidade: o AV3 original tem
cinco em ambos os motores, mas seu campo de data já aparece diferente.

A avaliação deve separar conteúdo, campos, tabelas, imagens/ancoragem, fontes,
quebras e preservação ao salvar e reabrir. Pesos para uma nota percentual exigiriam
critérios aprovados e corpus representativo; nenhum percentual foi atribuído.

Recomendação: manter G1 pendente. Corrigir/isolar o caso mínimo de `SAVEDATE` e da
ancoragem no fornecedor ou versão candidata, controlar as fontes e repetir esses
modelos. Se igualdade com Word for requisito obrigatório, comparar outro motor
com os mesmos documentos antes de escolher. Não alterar silenciosamente os modelos
oficiais nem prometer 100% de compatibilidade para DOCX arbitrários.

## Validação e encerramento

- Navegador real/WOPI: três ciclos AV3, versões baixadas e comparadas.
- Exportação Word original/final e variantes diagnósticas; conversão CODE com
  `X-Source-Version` conferido; conversão independente LibreOffice.
- SHA-256 dos três arquivos recebidos (relatório DOCX, AV3 PDF e AV3 DOCX) preservado.
- Sessões encerradas e host restaurado para o corpus sintético; montagem de fontes
  retirada. Nenhum commit, push, deploy ou alteração de `.env`.
- Os 25 testes Java do host do ensaio anterior continuam válidos: nenhum código
  Java mudou nesta rodada. Não repetidos. API, migrações e PostgreSQL não se aplicam
  às alterações de documentação e ensaios em cópias privadas.
- `render_docx.py` já comprovadamente indisponível por falta de `soffice.exe` no
  Windows; não repetido. Renderização efetiva: PDFs Word/CODE/LibreOffice do
  contêiner e PNGs por pypdfium2/Pillow do runtime fornecido.

Referências externas consultadas: [manual oficial do Collabora sobre fontes](https://sdk.collaboraonline.com/CO-SDK-manual.pdf)
(trecho indexado consultável; abertura integral bloqueada pelo servidor) e
[notas oficiais de compatibilidade/importação DOCX](https://www.collaboraonline.com/collabora-office-24-04-release-notes/).
As conclusões acima se baseiam nos arquivos e resultados locais desta versão fixada.
