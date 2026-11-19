# ALPHA-001 — Riscos, gates e decisões

17/11/2026; branch `beco`; HEAD `c3d484e76f17c2d93bf0c68ad3ff54280ee8ad39`. Recomendação **NÃO LIBERAR**. Registro novo, sem editar gates históricos.

## Critério

P0: invulnerabilidade infinita, travamento, corrupção/perda não consentida, duplicação/isolamento quebrado, exploit crítico, instalação/conexão impossível ou dependência obrigatória de recuperação não autorizada. P1: jornada interrompida, skill/controle indispensável sem uso, progressão inviável, visual gravemente enganoso ou desempenho insuficiente. P2: acabamento e recurso dispensável no escopo aprovado.

Estados: IMPLEMENTADO, TESTADO, PENDENTE, BLOQUEADO, NÃO VERIFICADO. Gravidade de risco não significa ocorrência comprovada. Custos abaixo são relativos (baixo/médio/alto), não estimativas de prazo.

## Registro priorizado

| ID | Prioridade / situação Alpha | Impacto e evidência | Probabilidade/reprodutibilidade | Dependência / custo-risco | Condição objetiva para resolver / decisão exigida |
|---|---|---|---|---|---|
| R01 / A-04 | P0 / BLOQUEADO | `LightningField.advance` conserva fase se `skillReappear` falha; `receiveDamage` ignora dano na fase; reprodução histórica aos 180 ticks | Reproduzível em câmara artificial; ocorrência explorável no mapa candidato NÃO VERIFICADO | Política de término, identidade/recuperação se escolhida; custo alto, risco de gameplay | Responsável aprovar caminho formal; patch e testes isolados com falta de apoio real, observador, bordas e cleanup; nenhum timeout ativado nesta auditoria |
| R02 / Continuidade | P1 / PENDENTE; P0 se recuperação for obrigatória sem autorização | Readmissão cria sessão nova e descarta progresso/recargas; não prometer continuidade | Consequência explícita do código e R2; experiência física não ensaiada | Aceite de progresso temporário e comunicação; baixo para escopo, alto para recuperação | Aceite escrito do escopo de sessão e aviso prévio ao tester; se recuperação exigida, outra tarefa autorizada e contrato completo antes de liberar |
| R03 / Jornada e skill inicial | P1 / PENDENTE | Conteúdo aplica 10.000 HP; criação usual nível 1; domínio no 15; teste R1 usa 40 HP | Diferença estática confirmada; tempo real NÃO VERIFICADO | Perfil de partida/catálogo/mapa e eventual aprovação numérica; médio | Duas pessoas completam jornada com ao menos uma skill em 20–30 min no binário/conteúdo candidato, sem comando de teste oculto; alternativamente aprovar escopo menor explicitamente |
| R04 / Instalação/conexão | P1 / PENDENTE; elevar a P0 se impossível | EXE nativo e dois PCs/Radmin sem homologação atual | NÃO VERIFICADO; builds e TCP histórico não substituem instalação | Pacote congelado, Windows alvo, duas máquinas/rede autorizada; médio | Instalação limpa A/B, hashes, runtime/assets, handshake e troca de papéis funcionando |
| R05 / A-03 trajetória | P1 / PENDENTE | Curva intermediária diverge da reta visual; terminal de alvo sem identidade completa; 28,676 histórico | Reproduzido historicamente em cenário controlado; magnitude atual NÃO VERIFICADO | Contrato wire/decisão UX; médio-alto se houver extensão | Repetir alvo móvel/lateral, morte/geração/região, medir contato/HP separados do desenho; aceitar limite de UX ou aprovar solução própria sem regredir 6/7 |
| R06 / Foco | P1 / PENDENTE | ORG-02-R2 terminou sem causa da intermitência; TAB/SPACE/ENTER são centrais | Intermitente histórico, não reproduzido nesta etapa | Desktop reservado/teclado físico; médio, risco de remendo | Jornada com texto, modais, Alt+Tab, resize e reconexão sem ataque em botão indevido/tecla presa; falha com rastreio, sem retry contado como solução |
| R07 / Economia e privacidade | P0 potencial / NÃO VERIFICADO | Duplicação/isolamento quebrado seria crítico; existem ledgers, sequências e ficha privada, sem falha nova demonstrada | Sem exploit atual reproduzido; testes antigos relevantes | Mesmo conteúdo/peer e concorrência; médio | Replays de kill/missão/consumo sem dupla concessão; B não recebe ficha privada de A; ID/geração antigos não alteram nova sessão |
| R08 / Estabilidade | P1 / NÃO VERIFICADO | Busca global A-04 roda no owner; filas limitadas não garantem tick/FPS | Medições históricas variáveis; sem sessão atual contínua | Resolver gate A-04, hardware/rede/métricas; médio | Medir 20–30 min, tick/latência/FPS/CPU/RAM/GC/filas, sem crash/trava/crescimento contínuo inexplicado; aprovar orçamento numérico |
| R09 / Segurança do canal | P1 / NÃO VERIFICADO | App não demonstrado como autenticado/confidencial; role guest não prova identidade | Framing tem limites; auditoria adversarial completa não feita | Acesso fechado e confiança da rede; médio-alto se ampliado | Restringir teste à rede aprovada, sem credenciais/saves reais; fuzz local isolado e decisão separada antes de exposição pública |
| R10 / Arte racial e direções | P2 / PENDENTE | Human/Reaper apenas IDLE/WALK cardinais; Aetheri/Spirit provisórios Human | Confirmado estático; estética atual NÃO VERIFICADO | Aceite artístico; baixo para documentar, alto para novos conjuntos | Responsável aceitar arte provisória e feedback de combate legível; oito vetores de skill não aprovam oito poses |
| R11 / IA/conteúdo/recompensas | P1 / NÃO VERIFICADO | Caminhos existentes, mas mobs novos podem não recompensar e mapa ativo depende do launcher | Sem sessão real no conteúdo congelado | Escolher região/NPC/inimigo/recompensas existentes; médio | Dois jogadores veem mesma morte/respawn, XP/drop corretos e missão alcançável; não usar quests.properties como prova de quest ativa |
| R12 / Configurações/áudio | P2 / NÃO VERIFICADO | Volume geral/mute precisam ensaio; controles independentes não comprovados | Sem teste físico atual | Hardware e preferências descartáveis; baixo | Confirmar controles oferecidos e comunicação sem prometer recurso ausente |

