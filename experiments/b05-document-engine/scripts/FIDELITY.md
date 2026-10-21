# Ensaio reproduzível de fidelidade

Usar Python/Node do runtime do Codex, com pypdf, pypdfium2, Pillow, python-docx e
lxml para análise, e Playwright/Chromium do runtime. São dependências de laboratório,
não da API. Word instalado e PowerShell são necessários para `word_control.ps1`.

1. Criar manifesto JSON privado em `.runtime`, com dois objetos `source` apontando
   aos DOCX autorizados: relatório primeiro, AV3 depois.
2. `prepare_fidelity.py --manifest <privado.json> --out <novo-diretorio-em-.runtime>`
   cria seis cópias independentes e um overlay. Nunca reutilizar uma rodada anterior.
3. `lab.py -f <overlay> up -d wopi-host collabora gateway` sobe o laboratório CODE.
   Capturar originais e ambiente com `fidelity.py capture` / `environment`.
4. `editor_cycles.cjs --run <rodada> --playwright <pacote> --chrome <executavel> --python <runtime>`
   executa os ciclos. O pacote Playwright contém seu package.json. O transporte é
   composição/UNO no navegador; não é um teste exclusivo de teclado humano.
5. Jobs JSON privados para `word_control.ps1 -JobsFile <jobs.json>` têm `id`, `source`
   (cópia) e `pdf`. Controles também têm `output_docx`, `paragraph: 1` e
   `append: " B1 B2 B3"` para preenchimento, ou append vazio para controle sem edição.
   Exportações finais usam o source.docx do checkpoint. Não salvar sobre fontes.
6. `fidelity.py capture --document <uuid> --previous <anterior> --version <observada> --out <novo-checkpoint>`
   captura a versão imutável observada, não um ponteiro posterior. Diretório existente
   é recusado; `inventory.json` marca conclusão. Tentativa incompleta precisa de novo destino.
7. `fidelity.py render --pdf <privado> --out <privado>` gera todas as páginas.
   Conferir também os campos ao reabrir no Word, não apenas o PDF do motor.

Para ONLYOFFICE, preparar a rodada em `.runtime/onlyoffice`; subir
`docker compose -f compose.onlyoffice.yaml up -d`. Após healthcheck 200, executar
`onlyoffice_init.py`, que restringe o egress WOPI ao IP real do host dessa rede.
Repetir o helper se o IP mudar. Não imprime local.json, chaves ou tokens.
Executar `onlyoffice_cycles.cjs` com os mesmos argumentos de runtime. Ele espera
PutFile após fechar cada sessão. O cenário inserir/remover tem save intermediário.

O comparador usa Community 10.0.0.1 fixado por digest e dependências internas do
fornecedor, com portas loopback. JWT está desativado somente neste laboratório
confiável; capacidades WOPI são verificadas pelo host. Não é configuração de produto.
O gateway bloqueia conversão/comandos publicados. `fidelity.convert_onlyoffice`
usa a API interna, verifica o hash da versão capturada e recusa PDF já existente.
O laboratório não implementa validação de proof keys do fornecedor e não é multiusuário.

Reproduções: `make_reproductions.py --anchor-source <copia-AV3> --out <privado>`.
Os casos simples não reproduzem todas as falhas CODE. Não publicar arquivos gerados
nem abrir chamados automaticamente. Não distribuir fontes do Windows em imagens.

Encerramento: fechar navegadores, restaurar Compose padrão e parar o Compose
ONLYOFFICE sem remover volumes. Não usar down -v nem apagar evidências.
