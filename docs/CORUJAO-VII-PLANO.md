> Plano original recebido em 27/10/2026 e preservado abaixo. Em 31/10 o usuário autorizou todas as TASKs e pediu relatório PDF. Implementação e validação técnica local de 201–212 encerradas; revisão visual humana e Radmin físico permanecem pendentes. Consulte [relatório final](CORUJAO-VII-RELATORIO-FINAL.md). A distribuição oficial 0.201.0 conserva a entrega anterior; foram criados dois pacotes privados 0.212.0-private. O aceite do Corujão VI é histórico, não um teste novo.

> Autorização ampliada em 31/10/2026: o usuário autorizou executar todas as
> TASKs deste prompt em ordem e solicitou relatório completo em PDF ao final.
> Limites históricos de execução apenas de 201/202 ficam superados por essa
> instrução. Preservam-se as restrições técnicas e a entrega privada da TASK-212.

# SPIRIT GATE — CORUJÃO VII
## PLANO DIRETOR DE EVOLUÇÃO DA JOGABILIDADE
### TASK-201 até TASK-212

**Projeto:** Spirit Gate / Projeto-K  
**Linguagem principal:** Java 17  
**Arquitetura:** RPG 2D multiplayer, servidor autoritativo, cliente Swing/Java2D, comunicação TCP  
**Referência funcional:** Corujão VI concluído e versão gráfica 0.200.3, sujeitos à conferência no código atual  
**Objetivo:** transformar os sistemas técnicos já desenvolvidos em uma experiência de MMORPG progressivamente mais completa.

---

# 1. MISSÃO PRINCIPAL

Iniciar uma nova etapa de desenvolvimento do Spirit Gate.

Os ciclos anteriores concentraram esforços em:

- Comunicação multiplayer.
- Sincronização de jogadores.
- Colisões físicas.
- World Studio.
- Edição em tempo real.
- Publicação de mapas.
- Distribuição de revisões.
- Controle de integridade.
- Tratamento de desconexões.
- Combate PvE compartilhado.
- Persistência sintética experimental.
- Empacotamento e distribuição.
- Sprites Reaper no multiplayer.

O Corujão VII deverá se concentrar na experiência do jogador.

**Prioridade absoluta: criar funcionalidades visíveis, jogáveis, integradas e verificáveis, sem comprometer o funcionamento do multiplayer existente.**

Não reconstruir funcionalidades que o projeto já possui.

Primeiro auditar os sistemas atuais; depois estender somente o necessário.

---

# 2. REGRAS DE ENGENHARIA

Antes de implementar qualquer TASK:

1. Inspecionar o código real.
2. Ler a documentação existente.
3. Confirmar branch e HEAD, quando houver acesso ao Git.
4. Identificar alterações não commitadas.
5. Verificar os sistemas já implementados.
6. Mapear dependências técnicas.
7. Registrar o baseline.
8. Preparar um plano curto.
9. Aplicar alterações incrementais.
10. Criar testes positivos e negativos.
11. Executar regressões aplicáveis.
12. Documentar resultados e limitações.

## Preservação obrigatória

Não danificar:

- Protocolo multiplayer V1–V4.
- Compatibilidade histórica.
- Launcher Anfitrião/Convidado.
- Atualizador.
- World Studio e Preview Server.
- Publicação de mapas.
- Sistema de colisões.
- Combate PvE existente.
- Raças e domínios.
- Shunpo.
- Inventário e progressão existentes.
- Saves pessoais V1/V2.
- Assets originais.
- Checkpoints anteriores.
- Coop histórico.
- RPG offline.

Mudanças incompatíveis de protocolo, schema ou sistema de persistência exigem proposta arquitetural específica.

Não introduzir autenticação pública, banco externo, TLS ou contas reais como consequência indireta destas tarefas.

---

# 3. METODOLOGIA DE EXECUÇÃO

Executar as TASKs em ordem numérica, observando dependências.

Não implementar todas de uma só vez.

Cada TASK deve possuir:

