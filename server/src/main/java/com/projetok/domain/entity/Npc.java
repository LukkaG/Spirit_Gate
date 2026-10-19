package com.projetok.domain.entity;

/**
 * Sucessora de NPC.java (que hoje é uma classe vazia, sem herança, desconectada do resto
 * do sistema). Um Npc de diálogo NÃO implementa Damageable/Attacker de propósito — um
 * vendedor não deveria poder ser atacado a menos que isso seja adicionado explicitamente
 * no futuro (ex.: um NpcGuardiao que também implemente Damageable), nunca por herdar de
 * uma superclasse genérica demais.
 */
public final class Npc extends GameEntity {

    private final DialogueScript dialogue;

    public Npc(int id, String name, Position position, DialogueScript dialogue) {
        super(id, name, position);
        this.dialogue = dialogue;
    }

    public DialogueScript startDialogue(Player initiator) {
        return dialogue;
    }
}
