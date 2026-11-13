# Spirit Gate - HUD compacta, itens e ficha do personagem

Fechamento: 01/11/2026. Branch beco. HEAD 6d72c6595dee6dbe4dea832fe6047ee81f51d4a3.
Versão privada: 0.224.0-private. Evolução posterior ao sistema de alvo 0.223.0-private.

## Resultado

A HUD inferior foi compactada. Os ícones de ação têm 34 pixels e os utilitários, 32 pixels. Foram retirados os fundos e as molduras individuais permanentes e os três grandes containers. Os grupos ficam próximos ao centro da faixa inferior, com espaçamento de quatro pixels entre ícones. Hover é leve, foco de teclado tem contorno e seleção usa um pequeno traço ciano. As recargas aparecem como arco e texto. Reservas continuam sem ação, representadas discretamente por pontos.

HP e energia permanecem alinhados com as ações centrais. A faixa inferior passou de 148 para 104 pixels de altura. O painel de alvo fica oculto quando não há seleção, liberando espaço no mundo. Ataque dirigido, Shunpo com/sem alvo, TAB contextual, cooldowns e demais ações continuam funcionais.

## Inventário com detalhes contextuais

Os slots medem 56x56 e exibem somente ícone e quantidade. Arma, armadura, poção, cristal e relíquia usam símbolos correspondentes do sistema vetorial existente. Nomes e raridades deixaram o grid; continuam disponíveis aos leitores de tela.

Clique em um slot ou selecione-o com as setas e pressione ENTER para abrir o pequeno painel de detalhes junto à célula. Ele mostra nome, descrição, quantidade, raridade e nível mínimo. Descrições longas têm rolagem. Equipar e consumir aparecem como botões com ícones e nomes acessíveis. A ação incompatível com o item fica desabilitada.

O painel contextual pertence ao inventário e não cria uma janela do Windows. Sua posição é limitada à área disponível, acompanha redimensionamento/rolagem e fecha se o item deixa o filtro ou sai do inventário. ESC fecha primeiro os detalhes; uma segunda pressão fecha o inventário. X dos detalhes fecha apenas o painel pequeno, enquanto X do inventário fecha o conjunto.

Armadura, Armas, Drops, Todos, Organizar, Recolher pendentes e Equipamentos ficam no rodapé como ícones compactos, com tooltip e nome acessível. Busca, filtros, organização local e quantidades foram preservados. Organizar continua alterando somente a ordem visual. Equipar/consumir/coletar dependem da confirmação do servidor. Desequipar passou para a ficha do personagem.

## Personagem e Habilidades separados

O bloco esquerdo tem três acessos: Personagem, Inventário e Habilidades. O bloco direito conserva Missões, Menu e Configurações. São seis painéis internos principais, mantido um ativo por vez.

Personagem mostra identidade, nível, XP, ataque, defesa, HP, energia, equipamentos e atributos treinados. A seção de equipamentos possui Arma, Torso, Pernas e Acessório, cada um com ícone, nome/estado vazio, descrição em tooltip e opção de desequipar. Os itens exibidos vêm da ficha privada confirmada.

Habilidades tem uma apresentação própria para ataque racial, Shunpo ofensivo, Shunpo defensivo e poção, com símbolos e descrições. A seleção da ação rápida, sua ativação, escolha de domínio e estados de recarga continuam disponíveis. Os domínios aparecem com seus nomes de apresentação, conservando os IDs internos nos pedidos. Os combos usam cores navy/ciano também no foco e nas opções.

## Atributos: modelo existente reaproveitado

Após a indicação do responsável, foi localizada a camada de domínio: Player.java usa StatsComponent, com strength, intelligence, agility, level e unspentPoints. O responsável confirmou reutilizar Força, Inteligência e Agilidade. VIT e ENERGY não foram criados como atributos distribuíveis; energia continua como recurso de combate.

A distribuição usa StatsComponent.spendOnStrength/Intelligence/Agility e LevelingRules.pointsForLevel. Ao criar a sessão, o jogador tem zero pontos treinados e zero pontos livres no nível 1. Ao alcançar os níveis 2-9 recebe cinco pontos por nível; nos níveis 10-19, três; do nível 20 em diante, um. Exemplo verificado: nível 20 disponibiliza 71 pontos. Consultar a ficha novamente não concede os pontos outra vez.

Os campos apresentados são investimentos manuais da sessão, separados dos valores básicos e da progressão automática já existentes na partida RPG. Não foram transplantados os presets raciais do outro motor para as origens V6. Isso preserva HP, ataque inicial e progressão automática anteriores até que o jogador distribua pontos.

Força acrescenta ao dano básico a fórmula já existente de UnarmedStrike: strength/2, com divisão inteira. Assim, dois pontos de Força acrescentam um de dano. O valor de ATQ da ficha é atualizado pelo servidor. INT e AGI podem receber pontos, mas ainda não alteram as habilidades disponíveis nesta partida. Essa condição aparece nas linhas e tooltips como reserva para futuras técnicas; não foram inventadas magias, velocidade ou críticos para justificar esses atributos.

