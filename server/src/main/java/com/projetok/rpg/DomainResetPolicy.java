package com.projetok.rpg;

/** Regra do reset: 35 Spirit Coins. Compra e validacao financeira pertencem ao servico online. */
public record DomainResetPolicy(long cashCost) {
    public DomainResetPolicy {
        if (cashCost < -1 || cashCost == 0) throw new IllegalArgumentException("Custo de reset invalido");
    }
    public boolean enabled() { return cashCost > 0; }
    public static DomainResetPolicy disabled() { return new DomainResetPolicy(-1); }
    public static DomainResetPolicy builtin() {
        String configured = System.getProperty("spiritgate.domainReset.cashCost");
        return configured == null ? new DomainResetPolicy(35) : new DomainResetPolicy(Long.parseLong(configured));
    }
}
