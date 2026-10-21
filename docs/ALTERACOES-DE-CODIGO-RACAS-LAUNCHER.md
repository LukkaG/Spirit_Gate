# Spirit Gate — alterações de código

Relatório técnico da entrega de raças, domínios e launcher. Emitido em 21/10/2026.

## 1. Escopo e referência

Este relatório descreve o commit 07accf07e0a301207bf4ff253a222b65ac4095b9 ("adicao das racas") em comparação com seu pai f9b5de1eedb4c6f745d4767e0e75eb3413600149. O diff é extraído diretamente do Git, não reconstruído a partir da conversa.

Foram identificados 91 arquivos alterados: 81 arquivos de código, configuração, scripts e testes, mais 10 documentos e evidências. O inventário inclui arquivos adicionados, modificados e removidos. O anexo do PDF contém o diff integral dos 81 arquivos técnicos, com as linhas adicionadas e removidas e o contexto original.

O PDF de raças e domínios é o documento de design. Este é o documento de implementação. O patch separado preserva os mesmos bytes do diff para consulta por ferramentas. Linhas longas do anexo são quebradas apenas para caber na página.

## 2. Modelo de personagem e persistência

CharacterRaces centraliza Reaper, Human, Aetheri e Spirit e seus fundamentos iniciais. CharacterIdentity passa a validar raças atuais; a criação rejeita Guardian/Mystic como novas opções, enquanto o construtor permite sua normalização ao ler identidades antigas. O acesso race() foi acrescentado e o componente archetype permanece por compatibilidade do snapshot.

Domains cadastra dez caminhos com raça, descrição e técnicas para prévia: quatro de Reaper, dois humanos, dois de arco Aetheri e dois de trevas Spirit. DomainProgression mantém um único domínio, a escolha a partir do nível 15, XP em meias unidades, data do reset e conclusão da quest. Não permite alternância normal entre domínios.

O save V2 grava race explicitamente. LocalSaveStore aceita V1, migra Guardian para Reaper e Mystic para Human e registra a migração, preservando identidade, atributos-base, XP, itens, equipamento e quests. RpgSave valida compatibilidade racial e nível mínimo do domínio. CharacterSaves apresenta os nomes das raças no catálogo.

Antes da escolha, não se acumula XP de domínio. Depois dela, a implementação recebe metade da recompensa geral no domínio. A proporção foi confirmada para o mob de nível 1; seu uso em quests continua provisório. Existe armazenamento de XP de domínio, mas ainda não existe cálculo do nível de domínio nem requisitos finais por técnica.

## 3. Combate, progressão e reset

RpgWorld inicia sem domínio: Reaper recebe small_sword de poder adicional zero, Human/Aetheri começam com socos e Spirit com feixe luminoso básico. Qualquer raça pode optar por socos sem ganhar especialização marcial. Feixe básico de Spirit não concede domínio de luz.

O ataque básico de Aetheri passa a usar alcance de 280 pixels ao escolher bow_long e 140 em bow_short. Os demais ataques iniciais usam 82 pixels para Reaper, 60 para socos e 180 para Spirit. Esses valores são provisórios. As árvores e habilidades elementais finais não estão implementadas pelas prévias.

Shunpo ofensivo e defensivo exigem nível 20 em todas as raças e compartilham recarga. O ofensivo valida alvo visível, alcance e destino; o defensivo recua e cria uma janela de esquiva. O estado de recarga, vigor, direção e esquiva é preservado ao salvar. RpgSecondaryAbility foi adaptado aos metadados do Shunpo defensivo; os antigos ataques de arquétipo foram retirados do fluxo atual.

Parâmetros provisórios do Shunpo: custo 25 de vigor, recarga de 90 ticks, alcance ofensivo 240 pixels, recuo defensivo 70 pixels e janela de 6 ticks, com simulação de 30 Hz. Apenas o requisito de nível 20 está fechado pelo design.

EnemyService adiciona nível explícito, com compatibilidade para definições antigas. Os mobs atuais são tratados como nível 1 e recompensam 3 XP geral e 1,5 de domínio após a escolha. Níveis de mob acima de 1 são recusados enquanto suas recompensas não forem definidas. A curva geral continua provisória; cap=0 retira o teto de design, mantendo 1.000.000 XP como proteção técnica do save.

DomainResetPolicy define 35 Spirit Coins, equivalentes a R$ 35. DomainProgression exige quest concluída, saldo suficiente e um mês de calendário desde o último reset; zera o domínio e suas técnicas, preservando a base. A validação é uma regra de domínio, sem checkout ou débito financeiro real integrado. O cliente informa essa indisponibilidade. Conteúdo da quest e serviço de carteira continuam pendentes; o intervalo por personagem é uma interpretação provisória.

