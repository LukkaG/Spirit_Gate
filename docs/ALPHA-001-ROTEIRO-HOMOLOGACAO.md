# ALPHA-001 — Roteiro de homologação

Preparado em 17/11/2026 para HEAD `c3d484e76f17c2d93bf0c68ad3ff54280ee8ad39`. **NÃO REALIZADO**: roteiro manual futuro para duas máquinas. Não é autorização para distribuir, instalar ferramentas, publicar, ativar timeout ou alterar o jogo.

## Pré-condições e segurança do ensaio

Resolver/decidir formalmente o gate A-04 antes de convidar testers externos. Selecionar um candidato imutável com binário, conteúdo, assets, runtime e hashes fornecidos pelo responsável, sem usar o nome da pasta de dist como atestado de equivalência ao HEAD. Escolher qual entrada deve ser homologada: EXE nativo e/ou bootstrap atualizável; testar ambas se ambas forem prometidas.

PC A/PC B Windows no ambiente alvo, diretórios novos sem saves reais; dois participantes consentidos; identificadores fictícios. Rede autorizada (Radmin se escolhido), com adaptador/porta definidos pelo responsável. Não alterar firewall, adaptadores ou configurações automaticamente. Trocar papéis A/B em uma segunda sessão. Não conectar serviços/DB/feeds externos sem autorização específica do ensaio.

Antes do teste, informar claramente: **“personagem e progresso desta partida são temporários; reconectar cria nova sessão e não restaura XP, itens, moedas, missões nem recargas anteriores”**, se esse escopo for aprovado. A existência do botão “Reconectar nova sessão” não substitui o aviso e o aceite.

O caminho com conteúdo depende da revisão ativa ou do builtin escolhido por `LauncherSession.start`; não foi inspecionado conteúdo pessoal de studio-projects. Portanto o hash e o mapa efetivo devem ser registrados antes de prometer percurso. Não promover nem publicar conteúdo para preparar este roteiro.

## Percurso de ponta a ponta

Cada linha exige evidência de ambos os peers e o resultado esperado. Evidência atual é estática/histórica; nenhuma transição foi executada pela ALPHA-001. Sequência funcional, sem tempos garantidos. Se não for possível cobrir em 20–30 min, registrar o ponto de interrupção e revisar escopo, sem completar artificialmente a ficha.

