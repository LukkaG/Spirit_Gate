# Spirit Gate - seleção de alvo e combate dirigido

Fechamento: 01/11/2026. Branch beco. Base: 6d72c6595dee6dbe4dea832fe6047ee81f51d4a3.
Entrega privada: 0.223.0-private. Evolução posterior às TASKs 213-222.

## Regras confirmadas pelo responsável

TAB seleciona o primeiro inimigo elegível e alterna para o próximo a cada nova pressão. A lista começa pelos mais próximos, com desempate pelo ID. Ao chegar ao fim, volta ao primeiro. A seleção inclui mobs e chefes vivos do mapa/região atual, mesmo fora da câmera; não inclui jogadores, NPCs, entidades de outra região ou inimigos derrotados. O limite de alcance pertence à ação, não à possibilidade de selecionar um inimigo.

O ataque básico funciona sem alvo, conservando a regra de acerto existente. Com alvo, vira o personagem em direção à vítima selecionada, sem caminhar até ela. Só causa dano se esse alvo estiver no alcance real do ataque e sem obstáculo. Não troca silenciosamente para um inimigo mais próximo se o selecionado estiver longe ou bloqueado.

Shunpo ofensivo tem alvo opcional. Com alvo, aproxima-se dele dentro do alcance de 240 unidades, respeitando linha de visão, colisão, caminho e espaço de chegada. Mantém a distância de aproximação preexistente de 42 unidades do alvo. Sem alvo, avança 70 unidades na direção em que o personagem está olhando, parando nos limites e obstáculos. O avanço usa a mesma distância do Shunpo defensivo atual, sem acrescentar dano.

O nível mínimo 20, o custo de 25 de energia e a recarga compartilhada de 90 ticks/3 segundos continuam iguais. Se o avanço estiver totalmente bloqueado, não consome energia nem inicia recarga. Quando parte do deslocamento é possível, executa o avanço possível e paga o custo normal, como a esquiva existente.

Shunpo defensivo continua livre, recuando na direção oposta à orientação atual. Nenhuma habilidade atual exige alvo obrigatório, conforme confirmação do responsável. Essa exigência será aplicada às próximas skills quando forem definidas; as rotinas de alvo exato, geração, alcance e obstáculos já estão disponíveis para reutilização. Não foram inventadas habilidades novas.

## Interface e teclado

A HUD mostra nome, HP confirmado, distância e alcances de referência do alvo. Um contorno ciano marca a entidade no mundo. O botão X desse painel limpa a seleção; o ataque básico continua utilizável. O destaque de alvo na partida V6 passa a refletir a seleção explícita, sem ser trocado pelo último hit.

Durante gameplay, TAB seleciona/troca o alvo inclusive com foco em um botão da HUD, sem abrir esse botão. Dentro de inventário, missões, perfil, menu e configurações, TAB continua navegando pelos controles. Campos de texto e captura de atalhos continuam protegidos. SHIFT+TAB mantém a navegação reversa. SPACE ataca; ENTER ativa o botão focado. As configurações exibem a nova distinção de TAB.

A hotbar distingue alvo fora do alcance para o Shunpo. Sem alvo, informa o avanço na direção atual. O indicador de distância ajuda a leitura, mas não autoriza uma ação: o servidor decide alcance, obstáculos, nível, energia e recarga.

Os layouts foram verificados em 1366x768, 1000x700 e 640x480. Não foi criado timer adicional por alvo; a HUD usa a atualização existente.

## Implementação e autoridade

RpgWorld recebeu entradas para ataque com alvo exato, Shunpo dirigido e Shunpo à frente. As chamadas antigas de ataque e Shunpo continuam disponíveis aos fluxos anteriores. A rotação é estado do servidor e aparece nos snapshots dos dois clientes. A direção aceita por um comando de movimento também é registrada imediatamente, evitando que mover/parar no mesmo tick deixe o avanço apontando para a direção antiga.

