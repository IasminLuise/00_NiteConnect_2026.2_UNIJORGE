# B05.0 — laboratório documental descartável

Branch `spike/b05-collabora-wopi-docx`, base `e643408`. Plano revisado e limites em
`../../docs/implementation/bloco-05.md`. Não é integração de produto nem GO institucional.
Sem PostgreSQL, Flyway V6, alterações de `apps/api`, Entra, credenciais ou deploy.

## Execução local

Requer Java 21, Maven (ou wrapper já existente), Python 3.10+, Docker Compose v2
e Docker Desktop/Engine funcionando. No Windows iniciar Docker Desktop antes.
O experimento usa aproximadamente 1 GiB em repouso, com limites de 3 GiB para CODE,
512 MiB para Java, 64 MiB para Nginx e 1 GiB para o comparador sob demanda.
Não compartilhar a interface de laboratório nem inserir documentos reais.

A partir da raiz do repositório:

```powershell
mvn --batch-mode -f experiments/b05-document-engine/wopi-host/pom.xml verify
python -m unittest discover -s experiments/b05-document-engine/scripts -p "test_*.py"
python experiments/b05-document-engine/scripts/generate_fixtures.py
python experiments/b05-document-engine/scripts/prepare_runtime.py
python experiments/b05-document-engine/scripts/lab.py build wopi-host libreoffice
python experiments/b05-document-engine/scripts/lab.py up -d
python experiments/b05-document-engine/scripts/wait_ready.py
python experiments/b05-document-engine/scripts/measure.py
```

Abrir `http://localhost:18085`. Escolher fixture e modo, abrir no editor.
A primeira abertura de CODE pode exibir apresentação: fechar pelo X, sem alterar
o arquivo. Digitar marcador sintético, salvar, fechar o editor, reabrir pelo host
e conferir o DOCX persistido. Repetir três ciclos. O botão da página externa revoga
a sessão; o host nega leituras/salvamentos posteriores. Revogar não apaga bytes já
recebidos pelo cliente, nem equivale a encerrar toda sessão de coedição no motor.

Para parar, preservando todos os arquivos de teste:

```powershell
python experiments/b05-document-engine/scripts/lab.py stop
```

Para remover somente contêineres/rede deste experimento, preservando os bind mounts:

```powershell
python experiments/b05-document-engine/scripts/lab.py down
```

Não executar `down -v`, limpeza geral do Docker ou exclusão de `.runtime` como
parte da reprodução. Em uma execução nova, escolher diretório de teste vazio
explicitamente e preservar a execução anterior. O host não atualiza fixtures já
importadas silenciosamente; regenerar arquivos não muda versões já persistidas.

## Topologia e controles

- Navegador → `127.0.0.1:18085` → host Java.
- Navegador → `127.0.0.1:19980` → Nginx → Collabora `9980`, sem porta direta publicada.
- Collabora → `http://wopi-host:8080/wopi/files/{id}` na bridge dedicada.
- Host → `/cool/convert-to/pdf` interno. Gateway nega essa rota ao navegador.
- LibreOffice roda separado, sem rede, com perfil novo em `/tmp` a cada execução.

HTTP/WS é exceção exclusiva deste laboratório em loopback. HTTPS/WSS e configuração
institucional continuam pendentes. A bridge dedicada permite saída de rede; não é
um sandbox sem egress. O Docker Desktop desta máquina não publicou portas na
bridge `internal`, portanto essa opção não foi mantida. Validar isolamento de
rede adicional antes de conteúdo hostil ou qualquer ambiente compartilhado.

Sub-rede técnica reservada: `172.30.50.0/24`. Se houver colisão local, alterar em
conjunto Compose e `prepare_runtime.py`, regenerar configuração e testar novamente.
As imagens de CODE, Java, Debian e Nginx são fixadas por digest. Os pacotes principais
do LibreOffice/fontes têm versões exatas; dependências transitivas estão no manifesto
`/package-versions.txt` da imagem. Mirrors apt podem retirar versões: a construção
deve falhar nesse caso, sem atualizar silenciosamente. Não é snapshot completo de apt.

O wrapper `lab.py` passa UID/GID do usuário comum no Linux para volumes graváveis;
no Windows usa UID 10001. Usar o wrapper também nas conversões. Não cria `.env`.
Nginx foi necessário porque a allowlist do motor isoladamente aceitou conversão
direta por loopback nesta versão/topologia; o gateway bloqueia a rota externamente.

## Contrato do laboratório

`/lab` é um painel administrativo local sem login, somente para dados sintéticos.
Permite criar sessão read/write, revogar e consultar todas as versões das fixtures.
Não modela `AccessService`, `ProjectAccess`, participantes reais ou autorização NITE.
Mutações exigem Origin `http://localhost:18085`; não há CORS permissivo.

