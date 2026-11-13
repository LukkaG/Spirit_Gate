package com.projetok.rpg;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Catálogo declarativo das técnicas propostas para o Spirit Gate.
 * Não executa combate, não habilita ações de rede e não altera os domínios dos saves.
 * Alcances usam unidades de mundo; tempos usam ticks de 30 Hz. O balanceamento é provisório.
 */
public final class RpgSkillCatalog {
    /** Forma de escolher o alvo; TARGET pode selecionar mob ou aliado nas curas híbridas. */
    public enum Targeting { TARGET, SELF, GROUND, LINE, CONE, ALLY }

    /** Padrão executado no servidor, separado da apresentação visual. */
    public enum Pattern {
        PROJECTILE, MELEE_AREA, PRISON, VORTEX, DASH, MULTI_HIT,
        KNOCKBACK_CHECKPOINT, CHECKPOINT_PULL, LINE_PROJECTILE, CHAIN, CIRCULAR_DASH,
        FISSURE, FISSURE_ERUPTION, RAIN, DAMAGE_POOL, WEB, BEAM, AREA_PRISON,
        QUICKSAND, ARROW, SENTRY, CONE_VOLLEY, HEAL, AREA_HEAL, REVIVE, CLEANSE,
        PUNCH, GROUND_STOMP, BODY_SLAM, CONE_COMBO
    }

    /** ROOT bloqueia movimento; STUN bloqueia movimento, ataques e habilidades. */
    public enum EffectType { SLOW, ROOT, STUN, BLEED, BURN }

    /** Restrição de dano: as curas híbridas Luz 1/5 só ferem mobs, nunca jogadores. */
    public enum DamageScope { NONE, ENEMIES, MOBS_ONLY }

    /**
     * Efeito proposto. Em SLOW, potencyPermille é a fração da velocidade preservada.
     * BLEED/BURN usam dano por pulso e intervalo; ROOT/STUN não têm dano próprio.
     */
    public record Effect(EffectType type, int durationTicks, int potencyPermille,
                         int intervalTicks, int baseDamage) {
        /** Recusa durações ilimitadas e combinações ambíguas de controle e dano periódico. */
        public Effect {
            Objects.requireNonNull(type, "tipo de efeito");
            if (durationTicks < 1 || potencyPermille < 0 || potencyPermille > 1000
                    || intervalTicks < 0 || baseDamage < 0)
                throw new IllegalArgumentException("Efeito inválido");
            boolean periodic = type == EffectType.BLEED || type == EffectType.BURN;
            if (periodic && (intervalTicks < 1 || intervalTicks > durationTicks || baseDamage < 1)
                    || !periodic && (intervalTicks != 0 || baseDamage != 0)
                    || type == EffectType.SLOW && (potencyPermille < 1 || potencyPermille >= 1000)
                    || type != EffectType.SLOW && potencyPermille != 0)
                throw new IllegalArgumentException("Parâmetros incompatíveis com o efeito");
        }

        /** Identifica controles que suspendem o deslocamento, sem confundir lentidão com prisão. */
        public boolean blocksMovement() { return type == EffectType.ROOT || type == EffectType.STUN; }

        /** Prisões permitem ataques e habilidades; somente atordoamento impede essas ações. */
        public boolean blocksActions() { return type == EffectType.STUN; }
    }

    /**
     * Regras temporais adicionais. hits representa impactos, saltos, dashes ou tiros conforme pattern.
     * durationTicks é a vida da área/estado; checkpointTicks é a janela para consumir o checkpoint.
     * repeatDamagePermille reduz cada repetição no mesmo alvo quando CHAIN não encontra outro inimigo.
     */
    public record Mechanics(Pattern pattern, int hits, int intervalTicks, int durationTicks,
                            String prerequisiteSkill, int checkpointTicks, int repeatDamagePermille) {
        /** Mantém referências e limites explícitos, sem inferir habilitação ou disponibilidade. */
        public Mechanics {
            Objects.requireNonNull(pattern, "padrão de habilidade");
            Objects.requireNonNull(prerequisiteSkill, "pré-requisito");
            if (hits < 1 || intervalTicks < 0 || durationTicks < 0 || checkpointTicks < 0
                    || repeatDamagePermille < 1 || repeatDamagePermille > 1000
                    || !prerequisiteSkill.isEmpty() && !prerequisiteSkill.matches("[a-z]+_[1-5]"))
                throw new IllegalArgumentException("Mecânica inválida");
        }
    }

