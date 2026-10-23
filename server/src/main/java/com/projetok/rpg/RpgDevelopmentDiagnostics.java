package com.projetok.rpg;

import java.util.*;

/** Diagnóstico apenas de snapshots residentes, sem I/O ou controles sobre saves pessoais. */
public final class RpgDevelopmentDiagnostics {
    private RpgDevelopmentDiagnostics() { }
    private static long flag(RpgSave state,String key){String prefix=key+":";return state.flags().stream().filter(f->f.startsWith(prefix)).mapToLong(f->Long.parseLong(f.substring(prefix.length()))).findFirst().orElse(0);}
    public static List<String> lines(RpgWorld.View v,String map){
        var state=v.state();var domain=DomainProgression.read(state.flags());long cooldown=Math.max(0,flag(state,"ability-until")-v.ticks());
        return List.of("DESENVOLVIMENTO | sessão sintética", "Mapa: "+map+" | região: "+state.region(),
            String.format(Locale.ROOT,"Posição lógica: %.2f, %.2f | tick %d",v.x(),v.y(),v.ticks()),
            "Raça: "+v.identity().race()+" | nível: "+v.level(),"Domínio: "+(domain.chosen()?domain.domain():"não escolhido (nível 15+)"),
            "XP geral: "+state.xp()+" | XP domínio: "+domain.xpText()+" [meias unidades: "+domain.xpHalfUnits()+"]",
            "Shunpo: "+(v.level()<DomainProgression.SHUNPO_LEVEL?"bloqueado (nível 20+)":cooldown>0?"recarga compartilhada":"habilitado")+" | ticks restantes: "+cooldown,
            "Vigor: "+flag(state,"stamina")+"/1000 | esquiva até: "+flag(state,"evade-until"),
            "Eventos recentes: "+v.events().size()+" | NPC: "+v.activeNpc(),"Runtime: "+v.message());
    }
}
