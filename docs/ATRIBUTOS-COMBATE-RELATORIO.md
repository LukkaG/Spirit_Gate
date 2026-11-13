# Spirit Gate - atributos de combate

Fechamento: 01/11/2026. Branch beco, HEAD 6d72c6595dee6dbe4dea832fe6047ee81f51d4a3.
Versão privada: 0.225.0-private. Continuação autorizada pelo responsável.

## Regras aprovadas e implementadas

| Atributo | Efeito | Limite |
|---|---|---|
| Força (STR) | +1 de dano básico e de técnica desarmada a cada 2 pontos | Divisão inteira |
| Inteligência (INT) | +1 de dano de domínio a cada 2 pontos | Divisão inteira |
| Agilidade (AGI) | +1% de velocidade do ataque básico por ponto | Máximo +100% |
| Agilidade (AGI) | +0,5 ponto percentual de esquiva contra hit básico por ponto | Máximo 25% |

O responsável confirmou esses coeficientes como valores iniciais. Ganho de pontos por nível, saldo e distribuição continuam usando StatsComponent e LevelingRules. Os investimentos são privados e de sessão, como na build anterior.

STR não aumenta o dano de domínio. INT não aumenta ataque básico, socos, cura ou deslocamento. AGI não acrescenta dano às técnicas. O bônus antigo de AGI em WaterStyle foi substituído pelo bônus de INT. Nenhuma habilidade nova foi criada.

## Dano básico, desarmado e de domínio

AttributeCombatRules centraliza quatro naturezas de dano: BASIC, UNARMED_SKILL, DOMAIN_SKILL e OTHER. Básico e desarmado usam STR/2; domínio usa INT/2; outras naturezas não recebem esses bônus. A natureza é definida pelo código que executa a ação no servidor, não por um campo de dano enviado pelo cliente.

O ataque básico racial, com ou sem arma, e o soco preservam seu dano base/progressão/equipamento e recebem o bônus de Força já existente. UnarmedStrike agora usa a regra central para as técnicas desarmadas. WaterStyle, a implementação de estilo de domínio existente na camada de combate, calcula seu dano base mais INT/2, sem contribuição de STR ou AGI.

RpgWorld também expõe o cálculo tipado para os executores de técnicas. Os domínios de Spirit, Reaper e Aetheri conservam seus catálogos e restrições de escolha. As técnicas de dano desses domínios ainda não possuem executores na partida RPG V6; a fórmula está pronta para ser usada por elas. A escolha de domínio não foi transformada em desbloqueio de habilidades inexistentes.

Shunpo ofensivo e defensivo continuam sendo deslocamento/esquiva, sem dano. Portanto, não recebem aumento de dano por STR ou INT. Alcance, alvo opcional do ofensivo, avanço sem alvo, energia e recarga compartilhada foram preservados.

## Cadência por Agilidade

O intervalo básico é calculado no servidor:

    multiplicador = 1 + min(AGI, 100) / 100
    intervalo = 12 ticks / multiplicador

O servidor continua operando em 30 ticks por segundo. Sem AGI, a recarga básica permanece em 12 ticks, ou 0,4 segundo. Com 50 AGI, passa para 8 ticks. Com 100 AGI, chega a 6 ticks, ou 0,2 segundo. Pontos além de 100 não aumentam mais a cadência.

O prazo mantém a fração de tick entre ataques consecutivos. Isso permite que até 1 AGI produza o aumento médio de 1%, em vez de perder o bônus por arredondamento em cada golpe. Após ociosidade, não há acúmulo de crédito para disparar ataques em rajada. A recarga corrente não é apagada ao distribuir pontos: o intervalo novo é usado nos ataques seguintes.

O servidor recusa novas intenções enquanto o prazo não terminou. Não foram adicionados autoataque, repetição automática de teclado ou mudança de cooldown de Shunpo/poção. As rotinas existentes de input e sequência de comandos foram preservadas.

A hotbar usa os ticks confirmados e a nova duração de referência para o arco de recarga. Os eventos HIT confirmados passam a marcar o início visual de cada golpe, inclusive quando a cadência fica menor que os 12 ticks anteriores. Eventos repetidos continuam protegidos pela sequência.

## Esquiva de hit básico

A chance é calculada no servidor:

    chance = min(25%, AGI x 0,5%)

