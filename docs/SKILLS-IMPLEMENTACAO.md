# Implementação das 40 habilidades — Spirit Gate

**Atualização V9:** investidas de Água/Vento/Raio agora são cortes frontais sem alvo; o especial de trovão cria uma fase invisível, sem colisão e imune a dano, seguida de quatro pulsos de área. A autoridade continua no servidor e os formatos de rede permanecem. [Relatório e limitações](SKILLS-REFINAMENTO-V9.md), [capturas](skills-preview-v9/README.md).

Entrega local de 01/11/2026, branch `beco`, base `6d72c65`. O checkout já continha alterações e 2.432 exclusões históricas; elas não foram descartadas. O responsável autorizou expressamente a extensão negociada de rede e a implementação das quarenta técnicas.

## Resultado e acesso

Foram implementados oito conjuntos de cinco habilidades: Água, Vento, Raio, Fogo, Spirit, Aetheri, Luz e Artista Marcial. O executor é autoritativo e exclusivo das sessões sintéticas multiplayer. A interface acrescenta cinco slots, com atalhos padrão F5–F9, mira por clique para área/linha/cone e seleção de aliado para suporte. O catálogo detalhado, inclusive valores provisórios, está em [SKILLS-CATALOGO.md](SKILLS-CATALOGO.md).

A partida normal mantém a escolha de domínio no nível 15. A ferramenta [testar-skills.cmd](../tools/testar-skills.cmd) permite revisar os oito kits em uma sessão local efêmera de nível 20, com personagem auxiliar opcional. O [guia de teste](SKILLS-COMO-TESTAR.md) explica controles, Sentry, revive e troca de kit.

## Arquivos e comportamento

Os caminhos Java abaixo são relativos a `server/src/main/java/com/projetok/`.

| Área | Arquivos | Alteração |
|---|---|---|
| Regras | `rpg/RpgSkillCatalog.java`, `rpg/RpgSkillEngine.java` | Quarenta definições, energia, recargas, projéteis, dashes, multihit, DoT, controles e suporte. |
| Mundo | `rpg/RpgWorld.java`, `rpg/EnemyService.java` | Hooks localizados de colisão, movimento, dano e estado efêmero. Dano e mortes reutilizam o ledger compartilhado. |
| Rede | `multiplayer/SkillProtocol.java`, `Protocol.java`, `SessionSimulation.java`, `RpgSessionWorld.java`, `LocalSimulationServer.java`, `LocalClient.java` | Aplicação 7 negociada, intenções e eventos limitados, recargas privadas e validação de alvo/geração/revisão. |
| Apresentação | `multiplayer/NetworkSkillControls.java`, `NetworkSkillPresentation.java`, `NetworkHotbar.java`, `GameInputRouter.java`, `NetworkSettingsPanel.java`, `GameHudStyle.java`, `RpgNetworkClientLauncher.java`, `prototype/DebugPanel.java` | Slots e ícones, mira, destinatários, remapeamento e animações confirmadas por eventos. |
| Entrada atual | `prototype/rpg/RadminSessionPanel.java` | O fluxo atual passa a negociar explicitamente a aplicação 7. |
| Assets | `assets/skills/`, `tools/prepare_skill_assets.py`, `docs/skills-preview/` | Quarenta clips PNG, manifesto, origem/hashes, GIFs e pranchas de revisão. |
| Prévia | `tools/SkillsLocalPreview.java`, `tools/testar-skills.cmd` | Revisão local separada dos fontes de produção, com preferências isoladas. |

## Regras preservadas e decisões

