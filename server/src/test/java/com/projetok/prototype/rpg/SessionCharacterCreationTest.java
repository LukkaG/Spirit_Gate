package com.projetok.prototype.rpg;

import com.projetok.multiplayer.NetworkPlayerVisuals;
import com.projetok.multiplayer.SessionCharacterSelection;
import java.awt.Container;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import javax.swing.JComboBox;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class SessionCharacterCreationTest {
    private static final Path ASSETS=Path.of("../assets/dev");
    private static NetworkPlayerVisuals visuals;
    private static CosmeticCatalog catalog,previewCatalog;

    @BeforeAll static void resources()throws Exception {
        visuals=new NetworkPlayerVisuals(ASSETS.resolve("reaper"),RpgPlayerSprites.load(ASSETS.resolve("player-base"),250,.5));
        catalog=CosmeticCatalog.load(ASSETS.resolve("generated/corujao-iv-v1"));
        previewCatalog=catalog.onLogicalCanvas(64,64,32,60,NetworkPlayerVisuals.LEGACY_SCALE);
    }

    private static SessionCharacterCreation form(){return new SessionCharacterCreation(visuals,catalog,previewCatalog);}

    @Test void validatesNameAndRecognizesAllRacesWithExplicitFallback()throws Exception {
        SwingUtilities.invokeAndWait(()->{
            var form=form();assertEquals(4,form.races.size());form.setPlayerName("!");
            assertThrows(IllegalArgumentException.class,form::selection);
            form.setPlayerName("  Herói da sessão  ");
            for(String race:new String[]{"reaper","human","aetheri","spirit"}) {
                form.setSelectedRace(race);var selection=form.selection();
                assertEquals(race,selection.race());assertEquals("Herói da sessão",selection.name());
                var info=visuals.renderInfo(race);assertTrue(info.available());
                if(race.equals("aetheri")||race.equals("spirit")) {
                    assertTrue(info.provisional());assertEquals("human",info.atlasRace());
                    assertTrue(form.resourceNotice().contains("aparência provisória Human"));
                    assertTrue(form.resourceNotice().contains("não possui atlas próprio"));
                }else assertFalse(info.provisional());
            }
            assertThrows(IllegalArgumentException.class,()->form.setSelectedRace("unknown"));
        });
    }

    @Test void onlyOffersCompatibleCosmeticsAndCanRecoverTheSelection()throws Exception {
        SwingUtilities.invokeAndWait(()->{
            var form=form();form.setPlayerName("Aventureiro");form.setSelectedRace("human");
            assertTrue(form.hair.isEnabled());assertTrue(form.clothing.isEnabled());
            select(form.hair,"hair_short");select(form.clothing,"tunic");
            var chosen=form.selection();assertEquals("hair_short",chosen.hair());assertEquals("tunic",chosen.clothing());
            form.setSelectedRace("reaper");assertFalse(form.hair.isEnabled());assertFalse(form.clothing.isEnabled());
            assertEquals("none",form.selection().hair());assertEquals("none",form.selection().clothing());
            assertTrue(form.resourceNotice().contains("Acessórios compatíveis ainda indisponíveis"));
            form.setSelection(chosen);assertEquals(chosen,form.selection());
            form.setSelectedRace("aetheri");assertEquals("hair_short",form.selection().hair());
            assertTrue(form.hair.isEnabled());
            form.setSelectedRace("reaper");select(form.hair,"hair_short");
            assertThrows(IllegalArgumentException.class,form::selection,"Mesmo um modelo de UI adulterado deve recusar acessórios Reaper");
        });
    }

    @Test void missingRequiredAtlasBlocksEntryInsteadOfClaimingACompleteRace(@TempDir Path temporary)throws Exception {
        var missingReaper=new NetworkPlayerVisuals(temporary.resolve("absent"),RpgPlayerSprites.load(ASSETS.resolve("player-base"),250,.5));
        var missingHuman=new NetworkPlayerVisuals(ASSETS.resolve("reaper"),null);
        SwingUtilities.invokeAndWait(()->{
            var first=new SessionCharacterCreation(missingReaper,null,null);first.setPlayerName("Recurso ausente");
            assertFalse(missingReaper.renderInfo("reaper").available());
            assertTrue(assertThrows(IllegalArgumentException.class,first::selection).getMessage().contains("Reaper indisponível"));
            first.setSelectedRace("human");assertEquals("human",first.selection().race());
            assertFalse(first.hair.isEnabled());assertTrue(first.resourceNotice().contains("Personalização indisponível"));
            var second=new SessionCharacterCreation(missingHuman,null,null);second.setPlayerName("Atlas ausente");
            assertEquals("reaper",second.selection().race());
            for(String race:new String[]{"human","aetheri","spirit"}) {
                second.setSelectedRace(race);assertFalse(missingHuman.renderInfo(race).available());
                assertThrows(IllegalArgumentException.class,second::selection);
            }
        });
    }

    @Test void paintsRealPreviewsWithoutCreatingWindowsOrTouchingPersonalSaves(@TempDir Path temporary)throws Exception {
        Path sentinel=temporary.resolve("saves/characters/existing.properties");Files.createDirectories(sentinel.getParent());Files.writeString(sentinel,"preservado");
        String previous=System.getProperty("spiritgate.rpg.dataDir");System.setProperty("spiritgate.rpg.dataDir",temporary.toString());
        int windows=java.awt.Window.getWindows().length;
        try {
            SwingUtilities.invokeAndWait(()->{
                var form=form();form.setPlayerName("Prévia de sessão");
                for(int[] size:new int[][]{{800,350},{640,320},{420,300}}) {
                    form.setSize(size[0],size[1]);layout(form);var image=new BufferedImage(size[0],size[1],BufferedImage.TYPE_INT_ARGB);
                    var graphics=image.createGraphics();try{form.printAll(graphics);}finally{graphics.dispose();}
                    assertTrue(form.preview.getWidth()>0);assertTrue(form.preview.getHeight()>0);assertTrue(form.name.getWidth()>0);
                }
                form.setSize(640,350);layout(form);form.setSelectedRace("reaper");var reaper=render(form.preview);
                form.setSelectedRace("human");var human=render(form.preview);
                assertFalse(Arrays.equals(reaper,human),"A prévia deve trocar o atlas, não apenas o nome da raça");
                form.setSelectedRace("aetheri");assertArrayEquals(human,render(form.preview),"Fallback Aetheri usa o mesmo raster Human, com aviso separado");
                form.setSelection(new SessionCharacterSelection("Nova tentativa","spirit","none","none"));
                assertEquals("spirit",form.selection().race());
            });
            assertEquals(windows,java.awt.Window.getWindows().length,"O formulário não deve criar outra janela");
            assertEquals("preservado",Files.readString(sentinel));
            try(var files=Files.walk(temporary)){assertEquals(1,files.filter(Files::isRegularFile).count());}
        }finally {
            if(previous==null)System.clearProperty("spiritgate.rpg.dataDir");else System.setProperty("spiritgate.rpg.dataDir",previous);
        }
    }

    private static void select(JComboBox<CosmeticCatalog.Definition> combo,String id) {
        for(int i=0;i<combo.getItemCount();i++)if(combo.getItemAt(i).id().equals(id)){combo.setSelectedIndex(i);return;}
        fail("Opção esperada indisponível: "+id);
    }
    private static int[] render(javax.swing.JPanel component) {
        var image=new BufferedImage(component.getWidth(),component.getHeight(),BufferedImage.TYPE_INT_ARGB);
        var graphics=image.createGraphics();try{component.printAll(graphics);}finally{graphics.dispose();}
        return image.getRGB(0,0,image.getWidth(),image.getHeight(),null,0,image.getWidth());
    }
    private static void layout(Container container){container.doLayout();for(var child:container.getComponents())if(child instanceof Container nested)layout(nested);}
}