## A-04: prova operacional e alternativas, sem implementação

Fontes atuais: [LightningField.advance](../server/src/main/java/com/projetok/rpg/RpgSkillEngine.java), [skillReappear/receiveDamage](../server/src/main/java/com/projetok/rpg/RpgWorld.java), [playerReentrySpace/stepRegion](../server/src/main/java/com/projetok/rpg/SharedRpgEncounter.java). O executor resolve os quatro cortes uma vez, mantém `resolved` e volta nos ticks seguintes se não houver apoio. Não repete os quatro danos na espera. O jogador continua intangível e fora da seleção usual dos mobs. O owner continua processando o mundo e o outro peer, mas a busca de apoio consome o mesmo tick e pode atrasá-lo. Não há medição atual de atraso.

`skillReappear` testa origem, círculos até raio 128 e varredura da área em passo 4, respeitando travessia do cenário; não garante apoio quando tudo está ocupado. O renderer tem fusível visual de cinco segundos, que não encerra a imunidade autoritativa. O teste histórico de 180 ticks não é um máximo implementado.

| Referência | O que efetivamente provou | O que não provou |
|---|---|---|
| [225 / R1](TASK-225-R1-RELATORIO-FINAL.md) | Condição extrema real no engine, quatro cortes, espera e retorno; candidato isolado a 92 ticks | Prazo instalado em produção, performance garantida ou exploração no mapa de release |
| [R2](TASK-225-R2-RELATORIO-FINAL.md) | Retirada TCP por adaptador de teste, observador permanece, fila antiga recusada, readmissão efêmera revalidada | Engine solicitando retirada ao chegar a 92 sem apoio; recuperação do personagem |
| [R3](TASK-225-R3-RELATORIO-FINAL.md) | Envelope sintético, posse/consumo único e cenários locais da política B | Importação real multiplayer, peer autenticado, durabilidade após restart ou integração ORG-03 |

| Opção reservada ao responsável | Benefício | Custo/risco e dependência |
|---|---|---|
| Correção formal aprovada | Encerramento verificável com política clara | Definir término seguro, recursos, identidade, efeitos e recuperação se necessária; testes owner/TCP; não acoplar a ORG-03 implicitamente |
| Mitigação temporária aprovada | Reduzir exposição num recorte interno | Demonstrar alcance da mitigação no mapa/escopo e comunicar limites; restringir mapa não prova ausência universal do problema; retirada pode perder progresso/resetar recargas |
| Exclusão controlada da skill aprovada | Retirar a condição do escopo distribuído | Altera disponibilidade/identidade do kit e exige UI/servidor consistentes e regressão; não basta esconder botão; nenhuma skill desabilitada aqui |

Habilitar 92 ticks isoladamente **não é correção aprovada**: retiraria o jogador sem necessariamente recuperar XP/itens/recargas. A opção B recomendada historicamente não é autorização de implementação e não é resolvida pelo simples fato de existirem documentos ORG-03/D33.