- Escopo delimitado.
- Funcionalidade demonstrável.
- Testes específicos.
- Regressões.
- Evidências.
- Relatório final.
- Riscos e pendências.
- Próxima ação.

As TASKs devem reutilizar sistemas existentes.

**Não confundir código compilado com uma funcionalidade realmente jogável.**

Para funcionalidades visuais, preparar capturas ou testes gráficos quando possível.

Para funcionalidades multiplayer, utilizar servidor real e clientes reais em testes locais; mocks podem complementar, mas não substituir provas integradas.

Testes em dois computadores somente podem ser declarados realizados se houver evidência real dessa execução.

---

# 4. TASK-201 — CRIAÇÃO E SELEÇÃO DE PERSONAGEM

## Objetivo

Eliminar a atribuição fixa de raça de acordo com a ordem de conexão.

A documentação anterior indica que o primeiro participante recebia Reaper e o segundo Human. Confirmar se esse comportamento ainda existe.

Implementar um fluxo de criação e seleção de personagem para a sessão multiplayer experimental.

## Funcionalidades

- Interface de criação.
- Nome do personagem.
- Seleção de raça.
- Prévia da sprite disponível.
- Aparência compatível com os assets instalados.
- Validação dos campos.
- Confirmação de escolha.
- Entrada no mundo com personagem selecionado.

## Raças

- Reaper.
- Human.
- Aetheri.
- Spirit.

Uma raça sem sprites suficientes deverá indicar claramente essa limitação, utilizando somente um fallback previamente definido e compatível.

Não inventar assets ou apresentar uma raça incompleta como artisticamente concluída.

## Multiplayer

A escolha deverá ser consistente no servidor e nos dois clientes.

O servidor deve validar os dados e manter autoridade sobre o personagem criado.

O jogador A poderá selecionar Reaper enquanto o jogador B seleciona Human, ou ambos poderão selecionar a mesma raça, conforme as regras do jogo.

Não vincular a identidade permanente do personagem somente ao nome informado.

## Limites

Não implementar contas reais nem salvar personagens online permanentemente nesta tarefa.

A criação será inicialmente associada à sessão experimental.

Se for necessária extensão de protocolo, preparar análise de compatibilidade e solicitar autorização antes de introduzir mudança incompatível.

## Critérios de pronto

- Tela funcional.
- Seleção validada.
- Raças reconhecidas.
- Dois clientes exibem corretamente ambos os personagens.
- Nenhuma alteração indevida em personagens offline.
- Regressões essenciais aprovadas.

---

# 5. TASK-202 — SISTEMA DE SPRITES POR RAÇA

> Atualização de 31/10/2026: o usuário autorizou prosseguir com esta tarefa.
> Implementação local e inventário em [TASK-202.md](TASK-202.md), validações em
> [TASK-202-TESTES.md](TASK-202-TESTES.md). As referências a “não iniciada” no
> pedido original e no adendo anterior são históricas. TASK-203 não iniciada.

## Objetivo

Evoluir a renderização dos personagens para utilizar sprites específicas por raça.

## Auditoria

Inventariar:

- Reaper.
- Human.
- Aetheri.
- Spirit.
- Idle.
- Walk.
- Run.
- Attack.
- Hit.
- Death.

Registrar quais animações realmente existem.

Não confundir planejado com implementado.

## Implementação

- Catálogo de sprites por raça.
- Direções suportadas.
- Número de quadros.
- Escala.
- Pivô.
- Tempo por quadro.
- Alinhamento com o solo.
- Compatibilidade com acessórios.
- Fallback explícito e seguro.

Preservar o atlas Reaper integrado na versão 0.200.3.

Não alterar colisões apenas porque a sprite possui outra dimensão.

## Critérios de pronto

Duas raças diferentes devem ser corretamente identificadas e renderizadas em uma sessão multiplayer.

As demais raças deverão funcionar com recursos válidos ou permanecer claramente limitadas até receberem assets adequados.

---

# 6. TASK-203 — ANIMAÇÕES DE COMBATE

## Objetivo

Adicionar animações visuais coerentes com os eventos de combate.

