# Refinamento V8 — agarrão, raio e rastros de movimento

Implementação local de 02/11/2026, branch `beco`, HEAD `6d72c65`, preservando o trabalho preexistente. V8 aqui identifica arte/apresentação; não muda a versão do protocolo.

## Comportamento entregue

**Martial Artist 4:** o executor informa a pegada e o ponto real da pancada depois de resolver a colisão. A apresentação exibe oito frames de mãos/lançamento durante 200 ms e oito de impacto/dissipação durante 360 ms. São efeitos sobre os personagens existentes, sem poses corporais inéditas. As regras anteriores de deslocamento, dano 34, stun, custo e recarga permanecem; dano/controle continuam imediatos no servidor. O atraso entre as fases é somente visual.

**Raio:** os cinco slots e o contato elétrico usam amarelo, núcleo claro e bordas âmbar/laranja. Corte e corrente compartilham fonte, respeitando timings distintos. Ícones usam as mesmas folhas, pela infraestrutura da hotbar existente. Não houve alteração na estrutura, paleta navy/ciano ou hotkeys da interface.

**Água 5, Vento 3 e Raio 4/5:** o rastro usa histórico das posições efetivamente desenhadas. Não interpola até o destino da habilidade. Cada trecho é recortado ao caminho percorrido, com proteção sobre o corpo, espaçamento contínuo e dissipação de até 220 ms, ou 80 ms em movimento reduzido. O observador usa as mesmas posições interpoladas que desenham o personagem. O circuito conserva suas quatro pernas, em vez de descartar os eventos seguintes à primeira.

O servidor acrescenta um evento pontual de término na posição real, tanto ao concluir quanto ao interromper a investida. O cliente limita a coleta nesse término e não acompanha o caminhar normal posterior. Correções grandes/teleportes encerram a coleta sem desenhar uma faixa entre pontos distantes. Região, revisão, morte e desconexão limpam o estado. O histórico é limitado a 96 pontos por rastro e 16 rastros.

## Arquivos e integração

- `server/src/main/java/com/projetok/rpg/RpgSkillEngine.java`: eventos de pegada/impacto e término de dash. O circuito publica o primeiro trecho no lançamento e conserva os seguintes. Não altera geometria ou dano.
- `server/src/main/java/com/projetok/multiplayer/NetworkSkillPresentation.java`: histórico, recorte, dissipação, duas fases marciais e overlay V8.
- `server/src/main/java/com/projetok/multiplayer/RpgNetworkClientLauncher.java`: alimenta os efeitos depois de amostrar a posição usada pelo desenho, corrigindo o desacordo com o observador.
- `server/src/test/java/com/projetok/rpg/RpgSkillEngineTest.java`: término real, interrupção por root, quatro trechos e ponto de pancada junto à parede.
- `server/src/test/java/com/projetok/multiplayer/NetworkSkillPresentationTest.java`: ausência de pintura futura, curvas, término tardio, teleporte, pegada antes do impacto, limpeza, paleta e legibilidade das texturas reais em segmentos curtos.
- `server/src/test/java/com/projetok/multiplayer/GrabDashRefinementGuiTest.java`: cinco cenários com servidor local e dois clientes, capturas e expiração.
- `assets/skills/refined-v8/` e `tools/prepare_skill_assets_v8.py`: nove atlas/67 frames, manifesto opcional e reprodução determinística.

As mensagens e a serialização existentes da aplicação 7 carregam as fases por coordenadas; não há campo, tipo ou negociação novos. Os buffers mantêm os limites anteriores. Cliente e servidor devem carregar juntos esta revisão para apresentar as novas fases corretamente. Não houve mudança de banco, save, offline/Coop, launcher ou publicação.

## Arte e qualidade

Dez gerações/edições nativas foram preservadas; oito fontes aceitas foram empacotadas. As primeiras folhas elétricas precisaram de correção de transparência/margem. A lança usa um recorte de faixa explícito para conservar dois pixels que ultrapassavam a divisão aritmética. As primeiras texturas longas ficaram discretas quando recortadas em trechos curtos; água e vento receberam fragmentos próprios, mantendo os ícones e a arte principal dos dashes.