    /**
     * Definição imutável de um slot. baseDamage é dano por impacto antes de atributos/defesa;
     * healing é recuperação por alvo. Custo usa unidades de energia, sem conversão para décimos.
     * Todos os números de balanceamento são propostas, exceto os requisitos explícitos do pedido.
     */
    public record Definition(String id, String kit, int slot, String name, String description,
                             Targeting targeting, Mechanics mechanics, List<Effect> effects,
                             int range, int radius, int energyCost, int cooldownTicks,
                             int baseDamage, int healing, DamageScope damageScope) {
        /** Valida o vínculo entre ID/kit/slot e copia a lista de efeitos para impedir mutação externa. */
        public Definition {
            Objects.requireNonNull(id, "id"); Objects.requireNonNull(kit, "kit");
            Objects.requireNonNull(name, "nome"); Objects.requireNonNull(description, "descrição");
            Objects.requireNonNull(targeting, "alvo"); Objects.requireNonNull(mechanics, "mecânica");
            Objects.requireNonNull(damageScope, "escopo de dano");
            effects = List.copyOf(effects);
            if (!KITS.contains(kit) || slot < 1 || slot > 5 || !id.equals(kit + "_" + slot)
                    || name.isBlank() || description.isBlank() || range < 0 || radius < 0
                    || energyCost < 0 || cooldownTicks < 1 || baseDamage < 0 || healing < 0
                    || damageScope == DamageScope.NONE && (baseDamage > 0
                        || effects.stream().anyMatch(e -> e.baseDamage() > 0))
                    || damageScope != DamageScope.NONE && baseDamage == 0
                        && effects.stream().noneMatch(e -> e.baseDamage() > 0))
                throw new IllegalArgumentException("Definição de habilidade inválida");
        }
    }

    // --- Kits únicos e aliases dos domínios históricos ---

    private static final List<String> KITS = List.of("water", "wind", "thunder", "fire",
            "spirit", "aetheri", "light", "martial");
    private static final Map<String, String> DOMAIN_KITS = Map.of(
            "water", "water", "wind", "wind", "thunder", "thunder", "fire", "fire",
            "light", "light", "martial", "martial", "bow_long", "aetheri", "bow_short", "aetheri",
            "darkness_debuff", "spirit", "darkness_control", "spirit");

