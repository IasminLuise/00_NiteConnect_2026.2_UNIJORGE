# Relatos preparados para fornecedores — não enviados

## ONLYOFFICE Community 10.0.0.1: SAVEDATE convertido em REF

Resultado esperado: conservar o tipo do campo e permitir atualização válida no Word.
Resultado observado: `SAVEDATE \\* MERGEFORMAT` torna-se `REF \\* MERGEFORMAT`;
o Word apresenta erro de indicador sem nome. O PDF ONLYOFFICE conserva o cache e
não revela o defeito. A imagem e a paginação corretas não compensam essa perda.

Reprodução independente do documento oficial:

1. Gerar F07 com `scripts/make_reproductions.py`: campo simples em cabeçalho,
   resultado de data em cache e duas páginas sintéticas.
2. Abrir no ONLYOFFICE via WOPI, inserir texto, salvar/fechar, reabrir, remover o
   texto e salvar/fechar novamente. Capturar as duas versões PutFile.
3. Comparar instruções OOXML com `fidelity.inventory`: SAVEDATE deixa de existir e
   REF surge. Reabrir a versão final no Word e atualizar/exportar seus campos.

Evidência privada: `.runtime/onlyoffice-reproductions-20261017`, F07-noop-remove.
O script gerador é revisável; anexos binários devem passar por inspeção antes de
qualquer publicação. Nenhuma fonte proprietária será anexada.

## CODE 26.04.5.1.1: máscara de SAVEDATE e paginação

AV3: campo SAVEDATE sem máscara explícita é regravado com localidade `[$-1B000411]`;
data japonesa no editor e erro de formato no Word. Configuração pt_BR do processo
não resolveu. A reprodução simples F07 modifica máscara, mas não reproduz esta
localidade; não deve ser apresentada como caso mínimo conclusivo.

Relatório: original Word tem sete páginas; CODE gera seis, inclusive com Calibri.
Salvar sem alteração final faz o Word também gerar seis. Restaurar apenas a grade
da tabela em cópia diagnóstica não resolveu. Uma reprodução sintética mínima que
preserve essa divergência ainda não está concluída.

Esses relatos precisam de redução adicional dos documentos privados ou de um canal
autorizado antes de envio. Não anexar os arquivos preenchidos do usuário.

## Ocorrência histórica de imagem: sem defeito persistente atribuído

O PDF de uma rodada antiga mostrava o logotipo somente na última página. A
reexportação do mesmo DOCX e os controles equivalentes mostraram o logotipo em
todas as páginas. Registrar como ocorrência de renderização ainda sem reprodução
estável; não reportar como corrupção comprovada da ancoragem pelo motor.

## Adendo B05.0.1 — relatos focais atualizados, ainda não enviados

As notas anteriores permanecem como histórico. O novo protocolo e os resultados
estão em `b05-savedate-diagnosis.md` e `b05-collabora-pagination.md`.

### ONLYOFFICE — SAVEDATE simples versus complexo

Gerar `field_package(False)` e `field_package(True)` de `compatibility_cases.py`.
São documentos sintéticos, mesma instrução e cache, um cabeçalho e um parágrafo.
No build fixado 10.0.0.1, o simples já reconstrói REF no modelo importado; o host
mantém a origem SAVEDATE até o save. Inserir/salvar/fechar/remover/salvar produz
duas versões REF e erro Word. O complexo conserva instrução e atualiza no Word.
Repetição independente do simples confirmou o erro. Solicitar investigação da
importação/representação de campos simples e preservação de instruções desconhecidas;
o módulo nativo exato não foi provado. Não recomendar modificar o modelo oficial.

### CODE — máscara inválida reproduzida sem AV3

O mesmo `field_package(False)`, com defaults pt-BR, reproduz após save a máscara
com `[$-1B000411]` e erro de formato ao atualizar no Word. O corpo é restaurado
exatamente após remover XYZ. Na fixture mínima C0 o resultado fica vazio; depois
do save aparece a data japonesa. Não apresentar esse C0 como reprodução integral
da aparência inicial do AV3. Não são necessários logo, textos, hyperlinks ou XML
privados para demonstrar o defeito de exportação/Word.

### CODE — tamanho herdado em tabela

Gerar `field_package(False, table=True)`: tabela sintética, Normal 12 pt, sem fonte
binária nem estilo institucional. Word produz 2 páginas, CODE 1; após C1, Word lê
1 página e tamanho 10 pt materializado no salvo. Adicionar somente docDefaults
12 pt não resolveu. Solicitar correção da resolução/materialização de tamanho em
tabelas, sem afirmar qual função do motor é responsável.

Antes de qualquer envio, revisar os binários gerados e metadados. Usar somente os
geradores fonte-livres acima; não anexar F03/F04/F06, AV3, relatório oficial ou
fontes proprietárias. Anexar versão/digests, build Word e passos descritos nos
relatórios. Nenhum chamado/URL/resposta/prazo de fornecedor existe nesta execução.
