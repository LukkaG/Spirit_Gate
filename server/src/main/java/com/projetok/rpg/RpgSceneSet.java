package com.projetok.rpg;
import com.projetok.domain.entity.Position;
import com.projetok.domain.movement.CollisionService;
import java.io.*;
import java.util.*;

/** Configuracao opt-in de sessoes de autoria; nao muda o formato de save legado. */
public record RpgSceneSet(String initialRegion,Map<String,Geometry> maps,Map<String,String> enemies,
        Map<String,String> npcs,Map<String,String> objects,Map<String,String> portals) {
    /** Geometria de uma regiao de conteudo, limitada ao DTO atual 960x640. */
    public record Geometry(CollisionService.Space space,Position spawn) { }
    public RpgSceneSet {
        maps=Map.copyOf(maps);enemies=Map.copyOf(enemies);npcs=Map.copyOf(npcs);objects=Map.copyOf(objects);portals=Map.copyOf(portals);
        if(maps.isEmpty()||maps.size()>3||!maps.containsKey(initialRegion)||enemies.size()>64||npcs.size()>16||objects.size()>128||portals.size()>64)throw new IllegalArgumentException("Cenario excede limites do RPG");
        for(var e:maps.entrySet()){RpgContent.builtin().get("region",e.getKey());if(e.getValue().space().width()!=960||e.getValue().space().height()!=640)throw new IllegalArgumentException("Autoria RPG inicial exige 960x640");}
    }
    /** Carrega os dados nativos existentes para construtores padrao. */
    public static Map<String,String> entries(String resource) {
        try(InputStream in=RpgSceneSet.class.getResourceAsStream("/rpg/"+resource+".properties")) {
            if(in==null)throw new IOException("Recurso ausente: "+resource);Properties p=new Properties();p.load(in);Map<String,String> map=new TreeMap<>();p.forEach((k,v)->map.put(k.toString(),v.toString()));return Map.copyOf(map);
        }catch(IOException error){throw new IllegalStateException(error);}
    }
}
