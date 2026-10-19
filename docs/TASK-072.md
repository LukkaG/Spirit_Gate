# TASK-072 - Combate autoritativo

Servidor integra ataque basico via CombatService e os servicos REAIS LightningStepService/CryostasisService/StaminaService, sem alterar constantes historicas. Alvo, alcance, linha livre, vida, custo, recarga e colisao resolvidos pelo servidor. Intencoes duplicadas/antigas recusadas; ate oito comandos por sessao/tick.

Chefe compartilhado de teste criado via MobFactory, ainda sem IA/recompensa nesta etapa. Cliente observa HP/posicao/vigor do snapshot e nao envia dano ou HP. Ataque basico tem alcance 82 e recarga 12 ticks; habilidades preservam 35/55 vigor e recargas existentes. Testes comprovam dano unico, recarga e consumo real dos servicos. Testes de duas sessoes passaram a contar jogadores separadamente do chefe, sem alterar baselines da arena.
