package com.projetok.rpg;
import java.io.*;
import java.util.*;
public final class Archetypes {
    public record Definition(String id,int hp,int attack,String ability,int cost,int cooldown,int effect) {
        public Definition { if(hp<1||hp>1000||attack<1||attack>100||!Set.of("dash","mend").contains(ability)||cost<1||cost>100||cooldown<1||cooldown>900||effect<1||effect>200)throw new IllegalArgumentException("Arquetipo invalido"); }
    }
    private Archetypes() { }
    public static Definition get(String id) {
        if(!Set.of("guardian","mystic").contains(id))throw new IllegalArgumentException("Arquetipo desconhecido");
        return Builtin.VALUES.get(id);
    }
    private static final class Builtin {static final Map<String,Definition> VALUES=Map.of("guardian",read("guardian"),"mystic",read("mystic"));}
    private static Definition read(String id) {
        try(InputStream in=Archetypes.class.getResourceAsStream("/rpg/archetypes.properties")) {
            if(in==null)throw new IOException("Arquetipos ausentes"); Properties p=new Properties();p.load(in);
            String[] v=p.getProperty(id).split("\\|");
            return new Definition(id,Integer.parseInt(v[0]),Integer.parseInt(v[1]),v[2],Integer.parseInt(v[3]),Integer.parseInt(v[4]),Integer.parseInt(v[5]));
        }catch(IOException e){throw new IllegalStateException(e);}
    }
}
