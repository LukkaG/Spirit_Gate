package com.projetok.prototype.rpg;

import com.projetok.prototype.DirectionalAnimation.Direction;
import com.projetok.rpg.CharacterRaces;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Contrato de apresentação dos atlas existentes; não define hitboxes ou combate. */
public final class RaceSpriteCatalog {
    public enum Action { IDLE, WALK, RUN, ATTACK, HIT, DEATH }
    public record Clip(int frames, int frameMillis) { }
    /** Pivô e canvas referem-se ao raster residente, após normalização do Human. */
    public record Definition(String race, String atlasRace, boolean provisional,
                             boolean accessories, double sourceScale, int width, int height,
                             int pivotX, int pivotY, Set<Direction> directions, Map<Action, Clip> clips) {
        public Definition {
            directions=Set.copyOf(directions);clips=Map.copyOf(clips);
        }
        public boolean supports(Action action) { return clips.containsKey(action); }
    }
    private static final Set<Direction> DIRECTIONS=Set.of(Direction.values());
    private static final List<Definition> VALUES=CharacterRaces.all().stream().map(race -> {
        boolean reaper=race.id().equals("reaper");
        return new Definition(race.id(),reaper?"reaper":"human",
            !reaper&&!race.id().equals("human"),!reaper,reaper?1.0:0.4,
            64,64,32,60,DIRECTIONS,Map.of(Action.IDLE,new Clip(1,reaper?125:250),
                Action.WALK,new Clip(reaper?8:4,reaper?125:250)));
    }).toList();
    private RaceSpriteCatalog() { }
    public static List<Definition> all() { return VALUES; }
    public static Definition get(String race) {
        return VALUES.stream().filter(value -> value.race().equals(race)).findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Raça sem catálogo: "+race));
    }
}
