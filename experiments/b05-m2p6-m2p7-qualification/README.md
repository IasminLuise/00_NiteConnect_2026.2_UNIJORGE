# B05-M2P6 + B05-M2P7 — equivalência OOXML e qualificação institucional

**Duas respostas em um único patch, com uma única execução offline.** Complementa o relatório real `windows-2026-10-24(1).md` (M2P4 + M2P5) e usa a saída `decision.json` do kit anterior. Não modifica `apps/api`, a configuração ONLYOFFICE, Flyway, gates, dados oficiais nem serviços de produção.

## Resultado anterior que NÃO deve ser reclassificado

A campanha de 24/10/2026 encontrou **1.452 pixels diferentes no controle sem edição**, em duas regiões (109 na célula 2.3, 1.343 no parágrafo de validação 5). Duas exportações independentes da origem no Word geraram raster idêntico naquela condição. Os pares SDK/callback tinham valores PAGE e metadados armazenados diferentes; SAVEDATE pode mudar no SaveAs2. A matriz M2P5 ficou com 2 `PASS_SCOPE_ONLY`, 9 `UNPROVEN`, 0 `FAIL`, decisão `HOLD_DECISION_NOT_ENOUGH_EVIDENCE`.

Essas descobertas são **contexto histórico**, não resolvidas por este patch. O analisador estrutural **não mede a origem causal dos 1.452 pixels**. Nenhum `UNPROVEN` anterior desaparece sem registro específico de adjudicação e sua evidência, que ainda depende de verificação independente.

## Objetivos e produtos

| Marco | Resposta técnica | Artefato |
| --- | --- | --- |
| **M2P6** | O que se pode demonstrar, ou ainda não, sobre partes OPC, referências internas, estilos explícitos, notas e instruções de campos do par origem → SDK → callback? | `qualification.json` → `m2p6` (inventário dos três caminhos, avisos e limites explícitos) |
| **M2P7** | Há corpus representativo autorizado, resolução rastreável dos 9 critérios anteriores, controles normais de backend e revisão independente suficientes para *submeter* uma decisão de adoção? | `qualification.json` → `m2p7` (matriz de lacunas, riscos e recomendação consultiva) |

A recomendação possível é:

- `HOLD_DECISION_NOT_ENOUGH_EVIDENCE`: faltam fontes, validações, cobertura, autorizações ou pareceres;
- `STOP_AND_REVIEW_DEMONSTRATED_OR_ATTESTED_RISKS`: inconsistência OPC nova mensurável, FAIL anterior ou declaração documentada de falha/rejeição — exige apreciação independente; **não é rollback automático**;
- `READY_FOR_FORMAL_GATE_REVIEW_NOT_APPROVAL`: as evidências e declarações exigidas estão presentes para análise formal, **não é GO**, nem comprova que as declarações sejam verdadeiras.

**Invariantes obrigatórios:** `HOLD_QUARANTINE`, `canPromote=false`, `officialApproved=false`, `backend_final_integration_authorized=false` e G1/G2/M3 bloqueados em *qualquer* resultado. Não há configuração de exceções, tolerância de pixels, normalização reparadora, alteração de código da API ou promoção automática.

## Integração (Windows / PowerShell)

Na raiz do repositório real, após preservar alterações locais existentes e conferir a branch `feat/b05-core-docs-api`:

```powershell
git branch --show-current
git status --short
git apply --check .\NITE_CONNECT_B05-M2P6_M2P7.patch
git apply .\NITE_CONNECT_B05-M2P6_M2P7.patch

cd experiments/b05-m2p6-m2p7-qualification
py -3 -m unittest discover -s tests -v
py -3 -m compileall -q b05_qualification evaluate.py
```

O kit exige apenas **Python 3.10+ da biblioteca padrão**. Testes usam DOCX inteiramente fictícios gerados em diretórios temporários. O kit não lança o editor nem abre Word/PDF: consome os DOCX originais e os laudos prévios, imutáveis.

### Preparação da campanha privada

