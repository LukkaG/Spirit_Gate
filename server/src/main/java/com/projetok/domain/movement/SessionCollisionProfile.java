package com.projetok.domain.movement;

import java.util.Map;

/** Versioned content, never a personal attribute. Offsets/category disabling are
 * deliberately unsupported by the current circle/capsule solver. */
public record SessionCollisionProfile(String id,int version,float actorRadius,float sceneryHalfWidth) {
    public static final SessionCollisionProfile DEFAULT=new SessionCollisionProfile("original-ground",1,12,22);
    public SessionCollisionProfile {
        if(id==null||!id.matches("[a-z][a-z0-9_-]{0,63}")||version!=1||!Float.isFinite(actorRadius)||actorRadius<8||actorRadius>18
                ||!Float.isFinite(sceneryHalfWidth)||sceneryHalfWidth<actorRadius||sceneryHalfWidth>28)
            throw new IllegalArgumentException("Perfil: raio 8..18; meia largura entre raio e 28; versao 1");
    }
    public Map<String,String> metadata(){return Map.of("collision.id",id,"collision.version","1","collision.radius",""+actorRadius,"collision.scenery",""+sceneryHalfWidth);}
    public String context(){return "synthetic-session";}
    public String shape(){return "actors-circle/scenery-horizontal-capsule";}
    public float offsetX(){return 0;}
    public float offsetY(){return 0;}
    public String spriteReference(){return "original-player-base@0.5";}
    public static SessionCollisionProfile read(Map<String,String> metadata){
        for(String key:metadata.keySet())if(key.startsWith("collision.")&&!DEFAULT.metadata().containsKey(key))throw new IllegalArgumentException("Parametro fisico nao suportado: "+key);
        return new SessionCollisionProfile(metadata.getOrDefault("collision.id",DEFAULT.id),Integer.parseInt(metadata.getOrDefault("collision.version","1")),
                Float.parseFloat(metadata.getOrDefault("collision.radius","12")),Float.parseFloat(metadata.getOrDefault("collision.scenery","22")));
    }
}