## Funcionalidades

- Início do ataque.
- Execução do golpe.
- Impacto.
- Recebimento de dano.
- Recuperação.
- Morte.
- Retorno à animação Idle/Walk.

## Multiplayer

O servidor mantém autoridade sobre:

- Acerto.
- Dano.
- Cooldown.
- Vida.
- Morte.

A animação visual não determina, sozinha, se o ataque acertou.

Os clientes deverão apresentar os mesmos eventos essenciais.

## Regras

- Não duplicar dano por evento repetido.
- Não criar ataques adicionais por repetição de frames.
- Não permitir animações conflitantes.
- Não impedir movimentação indevidamente.
- Não modificar a hitbox física sem aprovação.

## Critérios de pronto

Combate com inimigo compartilhado e feedback coerente em ambos os clientes.

---

# 7. TASK-204 — INTERFACE DE COMBATE

## Objetivo

Melhorar a apresentação do combate.

## Funcionalidades

- Barra de HP.
- Barra de stamina/energia, quando prevista no sistema.
- Dano flutuante.
- Indicador de alvo.
- Feedback de hit.
- Feedback de morte.
- Cooldowns.
- Estados de jogador.

## Diretrizes

Interface compatível com Swing/Java2D.

Não bloquear a thread de renderização.

Não criar variáveis fictícias de gameplay apenas para preencher a interface.

Dados autoritativos deverão vir dos sistemas existentes.

## Critérios de pronto

Informações corretas e legíveis durante PvE com dois jogadores.

---

# 8. TASK-205 — INTELIGÊNCIA DOS INIMIGOS

## Objetivo

Evoluir o comportamento dos inimigos sem comprometer a simulação compartilhada.

## Estados sugeridos

- IDLE.
- PATROL.
- ALERT.
- CHASE.
- ATTACK.
- RETURN.
- DEAD.
- RESPAWN.

Adaptar essa proposta ao motor existente.

## Funcionalidades

- Detectar jogadores.
- Selecionar alvo.
- Perseguir.
- Atacar dentro do alcance.
- Respeitar obstáculos.
- Abandonar perseguição excessiva.
- Retornar à origem.
- Compartilhar estado entre clientes.
- Reagir à morte do alvo.

## Segurança de simulação

A inteligência deve executar no servidor.

Não permitir que cada cliente simule decisões diferentes do mesmo inimigo.

## Critérios de pronto

Ambos os jogadores observam o mesmo comportamento de perseguição, ataque e retorno.

---

# 9. TASK-206 — SISTEMA DE LOOT E DROPS

## Objetivo

Adicionar recompensas jogáveis para inimigos derrotados.

## Funcionalidades

- Tabelas de loot.
- Probabilidades validadas.
- Quantidades.
- Itens permitidos.
- Recompensas compartilhadas ou individuais conforme regra explícita.
- Coleta.
- Notificação.
- Histórico de eventos.

## Regras

- Loot decidido pelo servidor.
- Não gerar duas recompensas para a mesma morte.
- Não duplicar itens em retransmissões.
- Não permitir coleta de loot inexistente.
- Não atribuir recompensas a participantes inválidos.

Reutilizar os catálogos atuais de itens e recompensas.

Não implementar economia persistente global nesta tarefa.

## Critérios de pronto

Dois jogadores derrotam um inimigo e recebem ou coletam recompensas segundo regras consistentes, sem duplicação.

---

# 10. TASK-207 — INVENTÁRIO MULTIPLAYER

## Objetivo

Integrar a experiência de inventário e equipamentos ao modo multiplayer.

## Funcionalidades

- Abrir inventário.
- Visualizar itens.
- Equipar.
- Desequipar.
- Consumir itens.
- Visualizar quantidades.
- Atualizar aparência quando houver recurso compatível.
- Sincronizar resultados relevantes.

## Regras

- Servidor valida operações.
- Quantidades nunca negativas.
- Não equipar item inexistente.
- Não consumir o mesmo item duas vezes.
- Verificar slots e categorias.
- Evitar duplicação em operações repetidas.

