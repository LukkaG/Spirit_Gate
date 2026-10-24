# Combate PvE autoritativo

Servidor e cliente precisam negociar aplicação 3. SPACE envia intenção ATTACK com parâmetros zero. A conexão identifica o atacante. `RpgWorld.attack()` escolhe o inimigo vivo mais próximo no alcance racial existente, verifica linha de visão da geometria, HP e cooldown de 12 ticks e calcula `attackPower()`. A thread serializa golpes concorrentes; o primeiro golpe letal impede dano posterior à mesma geração. PvP não possui comando ou caminho de dano.

O encontro publica HIT, DEATH e eventos de recompensa com sequência monotônica, tick global, jogador, inimigo, geração e quantidade. O cliente apresenta HP; não aplica dano nem recompensa localmente. O ledger recente se repete nos snapshots: consumidores que criarem efeitos precisam deduplicar por sequência. Não há entrega garantida de eventos antigos.

Política padrão `SYNTHETIC_PARTICIPANTS`: cada ID que causou dano positivo recebe um evento de 3 unidades sintéticas por derrota. Isso não adiciona XP, moedas ou loot persistente. `--no-synthetic-rewards` seleciona `DISABLED` e publica REWARD_REFUSED. O histórico de participantes inclui quem saiu antes da morte; quest só avança em ledgers ainda presentes. A recompensa da missão `.sgquest` é aplicada somente ao inventário sintético da sessão, pelo plano real `QuestRewards`.

Shunpo reutiliza `castAbility()` e `castSecondaryAbility()`: nível 20, custo 25, cooldown compartilhado 90 ticks, janela defensiva de seis ticks, destinos e trajetos existentes. SHIFT aproxima; Q esquiva. Perfis padrão são nível 1; `--synthetic-level 20` habilita fixtures de nível 20 no servidor. Não executa técnicas conceituais de domínio. Cobertura de vigor/parede continua nas regressões do domínio; a cobertura específica de rede deve ser ampliada.

Morte de jogador é publicada por HP zero e bloqueia ações de combate. R solicita recuperação existente em spawn seguro. O slime usa respawn existente de 240 ticks. Eventos duráveis e apresentação específica de cadáver do jogador permanecem pendentes.
