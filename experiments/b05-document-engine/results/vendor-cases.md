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
