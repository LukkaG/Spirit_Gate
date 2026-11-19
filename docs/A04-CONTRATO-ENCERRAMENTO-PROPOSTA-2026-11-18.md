# A-04 — Contrato proposto de encerramento e continuidade

Data: 18/11/2026. Estado: **preparação concluída; proposta de comportamento para decisão; runtime não alterado nesta etapa**.

## Objetivo e escopo

Definir como terminar a fase de Quatro relâmpagos (`thunder_5`) quando não existir apoio físico válido, sem transformar uma falha de materialização em perda silenciosa de personagem, recarga gratuita ou duplicação de recompensas.

Esta entrega atende à próxima tarefa recomendada: comparar alternativas, definir um contrato candidato e seus critérios de validação. Não ativa prazo, recuperação, exclusão da skill ou alteração de protocolo. Não integra ORG-03, D33, os experimentos de persistência 193–196 nem saves pessoais. Não atribui novo número de TASK.

## Estado atual verificado

Checkout: branch `beco`, HEAD `c3d484e76f17c2d93bf0c68ad3ff54280ee8ad39`. Existem alterações locais anteriores, inclusive a correção de deadline de handshake em `LocalSimulationServer`; foram preservadas.

| Evidência atual | Consequência |
| --- | --- |
| `RpgSkillEngine.LightningField.advance()` espera a duração da skill, aplica os impactos sob guarda `resolved` e permanece pendente se `skillReappear()` falhar. | Os quatro cortes não se repetem; a espera não possui limite próprio. |
| `RpgWorld.skillReappear()` procura origem, anéis e grade global; exige posição livre e segmento permitido pelo cenário. | Um mapa sem apoio elegível não tem saída garantida por aumentar a busca. |
| `RpgWorld.receiveDamage()` ignora dano durante `skillIntangible()`. | A espera prolonga a imunidade autoritativa. |
| `RpgSkillEngine.remove()` apaga sessão, pendências e eventos; `state()` sem sessão retorna cinco recargas zero. | Capturar depois da remoção perde a informação das recargas. |
| `RpgSessionWorld.leave()` chama `skills.remove()` e retira a participação. | O fluxo comum de saída não constitui retenção de personagem. |
| A interface atual `SessionSimulation` não oferece importação completa nem prova de posse para recuperação. | A recuperação de laboratório não pode ser apresentada como recurso disponível no servidor. |

Referências históricas: [ADR original](TASK-225-ADR-A04-PROPOSTA.md), [plano R2](TASK-225-R2-PLANO-INTEGRACAO-A04.md) e [ADR de continuidade R3](TASK-225-R3-ADR-CONTINUIDADE.md). As letras das alternativas variam entre documentos; **A/B/C abaixo seguem a R3**, e não a tabela do ADR original.

### Revalidação desta etapa

Executado contra cópia temporária de fontes e assets, sem iniciar servidor público, cliente visual ou banco:

```powershell
python tools/server_quality/run_quality.py --mode test --offline --tests 'Task225MaterializationTest#freeSupportNearWallsBordersAndObstaclesMaterializesNormally+allyOnTheOriginMaterializesCasterAtAnotherValidSupport+noArtificialSupportKeepsImmunityBeyondVisualFuseButReentersWhenSpaceReturns+lifecycleCleanupReleasesOldWorldAndDoesNotTransferPhase'
```

Resultado: **11 execuções JUnit, zero falhas, erros ou ignorados**. Incluem apoio normal em obstáculos/bordas, aliado na origem, ausência artificial de apoio por 180 ticks e limpeza em cinco cenários de ciclo de vida. O método de medição que escreve relatório histórico não foi selecionado.

No cenário bloqueado: quatro impactos únicos, dano ignorado, ausência de evento terminal e fase ainda ativa aos 180 ticks; após liberação artificial de apoio, materialização no tick seguinte e dano novamente efetivo. Isso confirma o defeito atual, não uma correção. Não demonstra exploração no mapa distribuído, recuperação por TCP ou homologação física/Radmin.

Resumo, XML e log desta execução estão arquivados em `.task-checkpoints/A04-CONTRATO-20261118/`. A origem temporária foi `C:/Users/lucas/AppData/Local/Temp/spiritgate-quality-xzy_xn8d`.

## Alternativas e consequência para o jogador

