package com.projetok.rpg;
import java.util.*;
import java.io.*;
public final class WorldObjects {
    public record ObjectView(String id,String region,float x,float y,String item,int amount,int respawn,boolean available) { }
    private final List<ObjectView> definitions=new ArrayList<>();
    public WorldObjects(RpgContent content,ItemCatalog items) {this(content,items,RpgSceneSet.entries("objects"));}
    /** Objetos de teste reutilizam itens e regras de coleta existentes. */
    public WorldObjects(RpgContent content,ItemCatalog items,Map<String,String> definitions) {
        if(definitions.size()>128)throw new IllegalArgumentException("Objetos excessivos");
        for(String id:new TreeSet<>(definitions.keySet())) {
            String[] v=definitions.get(id).split("\\|",-1);if(v.length!=6||!id.matches("[a-z_]{1,30}"))throw new IllegalArgumentException("Objeto invalido");content.get("region",v[0]);items.get(v[3]);float x=Float.parseFloat(v[1]),y=Float.parseFloat(v[2]);int n=Integer.parseInt(v[4]),respawn=Integer.parseInt(v[5]);
            if(!Float.isFinite(x)||!Float.isFinite(y)||x<18||x>942||y<18||y>622||n<1||n>99||respawn<0||respawn>9000)throw new IllegalArgumentException("Objeto fora dos limites");
            this.definitions.add(new ObjectView(id,v[0],x,y,v[3],n,respawn,true));
        }
    }
    public List<ObjectView> views(String region,long tick,Set<String> flags){return definitions.stream().filter(o->o.region().equals(region)).map(o->new ObjectView(o.id(),o.region(),o.x(),o.y(),o.item(),o.amount(),o.respawn(),!flags.contains("opened:"+o.id())&&flags.stream().filter(f->f.startsWith("object-until:"+o.id()+":")).allMatch(f->Long.parseLong(f.substring(f.lastIndexOf(':')+1))<=tick))).toList();}
}