    private static final List<Definition> VALUES = List.of(
        // Água: prisões restringem somente o movimento; os golpes próximos acompanham o lançador.
        skill("water", 1, "Corte de água", "Corte de água que viaja até o inimigo e aplica lentidão.",
                Targeting.TARGET, once(Pattern.PROJECTILE), List.of(slow(60)), 240, 0, 12, 45, 20, 0, DamageScope.ENEMIES),
        skill("water", 2, "Corte de maré", "Corte corpo a corpo em área presa ao jogador, com lentidão.",
                Targeting.SELF, once(Pattern.MELEE_AREA), List.of(slow(60)), 0, 82, 16, 75, 22, 0, DamageScope.ENEMIES),
        skill("water", 3, "Prisão de água", "Imobiliza o alvo por 2 segundos; ele ainda pode atacar e usar habilidades.",
                Targeting.TARGET, once(Pattern.PRISON), List.of(root(60)), 220, 0, 20, 180, 0, 0, DamageScope.NONE),
        skill("water", 4, "Vórtice de água", "Redemoinho ao redor do jogador, acompanhando seu giro de espada.",
                Targeting.SELF, timed(Pattern.VORTEX, 4, 10, 40), List.of(), 0, 100, 25, 180, 9, 0, DamageScope.ENEMIES),
        skill("water", 5, "Investida de água", "Avança na direção frontal e corta os inimigos atravessados, envolto em água.",
                Targeting.LINE, timed(Pattern.DASH, 1, 0, 8), List.of(), 240, 22, 25, 150, 30, 0, DamageScope.ENEMIES),

        // Vento: o checkpoint pertence ao lançador e só pode ser consumido uma vez pelo quinto slot.
        skill("wind", 1, "Corte de vento", "Corte de vento que viaja até o inimigo selecionado.",
                Targeting.TARGET, once(Pattern.PROJECTILE), List.of(), 260, 0, 12, 45, 22, 0, DamageScope.ENEMIES),
        skill("wind", 2, "Três cortes de vento", "Três cortes sucessivos no alvo, com sangramento.",
                Targeting.TARGET, timed(Pattern.MULTI_HIT, 3, 3, 6), List.of(dot(EffectType.BLEED, 90)), 240, 0, 20, 120, 10, 0, DamageScope.ENEMIES),
        skill("wind", 3, "Passo cortante", "Investida frontal sem alvo, cortando os inimigos durante a passagem.",
                Targeting.LINE, timed(Pattern.DASH, 1, 0, 8), List.of(), 240, 20, 22, 120, 28, 0, DamageScope.ENEMIES),
        skill("wind", 4, "Vendaval de repulsão", "Dispara três lâminas frontais; ao atingir, empurra inimigos e cria um checkpoint para o retorno.",
                Targeting.LINE, new Mechanics(Pattern.KNOCKBACK_CHECKPOINT, 1, 0, 0, "", 150, 1000), List.of(), 240, 70, 22, 150, 16, 0, DamageScope.ENEMIES),
        skill("wind", 5, "Vendaval de retorno", "Sem alvo: consome as marcas da habilidade 4 e puxa todos os inimigos afetados ainda ao alcance.",
                Targeting.SELF, new Mechanics(Pattern.CHECKPOINT_PULL, 1, 0, 0, "wind_4", 150, 1000), List.of(), 300, 70, 18, 120, 16, 0, DamageScope.ENEMIES),

        // Raio: a corrente totaliza quatro impactos, inclusive quando precisa repetir o mesmo alvo.
        skill("thunder", 1, "Corte de raio", "Corte elétrico que viaja até o inimigo selecionado.",
                Targeting.TARGET, once(Pattern.PROJECTILE), List.of(), 240, 0, 12, 45, 22, 0, DamageScope.ENEMIES),
        skill("thunder", 2, "Lança de raio", "Lança de raio em linha reta que atordoa os inimigos atingidos.",
                Targeting.LINE, once(Pattern.LINE_PROJECTILE), List.of(stun(30)), 280, 18, 20, 120, 24, 0, DamageScope.ENEMIES),
        skill("thunder", 3, "Corrente de raio", "Até 4 impactos entre inimigos; sem outro alvo, repete no atual com dano decrescente.",
                Targeting.TARGET, new Mechanics(Pattern.CHAIN, 4, 8, 24, "", 0, 750), List.of(), 240, 130, 25, 150, 20, 0, DamageScope.ENEMIES),
        skill("thunder", 4, "Investida relâmpago", "Investida frontal que corta e atordoa inimigos atravessados, sem exigir alvo.",
                Targeting.LINE, timed(Pattern.DASH, 1, 0, 8), List.of(stun(30)), 240, 20, 25, 150, 30, 0, DamageScope.ENEMIES),
        skill("thunder", 5, "Quatro relâmpagos", "Fica invisível, sem colisão e imune a dano por cerca de 1,07 segundo. Ao reaparecer, quatro cortes atingem toda a área de conjuração.",
                Targeting.SELF, timed(Pattern.CIRCULAR_DASH, 4, 8, 32), List.of(stun(30)), 0, 110, 40, 270, 14, 0, DamageScope.ENEMIES),

        // Fogo: dano periódico e fissuras permanecem separados dos impactos iniciais.
        skill("fire", 1, "Corte de fogo", "Corte de fogo que viaja até o inimigo selecionado.",
                Targeting.TARGET, once(Pattern.PROJECTILE), List.of(), 240, 0, 12, 45, 22, 0, DamageScope.ENEMIES),
        skill("fire", 2, "Fissura ardente", "Corte que abre uma fissura em linha reta no chão e aplica queimadura.",
                Targeting.LINE, timed(Pattern.FISSURE, 1, 0, 90), List.of(dot(EffectType.BURN, 90)), 260, 24, 20, 120, 22, 0, DamageScope.ENEMIES),
        skill("fire", 3, "Vórtice de fogo", "Vórtice a partir do jogador, acompanhando-o e aplicando queimadura.",
                Targeting.SELF, timed(Pattern.VORTEX, 4, 10, 40), List.of(dot(EffectType.BURN, 90)), 0, 100, 25, 180, 9, 0, DamageScope.ENEMIES),
        skill("fire", 4, "Erupção de fogo", "Abre uma fissura de fogo e provoca uma grande erupção na extremidade.",
                Targeting.LINE, timed(Pattern.FISSURE_ERUPTION, 2, 15, 15), List.of(), 280, 85, 30, 210, 25, 0, DamageScope.ENEMIES),
        skill("fire", 5, "Chuva de cortes de fogo", "Chuva de cortes em área selecionada, com queimadura.",
                Targeting.GROUND, timed(Pattern.RAIN, 5, 12, 60), List.of(dot(EffectType.BURN, 90)), 280, 100, 40, 270, 10, 0, DamageScope.ENEMIES),

        // Spirit: os dois domínios históricos compartilham as mesmas cinco técnicas.
        skill("spirit", 1, "Poça espiritual", "Cria uma poça que causa dano em área e imobiliza inimigos.",
                Targeting.GROUND, timed(Pattern.DAMAGE_POOL, 4, 15, 60), List.of(root(30)), 240, 85, 22, 150, 8, 0, DamageScope.ENEMIES),
        skill("spirit", 2, "Teia espiritual", "Dispara uma teia em linha que imobiliza os inimigos atingidos.",
                Targeting.LINE, once(Pattern.WEB), List.of(root(60)), 260, 22, 18, 120, 0, 0, DamageScope.NONE),
        skill("spirit", 3, "Feixe espiritual", "Projeta um feixe de luz que causa dano na direção escolhida.",
                Targeting.LINE, once(Pattern.BEAM), List.of(), 280, 20, 20, 90, 30, 0, DamageScope.ENEMIES),
        skill("spirit", 4, "Prisão espiritual", "Cria uma prisão em área que imobiliza sem causar dano.",
                Targeting.GROUND, timed(Pattern.AREA_PRISON, 1, 0, 60), List.of(root(60)), 240, 95, 25, 180, 0, 0, DamageScope.NONE),
        skill("spirit", 5, "Areia movediça", "Cria uma área de lentidão sem causar dano.",
                Targeting.GROUND, timed(Pattern.QUICKSAND, 1, 0, 120), List.of(slow(30)), 260, 110, 25, 180, 0, 0, DamageScope.NONE),

        // Aetheri: ambos os arcos usam o mesmo kit, sem duplicar definições ou alterar saves.
        skill("aetheri", 1, "Flecha precisa", "Dispara uma flecha contra o alvo selecionado.",
                Targeting.TARGET, once(Pattern.ARROW), List.of(), 280, 0, 10, 36, 22, 0, DamageScope.ENEMIES),
        skill("aetheri", 2, "Sentinela", "Fica imóvel por 4 segundos; ataque básico dispara flechas target, mais rápidas com agilidade. Conjurar novamente cancela.",
                Targeting.SELF, timed(Pattern.SENTRY, 1, 10, 120), List.of(), 280, 0, 22, 150, 12, 0, DamageScope.ENEMIES),
        skill("aetheri", 3, "Chuva de flechas", "Cria uma chuva de flechas na área selecionada.",
                Targeting.GROUND, timed(Pattern.RAIN, 5, 12, 60), List.of(), 300, 100, 30, 180, 10, 0, DamageScope.ENEMIES),
        skill("aetheri", 4, "Flecha de energia", "Dispara uma grande flecha de energia azul frontal que percorre a linha até o contato.",
                Targeting.LINE, once(Pattern.LINE_PROJECTILE), List.of(), 320, 28, 25, 150, 34, 0, DamageScope.ENEMIES),
        skill("aetheri", 5, "Leque de flechas", "Dispara sete flechas viajantes em leque na direção em que o personagem olha.",
                Targeting.CONE, timed(Pattern.CONE_VOLLEY, 7, 0, 0), List.of(), 220, 80, 30, 180, 28, 0, DamageScope.ENEMIES),

        // Luz: TARGET distingue aliados curáveis de mobs danificáveis; REVIVE exige aliado morto.
        skill("light", 1, "Cura básica", "Cura um aliado ou o próprio usuário; contra um mob, causa dano em vez de curar.",
                Targeting.TARGET, once(Pattern.HEAL), List.of(), 220, 0, 12, 60, 20, 30, DamageScope.MOBS_ONLY),
        skill("light", 2, "Cura em área", "Cura o usuário e aliados na área ao seu redor.",
                Targeting.SELF, once(Pattern.AREA_HEAL), List.of(), 0, 110, 25, 150, 0, 24, DamageScope.NONE),
        skill("light", 3, "Reviver", "Revive um aliado derrotado na posição válida em que ele se encontra.",
                Targeting.ALLY, once(Pattern.REVIVE), List.of(), 160, 0, 40, 900, 0, 40, DamageScope.NONE),
        skill("light", 4, "Purificação", "Remove estados negativos de um aliado ou do próprio usuário.",
                Targeting.ALLY, once(Pattern.CLEANSE), List.of(), 220, 0, 18, 180, 0, 0, DamageScope.NONE),
        skill("light", 5, "Cura superior", "Cura mais que a habilidade 1; contra um mob, causa dano em vez de curar.",
                Targeting.TARGET, once(Pattern.HEAL), List.of(), 240, 0, 30, 210, 40, 65, DamageScope.MOBS_ONLY),

        // Marcial: alcance próximo, impacto no solo e combo frontal originado no jogador.
        skill("martial", 1, "Soco preciso", "Desfere um soco contra o alvo selecionado.",
                Targeting.TARGET, once(Pattern.PUNCH), List.of(), 65, 0, 10, 36, 24, 0, DamageScope.ENEMIES),
        skill("martial", 2, "Sequência de socos", "Aplica múltiplos socos sucessivos no alvo selecionado.",
                Targeting.TARGET, timed(Pattern.MULTI_HIT, 4, 4, 12), List.of(), 65, 0, 18, 100, 9, 0, DamageScope.ENEMIES),
        skill("martial", 3, "Impacto sísmico", "Chuta o chão e cria fissuras que causam dano em área ao redor do jogador.",
                Targeting.SELF, once(Pattern.GROUND_STOMP), List.of(), 0, 110, 22, 150, 28, 0, DamageScope.ENEMIES),
        skill("martial", 4, "Body slam", "Agarra o inimigo próximo e o golpeia contra o chão, causando atordoamento.",
                Targeting.TARGET, once(Pattern.BODY_SLAM), List.of(stun(30)), 55, 35, 25, 180, 34, 0, DamageScope.ENEMIES),
        skill("martial", 5, "Combo de punhos e chutes", "Executa vários socos e chutes em cone frontal a partir do jogador.",
                Targeting.CONE, timed(Pattern.CONE_COMBO, 6, 4, 20), List.of(), 170, 95, 35, 240, 8, 0, DamageScope.ENEMIES)
    );
    private static final Map<String, Definition> BY_ID = VALUES.stream()
            .collect(Collectors.toUnmodifiableMap(Definition::id, d -> d));

