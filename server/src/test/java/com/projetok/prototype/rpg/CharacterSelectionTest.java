package com.projetok.prototype.rpg;

import com.projetok.rpg.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import javax.swing.*;
import java.nio.file.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class CharacterSelectionTest {
    @TempDir Path folder;
    @Test void loadingEmptyValidCorruptAndKeyboardActions() throws Exception {
        var repo=new CharacterSaves(folder);var state=new RpgWorld(CharacterIdentity.create("Viajante","mystic","amber")).save();new LocalSaveStore(repo.newFile(state.identity())).save(state);Files.writeString(folder.resolve("broken.properties"),"broken");var slots=repo.list();
        SwingUtilities.invokeAndWait(()->{
            var view=new CharacterSelection();assertFalse(view.play.isEnabled());assertFalse(view.create.isEnabled());
            view.loaded(java.util.List.of());assertTrue(view.create.isEnabled());assertTrue(view.status.getText().contains("Nenhum"));
            view.loaded(slots);int valid=slots.get(0).state()!=null?0:1;view.list.setSelectedIndex(valid);assertTrue(view.play.isEnabled());assertTrue(CharacterSelection.describe(slots.get(valid)).contains("Nível 1"));assertTrue(CharacterSelection.describe(slots.get(valid)).contains("Místico"));
            AtomicInteger played=new AtomicInteger(),back=new AtomicInteger();view.play.addActionListener(e->played.incrementAndGet());view.back.addActionListener(e->back.incrementAndGet());view.list.getActionMap().get("play").actionPerformed(null);view.getActionMap().get("back").actionPerformed(null);assertEquals(1,played.get());assertEquals(1,back.get());
            view.list.setSelectedIndex(1-valid);assertFalse(view.play.isEnabled());view.list.getActionMap().get("play").actionPerformed(null);assertEquals(1,played.get());
            view.preview.setSize(300,320);view.preview.paint(new java.awt.image.BufferedImage(300,320,2).getGraphics());
        });
    }
    @Test void archiveRequiresExplicitConfirmationAndPreservesBackupAndRecovery() throws Exception {
        var repo=new CharacterSaves(folder);var state=new RpgWorld().save();Path file=repo.newFile(state.identity());var store=new LocalSaveStore(file);store.save(state);store.save(state);Path backup=file.resolveSibling(file.getFileName()+".bak");byte[] original=Files.readAllBytes(backup);var slot=repo.list().get(0);
        assertThrows(IllegalArgumentException.class,()->repo.archive(slot,"sim"));assertTrue(Files.exists(file));
        Path archived=repo.archive(slot,"ARQUIVAR");assertTrue(Files.exists(archived));assertFalse(Files.exists(file));assertArrayEquals(original,Files.readAllBytes(backup));assertTrue(repo.list().isEmpty());assertEquals(state,new LocalSaveStore(archived).load());
        store.save(state);Files.writeString(file,"corrupt");var recovered=repo.list().stream().filter(CharacterSaves.Slot::backup).findFirst().orElseThrow();assertThrows(java.io.IOException.class,()->repo.archive(recovered,"ARQUIVAR"));Path recovery=repo.recoveryFile(state.identity());new LocalSaveStore(recovery).save(recovered.state());assertEquals("corrupt",Files.readString(file));assertArrayEquals(original,Files.readAllBytes(backup));
    }
    @Test void offlineEntryContainsNoPasswordControls() throws Exception {
        SwingUtilities.invokeAndWait(()->{var entry=new SpiritGateEntry(null,()->{});assertEquals("Lembrar perfil local",entry.remember.getText());assertFalse(hasPassword(entry));});
    }
    private boolean hasPassword(java.awt.Container c){for(var child:c.getComponents()){if(child instanceof JPasswordField)return true;if(child instanceof JLabel l&&l.getText().equals("Password"))return true;if(child instanceof java.awt.Container nested&&hasPassword(nested))return true;}return false;}
}