| Alternativa | Experiência do jogador | Benefício | Limite e decisão |
| --- | --- | --- | --- |
| A — retirar e usar a criação comum | A conexão termina; entrar outra vez cria personagem novo, com outro estado e recargas reiniciadas. | Menor implementação inicial. | Perde continuidade e permite vantagem por reset. Não recomendada como correção transparente. |
| B — retirar, reter e recuperar com posse comprovada | O jogador sai da participação bloqueada; recebe estado claro de recuperação pendente; retorna com o mesmo progresso e prazos, somente quando houver admissão segura. | Separa fim da participação de perda do personagem. | Recomendada como solução definitiva; exige mecanismos ainda ausentes. Retorno imediato não é garantido. |
| C — manter a conexão e achar outro apoio/reservar posição | O personagem reaparece na mesma participação, se houver posição permitida. | Evita nova conexão no caso favorável. | Uma posição antes livre pode estar ocupada agora; reserva altera colisão; teleporte pode alterar região/regras. Não garante saída em todo mundo bloqueado. |
| Mitigação — indisponibilizar temporariamente `thunder_5` na alpha | A técnica fica indisponível com feedback coerente no servidor e cliente. | Evita novas ocorrências por essa técnica enquanto a solução é construída. | Altera disponibilidade e balanceamento; não é correção definitiva e não foi aplicada. Precisa de decisão específica. |

Manter a espera atual conserva o comportamento existente, mas mantém o bloqueio A-04. Remover apenas a imunidade sobre um corpo sem apoio também não resolve a materialização e pode produzir dano/morte em condição física inválida.

Não é possível prometer simultaneamente materialização em prazo finito, nenhuma sobreposição, nenhuma mudança de posição/regra e permanência no mundo quando não existe apoio válido. É necessário admitir uma saída controlada ou aprovar uma regra física diferente.

## Recomendação para decisão

Adotar **B como direção de implementação**, condicionada aos gates abaixo. Preservar a skill e a busca atuais até existir solução integral ou mitigação escolhida explicitamente. Os **92 ticks permanecem um candidato de ensaio**, composto por 32 ticks normais mais 60 de tolerância; não são configuração homologada nem limite de tempo de parede.

Separar duas decisões de produto:

1. **Consequência:** aceitar retirada temporária com progresso retido e retorno condicionado a apoio seguro, sem reset de personagem, HP, energia ou recargas.
2. **Garantia de retenção:** recuperação somente durante o mesmo processo, com limitação explícita diante de crash, ou recuperação durável após reinício. A segunda exige projeto próprio de persistência e autenticação; não pode ser inferida do protótipo R3.

Uma retenção só em memória pode servir a um ensaio privado deliberadamente limitado. Não atende à promessa de continuidade após queda do servidor. Nenhuma dessas decisões está presumida como aprovada por esta documentação.

## Contrato candidato da alternativa B

### Invariantes

- O servidor é a autoridade sobre identidade, estado, posição, recompensas e relógios; o cliente não fornece um snapshot confiável.
- Uma execução aplica seus quatro cortes uma única vez. A falha de saída não reaplica dano nem recompensa.
- Uma identidade possui no máximo uma participação ativa; socket, ID de peer e geração antigos nunca reassumem posse.
- A retirada não cura, revive, concede energia, renova buffs, limpa penalidades gratuitamente ou zera recargas.
- Um apoio bloqueado recusa readmissão; não autoriza sobreposição, teleporte arbitrário ou criação substituta silenciosa.
- Recursos e recibos de recompensa são preservados juntos. Recuperação não reexecuta mortes, entregas ou concessões.
- Falha ou expiração da autorização de recuperação não significa apagar automaticamente o estado retido.

### Transições e ordem

Estados propostos, ainda sem novas classes ou mensagens no runtime:

1. **Fase em execução:** preserva duração normal, impactos e regra geométrica existentes.
2. **Aguardando apoio:** tenta materializar; se conseguir, limpa flags e emite término normalmente. No último tick candidato, a tentativa de apoio precede a decisão de retirada.
3. **Preparando retenção:** o owner revalida personagem, execução, geração, região e revisão; captura estado completo e prazos **antes** de qualquer `remove()`/`leave()`. Novos comandos dessa participação não podem alterar a captura durante o commit.
4. **Retido e retirado:** somente após confirmação de retenção, cancela transitórios, remove a participação e solicita fechamento coordenado do peer. Mantém o pedido até o transporte consumi-lo; não produz snapshots privados de sessão já removida. O observador continua conectado.
5. **Recuperação pendente:** prova de posse, contexto ou apoio inválidos mantêm retenção sem consumo indevido. Mostrar motivo seguro e possibilidade de nova tentativa; não informar recuperação concluída.
6. **Readmitido:** valida identidade, revisão, época, região, capacidade e apoio no owner; instala estado e prazos por APIs explícitas; consome a operação uma vez; entrega nova geração e confirmação coerente. Retry de resposta perdida consulta recibo da mesma operação, sem nova concessão.

