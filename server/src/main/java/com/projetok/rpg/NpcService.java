package com.projetok.rpg;
import com.projetok.domain.entity.Position;
import java.util.*;
import java.io.*;
public final class NpcService {
    /** NPC existente com entrada de dialogo validada; assinatura anterior preservada. */
    public record View(String id,String name,String region,float x,float y,String dialogue) {
        /** Entrada padrao dos NPCs historicos. */
        public View(String id,String name,String region,float x,float y){this(id,name,region,x,y,id+".start");}
    }
    private final List<View> npcs=new ArrayList<>();
    public NpcService(RpgContent content) {this(content,RpgSceneSet.entries("npcs"));}
    /** Configuracao de posicoes para sessao isolada, validada pelos catalogos reais. */
    public NpcService(RpgContent content,Map<String,String> definitions) {
        if(definitions.size()>16)throw new IllegalArgumentException("NPCs excessivos");
        for(String id:new TreeSet<>(definitions.keySet())) {
            String[] v=definitions.get(id).split("\\|",-1);var base=content.get("npc",id);
            if((v.length!=3&&v.length!=5)||!base.region().equals(v[0]))throw new IllegalArgumentException("NPC invalido");
            float x=Float.parseFloat(v[1]),y=Float.parseFloat(v[2]);
            if(!Float.isFinite(x)||!Float.isFinite(y)||x<18||x>942||y<18||y>622)throw new IllegalArgumentException("Posicao de NPC invalida");
            String dialogue=v.length==5?v[3]:id+".start",name=v.length==5?new String(Base64.getUrlDecoder().decode(v[4]),java.nio.charset.StandardCharsets.UTF_8):base.name();
            if(name.isBlank()||name.length()>80||name.chars().anyMatch(Character::isISOControl)||!dialogue.startsWith(id+".")||!new DialogueService().ids().contains(dialogue))throw new IllegalArgumentException("Nome ou dialogo de NPC invalido");
            npcs.add(new View(id,name,v[0],x,y,dialogue));
        }
    }
    public List<View> views(String region){return npcs.stream().filter(n->n.region().equals(region)).toList();}
    public Optional<View> nearest(String region,Position player){return views(region).stream().filter(n->Math.hypot(n.x()-player.x(),n.y()-player.y())<=70).min(Comparator.comparingDouble(n->Math.hypot(n.x()-player.x(),n.y()-player.y())));}
}
