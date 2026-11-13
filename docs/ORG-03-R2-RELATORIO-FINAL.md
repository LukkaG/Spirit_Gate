# ORG-03-R2 — Relatório final

## Resultado

Concluída a investigação e a captura **parcial permitida em laboratório**,
com 21 testes novos e 270 dirigidos aprovados. Suíte geral headless: 1079
executados aprovados, 57 excluídos, zero falhas/erros, 1136 reportados em 275 classes.
**Não existe captura integral pronta para recuperação ou save seguro.**

Todos os candidatos efetivamente exportados permanecem incompletos. O validador
R1 continua rejeitando os blocos ausentes e mantendo pendências de autorização,
mundo, confirmação durável e clock. readyForActivation não foi alterado.

## Adaptações realmente implementadas e testadas

Somente dois arquivos novos em server/src/test/java/com/projetok/rpg:

- CharacterSnapshotCapture.java: operação de leitura em owner explícito de
  laboratório, cópias imutáveis, validação R1 e diagnóstico de lacunas.
- CharacterSnapshotCaptureTest.java: fixture real descartável de RpgWorld,
  SharedRpgEncounter/engine/catálogos e 21 casos. Sem integração ao servidor.

Lidos/coerentemente copiados: XP, domínio/meias unidades/reset, 20 slots com
índices/quantidades, equipamento separado e moedas, região/posição/tick.
Observados sem completar blocos: UUID/nome/raça/aparência, HP/energia absoluta,
AGI/Energy efetivos, máximos, contadores inteiros básicos e resumos autorados.

Prêmio usado no teste veio de morte real do slime de 40 HP do catálogo, com 5 moedas
e 3 XP, sem reduzir HP de produção, alterar limite ou atribuir prêmio diretamente.
A cópia não reequipa, soma itens ou executa missão. O teste de fase usa cast real
de Quatro relâmpagos e confirma intangibilidade ainda ativa no mundo após ler,
sem representá-la como execução recuperável.

## Campos indisponíveis e políticas pendentes

Não capturados: atributos brutos/pontos/checkpoint/modo, fração de regeneração,
deadlines precisos e presença/binding do executor, ledger de objetivos builtin/
autorados, recibos completos, watermarks, direitos de loot com referência e
restrições com origem/prazo/política. Cosméticos e metadados integrais de admissão
também não vêm de RW isolado. Esses blocos são nulos, não zero/lista vazia.

manualAttributes pode conceder pontos lazy; save escreve flags e snapshot chama
save. Nenhum é chamado pelo capturador. Views de atributos/energia do backend
também chegam ao caminho lazy. state(id) do engine não distingue prontidão de
sessão ausente; não é usado para completar cooldowns nem lido após remove.

CharacterId, hash/regras, época/revisão e horário do protótipo são explicitamente
sintéticos e fornecidos pelo controlador. O UUID observado continua separado.
Não há conta, dono por nome/IP, geração durável inventada ou handshake novo.

Recibos completos **não existem como simples getter**: flags antigas não provam
concessão confirmada, revisão econômica ou durabilidade. Não foram convertidas
em recibos. Política de legado, retenção, relógio entre reinícios, penalidades
adversas e futuras fontes de autoridade continuam pendentes.

## APIs mínimas propostas e limite de autorização

A proposta está em [lacunas do runtime](ORG-03-R2-LACUNAS-DO-RUNTIME.md):
read model puro de RW, export de timing com presença/binding em RE, export de
ledger de missões e agregação coordenada por participação/revisão no owner.
Nenhuma foi implementada. Qualquer edição desses componentes requer autorização
específica sobre essa proposta antes de começar.

Mesmo com acessores, identidade/revisão confiáveis e recibos/economia precisam
de contratos próprios. Não se solicita integração automática, instalação,
PostgreSQL ou alteração A-04 como consequência do encerramento desta entrega.

## Consistência e limites das evidências

A fixture executa preparo, comandos, captura e verificação em org03-r2-owner.
Não há tick, I/O, callbacks de mutação ou writers paralelos durante capture.
Guarda thread, identidade/região/contexto e NetworkState antes/depois; não é
atomicidade de coop-world, detecção de ABA nem proteção contra todos os writers
externos no mesmo tick. Source.present é da fixture, não estado autenticado do peer.

Comprovadas cópias imutáveis após mudança posterior da bolsa, consultas repetidas
sem alterar valores observáveis/estado inicializado, morte sem respawn e ROOT
sem limpeza. Atributos completos, fração, deadlines, objetivos e recibos não
receberam alegação de precisão/captura que as APIs não permitem demonstrar.

Testes que precisam dessas APIs e corridas reais de revisão/remoção continuam
pendentes. Resultado COMPLETE_CANDIDATE e política isolada são estados reservados
do contrato futuro; não foram produzidos ou declarados testados pelo protótipo.

## Testes, falha intermediária e preservação

Primeira rodada: 68 casos, 20 erros de preparo da fixture, nenhum caso R1 falhou.
interact priorizou portal e afastou o personagem do NPC; foi usado o helper
existente interactSyntheticNpc, preservando asserções. Rodada corrigida: 68 verdes.
Final dirigida: 270 verdes. Geral: 1079 executados verdes, 57 excluídos em headless.
O caso de retirada ganhou verificação de recarga ativa antes de remover o engine;
passou isolado e a suíte geral foi repetida integralmente, com os mesmos totais.
Detalhes/20 requisitos: [testes](ORG-03-R2-TESTES.md).
Falhas intermediárias e XMLs foram mantidos, não classificados como defeitos de gameplay.

2351 arquivos anteriores inventariados e 11 copiados no checkpoint; comparação
final em temp/org03-r2/validation.json. Nenhuma classe Java anterior, incluindo
R1/RpgWorld/RpgSessionWorld/servidor/engine, foi alterada; nenhum fonte novo de
produção. Somente dois arquivos de teste novos, cinco docs e adendo ao principal.
Dois TXT de evidência regenerados por testes antigos foram arquivados/restaurados.

Preservados 40 skills/combate/balanceamento, protocolos 6/7, inventário/progressão/
missões/loot, mobs/IA/colisões, HUD/hotkeys, offline/Coop, World Studio, launcher,
saves e banco. Sem commit, push, release, instalação ou publicação.
ORG-04/TASK-226 não iniciadas. A-04 permanece sem timeout definitivo ou mudança
de imunidade; laboratório R3 não foi editado. Sem homologação de frontend/Radmin.

## Entregáveis, reversão e próximo passo

[Escopo](ORG-03-R2.md), [testes](ORG-03-R2-TESTES.md),
[matriz completa](ORG-03-R2-MATRIZ-EXTRACAO.md),
[lacunas/proposta](ORG-03-R2-LACUNAS-DO-RUNTIME.md), este relatório e referência
no principal de organização. Os sete critérios do gate R2 foram atendidos no
escopo permitido de investigação/captura parcial, sem afirmar captura integral.

Reversão: remover somente os dois testes novos, cinco docs e adendo R2,
preservando R1 e todo trabalho anterior. Principal anterior copiado em
temp/org03-r2/ORGANIZACAO-before.md; sem reset/clean global.

Próximo passo recomendado, dependente de autorização própria: revisar/aprovar
as APIs read-only mínimas e a política para informação econômica ausente.
Não iniciar importação antes da leitura completa, autoridade e testes de
consistência reais definidos no gate do documento de lacunas.