- Os formatos anteriores de mensagens V1–V6 foram preservados. `LocalClient.gameplay()` continua negociando V6; `skillGame()` solicita a aplicação 7. Um cliente antigo participa com suas ações anteriores e não recebe os novos frames de skills.
- Cliente e servidor atualizados são necessários para as novas técnicas. O cliente envia slot, alvo/geração e mira; o servidor determina o domínio, dano, custo, alcance, colisão e resultado. Não há fallback silencioso de alvo inválido para ataque livre.
- `ROOT` restringe movimento; não equivale a `STUN`. A prisão de Água dura 60 ticks/2 segundos e permite ataques. Os demais números de balanceamento são propostas explícitas.
- Recargas usam o relógio privado do personagem; eventos e controles de mobs usam o relógio global da sessão. A entrada tardia de outro cliente não apaga recargas existentes.
- Vento 5 consome um checkpoint exclusivo criado por Vento 4, com geração e validade verificadas. Repulsão e puxão usam corpos atuais e respeitam paredes.
- Corrente de Raio tem até quatro impactos; a repetição no mesmo alvo reduz o dano. Sentry fornece cinco disparos rápidos por ataques separados. O combo marcial entrega seis impactos na duração declarada.
- Luz 1/5 curam aliados ou ferem mobs, sem criar PvP. Cura comum não revive. Revive exige aliado derrotado e apoio livre. Spirit 4/5 não causam dano.
- Não houve mudança de formato de saves, banco ou migração. Offline/Coop mantêm seus caminhos anteriores. Launcher, publicações e distribuição existente não foram atualizados.

## Arte e origem

Foram examinados 415 PNGs em `C:/Users/lucas/Downloads/animations`. Os originais foram preservados. O pacote final contém 40 animações e 354 frames, com oito variantes editadas pela ferramenta nativa `image_gen`, além de recortes e composição da arte fornecida. As alterações de paleta e detalhes mantêm a relação com as fontes; não se declara todo o material como desenho original.

`assets/skills/provenance.json` registra os arquivos, hashes, grades e transformações. `assets/skills/variants/` preserva as edições e prompts. Modelo e custo não foram informados pela ferramenta nativa. O script usa Pillow para recortar, organizar, compor e escalar por vizinho mais próximo; não repinta as fontes. A reprodução separada gerou PNGs idênticos por SHA-256.

O QA dos assets está em `docs/skills-preview/qa.json`: 354 frames, sem frames vazios, falhas de grade ou avisos. Os PNGs de runtime totalizam aproximadamente 1,75 MB. Essas verificações técnicas não substituem aprovação artística dentro do jogo.

## Verificação

Os logs completos ficam em `.task-checkpoints/SKILLS40/`. Compilação com JDK 17, Maven offline e o cache já existente, sem instalar dependências.

- `focused-first.log`: 35 testes aprovados de catálogo, protocolo, assets e regressões de combate.
- `engine-first.log`: 66 testes aprovados, incluindo 53 casos do executor, catálogo e dois clientes TCP.
- `java-full.log`: suíte completa com 798 testes, zero falhas/erros e 30 casos gráficos abortados pela execução headless.
- `gui-first.log`: uma tentativa interrompida em compilação por argumento excedente na fixture nova de teste; corrigido sem mudar o runtime.
- `gui-final.log`: 96 execuções (39 casos nas classes gráficas e 57 de mecânicas), com duas falhas no injetor AWT de um teste antigo de teclado, que encontrou foco temporariamente nulo antes do dispatch. As novas habilidades e os demais casos passaram nessa rodada.
- `gui-isolated.log`: 61 aprovados, zero falhas/abortos; inclui os 57 casos atuais do executor, três casos de input e a GUI das skills, com dois clientes e nove slots em 1000×700 e 640×480. A captura inicial usa 1200×800.
- `gui-focus-order.log`: quatro aprovados, repetindo a ordem original entre o teste da ficha e o de input. Não foi necessário alterar o roteador de produção nem o injetor `SwingKeys`; a intermitência de foco continua registrada.
- A ferramenta foi inicialmente compilada com `javac --release 17`. Após o relato de abertura descrito no adendo abaixo, o seletor e o caminho real até a partida também foram exercitados; a revisão humana da experiência continua pendente.

