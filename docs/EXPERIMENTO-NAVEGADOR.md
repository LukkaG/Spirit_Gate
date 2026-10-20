# Spirit Gate — teste público de sprites no navegador

Publicado por solicitação explícita do responsável, em adaptação isolada para ampliar os testes visuais.

**Site público:** https://spirit-gate-sprites-lab.eageremu2049.chatgpt.site

**Branch secundária:** https://github.com/LukkaG/Spirit_Gate/tree/browser-sprites-preview

Commit publicado: `08e30be31d2812c60579035e2b271c242d899aa5`. A branch tem checkout e histórico independentes. O checkout original permanece em `beco`, HEAD `19293c1`; a branch de atualizações desktop `updates` não foi modificada. Não há merge automático da experiência web na produção.

## Como testar e obter feedback

Usar computador com teclado, escolher uma das quatro configurações e clicar em **Iniciar teste**. O cliente entra diretamente com um personagem visitante. WASD movimenta e F5 salva no filesystem virtual do navegador. Os saves não são importados do computador nem enviados para um servidor de jogo. Cada nova abertura do teste começa com um visitante novo; o menu continua acessível pelo botão de salvar/voltar.

O primeiro carregamento baixa o ambiente Java e pode demorar mais que as aberturas seguintes. A versão é experimental, sem multiplayer. O alvo de teste é desktop; não foram implementados controles de toque.

As quatro configurações são: 50%/150 ms, 40%/220 ms (padrão), 35%/220 ms e 40%/280 ms. A página ajuda a preparar feedback de tamanho, ritmo e comentários. **Copiar avaliação** permite compartilhar o texto manualmente; **Enviar pelo GitHub** abre uma issue pré-preenchida, com login do testador. Issues do repositório estão habilitadas. Nada é enviado automaticamente e não há banco central de avaliações anônimas nesta entrega.

## Isolamento e adaptação

`browser-preview/` contém fontes/copias próprias, recursos, build, página estática e metadados do Site. Os arquivos não são symlinks para a produção. `snapshot.json` registra os hashes do código desktop de origem; `check.py` confirmou os mesmos 135 arquivos de produção após a publicação, além dos 12 PNGs originais intactos.

O runtime CheerpJ 4.3 executa Java 17/Swing no navegador, usando o CDN oficial. Não foi redistribuído o runtime proprietário. Créditos estão visíveis no site; a documentação oficial admite projetos pessoais na Community License. Referências: https://cheerpj.com/docs/licensing e https://cheerpj.com/docs/reference/cheerpjInit.

Adaptações exclusivas da cópia web:

- `BrowserLauncher` cria um visitante e inicia o RPG, sem exigir cadastro/criação manual para avaliar sprites.
- O template de apresentação usa `java.util.Random`: o provider LXM do JDK desktop não está disponível nesse runtime web. Regras de RPG não foram alteradas.
- JAR e PNGs são baixados por fetch normal, verificados por SHA-256 e montados em memória em `/str/`. Esse mount não aceita subdiretórios; os PNGs mantêm seus nomes originais, com pasta virtual selecionada por propriedade exclusiva web. A lista explícita vem dos metadados, mantendo checagem de hash/dimensões/alfa.
- Dados graváveis usam `/files/spirit-gate-web`, separados dos assets e dos saves desktop.
- F5 é encaminhado ao jogo, evitando recarregar a página durante o teste.

Essa carga em memória resolveu uma diferença real da hospedagem: os pedidos HTTP Range tentados pelo runtime não eram preservados pelo host estático, embora o servidor local os suportasse. A publicação final não depende de Range para os arquivos do jogo.

## Validação real

Compilação Java 17 da cópia web; validação dos 12 hashes PNG; ausência de DB, servidor Coop e testes no JAR; preservação da produção desktop por hash.

QA no Edge headless, primeiro local e depois no URL público, sem autenticação de usuário no Site:

- Janela real do RPG com sprite do personagem carregada.
- Teclado nas quatro direções, com capturas e comparação de deslocamento do personagem por pixels.
- F5 sem reload e diálogo real **“Progresso salvo”** inspecionado.
- Avaliação preparada com a configuração selecionada, tamanho, ritmo e comentários.
- Nenhuma exceção Java, recusa do conjunto de sprites ou erro de rede no log final.

Evidências locais, ignoradas pelo Git: `browser-preview/qa/game-results.json`, `browser-latest.log`, `public-movement-pixels.json`, `idle.png`, `walk-right/front/left/back.png`, `save.png` e `game-full.png`. O QA não enviou issue nem feedback em nome de terceiros. Não equivale a homologação artística ou teste em todos os navegadores/dispositivos.

Deploy: Sites, projeto `appgprj_6ad6bfdc75f08191acf72df444ed0500`, versão salva 2, deployment `appgdep_6ad6c415e30881918cd60af07c352156`, estado retornado `succeeded`. Acesso público configurado para permitir compartilhar o link sem convite individual.

## Manutenção

Atualização posterior solicitada: o padrão publicado passa a **50% e 250 ms por frame**, preservando as alternativas de comparação. Versão Sites 3, commit `1bde27d79ef1465870a6de58a7c5b5ec2892d353`, deployment `appgdep_6ad6d4752f088191bf545dfa9c1958d8`, resultado `succeeded`. O QA no endereço público confirmou as quatro direções, os parâmetros do feedback e F5 sem reload, sem exceções Java. Os 12 PNGs originais foram verificados por hash. O snapshot de verificação incorpora somente o retorno de escala desktop já realizado anteriormente; este deploy não modifica o desktop.

Na cópia web: `python build.py`, `python check.py`, `node serve.mjs` e `node qa-game.mjs`. O README da branch descreve a cópia isolada. Para republicar, sincronizar o estado exato da cópia com o projeto Sites e salvar/deployar nova versão; atualizar apenas a branch GitHub não altera o site automaticamente. A versão desktop e o atualizador mantêm seus próprios processos de distribuição.

Se algo der errado no experimento, manter o desktop e desativar/reverter somente o Site ou a branch secundária. Nenhum ajuste desta cópia foi aplicado diretamente no cliente desktop.
