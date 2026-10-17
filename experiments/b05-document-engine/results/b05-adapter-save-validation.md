# B05.0.2 C — validação e salvamento

O host possui porta opt-in `CandidateGate` e política `ScriptedDocxGate`, anterior
ao commit atômico existente. Roteiro independente define o texto esperado;
saída do motor não é gabarito. Uma recusa não cria versão confirmada, mantém o
ponteiro e conserva a candidata em quarentena privada limitada. HTTP 500 sem
header de versão de sucesso; CAS/locks continuam 409. Sem reparação XML.

Testes Java cobrem aceite de edição esperada, retenção da versão anterior,
rejeição HTTP, roteiro ausente, CAS antigo e campo SAVEDATE→REF/imagem alterada.
A suíte anterior de token/read-only/revogação/locks/falha de storage também foi
reexecutada. Esses testes não demonstram UX em cada falha operacional.

## Browser real

Após inserir XYZ/solicitar save, o cliente mostrou **“All changes saved”** sem
nova versão corrente no host. O PutFile recebido depois do fechamento foi recusado
por `UNVERIFIED_FIELDS_OR_LAYOUT`. O ponteiro não avançou.

Na reabertura, XYZ continuava disponível no backup do editor e surgiu aviso
explícito de arquivo ainda não salvo no storage, orientando Save as. Portanto
**não foi observada perda silenciosa definitiva**, nem comprovada recuperação
durável por download/restore. O aviso posterior não torna a confirmação anterior
uma confirmação de persistência WOPI. O contrato de save pedido pelo plano não passou.

O corpo da candidata era exatamente XYZ + corpo original; continha SAVEDATE
complexo completo. Styles/settings, propriedades de documento/header e partes
adicionais foram reserializados. A política conservadora não certifica esses deltas;
não se afirma que sejam corrupção ou regressão visual comprovada. Não foi construída
uma whitelist aprendida da candidata para contornar a recusa.

O probe de modelo interno anterior não encontrou campos complexos dentro de seus
limites; uma leitura maior também foi inconclusiva. A preservação da instrução
no DOCX candidato é evidência de exportação, não prova de cálculo/importação
funcional do SAVEDATE no navegador.

## Decisão

Gate C não passou: **NO-GO-F do fluxo**, sem atribuir a recusa conservadora a defeito
nativo adicional do motor. C1 completo, C2 e gates D/E não foram liberados. A próxima
arquitetura precisa distinguir cache do editor de versão validada no storage e
provar recuperação, além da semântica dos documentos. Não integrar esse protótipo.
