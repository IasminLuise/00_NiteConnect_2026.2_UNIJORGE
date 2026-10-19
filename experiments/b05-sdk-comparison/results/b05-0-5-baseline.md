# B05.0.5 — baseline congelado

Execução em 19/10/2026, America/Bahia. Base `4606fe5`, head verificado do PR #13
aberto. Checkout limpo antes de criar `spike/b05-sdk-diagnostico`, sem modificar
branches anteriores. Referência funcional: especificação consolidada, pp. 7, 12–13,
incluindo RNF-019; nenhuma regra de negócio foi implementada.

- SuperDoc 2.23.0 / Apryse WebViewer 12.1.0, lockfile inalterado.
- Node 24.19.0, Python 3.13, TypeScript 5.9.3, Vite 7.3.7; npm e plataforma
  exatos no manifesto privado. Chromium instalado em chromium-1243; versão real
  do navegador registrada pelo userAgent nas medições. Word 16.0.17932.
- `npm ci --ignore-scripts --no-audit --no-fund`, cópia de assets, build e os
  17 testes anteriores executados novamente: PASS. Warning de chunks grandes
  não significa falha de compilação nem aprovação de fidelidade.
- Dois DOCX locais e PDF de referência preservados. Proveniência e hashes somente
  privados. Foram congelados 155 arquivos Word/browser da B05.0.4, sem alterá-los.
  Não se trata do corpus histórico de 177 arquivos da B05.0.3, indisponível aqui.
- Origens HTTP/WS limitadas a loopback; telemetria SuperDoc desabilitada. Seis
  sessões novas registradas, nenhuma tentativa de egress observada. Nenhum
  arquivo institucional aberto no Apryse. Trial sem chave, compra ou conta nova.

Evidências: `.private/b0505/baseline-freeze.json`, `session-manifest.json`,
`npm-ci.txt`, `build-baseline.txt`, `tests-baseline.txt`, `spec-extract.json`.
São dados locais ignorados, não anexos ao PR.

Gate A: **PASS no laboratório**, sem aprovação jurídica/operacional para produto.

