package com.projetok.multiplayer;

/** Backend de sessão: somente a thread proprietária do servidor invoca seus métodos. */
public interface SessionSimulation {
    boolean join(int id,String name);
    void leave(int id);
    boolean accept(int id,Protocol.Intent intent);
    void step();
    Protocol.State snapshot();
    default boolean supportsCombat(){return false;}
    default Protocol.CombatState combatSnapshot(){throw new IllegalStateException("Combate indisponivel");}
    default boolean request(int id,Protocol.WorldRequest request){return false;}
    default Protocol.State snapshotFor(int id){return snapshot();}
    default Protocol.CombatState combatSnapshotFor(int id){return combatSnapshot();}
    default java.util.List<Protocol.Appearance> appearancesFor(int id){return appearances();}
    default boolean supportsCosmetics(){return false;}
    default boolean appearance(int id,Protocol.AppearanceRequest request){return false;}
    default java.util.List<Protocol.Appearance> appearances(){return java.util.List.of();}
}