Foram corrigidos o evento de morte repetido em alvo já morto, a redução indevida de XP em recompensa e o alvo oculto mais próximo impedindo ataque a outro visível. A atribuição de XP geral/domínio valida as duas parcelas antes de alterar o estado, evitando atualização parcial em caso de overflow.

## 4. Interface e World Studio

CharacterCreation troca os dois arquétipos por quatro raças, apresenta o início como pequena alma/aprendiz e informa a ausência de domínio. CharacterSelection mostra raça, domínio e XP fracionária. RpgMenu remove a entrada de arena do menu atual sem remover o motor que sustenta outras ferramentas.

DomainPreview adiciona demonstrações Java2D animadas dos dez caminhos e lista técnicas de conceito. A consulta não concede XP nem desbloqueios. A escolha é habilitada apenas no nível 15 e sem domínio atual. Foi corrigido um problema de primeiro layout que podia ocultar a cena da prévia.

RpgLauncher integra prévias, escolha, salvamento e mensagens de reset, e atualiza a barra de ações para Shunpo e socos. RpgHud, RpgCombatStatus e GettingStarted refletem raça, domínio, XP e nível mínimo do Shunpo. RpgPlayerSprites, RpgVisualState e RpgCombatFeedback diferenciam feixes, arco e socos por eventos reais, sem criar regras de combate no desenho.

StudioExport e StudioPlaytest passam a criar Reaper. Foi removido o vínculo duplicado da tecla F no playtest. Editor, colisão, movimento e testes de mapa continuam preservados.

## 5. Launcher, distribuição e limpeza

Os cinco inicializadores Windows usam o fluxo compartilhado. compile-local.ps1 verifica JDK 17+, hashes de fontes/recursos/saídas e recompila para --release 17 quando necessário. A saída é isolada em server/target/launcher/classes. Uma falha de compilação impede o uso silencioso de classes antigas.

launch-local.ps1 seleciona cliente RPG, sprites, editor ou mapa. Os scripts portáteis de RPG, sprites e Studio compilam as fontes atuais e ajustam módulos do runtime ao JDK disponível. O empacotamento RPG aceita reutilização de runtime previamente validado por binários e módulos.

LauncherSelfCheck e a opção --verify-install verificam quatro raças, dez domínios, bloqueios iniciais e ciclo do save V2 sem janela nem personagens pessoais. run-local-tests.py executa JUnit Jupiter com dependências já presentes no cache, sem Maven ou rede. gerar_pdf_dominios.py gera o PDF de design.

Foram removidos KServer, Kclient, Network, DatabaseManager, Main, Archetypes e archetypes.properties, além das dependências Kryonet e PostgreSQL do POM. Permanecem o motor compartilhado, os testes úteis e o World Studio. A limpeza não apagou saves pessoais nem repositórios históricos de evidências.

O laboratório web anterior foi restrito ao proprietário por uma operação externa de Sites. Essa alteração de acesso está registrada na documentação, mas não é uma alteração de código no patch. O conector não oferecia exclusão definitiva.

## 6. Testes e evidências da entrega

Os registros da entrega em 21/10/2026 informam 191 fontes Java e 144 arquivos de teste compilados, 413 testes Java aprovados, 6 ignorados/abortados e zero falhas. A suíte Python registrou 30 testes aprovados. Esses números documentam a validação anterior; a geração deste PDF não reexecutou as suítes.

Os testes alterados e novos cobrem criação e seleção, prévias e layout, XP fracionária, bloqueio de escolha/troca, reset e calendário, preservação da base, migração V1/V2, Shunpo e recursos, alvo visível, evento de morte e compatibilidade com Studio. Os probes visuais e de jornada foram atualizados às novas raças e ações.

VALIDACAO-LAUNCHER-RACAS.json registra pacote 1.0.0-rc1 com Java integrado, 148 arquivos conferidos por tamanho/SHA-256, extração nova em caminho com espaços e retorno 0 do executável. O resultado informa quatro raças, dez domínios, saveSchema=2 e paymentEnabled=false. O compilador foi javac 24.0.2 com alvo Java 17.

Naquela sessão, a execução Java precisou ocorrer fora das restrições do sandbox por bloqueio de leitura de classpath/java.security. Isso não equivale a homologação em outro Windows nem a lançamento comercial.

## 7. Limites e próximos trabalhos

Ainda faltam o percurso jogável da pequena alma, árvores e skills finais, cálculo do nível de domínio, requisitos de técnicas, conteúdo da quest de reset, carteira e pagamentos reais, balanceamento e homologação de lançamento. As prévias não executam as skills finais no mundo de combate.