Reutilizar InventoryService, equipamentos e modelos reais encontrados na auditoria.

## Critérios de pronto

Inventário funcional em uma sessão de dois jogadores, respeitando isolamento entre personagens.

---

# 11. TASK-208 — PROGRESSÃO E NÍVEIS

## Objetivo

Expandir e conectar os sistemas existentes de progressão à experiência multiplayer.

## Funcionalidades

- Experiência recebida.
- Nível atual.
- Próximo nível.
- Ganho de nível.
- Atributos existentes.
- Recompensas de progressão.
- Condições de desbloqueio.

## Regras

- Servidor autoritativo.
- Sem XP duplicado.
- Sem level-up repetido por replay.
- Não permitir XP arbitrário enviado pelo cliente.
- Respeitar domínio e raça quando aplicável.

Não migrar saves pessoais ou integrar a persistência sintética à conta do jogador.

## Critérios de pronto

Ganho de XP e progressão visíveis e consistentes em uma partida experimental.

---

# 12. TASK-209 — HABILIDADES E DOMÍNIOS

## Objetivo

Evoluir habilidades utilizáveis por jogadores de diferentes raças e domínios.

## Auditoria

Identificar habilidades e mecânicas atuais, incluindo:

- Shunpo.
- Cooldowns.
- Domínios.
- Requisitos de nível.
- Recursos de habilidade.
- Alcances.
- Dano.
- Estados de combate.

## Funcionalidades

- Seleção de habilidade.
- Hotbar.
- Uso.
- Cooldown.
- Custos.
- Feedback.
- Validação pelo servidor.
- Efeitos visuais compatíveis.

Não criar habilidades específicas que dependam de sprites ou efeitos inexistentes sem registrar seus recursos pendentes.

## Critérios de pronto

Uso consistente de habilidades por dois jogadores, com cooldown e autoridade do servidor.

---

# 13. TASK-210 — MISSÕES MULTIPLAYER

## Objetivo

Integrar missões à experiência jogável online.

## Funcionalidades

- NPC oferecendo missão.
- Aceitação.
- Objetivo.
- Acompanhamento.
- Progresso.
- Conclusão.
- Recompensa.
- Feedback visual.

## Tipos iniciais

Priorizar tipos já suportados pelo motor:

- Derrotar inimigos.
- Interagir com NPCs.
- Coletar recursos.
- Visitar região.

Não presumir que todas as missões autoradas no editor já sejam executáveis no multiplayer.

## Regras

- Progresso validado pelo servidor.
- Recompensa única.
- Eventos repetidos não duplicam avanço.
- Missões individuais e compartilhadas precisam de regras explícitas.

## Critérios de pronto

Uma missão completa pode ser iniciada, acompanhada e concluída corretamente dentro de uma sessão multiplayer.

---

# 14. TASK-211 — EXPANSÃO DO MUNDO

## Objetivo

Transformar as regiões existentes em espaços mais interessantes para exploração.

## Recursos

- Novas áreas compatíveis.
- Novos obstáculos.
- Pontos de interesse.
- NPCs.
- Inimigos.
- Portais.
- Zonas de combate.
- Elementos decorativos.
- Melhoria de ambientação.

## World Studio

Utilizar a infraestrutura de mapas existente.

Não adicionar regiões acima do limite aceito pelo runtime sem uma proposta arquitetural própria.

Não presumir suporte a mapas infinitos ou streaming de mundo.

## Critérios de pronto

Exploração funcional com dois jogadores, transições corretas e colisões validadas.

---

# 15. TASK-212 — INTEGRAÇÃO FINAL DO CORUJÃO VII

## Objetivo

Integrar as funcionalidades aprovadas das TASKs 201–211 e realizar homologação técnica.

## Testes

- Criação de personagem.
- Seleção de raça.
- Aparência.
- Animações.
- Multiplayer.
- Combate.
- Inimigos.
- Drops.
- Inventário.
- Progressão.
- Habilidades.
- Missões.
- Portais.
- Exploração.
- Reconexão.
- Publicação de mapas.
- Colisões.
- Launcher.
- Atualizador, quando aplicável.
- Regressões offline e Coop.

