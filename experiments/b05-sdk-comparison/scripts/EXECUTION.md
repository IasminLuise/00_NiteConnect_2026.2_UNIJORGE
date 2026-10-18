# Reprodução B05.0.4

Usar Node 24, Python com python-docx 1.2.0 e dependências fixadas pelo lockfile.
No Windows de execução, runtimes e Playwright vieram do ambiente de ferramentas.
Não adicionar Playwright ao produto nem usar dados oficiais em CI.

    python -m unittest discover -s experiments/b05-sdk-comparison/tests -v
    cd experiments/b05-sdk-comparison/web
    npm ci --ignore-scripts --no-audit --no-fund
    npm run vendor:apryse
    npm run build
    npm run dev

O servidor fica somente em http://127.0.0.1:5173. Assets do Apryse são copiados
do pacote instalado para public/lib, ignorado pelo Git. A CSP local não é uma
política de produção. Não publicar o diretório dist com documentos privados.

Em outro terminal, a partir da raiz:

    python experiments/b05-sdk-comparison/scripts/make_synthetic.py --out experiments/b05-sdk-comparison/.private/new-run/fixtures/compat-synthetic.docx
    python experiments/b05-sdk-comparison/scripts/make_synthetic.py --extended --out experiments/b05-sdk-comparison/.private/new-run/fixtures/compat-extended.docx
    node experiments/b05-sdk-comparison/scripts/browser_probe.cjs --run experiments/b05-sdk-comparison/.private/new-run --engine superdoc --tag noop --playwright PLAYWRIGHT_DIR --chrome CHROME_EXE

Substituir os dois últimos valores por paths absolutos do pacote Playwright e do
Chromium. Requer Playwright com routeWebSocket. As saídas ficam em browser/TAG,
que deve ser novo. O runner recusa sobrescrever esse checkpoint. O input default
é a fixture básica; --source pode apontar para a estendida/candidata no mesmo run.

Fases: --phase noop, edit, remove. Edit insere XYZ no primeiro título sintético;
remove espera a candidata editada como --source. Os alvos são próprios da fixture,
não um editor automatizado genérico de modelos oficiais. --lifecycle true confirma
export com seletor alterado e reabertura na mesma página; --settle 8000 estabiliza
o snapshot de paginação. Apryse não teve alteração por teclado confirmada nesta
execução: não reutilizar seu export como gabarito de edição positiva.

    python experiments/b05-sdk-comparison/scripts/ooxml_gate.py --before SOURCE.docx --after CANDIDATE.docx --allow-paragraph-id-additions --allow-plain-text-run-segmentation --out experiments/b05-sdk-comparison/.private/new-run/comparison.json

Acrescentar --expected-prefix XYZ somente no caso edit confirmado. Sem as duas
flags, o gate mantém a política estrita. Ler comparison-policy.md: nenhuma flag
dispensa formatação, campos, mídias, relações ou remoção de partes de notas.
CLI escreve o resultado; seu exit 0 indica inspeção concluída, **não** PASS.

Word local:

    ./experiments/b05-sdk-comparison/scripts/word_probe.ps1 -JobsFile JOBS_JSON_PRIVADO

Jobs contêm id/source/pdf/json/update_fields; todos os caminhos devem estar em
.private. DOCX abre readonly; macros e atualização de links desabilitadas. Campos
locais de data/paginação podem ser atualizados em memória. A exportação PDF Word
pode atualizar SAVEDATE novamente: controlar o instante, não acusar diferença de
horário como alteração visual do SDK. Não salvar por cima da origem.

Evidências desta execução ficam em .private/b0504: incoming (pacote recebido),
manifests, fixtures, browser, word, análises de deltas e logs RED/GREEN. Não estão
no PR. Não há manifesto histórico B05.0.3 disponível para repetir os 177 hashes;
os originais atuais têm congelamento próprio. Nenhum resultado histórico foi
apagado ou alterado por esta implementação.

Regra de parada: C0 oficial SuperDoc falhou na prévia/estrutura; não executar C1
ou UX completa. Apryse oficial permanece bloqueado. Sem compra, chave ou modelos
em servidores de terceiros. Pare o Vite com Ctrl+C após o ensaio. G1/G2 não mudam.