O botão + envia um pedido para gastar um ponto. Enquanto aguarda a confirmação, os botões ficam temporariamente desabilitados. Sem pontos, morto, desconectado ou sincronizando, não é possível distribuir pela interface. O servidor valida nome do atributo, saldo e estado do jogador, desconta um ponto por operação e rejeita gastos inválidos. Nenhum valor de atributo é aceito como número calculado pelo cliente.

Os atributos são privados de cada jogador e seguem a duração efêmera da sessão multiplayer. A troca de revisão copia esse estado em memória. Não há gravação de atributos em saves pessoais, alteração de schemas de save ou promessa de recuperação após desconectar/reiniciar a sessão.

## Transporte e compatibilidade

QUERY_ATTRIBUTES e ALLOCATE_ATTRIBUTE foram acrescentadas ao fim das operações de gameplay. A resposta Attributes usa um frame separado, tipo 19, com hash da revisão e campos limitados. O formato da ficha V6 anterior não foi alterado. A resposta só é enviada ao cliente que pede sua própria ficha; clientes antigos que não fazem a consulta continuam recebendo as mensagens anteriores.

O cliente consulta os atributos ao abrir Personagem, após distribuir um ponto e periodicamente durante a partida. A resposta com revisão incompatível é descartada. Alocação, consulta e ações de combate compartilham a sequência e os limites do servidor. Fila de controle cheia fecha apenas aquele peer, preservando o servidor. Não há acesso a banco ou serviço externo novo.

Use cliente e servidor 0.224.0-private para o recurso novo: builds anteriores não conhecem as operações de atributos. Offline, Coop, launcher, atualizador e Studio mantêm os caminhos anteriores. Nenhuma dependência foi instalada. Sem commit, push, release ou publicação oficial.

## Arquivos principais

- NetworkInventoryPanel.java: slots sem texto, ícones por categoria, painel contextual e ações compactas.
- NetworkCharacterPanel.java (novo): ficha, equipamento e distribuição de atributos.
- NetworkHotbar.java: ícones pequenos, reservas discretas e conteúdo separado de Habilidades.
- NetworkGameHud.java: grupos próximos, novo acesso de Habilidades e altura reduzida.
- GameHudStyle.java: ícones vetoriais adicionais, botões sem fundos permanentes e combos consistentes.
- GamePanels.java: prioridade de ESC para fechar o painel contextual antes da janela principal.
- RpgNetworkClientLauncher.java: seis painéis, atualizações e consulta privada de atributos.
- RpgWorld.java: investimentos manuais, pontos por nível, bônus de Força e cópia de revisão.
- GameplayProtocol.java, Protocol.java, SessionSimulation.java, RpgSessionWorld.java, LocalSimulationServer.java e LocalClient.java: operações e resposta privada com validação autoritativa.

Esses arquivos ficam em server/src/main/java/com/projetok/multiplayer/, exceto RpgWorld.java, em rpg/. StatsComponent, LevelingRules e UnarmedStrike foram reutilizados sem alterar suas regras.

Novos testes: AttributeAllocationTest e CompactHudCharacterGuiTest. Task213HudSwingTest foi atualizado para verificar a nova composição e seus limites, mantendo as verificações de dois clientes, combate, minimapa, janelas e descarte. O probe empacotado ganhou uma opção para verificar atributos, painéis separados e detalhes de item no JAR distribuído.

## Testes e evidências

Suíte Java completa: 710 testes executados, 682 aprovados, 28 abortados por ausência de ambiente gráfico nessa rodada, zero falhas e zero ignorados. Rodada gráfica serializada: 19 classes, 49 execuções aprovadas, zero falhas ou abortos. Compilação: 260 fontes principais e 232 arquivos de teste.

Os totais das rodadas se sobrepõem; não devem ser somados como testes únicos. A rodada gráfica cobre os abortos por ausência de GUI da rodada geral.

A integração gráfica verificou dois jogadores reais: pontos do nível 20, dois cliques de Força com confirmação, ATQ aumentando em um e saldo passando de 71 para 69, sem mudar os atributos do colega. Também desequipou pela ficha, abriu o item por ENTER e mouse, conferiu ESC e equipou pelo painel contextual, aguardando a confirmação do servidor.

Personagem, Habilidades e detalhes de item foram capturados em 1366x768, 1000x700 e 640x480. O popup permaneceu dentro do inventário. A nova HUD foi verificada quanto a tamanho, alinhamento, separação dos grupos, ações existentes, quatro reservas sem função e ausência de janelas nativas secundárias.

Os testes de domínio verificaram: saldo inicial, curva de pontos, ausência de ganho duplicado por consulta, atributo inválido, esgotamento sem saldo negativo e bônus de Força. Os testes TCP comprovaram privacidade e atualização de ATQ. A serialização do frame novo e da ficha antiga foi validada.

