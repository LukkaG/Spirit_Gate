# Refinamento V6 — cortes elementais e combo de vento

Revisão local de 02/11/2026 na branch `beco`, HEAD `6d72c65`. V6 neste título é a revisão da arte; não indica alteração da versão do protocolo. O pedido autorizou novas sprites e a conversão do primeiro golpe do combo de vento em skillshot, mantendo o retorno target.

## Resultado

- **Fogo 5:** dois cortes diagonais formam um X flamejante e se dissipam. Cinco X aparecem por onda visual, com pequeno escalonamento de formação. As cinco ondas acompanham os pulsos existentes de 12 ticks; cada área continua causando um único dano por vítima/pulso. Área, queimadura, custo e recarga permanecem.
- **Água 2:** arco de água percorre o giro da espada, fecha o anel e se desfaz. Centro transparente alinhado ao centro visual do corpo, acompanhando o jogador. É o único golpe do catálogo com o padrão corpo a corpo `MELEE_AREA`; não foram substituídos os vórtices aprovados na V5.
- **Martial 3:** oito rachaduras principais, chão vazio entre elas e dissipação discreta, sem a grande explosão de pedras. Raio de dano 110 preservado; desenho de 240 × 180, com fonte própria em resolução suficiente.
- **Vento 4:** agora `LINE`, smartcast frontal cardinal, sem alvo obrigatório. As três lâminas compõem um projétil que varre segmentos reais, termina no primeiro corpo elegível ou parede e apresenta dissipação orientada pelo disparo. O raio do projétil é 18; a área de empurrão continua 70. Só um contato com inimigo concede checkpoint, na posição realmente alcançada depois do deslocamento. A janela de 150 ticks começa nesse acerto.
- **Vento 5:** continua `TARGET`, dependente do inimigo/geração do checkpoint. Preserva a resolução autoritativa imediata da atração; as lâminas apresentam o movimento inverso, do checkpoint ao usuário. Um único evento substitui os dois eventos visuais sobrepostos anteriores. A animação não aplica dano extra.

Errar o skillshot consome normalmente o custo e a recarga do lançamento, mas não concede retorno. Uma tentativa inválida de retorno não consome energia nem checkpoint. Morte, troca de região/domínio e desconexão continuam cancelando ações pela infraestrutura existente.

## Implementação e arte

Os hunks de produção ficam em `server/src/main/java/com/projetok/rpg/RpgSkillCatalog.java`, `RpgSkillEngine.java` e `server/src/main/java/com/projetok/multiplayer/NetworkSkillPresentation.java`. A repulsão reutiliza a varredura de projéteis e o deslocamento com corpos atuais; não altera `CollisionService`. A apresentação carrega `assets/skills/refined-v6/manifest.properties` depois da V5, com fallback para versões anteriores se esse overlay não existir.

Quatro folhas aceitas geram cinco atlas/54 frames exportados: 48 frames principais e seis reaproveitados para dissipação de vento. As oito fontes/edições nativas, variantes e prompts estão em `assets/skills/refined-v6/art/`. A folha de água que cruzava células e a versão sísmica com fundo indevido foram rejeitadas antes de integrar a arte aceita. O fundo foi extraído com edição nativa, não por repintura em Python. As referências originais `225.png`, `275.png`, `277.png` e `304.png` orientaram formas novas; os anexos do usuário também orientaram o desenho.

`tools/prepare_skill_assets_v6.py` faz recorte, registro e redução nearest com uma única escala por sequência, sem ampliação ou recoloração. O limiar de alfa serve para medir o recorte dos resíduos quase transparentes, não para repintar o conteúdo. Modelo/custo não retornados pela ferramenta ficam nulos na proveniência.

QA: nenhum frame vazio, nenhum pixel na borda de exportação, nenhuma ampliação e nenhum aviso. O início ocupa entre 2,6% e 9,53% da cobertura do ápice e o final entre 1,09% e 3,79%, medidos com alfa ≥16. A reprodução independente gerou os cinco PNGs e dois metadados idênticos. Os **117 PNGs anteriores** e os **415 originais** permaneceram intactos por hash. [Prancha, GIFs e capturas de dois clientes](skills-preview-v6/README.md).