Controle poder atingir aliados da mesma party está definido no design, mas depende das skills e da integração multiplayer. A luz não ganha vantagem automática contra trevas. O cliente atual permanece local; o pacote validado é um candidato de desenvolvimento.

## 8. Inventário completo

A = adicionado; M = modificado; D = removido. + e - representam linhas do diff. Arquivos binários usam o marcador do Git. As dez entradas de documentação aparecem aqui por rastreabilidade; seu conteúdo completo não integra o anexo de código.

- [M] README-PROTOTIPO.md (+4 / -358)
- [M] README-RPG.md (+12 / -4)
- [A] docs/AUDITORIA-PRE-CLASSES.md (+170 / -0)
- [A] docs/ENTREGA-RACAS-LAUNCHER.md (+97 / -0)
- [M] docs/EXPERIMENTO-NAVEGADOR.md (+2 / -0)
- [M] docs/ROTEIRO-RPG-OFFLINE.md (+16 / -19)
- [A] docs/SISTEMA-DE-RACAS-E-DOMINIOS.md (+195 / -0)
- [A] docs/Spirit_Gate_Racas_e_Dominios.pdf (+169 / -0)
- [A] docs/VALIDACAO-LAUNCHER-RACAS-CLI.json (+1 / -0)
- [A] docs/VALIDACAO-LAUNCHER-RACAS.json (+63 / -0)
- [M] jogar-rpg-offline.cmd (+3 / -7)
- [M] rodar-prototipo.cmd (+5 / -32)
- [M] server/pom.xml (+1 / -12)
- [D] server/src/main/java/com/projetok/DatabaseManager.java (+0 / -30)
- [D] server/src/main/java/com/projetok/KServer.java (+0 / -71)
- [D] server/src/main/java/com/projetok/Kclient.java (+0 / -29)
- [D] server/src/main/java/com/projetok/Main.java (+0 / -11)
- [D] server/src/main/java/com/projetok/Network.java (+0 / -46)
- [M] server/src/main/java/com/projetok/domain/combat/CombatService.java (+4 / -1)
- [M] server/src/main/java/com/projetok/prototype/rpg/CharacterCreation.java (+51 / -34)
- [M] server/src/main/java/com/projetok/prototype/rpg/CharacterSelection.java (+13 / -11)
- [A] server/src/main/java/com/projetok/prototype/rpg/DomainPreview.java (+169 / -0)
- [M] server/src/main/java/com/projetok/prototype/rpg/GettingStarted.java (+1 / -1)
- [A] server/src/main/java/com/projetok/prototype/rpg/LauncherSelfCheck.java (+28 / -0)
- [M] server/src/main/java/com/projetok/prototype/rpg/RpgCombatFeedback.java (+18 / -3)
- [M] server/src/main/java/com/projetok/prototype/rpg/RpgCombatStatus.java (+3 / -3)
- [M] server/src/main/java/com/projetok/prototype/rpg/RpgHud.java (+2 / -1)
- [M] server/src/main/java/com/projetok/prototype/rpg/RpgLauncher.java (+24 / -5)
- [M] server/src/main/java/com/projetok/prototype/rpg/RpgMenu.java (+0 / -1)
- [M] server/src/main/java/com/projetok/prototype/rpg/RpgPlayerSprites.java (+4 / -4)
- [M] server/src/main/java/com/projetok/prototype/rpg/RpgVisualState.java (+1 / -1)
- [D] server/src/main/java/com/projetok/rpg/Archetypes.java (+0 / -21)
- [M] server/src/main/java/com/projetok/rpg/CharacterIdentity.java (+6 / -4)
- [A] server/src/main/java/com/projetok/rpg/CharacterRaces.java (+34 / -0)
- [M] server/src/main/java/com/projetok/rpg/CharacterSaves.java (+1 / -1)
- [A] server/src/main/java/com/projetok/rpg/DomainProgression.java (+83 / -0)
- [A] server/src/main/java/com/projetok/rpg/DomainResetPolicy.java (+14 / -0)
- [A] server/src/main/java/com/projetok/rpg/Domains.java (+43 / -0)
- [M] server/src/main/java/com/projetok/rpg/EnemyService.java (+6 / -3)
- [M] server/src/main/java/com/projetok/rpg/LocalSaveStore.java (+9 / -5)
- [M] server/src/main/java/com/projetok/rpg/RpgCombatEvent.java (+1 / -1)
- [M] server/src/main/java/com/projetok/rpg/RpgProgression.java (+9 / -5)
- [M] server/src/main/java/com/projetok/rpg/RpgSave.java (+2 / -1)
- [M] server/src/main/java/com/projetok/rpg/RpgSecondaryAbility.java (+4 / -7)
- [M] server/src/main/java/com/projetok/rpg/RpgWorld.java (+81 / -26)
- [M] server/src/main/java/com/projetok/studio/StudioExport.java (+1 / -1)
- [M] server/src/main/java/com/projetok/studio/StudioPlaytest.java (+3 / -3)
- [D] server/src/main/resources/rpg/archetypes.properties (+0 / -2)
- [M] server/src/main/resources/rpg/content.properties (+1 / -0)
- [M] server/src/main/resources/rpg/items.properties (+1 / -0)
- [M] server/src/main/resources/rpg/progression.properties (+1 / -1)
- [A] server/src/test/java/com/projetok/domain/combat/CombatServiceDeathTest.java (+31 / -0)
- [M] server/src/test/java/com/projetok/prototype/rpg/AmbientFeedbackTest.java (+1 / -1)
- [M] server/src/test/java/com/projetok/prototype/rpg/CharacterCreationTest.java (+11 / -5)
- [M] server/src/test/java/com/projetok/prototype/rpg/CharacterFactsTest.java (+1 / -1)
- [M] server/src/test/java/com/projetok/prototype/rpg/CharacterSelectionTest.java (+2 / -2)
- [M] server/src/test/java/com/projetok/prototype/rpg/CharacterSelectionVisualTest.java (+5 / -4)
- [M] server/src/test/java/com/projetok/prototype/rpg/CorujaoHudTest.java (+14 / -6)
- [A] server/src/test/java/com/projetok/prototype/rpg/DomainPreviewTest.java (+43 / -0)
- [M] server/src/test/java/com/projetok/prototype/rpg/GettingStartedTest.java (+2 / -1)
- [M] server/src/test/java/com/projetok/rpg/ArchetypesTest.java (+12 / -4)
- [M] server/src/test/java/com/projetok/rpg/CharacterIdentityTest.java (+2 / -2)
- [M] server/src/test/java/com/projetok/rpg/CorujaoAbilitiesTest.java (+29 / -18)
- [M] server/src/test/java/com/projetok/rpg/CorujaoCombatRegressionTest.java (+8 / -8)
- [A] server/src/test/java/com/projetok/rpg/DomainProgressionTest.java (+83 / -0)
- [M] server/src/test/java/com/projetok/rpg/DungeonCycleTest.java (+1 / -1)
- [M] server/src/test/java/com/projetok/rpg/EnemyRpgTest.java (+2 / -2)
- [M] server/src/test/java/com/projetok/rpg/EquipmentRpgTest.java (+1 / -1)
- [M] server/src/test/java/com/projetok/rpg/ItemCatalogTest.java (+1 / -1)
- [A] server/src/test/java/com/projetok/rpg/LegacyRaceMigrationTest.java (+30 / -0)
- [M] server/src/test/java/com/projetok/rpg/LocalSaveStoreTest.java (+1 / -1)
- [M] server/src/test/java/com/projetok/rpg/OfflineCycleTest.java (+1 / -1)
- [M] server/src/test/java/com/projetok/rpg/RpgProgressionTest.java (+8 / -2)
- [A] server/src/test/java/com/projetok/rpg/RpgRaceRuntimeTest.java (+76 / -0)
- [M] server/src/test/java/com/projetok/studio/StudioFinalRegressionTest.java (+1 / -1)
- [M] server/src/test/java/com/projetok/studio/StudioIntegrationTest.java (+1 / -1)
- [M] server/src/test/java/com/projetok/studio/StudioRuntimePlaytestTest.java (+1 / -1)
- [M] testar-mapa-rpg.cmd (+3 / -3)
- [M] testar-rpg-sprites.cmd (+3 / -9)
- [M] tools/SpiritGateCreationProbe.java (+3 / -3)
- [M] tools/SpiritGateFinalJourneyProbe.java (+8 / -6)
- [M] tools/SpiritGateSelectionProbe.java (+1 / -1)
- [M] tools/SpiritGateSelectionVisualProbe.java (+1 / -1)
- [M] tools/build-rpg-portable.ps1 (+33 / -6)
- [M] tools/build-rpg-sprites-portable.ps1 (+9 / -5)
- [M] tools/build-world-studio.ps1 (+7 / -3)
- [A] tools/compile-local.ps1 (+112 / -0)
- [A] tools/gerar_pdf_dominios.py (+100 / -0)
- [A] tools/launch-local.ps1 (+40 / -0)
- [A] tools/run-local-tests.py (+137 / -0)
- [M] world-studio.cmd (+2 / -6)
