# TASK-225-R1 — Saneamento de regressões e segurança de materialização

Referência: 03/11/2026, branch `beco`, HEAD `6d72c6595dee6dbe4dea832fe6047ee81f51d4a3`. Adendo exclusivo da TASK-225; TASK-226 não iniciada.

## Contexto e motivação

A TASK-225 entregou auditoria das 40 habilidades e correção de limpeza visual, com 45 casos novos aprovados, mas registrou duas falhas anteriores de XP/missão e a reprodução condicional do A-04. Esta ordem autoriza corrigir fixtures comprovadamente desatualizadas e aprofundar a investigação, sem mudar o gameplay de produção.

## Estado atual verificado

Checkout com alterações locais anteriores: 54 modificações, 2.429 exclusões e 335 entradas não rastreadas no Git inicial. JDK 17, Maven e cache existentes; build em `server/pom.xml`. Status consultado do monitor sem pendências/falhas listadas, com timestamp antigo; isso não demonstra processo ativo. Nenhum relatório novo de erro pendente foi identificado nessa lista.

Baseline em `.task-checkpoints/CORUJAO-IX/TASK-225-R1/`: 2.715 hashes protegidos, 557 fontes/testes, 186 arquivos de skills (152 PNGs), cópia de fontes/documentação, Git/diff e pedido integral. Comparação com os hashes dos entregáveis finais da TASK-225: **nenhuma diferença** no início desta R1. Frente ao início da TASK-225, as diferenças protegidas são exatamente o renderer e os dois documentos de progresso/pendências já entregues naquela etapa. O manifesto R1 também inclui PDFs que o manifesto anterior excluía; ausência no manifesto antigo não significa criação nesta R1. Checkpoints 223–225 preservados.

Reprodução inicial: seis casos (`Task208ProgressionTest`, `Task210QuestJourneyTest`, `EnemyDurabilityTest`), duas falhas e zero erros/skips. O piso de HP 10.000 é explícito em `RpgSessionWorld.enableRevisions` e exigido por `EnemyDurabilityTest`; permanece vigente. As fixtures tentam matar com no máximo seis ataques básicos, insuficientes para gerar a morte. O ledger de XP e o objetivo de missão dependem dessa morte real.

## Objetivo

Restaurar os testes de progressão/missão sem enfraquecer suas verificações, comprovar o fluxo real de combate/recompensa e produzir uma ADR verificável sobre o término seguro do A-04. Manter separado o aceite das regressões e o gate bloqueante da habilidade.

## Escopo incluído

- Ajustar exclusivamente fixtures de teste comprovadamente incompatíveis com a resistência atual.
- Verificar morte autoritativa, contribuição, XP único, objetivo ACTIVE → READY, entrega/recompensa única, rejeição de replay e isolamento dos jogadores.
- Investigar espera consecutiva e custo cumulativo da busca, liberação de apoio, ciclo de sessão/reconexão/região, perda de término e observadores.
- Comparar alternativas A/B/C na ADR, inclusive limites reais de transporte, readmissão e cooldown.
- Executar testes focalizados, suíte geral e complemento Swing; preservar evidências intermediárias e assets.

## Fora do escopo

Mudança em produção de HP, XP, missões, dano, imunidade, materialização, duração, política de saída/reconexão, protocolo ou capacidade de participantes. Sem implementar a proposta A-03, criar opcodes, inferir identidade por proximidade, mudar HUD/hotkeys, publicar conteúdo, instalar ferramentas/dependências, alterar banco/save, fazer commit/push ou iniciar TASK-226.

## Arquivos/módulos envolvidos (confirmados)

Inspecionados: `RpgSessionWorld`, `RpgWorld`, `SharedRpgEncounter`, `EnemyService`, `RpgSkillEngine`, `LocalSimulationServer`, `LocalClient`, `SkillProtocol`, `NetworkSkillPresentation`, `LauncherSession` e catálogos de inimigos/missões. Produção deve permanecer byte a byte igual ao baseline R1.

