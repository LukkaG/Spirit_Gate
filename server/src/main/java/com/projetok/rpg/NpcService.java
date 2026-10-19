package com.projetok.rpg;
import com.projetok.domain.entity.Position;
import java.util.*;
import java.io.*;
public final class NpcService {
    public record View(String id,String name,String region,float x,float y) { }
    private final List<View> npcs=new ArrayList<>();
    public NpcService(RpgContent content) {
        try(InputStream in=NpcService.class.getResourceAsStream("/rpg/npcs.properties")) {
            if(in==null)throw new IOException("NPCs ausentes");Properties p=new Properties();p.load(in);
            for(String id:new TreeSet<>(p.stringPropertyNames())) {
                String[] v=p.getProperty(id).split("\\|");var base=content.get("npc",id);
                if(v.length!=3||!base.region().equals(v[0]))throw new IllegalArgumentException("NPC invalido");
                float x=Float.parseFloat(v[1]),y=Float.parseFloat(v[2]);
                if(!Float.isFinite(x)||!Float.isFinite(y)||x<18||x>942||y<18||y>622)throw new IllegalArgumentException("Posicao de NPC invalida");
                npcs.add(new View(id,base.name(),v[0],x,y));
            }
        }catch(IOException e){throw new IllegalStateException(e);}
    }
    public List<View> views(String region){return npcs.stream().filter(n->n.region().equals(region)).toList();}
    public Optional<View> nearest(String region,Position player){return views(region).stream().filter(n->Math.hypot(n.x()-player.x(),n.y()-player.y())<=70).min(Comparator.comparingDouble(n->Math.hypot(n.x()-player.x(),n.y()-player.y())));}
}