## Empacotamento

Gerar dois pacotes privados novos, quando a qualidade permitir:

- Anfitrião.
- Convidado.

Preservar distribuições anteriores.

Utilizar manifests e hashes.

Não publicar automaticamente no GitHub.

## Entrega

Criar relatório de fechamento do Corujão VII com resultados comprovados, funcionalidades disponíveis, limitações, bugs e recomendações.

---

# 16. GATES DE QUALIDADE

## GATE A — TASK-201/202

Criação, seleção e renderização de personagem sem regressão multiplayer.

## GATE B — TASK-203/204/205

Combate, interface e inteligência dos inimigos integrados à autoridade do servidor.

## GATE C — TASK-206/207/208/209

Loot, inventário, progressão e habilidades sem duplicação de estado.

## GATE D — TASK-210/211

Missões e exploração integradas ao gameplay existente.

## GATE E — TASK-212

Regressões, pacote privado, documentação e evidências.

Falhas críticas em identidade, estado do inventário, duplicação de recompensas, perda de dados, rollback ou autoridade do servidor bloqueiam as tarefas dependentes.

---

# 17. DOCUMENTAÇÃO E CHECKPOINTS

Cada tarefa deverá gerar:

- `TASK-XXX.md`
- `TASK-XXX-TESTES.md`
- `TASK-XXX-RELATORIO-FINAL.md`

Checkpoints em:

`.task-checkpoints/TASK-XXX/corujao-vii/`

Produzir também:

- `CORUJAO-VII-PLANO.md`
- `CORUJAO-VII-PROGRESSO.md`
- `CORUJAO-VII-PENDENCIAS.md`
- `CORUJAO-VII-RELATORIO-FINAL.md`, ao encerrar.

## Arquivo de continuidade

Manter:

`CONTINUIDADE-SPIRIT-GATE.md`

Atualizar após cada TASK com:

- Última tarefa validada.
- Tarefa atual.
- Branch/HEAD, quando disponíveis.
- Arquivos modificados.
- Testes executados.
- Pendências.
- Próximo passo.

Se o ambiente de execução não tiver acesso a Git ou ao sistema operacional do usuário, registrar a limitação. Não inventar verificações.

---

# 18. TRABALHO PELO CHATGPT WEB

O desenvolvimento pode ocorrer diretamente em uma conversa do ChatGPT.

Nesse caso:

1. Trabalhar sobre arquivos reais anexados.
2. Inspecionar o código antes de modificar.
3. Preservar estrutura de pacotes.
4. Produzir arquivos Java completos ou patches.
5. Executar testes quando o ambiente suportar.
6. Não declarar testes locais do usuário como executados pelo ChatGPT.
7. Entregar modificações para aplicação no VSCode.
8. Registrar quais arquivos foram efetivamente examinados.
9. Usar os relatórios anteriores para contextualização, não como substitutos do código atualizado.
10. Solicitar somente os arquivos ou recursos adicionais indispensáveis.

Se o ZIP não puder ser lido, não inventar nomes, métodos ou assinaturas do código.

Registrar o impedimento e utilizar apenas fontes efetivamente acessíveis.

---

# 19. RESTRIÇÕES GERAIS

Não executar sem autorização específica:

- Git push.
- Publicação de release.
- Troca de branch.
- Reset/clean.
- Migração destrutiva.
- Exclusão de saves pessoais.
- Integração da persistência sintética às contas reais.
- Instalação de novas dependências.
- Abertura de endpoints administrativos remotos.
- Reescrita geral do Game Server.
- Alteração incompatível de protocolo.

A homologação anterior foi informada como concluída pelo responsável.

Registrar isso como aceite humano do baseline anterior, sem apresentar como teste novo deste Corujão.

As novas funcionalidades exigem suas próprias validações.

---

# 20. PRIMEIRA AÇÃO

**Iniciar exclusivamente a TASK-201.**