    private RpgSkillCatalog() { }

    // --- Consulta sem efeitos colaterais ---

    /** Retorna as quarenta técnicas únicas, ordenadas por kit e slot. */
    public static List<Definition> all() { return VALUES; }

    /** Consulta um ID estável; erro de digitação não produz habilidade de fallback. */
    public static Definition get(String id) {
        Definition found = BY_ID.get(Objects.requireNonNull(id, "id"));
        if (found == null) throw new IllegalArgumentException("Habilidade desconhecida: " + id);
        return found;
    }

    /** Retorna os cinco slots de um kit, sem duplicar arco ou Spirit por especialização. */
    public static List<Definition> forKit(String kit) {
        if (!KITS.contains(Objects.requireNonNull(kit, "kit")))
            throw new IllegalArgumentException("Kit desconhecido: " + kit);
        return VALUES.stream().filter(d -> d.kit().equals(kit)).toList();
    }

    /**
     * Resolve somente IDs de domínio já existentes. Sem domínio escolhido, retorna lista vazia.
     * Esta consulta não escolhe domínio, não concede técnicas e não altera a progressão.
     */
    public static List<Definition> forDomain(String domain) {
        Objects.requireNonNull(domain, "domínio");
        if (DomainProgression.NONE.equals(domain)) return List.of();
        Domains.get(domain);
        return forKit(DOMAIN_KITS.get(domain));
    }

