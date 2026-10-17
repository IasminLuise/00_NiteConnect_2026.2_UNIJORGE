# B05.0.2 A — baseline

17/10/2026. Branch `spike/b05-adaptador-docx`, base `ac60d148ace51784aecfc7d648d2e95ffc524f8b`.
PR #9 e #10 abertos no início; nenhuma alteração em suas branches ou main.

42 arquivos conferidos: três originais, 29 checkpoints, dez controles Word.
Hashes e caminhos estão somente no manifesto privado. Os modelos oficiais
continuam intocados e não foram normalizados nesta rodada.

Motor: ONLYOFFICE Community 10.0.0.1, índice
`sha256:ca17889f10b11141cc6441836aebdd2835cf3e1b49681b2fd6e155c94b7850b3`.
Word local 16.0/build 16.0.17932. CODE permaneceu comparativo histórico;
nenhuma nova montagem de fontes, imagem ou teste de paginação CODE.

Host derivado do laboratório, imagem local separada `nite-b05-wopi:b0502`;
a imagem padrão não foi retaggeada. Dados em diretórios novos `.runtime/b0502`.
Uma tentativa inicial de browser ocorreu antes de o host terminar o startup:
`ERR_EMPTY_RESPONSE`, excluída dos resultados e mantida como erro de instrumentação.
Outra tentativa com novo dataset, mas o mesmo ID WOPI, restaurou o backup anterior
do motor e já iniciou com aviso. Foi excluída como repetição independente; permaneceu
privada. A repetição válida usa outra identidade WOPI e o runner recusa aviso de
backup anterior antes de inserir texto. Trocar somente o volume não isola o cache.