| Operação | Contrato implementado |
|---|---|
| `GET /wopi/files/{id}` | CheckFileInfo, permissões da capacidade, versão e LastModifiedTime |
| `GET /wopi/files/{id}/contents` | GetFile com capacidade válida do documento |
| `POST /wopi/files/{id}/contents` | `X-WOPI-Override: PUT`; DOCX validado; CAS timestamp; versão imutável |
| `POST /wopi/files/{id}` | LOCK/REFRESH_LOCK/UNLOCK/GET_LOCK; X-WOPI-OldLock implementa relock |
| `POST /lab/editor` | Discovery do servidor fixado, ação DOCX e POST do token ao iframe |
| `POST /lab/revoke/{sessionId}` | Revoga capacidade da sessão |
| `GET /lab/documents` | Catálogo sintético e IDs das versões |
| `GET /lab/documents/{id}/current` | Identificador da versão corrente |
| `GET /lab/documents/{id}/versions/{version}` | Bytes imutáveis de uma versão do laboratório |
| `POST /lab/documents/{id}/versions/{version}/pdf` | Converte versão exata; devolve X-Source-Version |

Tokens aleatórios de 256 bits; apenas hashes em memória, TTL de 30 minutos no editor,
revogação e escopo por documento. Reiniciar o host invalida todas as sessões.
Cada sessão representa pessoa sintética distinta; nenhuma conta institucional.
Tokens nunca devem ser colocados em Git, logs, resultados ou capturas de rede.
Save As/PutRelativeFile, criação oficial, exclusão e rename não são anunciados.

Timestamp CAS tem resolução de micros, monotônica entre versões mesmo com Clock
fixo. Missing/stale timestamp não permite force-overwrite. Locks WOPI são adicionais;
não se presume que Collabora emita o mesmo fluxo do Microsoft 365. Validar a
coedição e revogação em execução real antes de G1. Escritas e revogações serializadas
somente neste processo; não é o protocolo transacional PostgreSQL do produto.

Limites técnicos: entrada 8 MiB, parte descomprimida 16 MiB, total descomprimido
64 MiB e 512 entradas. XML sem DTD/entidades externas; sem macros ou embeddings.
Relações externas são recusadas, exceto hyperlinks HTTPS sem credenciais e com host
válido, preservados sem consulta ao destino pelo host. Imagens/templates externos
continuam proibidos. ZIP64 e split archives recusados. Não é antivírus nem homologação
para arquivos maliciosos. Fonte nunca é substituída pelo PDF.

Objetos e ponteiro current ficam em `.runtime/data`; CREATE_NEW, flush e troca
atômica de ponteiro preservam versões anteriores. Falhas podem deixar objetos órfãos:
não há expurgo automático. Flush de diretório, proteção contra manipulação local,
backup/restore, quotas, multi-processo e reconciliação completa continuam pendentes.

## Evidências e CI

`measure.py` baixa a versão corrente exata, registra seu SHA-256, converte os mesmos
bytes nos dois motores e usa arquivo exclusivo por execução no LibreOffice para
não reutilizar saídas antigas. Tempos do comparador incluem inicialização do contêiner;
não comparar esses números como benchmark puro dos motores.

`render_comparison.py` exige pypdfium2/Pillow, disponíveis no runtime do Codex;
renderiza todas as páginas lado a lado e registra texto extraído e contagens.
Essas bibliotecas são opcionais para análise, não dependências da API/produto.
Texto extraído em ordem diferente não demonstra perda; inspecionar renderização.

Workflow próprio `.github/workflows/b05-document-engine.yml`: host, regressão do
comparador, fixtures, Docker e conversão de F01. Não substitui E2E do editor,
inspeção visual, testes mobile/carga ou aprovação editorial. A CI da API permanece
intacta e seus 185 testes não validam este experimento.

Resultados e lacunas: `ADR-B05-001.md`. Somente G1 explicitamente aprovado permite
considerar o motor escolhido; D01–D26 e G2 ainda bloqueiam implementação de produto.

Ensaio autorizado com documentos oficiais: `results/2026-10-17-official-models.md`.
O relatório fornecido contém dados pessoais; fontes, cópias, versões e renderizações
ficaram em `.runtime/official-20261017`, ignorado pelo Git, em armazenamento separado
do corpus sintético. O laboratório padrão foi restaurado após encerrar as sessões.

Ensaio controlado posterior, comparação ONLYOFFICE e decisão NO-GO:
`results/2026-10-17-fidelity-decision.md`. Instruções reproduzíveis em
`scripts/FIDELITY.md`. Os modelos oficiais continuam intactos.

Pesquisa focal B05.0.1, em branch derivada e PR empilhado: protocolo em
`scripts/FIDELITY-B0501.md` e resultado em `results/b05-compatibility-final-decision.md`.
Mantém NO-GO; acrescenta reproduções e diagnóstico, sem habilitar edição no produto.

Pesquisa B05.0.2: `scripts/adapter_protocol.md`,
`results/b05-adapter-final-decision.md`. Adaptador pré-editor por manifesto privado
e validação opt-in `LAB_SAVE_SCRIPT` somente no host experimental. Parada no Gate C;
sem normalização oficial ou integração de produto. Após Maven e testes Python,
`python scripts/adapter_ci.py` verifica uma fixture sintética transformada contra
o gate Java: aceite controlado e recusa de regressão REF. Não é ensaio de motor/Word.
