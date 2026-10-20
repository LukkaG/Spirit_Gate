package com.projetok.studio;
import com.projetok.rpg.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class NpcEditingTest {
    @Test void realNpcNameDialogueAndInvalidReferences(){var e=new WorldDocument.Element("ancia",WorldDocument.Kind.NPC,"","",new WorldDocument.Crop(0,0,32,32),200,320,32,32,"interaction",true,false,"elder","FRONT",Map.of("npc.name","Aurora | Guia","npc.dialogue","elder.help","npc.quest","first_steps"));var service=new NpcService(RpgContent.builtin(),Map.of("elder",NpcEditing.definition(e,"village")));assertEquals("Aurora | Guia",service.views("village").get(0).name());assertEquals("elder.help",service.views("village").get(0).dialogue());assertThrows(IllegalArgumentException.class,()->new NpcService(RpgContent.builtin(),Map.of("elder","village|200|320|trader.start|QQ")));assertTrue(new DialogueService().ids().contains("elder.start"));}
}