    // --- Construção das propostas de balanceamento ---

    /** Centraliza somente a montagem das definições; não aplica fórmulas de dano. */
    private static Definition skill(String kit, int slot, String name, String description,
                                    Targeting targeting, Mechanics mechanics, List<Effect> effects,
                                    int range, int radius, int cost, int cooldown,
                                    int damage, int healing, DamageScope scope) {
        return new Definition(kit + "_" + slot, kit, slot, name, description, targeting, mechanics,
                effects, range, radius, cost, cooldown, damage, healing, scope);
    }

    /** Cria um padrão de impacto único e sem estado persistente. */
    private static Mechanics once(Pattern pattern) { return timed(pattern, 1, 0, 0); }

    /** Registra uma sequência ou área com relógio explícito, sem dependência de outro slot. */
    private static Mechanics timed(Pattern pattern, int hits, int interval, int duration) {
        return new Mechanics(pattern, hits, interval, duration, "", 0, 1000);
    }

    /** Lentidão provisória de 50%, renovável sem somar duração. */
    private static Effect slow(int duration) { return new Effect(EffectType.SLOW, duration, 500, 0, 0); }

    /** Imobilização que preserva ataques e outras habilidades. */
    private static Effect root(int duration) { return new Effect(EffectType.ROOT, duration, 0, 0, 0); }

    /** Atordoamento que suspende movimento e ações. */
    private static Effect stun(int duration) { return new Effect(EffectType.STUN, duration, 0, 0, 0); }

    /** Dano periódico provisório de três pontos a cada segundo. */
    private static Effect dot(EffectType type, int duration) { return new Effect(type, duration, 0, 30, 3); }
}