As rodadas se sobrepõem; seus totais não devem ser somados como testes únicos. Testes novos cobrem as quarenta técnicas, ordem temporal de impactos, controles, ledger/morte/respawn, parede fina, corpos atuais, checkpoint, seis golpes marciais, dano zero, suporte, bytes legados, rejeições de wire, clientes V6/V7 e entrada tardia.

Comando da suíte completa, executado dentro de `server/`:

```powershell
mvn test -o "-Dmaven.repo.local=C:\Users\lucas\Documents\GitHub\Projeto-K\.maven-repository" "-Djava.awt.headless=true"
```

As rodadas focadas usam o mesmo cache e `-Dtest=...`. As gráficas usam `-Djava.awt.headless=false`. Os 30 abortos da rodada completa foram cobertos pelas classes da complementação gráfica. Os quatro testes adicionais do executor foram executados após a rodada completa, sem mudança de runtime entre essas validações.

O conjunto dos relatórios mais recentes por classe reúne 802 resultados aprovados, sem falhas, erros ou abortos restantes (`verification-final.json`); esse agregado combina rodadas e não é o total de uma única invocação Maven. As tentativas anteriores e a intermitência de foco permanecem nos logs.

Integridade final: os 2.273 arquivos existentes no snapshot pré-suíte de assets/docs/saves permaneceram idênticos, sem ausências. Três capturas históricas foram regeneradas pelos testes; suas saídas foram arquivadas em `suite-generated/` e removidas dos caminhos que já estavam excluídos antes da tarefa. O Git conservou as 2.432 exclusões históricas. Evidências em `integrity-final.json`; sete novos documentos/capturas intencionais foram mantidos.

Capturas principais:

- [Vórtice no lançador](skills-preview/runtime/cliente-water-vortex.png) e [no observador](skills-preview/runtime/observador-water-vortex.png).
- [Janela de habilidades](skills-preview/runtime/habilidades-agua.png).
- Hotbar com nove slots em [1000×700](skills-preview/runtime/hotbar-nove-slots-1000.png) e [640×480](skills-preview/runtime/hotbar-nove-slots-640.png).

## Limites, reversão e próximo passo

Continuam pendentes a aprovação visual humana dos sprites/escala/timing, balanceamento jogado, teclado físico, Radmin entre máquinas e launcher nativo. Aparências próprias de Spirit/Aetheri dependem do catálogo racial existente; efeitos não acrescentam animações corporais inéditas. O circuito de Raio 5 usa quatro segmentos de dash, como descrito no catálogo.

Os EXEs/ZIPs anteriores continuam com seus fontes empacotados anteriores. Não houve commit, push, release, publicação ou envio a `updates`.

A reversão deve ser localizada, preservando as mudanças anteriores deste checkout. Há cópias de referência dos arquivos centrais em `.task-checkpoints/SKILLS40/baseline/` e snapshot pré-suíte de assets/docs/saves em `pre-suite/`. Não usar `git reset`, `clean` ou restauração global: diversos arquivos envolvidos já eram modificados ou não rastreados antes da tarefa. Remover a extensão e seus pontos de chamada em conjunto, após revisar o diff, evita deixar um cliente negociando recursos ausentes.

Próximo passo sugerido: abrir a prévia, revisar um kit por vez com os roteiros do guia e anotar ajustes de aparência e balanceamento antes de preparar qualquer distribuição.

## Adendo — abertura da prévia sem indicação de seleção

Em 01/11/2026, o responsável relatou o console parado em “Compilando as fontes locais”. A inspeção confirmou que Maven e javac já haviam terminado: o processo Java aguardava o seletor modal `Spirit Gate | Revisar habilidades`. A caixa existia e estava visível, mas o console não informava essa etapa e ela não havia recebido destaque. O stack da thread principal estava em `choose()`/`invokeAndWait`, com o EDT aguardando `Dialog.show`.