RpgSessionWorld guarda a seleção por jogador e a geração do spawn. Revalida antes de agir e limpa a seleção na morte do jogador/inimigo, mudança de região, retorno e sincronização/troca de revisão. Um inimigo que renasce com o mesmo ID não herda a seleção anterior.

O cliente envia a intenção, incluindo se queria agir sem alvo ou contra um ID específico. As operações SELECT_TARGET, BASIC_ATTACK e SHUNPO_OFFENSIVE foram acrescentadas ao fim do enum de gameplay, preservando os ordinais e formatos anteriores. O comando com alvo inválido é recusado; não se transforma em ataque livre ou avanço para frente. A seleção local serve à resposta visual, enquanto o servidor revalida identidade, região, vida e geração.

Os frames e DTOs anteriores não foram reformulados. A extensão é do gameplay V6 e requer cliente e servidor atualizados para o recurso; servidor anterior não conhece as novas operações. Aplicações legadas, offline e Coop conservam seus caminhos anteriores. Não há PvP novo, alteração de saves ou instalação de dependências.

A consulta de obstáculos para alvo explícito usa traçado contínuo. Um teste novo identificou que a consulta antiga por amostras podia deixar passar uma parede de um pixel; o caminho dirigido agora rejeita esse caso sem mudar a consulta histórica dos fluxos anteriores.

## Arquivos principais

- server/src/main/java/com/projetok/rpg/RpgWorld.java
- server/src/main/java/com/projetok/multiplayer/GameplayProtocol.java
- server/src/main/java/com/projetok/multiplayer/RpgSessionWorld.java
- server/src/main/java/com/projetok/multiplayer/LocalClient.java
- server/src/main/java/com/projetok/multiplayer/GameInputRouter.java
- server/src/main/java/com/projetok/multiplayer/NetworkCombatPresentation.java
- server/src/main/java/com/projetok/multiplayer/NetworkTargetPanel.java (novo)
- server/src/main/java/com/projetok/multiplayer/NetworkGameHud.java
- server/src/main/java/com/projetok/multiplayer/NetworkHotbar.java
- server/src/main/java/com/projetok/multiplayer/NetworkSettingsPanel.java
- server/src/main/java/com/projetok/multiplayer/RpgNetworkClientLauncher.java

Testes novos: TargetedCombatTest, TargetCombatServerTest e TargetCombatGuiTest. GameInputRoutingTest ganhou cobertura da nova prioridade de TAB. Task209AbilitiesTest e Task214To220UiTest foram ajustados à seleção explícita. O probe de interface do pacote aceita uma prova opcional de alvo.

## Validação

Suíte Java completa: 706 testes executados, 679 aprovados, 27 abortados por ausência de ambiente gráfico nessa rodada, zero falhas e zero ignorados. Rodada gráfica: 17 classes, 45 execuções aprovadas, zero falhas ou abortos. Compilação: 259 fontes principais e 230 arquivos de teste.

Os totais das rodadas se sobrepõem e não devem ser somados como testes únicos. A rodada GUI é serializada e cobre os abortos por ambiente gráfico da rodada geral.

Os testes cobrem: ataque livre; giro sem deslocamento; vítima selecionada em vez da mais próxima; alvo longe/bloqueado; parede fina; ausência de gasto na rejeição; custo e recarga compartilhada; Shunpo à frente; direção atual após mover/parar; colisão; spawn renascido; rejeição de jogador/NPC como alvo; estado privado de cada seleção; dois clientes vendo posição e direção; serialização e ordinais antigos; TAB no mundo e em painéis/texto; repetição de tecla; limpeza e três resoluções.

A seleção e o ataque também foram conferidos no JAR distribuído, com assets e runtime dos pacotes privados. O smoke TCP usa três processos separados, dois clientes e um servidor, verificando os manifests dos dois papéis.

## Evidências