## Verificação

Evidências em `.task-checkpoints/SKILLS40/refinamento-v6/`:

- `core-first.log`: a primeira rodada identificou expectativas do empurrão instantâneo e do antigo apoio visual da Água 2. Os testes foram atualizados para aguardar contato real. A janela de expiração passou a ser contada a partir desse contato. Um cenário com dois mobs foi imobilizado durante o voo para isolar a geometria sob teste; as verificações de separação dos corpos e dano permaneceram.
- `core-second.log`: 132 aprovados, sem falhas/erros. Inclui regressões novas de lançamento sem alvo, seleção fora do eixo, parede, geração errada e alvo errado no retorno.
- `focused-final.log`: 144 aprovados, sem falhas/erros/ignorados, incluindo quatro cenários com duas janelas TCP, controles, Sentinela e deslocamento. Depois foi acrescentada a regressão da orientação vertical do impacto de vento.
- `full-headless.log`: suíte geral com **896 casos, zero falhas/erros, 46 gráficos ignorados**, sobre o ajuste de orientação final.

Comandos Maven em `server/`, offline, usando o cache existente:

```powershell
mvn test -o "-Dmaven.repo.local=../.maven-repository" "-Djava.awt.headless=true"
```

As rodadas também usaram `-Dspiritgate.logs.dir` isolado no checkpoint. A seleção completa da rodada gráfica fica em `gui-selection.txt`, passada em `-Dtest` com `-Djava.awt.headless=false`; resultados finais são registrados no adendo abaixo. Capturas anteriores dos diretórios de preview foram preservadas, e capturas históricas recriadas pela suíte foram arquivadas para manter suas exclusões preexistentes.

O complemento `gui-final.log` executou 83 casos: 82 aprovados e uma falha de preparação de foco em `SentryRefinementGuiTest` (`Evento sem componente focado`). A fixture focava o botão da habilidade, que pode perder foco ao entrar em recarga. O teste passou a focar e confirmar o painel do mundo, que continua habilitado durante a habilidade, preservando os eventos reais de teclado e todas as verificações do toggle. Nenhum controle de produção foi alterado para contornar essa falha; o fonte original do teste está no baseline.

A repetição `gui-focus-green.log` aprovou **33 casos**, incluindo Sentinela, 28 casos de apresentação e os quatro cenários novos com dois clientes. O último XML por classe, arquivado em `final-reports/` e resumido em `verification-tests.json`, reúne **896 casos aprovados em 245 classes, sem falhas/erros/ignorados**. Esse agregado combina a suíte geral, o complemento gráfico e a repetição focalizada; não é uma execução única de 896 testes com janelas habilitadas.

As 49 capturas anteriores guardadas antes do complemento foram restauradas. As três capturas históricas recriadas pela suíte foram movidas para `suite-generated/`, com hashes, mantendo as 2.432 exclusões preexistentes. Os 117 PNGs anteriores foram reconferidos após os testes e permaneceram intactos.

## Compatibilidade, limites e reversão

Não houve alteração de mensagens, serialização, negociação, saves, banco, launcher ou publicação. Cliente e servidor devem carregar juntos esta revisão de gameplay/apresentação sobre a aplicação 7 existente. Dano, colisão, recarga e checkpoint continuam autoritativos. Offline/Coop não receberam a nova apresentação multiplayer. A ferramenta continua permitindo troca de domínios sem fechar a janela.

Para carregar esta revisão, encerre a prévia antiga uma vez e execute `tools/testar-skills.cmd`. A estética e o balanceamento continuam sujeitos à avaliação humana. Não foram executados testes físicos/Radmin nem launcher nativo.

Para reverter, revisar somente os hunks desta rodada contra `baseline/` no checkpoint e retirar a referência ao overlay V6. Reverter conjuntamente catálogo, executor e apresentação do combo de vento. Os atlas anteriores permanecem intactos. Não usar reset global: o checkout contém trabalho preexistente extenso. Não houve commit, push ou publicação.
