package com.projetok.rpg;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/** XP de dominio em meias unidades inteiras: 3 unidades representam exatamente 1,5 XP. */
public record DomainProgression(String domain, long xpHalfUnits, LocalDate lastReset, boolean resetQuestComplete) {
    public static final String NONE = "none";
    public static final int INITIATION_LEVEL = 15;
    public static final int SHUNPO_LEVEL = 20;
    public DomainProgression {
        Objects.requireNonNull(domain, "dominio");
        if (!domain.equals(NONE)) Domains.get(domain);
        if (xpHalfUnits < 0 || domain.equals(NONE) && xpHalfUnits != 0)
            throw new IllegalArgumentException("XP de dominio invalida");
    }
    public static DomainProgression initial() { return new DomainProgression(NONE, 0, null, false); }
    public boolean chosen() { return !domain.equals(NONE); }
    public double xp() { return xpHalfUnits / 2.0; }
    public String xpText() { return (xpHalfUnits / 2) + (xpHalfUnits % 2 == 0 ? "" : ",5"); }
    public DomainProgression choose(String race, int characterLevel, String selected) {
        if (characterLevel < INITIATION_LEVEL) throw new IllegalStateException("Iniciacao disponivel no nivel 15");
        if (chosen()) throw new IllegalStateException("Dominio ja escolhido; reset necessario");
        if (!Domains.get(selected).race().equals(CharacterRaces.get(race).id()))
            throw new IllegalArgumentException("Dominio indisponivel para esta raca");
        return new DomainProgression(selected, 0, lastReset, resetQuestComplete);
    }
    /** Recompensas de quests e mobs usam metade da XP normal; sem dominio, nao acumulam maestria. */
    public DomainProgression award(long characterXp) {
        if (characterXp < 0) throw new IllegalArgumentException("Recompensa invalida");
        return chosen() ? new DomainProgression(domain, Math.addExact(xpHalfUnits, characterXp), lastReset, resetQuestComplete) : this;
    }
    public DomainProgression completeResetQuest() { return new DomainProgression(domain, xpHalfUnits, lastReset, true); }
    public LocalDate nextResetDate() { return lastReset == null ? null : lastReset.plusMonths(1); }
    /** Valida tudo antes de retornar. O chamador debita cash somente apos o sucesso. */
    public DomainProgression reset(LocalDate today, long cashBalance, DomainResetPolicy policy) {
        Objects.requireNonNull(today); Objects.requireNonNull(policy);
        if (cashBalance < 0) throw new IllegalArgumentException("Saldo de cash invalido");
        if (!policy.enabled()) throw new IllegalStateException("Reset indisponivel");
        if (!chosen()) throw new IllegalStateException("Personagem sem dominio");
        if (!resetQuestComplete) throw new IllegalStateException("Conclua a quest de reset");
        if (nextResetDate() != null && today.isBefore(nextResetDate())) throw new IllegalStateException("Aguarde um mes desde o ultimo reset");
        if (cashBalance < policy.cashCost()) throw new IllegalStateException("Cash insuficiente");
        return new DomainProgression(NONE, 0, today, false);
    }
    public static DomainProgression read(Set<String> flags) {
        String domain = NONE; long xp = 0; LocalDate reset = null; boolean quest = false;
        Set<String> seen = new HashSet<>();
        for (String flag : flags) {
            String key = flag.substring(0, flag.indexOf(':') < 0 ? flag.length() : flag.indexOf(':'));
            if (!Set.of("domain", "mastery-half", "reset-last", "reset-quest").contains(key)) continue;
            if (!seen.add(key) || flag.indexOf(':') < 0) throw new IllegalArgumentException("Estado de dominio duplicado/invalido");
            String value = flag.substring(flag.indexOf(':') + 1);
            switch (key) {
                case "domain" -> domain = value;
                case "mastery-half" -> xp = Long.parseLong(value);
                case "reset-last" -> {
                    try { reset = LocalDate.parse(value); }
                    catch (java.time.DateTimeException invalid) { throw new IllegalArgumentException("Data de reset invalida", invalid); }
                }
                case "reset-quest" -> {
                    if (!value.equals("complete")) throw new IllegalArgumentException("Quest de reset invalida");
                    quest = true;
                }
                default -> throw new IllegalArgumentException("Estado de dominio invalido");
            }
        }
        return new DomainProgression(domain, xp, reset, quest);
    }
    public void writeTo(Set<String> flags) {
        flags.removeIf(f -> f.startsWith("domain:") || f.startsWith("mastery-half:") || f.startsWith("reset-last:") || f.startsWith("reset-quest:") || !chosen() && f.startsWith("domain-skill:"));
        flags.add("domain:" + domain);
        flags.add("mastery-half:" + xpHalfUnits);
        if (lastReset != null) flags.add("reset-last:" + lastReset);
        if (resetQuestComplete) flags.add("reset-quest:complete");
    }
    public void validateFor(CharacterIdentity identity, long characterXp) {
        if (chosen() && (!Domains.get(domain).race().equals(identity.race()) || RpgProgression.builtin().level(characterXp) < INITIATION_LEVEL))
            throw new IllegalArgumentException("Dominio incompativel com personagem");
    }
}
