# Spirit Gate - relatório do adendo de janelas e teclado

Data: 31/10/2026. Branch: beco. Base: 6d72c6595dee6dbe4dea832fe6047ee81f51d4a3.

## Resultado e escopo

Adendo implementado na interface da partida multiplayer V6, sobre a TASK-213 e seu refinamento visual. Inventário, perfil, missões, menu e configurações agora abrem como painéis internos do jogo, sem decoração de janela do Windows. Usam navy translúcido, bordas ciano, fechamento próprio, foco visível e arraste limitado à área disponível. A janela principal do jogo mantém sua moldura externa normal.

A composição inferior em três blocos, as barras alinhadas e os quatro slots funcionais com reservas visuais foram preservados. Os painéis reaproveitam os dados e ações existentes. O inventário ganhou busca local por nome; isso não altera o inventário autoritativo.

Esta entrega atende ao adendo e fornece a base das TASKs 214-217. Não declara concluídos o inventário completo da 214, a evolução de missões da 215, todo o escopo da 216 ou a consolidação geral da 217. As TASKs 218-222 também permanecem pendentes. O prompt original do ciclo determina iniciar somente a TASK-213; o novo adendo autorizou estas correções imediatas de janelas, teclado e apresentação de configurações.

## Comportamento do teclado

O roteador central recebe os eventos apenas da janela da partida. As ações existentes continuam na ActionMap; as associações globais antigas são retiradas enquanto ele está instalado e restauradas no descarte, impedindo que uma tecla bloqueada pelo contexto reapareça como comando de gameplay.

- TAB e SHIFT+TAB mantêm a navegação de foco. SHIFT isolado executa sua ação ao soltar, sem confundir SHIFT+TAB com habilidade.
- SPACE executa ataque durante gameplay, mesmo com foco em inventário, configurações ou outro botão da HUD. Não aciona acidentalmente o botão destacado.
- ENTER ativa o botão focado. O atalho de inventário funciona mesmo com foco em outro botão.
- Campos de texto, controles de edição, captura de atalhos e configurações modais recebem sua entrada sem movimento ou combate. Uma checkbox nas configurações mantém o comportamento normal de SPACE.
- Repetição de KEY_PRESSED não duplica a ação. Mudança de contexto, desconexão e recuperação limpam o estado; teclas já pressionadas aguardam soltura antes de serem aceitas novamente.
- Outras janelas não são interceptadas. Ao fechar a partida, dispatcher, listener de foco e bindings são removidos/restaurados.

## Configurações integradas

Áudio contém o volume geral e o mute reais já suportados. Acessibilidade oferece redução de animações/iluminação decorativas. A resolução ajusta a janela. Os controles exibem os atalhos atuais e permitem captura com validação de conflitos e teclas reservadas.

Cancelar, X e ESC descartam o rascunho. Aplicar grava volume, mute e preferência de redução usando o RpgPreferences existente, mantendo seu formato e caminho. Os atalhos personalizados são aplicados somente à sessão atual e atualizam os rótulos da HUD; não são gravados em saves. Resolução também mantém comportamento de sessão. Não foram criados controles separados fictícios para música e efeitos.

O carregamento e a gravação não bloqueiam a interface. O fechamento durante carregamento invalida resultados atrasados; durante confirmação da gravação, os controles e o fechamento ficam bloqueados até seu resultado. Erros de reconexão do V6 aparecem no painel de menu existente.

## Arquivos de produção

Todos os caminhos abaixo são relativos a server/src/main/java/com/projetok/.

- multiplayer/GameInputRouter.java: novo roteador contextual, captura, validação/remapeamento de sessão e descarte.
- multiplayer/GamePanels.java: novo gerenciador de painéis, modalidade, foco, fechamento, arraste e limites.
- multiplayer/NetworkSettingsPanel.java: nova apresentação integrada de configurações e rascunho Aplicar/Cancelar.
- multiplayer/RpgNetworkClientLauncher.java: integração dos cinco painéis, disponibilidade do input, reconexão e ciclo de vida.
- multiplayer/GameHudStyle.java: componentes locais navy/ciano, transparência, foco, sliders e controles.
- multiplayer/NetworkGameHud.java: camada dos painéis, layout e rótulos dos utilitários.
- multiplayer/NetworkHotbar.java: rótulos dos atalhos e disposição do perfil.
- multiplayer/NetworkInventoryPanel.java: busca local e ações ajustadas ao espaço disponível.
- multiplayer/NetworkQuestPanel.java: disposição e rótulos de ações existentes.
- prototype/rpg/PreferencesDialog.java: acesso ao caminho de preferências já existente.
- prototype/rpg/SpiritTheme.java: exposição das cores existentes e da opção de movimento reduzido.

Não houve alterações no protocolo, dano, cooldowns, habilidades do servidor, colisão, progressão, regras de missões ou inventário autoritativo. Os fluxos legados de offline/Coop e World Studio foram preservados. Nenhuma dependência adicionada.

## Testes e evidências

Compilação: 258 fontes principais e 226 arquivos de teste.

