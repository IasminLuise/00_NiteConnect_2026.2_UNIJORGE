# B05.0.3 — persistência e recuperação

**Transporte aprovado no controle estrutural; Gate B reprovado com validação
ativa.** O resultado não é uma falha de entrega do callback nem prova de
corrupção definitiva do motor.

O host experimental faz sua própria operação `saveVersion` com CAS por UUID de
versão, autorização e gate anteriores. Não simula `PutFile`. O caminho é:

```mermaid
sequenceDiagram
    participant B as Navegador
    participant E as ONLYOFFICE
    participant H as Host experimental
    B->>E: XYZ / Ctrl+S
    E-->>B: Todas as alterações foram salvas
    Note over B,H: NITE ainda pendente; commits = 0
    B->>E: Encerrar editor
    E->>H: callback 2 assinado + URL temporária
    H->>E: download limitado para IP fixado
    H->>H: DOCX + gate + CAS
    alt controle estrutural
        H->>H: versão imutável e estado durável
        H-->>E: error = 0
        H-->>B: committed / versão / hash
    else gate ativo
        H->>H: candidata em quarentena; versão anterior mantida
        H-->>E: HTTP 500 / error = 1
        H-->>B: rejected / recuperação necessária
    end
```

As duas primeiras tentativas foram diagnósticos de interoperabilidade, sem
versão promovida: 401 pelo JWT plano no corpo; depois 400 porque o caminho real
incluía `/cache/files/data/`. Uma rodada focal de correção compreendeu **dois
ajustes de contrato**, cada qual com teste RED→GREEN e uma tentativa intermediária.
Não houve alteração de gate, cache reaproveitado, fonte aprendida ou outra versão
do motor. Os diagnósticos malsucedidos permanecem privados, sem contá-los como
testes positivos.

Na tentativa `control-contract-fixed`, a mesma F01 foi aberta com key nova:
inserção, indicador nativo, fechamento, callback 2, um commit e download da versão
exata. Hash do corpo baixado, header e estado concordam. Antes do callback havia
zero commits e a barra externa não declarou sucesso.

Na tentativa `strict-contract-fixed`, key/documento novos e os mesmos bytes
iniciais, a candidata chegou. `ScriptedDocxGate` recusou com
`SAVE_REJECTED_UNVERIFIED_FIELDS_OR_LAYOUT`. Texto por parágrafo corresponde ao
roteiro. Foram zero commits, ponteiro anterior preservado, erro real no callback,
quarentena e download autenticado explicitamente **não confirmado**. A barra NITE
não mostrou “Salvo”. O indicador nativo antes de fechar era a mesma mensagem do
controle; não é confirmação do armazenamento NITE.

Após reinício real do container do host, ambos os downloads retornaram exatamente
os bytes anteriores: versão aceita no controle e candidata rejeitada no ensaio
estrito. Reabrir o controle criou key nova para o mesmo documento/versão, o endpoint
de fonte retornou o hash confirmado e o editor abriu essa sessão. O Word abriu
os DOCX exatos. Não foi necessário reconstruir candidata a partir do cache.

Foram implementados e testados em Java: autenticação/escopo, status 1/2/3/4/6/7
sem promover status informativos, replay idempotente após reconstrução, chave
obsoleta, readonly, REF recusado, redirecionamento de download recusado, CAS e
falha de metadados posterior a commit válido (reconciliação, sem falso rollback).
Após a revisão, status 6 exige operação pendente não vazia e correlação exata;
não aceita checkpoints espontâneos sem ordenação. Reabrir uma sessão substituída
é recusado e uma nova sessão não anuncia recuperação localizada apenas na antiga.
Um caso independente mínimo com alteração `XYZ` sem outros deltas é aceito pelo
gate; a política não recusa toda edição por definição. Esses testes **não
substituem ensaios reais** de force save, coedição, timeout/retry do motor ou
recuperação de todas as falhas. Esses ensaios não ocorreram após a parada B2.

Segurança do fetch: origem literal permitida, prefixo cache da key, destino IP
fixo sem resolução DNS, redirects recusados, limite DOCX de 8 MiB, connect/read
timeouts e prazo de streaming. Capacidade e JWT não são registrados pelo host.
Logs diagnósticos do fornecedor podem conter tokens/URLs; estão privados,
ignorados e não são publicados. TLS/WSS, redaction operacional e auditoria de
produção continuam sem homologação.

O controle focal WOPI novo **não foi concluído**: o runner histórico aguardou
`Page 1 of` e expirou. Um restart do host foi iniciado enquanto ele ainda
aguardava; a execução está invalidada, não é resultado negativo do protocolo.
Nenhuma repetição foi feita. O contraste WOPI é limitado à evidência histórica
B05.0.2; não há comparação causal completa nova nem ranking de desempenho.

Evidências privadas: `active/browser/{control-contract-fixed,strict-contract-fixed,readonly,recovery-after-restart}/`,
`analysis/`, `wopi-control/` sob `.runtime/b0503/`. Estado final e IDs/hash em
`host-result.json`, `timeline.json` e `result.json`; sem publicá-los.
