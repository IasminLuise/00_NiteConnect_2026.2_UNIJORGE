# B05 — bancada experimental ONLYOFFICE

Escopo: controles locais, privados e predominantemente sintéticos da rota ONLYOFFICE por WOPI e Docs API. Os dois protocolos usam o mesmo motor. Esta bancada não é integração do backend NITE e não autoriza documentos oficiais, compartilhamento externo ou produção. Estado vigente: NO-GO para edição de documentos oficiais; consulte o [relatório final](results/b05-docs-api-final-decision.md) e a [ADR B05-005](../../docs/implementation/adr-b05-005-selecao-motor-documental.md).

## Pré-requisitos e testes

Java 21, Maven, Python 3.10+ e Docker Compose v2.

```powershell
mvn --batch-mode -f experiments/b05-document-engine/wopi-host/pom.xml verify
python -m unittest discover -s experiments/b05-document-engine/scripts -p "test_*.py"
node experiments/b05-document-engine/scripts/test_savedate_sdkjs_source.cjs
```

Os comandos Docker devem apontar explicitamente para `compose.onlyoffice.yaml` ou `compose.docs-api.yaml`. O wrapper `scripts/lab.py` usa somente `compose.onlyoffice.yaml`:

```powershell
python experiments/b05-document-engine/scripts/generate_fixtures.py
python experiments/b05-document-engine/scripts/lab.py up -d
python experiments/b05-document-engine/scripts/wait_ready.py
```

O controle WOPI abre em `http://localhost:18086`. Usar somente fixtures sintéticas. As execuções devem receber pastas privadas novas sob `.runtime`; não sobrescrever resultados anteriores. Ao encerrar uma execução iniciada pelo wrapper:

```powershell
python experiments/b05-document-engine/scripts/lab.py down
```

Não usar `down -v`, limpeza geral do Docker ou exclusão em massa de `.runtime`. A remoção de containers/volumes em uso requer inspeção prévia dos mounts e autorização específica.

## Componentes mantidos

- `compose.onlyoffice.yaml`: controle WOPI local com imagem fixada por digest.
- `compose.docs-api.yaml`: controle Docs API local com imagem fixada por digest.
- `wopi-host`: host experimental de documentos e sessões sintéticas; não é `apps/api`.
- `fixtures/` e `scripts/`: fixtures, gates, ciclos e controles focados ONLYOFFICE.
- `patches/onlyoffice-sdkjs-3679e037-savedate.patch`: patch-fonte experimental SAVEDATE/F9; não integrado ao DocumentServer oficial.
- `scripts/test_savedate_sdkjs_source.cjs`: regressão do patch-fonte.
- `results/b05-docs-api-*.md`: registro versionado do NO-GO e da evidência associada.
- `.runtime/`: resultados privados ignorados pelo Git; preservar runs identificados e não substituir a evidência ausente de B05.0.3 por outro experimento.

## Limites de segurança e evidência

O host aceita somente sessões de laboratório e fixtures. Tokens, documentos privados e hashes de fixtures reais não devem ser gravados em Git ou enviados a terceiros. A validação de callback, gate, exportação e recuperação na bancada não comprova autorização contextual, storage ou integração do produto.

O relatório NO-GO aponta `.runtime/b0503`, ausente neste checkout. `aud-onlyoffice-spacing-02` é outro ensaio e seus manifests indicam casos A/B/C não executados; não o tratar como evidência equivalente. Registre a indisponibilidade e repita o ensaio antes de alegar reprodutibilidade bruta.

O gate existente não é alterado nesta limpeza. Qualquer exceção futura requer regra específica, controles positivos e negativos e aprovação separada.