O script faz apenas recorte, registro e redução nearest, com uma escala por sequência, sem ampliar ou repintar. QA estrutural: nove clips, 67 frames, zero vazios, zero bordas ocupadas, zero ampliações, nenhum aviso. A avaliação de paleta exige predominância quente e tolera resíduo isolado de geração de até 0,1% dos pixels quentes; uma expectativa inicial de zero pixels frios identificou um único pixel residual na folha circular e foi substituída por esse critério proporcional. Não houve recoloração por código para mascarar o resultado.

A [reprodução e integridade](skills-preview-v8/verification.json) confirmam nove PNGs e dois metadados idênticos, 133 PNGs anteriores intactos e 415 originais intactos. [Prancha, GIFs e capturas](skills-preview-v8/README.md). Modelo/custo não retornados pela ferramenta ficam registrados como nulos.

## Verificação

Evidências em `.task-checkpoints/SKILLS40/refinamento-v8/`:

- `core-first.log`: 136 aprovados antes das regressões novas.
- `core-second.log`: a expectativa de zero pixels frios encontrou o resíduo isolado descrito acima; os demais casos passaram.
- `gui-first.log`: 139 aprovados, incluindo as duas janelas. A inspeção visual levou ao ajuste de legibilidade dos fragmentos.
- `gui-refined.log`: **142 aprovados**, sem falhas/erros/ignorados, incluindo 39 casos de apresentação, 97 de combate, a prova TCP de deslocamento e cinco cenários gráficos novos.
- `full-headless.log`: **915 casos, zero falhas/erros, 51 gráficos ignorados em headless**, sobre a implementação final.

Comando da suíte geral em `server/`:

```powershell
mvn test -o "-Dmaven.repo.local=../.maven-repository" "-Djava.awt.headless=true"
```

As rodadas usaram `-Dspiritgate.logs.dir` isolado no checkpoint. A seleção do complemento gráfico fica em `gui-selection.txt`, passada em `-Dtest` com `-Djava.awt.headless=false`. O resultado final é registrado no adendo abaixo. As capturas anteriores foram preservadas antes da rodada geral; capturas históricas recriadas pela suíte são arquivadas para manter suas exclusões preexistentes.

O complemento `gui-final.log` executou **99 casos: 98 aprovados e uma falha intermitente de foco** em `GamePanelsIntegrationTest` (`Evento sem componente focado`), depois de capturar os painéis em 640 px. Esse teste usa o modo gameplay sem habilidades; não foi alterado para contornar a falha. A repetição isolada, com evidências próprias em `panels-probe/`, aprovou o caso em `panels-probe.log`, sem alteração de código. Essa repetição não comprova estabilidade absoluta do teste de foco em lote.

O último XML por classe em `final-reports/`, resumido em `verification-tests.json`, reúne **915 casos aprovados em 246 classes**, sem falhas/erros/ignorados. O agregado combina suíte geral, complemento gráfico e repetição isolada; não é uma única invocação Maven. A ferramenta `tools/SkillsLocalPreview.java` também compilou com `javac --release 17` sobre as classes finais.

As 60 capturas anteriores foram restauradas. Três capturas históricas geradas pela suíte foram arquivadas em `suite-generated/`, com hashes, restaurando as 2.432 exclusões preexistentes no checkout. Os 133 PNGs anteriores foram reconferidos depois dos testes e permaneceram intactos.

## Uso, limites e reversão

Reabrir `tools/testar-skills.cmd` uma vez carrega as novas classes e folhas. A troca de domínio na janela permanece disponível. Estética e balanceamento continuam sujeitos à revisão humana; não foram executados testes físicos/Radmin ou launcher nativo.

Para reverter, comparar somente os hunks dos três fontes de produção com `baseline/` no checkpoint, retirando conjuntamente o histórico visual, a alimentação pelas posições exibidas e os eventos extras de fase. O overlay V8 pode ser retirado sem apagar as versões anteriores. Não usar reset global. Não houve commit, push ou publicação.
