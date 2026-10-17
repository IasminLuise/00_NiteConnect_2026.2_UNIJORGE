# B05.0.1 D — candidatos, hipóteses e ponto de parada

17/10/2026. Não foi instalada outra imagem nem aplicado patch. As consultas ao
registro e às fontes públicas não forneceram uma correção pertinente justificável
para A/B de uma versão diferente. Isso não prova inexistência de correção futura.

## Imagens verificadas

- CODE `26.04.5.1.1`, manifesto amd64 `56541aad…ccd402`. O latest consultado
  resolve ao mesmo manifesto da imagem usada; assinatura/attestation não é versão.
- ONLYOFFICE `10.0.0.1`, índice OCI `ca17889f…7850b3`; manifesto amd64
  `bff1a9fd…3fd5`. O conteúdo do índice fixado e da tag consultada coincide.
  A diferença entre índice e manifesto de plataforma não é upgrade.
- GitHub [release ONLYOFFICE v10.0.0](https://github.com/ONLYOFFICE/DocumentServer/releases/tag/v10.0.0)
  continuava a release mais recente consultada. A leitura do parser público ainda
  mostra fallback para REF, conforme relatório B. Não foi localizado patch oficial
  para os defeitos específicos nas buscas realizadas. Busca negativa é limitada.

Metadados completos das consultas foram preservados privadamente; os digests
completos versionáveis estão no manifesto do baseline. Nenhuma imagem flutuante
foi executada.

## Experimentos controlados

| ID | Hipótese/variável | Controle e resultado | Decisão |
|---|---|---|---|
| CX-001 | Forma simples/complexa altera a preservação de SAVEDATE no ONLYOFFICE | Mesmo pacote mínimo, cache e conteúdo; simples vira REF antes do save solicitado e falha no Word; complexa preserva instrução no round-trip e atualiza no Word | Confirmada; informar fornecedor, não adaptar o modelo oficial |
| CX-002 | Contexto styles/settings do AV3 é indispensável à máscara japonesa CODE | Campo isolado com contexto original versus sintético; ambos falham | Refutada; contexto não necessário |
| CX-003 | Elementos privados do AV3 são necessários à falha CODE | Campo/arquivo gerados inteiramente do zero também geram máscara inválida e erro Word | Refutada; reprodução fonte-livre obtida |
| CX-004 | Tamanhos 10 pt materializados pelo CODE explicam a redução persistida | Somente 304 propriedades 20→24 no salvo; Word 6→7, distribuição de tamanhos e fronteiras dos parágrafos coincidem com original | Confirmada para paginação dessa amostra; cópia diagnóstica não é solução |
| CX-005 | Ausência de tamanho em docDefaults causa sozinha o fallback na tabela | Tabela sintética Normal 12 pt; adicionar só docDefaults 12 pt mantém CODE 1 contra Word 2 | Refutada; não é configuração corretiva demonstrada |

Os limites iniciais foram respeitados: uma hipótese de representação no
ONLYOFFICE; duas reduções controladas de SAVEDATE e duas hipóteses de paginação
no CODE. As reduções são investigação do mesmo defeito, não versões candidatas.

## Comandos e evidência equivalente a RED

- Gerador `compatibility_cases.py`: campo simples, complexo, duas reduções, tabela
  e cópia de tamanhos. Entradas oficiais são somente cópias privadas. A tabela com
  docDefaults adiciona apenas `<w:sz w:val="24"/>` ao `rPrDefault/rPr` da fixture.
- `onlyoffice_cycles.cjs --run <privado>`: C1 com save intermediário e duas versões.
- `editor_cycles.cjs --persistent-noop true --run <privado>`: C1 com duas versões;
  conteúdos finais comparados exatamente aos originais.
- `probe_onlyoffice_import.cjs`: somente leitura do modelo sintético antes de editar.
- `word_layout_probe.ps1 -JobsFile <privado>`: Word repaginado, campos locais
  atualizados, PDF e propriedades efetivas de cada versão salvos privadamente.
- Capturas identificam versões imutáveis; as conversões não seguem `current`.

As falhas dos motores permaneceram RED; não houve GREEN corretivo para eles.
Os testes da instrumentação tiveram RED→GREEN e não certificam fidelidade dos motores.

## Seleção/rejeição

ONLYOFFICE: NO-GO para o conjunto oficial devido ao SAVEDATE simples do AV3.
O controle complexo oferece direção concreta de investigação para o fornecedor;
não justifica um adaptador silencioso nem demonstra toda a semântica no navegador.

CODE: NO-GO para relatório e AV3. Há agora reproduções fonte-livres para máscara
de data e tamanho em tabela. Falta uma correção do motor e revalidação dos modelos.

Não criar fork permanente: um patch exige identificar a fronteira nativa/SDK,
implementar semântica e regressões, fixar build/licença e assumir manutenção.
Nenhum patch com essas evidências foi produzido nesta rodada. Configurações de
fontes/localidade já refutadas não foram repetidas. Nenhuma transformação corretiva
de XML foi incluída no host, exportador, download ou caminho de salvamento.

Os candidatos falharam nos testes focais. Pelo gate D, não se repetiram C0/C1/C2
oficiais nem todo F01–F09 sem candidato potencialmente corrigido. Essa é a aplicação
do ponto de parada, não uma nova aprovação dos resultados antigos.