- .task-checkpoints/TARGET-COMBATE/java-full-final.log
- .task-checkpoints/TARGET-GUI-FINAL/corujao-viii/verification-gui.json
- .task-checkpoints/TARGET-GUI-FINAL/corujao-viii/ui-regression/target-1366.png
- .task-checkpoints/TARGET-GUI-FINAL/corujao-viii/ui-regression/target-1000.png
- .task-checkpoints/TARGET-GUI-FINAL/corujao-viii/ui-regression/target-640.png
- .task-checkpoints/TARGET-COMBATE/package-build/build-evidence.json
- .task-checkpoints/TARGET-COMBATE/package-smoke/result.json
- .task-checkpoints/TARGET-COMBATE/package-ui/target-result.json
- .task-checkpoints/TARGET-COMBATE/integrity-final.json
- .task-checkpoints/TARGET-COMBATE/implementation.patch

## Pacote privado

Anfitrião: dist/SpiritGate-Target-0.223.0-private/Anfitriao/SpiritGate-Atualizavel-0.223.0-private-Windows64.zip

SHA-256: 987bbe44b3db1d2716e371260bc3516b4bc46e6ec8ab633851e792f30f3a6c5c

Convidado: dist/SpiritGate-Target-0.223.0-private/Convidado/SpiritGate-Atualizavel-0.223.0-private-Convidado-Windows64.zip

SHA-256: b0365db5f0ae61cbb92db253cc8b3f64fa6cbc9c87c67f95cb096d5ea35f2a9d

Manifest comum SHA-256: 2a8825b53fbf21ccb49a34e4c4d3ad991f7a393b042728cfc890541584281511.

391 arquivos por papel conferidos com o manifest. O smoke de três processos terminou com códigos zero. A prova de interface confirmou código de produção vindo do JAR e ataque dirigido sem caminhada. ZIPs e seus hashes foram conferidos novamente ao fechar a entrega.

Extraia cada ZIP por completo em uma pasta nova e distinta. Use SpiritGate-Launcher.exe da raiz. Anfitrião hospeda; convidado conecta. Use os dois pacotes da versão 0.223.0-private para testar o alvo. A distribuição anterior foi preservada. Não houve commit, push, publicação ou configuração de feed remoto.

## Integridade e limitações

5.505 arquivos preexistentes em assets, saves e dist permaneceram idênticos ao início. Um launcher.lock em uso foi excluído da leitura do baseline; a instalação aberta não foi encerrada nem modificada. Nenhum arquivo de gameplay mudou durante a validação final. As 2.432 exclusões históricas do workspace foram preservadas. Arquivos sintéticos regenerados pelos testes foram arquivados; evidências antigas de mapas foram restauradas byte a byte após guardar as novas saídas.

O teste de teclado utiliza a fila AWT, foco real e bindings Swing. Não comprova digitação física: o probe anterior mostrou que Robot não entrega eventos nativos neste ambiente. Teclado físico no Windows, execução do iniciador EXE e Radmin entre duas máquinas continuam pendentes de conferência humana.

A seleção é PvE e privada de cada jogador; outros clientes observam a rotação e as ações confirmadas, sem receber a interface de seleção do colega. Habilidades futuras com alvo obrigatório ainda dependem de sua definição. A distância do avanço sem alvo é 70 unidades nesta implementação e está explicitada para revisão de gameplay.

Os relatórios anteriores do Corujão VIII são históricos da versão 0.222.0-private. Este documento registra a evolução posterior e as correções de escopo enviadas pelo responsável durante a implementação.

## Capturas e PDF

- [Alvo em 1366x768](TARGET-1366.png)
- [Alvo em 1000x700](TARGET-1000.png)
- [Alvo em 640x480](TARGET-640.png)
- [Seleção limpa](TARGET-CLEARED.png)
- [Relatório em PDF](Spirit_Gate_Target_Combate_Relatorio.pdf)
