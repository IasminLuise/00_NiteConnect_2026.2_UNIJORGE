# Ensaio local com dois documentos oficiais — 17/10/2026

Escopo autorizado: testar os arquivos fornecidos pelo usuário na spike B05.0.
Conteúdo dos documentos é dado de teste, não instrução nem homologação de regra
institucional. Nenhuma integração com a API do produto ou produção.

## Amostras e proteção dos originais

- Relatório individual em DOCX, já preenchido: uma tabela, 26 linhas, sete colunas,
  quatro hyperlinks HTTPS e uma imagem. Seção A4 paisagem.
- Plano de AV3 em PDF: outro documento, cinco páginas retrato, sem campos AcroForm.
  Não é a exportação de referência do DOCX do relatório.

As duas fontes nos Downloads permaneceram com SHA-256 idêntico ao da importação.
O relatório tem dados pessoais: arquivos, texto, destinos dos links e capturas não
integram os resultados versionáveis. Evidências detalhadas estão somente em
`.runtime/official-20261017`, ignorado pelo Git. O diretório privado não é criptografado
por este experimento e continua sujeito às permissões do Windows.

Referência do relatório exportada no Microsoft Word instalado, versão 16.0,
build 16.0.17932. Abertura de cópia em leitura, macros desabilitadas e atualização de
links desabilitada; opções restauradas ao final. Não pressupõe equivalência entre
todas as versões do Word ou configurações de impressão.

## Resultados medidos

| Arquivo exato | Word | Collabora CODE 26.04.5.1.1 | LibreOffice 7.4.7 |
|---|---|---|---|
| DOCX original, antes de salvar no editor | 7 páginas | 6 páginas | 7 páginas |
| Mesma versão DOCX após três ciclos no CODE | 6 páginas | 6 páginas | 7 páginas |
| Fontes nos PDFs do original | Calibri | Carlito | DejaVu Sans |

Os mesmos bytes foram usados em cada comparação. A conversão CODE da versão
final confirmou o identificador `X-Source-Version`. A contagem igual de páginas
entre Word e LibreOffice no original **não** significa layout equivalente: mudam
quebras de linha, divisão de linhas da tabela e distribuição do conteúdo.

Três ciclos reais de edição → Ctrl+S → download/verificação, com reabertura entre
ciclos, concluídos no Chromium local. Inserções identificáveis no fim da cópia
persistiram. Comparação OOXML antes/depois, removendo somente esses marcadores:

- Texto original preservado, desconsiderando espaços em branco.
- Uma tabela e 26 linhas preservadas.
- Quatro destinos de hyperlinks preservados.
- Bytes da imagem incorporada preservados.
- DOCX salvo abriu novamente no Word, que exportou seis páginas.

Nos PDFs do original, os três motores extraíram 8.910 caracteres sem espaços,
com a mesma multiplicidade de caracteres. A sequência difere por ordem de extração
das células e quebras de página; isso não prova igualdade semântica nem perda.
A conferência OOXML e a inspeção das renderizações são evidências complementares.
Todas as páginas foram rasterizadas em PNG; inspeção visual em folhas de contato
mostrou tabela e imagem presentes, mas alterações de tipografia, bordas e paginação.
Não houve comparação pixel a pixel nem aceite editorial.

Abertura adicional em emulação Pixel 7, viewport 390×844, somente leitura:
documento renderizou. A tabela paisagem exige zoom; não constitui homologação de
usabilidade mobile nem teste em aparelho físico.

O PDF de AV3 foi renderizado e inspecionado nas cinco páginas. Permite analisar
o exemplo visual; não exercita preenchimento, edição DOCX nem round-trip desse
modelo. É necessário o DOCX correspondente para ensaiar essas operações.

Posteriormente o usuário forneceu o DOCX: teste de edição/salvamento e falhas de
campos/cabeçalho registrados em `2026-10-17-av3-and-fidelity.md`. A limitação acima
descreve apenas a primeira rodada com PDF.

## Ajuste necessário no host experimental

O validador rejeitava qualquer relacionamento externo, incluindo hyperlinks HTTPS
comuns do relatório. Foi reproduzida a rejeição antes da correção. Agora aceita
somente o tipo OOXML `hyperlink`, URI HTTPS com host válido e sem userinfo;
não acessa os destinos. Imagens/templates externos e protocolos file/javascript/http,
assim como links com credenciais, continuam recusados. A política é da spike,
sem alterar contratos ou política de upload do produto.

`mvn --batch-mode -f experiments/b05-document-engine/wopi-host/pom.xml verify`:
25 testes, zero falhas/erros/skips, BUILD SUCCESS após a alteração Java final.
Inclui duas regressões novas: aceitação/preservação do hyperlink e rejeições.

O `render_docx.py` do skill de documentos foi tentado, mas não encontrou
`soffice.exe` no PATH. A renderização efetiva usou o LibreOffice do contêiner já
fixado, exportações do Word/CODE e pypdfium2/Pillow do runtime fornecido.
Nenhuma dependência adicionada ao produto.

## Decisão técnica e pendências

Edição e persistência funcionaram para esta amostra; **fidelidade ao Word não foi
aprovada**. A alteração também aparece ao reabrir o DOCX salvo no Word. Há
substituição de fontes comprovada nos renderizadores; atribuir toda a divergência
somente às fontes seria uma inferência ainda não validada.

Próximo diagnóstico recomendado: controlar disponibilidade/licenciamento de fontes
e comparar as propriedades de tabela e parágrafos regravadas pelo CODE, repetindo
o ensaio antes de escolher o motor. Não modificar o modelo oficial para mascarar
a divergência. G1 permanece pendente, inclusive aceite editorial, demais modelos
críticos e lacunas operacionais do ADR.

Sessões oficiais encerradas, CODE reiniciado e host restaurado com corpus sintético.
Não houve commit, push, publicação, deploy ou alteração de `.env`.
