package com.projetok.rpg;
import java.util.*;
import java.io.*;
public final class WorldObjects {
    public record ObjectView(String id,String region,float x,float y,String item,int amount,int respawn,boolean available) { }
    private final List<ObjectView> definitions=new ArrayList<>();
    public WorldObjects(RpgContent content,ItemCatalog items) {
        try(InputStream in=WorldObjects.class.getResourceAsStream("/rpg/objects.properties")) {
            if(in==null)throw new IOException("Objetos ausentes");Properties p=new Properties();p.load(in);
            for(String id:new TreeSet<>(p.stringPropertyNames())) {
                String[] v=p.getProperty(id).split("\\|");if(v.length!=6||!id.matches("[a-z_]{1,30}"))throw new IllegalArgumentException("Objeto invalido");content.get("region",v[0]);items.get(v[3]);float x=Float.parseFloat(v[1]),y=Float.parseFloat(v[2]);int n=Integer.parseInt(v[4]),respawn=Integer.parseInt(v[5]);
                if(!Float.isFinite(x)||!Float.isFinite(y)||x<18||x>942||y<18||y>622||n<1||n>99||respawn<0||respawn>9000)throw new IllegalArgumentException("Objeto fora dos limites");
                definitions.add(new ObjectView(id,v[0],x,y,v[3],n,respawn,true));
            }
        }catch(IOException e){throw new IllegalStateException(e);}
    }
    public List<ObjectView> views(String region,long tick,Set<String> flags){return definitions.stream().filter(o->o.region().equals(region)).map(o->new ObjectView(o.id(),o.region(),o.x(),o.y(),o.item(),o.amount(),o.respawn(),!flags.contains("opened:"+o.id())&&flags.stream().filter(f->f.startsWith("object-until:"+o.id()+":")).allMatch(f->Long.parseLong(f.substring(f.lastIndexOf(':')+1))<=tick))).toList();}
}
