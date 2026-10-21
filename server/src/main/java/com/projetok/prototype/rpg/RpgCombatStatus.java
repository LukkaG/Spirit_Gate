package com.projetok.prototype.rpg;
import com.projetok.rpg.*;
import java.util.Locale;

/** Modelo puro do HUD, calculado dos valores publicados pelo mundo. */
public record RpgCombatStatus(int hp,int maxHp,int stamina,long attackTicks,long abilityTicks,boolean abilityReady,boolean lowHp) {
    /** Le valores de save existentes; nao cria selecao de alvo nem recursos paralelos. */
    public static RpgCombatStatus from(RpgWorld.View v) {
        int stamina=(int)value(v,"stamina",1000);long attack=Math.max(0,value(v,"attack-until",0)-v.ticks());
        long ability=Math.max(0,value(v,"ability-until",0)-v.ticks());
        return new RpgCombatStatus(v.state().hp(),v.maxHp(),stamina,attack,ability,
            v.level()>=RpgWorld.SHUNPO_LEVEL&&v.state().hp()>0&&ability==0&&stamina>=RpgWorld.SHUNPO_COST*10,
            v.state().hp()>0&&v.state().hp()*4<=v.maxHp());
    }
    private static long value(RpgWorld.View v,String prefix,long fallback){return v.state().flags().stream().filter(f->f.startsWith(prefix+":" )).mapToLong(f->Long.parseLong(f.substring(prefix.length()+1))).findFirst().orElse(fallback);}
    /** Duracao restante arredondada para decimos; usa 30 ticks por segundo. */
    public static String cooldown(long ticks){return ticks<=0?"Pronto":String.format(Locale.ROOT,"%.1f s",ticks/30.0);}
    /** Resumo curto para janelas pequenas. */
    public String text(){return "Golpe: "+cooldown(attackTicks)+" | Shunpo: "+cooldown(abilityTicks)+(abilityReady?"":" (indisponível)")+(lowHp?" | HP BAIXO":"");}
}
