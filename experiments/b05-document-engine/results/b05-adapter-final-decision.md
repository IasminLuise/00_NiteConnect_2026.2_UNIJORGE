# B05.0.2 F — decisão de mitigação e salvamento

17/10/2026. **NO-GO-F para liberar ONLYOFFICE + adaptador no fluxo ensaiado.**
A viabilidade da transformação nos modelos oficiais continua **INCONCLUSIVA**:
não se passou o Gate C e, portanto, não se executou D/E. A pesquisa chegou ao
ponto de parada aprovado; não é conclusão do Bloco 05 ou homologação de motor.

## O que foi comprovado

- Adaptador `b0502-savedate-1` por hash/version/instruções exatas, somente na entrada.
  Partes não tocadas byte-idênticas; manifesto canônico; determinismo/idempotência.
- No controle fonte-livre com core properties, Word 16.0/build 16.0.17932 reconhece
  tipo 22 e atualiza a mesma data válida no original e normalizado, uma página.
- A candidata do browser preserva SAVEDATE complexo e o corpo XYZ esperado.
  Isso é evidência sintética; não certifica o AV3 ou cálculo de SAVEDATE no editor.
- O host recusa a candidata antes do commit, mantém a versão corrente e todas as
  versões confirmadas, conserva rejeição privada e devolve erro sem versão de sucesso.

## Por que não avançou

O editor anunciou **“All changes saved” enquanto o host ainda não tinha confirmado
uma nova versão**. No ensaio com recusa, a candidata chegou após fechamento e
não foi promovida. Reabrir mostrou XYZ preservado no backup e aviso de que não
estava salvo no storage, com orientação de Save as. Não houve prova de perda
definitiva; também não se demonstrou recuperação durável nem confirmação de
persistência antes da mensagem de sucesso. Esse é o bloqueio contratual de C.

A política conservadora recusou alterações não certificadas em styles/settings,
documento/header e partes adicionais. **Não se atribui corrupção a esses deltas.**
Uma política mais permissiva precisaria de equivalência demonstrada, não aprendizado
do próprio resultado. A falta de save real aceito é limitação explícita do protótipo.

O probe interno B05.0.1 não reconheceu o campo complexo; expandir a leitura não
resolveu a limitação. Não confundir ausência de match com campo apagado. A instrução
foi verificada no DOCX candidato; importação/cálculo no browser não foram aprovados.

## Corpus, versões e controles