A correção ficou em `tools/testar-skills.cmd`, `tools/SkillsLocalPreview.java` e neste guia de uso: console com três etapas e saída Maven visível; logs da aplicação para seleção, sprites, servidor, janela e sincronização; diálogo nomeado, centralizado e com foco solicitado. O destaque acima das demais janelas é temporário e liberado na ativação, por timer ou ao fechar. Erros do script permanecem visíveis. A compilação da ferramenta passou para `preview-classes-v2`, preservando as classes do processo anterior ainda aberto.

Foi acrescentado `tools/SkillsLocalPreviewProbe.java`, fora dos fontes de produção. O probe conferiu oito kits, posição/visibilidade do diálogo, cancelar, selecionar e abrir a partida real de Água no nível 20; confirmou domínio, PNGs e encerramento do cliente/servidor. O teste do CMD executou a cadeia real Maven → javac → seletor, verificou as três etapas e cancelou apenas sua própria janela, com saída 0. Não foi alterado código de gameplay ou protocolo nesta correção.

Evidências: `.task-checkpoints/SKILLS40/preview-fix/evidence/result.json`, capturas do seletor/partida, `probe.log`, `startup-console.log` e `startup-result.json`. As versões anteriores dos arquivos corrigidos estão em `preview-fix/baseline/`. A reversão desse adendo é localizada nesses arquivos da ferramenta; não requer restauração do restante do projeto.

## Adendo — confirmação de domínio com personagem auxiliar

O responsável encontrou outro problema ao abrir Vento com o auxiliar habilitado: “O servidor não confirmou o domínio de treino”. A prévia aguardava o principal e enviava sua escolha antes de aguardar o aliado. O `RESUME` do primeiro cliente não implica que o segundo terminou a sincronização; enquanto algum ingresso está pendente, o servidor mantém a barreira global e recusa ações de gameplay.

A reprodução controlada segurou o renderer do auxiliar: o principal recebeu ficha e skills no tick zero, enfileirou `CHOOSE_DOMAIN wind`, mas o servidor corretamente conservou `none`. Após liberar o auxiliar, o mesmo pedido foi aceito. Isso confirmou a causa sem depender da velocidade da máquina (`preview-domain-fix/race-before.log`).

`tools/SkillsLocalPreview.java` agora aguarda todos os personagens da prévia antes de escolher qualquer domínio. `chooseDomain` também aguarda uma ficha temporariamente indisponível até o prazo, em vez de interpretar `null` como recusa imediata. Não foram acrescentados reenvios cegos, atrasos fixos de inicialização nem mudanças nas validações do servidor.

O probe da ferramenta passou a cobrir a barreira com renderer retido e a inicialização dos oito kits com o aliado habilitado, além do caminho sem aliado. Ele verifica domínio principal e Luz do auxiliar, skills, PNGs e encerramento dos dois clientes/servidor. Os resultados e capturas ficam em `.task-checkpoints/SKILLS40/preview-domain-fix/evidence/`, com log em `probe.log`. Os backups desta correção estão em `preview-domain-fix/baseline/`.

A conferência de hashes dos 19 arquivos de produção registrados anteriormente não encontrou mudanças nesta correção (`production-integrity.json`). A correção e seus testes ficam na ferramenta de treino; gameplay, protocolo e dados pessoais foram preservados.

## Adendo — deslocamento do inimigo encerrava o servidor

O uso do Body Slam marcial provocou `IllegalStateException: Varredura exige posição inicial livre`, com origem em `RpgWorld.skillDisplace`. A reprodução em contato diagonal entre jogador em `(140,300)` e mob em `(165,325)` produziu a mesma cadeia de chamadas da captura do responsável.

O helper usava o cenário dilatado e os blocos de jogador preparados para a caminhada do personagem. A IA do inimigo, entretanto, usa cenário bruto e contatos circulares. Uma posição válida para esses círculos podia ser considerada ocupada no contexto diferente usado pela habilidade. Havia situações equivalentes junto a paredes/bordas e quando outros mobs já estavam sobrepostos.

