# Spirit Gate — Energy e regeneração

Relatório de implementação e validação local — 01/11/2026. Build privada 0.226.0-private, posterior às evoluções de HUD, alvo e atributos. Este documento complementa os relatórios anteriores do Corujão VIII.

## Resultado e regras aprovadas

Energy (ENE) foi adicionado como quarto atributo distribuível na ficha de Personagem, usando os mesmos pontos livres de Força, Inteligência e Agilidade. Cada ponto custa um ponto livre e fornece +2 de energia máxima e +1% de regeneração sobre a taxa base. O bônus de regeneração para em +100%; a capacidade continua aumentando além de 100 ENE, dentro do limite validado de atributos.

A base é 100 de energia e 15 unidades por segundo, à frequência nominal de 30 ticks/s. Exemplos:

| ENE investido | Energia máxima | Regeneração nominal |
| --- | --- | --- |
| 0 | 100 | 15/s |
| 1 | 102 | 15,15/s |
| 2 | 104 | 15,30/s |
| 50 | 200 | 22,50/s |
| 100 | 300 | 30/s |
| 101 | 302 | 30/s |

Investir aumenta o teto sem preencher a diferença instantaneamente. O jogador recupera essa diferença pela regeneração normal. Os custos das habilidades permanecem em unidades absolutas: os Shunpos continuam custando 25 de energia, independentemente da capacidade. STR, INT e AGI mantêm as regras aprovadas na versão 0.225.

## Implementação autoritativa

O servidor valida o nome do atributo, o saldo e os limites antes de descontar um ponto. A regeneração mantém um resto fracionário, permitindo que +1% faça diferença sem arredondamento prematuro. O total respeita a capacidade máxima; não acumula crédito quando está cheio e não regenera enquanto o personagem está morto. O retorno para casa reabastece até a capacidade atual. A cópia de mundo usada na troca de revisão preserva atributos, capacidade e resto fracionário.

A simulação armazena energia em décimos: 100 unidades equivalem a 1000 internamente. EnergyRules centraliza os coeficientes; os consumidores convertem para unidades visíveis. O construtor anterior de StatsComponent continua disponível e inicia ENE em zero. Gastar pontos nos outros atributos e subir de nível preserva ENE.

## Rede e compatibilidade

O Actor histórico mantém stamina normalizada entre 0 e 1000. Assim, capacidades superiores a 100 não ultrapassam os limites dos snapshots antigos. A ficha anterior e o frame 19 de atributos mantêm seus formatos.

Um novo pedido QUERY_ENERGY e a resposta privada frame 20 transportam atributo treinado, energia atual, capacidade e regeneração, com hash da revisão, tick e sequência. A resposta é enviada apenas ao solicitante. Os códigos anteriores das operações foram preservados pela inclusão ao final da enumeração. Os clientes antigos não recebem esse frame espontaneamente; o novo recurso requer o par de cliente e servidor atualizado.

A apresentação usa o estado e a indicação de sincronização obtidos atomicamente. Após investir em ENE, uma resposta antiga não libera a atualização pendente. Durante a mudança de revisão, a HUD mostra sincronização até receber metadados da revisão ativa, evitando exibir temporariamente a capacidade base de 100. O tick evita combinar uma capacidade nova com um snapshot anterior ao investimento. A hotbar compara energia absoluta com o custo real da ação.

Os saves legados continuam com stamina normalizada. A distribuição manual de atributos permanece de sessão, como na implementação anterior; este trabalho não acrescenta persistência durável de pontos ou migração de saves.

## Interface

A ficha Personagem apresenta Energy (ENE), botão de investimento, explicação por ponto, capacidade e taxa de regeneração. O botão usa o saldo validado pelo servidor e aguarda confirmação. A HUD e a ficha exibem atual/máximo reais, como 104/104 após dois investimentos. O painel ganhou altura e continua limitado à área disponível, com rolagem em janelas menores. Habilidades e equipamentos preservam suas seções e ações existentes.

As capturas automatizadas registram a ficha a 1366, 1000 e 640 pixels de largura, incluindo dois pontos em cada atributo e ENE 2. A imagem pequena mostra o conteúdo rolado até os atributos. São capturas de componentes reais conectados ao servidor, sem redesenho de mockup.

## Validação

Suíte Java final: 724 executados, 696 aprovados, 28 abortos gráficos, zero falhas e zero ignorados; processo Java terminou com código zero. Rodada GUI final: 22 classes, 63 execuções aprovadas, zero abortos e falhas. Compilação Java 17: 262 fontes principais e 235 arquivos de teste. A primeira captura de saída pelo PowerShell classificou avisos do compilador como NativeCommandError; a suíte foi confirmada diretamente pelo processo Java, com log e código de saída próprios.

Casos específicos cobrem: construtor compatível; saldo compartilhado; preservação de ENE nas outras operações; coeficientes e teto; ganho fracionário; custo fixo de Shunpo; ausência de preenchimento gratuito; capacidade cheia; morte e retorno; cópia de revisão e save normalizado. Os testes TCP usam dois clientes, capacidade acima de 100 e resposta privada. Outro teste publica uma revisão real e exige sincronização até a resposta Energy atualizada. Os testes gráficos investem em ENE pela ficha e conferem 104 de capacidade.

A regressão completa cobre também inventário, missões, alvo, combate, offline, Coop local, launcher, fechamento/reconexão e Studio. Os abortos da execução sem ambiente gráfico são condições explícitas dos testes; a rodada gráfica executa essas classes separadamente. As contagens das rodadas se sobrepõem e não representam testes únicos somados.

