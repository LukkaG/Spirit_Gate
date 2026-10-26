package com.projetok.domain.movement;

import java.util.List;

/** Shared geometry/presentation metadata of the synthetic RPG session, not a wire DTO. */
public final class RpgSessionGeometry {
    public record Structure(String id,CollisionService.Obstacle footprint,float projectionHeight){
        public Structure{if(id==null||id.isBlank()||footprint==null||!Float.isFinite(projectionHeight)||projectionHeight<0)throw new IllegalArgumentException("Invalid structure");}
        public float supportY(){return footprint.y()+footprint.height();}
        public float visualTop(){return footprint.y()-projectionHeight;}
    }
    public static final Structure CENTRAL=new Structure("central-gray-block",
            new CollisionService.Obstacle(380,180,120,120),48);
    private static final List<Structure> STRUCTURES=List.of(CENTRAL);
    private static final CollisionService.Space RAW=new CollisionService.Space(960,640,List.of(CENTRAL.footprint()),List.of());
    private static final CollisionService.Space PLAYER=RpgCollisionProfile.scenery(RAW);
    private RpgSessionGeometry(){}
    public static List<Structure> structures(){return STRUCTURES;}
    public static CollisionService.Space raw(){return RAW;}
    public static CollisionService.Space player(){return PLAYER;}
}
