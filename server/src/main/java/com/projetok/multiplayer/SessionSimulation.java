package com.projetok.multiplayer;

/** Backend de sessão: somente a thread proprietária do servidor invoca seus métodos. */
public interface SessionSimulation {
    boolean join(int id,String name);
    void leave(int id);
    boolean accept(int id,Protocol.Intent intent);
    void step();
    Protocol.State snapshot();
    default boolean supportsCosmetics(){return false;}
    default boolean appearance(int id,Protocol.AppearanceRequest request){return false;}
    default java.util.List<Protocol.Appearance> appearances(){return java.util.List.of();}
}