## A-03: separar os três assuntos

1. Limpeza do lançador: correção presente em `NetworkSkillPresentation.actors`, incluindo tick antigo e prevenção de replay; estado **IMPLEMENTADO**, teste histórico identificado.
2. Trajetória intermediária: alvo móvel é seguido pela simulação, apresentação usa segmento inicial. Medida 28,676 é histórica; estado **PENDENTE / P1** para julgamento visual no Alpha.
3. Terminal do alvo: morte/geração inválida pode cancelar executor sem identificar aquele voo na apresentação até expirar. Não extrapolar a limpeza do lançador a todo cancelamento.

Contato físico e queda de HP foram corretos no ensaio antigo. Não há evidência atual de dano duplicado/exploit que permita chamar A-03 de P0. Se o visual comprometer seleção/esquiva/jornada, bloquear o escopo como P1 até decisão. Não alterar aplicações 6/7 nesta tarefa.

## Piso de HP, XP e acesso às skills

[CONTENT_ENEMY_MIN_HP](../server/src/main/java/com/projetok/multiplayer/RpgSessionWorld.java) permanece 10.000 na ativação de revisões; [EnemyService](../server/src/main/java/com/projetok/rpg/EnemyService.java) usa o máximo entre catálogo e piso. [EnemyDurabilityTest](../server/src/test/java/com/projetok/rpg/EnemyDurabilityTest.java) protege essa regra. [Task225R1CombatFixture](../server/src/test/java/com/projetok/multiplayer/Task225R1CombatFixture.java) prepara os 40 HP do slime do catálogo **em teste**. Esse contraste não prova jornada real viável.

A fábrica comum começa nível 1; domínio exige 15, e [progression.properties](../server/src/main/resources/rpg/progression.properties) tem step 60. Não afirmar número de kills/tempo total sem medir ganhos e conteúdo real. `coop_slime` dá uma potion, sem XP/moedas próprios; crédito do encontro e recompensa de missão são fontes diferentes. Escolher uma skill habilitada na jornada de 20–30 min é uma decisão concreta pendente, não um detalhe que fixtures sintéticas resolvem.

## Sequência sugerida ALPHA-002…008

Somente proposta; IDs não representam tasks criadas, aprovadas ou iniciadas. Se houver planejamento paralelo ainda não presente no checkout, conciliar antes de atribuição definitiva.

| Ordem proposta | Entrega pretendida | Dependência / checkpoint |
|---|---|---|
| ALPHA-002 | Aprovar escopo do Alpha: conteúdo/região, nível/acesso a uma skill, sessão temporária, arte e política A-04 | Ata de decisões; A-04 mantém bloqueio até solução aprovada; nenhuma integração automática com ORG-03 |
| ALPHA-003 | Implementar e verificar somente correção/mitigação A-04 explicitamente escolhida | Plano de patch e rollback próprios; ausência real de apoio, quatro hits únicos, cleanup e dois peers; sem ativar 92 por inferência |
| ALPHA-004 | Validar jornada, HP/XP/quest/itens e eventual ajuste aprovado; decidir tolerância A-03 | Conteúdo congelado, 20–30 min, dados de sessão; valores apenas com autorização |
| ALPHA-005 | Regressão segura de rede 6/7, privacidade, replays, reconexão e input | Árvore isolada, logs/outputs novos, contagens únicas, nenhum save real |
| ALPHA-006 | Validar GUI/arte/controles e medir desempenho em candidato local | Foco reproduzível ou limite aceito; métricas e critérios aprovados; sem arte nova implícita |
| ALPHA-007 | Preparar/verificar candidato Anfitrião/Convidado e instalação EXE limpa | Hash de binário/conteúdo/runtime; autorização de empacotamento própria; sem publicar canal |
| ALPHA-008 | Homologação física A/B e decisão de distribuição fechada | Roteiro preenchido, sessões 20–30 min, dois papéis, bugs classificados; publicação só com autorização separada |

## Decisões necessárias antes de implementar

Aprovar: política A-04 e consequências de eventual retirada; aceitação de progresso temporário e reset na readmissão; conteúdo/nível/caminho para uma skill; eventual balanceamento; tolerância A-03; arte provisória; métricas mínimas/hardware alvo; candidato EXE/entrada atualizável e rede autorizada. Recuperação durável, protocolo novo, integração ORG-03/D33, TASK-226, publicação e alterações de trajetórias exigem decisões próprias fora desta auditoria.

Não foram aplicadas mitigações nem fechados gates. Próximo ato desta conversa termina com a entrega ALPHA-001.

