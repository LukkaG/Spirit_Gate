package com.projetok.studio;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.nio.file.Path;
import javax.swing.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

/** Gesto cancelado e prévia não podem modificar o projeto confirmado. */
class StudioGestureSafetyTest {
    @TempDir Path project;
    @Test void escapeRestoresUncommittedPaintingAndKeepsUndo()throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless());
        SwingUtilities.invokeAndWait(()->{
            var ui=new WorldStudio(project);
            try{
                var tools=new PaintTools(ui);tools.tool="Lapis";tools.tile=true;ui.canvas.viewport=new StudioViewport(1,0,0);
                ui.brush=new AssetCatalog.Asset("a".repeat(64),"test.png",project.resolve("test.png"),"b".repeat(64),32,32,true,"Tilesets",new java.awt.image.BufferedImage(32,32,2),java.time.Instant.now());ui.brushCrop=new WorldDocument.Crop(0,0,32,32);
                var before=ui.document;ui.canvas.cursor=new Point(35,35);
                var ghost=ui.canvas.brushPreview.get();assertEquals(32,ghost.x());assertEquals(before,ui.document);
                ui.canvas.pressed.accept(new MouseEvent(ui.canvas,MouseEvent.MOUSE_PRESSED,1,0,35,35,1,false,MouseEvent.BUTTON1));assertFalse(ui.document.elements().isEmpty());
                ui.getRootPane().getActionMap().get("Cancelar gesto").actionPerformed(null);
                assertEquals(before,ui.document);assertEquals(0,ui.history.size());assertNull(ui.canvas.selectionBox);
                ui.canvas.released.accept(new MouseEvent(ui.canvas,MouseEvent.MOUSE_RELEASED,2,0,64,64,1,false,MouseEvent.BUTTON1));assertEquals(before,ui.document);
            }finally{ui.dispose();}
        });
    }
}