Testes existentes autorizados: `server/src/test/java/com/projetok/multiplayer/Task208ProgressionTest.java` e `Task210QuestJourneyTest.java`; helper e testes adicionais exclusivamente em `src/test`. `EnemyDurabilityTest` mantém sua finalidade e asserções. Inventário final em [TASK-225-R1-RELATORIO-FINAL.md](TASK-225-R1-RELATORIO-FINAL.md).

## Pré-requisitos e dependências

JDK/Maven/cache locais, sem instalação ou banco. Aplicações 6/7 existentes. Skill [spiritgate-server-engineer](../.agents/skills/spiritgate-server-engineer/SKILL.md) aplicada para autoridade, compatibilidade e evidências. A autorização do pedido permite ajustar fixtures; não autoriza implementar a alternativa de A-04.

## Plano de implementação incremental

1. Preservar baseline e reproduzir separadamente os dois caminhos de falha, mantendo a exigência de 10.000 HP no teste de resistência.
2. Tornar explícita a configuração dos testes de XP/missão: encontro real com HP oficial do catálogo, preparado em memória antes dos sockets, sem editar HP de produção nem aplicar XP/morte diretamente.
3. Executar ataques legítimos por TCP, verificar morte/geração/ledger e manter as verificações originais de XP, missão, recompensa e isolamento.
4. Acrescentar mundos descartáveis e candidatos isolados para investigar A-04, com limites externos e custo acumulado, sem incorporar a política candidata ao runtime.
5. Produzir ADR comparando A/B/C e registrar a decisão específica pendente.
6. Rodar regressões, comparar integridade, arquivar capturas regeneradas e restaurar somente evidências históricas sobrescritas pelos testes.

## Critérios de aceite

Testes de progressão/missão aprovados sem atribuir XP/READY/morte manualmente nem reduzir cobertura; resistência de produção preservada; fluxos de combate e reward reais; suíte Java/Swing com resultados e limitações explícitos; 152 PNGs íntegros; ADR completa e A-04 ainda identificado como gate separado. Suíte verde não equivale a homologação integral da TASK-225.

## Plano de testes

Comandos e resultados em [TASK-225-R1-TESTES.md](TASK-225-R1-TESTES.md). Maven offline com cache existente, erros isolados no checkpoint e limite externo de execução. Os testes do A-04 usam contagens finitas e mundos efêmeros. Testes TCP preservam o transporte real; dois renderers de observadores não serão descritos como três sockets simultâneos: servidor, sessão e encontro admitem somente dois participantes.

## Riscos e compatibilidade

As fixtures de catálogo exercitam XP/missão sem depender do piso de treino; não substituem a cobertura de durabilidade 10.000 nem homologam uma batalha completa contra esse alvo resistente. A-04 conserva a espera sem limite e divergência visual atual até decisão aprovada. Timeout de saída exige coordenação entre thread proprietária e transporte; remover apenas a skill não equivale a encerrar participação. Reconectar atualmente cria sessão efêmera nova, com consequências para recargas/progresso que precisam ser decididas.

## Plano de rollback

Reverter somente os trechos das duas fixtures modificadas, comparando com `baseline/server/src/test/` do checkpoint R1, e retirar os novos helpers/testes caso não haja referências posteriores. Restaurar apenas a seção de referência ao adendo e mudanças de progresso/pendências desta R1, preservando o relato original e os demais trabalhos locais. Nenhuma mudança wire/save/banco ou produção para reverter. Não usar reset/clean ou restauração global.

## Entregáveis e relatório final

Este plano, testes, relatório final, [ADR A-04 proposta](TASK-225-ADR-A04-PROPOSTA.md), medições e checkpoint R1; referência no relatório original da TASK-225 e atualização dos gates. A decisão sobre produção A-04 permanece com o responsável após avaliar a ADR.
