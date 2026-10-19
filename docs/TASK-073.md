# TASK-073 - Encontro cooperativo

Chefe compartilhado criado por MobFactory, nivel 14 da curva demon (302 HP), persegue/ataca com MovementService/CombatService e respeita Freeze. Dois jogadores podem causar dano no mesmo alvo; quem contribuiu dano positivo e permanece conectado recebe 20 moedas transitorias uma unica vez. Desconectar cancela habilidades da sessao e perde elegibilidade; reconectar cria identidade nova sem recompensa retroativa.

Nenhum inventario/save offline importado ou sincronizado. Ledger do encontro e autoritativo, nao ha mensagem cliente para pedir recompensa. Teste deterministico executa combate de dois controladores reais por intencoes, derrota compartilhada, repeticao de ataques e reconexao sem duplicacao.

Fechamento do contrato V1 antes da primeira distribuicao de rede: Actor tambem carrega facingX/facingY float e flags byte (1 dash ativo, 2 aguardando pouso, 4 Freeze). DTO/interpolacao preservam esses dados. Nao existe compatibilidade declarada com prototipos intermediarios ainda nao distribuidos.