1. Crie uma **nova** pasta ignorada pelo Git (por exemplo `.private/campaign-01`) e copie, mantendo hashes/proveniência, o arquivo **real** `decision.json` gerado pela M2P5. Não use o `windows-2026-10-24(1).md` como substituto de JSON de máquina.
2. Copie para subpastas privadas os DOCX reais (origem, SDK e callback quando houver), cada um com caminho físico único; também é permitido corpus oficial autorizado *novo*, separado do corpus sintético da M2P5. Um manifesto com apenas uma fixture sintética não comprova representatividade.
3. Use `examples/manifest.template.json` para preencher `path` e SHA-256 de **cada arquivo**. `corpus_kind=official_controlled` só com autorização documental, escopo aprovado e custódia apropriada. Arquivos podem conter dados pessoais e devem permanecer privados.
4. Para a avaliação de prontidão, inclua **ao menos um documento `operation=no_edit` e um `operation=edited`**; o segundo exige `expected_edit` (troca exata única) e `callback_docx`. A execução com um único modo continua permitida para diagnóstico, mas ficará em HOLD. Mudanças estruturais em qualquer par exigem `semantic_review_receipt` individual para deixar a lacuna documental encaminhada à revisão externa. O recibo não demonstra, por si só, equivalência.
5. Informe **feature coverage requerida** no escopo institucional (`required_features`). O código encontra indícios OOXML para os 13 tipos suportados, mas a observação de um marcador XML não comprova funcionamento completo de tal recurso.
6. Registre os 8 controles de integração (normal API *sem* override de `download`; autenticação Entra; autorização por papel; callback assinado; quarentena/head; exportação; provenance SDK/callback; recuperação/auditoria). `PASS_ATTESTED` exige um arquivo de evidência com hash, mas sua veracidade **não é validada pelo código**.
7. Registre cada critério `UNPROVEN` que a saída M2P5 realmente contém em `adjudications`: `PENDING`, `RESOLVED_ATTESTED` ou `REJECTED_ATTESTED`. Mesmo `RESOLVED_ATTESTED` não constitui aprovação pelo programa. Não eliminar ou rebatizar critérios para obter prontidão.
8. Preserve pareceres independentes de **responsável técnico, editorial e institucional** e o aceite do escopo do corpus, cada um com comprovante próprio. O código não verifica assinaturas nem identidade; deve haver auditoria externa.

Para obter SHA-256 no Windows: `Get-FileHash -Algorithm SHA256 .\caminho\arquivo.docx` (converter para minúsculas). Cada arquivo de evidência precisa ser arquivo real, não link remoto. Não inclua segredos ou conteúdo de documentos nos comprovantes compartilháveis.

### Execução — duas respostas em uma avaliação

```powershell
cd experiments/b05-m2p6-m2p7-qualification
py -3 evaluate.py --manifest .private/campaign-01/manifest.json --out-dir .private/campaign-01/qualification-01
```

Serão emitidos `qualification.json` e `qualification.md` **somente se todas as entradas forem verificadas antes/depois da análise**. O diretório de saída não pode existir anteriormente, para preservar evidências. O código lê DOCX sem salvá-los. Nenhum arquivo de entrada é modificado pelo analisador.

Se a campanha for interrompida por hash divergente ou evidência ausente, corrija o manifesto/aquisição e repita em novo diretório. **Não** ajuste o documento para fazer o teste passar.

### Limites explícitos de implementação

- Verifica pacote ZIP OPC, content types, relações internas e IDs `r:id`/`r:embed`/`r:link`; links externos são inventariados, não acessados. Um diagnóstico limpo não certifica o padrão OPC inteiro.
- Compara texto *alinhado por posição de parágrafo* e assinaturas de formatação explícita `w:rPr`/`w:pPr` para caracteres inalterados. Não calcula estilos herdados ou `computed fonts` no Word. Movimentação de parágrafos invalida a hipótese de alinhamento e requer revisão humana.
- Compara texto referenciado de notas por hash e instruções de campo em ordem. Isso **não** prova equivalência semântica de notas, recálculo de campos, estilos renomeados nem layout.
- Não volta a renderizar 12 PDFs do M2P4 nem converte documentos oficiais silenciosamente. Laudos visuais anteriores continuam vigentes; sua conclusão não é revogada pela inspeção de XML.
- Recibos de autorização, integração e revisão são **provas de presença de arquivo e autoria declarada**, não verificação de assinatura digital, método experimental ou imparcialidade. A recomendação máxima é *pronto para revisão*, nunca *aprovado*.
- Sem o repositório real, o instrumento não pode afirmar integração final no backend. Apenas a equipe e instância autorizadas podem executar sessões reais e gates oficiais.

## Privacidade, segurança, encerramento

O JSON compartilhável usa IDs locais de corpus e nomes de partes internas OOXML (não nomes de arquivos do SO), contagens e flags. Não grava tokens, hashes brutos de DOCX, parágrafos integrais, endereços de túnel, nomes de revisores, PDFs ou IDs da API. **Não use IDs locais com dados pessoais**, e revise todo arquivo antes de compartilhá-lo. Mantenha entradas, recibos e DOCX sob `.private/` e não os adicione ao Git.

O avaliador é offline e não verifica se contêineres/Word/túneis foram desligados pela equipe de aquisição. Nenhum commit, merge, deploy ou relaxamento de gate é autorizado por este documento.
