package com.projetok.multiplayer;

import com.projetok.prototype.rpg.RpgPlayerSprites;
import com.projetok.prototype.rpg.RaceSpriteCatalog;
import java.nio.file.Path;

/** Escolhe o atlas pela raca recebida do servidor; nao altera estado de jogo. */
public final class NetworkPlayerVisuals {
    /** Recursos da escolha; provisional indica atlas emprestado, nao aprovacao artistica. */
    public record RaceRenderInfo(String race,String atlasRace,boolean available,boolean provisional,String detail) { }
    // As artes possuem densidades distintas: Human tem cerca de 135 px de altura,
    // enquanto Reaper tem 54 px. A referencia e a altura visivel, nao o canvas.
    public static final double LEGACY_SCALE=0.4;
    public static final double REAPER_SCALE=1.0;
    private final RpgPlayerSprites legacy;
    private final RpgPlayerSprites reaper;
    public NetworkPlayerVisuals(Path folder,RpgPlayerSprites legacy) {
        var human=RaceSpriteCatalog.get("human");
        this.legacy=legacy==null?null:legacy.withScale(human.sourceScale()).onLogicalCanvas(human.width(),human.height(),human.pivotX(),human.pivotY());
        var definition=RaceSpriteCatalog.get("reaper");
        var loaded=RpgPlayerSprites.loadOptional(folder,definition.clips().get(RaceSpriteCatalog.Action.WALK).frameMillis(),definition.sourceScale());
        reaper=loaded==null?null:loaded.withScale(REAPER_SCALE);
    }
    public RpgPlayerSprites sprites(String race) {
        return "reaper".equals(race)?reaper:legacy;
    }
    public boolean usesReaper(String race) {return "reaper".equals(race)&&reaper!=null;}
    public int frameMillis(String race) {return definition(race).clips().get(RaceSpriteCatalog.Action.WALK).frameMillis();}
    /** Identidade vazia pertence apenas ao caminho legado sem aparência negociada. */
    public RaceSpriteCatalog.Definition definition(String race) {return RaceSpriteCatalog.get(race==null||race.isEmpty()?"human":race);}
    public boolean supportsAccessories(String race) {return definition(race).accessories()&&sprites(race)!=null;}
    /** Informa limitações e mantém a escolha de atlas igual na prévia e no mundo. */
    public RaceRenderInfo renderInfo(String race) {
        var definition=com.projetok.rpg.CharacterRaces.get(race);
        var spriteDefinition=definition(race);
        if(race.equals("reaper"))return new RaceRenderInfo(race,"reaper",reaper!=null,false,
            reaper==null?"Reaper indisponível: o atlas necessário não foi carregado. Fallback geométrico no mundo; entrada bloqueada.":
                "Reaper: sprites próprias instaladas. Acessórios compatíveis ainda indisponíveis.");
        boolean provisional=spriteDefinition.provisional();
        if(legacy==null)return new RaceRenderInfo(race,"human",false,provisional,
            definition.name()+" indisponível: o atlas Human necessário não foi carregado.");
        return new RaceRenderInfo(race,"human",true,provisional,provisional?
            definition.name()+": aparência provisória Human. Esta raça ainda não possui atlas próprio.":
            "Human: sprites e acessórios experimentais instalados.");
    }
}
