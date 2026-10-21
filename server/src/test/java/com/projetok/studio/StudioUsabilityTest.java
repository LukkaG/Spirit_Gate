package com.projetok.studio;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.time.Instant;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

class StudioUsabilityTest {
    @TempDir Path project;
    @Test void resizePreservesContentAndCanBeUndone(){
        var original=EditorOperations.pencil(WorldDocument.empty("mapa",128,128),EditorOperationsTest.brush(),96,96,true);
        var expanded=original.resized(256,192);assertEquals(original.elements(),expanded.elements());assertEquals(original.layers(),expanded.layers());
        var history=new EditorHistory(original);history.commit(expanded);assertEquals(original,history.undo());assertEquals(expanded,history.redo());
        assertThrows(IllegalArgumentException.class,()->original.resized(8193,128));assertThrows(IllegalArgumentException.class,()->original.resized(64,64));
    }
    @Test void rightDragFillsIntermediateCellsAndCommitsOneUndo() throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless());
        SwingUtilities.invokeAndWait(()->{
            var ui=new WorldStudio(project);
            try{
                ui.canvas.viewport=new StudioViewport(1,0,0);
                ui.brush=new AssetCatalog.Asset("a".repeat(64),"assets/test.png",project.resolve("test.png"),"b".repeat(64),32,32,true,"Tilesets",new BufferedImage(32,32,BufferedImage.TYPE_INT_ARGB),Instant.now());
                ui.brushCrop=new WorldDocument.Crop(0,0,32,32);
                var tools=new PaintTools(ui);tools.tile=true;var before=ui.document;
                ui.canvas.pressed.accept(mouse(ui,MouseEvent.MOUSE_PRESSED,1,1,MouseEvent.BUTTON3));
                ui.canvas.dragged.accept(mouse(ui,MouseEvent.MOUSE_DRAGGED,97,1,MouseEvent.NOBUTTON));
                assertEquals(4,ui.document.elements().size());assertEquals(0,ui.history.size());
                ui.canvas.released.accept(mouse(ui,MouseEvent.MOUSE_RELEASED,97,1,MouseEvent.BUTTON3));
                assertEquals(4,ui.document.elements().size());assertEquals(1,ui.history.size());assertEquals(before,ui.history.undo());
                assertEquals(4,ui.history.redo().elements().size());
            }finally{ui.dispose();}
        });
    }
    @Test void mouseButtonsKeepIndependentAssetsCropsAndPaintingModes() throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless());
        SwingUtilities.invokeAndWait(()->{
            var ui=new WorldStudio(project);
            try{
                ui.canvas.viewport=new StudioViewport(1,0,0);var tools=new PaintTools(ui);tools.tool="Lapis";
                var left=new AssetCatalog.Asset("a".repeat(64),"assets/left.png",project.resolve("left.png"),"b".repeat(64),64,64,true,"Tilesets",new BufferedImage(1,1,BufferedImage.TYPE_INT_ARGB),Instant.now());
                var right=new AssetCatalog.Asset("c".repeat(64),"assets/right.png",project.resolve("right.png"),"d".repeat(64),64,64,true,"Tilesets",new BufferedImage(1,1,BufferedImage.TYPE_INT_ARGB),Instant.now());
                var leftCrop=new WorldDocument.Crop(32,0,32,32);var rightCrop=new WorldDocument.Crop(0,16,16,48);
                tools.assign(left,leftCrop,true,false);tools.assign(right,rightCrop,false,true);
                ui.brush=right;ui.brushCrop=rightCrop;
                ui.canvas.pressed.accept(mouse(ui,MouseEvent.MOUSE_PRESSED,1,1,MouseEvent.BUTTON1));
                ui.canvas.released.accept(mouse(ui,MouseEvent.MOUSE_RELEASED,1,1,MouseEvent.BUTTON1));
                ui.canvas.pressed.accept(mouse(ui,MouseEvent.MOUSE_PRESSED,65,1,MouseEvent.BUTTON3));
                ui.canvas.released.accept(mouse(ui,MouseEvent.MOUSE_RELEASED,65,1,MouseEvent.BUTTON3));
                var paintedLeft=ui.document.elements().get(0);var paintedRight=ui.document.elements().get(1);
                assertEquals(left.id(),paintedLeft.asset());assertEquals(leftCrop,paintedLeft.crop());assertEquals(WorldDocument.Kind.TILE,paintedLeft.kind());
                assertEquals(right.id(),paintedRight.asset());assertEquals(rightCrop,paintedRight.crop());assertEquals(16,paintedRight.width());assertEquals(48,paintedRight.height());
                assertEquals(2,ui.history.size());assertEquals(1,ui.history.undo().elements().size());assertEquals(0,ui.history.undo().elements().size());
            }finally{ui.dispose();}
        });
    }
    @Test void paletteUsesActualMouseButtonWithoutOverwritingOtherBrush() throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless());
        SwingUtilities.invokeAndWait(()->{
            var ui=new WorldStudio(project);
            try{
                var tools=new PaintTools(ui);tools.tool="Lapis";ui.canvas.viewport=new StudioViewport(1,0,0);
                var asset=new AssetCatalog.Asset("a".repeat(64),"assets/terrain.png",project.resolve("terrain.png"),"b".repeat(64),64,32,true,"Tilesets",new BufferedImage(1,1,BufferedImage.TYPE_INT_ARGB),Instant.now());
                var asphalt=new WorldDocument.Crop(0,0,32,32);var water=new WorldDocument.Crop(32,0,32,32);
                var palette=ui.tilePalette;palette.showAsset(asset);palette.choose=(crop,right)->{tools.assign(asset,crop,true,right);palette.select(crop);};
                palette.sheet.setSize(128,64);
                palette.sheet.dispatchEvent(new MouseEvent(palette.sheet,MouseEvent.MOUSE_PRESSED,1,0,10,10,1,false,MouseEvent.BUTTON1));
                palette.sheet.dispatchEvent(new MouseEvent(palette.sheet,MouseEvent.MOUSE_PRESSED,2,0,42,10,1,false,MouseEvent.BUTTON3));
                assertEquals(asphalt,tools.leftBrush.crop());assertEquals(water,tools.rightBrush.crop());
                // Clicking the same selected tile with another button must still assign it.
                palette.sheet.dispatchEvent(new MouseEvent(palette.sheet,MouseEvent.MOUSE_PRESSED,3,0,42,10,1,false,MouseEvent.BUTTON1));
                assertEquals(water,tools.leftBrush.crop());assertEquals(water,tools.rightBrush.crop());
                palette.sheet.dispatchEvent(new MouseEvent(palette.sheet,MouseEvent.MOUSE_PRESSED,4,0,10,10,1,false,MouseEvent.BUTTON1));
                for(int button:new int[]{MouseEvent.BUTTON1,MouseEvent.BUTTON3}){
                    int x=button==MouseEvent.BUTTON1?1:65;
                    ui.canvas.pressed.accept(mouse(ui,MouseEvent.MOUSE_PRESSED,x,1,button));ui.canvas.released.accept(mouse(ui,MouseEvent.MOUSE_RELEASED,x,1,button));
                }
                assertEquals(asphalt,ui.document.elements().get(0).crop());assertEquals(water,ui.document.elements().get(1).crop());
            }finally{ui.dispose();}
        });
    }
    @Test void librarySelectionOpensFullTilesheetIncludingPartialEdges() throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless());
        SwingUtilities.invokeAndWait(()->{
            var ui=new WorldStudio(project);
            try{
                var asset=new AssetCatalog.Asset("a".repeat(64),"assets/full.png",project.resolve("full.png"),"b".repeat(64),70,45,true,"Tilesets",new BufferedImage(1,1,BufferedImage.TYPE_INT_ARGB),Instant.now());
                ui.assets.addElement(asset);ui.library.setSelectedIndex(0);
                assertEquals(asset,ui.tilePalette.asset);assertEquals(ui.tilePalette,ui.inspectorTabs.getSelectedComponent());
                assertEquals(new Dimension(70,45),ui.tilePalette.sheet.getPreferredSize());assertEquals(6,ui.tilesetCrops.size());
                assertEquals(new WorldDocument.Crop(64,32,6,13),ui.tilesetCrops.get(5));
                for(var component:ui.toolbar.getComponents())if(component instanceof JButton button)assertNotEquals("Recortar tileset",button.getText());
            }finally{ui.dispose();}
        });
    }
    @Test void dragSelectMoveCopyAndPastePreserveTheGroup() throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless());
        SwingUtilities.invokeAndWait(()->{
            var ui=new WorldStudio(project);
            try{
                ui.canvas.viewport=new StudioViewport(1,0,0);var tools=new PaintTools(ui);
                var initial=EditorOperations.pencil(EditorOperations.pencil(ui.document,EditorOperationsTest.brush(),32,32,true),EditorOperationsTest.brush(),64,32,true);
                ui.document=ui.history.reset(initial);
                ui.canvas.pressed.accept(mouse(ui,MouseEvent.MOUSE_PRESSED,0,0,MouseEvent.BUTTON1));
                ui.canvas.dragged.accept(mouse(ui,MouseEvent.MOUSE_DRAGGED,95,63,MouseEvent.NOBUTTON));assertNotNull(ui.canvas.selectionBox);
                ui.canvas.released.accept(mouse(ui,MouseEvent.MOUSE_RELEASED,95,63,MouseEvent.BUTTON1));assertEquals(2,ui.canvas.selected.size());assertNull(ui.canvas.selectionBox);
                ui.canvas.pressed.accept(mouse(ui,MouseEvent.MOUSE_PRESSED,33,33,MouseEvent.BUTTON1));
                ui.canvas.dragged.accept(mouse(ui,MouseEvent.MOUSE_DRAGGED,65,65,MouseEvent.NOBUTTON));
                ui.canvas.released.accept(mouse(ui,MouseEvent.MOUSE_RELEASED,65,65,MouseEvent.BUTTON1));
                assertEquals(64,ui.document.elements().get(0).x());assertEquals(96,ui.document.elements().get(1).x());assertEquals(1,ui.history.size());
                tools.copySelection();ui.lastCursor=new Point(160,160);tools.pasteSelection();assertEquals(4,ui.document.elements().size());assertEquals(2,ui.canvas.selected.size());
                assertEquals(160,ui.document.elements().get(2).x());assertEquals(192,ui.document.elements().get(3).x());
                assertEquals(2,ui.history.undo().elements().size());assertEquals(initial,ui.history.undo());
            }finally{ui.dispose();}
        });
    }
    @Test void tilesheetDragCopiesWholeRegionWithoutShrinkingIt() throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless());
        SwingUtilities.invokeAndWait(()->{
            var ui=new WorldStudio(project);
            try{
                var tools=new PaintTools(ui);var palette=ui.tilePalette;
                var asset=new AssetCatalog.Asset("a".repeat(64),"assets/sheet.png",project.resolve("sheet.png"),"b".repeat(64),96,64,true,"Tilesets",new BufferedImage(1,1,BufferedImage.TYPE_INT_ARGB),Instant.now());
                palette.showAsset(asset);
                palette.sheet.dispatchEvent(new MouseEvent(palette.sheet,MouseEvent.MOUSE_PRESSED,1,0,1,1,1,false,MouseEvent.BUTTON1));
                palette.sheet.dispatchEvent(new MouseEvent(palette.sheet,MouseEvent.MOUSE_DRAGGED,2,MouseEvent.BUTTON1_DOWN_MASK,63,63,1,false,MouseEvent.NOBUTTON));
                palette.sheet.dispatchEvent(new MouseEvent(palette.sheet,MouseEvent.MOUSE_RELEASED,3,0,63,63,1,false,MouseEvent.BUTTON1));
                assertEquals(new WorldDocument.Crop(0,0,64,64),tools.leftBrush.crop());assertFalse(tools.leftBrush.tile());
                tools.copySelection();ui.lastCursor=new Point(128,128);tools.pasteSelection();var pasted=ui.document.elements().get(0);
                assertEquals(64,pasted.width());assertEquals(64,pasted.height());assertEquals(asset.id(),pasted.asset());
            }finally{ui.dispose();}
        });
    }
    static MouseEvent mouse(WorldStudio ui,int event,int x,int y,int button){return new MouseEvent(ui.canvas,event,System.currentTimeMillis(),event==MouseEvent.MOUSE_RELEASED?0:MouseEvent.BUTTON3_DOWN_MASK,x,y,1,false,button);}
}
