package com.projetok.prototype.rpg;

import com.projetok.rpg.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class CharacterSelectionVisualTest {
    @TempDir Path folder;

    @Test void cardsSummaryBackupAndActionsPreserveTheCatalogContract() throws Exception {
        var repo=new CharacterSaves(folder);
        var state=new RpgWorld(CharacterIdentity.create("Sentinela","reaper","azure")).save();
        Path file=repo.newFile(state.identity());var store=new LocalSaveStore(file);
        store.save(state);store.save(state);Files.writeString(file,"corrupt");
        var slots=repo.list();var backup=slots.stream().filter(CharacterSaves.Slot::backup).findFirst().orElseThrow();
        SwingUtilities.invokeAndWait(()->{
            var view=new CharacterSelection();view.loaded(slots);view.list.setSelectedValue(backup,true);
            assertTrue(view.play.isEnabled());assertFalse(view.delete.isEnabled());
            assertEquals("Sentinela",view.name.getText());assertTrue(view.details.getText().contains("Reaper"));
            assertTrue(view.style.getText().contains("Domínio no nível 15"));
            var combat=RpgCombatStatus.from(RpgWorld.restore(state).snapshot());
            assertEquals("HP "+combat.hp()+" / "+combat.maxHp()+"   •   Vigor "+combat.stamina()/10+" / 100",view.resources.getText());
            var renderer=view.list.getCellRenderer();
            Component normal=renderer.getListCellRendererComponent(view.list,backup,1,false,false);
            Color normalColor=normal.getBackground();
            Component selected=renderer.getListCellRendererComponent(view.list,backup,1,true,false);
            assertNotEquals(normalColor,selected.getBackground());
            BufferedImage selectedImage=paint(selected,270,80);
            Component focused=renderer.getListCellRendererComponent(view.list,backup,1,true,true);
            assertNotEquals(selectedImage.getRGB(0,0),paint(focused,270,80).getRGB(0,0));
            for(JButton button:new JButton[]{view.play,view.create,view.back}){
                AtomicInteger calls=new AtomicInteger();button.addActionListener(e->calls.incrementAndGet());button.doClick();assertEquals(1,calls.get());
            }
            view.list.setSelectedValue(slots.stream().filter(s->s.state()==null).findFirst().orElseThrow(),true);
            assertFalse(view.play.isEnabled());assertTrue(view.delete.isEnabled());
            AtomicInteger archive=new AtomicInteger();view.delete.addActionListener(e->archive.incrementAndGet());view.delete.doClick();assertEquals(1,archive.get());
            assertEquals("Arquivo preservado",view.name.getText());
            view.loaded(java.util.List.of());assertTrue(view.empty.isVisible());assertFalse(view.play.isEnabled());assertFalse(view.delete.isEnabled());
        });
        assertEquals("corrupt",Files.readString(file));assertNotNull(new LocalSaveStore(file.resolveSibling(file.getFileName()+".bak")).load());
    }

    @Test void responsiveRenderingWithSpritesFallbackAndScrollableRoster() throws Exception {
        var repo=new CharacterSaves(folder);var state=new RpgWorld().save();
        new LocalSaveStore(repo.newFile(state.identity())).save(state);var slot=repo.list().get(0);
        Path art=Path.of("../assets/dev/player-base");if(!Files.isDirectory(art))art=RpgPlayerSprites.defaultFolder();
        var sprites=RpgPlayerSprites.load(art,250);
        SwingUtilities.invokeAndWait(()->{
            var view=new CharacterSelection();view.loaded(java.util.Collections.nCopies(20,slot));
            for(int[] size:new int[][]{{1000,620},{640,480},{800,600}}){
                for(var skin:new RpgPlayerSprites[]{null,sprites}){
                    view.preview.sprites=skin;paint(view,size[0],size[1]);
                    for(JComponent c:new JComponent[]{view.play,view.create,view.delete,view.techniques,view.back,view.preview,view.resources}){
                        Rectangle bounds=SwingUtilities.convertRectangle(c.getParent(),c.getBounds(),view);
                        assertTrue(bounds.width>0&&bounds.height>0,bounds.toString());
                        assertTrue(new Rectangle(0,0,size[0],size[1]).contains(bounds),bounds.toString());
                    }
                    assertTrue(view.play.getWidth()>view.play.getPreferredSize().width);
                    assertTrue(view.resources.getWidth()>=view.resources.getPreferredSize().width);
                    assertTrue(view.list.getScrollableTracksViewportWidth());
                }
            }
            assertNotNull(view.list.getInputMap().get(KeyStroke.getKeyStroke("DOWN")));
            view.list.setSelectedIndex(0);
            Object down=view.list.getInputMap().get(KeyStroke.getKeyStroke("DOWN"));
            view.list.getActionMap().get(down).actionPerformed(new java.awt.event.ActionEvent(view.list,0,""));
            assertEquals(1,view.list.getSelectedIndex());assertSame(view.list.getSelectedValue(),view.preview.slot);
            assertTrue(view.list.getFocusTraversalKeysEnabled());
            assertNotNull(view.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT).get(KeyStroke.getKeyStroke("ESCAPE")));
        });
    }
    static BufferedImage paint(Component c,int w,int h){c.setSize(w,h);if(c instanceof Container container)SpiritGateEntry.layout(container);var image=new BufferedImage(w,h,BufferedImage.TYPE_INT_RGB);Graphics2D g=image.createGraphics();c.printAll(g);g.dispose();return image;}
}
