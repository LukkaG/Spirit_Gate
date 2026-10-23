package com.projetok.studio;

import com.projetok.rpg.*;
import java.awt.*;
import java.nio.file.*;
import javax.swing.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

/** Fluxo visual de autoria com catálogos reais, sem personagens pessoais. */
class QuestBuilderTest {
    @TempDir Path project;
    @Test void editingRaceDomainObjectivesAndRoundTripIsLossless()throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless());
        SwingUtilities.invokeAndWait(()->{var ui=new QuestBuilder(project);try{
            ui.name.setText("Missão visual");ui.race.setSelectedItem("reaper");assertEquals(5,ui.domain.getItemCount());ui.domain.setSelectedItem("water");ui.level.setValue(15);var book=ui.book();book.requireValid();
            try{Path path=project.resolve("visual.sgquest");QuestDocuments.save(path,book);assertEquals(book,QuestDocuments.load(path));}catch(Exception e){throw new IllegalStateException(e);}
            ui.race.setSelectedItem("human");assertEquals(3,ui.domain.getItemCount());assertEquals("",ui.domain.getSelectedItem());assertEquals(5,ui.race.getItemCount());
            ui.duplicate();assertEquals(2,ui.book().quests().size());assertNotEquals(ui.quests.get(0).id(),ui.quests.get(1).id());
        }finally{ui.dispose();}});
    }
}
