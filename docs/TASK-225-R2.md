# TASK-225-R2 — Validação de encerramento seguro em laboratório

## Contexto e motivação

Autorização recebida em 03/11/2026: validar a alternativa A da ADR A-04 apenas em laboratório isolado. A R1 foi aprovada tecnicamente, com baseline de 999 casos aprovados. A falta de apoio ainda pode manter Quatro relâmpagos intangível indefinidamente.

## Estado atual verificado

Branch `beco`, HEAD `6d72c6595dee6dbe4dea832fe6047ee81f51d4a3`, checkout com numerosas alterações preexistentes. Monitor consultado: sem pendências declaradas. JDK 17.0.20.1, Maven 3.10.0, módulo `server/pom.xml`, cache offline existente. Não houve descarte de trabalho local.

`LightningField.advance` resolve quatro cortes uma vez e tenta reaparecer a partir do tick relativo 32, sem prazo final. `RpgSessionWorld.leave` já cancela skills, retira a sessão e sai do encontro. `SessionSimulation.disconnectRequested` já existe; o transporte o consulta antes dos snapshots privados e fecha o peer. A readmissão cria personagem efêmero novo.

## Objetivo

Demonstrar as bordas 31/32/91/92/93 em modelo isolado e validar a fronteira de retirada com servidor real e dois clientes TCP locais, sem ativar timeout no runtime.

## Escopo incluído

- Modelo candidato de prazo no classpath de teste, usando engine, mundo e encontro reais.
- Adaptador exclusivo de teste envolvendo `RpgSessionWorld` e a interface existente `SessionSimulation`.
- Saída coordenada, saída espontânea, duas aplicações observadoras (6 e 7), isolamento, fila real, snapshots, efeitos e readmissão com spawn recusado/aceito.
- Plano de integração e decisão explícita sobre perda de progresso/cooldowns e feedback.

## Fora do escopo

Ativação em produção, TASK-226, ACK, trajetória, alterações das 40 skills, protocolos, valores, colisões de produção, saves, inventário, World Studio, launcher, offline/Coop, instalação ou distribuição.

## Arquivos/módulos envolvidos (confirmados)

Novos testes em `server/src/test/java/com/projetok/rpg/Task225R2DeadlineLabTest.java` e `server/src/test/java/com/projetok/multiplayer/Task225R2ShutdownTcpLabTest.java`. Quatro documentos R2 em `docs/` e atualização de `CORUJAO-IX-PROGRESSO.md`/`CORUJAO-IX-PENDENCIAS.md`. Evidências locais em `.task-checkpoints/CORUJAO-IX/TASK-225-R2/`.

Referências de produção somente inspecionadas: `RpgSkillEngine`, `RpgWorld`, `SharedRpgEncounter`, `RpgSessionWorld`, `SessionSimulation`, `LocalSimulationServer`, `LocalClient` e `NetworkSkillPresentation`.

## Pré-requisitos e dependências

Preservar baseline R1, cache Maven, limite de dois participantes e protocolos atuais. Nenhum banco ou serviço externo é necessário. Não publicar revisões: as cenas de laboratório são construídas em memória antes de iniciar o servidor loopback na porta efêmera.

## Plano de implementação incremental

1. Preservar Git, hashes e cópias do estado recebido.
2. Exercitar o prazo como coordenador candidato externo ao engine.
3. Acionar retirada equivalente durante uma fase real pelo adaptador de teste, entre `step` e os snapshots.
4. Repetir casos focalizados e regressão completa, incluindo complemento gráfico.
5. Arquivar resultados, restaurar saídas históricas regeneradas e entregar plano sem patch de runtime.

## Critérios de aceite

- Última tentativa de materialização precede retirada candidata no tick 92; apoio nos ticks 91/92 evita encerramento.
- Tick 93 e pedidos duplicados não repetem retirada/dano; identidade/região/revisão/execução divergentes são recusadas no modelo.
- Uma retirada lógica por sessão; callback posterior de transporte é idempotente.
- Sem snapshots privados para sessão retirada, corpo fantasma, comando antigo aceito ou limpeza da outra execução.
- Readmissão revalida spawn e identidade e demonstra domínio/recargas iniciais, sem alegar restauração de progresso.
- Runtime ainda espera além de 92 quando o modelo candidato está desabilitado.

## Plano de testes

Ver [TASK-225-R2-TESTES.md](TASK-225-R2-TESTES.md). Todos os casos novos têm timeout JUnit e esperas finitas; cada execução Maven tem limite externo de 600 segundos. Nenhum terceiro cliente simultâneo.

## Riscos e compatibilidade

O modelo de prazo e o ensaio TCP são provas complementares: o segundo injeta uma causa equivalente durante a fase, não bloqueia uma partida real até o tick 92. Reflexão/injeção estão restritas aos novos testes; não existe endpoint, flag de gameplay ou alteração de código principal. A integração definitiva requer identidade de execução ativa, tombstone de retirada e validações próprias.

## Plano de rollback

Remover apenas os dois testes R2 e os documentos R2; reverter somente os acréscimos R2 nos dois documentos do Corujão a partir do baseline local, após conferir trabalho posterior. Evidências são preserváveis. Não há wire, save ou banco para migrar. Não usar reset/clean/restauração global.

## Entregáveis e relatório final

[Relatório](TASK-225-R2-RELATORIO-FINAL.md), [testes](TASK-225-R2-TESTES.md) e [plano de integração](TASK-225-R2-PLANO-INTEGRACAO-A04.md). A-04 permanece aberto até decisão e implementação autorizadas e validadas.
