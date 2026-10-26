# Inspeção local autorizada — B05 M2P8/M2P9

Escopo adicional autorizado pelo usuário em 24/10/2026 para viabilizar inspeção de
cópias privadas dos dois modelos oficiais. Não é homologação nem relaxamento do gate.

A API ganha sessões próprias de inspeção somente leitura, vinculadas a uma versão
imutável AVAILABLE ou QUARANTINED de uma cópia não oficial no produto. A criação
exige administrador ativo, acesso ao documento, motivo, flag explícita de inspeção,
modo local e bind/base URL em loopback. Sessões expiram em cinco minutos e podem
ser revogadas; cada leitura revalida autorização, vínculo, estado e hash do objeto.
O servidor do motor também precisa estar em loopback. A autorização de
administrador é revalidada depois da aquisição do lock administrativo, impedindo
que uma revogação concorrente seja ultrapassada por uma requisição em espera.

O administrador já pode baixar versões em quarentena pelo endpoint existente.
A nova sessão torna esse acesso auditado utilizável pelo editor somente leitura,
com exportação de inspeção explicitamente emitida pelo backend. O editor comum
continua download:false. Não há reassinatura de JWT no cliente nem force-promote.
O callback de inspeção aceita somente estados de leitura/encerramento/erro, nunca
salva candidata; statuses 2/6 são recusados. Head, versões e gate não são alterados.

Separadamente, POST `/api/v1/documents/{document}/inspection-samples` recebe uma
amostra física de até 8 MiB com `basedOnVersionId`, `expectedVersion` e `reason`.
Exige as mesmas condições locais, administrador e acesso contextual, valida o
pacote pelo DocxIntegrity existente e cria exclusivamente versão QUARANTINED com
`rejectionCode=INSPECTION_ONLY`, inclusive para bytes idênticos à origem. Essa
importação é auditada e incrementa o contador do documento sem mudar seu head;
não é callback do motor nem versão publicável. A abertura de qualquer inspeção
também valida o pacote antes de disponibilizá-lo ao motor.

Rotas autenticadas: POST `inspection-sessions` com `versionId` e `reason`, POST
`inspection-sessions/{id}/close` e POST `inspection-samples`, sempre sob o prefixo
`/api/v1/documents/{document}`. As fontes e callbacks têm rotas próprias sob
`/api/v1/document-provider/inspection-sources` e `inspection-cycles`.

Ativação exclusivamente na bancada: `DOCUMENTS_INSPECTION_ENABLED=true`,
`DOCUMENTS_LOCAL_DEVELOPMENT=true`, `SERVER_ADDRESS=127.0.0.1`, armazenamento
local, secrets técnicos externos ao Git e `DOCUMENTS_PUBLIC_BASE_URL`/motor em
loopback. O padrão da nova flag é false. Nenhum arquivo .env é necessário alterar.

Migração V9 adiciona somente sessões de inspeção; V1–V8 permanecem intactas.
Rotas próprias de fonte/callback usam capability e JWT, respectivamente, com
revogação/expiração. Cadastro/revogação são autenticados por Entra e auditados.
Nenhum segredo ou byte de documento aparece em logs ou DTOs de auditoria.

Validação: PostgreSQL/Flyway real; administrador versus integrante/anônimo;
versão de outro documento; expiração/revogação/perda de papel; callback forjado
ou tentativa de persistência; fonte em quarentena preservada; permissões normais
inalteradas. O uso desta via de bancada não comprova configuração normal de produção.

Validação em 24/10/2026: `mvn -q verify` concluiu com 278 casos, 277 executados
aprovados e um teste de symbolic links ignorado por falta de suporte no Windows.
Os testes adicionais cobrem configuração remota/desabilitada, revogação enquanto
a requisição espera o lock e recusa de motor externo exclusivamente na inspeção.
Limite conhecido do teste de expiração: ele também altera o valor vinculado ao
HMAC; não isola o avanço do relógio. A verificação de validade temporal permanece
implementada, mas essa demonstração isolada está pendente.
