# B05-M2P8 + M2P9 — Aceitação com modelos oficiais do NITE

**Novo formato, substitui a próxima rodada de fixture sintética.** Uma campanha, dois níveis: M2P8 (fidelidade visual/estrutural de documentos reais) e M2P9 (decisão arquitetural condicionada por evidência real). O kit não executa o editor sozinho: exige o runtime real instalado, operador autorizado e aquisição de arquivos originais, versões do SDK/callback, reabertura e PDFs Word. **Não alegar execução real somente por rodar testes unitários.**

## Pré-requisitos

Aplicados previamente `experiments/b05-m2p4-m2p5-engine-decision` e `experiments/b05-m2p6-m2p7-qualification` (usa analisadores já validados, sem copiar ou modificar seu código). Python 3.11+, PyMuPDF para PDFs; Pillow para visualizações. Em Windows, Word Desktop + ONLYOFFICE Docs instalado e backend NITE CONNECT em bancada isolada com permissões normais. Não realizar testes em produção.

Os arquivos `Plano de AV3 - MODELO.docx`, `RA_seunome_completo - Relatório Individual Semanal.docx`, e o PDF original do AV3 já foram fornecidos pelo usuário em conversas anteriores, mas **não são incluídos no ZIP**. O operador fornece seus bytes reais no Windows, sob autorização. A referência PDF de validação deve ser **gerada pelo Word do DOCX correspondente nesta rodada**; não usar apenas o PDF original como substituto da referência Word.

## Uso no Windows (PowerShell, na raiz do repositório)

```powershell
$kit = 'experiments/b05-m2p8-m2p9-official-acceptance'
python "$kit/prepare.py" --av3-docx 'C:\originais\Plano de AV3 - MODELO.docx' --weekly-docx 'C:\originais\RA_seunome_completo - Relatório Individual Semanal.docx' --av3-word-pdf 'C:\referencias\AV3-word.pdf' --weekly-word-pdf 'C:\referencias\Relatorio-word.pdf' --campaign "$kit/.private/campaign-official-01"
```

Leia `OPERACOES_OFICIAIS.md` e execute as **seis sessões no ONLYOFFICE real**. Copie cada saída física para a campanha, preencha os campos `null` do `manifest.json` com `{ "path": "...", "sha256": "..." }`; nunca use caminhos fora dela. Crie os recibos em JSON a partir dos exemplos (os valores ilustrativos não servem como evidência). Para calcular hash:

```powershell
(Get-FileHash -Algorithm SHA256 'caminho\arquivo.docx').Hash.ToLower()
```

Executar avaliação **somente após** aquisição (se não houver os artefatos, a decisão será HOLD):

```powershell
cd experiments/b05-m2p8-m2p9-official-acceptance
python -m unittest discover -s tests -v
python evaluate.py --manifest .private/campaign-official-01/manifest.json --out .private/campaign-official-01/qualification-01.json
python visualize.py --manifest .private/campaign-official-01/manifest.json --out-dir .private/campaign-official-01/visual-review-01
```

`qualification-01.json` é relatório **privado**, sem alteração dos arquivos de entrada. `visual-review-01` contém imagens de todas as páginas, também privadas. Para entregar relatório compartilhável, antes revisar o JSON e remover referências a dados pessoais; **não compartilhar os DOCX, PDFs nem PNGs sem autorização**.

## Saídas decisórias

- `HOLD_MISSING_OFFICIAL_EVIDENCE`: falta evidência; não há autorização de adoção.
- `STOP_AND_REVIEW_DEMONSTRATED_RISKS`: regressão/integridade ou rejeição exige revisão, não NO-GO automático.
- `NO_GO_RECOMMENDED_TWO_INDEPENDENT_REJECTIONS`: avaliação técnica e editorial rejeitaram a mesma operação real.
- `TECHNICALLY_ELIGIBLE_FOR_FORMAL_DECISION_NOT_APPROVED`: passa nos critérios instrumentados/atestados; ainda depende de homologação institucional.

**Sempre:** `canPromote=false`, `officialApproved=false`, `backend_final_integration_authorized=false`. A decisão é consultiva. Nenhum arquivo existente, gate, backend, credencial, modelo oficial ou evidência antiga é alterado por este patch aditivo.

## Limitações explícitas

O importador da Biblioteca ChatGPT localizou os modelos pelo nome e extraiu conteúdo, mas não forneceu os **bytes originais autorizados** nesta sessão. Não é possível aqui executar o ciclo real ONLYOFFICE/Word ou atestar o resultado dos modelos. O patch deve ser instalado e executado na bancada Windows com os arquivos originais. Testes unitários incluem exemplos fictícios apenas para validar o verificador, nunca para qualificar o motor.