Alterações localizadas:

- `rpg/SharedRpgEncounter.java`: fornece o contexto de deslocamento com geometria bruta da região e posições atuais dos jogadores como círculos. Os outros mobs vivos continuam bloqueando o deslocamento com seus corpos atuais. A IA reutiliza a mesma montagem dos corpos dos jogadores, preservando suas regras.
- `rpg/RpgWorld.java`: `skillDisplace` usa esse contexto e consulta a origem antes de iniciar a varredura. Se a origem já estiver ocupada, mantém a posição e informa que não há espaço; dano e demais efeitos do golpe seguem a regra existente de deslocamento bloqueado. Não há teleporte de recuperação, mudança de hitbox ou captura genérica da exceção.
- `rpg/RpgSkillEngineTest.java`: seis regressões cobrem contato diagonal, cenário bruto junto à parede, movimento paralelo à parede e retorno, mobs sobrepostos, corpo atual de outro jogador e borda do mapa.
- `multiplayer/SkillDisplacementServerTest.java`: dois clientes TCP V7 usam o golpe no contato diagonal que provocava o crash. O teste confirma dano, stun, retomada da perseguição e continuidade dos snapshots/conexões.

Validação em `.task-checkpoints/SKILLS40/displacement-fix/`: `red.log` reproduziu seis falhas antes da correção; `green-final.log` registrou **129 testes aprovados, sem falhas/erros/abortos**, incluindo os 63 testes do executor e a prova TCP. A primeira rodada após o ajuste identificou uma expectativa antiga que exigia distância estritamente maior que o raio até a parede. O teste foi tornado mais preciso: exige o centro em `231` para parede em `250` e raio `19`, verifica tangência livre na geometria bruta e mantém as verificações do retorno. A regra de tangência do `CollisionService` permaneceu inalterada.

Foram preservados protocolo, limites, paredes, autoridade, saves e os caminhos offline/Coop. O serviço global de colisão não foi modificado por este adendo. Backups dos dois fontes e do teste preexistente estão em `displacement-fix/baseline/`; a reversão deve revisar apenas esses hunks, preservando o restante das alterações locais. É necessário encerrar a prévia anterior e abrir o script novamente para carregar os fontes corrigidos.

## Adendo — refinamento de controles, contatos e arte V2

O retorno seguinte foi tratado em uma revisão localizada de smartcast frontal, mira de área por mouse/WASD/setas, seleção de aliados e suporte de Luz, trajetórias de projéteis, contato do circuito elétrico e arte. As três últimas técnicas de Água, Raio, impacto marcial e flechas glaciais receberam a sobreposição visual V2, preservando os PNGs anteriores. Fogo 2/4 passa a apresentar a frente avançando desde o lançador e a erupção apenas na chegada.

O [relatório V2](SKILLS-REFINAMENTO-V2.md) registra comportamento, arquivos, compatibilidade, testes, limitações e reversão. A suíte headless contabilizou 845 casos sem falhas/erros, com 33 gráficos abortados; o complemento gráfico aprovou 53 execuções e a última verificação localizada aprovou 13. O agregado mais recente por classe reúne 847 casos aprovados; não é uma única invocação Maven. O [guia de uso](SKILLS-COMO-TESTAR.md) já descreve os controles atuais e a seleção de aliados.

## Adendo — revisão V3 de Martial Artist e Aetheri

A revisão seguinte alinhou disparos ao centro visual e aos quatro lados do personagem, ampliou a cratera e o alcance/efeito do combo marcial e substituiu o gelo por energia azul em todo o kit Aetheri. Após confirmação do responsável, o básico de arco passou a skillshot frontal fora da Sentinela. A segunda técnica agora imobiliza por quatro segundos, converte SPACE em tiros target com cadência por agilidade e permite cancelamento ao conjurar novamente.

