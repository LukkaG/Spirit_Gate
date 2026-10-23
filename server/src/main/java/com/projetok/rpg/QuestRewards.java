package com.projetok.rpg;

/** Pré-validação transacional; o chamador só publica o plano após todas as verificações. */
public final class QuestRewards {
    private QuestRewards() { }
    public record Plan(InventoryService inventory,int coins,long xp,DomainProgression domain) { }
    /** Usa cópia do inventário e progressões reais, incluindo overflow de XP fracionária. */
    public static Plan prepare(InventoryService inventory,int coins,long xp,DomainProgression domain,QuestService.Definition definition) {
        QuestService.validateDefinition(RpgContent.builtin(),definition);
        var copy=inventory.copy();
        for(var objective:definition.objectives())if(objective.type().equals("collect")&&!copy.remove(objective.target(),objective.count()))throw new IllegalStateException("Itens insuficientes para concluir");
        int nextCoins=Math.addExact(coins,definition.coins());if(coins<0||nextCoins>1_000_000)throw new IllegalStateException("Limite técnico de moedas atingido");
        long nextXp=RpgProgression.builtin().award(xp,definition.xp());
        DomainProgression nextDomain=domain.award(definition.xp());
        if(!copy.add(definition.item(),definition.quantity()))throw new IllegalStateException("Libere espaço para a recompensa");
        return new Plan(copy,nextCoins,nextXp,nextDomain);
    }
}
