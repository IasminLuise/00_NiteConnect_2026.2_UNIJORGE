# Bloco 05 — registro de execução B05.0

17/10/2026. Branch `spike/b05-collabora-wopi-docx`, base main `e643408` limpa.
Plano anexado e revisão aceitos na conversa. Execução limitada ao experimento;
B05.1 encerrou apenas a verificação textual, não as decisões complementares.

- [x] Conferir PDF consolidado e corrigir rastreabilidade/autoridade/eventos/Diário.
- [x] Registrar plano revisado em `bloco-05.md`, preservando gates de produto.
- [x] Fixar CODE/Java/Debian/Nginx por digest; LO/fontes por versões explícitas.
- [x] Gerar fixtures sintéticas determinísticas F01–F10, sem arquivos privados.
- [x] Host Java separado: sessões/capacidades, locks/CAS, versões e validação DOCX.
- [x] RED→GREEN do host/HTTP; revisões com regressões específicas.
- [x] Página de laboratório, discovery, iframe e salvamento real pelo navegador.
- [x] Três ciclos F01, coedição básica e documento longo; confirmar bytes no host.
- [x] Leitura, revogação, IDOR e arquivo inválido pelo HTTP real.
- [x] Comparar Collabora/LibreOffice; renderizar todas as páginas sintéticas.
- [x] Indisponibilidade real, entrada de conversão negada, probe de logs.
- [x] Adicionar workflow separado para a spike, sem mudar CI da API.
- [ ] Completar P01–P20 com modelos/aceite e evidências ainda ausentes no ADR.
- [ ] G1 formal: decisão responsável de motor, fidelidade, suporte e operação.
- [ ] D01–D26 complementares/G2; nenhuma migração ou integração de produto iniciada.

## Verificações

| Comando/caso | Resultado |
|---|---|
| `mvn --batch-mode -f experiments/b05-document-engine/wopi-host/pom.xml verify` | Após alteração Java final para hyperlinks do modelo oficial: BUILD SUCCESS, 25 testes, zero falhas/erros/skips, JAR gerado. |
| `python -m unittest discover -s experiments/b05-document-engine/scripts -p "test_*.py"` | Regressão do PDF antigo: 1 teste aprovado. Repetir somente se comparador mudar. |
| `python .../generate_fixtures.py` | 9 DOCX sintéticos + F10 inválido; manifestos SHA-256. |
| `python .../lab.py config --quiet` | Configuração Compose válida. |
| `python .../wait_ready.py` | Health e discovery HTTP 200 em execução local. |
| `python .../measure.py` | Rodada inicial: 9/9 convertidos nos dois motores; rodada final após correções registrada em results/generated. |
| `python .../render_comparison.py` com runtime Python do Codex | Todas as páginas renderizadas; contagens iguais; F05 com ordem de extração de rodapé diferente. Aceite editorial pendente. |
| Chromium local / WOPI real | Três ciclos persistidos; ambas contribuições em coedição; F09 persistido; leitura 403 para save; download 200→401; documento alheio 404; DOCX falso 415. |
| `python .../probe_runtime.py` | Conversão externa 403; CODE parado 503; hash da fonte preservado; zero padrões de tokens em logs. |
| Parser YAML local; UTF-8/whitespace de todos os arquivos novos; `git diff --check` | Workflow parseado; 34 arquivos novos sem problemas de encoding/whitespace. Não substitui execução remota do workflow. |

Relatórios completos e matriz honesta em `experiments/b05-document-engine/ADR-B05-001.md`.
Artefatos sintéticos/temporários estão ignorados; não armazenar HAR/traces com tokens.

## Resultados anteriores ainda válidos / não executados

CI da API no commit-base: Actions run 38970129697, 185 testes, zero falhas/erros/skips.
Não repetir a API porque os arquivos e contratos dos Blocos 01–04 não foram alterados.
Testes PostgreSQL/migrações não se aplicam a esta spike sem persistência relacional.
Workflow novo ainda não foi publicado/executado no GitHub. Linux limpo não comprovado.
TLS/produção e aceite institucional não executados. Ensaio local dos dois exemplos
oficiais registrado em `experiments/b05-document-engine/results/2026-10-17-official-models.md`:
três ciclos do relatório DOCX preservaram conteúdo, mas alteraram layout; PDF de AV3
inspecionado como referência visual, sem edição. G1 continua pendente.

Rodada posterior com o DOCX AV3 autorizado: três ciclos de edição/salvamento,
corpo/tabelas/imagem preservados, mas falhas de SAVEDATE e logotipo ao reabrir no
Word. Diagnóstico de fontes/grade não resolveu a paginação do relatório semanal.
Detalhes em `experiments/b05-document-engine/results/2026-10-17-av3-and-fidelity.md`.
Nenhuma alteração adicional de código Java; as 25 regressões anteriores permanecem
válidas, sem repetição nesta rodada.

Execução do plano de fidelidade: `bloco-05-fidelity-execution.md`. O host passou a
28 testes após suporte limitado de discovery/timestamp para comparação ONLYOFFICE;
o executor passou em 12 testes Python. Decisão: NO-GO para o conjunto dos modelos,
por paginação CODE e SAVEDATE inválido em ambos. G1 permanece bloqueado. A ocorrência
anterior do logotipo não se repetiu na reexportação controlada do mesmo DOCX.

Uma rodada comparativa foi interrompida pela reconstrução local do wrapper; não
conta como rodada completa. O comparador agora grava checkpoints por arquivo e
relatórios por execução. Não reconstruir serviços durante uma medição.

## Decisões de execução

- Usar a branch solicitada a partir de main, sem criar branch codex ou tocar API.
- Host de teste usa filesystem local versionado somente dentro da spike; não é o
  adaptador institucional definitivo e não reutiliza armazenamento de evidências B03.
- Nginx somente no experimento, por falha comprovada da restrição de conversão na
  entrada loopback. Sem adicionar proxy ou dependência à arquitetura do produto.
- Controle /lab é confiável local e sintético, sem representar autorização NITE.
- Não criar .env nem credenciais; configurações técnicas não secretas nos arquivos.
- Sem commit, push, PR, merge ou deploy nesta etapa; arquivos disponíveis para revisão.