| Transição / pré-condição | Componentes / referências reais | Evidência atual / risco | Como executar e critério observável |
|---|---|---|---|
| 1. Instalar/abrir; candidato aprovado | [manual atualizável](LAUNCHER-RADMIN-ATUALIZAVEL.md); tools/build-windows-portable.ps1; RpgLauncher.main | Pacotes históricos; físico PENDENTE / P1, P0 se impossível | Em A/B, extrair/instalar em pasta nova; comparar hashes; abrir EXE prometido com runtime incluído e sem JDK de desenvolvimento como dependência; registrar versão, origem confiável, janela inicial e falhas |
| 2. Anfitrião inicia; porta/rede autorizadas | RadminSessionPanel.start; LauncherSession.start; LocalSimulationServer.start | Capacidade dois e guest guard no código / P1 | A escolhe hospedar e adaptador autorizado; confirmar início e erro legível em porta inválida/ocupada apenas no ambiente descartável; B não deve receber opção administrativa indevida |
| 3. Convidado conecta; A ativo | RadminSessionPanel.connect; LocalClient; Protocol.Hello; RevisionCoordinator | TCP 6/7 histórico / P1 | B conecta pelo endereço acordado sem divulgar IP nos relatórios; conferir versão de aplicação/hash de conteúdo e erro de incompatibilidade em fixture separada; não testar terceiro peer |
| 4. Ambos criam/escolhem; recursos carregados | SessionCharacterCreation; SessionCharacterSelection; RpgSessionWorld.join/joinCharacter; NetworkPlayerVisuals.renderInfo | Escolha efêmera e assets estáticos confirmados / P1 | Criar nomes fictícios distintos; percorrer Human/Reaper/Aetheri/Spirit em rodadas descartáveis; mostrar aparência provisória quando aplicável; não carregar save pessoal |
| 5. Entrada na região inicial; spawn livre | RpgSessionWorld.joinCharacter; WorldContent/CorujaoSevenWorld, revisão ativa | forest/spawns/offsets no código / P1 | Registrar nome/ID/hash do mapa; ambos aparecem sem sobreposição/parede; spawn ocupado deve recusar sem criar corpo parcial; manter outro peer conectado |
| 6. Explorar e observar; ambos admitidos | RpgSessionWorld.step; RemoteTimeline; RemoteSpriteAnimation; MobNavigation.route | Movimento/snapshots presentes / P1 | WASD/setas nos oito sentidos, parar, alternar foco; B vê A e vice-versa; enfrentar obstáculo e passagem estreita, sem atravessar; diagonal sem vantagem de velocidade; corpo cardinal é limite conhecido |
| 7. Aceitar missão antes de matar; NPC alcançável | RpgSessionWorld.questBook/request; corujao-v.sgquest; AuthoredQuestSession; QuestService | coop_slime em village/elder; um slime / P1 | Usar portal real forest→village e falar com elder; aceitar via UI/atalho atual; verificar AVAILABLE→ACTIVE na ficha de cada personagem; se o conteúdo ativo trocar esse livro, registrar a missão efetiva e não usar a outra por suposição |
| 8. Selecionar inimigo; voltar à região | GameInputRouter; LocalClient.selectTarget; NetworkSkillControls | TAB/SHIFT+TAB e alvo/geração no caminho atual / P1 | Alternar alvo por TAB/SHIFT+TAB e mouse; distinguir aliado/NPC/mob; alvo fora de região, morto ou fora de alcance deve ser recusado coerentemente |
| 9. Básico e skill; domínio habilitado | RpgSkillEngine.cast; DomainProgression.choose; NetworkHotbar; NetworkSkillSubmission | Domínio exige 15; jogador comum nível 1; caminho de acesso em 20–30 min NÃO VERIFICADO / P1 | Testar SPACE após TAB e F5–F9/remapeamento com uma skill realmente habilitada pelo perfil aprovado. Se não houver habilidade disponível, marcar transição BLOQUEADO para esse percurso; não conceder nível/XP por fixture escondida |
| 10. Confirmar recursos e feedback; skill disponível | GameplayProtocol.Sheet; SkillProtocol.State; NetworkSkillPresentation | Custo/recarga autoritativos; preview local não confirma / P1 | Registrar HP/EN/recarga antes/depois; tentar durante cooldown e sem energia; cancelar área por ESC sem gasto se ainda não enviada; observar aviso real do servidor, separado de prévia de alcance/LOS |
| 11. Inimigo morre e recompensa; ambos contribuíram | SharedRpgEncounter.hit; RpgWorld.sharedKill/sharedReward; LootService | Ledger por spawn/geração; piso 10.000 com conteúdo; R1=40 HP em teste / P1 | Ambos atacam e permanecem vivos/região; conferir morte igual nos dois clientes e XP/drop/moedas **nas fichas individuais**. Registrar tipo real do mob; não exigir drop de tipo sem recompensa configurada. Repetir evento/ação só em regressão isolada, não por ferramenta sobre peer real |
| 12. Missão READY e entrega única; objetivo atendido | QuestService.state/complete; AuthoredQuestSession.event; coop_slime | R1 saneou fixture; quest dá 0 XP/0 moedas e 1 potion / P1 | Conferir ACTIVE→READY após slime elegível; retornar a elder e entregar; READY→COMPLETED. Repetir entrega: não ganhar segunda poção/XP/moeda; B não herda conclusão de A sem seu próprio progresso |
| 13. Inventário/equipamento/consumo; itens reais disponíveis | InventoryService; ItemCatalog; NetworkInventoryPanel; RpgSessionWorld.gameplay | Interfaces existentes; disponibilidade de item no candidato NÃO VERIFICADO / P1 | Abrir inventário, equipar item existente e atender requisito; se não existir item equipável na jornada, registrar lacuna. Usar potion após dano, confirmar quantidade/HP e cooldown; não inventar item para completar a linha |
| 14. Morte, derrota e retorno; local seguro | RpgWorld.receiveDamage/returnHome; RpgSessionWorld.request(RESPAWN); NetworkGameHud | Caminho de retorno presente / P1 | Morrer de forma controlada; input de gameplay impedido/feedback legível; retornar só em apoio livre; apoio ocupado deve recusar sem sobrepor. Se Luz estiver habilitada, testar light_3 vivo/morto/obstruído sem dupla cura |
| 15. Queda de conexão; recursos/teclas ativos | LocalClient.reconnect; RpgSessionWorld.leave; GameInputRouter.reset; RpgReconnectIsolationTest | Readmissão nova sem restauração; R2 laboratório / P1 | Desconectar B pela rede no ensaio autorizado enquanto se move e tem cooldown; A continua. Verificar limpeza do ator/efeitos, envio bloqueado no desconectado, aviso de perda. Reconectar: ID/UUID novos, sem input/mira herdados; não confundir reset com preservação de recarga |
| 16. Encerrar sessão; teste concluído | RpgNetworkClientLauncher listeners; LauncherSession; LocalSimulationServer.close | Cleanup presente, sem execução atual / P1 | Fechar convidado e anfitrião pelos controles normais; conferir término dos processos da instalação, nenhuma alteração em saves anteriores e logs sanitizados. Trocar papéis para segunda rodada independente |