Uma rodada intermediária revelou que o ESC global fechava o inventário antes dos detalhes; foi corrigida a prioridade de fechamento. Uma expectativa histórica de layout ainda exigia o deslocamento e as reservas antigas; foi atualizada para os novos critérios compactos. Essas tentativas não contam como aprovação. A rodada final usa as fontes congeladas após as correções.

Evidências principais:

- .task-checkpoints/HUD-COMPACTA/java-full-final.log
- .task-checkpoints/HUD-COMPACTA-GUI-FINAL/corujao-viii/verification-gui.json
- .task-checkpoints/HUD-COMPACTA-GUI-FINAL/corujao-viii/ui-regression/
- .task-checkpoints/HUD-COMPACTA/integrity-final.json e implementation.patch
- .task-checkpoints/HUD-COMPACTA/package-build/build-evidence.json
- .task-checkpoints/HUD-COMPACTA/package-smoke/result.json
- .task-checkpoints/HUD-COMPACTA/package-ui/compact-result.json

## Pacotes privados

Anfitrião: dist/SpiritGate-HUD-0.224.0-private/Anfitriao/SpiritGate-Atualizavel-0.224.0-private-Windows64.zip

SHA-256: 3d81fe2290d8af96a1cc75a3ff6b75d14ba5f79acd92aa32a9635052f46517c1

Convidado: dist/SpiritGate-HUD-0.224.0-private/Convidado/SpiritGate-Atualizavel-0.224.0-private-Convidado-Windows64.zip

SHA-256: aa5222b2b8a65a316a8c7d7b029094aa38445c3a537af1d22080c3155890e9ca

Manifest comum SHA-256: 9eaf090860c5e3e4a7c95e692c2c1f1fa9253e2fd7a015f77790c76372aeb6b3.

Os 391 arquivos por papel passaram pela verificação do manifest; os três processos do smoke terminaram com código zero. ZIPs e hashes foram verificados novamente ao fechar a entrega.

Extraia integralmente cada ZIP em uma pasta nova e distinta. Use SpiritGate-Launcher.exe da raiz; Anfitrião hospeda e Convidado conecta. Os dois papéis devem usar 0.224.0-private. Os dados pessoais não estão incluídos. As builds anteriores ficam preservadas.

O smoke verifica o manifest e executa servidor e dois clientes em três processos usando seus runtimes/JARs. A prova gráfica abre duas janelas a partir do JAR distribuído, consulta atributos privados e captura Personagem, Habilidades e detalhes de item. Não equivale à execução do iniciador nativo EXE nem à homologação entre computadores.

## Integridade e limitações

7.097 arquivos preexistentes protegidos em assets, saves e dist permaneceram idênticos ao início. Um launcher.lock aberto foi excluído da leitura do baseline; a instalação em uso não foi encerrada ou alterada. As fontes permaneceram congeladas durante a validação final. As 2.432 exclusões históricas do workspace foram preservadas. Saídas sintéticas regeneradas pelos testes foram arquivadas; evidências históricas de mapas foram restauradas após preservar as novas saídas.

A verificação de teclado usa eventos na fila AWT, foco e bindings Swing reais. Digitação física no Windows, execução do iniciador EXE, revisão artística humana e Radmin entre dois computadores permanecem pendentes de conferência manual. As capturas não constituem aceite artístico humano.

INT e AGI não têm efeito nas habilidades atuais. Os pontos são investimentos de sessão, sem persistência pessoal ou reset novo. Essas limitações estão indicadas na interface e neste relatório.

## Capturas e PDF

- [HUD compacta](HUD-COMPACTA-1366.png)
- [HUD em 640x480](HUD-COMPACTA-640.png)
- [Detalhes do item](HUD-COMPACTA-ITEM-640.png)
- [Personagem, equipamento e pontos](HUD-COMPACTA-PERSONAGEM-1366.png)
- [Personagem compacto](HUD-COMPACTA-PERSONAGEM-640.png)
- [Habilidades separadas](HUD-COMPACTA-SKILLS-1000.png)
- [Relatório em PDF](Spirit_Gate_HUD_Compacta_Relatorio.pdf)


## Regras de atributos — 01/11/2026

Evolução 0.225.0-private: STR aumenta dano básico/desarmado, INT rege o bônus de domínio e AGI aumenta a cadência básica e a chance de esquiva de hit básico. Coeficientes e tetos foram aprovados pelo responsável. Shunpo continua sem dano; executores das próximas técnicas de domínio usarão a fórmula de INT. Validação: 689 testes gerais e 56 execuções GUI aprovados, sem falhas. [Regras e pacotes](ATRIBUTOS-COMBATE-RELATORIO.md) · [PDF](Spirit_Gate_Atributos_Combate_Relatorio.pdf). As indicações anteriores de INT/AGI reservadas são históricas da versão 0.224.0-private.