Uma tentativa intermediária do teste de distribuição terminou sem confirmação de atributos. A espera passou a observar os snapshots recebidos, evitando pings repetidos durante cada espera curta. As rodadas finais registradas abaixo passaram. O ajuste final da sincronização de Energy foi seguido de recompilação, regressão completa e gráfica e reconstrução dos pacotes; o candidato anterior ficou arquivado no checkpoint.

## Pacotes e integridade

Anfitriao: dist/SpiritGate-Energy-0.226.0-private/Anfitriao/SpiritGate-Atualizavel-0.226.0-private-Windows64.zip

SHA-256: b6c1f64daf7bc9456056aec29af9b28765fb2544df8708b7a6388fb3d48ea481

Convidado: dist/SpiritGate-Energy-0.226.0-private/Convidado/SpiritGate-Atualizavel-0.226.0-private-Convidado-Windows64.zip

SHA-256: cb8a81ce36649bf5b94edcccd3904333a4746200eaab52e77a5104e7dc34181c

Manifest comum SHA-256: 47e4638523b7c51d8ac23f299c59d9c6474e8270c5a643176681c6e5a599e7ff. Cada papel teve 391 arquivos conferidos, além de CRC e SHA-256 dos ZIPs.

Os pacotes privados incluem runtime. Os manifests de anfitrião e convidado são idênticos; os ZIPs diferem pelo papel de inicialização. O smoke executa o servidor e dois clientes em três processos com o runtime/JAR distribuído, em loopback. A prova gráfica adicional carrega a produção diretamente do JAR dos pacotes, abre os painéis integrados e consulta o frame privado de Energy.

10283 arquivos preexistentes protegidos permaneceram idênticos; 517 arquivos de fontes/recursos/testes ficaram congelados durante as validações finais. As 2432 exclusões históricas foram preservadas. O launcher.lock de uma instalação em uso foi excluído do inventário inicial, sem encerrar ou alterar essa instalação.

Não houve commit, push ou publicação. Os arquivos de assets, saves e distribuições anteriores foram comparados com o inventário inicial. As exclusões históricas do workspace foram preservadas. Evidências sintéticas recriadas pela suíte foram arquivadas no checkpoint e as evidências anteriores da TASK-211 foram restauradas.

## Arquivos e evidências

- server/src/test/java/com/projetok/multiplayer/AttributeAllocationTest.java
- server/src/test/java/com/projetok/multiplayer/CompactHudCharacterGuiTest.java
- server/src/test/java/com/projetok/multiplayer/EnergyProtocolTest.java
- server/src/test/java/com/projetok/rpg/EnergyRulesTest.java
- server/src/main/java/com/projetok/multiplayer/GameplayProtocol.java
- server/src/main/java/com/projetok/multiplayer/LocalClient.java
- server/src/main/java/com/projetok/multiplayer/LocalSimulationServer.java
- server/src/main/java/com/projetok/multiplayer/NetworkCharacterPanel.java
- server/src/main/java/com/projetok/multiplayer/NetworkCombatHud.java
- server/src/main/java/com/projetok/multiplayer/NetworkHotbar.java
- server/src/main/java/com/projetok/multiplayer/Protocol.java
- server/src/main/java/com/projetok/multiplayer/RpgNetworkClientLauncher.java
- server/src/main/java/com/projetok/multiplayer/RpgSessionWorld.java
- server/src/main/java/com/projetok/multiplayer/SessionSimulation.java
- server/src/main/java/com/projetok/rpg/RpgWorld.java
- server/src/main/java/com/projetok/domain/combat/EnergyRules.java
- server/src/main/java/com/projetok/domain/entity/component/StatsComponent.java
- tools/CorujaoEightUiProbe.java: verificação das fórmulas e frame privado no JAR.
- docs/ENERGY-RELATORIO.md, PDF, capturas ENERGY e notas de continuidade.

Checkpoints: `.task-checkpoints/ENERGY/` contém os hashes iniciais/finais, logs completos, evidências de build, smoke, GUI do pacote e metadados do PDF. `.task-checkpoints/ENERGY-GUI-RELEASE/corujao-viii/` contém o manifesto da rodada gráfica e capturas. Os checkpoints anteriores permanecem como histórico, sem serem contados como rodadas finais.

## Como conferir

1. Extrair o ZIP de anfitrião em uma pasta nova e abrir o launcher existente; usar a build correspondente no convidado.
2. Abrir Personagem. Com pontos livres, investir em ENE e verificar o desconto de um ponto e o aumento de duas unidades no máximo. Personagens de nível 1 começam sem pontos livres.
3. Usar Shunpo e observar o custo de 25 e a recuperação da energia. O investimento não preenche instantaneamente a capacidade adicional.
4. Conferir a separação de Personagem/Habilidades e os outros atributos. Reduzir a janela e rolar a ficha para acessar ENE.

## Limites da validação

Os testes são locais. Não substituem homologação entre computadores por Radmin, avaliação artística humana ou uso prolongado. Os testes de entrada utilizam eventos AWT e bindings Swing reais; teclado físico e o executável nativo não foram comprovados neste ambiente. A taxa por segundo corresponde aos ticks nominais e depende do ritmo efetivo da simulação. A persistência durável dos atributos de sessão segue fora deste incremento.

Documentos anteriores: [Corujão VIII](CORUJAO-VIII-RELATORIO-FINAL.md), [alvo](TARGET-COMBATE.md), [HUD compacta](HUD-COMPACTA-RELATORIO.md) e [atributos de combate](ATRIBUTOS-COMBATE-RELATORIO.md).


## Capturas e PDF

[Personagem](ENERGY-1366.png) · [Janela compacta](ENERGY-640.png) · [HUD](ENERGY-HUD.png) · [PDF](Spirit_Gate_Energy_Relatorio.pdf).
