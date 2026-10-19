package com.projetok.prototype.rpg;
import com.projetok.prototype.*;
import com.projetok.domain.movement.CollisionCategory;
import java.util.*;
/** Autoria sintetica reutilizando o mapa e renderer ja existentes. */
public final class RpgMaps {
    private RpgMaps() { }
    public static LocalArenaMap region(String id) {
        int floor=switch(id){case "village"->0x31594c;case "forest"->0x183e2c;case "ruins"->0x28263d;default->throw new IllegalArgumentException("Regiao ausente");};
        List<LocalArenaMap.Visual> visuals=new ArrayList<>();
        visuals.add(new LocalArenaMap.Visual("floor",0,0,960,640,0,floor));
        visuals.add(new LocalArenaMap.Visual("road",0,303,960,34,1,id.equals("ruins")?0x777080:0x9b8a62));
        visuals.add(new LocalArenaMap.Visual("plaza",110,280,155,190,1,id.equals("village")?0x8e9174:floor));
        visuals.add(new LocalArenaMap.Visual("solid",380,180,120,120,2,id.equals("village")?0xad8060:id.equals("forest")?0x617969:0x847999));
        visuals.add(new LocalArenaMap.Visual("roof",370,170,140,24,3,id.equals("village")?0x733f3e:0x42584d));
        if(id.equals("forest"))for(int i=0;i<9;i++)visuals.add(new LocalArenaMap.Visual("tree"+i,50+i*95,70,36,45,2,0x286346));
        if(id.equals("ruins"))for(int i=0;i<5;i++)visuals.add(new LocalArenaMap.Visual("stone"+i,580+i*65,120,24,30,2,0x817689));
        var physical=List.of(new LocalArenaMap.Physical("boundary",0,0,960,640,CollisionCategory.WORLD_BOUNDARY,null),new LocalArenaMap.Physical("solid",380,180,120,120,CollisionCategory.SOLID_OBSTACLE,null));
        return new LocalArenaMap(960,640,140,320,visuals,physical);
    }
}
