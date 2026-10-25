# B05.0.3 — reprodução isolada

Somente corpus sintético; não usar modelos oficiais enquanto B estiver reprovado.
Os scripts escrevem sob `.runtime/b0503`, ignorado. Não publicar logs, screenshots,
manifestos, HAR, JWT, links com capabilities ou documentos privados. Não reutilizar
diretório de preparação: o preparador recusa destino existente. O host é um
protótipo local, não uma API autorizada para produção.

## Montagem única

Java 21/Maven, Python standard library, Docker Compose e Node com Playwright.
Usar runtimes/dependências do workspace; Playwright é ferramenta de teste já
utilizada na spike. CI instala versão 1.58.2 em diretório temporário do runner,
sem adicionar dependência de produto. Local usou Chromium 1243 instalado.

```text
mvn -f experiments/b05-document-engine/wopi-host/pom.xml verify
python -m unittest discover -s experiments/b05-document-engine/scripts -p test_*.py
python experiments/b05-document-engine/scripts/adapter_ci.py
python experiments/b05-document-engine/scripts/prepare_docs_api.py --out experiments/b05-document-engine/.runtime/b0503/novo
docker build -t nite-b05-wopi:b0503 experiments/b05-document-engine/wopi-host
docker compose -f experiments/b05-document-engine/compose.docs-api.yaml -f experiments/b05-document-engine/.runtime/b0503/novo/compose.private.json up -d
python experiments/b05-document-engine/scripts/docs_api_wait.py
```

Em checkout limpo sem `fixtures/generated/F01.docx`, executar antes o gerador
sintético existente `generate_fixtures.py`. Não executá-lo sobre evidência privada
histórica. F02 local reutiliza os bytes B05.0.2; sem histórico local, gera a
fixture complexa source-free anterior e marca a proveniência no manifesto.

Compose publica só 127.0.0.1:18087/19982. Rede estática dedicada 172.29.53.0/24,
motor 172.29.53.10; não ligar outros projetos nem trocar a imagem fixada.
Configuração privada contém segredo novo e paths absolutos; não imprimir seu
conteúdo. Página de entrada autenticada fica em `private-manifest.json` local.

## Roteiro executado e parada

Substituir `PLAYWRIGHT_DIR` pelo diretório absoluto do pacote Playwright e
`CHROME_EXE` pelo executável instalado. São argumentos obrigatórios, não secrets.

```text
node experiments/b05-document-engine/scripts/docs_api_save_probe.cjs --run experiments/b05-document-engine/.runtime/b0503/novo --phase control --playwright PLAYWRIGHT_DIR --chrome CHROME_EXE
node experiments/b05-document-engine/scripts/docs_api_save_probe.cjs --run experiments/b05-document-engine/.runtime/b0503/novo --phase strict --playwright PLAYWRIGHT_DIR --chrome CHROME_EXE
node experiments/b05-document-engine/scripts/docs_api_save_probe.cjs --run experiments/b05-document-engine/.runtime/b0503/novo --phase readonly --playwright PLAYWRIGHT_DIR --chrome CHROME_EXE
```

`control`: DOCX estrutural sem gate conservador; exige commit e bytes/hash.
`strict`: exige recusa da saída atual e recuperação, **não considera essa recusa
aprovação de integridade**. Se o resultado mudar, parar e investigar a regressão,
sem mudar o gabarito para a saída. `readonly` conclui o preflight A.
Cada tentativa nova requer `--tag` novo, capability/document/key novos; a fonte
permanece idêntica. `timeline.json` registra amostras de estado e `lastStatus`,
não uma captura exaustiva de todos os callbacks do fornecedor.

Na execução local houve correção focal de leitura JWT e prefixo cache, com
RED→GREEN e tentativas intermediárias preservadas. Não se testou outra imagem.
Depois de B2 reprovado, **não executar `--phase force`, novas variantes de campo,
modelos oficiais nem Gates C/D/E**. O suporte de force save existe para contrato
isolado, mas não foi validado no editor real.

Para conferir a retenção já observada (não uma nova hipótese de compatibilidade):

```text
docker compose -f experiments/b05-document-engine/compose.docs-api.yaml -f experiments/b05-document-engine/.runtime/b0503/novo/compose.private.json restart wopi-host
python experiments/b05-document-engine/scripts/docs_api_wait.py
node experiments/b05-document-engine/scripts/docs_api_recovery_probe.cjs --run experiments/b05-document-engine/.runtime/b0503/novo --control control --rejected strict --playwright PLAYWRIGHT_DIR --chrome CHROME_EXE
```

O runner compara bytes aceitos/rejeitados anteriores, nova key, fonte assinada
com o hash da versão aceita e abertura no navegador. Não confundir recuperação
da candidata recusada com promovê-la a versão válida. Não reiniciar o host
enquanto outro ensaio estiver aguardando: isso invalidou o controle WOPI desta
execução, que não foi repetido. O runner WOPI histórico agora aceita `--host` sem
mudar o default; não é usado na CI nova.

## Word e evidência

Localmente, `docs_api_word_control.ps1 -Run <runtime-b0503>` cria em
`analysis/word-XYZ-control.docx` uma cópia independente com `XYZ` no início;
espera a estrutura local `active/fixtures/F01.docx`. Nunca recebe modelo oficial.
`word_layout_probe.ps1 -JobsFile <json-privado>` lê cópias com `source`, `pdf`,
`json`, `id`; informa páginas, campos e métricas de parágrafo. Desabilita macros e
atualização de links. O Word abre readonly para medição; não salva a candidata.

Inventário independente em `fidelity.inventory`, PDFs do Word e PNGs privados
devem ser inspecionados, incluindo texto, espaçamento e quebras. Não corrigir os
arquivos para eliminar o defeito observado. Screens/JSON detalhados são privados.

Evidência local desta execução: `.runtime/b0503/active/browser/` (quatro
tentativas de save, readonly e recuperação), `analysis/` (inventários, XML,
controle Word e métricas), `wopi-control/` (inconclusivo), logs RED/GREEN e
`historical-freeze.json` (177 arquivos). Não recuperar capabilities de logs.

## CI e encerramento

O workflow B05 executa host/Python/adapter e ONLYOFFICE com smoke de
transporte/recusa/retenção. Não publica artifacts do runtime. CI verde significa
regressão sintética, não GO de fidelidade. Word e oficiais ficam fora da CI.

```text
docker compose -f experiments/b05-document-engine/compose.docs-api.yaml -f experiments/b05-document-engine/.runtime/b0503/novo/compose.private.json stop
git diff --check
```

Manter dados/evidências para revisão; sem `down -v`, remoção de cache, reset ou
limpeza de outros projetos. Recalcular hashes históricos antes da entrega.
Não executar novos candidatos após o NO-GO sem nova decisão expressa.