### Lacunas de conteúdo e alternativa de escopo

No builtin da sessão, `coop_slime`/elder e portais existem, mas isso não comprova que o conteúdo selecionado do candidato os preserve. `quests.properties` descreve outras missões; não deve ser apresentado como livro necessariamente ativo. Equipamento disponível e acesso a uma skill exigem escolha concreta do conteúdo/perfil.

Se a jornada completa não couber, propor ao responsável um recorte menor **explicitamente aprovado**: exploração + básico + missão efêmera, deixando skill/progressão/equipamento fora do objetivo declarado; ou um perfil de teste autorizado com nível/conteúdo apropriado. A ALPHA-001 não implementa nenhum perfil e não considera o requisito original satisfeito por esse recorte hipotético.

## Ensaios dedicados

### A-04, somente em ambiente isolado aprovado

Usar reprodução de materialização sem apoio com engine real e observador, não bloquear o mapa do tester externo. Verificar quatro cortes únicos, fase, tentativa de apoio, ticks 31/32/91/92/93 e liberação tardia, efeito sobre outro jogador e tempo do owner. Ticks 92 são bordas do **candidato histórico**, não prazo instalado. A ausência de exploração no mapa inicial não aprova o código sem decisão. Nenhuma execução deste ensaio foi feita aqui.

### A-03 visual e físico em pistas separadas

Com alvo estático/lateral/móvel, registrar posição autoritativa e HP por tick e imagem do projétil/impacto no mesmo referencial. Incluir lançador morto/ausente com snapshot anterior e posterior, alvo morto, troca de geração e região. Confirmar limpeza existente do lançador sem anunciar terminal universal de alvo. Não reutilizar 28,676 como resultado novo. Descrever se o erro faz jogador entender acerto/esquiva de forma errada; só então julgar o bloqueio P1 no escopo.

### Input/HUD/UX

Resoluções 640×480 e 1366×768, além da nativa dos PCs; resize, zoom, sobreposição e arrasto limitado. Navy com alfa real, contorno ciano, mundo cobrindo área disponível, minimapa/hotbar legíveis. Abrir texto/menu/inventário e alternar TAB/SHIFT+TAB/SPACE/ENTER/ESC, mouse, remapeamento e Alt+Tab. Após perda de foco, nenhuma tecla presa. Capturar falha sem apagar ocorrência por uma repetição verde. Distinguir painel interno de modal de entrada; não exigir remover JFrame principal do jogo. Áudio: testar o que a UI oferece, sobretudo volume geral/mute, sem pressupor sliders independentes.

### Distribuição/atualização/rollback

Em cópias descartáveis e sem apontar a feed externa: verificar manifestos/hashes de todas as partes, runtime e assets necessários, perfis Anfitrião/Convidado e inicialização por EXE/entrada prometida. Falha de hash/arquivo interrompido deve recusar ativação parcial e conservar instalação anterior. Testar versão anterior somente pelos meios locais aprovados e registrar se rollback é apenas retenção da versão ou troca efetiva. Não efetuar publicação nem editar manifestos históricos para simular aprovação.

