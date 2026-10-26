package com.projetok.domain.movement;

/** Stable ground circle measured from the original 20 poses at half scale.
 * Combat ranges and cosmetic layers do not participate in this contract. */
public record CharacterFootprint(String id, float radius, float offsetX, float offsetY,
                                 double referenceScale, String shape) {
    public static final CharacterFootprint RPG_NETWORK =
            new CharacterFootprint("original-ground-v1",12,0,0,.5,"circle");
    public CharacterFootprint {
        if(id==null||id.isBlank()||!Float.isFinite(radius)||radius<=0
                ||!Float.isFinite(offsetX)||!Float.isFinite(offsetY)
                ||!Double.isFinite(referenceScale)||referenceScale<=0||!"circle".equals(shape))
            throw new IllegalArgumentException("Invalid footprint");
    }
}
