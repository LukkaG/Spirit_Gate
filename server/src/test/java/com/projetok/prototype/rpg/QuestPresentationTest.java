package com.projetok.prototype.rpg;
import com.projetok.rpg.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class QuestPresentationTest {
    @Test void rewardsComeFromCatalogAndUnavailableObjectivesStayHidden(){
        var world=new RpgWorld();var before=world.save();
        for(var quest:world.snapshot().journal()) {
            String text=ExplorationWindow.describe(quest);
            if(quest.state()==QuestService.State.UNAVAILABLE)assertFalse(text.contains(quest.objective()));
            else {var reward=new QuestService(RpgContent.builtin()).definition(quest.id());assertTrue(text.contains(reward.xp()+" XP"));assertTrue(text.contains(quest.objective()));}
        }
        assertEquals(before,world.save());assertEquals(world.snapshot().journal(),RpgWorld.restore(before).snapshot().journal());
    }
}
