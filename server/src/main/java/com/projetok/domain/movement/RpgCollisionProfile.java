package com.projetok.domain.movement;

import java.util.ArrayList;
import java.util.List;

/** Session-only category contract. Combat and enemy bodies keep their own rules.
 * A horizontal capsule (22x12 half extents) against scenery is equivalent to
 * radius 12 against rectangles dilated by 10 horizontally. */
public final class RpgCollisionProfile {
    public static final float PLAYER_RADIUS=CharacterFootprint.RPG_NETWORK.radius();
    public static final float SCENERY_HALF_WIDTH=22;
    public static final float SCENERY_PADDING_X=SCENERY_HALF_WIDTH-PLAYER_RADIUS;
    private RpgCollisionProfile(){}
    public static CollisionService.Obstacle effective(CollisionService.Obstacle raw){
        return new CollisionService.Obstacle(raw.x()-SCENERY_PADDING_X,raw.y(),
                raw.width()+2*SCENERY_PADDING_X,raw.height(),raw.category());
    }
    /** Apply once to raw map geometry, never to an already prepared space. */
    public static CollisionService.Space scenery(CollisionService.Space raw){
        return scenery(raw,SessionCollisionProfile.DEFAULT);
    }
    public static CollisionService.Space scenery(CollisionService.Space raw,SessionCollisionProfile profile){
        float padding=profile.sceneryHalfWidth()-profile.actorRadius();
        return boundaries(new CollisionService.Space(raw.width(),raw.height(),raw.obstacles().stream().map(o->new CollisionService.Obstacle(o.x()-padding,o.y(),o.width()+2*padding,o.height(),o.category())).toList(),raw.bodies()),profile.actorRadius());
    }
    /** Historical wire/save coordinate limits, without dilating actor bodies. */
    public static CollisionService.Space boundaries(CollisionService.Space source){
        return boundaries(source,PLAYER_RADIUS);
    }
    public static CollisionService.Space boundaries(CollisionService.Space source,float radius){
        float inset=18-radius;if(inset==0)return source;var obstacles=new ArrayList<>(source.obstacles());
        for(var border:List.of(new CollisionService.Obstacle(0,0,source.width(),inset),
                new CollisionService.Obstacle(0,source.height()-inset,source.width(),inset),
                new CollisionService.Obstacle(0,0,inset,source.height()),
                new CollisionService.Obstacle(source.width()-inset,0,inset,source.height())))
            if(!obstacles.contains(border))obstacles.add(border);
        return new CollisionService.Space(source.width(),source.height(),obstacles,source.bodies());
    }
}
