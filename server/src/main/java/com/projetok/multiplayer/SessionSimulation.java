package com.projetok.multiplayer;

/** Backend de sessão: somente a thread proprietária do servidor invoca seus métodos. */
public interface SessionSimulation {
    boolean join(int id,String name);
    void leave(int id);
    default boolean join(int id,String name,int application){return join(id,name);}
    /** A entrada com escolha explicita somente e aceita por backends que oferecem essa capacidade. */
    default boolean join(int id,SessionCharacterSelection selection,int application){return false;}
    /** Indica suporte a escolha previa de personagem sem alterar a entrada historica. */
    default boolean supportsCharacterCreation(){return false;}
    default String admissionFailure(){return "Servidor cheio";}
    default void closed(){}
    default boolean supportsContent(){return false;}
    default String contentHash(){return "";}
    default void contentAck(int id,Protocol.Content content){}
    default Protocol.Content nextContent(int id){return null;}
    default boolean disconnectRequested(int id){return false;}
    default boolean supportsGameplay(){return false;}
    /** Somente backends com executor autoritativo podem negociar a aplicação 7. */
    default boolean supportsSkills(){return false;}
    default boolean skill(int id,SkillProtocol.Request request){return false;}
    default SkillProtocol.State skillsSnapshotFor(int id){throw new IllegalStateException("Skills indisponíveis");}
    default boolean gameplay(int id,GameplayProtocol.Request request){return false;}
    default GameplayProtocol.Attributes attributesSnapshotFor(int id){throw new IllegalStateException("Atributos indisponíveis");}
    default GameplayProtocol.Energy energySnapshotFor(int id,long sequence){throw new IllegalStateException("Energia indisponível");}
    default GameplayProtocol.State gameplaySnapshotFor(int id){throw new IllegalStateException("Gameplay indisponivel");}
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