Primeiro auditar:

- RpgLauncher.
- RpgNetworkClientLauncher.
- Protocol.
- RpgSessionWorld.
- LocalSimulationServer.
- Modelos de jogador.
- Modelos de raça.
- NetworkPlayerVisuals.
- Seleção de atlas.
- Representação de aparência.
- Dados de sessão.

Confirmar nomes e caminhos no código real.

Depois apresentar:

1. Arquitetura atual.
2. Lacunas para seleção de personagem.
3. Proposta mínima de implementação.
4. Arquivos afetados.
5. Riscos de compatibilidade.
6. Testes planejados.

Implementar a TASK-201 somente após a auditoria e aprovação das mudanças arquiteturais necessárias.

Não iniciar automaticamente a TASK-202.

**MISSÃO FINAL DO CORUJÃO VII: evoluir o Spirit Gate de uma infraestrutura multiplayer funcional para uma experiência RPG 2D mais completa, preservando estabilidade, integridade e autoridade do servidor.**

## Adendo de execução aprovado — 27/10/2026

Este adendo registra a decisão posterior do usuário. O texto do pedido
original acima permanece preservado.

### Autorização

Após receber a auditoria e a proposta, o usuário aprovou explicitamente
implementar somente a TASK-201. A aprovação abrangeu a aplicação negociada 5,
o frame de criação antes da admissão e a apresentação Human provisória
identificada para Aetheri/Spirit, com Reaper sem acessórios incompatíveis.

O usuário também acrescentou o ajuste das janelas: a interface do launcher
fecha após confirmar que o processo do jogo iniciou; a conexão Radmin fica
em um modal; ao entrar na partida, as telas de navegação são descartadas.
O servidor do anfitrião permanece vivo na transferência e na reconexão,
encerrando com a janela final da partida.

### Estado da execução

A seleção de sessão, o ingresso autoritativo, a reconexão, o compartilhamento
da apresentação e o fluxo de janelas foram implementados nos fontes locais.
O fechamento local de 28/10/2026 registra fontes congeladas e validação das
quatro correções finais: apresentação compartilhada, conclusão do worker
durante cancelamento Radmin, reconexão durante confirmação de encerramento
sem janela ou erro tardio e falha ao gravar o ACK após iniciar Java. Nesse
último ramo, somente o processo novo é encerrado e o lock permanece até
confirmar a saída. As rodadas estão em [TASK-201-TESTES.md](TASK-201-TESTES.md).

Java completo: 671 executados, 651 aprovados em headless, 20 abortados por GUI,
zero falhas/skips; 243 fontes e 209 arquivos de testes. O consolidado gráfico
aprovou 28 execuções, cobrindo os 20 abortos e repetindo oito casos. Python:
97 aprovados; regressões PowerShell: 39 verificações do atualizador e 21 do
transporte GitHub aprovadas. Assets, dados pessoais e distribuição foram
preservados. O próximo passo é a revisão local do responsável.

Os bytes históricos V1–V4 foram preservados no caminho legado. O novo cliente
não ignora a seleção quando encontra servidor sem a capacidade necessária.
Não foram introduzidas contas reais, persistência pessoal online, arte nova
ou alterações da hitbox.

### Distribuição e próximos limites

A distribuição oficial `dist/SpiritGate-GitHub-0.201.0` não foi substituída
e não contém a seleção ou o fluxo WinForms novo. Nenhum preview ou pacote
novo foi gerado nesta TASK e não houve commit, push, release ou instalação
de dependências. Para testar os fontes atuais, usar `rodar-prototipo.cmd` e
as opções Radmin do Portal de entrada.

A TASK-202 não foi iniciada e exige nova instrução. O Gate A também depende
dela; o Corujão VII não está encerrado. A entrega privada do ciclo continua
prevista para a TASK-212, quando autorizada e com qualidade suficiente.
Não houve nova homologação entre dois computadores ou Radmin físico.

Continuidade: [CONTINUIDADE-SPIRIT-GATE.md](../CONTINUIDADE-SPIRIT-GATE.md).
