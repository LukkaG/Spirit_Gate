package com.projetok.rpg;

import java.util.Locale;
import java.util.stream.Collectors;

/** Textos de ajuda compartilhados pela interface e pelo relatório, sem alterar regras de combate. */
public final class RpgSkillDescriptions {
    private RpgSkillDescriptions() { }

    /** Explica a entrada necessária para conjurar, incluindo destinatários especiais de Luz. */
    public static String activation(RpgSkillCatalog.Definition skill) {
        if(skill.kit().equals("light"))return switch(skill.mechanics().pattern()){
            case HEAL -> "Selecione um aliado para curar ou um mob para causar dano. Sem alvo, cura você.";
            case REVIVE -> "Selecione um aliado derrotado. O local do corpo precisa estar livre para reviver.";
            case CLEANSE -> "Selecione um aliado vivo. Sem alvo, purifica você.";
            default -> "Ativação direta ao redor de você; não exige alvo.";
        };
        return switch(skill.targeting()){
            case TARGET -> "Selecione um inimigo com TAB ou clique e ative a habilidade.";
            case ALLY -> "Selecione um aliado e ative a habilidade.";
            case SELF -> "Ativação direta; não exige selecionar um alvo.";
            case LINE,CONE -> "Smartcast: sai imediatamente para frente, na direção em que você olha (quatro direções).";
            case GROUND -> "Escolha a área com mouse, WASD ou setas; confirme com ENTER ou a mesma tecla. ESC cancela.";
        };
    }

    /** Resume os controles e danos periódicos declarados no catálogo, usando segundos do servidor. */
    public static String effects(RpgSkillCatalog.Definition skill) {
        return skill.effects().stream().map(effect->switch(effect.type()){
            case ROOT -> "Imobiliza por "+seconds(effect.durationTicks())+"; permite atacar e usar habilidades.";
            case STUN -> "Atordoa por "+seconds(effect.durationTicks())+"; impede movimento, ataques e habilidades.";
            case SLOW -> "Reduz a velocidade em "+(100-effect.potencyPermille()/10)+"% por "+seconds(effect.durationTicks())+".";
            case BLEED,BURN -> (effect.type()==RpgSkillCatalog.EffectType.BLEED?"Sangramento":"Queimadura")+" por "+seconds(effect.durationTicks())+": "+effect.baseDamage()+" de dano-base a cada "+seconds(effect.intervalTicks())+".";
        }).collect(Collectors.joining(" "));
    }

    /** Formata ticks de 30 Hz em segundos, mantendo textos legíveis e independentes do locale da máquina. */
    public static String seconds(int ticks) {
        return String.format(Locale.ROOT,"%.2f",ticks/30.0).replaceAll("0+$", "").replaceAll("\\.$", "").replace('.',',')+" s";
    }
}
