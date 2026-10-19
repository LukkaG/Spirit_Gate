package com.projetok.rpg;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class DialogueTest {
    @Test void branchesCancelRejectInvalidAndRestart(){var w=new RpgWorld();assertFalse(w.choose(0));w.interact();assertFalse(w.choose(99));assertTrue(w.choose(0));assertEquals("elder.help",w.snapshot().dialogue().id());assertTrue(w.choose(0));assertNull(w.snapshot().dialogue());w.interact();assertEquals("elder.start",w.snapshot().dialogue().id());w.closeDialogue();assertNull(w.snapshot().dialogue());assertThrows(IllegalArgumentException.class,()->new DialogueService().node("missing",c->true));}
}
