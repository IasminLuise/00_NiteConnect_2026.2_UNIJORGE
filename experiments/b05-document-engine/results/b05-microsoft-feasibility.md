# B05.0.1 E — viabilidade documental Microsoft

17/10/2026. Análise de documentação oficial; nenhum tenant, site, conta ou licença
foi consultado por uma API institucional. Nenhuma autorização Graph, contratação,
upload ou integração Microsoft foi feita. Instalação local do Word e login Entra
não demonstram direitos institucionais de edição web.

## Matriz de rotas

| Rota | Elegibilidade nesta pesquisa | Bytes e versões | Abrir/editar/salvar | Autorizações, auditoria e dependências |
|---|---|---|---|---|
| Word web + storage próprio NITE via CSPP/WOPI | Não confirmado; cliente Microsoft 365 direto não é elegível ao CSPP por esse simples vínculo | NITE como fonte e host WOPI, se aprovado pelo programa | Wrapper web e serviço Microsoft; sujeito à adesão, validação e licenças | Requer enquadramento do fornecedor, identidade, WOPI/proof keys/TLS, licenças e contrato. Nenhum direito presumido |
| SharePoint/OneDrive institucional + catálogo NITE | Tecnicamente documentável; depende de autorização e licença institucionais não verificadas | Microsoft 365 passa a ser fonte dos arquivos e histórico; NITE mantém referências/metadados | Navegação autenticada por webUrl para o serviço Microsoft; Graph para operações autorizadas. Link não comprova iframe de edição embutido | Consentimento e acesso ao recurso, ACLs Microsoft, política de versões/retenção, auditoria e sincronização precisam de PoC e aprovação |
| Word desktop externo + reenvio ao NITE | Candidata de contingência, dependente de aceite da mudança de experiência | NITE poderia controlar cada upload; edição local fora da aplicação | Download/editar/reenvio versionado, sem editor web integrado | Licenças e ambientes dos usuários, validação do upload/conflitos/autoria/derivados ainda não implementados. Não atende ao requisito atual de editor integrado |

## CSPP

A [visão geral oficial](https://learn.microsoft.com/en-us/microsoft-365/cloud-storage-partner-program/online/overview)
define participação de ISVs de armazenamento SaaS, com domínio WOPI e arquivos
sob controle do parceiro, atendendo mais de um cliente externo. O programa não é
aberto diretamente a clientes comuns Microsoft 365; a aplicação é avaliada pela
Microsoft. A descrição atual do NITE Connect não comprova esse enquadramento.
Os usuários editores também precisam das licenças apropriadas.

O [processo de candidatura](https://learn.microsoft.com/en-us/microsoft-365/cloud-storage-partner-program/online/apply-for-cspp-program)
prevê adesão e revisão. Implementar WOPI localmente não concede acesso ao serviço
Microsoft. Não iniciar PoC com arquivos privados antes de autorização específica.

## SharePoint/OneDrive e Graph

[driveItem](https://learn.microsoft.com/en-us/graph/api/resources/driveitem?view=graph-rest-1.0)
expõe identidade do item e webUrl. [Upload de conteúdo](https://learn.microsoft.com/en-us/graph/api/driveitem-put-content?view=graph-rest-1.0)
documenta criação/substituição; para conta corporativa, a permissão delegada mínima
listada é Files.ReadWrite. Não se propõe acesso global ao tenant por padrão.
[Permissões Selected](https://learn.microsoft.com/en-us/graph/permissions-selected-overview)
permitem restringir recursos, exigindo consentimento e atribuição explícita de
acesso ao recurso; o caminho exato deve ser validado para cada operação na PoC.

[Versões](https://learn.microsoft.com/en-us/graph/api/driveitem-list-versions?view=graph-rest-1.0)
dependem da configuração do serviço/administrador e podem ter retenção finita;
não equivalem automaticamente a uma versão NITE para cada save. O NITE teria de
conciliar IDs/ETags/histórico e permissões sem apresentar um catálogo divergente.
Isso é trabalho futuro de arquitetura, não uma integração já comprovada.

[Conversão Graph](https://learn.microsoft.com/en-us/graph/api/driveitem-get-content-format?view=graph-rest-1.0)
documenta DOCX→PDF. A compatibilidade dos nossos modelos com Word web/Graph ainda
precisa ser ensaiada, inclusive campos, fontes, salvamento e download do item exato.
Não se deduz fidelidade de uma conversão Word desktop já aprovada.

## Dependências institucionais em aberto

Tipo de tenant e licenças atribuídas; administrador responsável; política de sites,
compartilhamento, consentimento e acesso da aplicação; ACLs por projeto; localização
e tratamento de dados; retenção/auditoria; custos, cotas, disponibilidade, suporte
e exportação de saída. Nenhum desses itens recebeu confirmação nesta pesquisa.

Recomendação condicional: se a instituição autorizar arquivos no Microsoft 365,
avaliar uma PoC de catálogo NITE + SharePoint com arquivos exclusivamente sintéticos
e menor acesso necessário. Se storage próprio for obrigatório, a rota CSPP não
pode ser assumida como alternativa disponível. Nenhuma rota Microsoft é GO nesta rodada.