O [relatório V3](SKILLS-REFINAMENTO-V3.md) registra execução, sobreposição de arte, compatibilidade e limitações. Suíte geral: 862 casos sem falhas/erros, sendo 34 gráficos abortados no modo headless; complemento gráfico: 60 aprovados; ajuste final de alinhamento: 112 aprovados. O agregado mais recente por classe reúne 862 aprovados. Os PNGs V1/V2 foram preservados, sem publicação ou alteração de saves.

## Adendo — agarrão, raio e rastros V8

A [revisão V8](SKILLS-REFINAMENTO-V8.md) integra pegada/body slam em duas fases, paleta amarela/laranja de raio e rastros limitados às posições percorridas, acompanhando também o personagem do observador. Nove atlas/67 frames complementam as versões anteriores. A suíte geral executou 915 casos sem falhas/erros, com 51 gráficos ignorados em headless. O complemento e uma repetição isolada de um teste com foco intermitente reúnem 915 aprovados no agregado por classe. [Prévias](skills-preview-v8/README.md).

## Adendo — presença visual do sísmico V7

O [sísmico V7](skills-preview-v7/README.md) mantém a composição limpa, com fissuras secundárias, bordas mais legíveis, pequenas lascas animadas e poeira curta. São 16 frames no mesmo tamanho de desenho 240 × 180; combate e raio de dano permanecem. Os 32 testes focalizados passaram, incluindo dois clientes reais. Os 130 PNGs anteriores foram preservados por hash.

## Adendo — cortes V6 e combo de vento

A [revisão V6](SKILLS-REFINAMENTO-V6.md) traz cortes em X para Fogo 5, giro de espada em Água 2 e sísmico mais simples. Vento 4 passa a skillshot frontal com checkpoint apenas no acerto; Vento 5 permanece target e mostra o percurso inverso. São cinco atlas/54 frames, com as versões anteriores preservadas. A suíte geral executou 896 casos sem falhas/erros, com 46 gráficos ignorados em headless. O complemento e a correção localizada da preparação de foco de um teste resultam em 896 aprovados no agregado do último XML por classe. [Prancha e capturas](skills-preview-v6/README.md).

## Adendo — ciclos visuais V5

A [revisão V5](SKILLS-REFINAMENTO-V5.md) acrescenta 36 frames para a erupção e os vórtices de Água/Fogo, com nascimento, ápice e dissipação sem loop. Preserva combate, protocolo e os 111 PNGs anteriores. A rodada focalizada aprovou 128 execuções e a gráfica 31, equivalentes a 135 casos distintos. [Prancha e capturas](skills-preview-v5/README.md); a aprovação estética humana permanece pendente.

## Adendo — refinamento V4 de impactos e arte

O [relatório V4](SKILLS-REFINAMENTO-V4.md) registra o término dos projéteis no contato, fissuras abertas progressivamente, três cortes de vento separados por 0,1 segundo, chuva de flechas com chegada ao chão e novas fontes de Martial Artist e Água. Nove atlas/75 frames complementam os anteriores, preservados por hash. A suíte geral executou 876 casos sem falhas/erros, com 39 gráficos ignorados em headless; o complemento gráfico aprovou 69 execuções. O agregado mais recente por classe reúne 876 aprovados em 243 classes. A revisão humana da arte e do balanceamento permanece pendente.

## Adendo — trocar domínio com a prévia aberta

A ferramenta ganhou um seletor com dez domínios e o botão **Trocar kit / reiniciar treino**. A troca reinicia somente a sessão sintética, conserva as janelas e seus limites e permite testar outro kit/raça sem fechar a partida. As regras de domínio do jogo normal permanecem. O [relatório da troca](SKILLS-TROCA-DE-KITS.md) registra o descarte de recursos, as provas gráficas/TCP com e sem auxiliar e a reversão localizada.