Um sorteio menor que essa chance evita o dano do hit básico inteiro. O limite é atingido com 50 AGI. Com AGI zero, não há sorteio. Dano zero e personagem morto também não consultam o sorteio.

EnemyService identifica a origem do golpe. O contato básico do inimigo é marcado como básico; o ataque preparado de área do chefe é marcado como habilidade/área. SharedRpgEncounter encaminha o primeiro para receiveBasicDamage e o segundo para receiveDamage. Assim, AGI não evita dano de habilidade, área ou ambiente.

A janela de evasão do Shunpo continua obedecendo à regra anterior e é verificada separadamente. Um golpe já ignorado por essa janela não precisa consumir outro sorteio de AGI. Defesa, dano mínimo, frequência dos inimigos, alcance e geometria não foram recalibrados nesta alteração.

O gerador pertence ao mundo do servidor. Testes injetam uma fonte determinística no pacote para conferir fronteiras de probabilidade e roteamento, sem depender de sorte. O cliente não envia resultado de esquiva, porcentagem ou dano calculado. Nenhum campo ou tipo de mensagem de rede novo foi necessário.

## Ficha e distribuição

A ficha do personagem mostra as novas descrições e os efeitos atuais: bônus básico/desarmado, bônus de domínio, velocidade básica e chance de esquiva básica. Os limites de velocidade/esquiva aparecem no tooltip do resumo. A distribuição segue confirmada pelo servidor e não permite saldo negativo ou cliques adicionais enquanto a interface aguarda resposta.

Exemplo capturado: STR 2, INT 2 e AGI 2 resultam em +1 de dano básico/desarmado, +1 de bônus de domínio, +2% de velocidade básica e 1,0% de esquiva básica. Os atributos do segundo jogador não mudam quando o primeiro distribui pontos.

Este relatório substitui a indicação histórica de INT/AGI reservadas da build 0.224.0-private quanto às fórmulas de combate. A implantação dos executores de técnicas de domínio permanece um trabalho distinto; não se declara uma técnica inexistente como jogável.

## Arquivos alterados

- domain/combat/AttributeCombatRules.java (novo): coeficientes, natureza do dano, limites e cadência fracionária.
- domain/combat/style/UnarmedStrike.java: regra central de dano desarmado por STR.
- domain/combat/style/WaterStyle.java: dano de domínio por INT.
- rpg/RpgWorld.java: intervalo autoritativo, prazo fracionário, recepção de hit básico e sorteio de esquiva.
- rpg/EnemyService.java: distinção entre contato básico e ataque de área, mantendo a entrada anterior compatível.
- rpg/SharedRpgEncounter.java: aplicação da natureza de dano ao defensor correto.
- multiplayer/NetworkHotbar.java: referência visual da recarga básica.
- multiplayer/NetworkCombatPresentation.java: início visual por hit confirmado.
- multiplayer/NetworkCharacterPanel.java: descrições e resumo dos efeitos.

Os caminhos acima são relativos a server/src/main/java/com/projetok/. O fork de revisão copia o prazo fracionário e a fonte de combate junto dos atributos em memória. Saves pessoais, assets e distribuição anterior não foram migrados ou modificados. Nenhuma dependência foi instalada.

## Validação

Suíte Java completa: 717 testes executados, 689 aprovados, 28 abortados por ausência de ambiente gráfico nessa rodada, zero falhas e zero ignorados. Rodada gráfica serializada: 20 classes, 56 execuções aprovadas, zero falhas e zero abortos. Compilação: 261 fontes principais e 233 arquivos de teste.

Os totais das duas rodadas se sobrepõem; não somar como testes únicos. A rodada GUI cobre os abortos gráficos da rodada geral.

AttributeCombatRulesTest cobre seis casos: separação de STR/INT/AGI por natureza; WaterStyle por INT; bônus pequenos e limites; ausência de crédito ocioso; recarga básica real em seis ticks; esquiva determinística só para hits básicos; fronteira exata do sorteio; origem básica versus área do chefe; e aplicação da esquiva no encontro compartilhado real.

AttributeAllocationTest ganhou um caso TCP: o jogador distribui 50 AGI e o servidor confirma recarga básica de até oito ticks, sem aumento do dano básico e sem modificar o colega. O segundo cliente observa o HIT confirmado. Os testes anteriores de saldo, privacidade, serialização e distribuição permanecem.

