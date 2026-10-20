package com.projetok.studio;
import com.projetok.rpg.*;
import java.util.*;

/** Configuracao de NPCs reais; missoes associadas sao referencias, sem concessao automatica. */
public final class NpcEditing {
    private NpcEditing(){}
    /** Valida nome, entrada de dialogo e missao existente. */
    public static void validate(WorldDocument.Element e){if(e.kind()!=WorldDocument.Kind.NPC)throw new IllegalArgumentException("Selecione um NPC");var p=e.properties();String name=p.getOrDefault("npc.name",RpgContent.builtin().get("npc",e.reference()).name());String dialogue=p.getOrDefault("npc.dialogue",e.reference()+".start");if(name.isBlank()||name.length()>80||name.chars().anyMatch(Character::isISOControl)||!dialogue.startsWith(e.reference()+".")||!new DialogueService().ids().contains(dialogue))throw new IllegalArgumentException("Nome ou dialogo invalido");String quest=p.getOrDefault("npc.quest","");if(!quest.isEmpty())RpgContent.builtin().get("quest",quest);String interaction=p.getOrDefault("npc.interaction","dialogue");if(!interaction.equals("dialogue"))throw new IllegalArgumentException("Interacao suportada: dialogue");}
    /** Configuracao textual limitada consumida pelo servico de NPCs real. */
    public static String definition(WorldDocument.Element e,String region){validate(e);var p=e.properties();String name=p.getOrDefault("npc.name",RpgContent.builtin().get("npc",e.reference()).name());return region+"|"+e.x()+"|"+e.y()+"|"+p.getOrDefault("npc.dialogue",e.reference()+".start")+"|"+Base64.getUrlEncoder().withoutPadding().encodeToString(name.getBytes(java.nio.charset.StandardCharsets.UTF_8));}
}