Morte, troca de revisão/região, substituição, desconexão espontânea e pedidos duplicados devem disputar a mesma decisão de ciclo de vida. Um pedido antigo não pode cancelar uma participação nova.

### Estado obrigatório

Inventário inicial para orientar levantamento completo, não schema pronto:

| Grupo | Regra candidata |
| --- | --- |
| Identidade, raça, domínio e aparência | Mesma identidade de personagem; novo peer/geração. Nome/IP/UUID público não comprovam posse. |
| XP, progressão, atributos e pontos | Captura exata; não reconceder pontos por derivar nível na readmissão. |
| HP, energia e regeneração | Preservar valores autoritativos e temporizações relevantes; não reconstruir apenas pela ficha normalizada do cliente. |
| Itens, equipamento e moedas | Restaurar o conjunto completo; nunca somar snapshot sobre criação nova. |
| Missões builtin/autoradas e recompensas | Preservar objetivos, conclusões e recibos junto aos recursos; definir contribuições de combate que resolvam após a retirada. |
| Skills e ações comuns | Deadlines autoritativos das cinco skills e demais ações cobradas; cobrir ataque, shunpo, poção, portal e temporizações precisas. Campo ausente recusa recuperação, não assume zero. |
| Efeitos com custo e penalidades | Inventariar e aprovar preservação por prazo ou compensação; não cancelar efeitos para oferecer vantagem. |
| Contexto | Versão do schema, revisão, região, época de relógio/mundo, geração de personagem e operação. |

Não restaurar fase bloqueada, filas de comandos, sockets, seleção de alvo, animações ou projéteis como se a execução antiga continuasse. O ledger global do encontro não pode ser substituído por snapshot individual.

Proposta de relógio durante ausência: conservar deadlines na mesma época monotônica; não pausá-los nem redefini-los. Reinício do processo exige conversão durável explicitamente projetada. Duração de retenção, expiração/rotação da autorização e política de arquivo de expirados ainda precisam de valores aprovados.

### Segurança e falhas

- A prova de recuperação deve ser opaca, vinculada à autoridade, personagem e geração, com consumo único, limites de tentativa, revogação e prazo. Não reutilizar grants locais dos laboratórios como autenticação de rede.
- Entrega de credencial exige canal protegido ou proteção equivalente aprovada. TCP simples não protege um bearer token contra observador da rede.
- Recuperação exige contrato negociado próprio; não inserir silenciosamente campos nas aplicações 6/7. Cliente incompatível não deve receber falso sucesso nem personagem novo como fallback implícito.
- Falha de captura, falta de memória, fila cheia ou falha de armazenamento **bloqueiam a ativação do prazo enquanto não houver contingência validada**. Não escolher entre perda de progresso e espera infinita dentro de um `catch`.
- Não fazer I/O externo sem limite no owner. Preparação, commit, resposta perdida e restart precisam de cenários de falha próprios.
- Definir orçamento de CPU da busca e limites de registros pendentes. Prazo em ticks não garante latência real se o owner estiver atrasado; ainda não existe medição representativa para fixar um valor de produção.

Feedback sugerido, condicionado ao contrato de transporte: “Não foi possível reaparecer com segurança. Sua participação foi encerrada; a recuperação do personagem está pendente.” Usar essa mensagem somente depois de retenção efetiva. Não prometer recuperação após crash quando o mecanismo for apenas em memória.

## Implementação incremental proposta