CompactHudCharacterGuiTest agora distribui STR, INT e AGI e confere o resumo da ficha nos clientes reais. Os painéis foram capturados em 1366x768, 1000x700 e 640x480. Ataque dirigido, teclado, inventário, habilidades, configurações, minimapa, fechamento, reconexão, offline e Studio foram cobertos pelas regressões existentes.

Uma tentativa intermediária usou uma região de chefe inexistente e tentou atacar novamente um inimigo já derrotado em uma fixture de nível alto. As fixtures foram corrigidas para a região real ruins e para verificar o término da recarga sem depender de uma vítima viva. Essas tentativas não foram contadas como aprovação. Nenhuma regra de runtime foi afrouxada para acomodar testes.

Evidências:

- .task-checkpoints/ATRIBUTOS-COMBATE/java-full-final.log
- .task-checkpoints/ATRIBUTOS-GUI-FINAL/corujao-viii/verification-gui.json
- .task-checkpoints/ATRIBUTOS-GUI-FINAL/corujao-viii/ui-regression/
- .task-checkpoints/ATRIBUTOS-COMBATE/integrity-final.json e implementation.patch
- .task-checkpoints/ATRIBUTOS-COMBATE/package-build/build-evidence.json
- .task-checkpoints/ATRIBUTOS-COMBATE/package-smoke/result.json
- .task-checkpoints/ATRIBUTOS-COMBATE/package-ui/attribute-rules-result.json

## Pacotes e integridade

Anfitrião: dist/SpiritGate-Atributos-0.225.0-private/Anfitriao/SpiritGate-Atualizavel-0.225.0-private-Windows64.zip

SHA-256: 2335d0ed06490f7ae2680bb912130e4bc2ececd0ff617669521cb03a0f4496ad

Convidado: dist/SpiritGate-Atributos-0.225.0-private/Convidado/SpiritGate-Atualizavel-0.225.0-private-Convidado-Windows64.zip

SHA-256: c9b5778d8e8a26d8bb384f3f91c547d3d5f847755116a78eaeb68ba2394f0e52

Manifest comum SHA-256: c055e8922259cefb270ba561e633b5fa0e5dabc4b8fabad0f6b6e604dcad747c.

Os 391 arquivos de cada papel foram conferidos contra o manifest. O smoke iniciou servidor e dois clientes em três processos, com os runtimes/JARs dos pacotes, terminando com três códigos zero. A prova gráfica abriu duas janelas a partir do JAR distribuído e conferiu as fórmulas de INT, teto de velocidade e teto de esquiva presentes no artefato final. ZIPs, CRC e SHA-256 foram verificados novamente.

Extraia os ZIPs integralmente em pastas novas e distintas. Abra SpiritGate-Launcher.exe da raiz; Anfitrião hospeda e Convidado conecta. Use a mesma versão 0.225.0-private nos dois papéis para que regras e descrições coincidam. Sem publicação automática ou feed remoto novo.

8.690 arquivos preexistentes protegidos em assets, saves e dist permaneceram idênticos ao início. Um launcher.lock aberto foi excluído da leitura do baseline; a instalação em uso não foi encerrada ou modificada. As fontes permaneceram congeladas durante a validação final. As 2.432 exclusões históricas do workspace foram preservadas. Saídas sintéticas regeneradas pelos testes foram arquivadas, e evidências históricas de mapa foram restauradas após guardar as novas saídas. Não houve commit, push, release ou publicação.

## Limitações e continuidade

A validação foi local. Teclado físico, execução do iniciador nativo EXE, revisão artística humana e Radmin entre dois computadores continuam pendentes de conferência manual. Os testes de teclado usam a fila AWT e os bindings Swing, não comprovando digitação física.

Os investimentos continuam efêmeros por sessão, sem persistência pessoal ou respec novo. A redução de cadência usa o relógio de simulação; latência e frequência de acionamento do jogador também influenciam os golpes efetivamente observados. Nenhuma técnica de domínio nova foi implementada nesta entrega.

## Capturas e PDF

- [Ficha com os bônus de combate](ATRIBUTOS-COMBATE-1366.png)
- [Ficha em janela compacta](ATRIBUTOS-COMBATE-640.png)
- [Painel de habilidades](ATRIBUTOS-COMBATE-SKILLS.png)
- [Relatório em PDF](Spirit_Gate_Atributos_Combate_Relatorio.pdf)
