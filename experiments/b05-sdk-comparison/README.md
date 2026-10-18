# B05.0.4 — Pré-qualificação DOCX SuperDoc × Apryse (não homologado)

> **Atualização 18/10/2026:** os dois SDKs foram instalados e a bancada compilou.
> SuperDoc passou os controles sintéticos sob equivalências restritas, mas C0
> oficial registrou seis páginas no editor contra cinco no Word para o AV3.
> Apryse abriu/exportou em trial; edição automatizada não foi confirmada.
> Nenhum SDK selecionado. Consulte [resultados atuais](results/decision.md),
> [execução reproduzível](scripts/EXECUTION.md), [política de comparação](scripts/comparison-policy.md)
> e [ADR](ADR-B05-004.md). O texto abaixo registra o roteiro da entrega recebida;
> a indisponibilidade de npm mencionada é do ambiente anterior. A CI atual verifica
> estrutura **e compilação**, sem aprovar fidelidade de SDK.

**Origem:** branch `spike/b05-sdk-prequalificacao`, derivada de `d03c495` (PR #12).
**Propósito:** comparar dois SDKs, mantendo gestão, versões e arquivos sob responsabilidade futura do NITE CONNECT. Nenhum componente de `apps/api`, Flyway, `apps/web` ou infraestrutura institucional é alterado. Nada é fundido à `main`.

## Segurança e privacidade

- **Modelos oficiais, PDFs, nomes, dados pessoais, hashes identificáveis, exports e capturas ficam apenas em `.private/`** (ignorada pelo Git). Nunca copiar para `web/public`, GitHub, npm, CI, issues, demonstrações públicas ou compartilhamentos de fornecedor.
- O laboratório usa **seleção de arquivo local** e não implementa upload para terceiros nem endpoint de salvamento. SDKs comerciais podem ter telemetria/licenciamento; estudar os termos e obter avaliação apropriada *antes* de abrir documentos institucionais no Apryse. Não inserir chaves comerciais no repositório ou histórico.
- Um SDK no navegador não é automaticamente um isolamento perfeito da rede: inspecionar requests, CSP, workers e domínios contactados. Uso local do arquivo não comprova ausência de telemetria.
- SuperDoc está sujeito a AGPLv3 ou contrato comercial; revisar compatibilidade com a distribuição pretendida do produto. Apryse exige licença de SDK e add-on DOCX para edição, observar prazo/limites de avaliação e tratamento de metadados.
- Os artefatos `.private/` existentes são apenas evidências locais; **não os publicar**.

## 1. Verificador OOXML, sem dependência do editor

Requisitos locais: Python 3.10+ e `python-docx==1.2.0` (apenas para gerar fixtures/testes).

```bash
python -m unittest discover -s experiments/b05-sdk-comparison/tests -v
python experiments/b05-sdk-comparison/scripts/make_synthetic.py --out experiments/b05-sdk-comparison/.private/fixtures/compat-synthetic.docx
python experiments/b05-sdk-comparison/scripts/ooxml_gate.py \
  --before experiments/b05-sdk-comparison/.private/fixtures/compat-synthetic.docx \
  --out experiments/b05-sdk-comparison/.private/baselines/synthetic.json
```

Executar uma vez com arquivos novos; os utilitários deliberadamente recusam sobrescrever os resultados. Para comparar a saída do SDK:

```bash
python experiments/b05-sdk-comparison/scripts/ooxml_gate.py \
  --before experiments/b05-sdk-comparison/.private/fixtures/compat-synthetic.docx \
  --after experiments/b05-sdk-comparison/.private/exports/superdoc-noop.docx \
  --out experiments/b05-sdk-comparison/.private/results/superdoc-noop.json
```

**Não editar os DOCX originais.** `--expected-prefix XYZ` só pode ser aplicado quando o roteiro foi efetivamente a inserção *exata* de XYZ no primeiro parágrafo não vazio do corpo; uma edição no editor por texto visual não autoriza inventar o resultado esperado.

O validador é propositalmente **conservador**: exige identidade de textos e propriedades por parágrafo, topologia de tabelas, instruções de campo, mídias e partes estruturais. Uma rejeição sinaliza diferença não certificada, **não prova corrupção**. Um PASS é apenas uma triagem OOXML; não prova fidelidade no Word, preservação da posição visual, atualização de campos ou persistência do NITE.

## 2. Interface isolada para ensaio interativo no navegador

Node 22.12+, npm com acesso ao registry e navegador desktop atual. **O npm não estava acessível no ambiente que preparou esta branch; os ensaios reais dos SDKs não foram executados aqui.**

```bash
cd experiments/b05-sdk-comparison/web
npm install
npm run vendor:apryse
npm run build
npm run dev
# usar somente http://127.0.0.1:5173
```

O mesmo shell Vite permite escolher SuperDoc ou Apryse, abrir uma fixture sintética local via seletor, editar e exportar a candidata `.docx`. Não contém um servidor de persistência. A exportação de cada candidato usa o contrato oficial: `superdoc.export({triggerDownload:false})` para SuperDoc, ou `getFileData({downloadType:'office'})` para Apryse. Em ambos os casos, **a saída não está homologada nem persistida pelo NITE**.

Para o Apryse, verificar se a edição DOCX está habilitada no trial e em qual modalidade; não confundir WebViewer PDF com o add-on editor DOCX. Revalidar as APIs caso a versão instalada difira da pinada.

## 3. Ordem do ensaio e portas de aprovação

**GATE A — disponibilidade/segurança:** versões efetivamente instaladas, identidade do pacote, tipo/licença, telemetria, inspeção de tráfego e ambiente isolado. Sem acesso ao SDK não há PASS nem FAIL de fidelidade: registrar `BLOCKED_ENVIRONMENT`.

**GATE B — triagem sintética simultânea, mesma ordem para ambos:** (B1) abertura e no-op export; (B2) prefixo XYZ, exportação/reabertura e reversão; (B3) campo `SAVEDATE` simples e complexo, `PAGE`/`NUMPAGES`; (B4) 8 pt de espaçamento efetivo, tabela, cabeçalho, logo sintético. Rodar os mesmos controles. Falha eliminatória bloqueia ensaio institucional.

**GATE C — oficiais, apenas candidato com B aprovado e licença/privacidade verificada:** cópias do AV3 DOCX e do relatório semanal; PDF original AV3 é referência estática independente. Não afirmar que 7 páginas do antigo relatório de outro corpus se aplicam ao relatório semanal recebido. Controlar no Microsoft Word: data dinâmica, tipo e instrução do campo, cache, cabeçalhos, quebras, campos de página, logo, tabelas, espaços, PDF visual e múltiplos ciclos. A referência AV3 PDF enviado possui **5 páginas**; verificar cada uma. Se houver divergência entre a própria origem DOCX e o PDF, registrar baseline, não imputá-la automaticamente ao SDK.

**GATE D — UX/licença/arquitetura (somente após C):** versão local e conflito CAS, revisão/comentários, permissões, coedição, acessibilidade, interface customizável, consumo de memória, limites e termos. Isso requer uma integração experimental futura; **não implementar API real nem V6 neste bloco**.

**GATE E — decisão:** `CANDIDATE` apenas após resultados reais B/C e aceite; `NO-GO` se houver defeito reproduzido e bloqueador, `INCONCLUSIVE` se falta prova e `BLOCKED` por impossibilidade de execução. Não classificar SDK como aprovado por documentação de fornecedor.

## 4. Artefatos e proveniência

- `scripts/ooxml_gate.py`: leitura, inventário e triagem conservadora.
- `scripts/make_synthetic.py`: fixture fonte-livre com campos dinâmicos e 8 pt.
- `tests/test_gate.py`: regressões verificáveis sem motor.
- `web/`: invólucro Vite local, sem dados de projeto.
- `results/decision.md`: decisão e pendências, sem documentos reais.
- `.github/workflows/b05-sdk-prequalification.yml`: testes **sintéticos estáticos apenas**; CI verde não aprova SDK.

Toda evidência de bytes/Word/PNG/hashes e dados de teste com documentos oficiais pertence a `.private/`. Não incluir os modelos originais ou suas cópias num patch/PR. Registrar SHA do SDK e do arquivo de trabalho em manifesto privado antes de fazer comparação.

## Fontes primárias consultadas

- https://docs.superdoc.dev/editor/quickstart/
- https://docs.superdoc.dev/editor/export-options/
- https://docs.apryse.com/web/guides/basics/open
- https://docs.apryse.com/web/guides/basics/save
- https://apryse.com/capabilities/docx-editor/edit-word-documents-programmatically
- Relatórios B05.0.1–B05.0.3 versionados no histórico, especialmente `results/b05-docs-api-final-decision.md`.
