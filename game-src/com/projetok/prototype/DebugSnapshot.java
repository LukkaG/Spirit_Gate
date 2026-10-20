package com.projetok.prototype;

import java.util.List;

/** Cópia imutável do estado do jogo, segura para consumo pela thread da interface. */
public record DebugSnapshot(
        List<ActorView> actors, List<Obstacle> obstacles, List<EffectView> effects,
        int playerHp, int playerMaxHp, int defeated, boolean gameOver, long ticks,
        String lastEvent, float facingX, float facingY, float cooldown, AttackView attack,
        float stamina, float maxStamina, float cryostasisCooldown, List<IceZoneView> iceZones,
        List<StatusView> statuses, boolean lightningStepActive, float lightningStepRemaining,
        float lightningStepCooldown, List<LightningTrailView> lightningTrails,
        boolean lightningStepAwaitingLanding, List<SoftObstacleView> softObstacles,
        SkillHudView cryostasisHud, SkillHudView lightningStepHud) {

    /** Copia os novos metadados visuais; a interface nunca recebe a lista mutável do mundo. */
    public DebugSnapshot {
        actors = List.copyOf(actors);
        obstacles = List.copyOf(obstacles);
        effects = List.copyOf(effects);
        iceZones = List.copyOf(iceZones);
        statuses = List.copyOf(statuses);
        lightningTrails = List.copyOf(lightningTrails);
        softObstacles = List.copyOf(softObstacles);
    }

    /** Disponibilidade calculada pela simulação, sem decisões de gameplay no painel. */
    public enum SkillState { READY, COOLDOWN, INSUFFICIENT_STAMINA, DEAD, ACTIVE, AWAITING_LANDING }

    /** Origem explícita do último retorno breve de uma habilidade. */
    public enum SkillFeedbackKind {
        NONE, ACCEPTED, INVALID_TARGET, DEAD, ZERO_DIRECTION, COOLDOWN,
        INSUFFICIENT_STAMINA, AWAITING_LANDING, SOLID_OR_BOUNDARY
    }

    /** Mensagem e duração copiadas da simulação; a interface não avança seu relógio. */
    public record SkillFeedbackView(SkillFeedbackKind kind, String message, float remaining) { }

    /** Linha pronta para renderização, com números e estado sem referências mutáveis. */
    public record SkillHudView(String skillId, String key, String label, float cost, float cooldown,
                               float stamina, SkillState state, String stateText,
                               SkillFeedbackView feedback) { }

    /** Aparências do demo; ambas usam retângulos explícitos de colisão leve. */
    public enum SoftObstacleKind { BUSH, POST }

    /** Hitbox publicada a partir da mesma geometria usada pela simulação. */
    public record SoftObstacleView(float x, float y, float width, float height, SoftObstacleKind kind) { }

    /** Segmento visual elétrico com tempo de vida; sem estado mutável da habilidade. */
    public record LightningTrailView(float x0, float y0, float x1, float y1,
                                     float remaining, float duration) { }

    /** Estado visual por ator, separado do contrato histórico de ActorView. */
    public record StatusView(int actorId, float freezeRemaining) { }

    /** Círculo visual fixo; somente valores, sem regras ou entidades. */
    public record IceZoneView(float x, float y, float radius, float elapsed, float duration) { }

    /** Informações suficientes para desenhar um personagem sem expor componentes mutáveis. */
    public record ActorView(int id, String name, float x, float y, float radius,
                            int hp, int maxHp, boolean player, boolean alive) { }

    /** Retângulo sólido no mapa em coordenadas de mundo (pixels de depuração). */
    public record Obstacle(float x, float y, float width, float height) { }

    /** Marcador visual transitório do resultado de um ataque. */
    public record AttackView(float x, float y, float facingX, float facingY, float remaining) { }

    /** Marcador visual transit?rio do resultado de um ataque. */
    public record EffectView(float x, float y, int damage, float remaining, boolean received) { }
}