Base `ac60d148ace51784aecfc7d648d2e95ffc524f8b`, branch `spike/b05-adaptador-docx`.
[PR #11](https://github.com/IasminLuise/00_NiteConnect_2026.2_UNIJORGE/pull/11)
aberto sobre #10, sem merge; commits e CI final registrados no PR.
ONLYOFFICE Community 10.0.0.1, índice
`sha256:ca17889f10b11141cc6441836aebdd2835cf3e1b49681b2fd6e155c94b7850b3`.
Host em imagem experimental separada; motor não atualizado. CODE sem novos ensaios.
Manifestos privados distinguem original, variante, versão WOPI, candidata rejeitada
e ponteiro retido; nenhuma versão validada fictícia. PDFs Word associados aos bytes
exatos, inclusive candidata de quarentena. Não houve PDF de versão promovida.

Controle inicial sem core properties produzia zeros antes da impressão: insuficiente
para provar data de último save. Controle novo corrigiu a fixture, preservando a
evidência anterior. A repetição sintética com core reproduziu o mesmo bloqueio de
confirmação/aviso; não equivale a duas rodadas oficiais C0/C1/C2.

## Gates e matriz A01–A32

| IDs | Estado e escopo real |
|---|---|
| A01 | PASS: 42 arquivos históricos/originais/controles conferidos, sem publicação |
| A02–A04 | PASS sintético: elegibilidade exata; complexos/outros tipos não reescritos |
| A05 | PASS sintético nas estruturas protegidas/aninhadas/flags desconhecidas cobertas |
| A06 | PASS automatizado de múltiplos campos/header/footer; não é inspeção oficial |
| A07 | PASS sintético de cache/switches/propriedades; data válida no controle com core |
| A08–A09 | PASS sintético de bytes de partes não tocadas/idempotência/determinismo |
| A10 | PASS limitado: ZIP/XML/DTD/macro/limites testados; não auditoria exaustiva de DOCX |
| A11 | PASS no controle sintético com core; oficial NÃO EXECUTADO |
| A12 | PARCIAL: source inalterada e export mantém SAVEDATE; modelo/cálculo interno inconclusivo |
| A13–A14 | NÃO COMPLETOS: primeiro insert chegou como candidata rejeitada; sem duas/três versões aceitas |
| A15–A16 | PASS automatizado: REF e mídia alterada recusados; versões anteriores preservadas |
| A17 | PASS em HTTP/CLI sintético controlado; NÃO PASS no save real do motor |
| A18 | PASS testes Java de CAS/concorrência anteriores reexecutados; não nova coedição no browser |
| A19 | PASS testes Java de token/read-only/revogação; UX operacional E NÃO EXECUTADA |
| A20 | PARCIAL: teste backend de storage verde; UX de disco/timeout não ensaiada |
| A21 | FAIL do contrato de confirmação: mensagem antes do commit; aviso posterior/backup observado, recuperação durável pendente |
| A22–A26 | NÃO EXECUTADOS: parada C; baseline Word 5/7 não substitui novos controles oficiais |
| A27 | PARCIAL: proveniência de variante/candidata/ponteiro retido; nenhuma versão promovida/PDF dela |
| A28 | Verificações locais verdes; CI do PR registrada no fechamento |
| A29 | Revisão/diff e privacidade registrados no fechamento; dados privados ignorados |
| A30 | PENDENTE: aceite institucional não solicitado/concedido por inferência |
| A31 | G1-F permanece bloqueado; G1 não aprovado; G2 fechado |
| A32 | Recomendação expressa abaixo, sem nova integração automática |

## Encaminhamento recomendado

**Não adotar este adaptador no produto nem continuar esperando fornecedor como
único caminho.** A preservação dos modelos é a preferência vigente; por isso,
recomenda-se submeter uma **PoC Microsoft 365/SharePoint com documentos sintéticos**
à autorização institucional. Proposta concreta: confirmar tenant/licenças/admin,
storage e retenção, acesso mínimo ao recurso, identidade/ACL e histórico; comparar
Word web/Graph nos modelos somente depois da autorização correspondente. A PoC
deve provar edição, versão exata, DOCX/PDF e exportação; não prometer iframe ou
CSPP sobre storage próprio. [Limites já pesquisados](b05-microsoft-feasibility.md).

Se manter storage exclusivamente próprio for obrigatório ou a instituição negar
essa PoC, a alternativa a decidir expressamente é revisão **formal** de novas
versões dos modelos, preservando originais e exigindo aceite editorial. Nenhuma
das duas decisões institucionais foi tomada pelo agente. Não há autorização para
contratar, enviar documentos, solicitar consentimento amplo ou implantar serviços.

## Entrega e limites de validação

40 testes Python e 37 testes Java do host aprovados após mudanças; smoke cruzado
adaptador→gate Java integra a CI com aceite sintético e rejeição de REF. Isso
não executa Word/browser nem documentos oficiais na CI. API principal/Flyway/
frontend/infra institucional não mudaram; seus testes não foram repetidos.

Evidências Word/browser e hashes identificáveis ficam privados. Serviços usados
devem ser restaurados/parados sem remover volumes antes da entrega. Revisão
independente e CI final são registradas no PR, sem aprovação automática G1/G2.

Revisão independente de `ac60d14..83e1bdd`: quatro Important confirmados e
corrigidos numa rodada, cada um com RED→GREEN: atributos sob namespace padrão,
campo aninhado entre parágrafos, redistribuição de texto entre runs/cache e CDATA.
Suítes finais verdes e smoke cruzado aprovado; nenhum Minor adiado. O revisor não
reproduziu Word/browser, não avaliou modelos oficiais, recuperação durável/restart,
estado remoto da CI ou aprovações institucionais. Permanecem os limites explicitados
na matriz; CI é conferida separadamente e não substitui esses aceites.

O veredito **não afirma que o editor ignorou erro depois da recusa**. A mensagem
foi anterior ao PutFile recusado; o aviso posterior foi observado. Não se demonstrou
confirmação de uma versão aceita nem recuperação durável no fluxo exigido.

Arquivos: [baseline](b05-adapter-baseline.md), [sintéticos](b05-adapter-synthetic-tests.md),
[save](b05-adapter-save-validation.md), [oficiais não executados](b05-adapter-official-models.md),
[protocolo](../scripts/adapter_protocol.md),
[execução](../../../docs/implementation/bloco-05-adapter-execution.md).