## Protocolo de 20–30 minutos e desempenho

Planejamento sugerido: 0–5 min instalação/conexão/criação; 5–10 exploração/NPC/missão; 10–20 combate/recompensa/skill/itens; 20–25 morte/retorno/queda/reentrada; 25–30 encerramento e coleta. É orçamento de teste, **não previsão de duração comprovada**. Se combate ou desbloqueio consumirem a sessão, registrar falha do percurso. Segunda sessão com papéis trocados deve ter seu próprio registro.

Amostragem inicial e a cada minuto em A/B: FPS (média/mínimo/p95 de frame quando disponível), CPU do processo, RAM privada/working set, pausas/contagens GC quando ferramenta já disponível, duração/intervalo do tick (nominal ~33,33 ms), RTT/latência, fila de comandos/controle/snapshots e eventos de perda/substituição. Usar ferramentas já instaladas; não instalar profiler para cumprir o roteiro.

Sem limite de FPS/RAM/RTT aprovado no pedido, registrar valores brutos e percentis sem inventar SLO. Proposta para aprovação: 30 FPS sustentados no hardware alvo, sem pausas que interrompam controle e sem fila crescente; calibrar limites de tick/RTT/memória com o responsável antes de chamar performance aprovada. Não confundir média boa com ausência de picos ou medição loopback com internet/Radmin.

## Ficha de evidência (a preencher)

```text
Ensaio / operador / data-hora inicial-final com fuso:
PCs: A e B (sem nome pessoal); Windows/CPU/RAM/GPU/resolução:
Papéis A/B; rede autorizada (LAN/Radmin), sem IP privado no documento:
Versão exibida / HEAD do código / hash binário / hash conteúdo / hashes runtime-assets:
Origem do candidato e entrada usada (EXE/atualizável):
Instalações limpas e diretórios descartáveis confirmados:
Personagens fictícios / raça / nível / domínio / skill habilitada:
Aviso de progresso temporário aprovado e mostrado:
Mapa/região/NPC/inimigo/missão/itens efetivos:
Transição 1..16: pré-condição, horário, ação, esperado, observado:
FPS/CPU/RAM/GC/tick/RTT/filas por minuto e resumo p50/p95/máximo:
Queda/retorno: ID/geração antes-depois (somente identificador técnico necessário):
XP/HP/EN/moedas/itens/missão/cooldowns antes-depois, sem dump de save:
A-03: contato/HP versus desenho, cenário e medição nova:
A-04: referência ao ensaio isolado aprovado, nunca presumido resolvido:
Reprodução de falha: passos mínimos, frequência, vídeo/captura sanitizada:
Arquivos de evidência novos, sem sobrepor checkpoints históricos:
Resultado: IMPLEMENTADO / TESTADO / PENDENTE / BLOQUEADO / NÃO VERIFICADO:
Severidade P0/P1/P2; responsável/decisão pendente:
Encerramento dos processos / segunda rodada com papéis trocados:
```

Remover credenciais, nomes pessoais, IPs privados, tokens, caminhos de contas e saves dos logs/capturas antes de anexar. Uma ficha preenchida sem execução não é prova. Agregar casos únicos por cenário e resultado mais recente; manter falhas anteriores e reexecuções separadas, sem inflar aprovação.

## Automatizados: realizados versus propostos

Realizados pela ALPHA-001: apenas inventário estático de 40 definições, 71 atlas e 56 frames/hashes, com script inline leitor; **zero casos JUnit/TCP/GUI**. Propostos, não realizados: seleção isolada dos testes citados na [matriz](ALPHA-001-MATRIZ-PRONTIDAO.md), cópia pública temporária com outputs/logs/home isolados, Maven offline sem instalar dependências e auditoria prévia de cada escrita. A suíte geral, os smokes e os builders existentes não devem ser executados diretamente sobre evidências históricas.

Teste em dois PCs, Radmin e EXE limpo: **PENDENTE / não realizado**. O preenchimento e a aprovação deste roteiro pertencem a uma autorização futura.