Regressão Java completa: 695 executados, 670 aprovados, 25 abortados por ausência de ambiente gráfico nessa rodada, zero falhas e zero ignorados. Esses testes gráficos foram cobertos na execução com GUI: 12 classes, 33 execuções aprovadas, zero abortos e zero falhas. Os totais se sobrepõem e não devem ser somados como testes únicos.

Novos testes: GameInputRoutingTest (dois casos), GamePanelsIntegrationTest (um caso) e utilitário SwingKeys. Task212PlayableUiTest e Task213HudSwingTest foram atualizados para verificar os painéis internos, incluindo ausência de janelas nativas secundárias. A rodada GUI também cobriu Task191LifecycleTest, RadminLauncherUiTest, SessionGameReconnectCloseTest, Task201RadminCancellationTest, Task201SessionWindowFlowTest e as três classes de regressão do Studio.

O teste integrado iniciou servidor e dois clientes V6 reais. Com foco nos botões, os ataques produziram hits confirmados pelo servidor e observados pelos dois clientes. Houve verificação de movimento observado pelo par, digitação sem comandos, atalhos, captura, Aplicar/Cancelar, fechamento, reabertura, arraste nos limites e remoção de listeners. Os cinco painéis foram verificados em 1366x768, 1000x700 e 640x480. Configurações compactas usam rolagem com ações de rodapé acessíveis. A pintura RGBA foi verificada com alpha entre zero e 255, além de capturas em cenários claro e escuro.

Comandos reproduzíveis:

    python tools/run-local-tests.py
    python tools/corujao7_verify.py ADENDO-JANELAS-GUI com.projetok.multiplayer.GameInputRoutingTest com.projetok.multiplayer.GamePanelsIntegrationTest com.projetok.multiplayer.Task213HudSwingTest com.projetok.multiplayer.Task212PlayableUiTest com.projetok.multiplayer.Task191LifecycleTest com.projetok.prototype.rpg.RadminLauncherUiTest com.projetok.prototype.rpg.SessionGameReconnectCloseTest com.projetok.prototype.rpg.Task201RadminCancellationTest com.projetok.prototype.rpg.Task201SessionWindowFlowTest com.projetok.studio.QuestBuilderTest com.projetok.studio.StudioGestureSafetyTest com.projetok.studio.StudioUsabilityTest --gui --cycle corujao-viii

Logs e resultados estruturados:

- .task-checkpoints/ADENDO-JANELAS-HOTKEYS/corujao-viii/java-full-final.log
- .task-checkpoints/ADENDO-JANELAS-GUI/corujao-viii/verification-gui.json
- .task-checkpoints/ADENDO-JANELAS-HOTKEYS/corujao-viii/integrity-final.json
- .task-checkpoints/ADENDO-JANELAS-HOTKEYS/corujao-viii/implementation.patch

## Limitações e verificações pendentes

java.awt.Robot não entregou eventos nativos de teclado neste ambiente. Uma prova isolada registrou ausência desses eventos e sucesso dos eventos enviados à fila AWT. Os testes aprovados passam por EventQueue, KeyEventDispatcher, foco real e bindings Swing; não chamam diretamente as ações para simular o resultado. Isso comprova o roteamento de eventos da aplicação, mas não constitui homologação de digitação física. A evidência está em keyboard-probe.log no checkpoint do adendo. As primeiras tentativas com Robot falharam e não foram contadas como aprovação.

Ainda requerem conferência humana: teclado físico no Windows, preferência visual final, legibilidade em diferentes monitores e sessão Radmin entre duas máquinas. Multiplayer com dois clientes foi validado localmente; não é prova de conectividade entre computadores distintos. A modalidade descrita se refere aos painéis internos; o fluxo preexistente de confirmação de encerramento continua usando seu mecanismo próprio.

Uma tentativa intermediária do teste de iluminação usou valor abaixo do mínimo aceito pelo motor. A fixture foi corrigida para 0,5, sem mudar limites do runtime, e a rodada final passou.

## Integridade e entrega

3.913 arquivos protegidos em assets, saves e dist permaneceram idênticos ao início. As fontes permaneceram estáveis durante as duas rodadas finais. As 2.432 exclusões históricas do workspace foram preservadas. Arquivos sintéticos gerados pelos testes foram arquivados no novo checkpoint; duas evidências antigas de mapa com timestamps regenerados foram restauradas byte a byte após arquivar as novas saídas.

Sem commit, push, release ou publicação. Os pacotes de distribuição anteriores foram preservados; esta entrega está no código-fonte e nas evidências, sem novo executável empacotado.

## Capturas reais

- ADENDO-settings-1366.png: configurações integradas em resolução ampla.
- ADENDO-settings-640.png: configurações compactas, rolagem e rodapé.
- ADENDO-inventory-1366.png: inventário integrado.
- ADENDO-inventory-text-focus.png: foco na busca de texto.
- ADENDO-settings-rebind-focus.png: captura contextual de atalho.
- ADENDO-journal-1000.png: missões integradas.
- ADENDO-inventory-bright.png e ADENDO-inventory-dark.png: composição em duas iluminações.

As capturas completas dos cinco painéis nas três resoluções ficam em .task-checkpoints/ADENDO-JANELAS-GUI/corujao-viii/ui-regression/. As imagens anteriores da TASK-213 foram preservadas.