| Etapa | Arquivos/módulos confirmados para avaliação | Critério de pronto |
| --- | --- | --- |
| 1. Fechar decisão de produto | Este documento e ADR R3 | Consequência B, alcance da retenção, prazos e contingência definidos. |
| 2. Inventariar captura/instalação | `RpgWorld`, `RpgSkillEngine`, `QuestService`, `AuthoredQuestSession`, `SharedRpgEncounter` | Todos os campos e relógios classificados, sem inferir estado por DTO visual. Testes de ida/volta e recibos. |
| 3. Projetar posse e admissão | `SessionSimulation`, `RpgSessionWorld`, `LocalSimulationServer`, `GameplayProtocol`, `LocalClient` | Contrato separado e revisado; recuperação transacional com consumo único e testes de abuso. Mudanças de protocolo tratadas explicitamente. |
| 4. Integrar encerramento | `RpgSkillEngine.LightningField`, coordenador e transporte | Caminho real engine → retenção → retirada → fechamento validado; falhas não perdem estado nem duplicam participação. |
| 5. Validar e só então ativar | Testes A-04, rede e roteiro alpha | Matriz abaixo aprovada; ensaio físico separado. |

Essas etapas são plano futuro. A etapa 3 pode requerer novo escopo de autenticação; durabilidade pode requerer outro projeto. Não reutilizar automaticamente módulos de ORG-03 ou alterar os modos offline/Coop.

## Matriz de aceite para o patch futuro

| Cenário | Resultado obrigatório |
| --- | --- |
| Apoio normal antes/no tick 32 | Duração e quatro impactos atuais preservados; término único. |
| Apoio surge nos ticks 91/92, se esse prazo for aprovado | Última tentativa vence retirada; não perder participação recuperável. |
| Sem apoio até 93 e além | Participação física encerrada conforme política aprovada, estado retido, ausência de imunidade órfã e observador operante. |
| Morte, revisão, região ou substituição concorrente | Uma decisão por geração; nenhuma intervenção sobre personagem novo. |
| Peer lento, desconexão e comandos antigos | Owner e observador progridem; comandos obsoletos recusados; liberação de capacidade uma vez. |
| Captura/commit falham | Contingência explícita e testada; nenhum descarte de progresso apresentado como sucesso. |
| Prova incorreta, replay ou duas recuperações concorrentes | Recusa segura ou uma única recuperação válida, sem acesso a outro personagem. |
| Resposta perdida antes/depois do commit | Retry idempotente com recibo e reconciliação; nenhum clone ou crédito repetido. |
| XP, itens, energia, HP, domínio, atributos e missões | Comparação exata antes/depois; sem refill, reset ou reconcessão. |
| Recargas e efeitos durante ausência | Deadlines continuam segundo regra aprovada; não limpar penalidades gratuitamente. |
| Apoio/capacidade/contexto recusados na volta | Estado permanece retido; tentativa não consome autorização indevidamente. |
| Expiração, retenção esgotada e restart | Política definida, observável e sem promessa maior que a garantia implementada. |
| Dois clientes reais TCP, aplicações compatíveis/incompatíveis | Fluxo A-04 real, sem adaptador que injete retirada equivalente; feedback correto e observador estável. |
| Regressão e desempenho | 40 skills, colisão, recompensas e modos preservados; carga limitada e orçamento do owner medidos. |

O teste atual que comprova espera aos 180 ticks é controle do defeito. Após implementação deliberada, sua expectativa deve mudar para a política escolhida; manter controles dos quatro impactos e do apoio normal. Não conservar um teste exigindo o defeito como critério da correção.

## Entrega, pendências e reversão

Nesta etapa foi criado somente este documento e arquivada a evidência local dos 11 testes. Nenhum código de produção, teste existente, protocolo, save, configuração de skill ou conteúdo publicado foi alterado. Nenhuma ferramenta instalada, commit ou push realizado.

A-04 continua bloqueante. As decisões de consequência e garantia de retenção precedem a implementação; o protótipo R3 não satisfaz esses gates sozinho. A homologação em duas máquinas/Radmin e launcher nativo permanece pendente.

Na inspeção inicial, o monitor registrava lista vazia de pendências, mas seu timestamp era antigo. Os TXT locais de 08/11 contêm erros de operação do World Studio (seleção vazia e identidade/schema de mapa inválido); não foram tratados como incidentes A-04 nem como corrigidos nesta entrega. Atendimento do editor é escopo separado.

Reversão desta entrega: remover apenas este documento novo, se desejado, preservando alterações anteriores. Para o patch futuro, planejar rollback antes de ativar; não retornar a uma versão incapaz de ler registros retidos nem apagar autorizações/estado para facilitar downgrade.
