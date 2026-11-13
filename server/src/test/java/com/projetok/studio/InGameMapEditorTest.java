package com.projetok.studio;

import com.projetok.prototype.DebugPanel;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BooleanSupplier;
import javax.swing.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

/** Verifica integracao Swing e aplicacao de gestos ao servidor real de edicao. */
class InGameMapEditorTest {
    @TempDir Path project;
    private static void await(BooleanSupplier condition)throws Exception{
        long end=System.nanoTime()+12_000_000_000L;var done=new boolean[1];
        do{SwingUtilities.invokeAndWait(()->done[0]=condition.getAsBoolean());if(done[0])return;Thread.sleep(25);}while(System.nanoTime()<end);
        fail("Condicao de integracao nao atendida no prazo");
    }
    private static JCheckBox editToggle(Container parent){
        for(var component:parent.getComponents()){
            if(component instanceof JCheckBox box&&box.getText().equals("Editar mapa no jogo"))return box;
            if(component instanceof Container container){var found=editToggle(container);if(found!=null)return found;}
        }return null;
    }
    @Test void embedsInsideGameAndRestoresContentMenuAndShortcuts()throws Exception{
        assumeFalse(GraphicsEnvironment.isHeadless());
        SwingUtilities.invokeAndWait(()->{
            var game=new JFrame();var original=new JPanel();var menu=new JMenuBar();game.setContentPane(original);game.setJMenuBar(menu);
            var input=game.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);var actions=game.getRootPane().getActionMap();
            var editor=new WorldStudio(project);
            try{editor.enterGame(game);assertNotSame(original,game.getContentPane());assertEquals(Boolean.TRUE,game.getRootPane().getClientProperty("worldEditorActive"));assertFalse(editor.isVisible());
                editor.dispose();assertSame(original,game.getContentPane());assertSame(menu,game.getJMenuBar());assertSame(input,game.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW));assertSame(actions,game.getRootPane().getActionMap());assertNull(game.getRootPane().getClientProperty("worldEditorActive"));
            }finally{editor.dispose();game.dispose();}
        });
    }
    @Test void clickOnGameCreatesDirectionalCollisionAndUpdatesAuthoritativeRevision()throws Exception{
        assumeFalse(GraphicsEnvironment.isHeadless());var ref=new AtomicReference<WorldStudio>();
        SwingUtilities.invokeAndWait(()->{var ui=new WorldStudio(Path.of("..").toAbsolutePath());ref.set(ui);ui.openDocument(FileTools.playable(WorldDocument.empty("mapa",960,640)),null);});
        var ui=ref.get();
        try{
            try{await(()->ui.liveTools.game!=null);}catch(AssertionError failed){SwingUtilities.invokeAndWait(()->System.err.println("EDITOR STATUS: "+ui.liveTools.state.getText()));throw failed;}var revision=new long[1];
            SwingUtilities.invokeAndWait(()->{
                var panel=(DebugPanel)ui.liveTools.game.getRootPane().getClientProperty("livePanel");panel.setSize(960,740);
                for(var component:ui.toolbar.getComponents())if(component instanceof JComboBox<?> combo){for(int i=0;i<combo.getItemCount();i++)if("Colisao".equals(combo.getItemAt(i)))combo.setSelectedIndex(i);}
                var p=panel.aimAt(350,300);assertTrue(Double.isFinite(p.x));assertTrue(Double.isFinite(p.y));revision[0]=ui.liveTools.preview.state().revision().number();
                panel.dispatchEvent(new MouseEvent(panel,MouseEvent.MOUSE_PRESSED,1,0,350,300,1,false,MouseEvent.BUTTON1));
                panel.dispatchEvent(new MouseEvent(panel,MouseEvent.MOUSE_RELEASED,2,0,350,300,1,false,MouseEvent.BUTTON1));
                assertEquals(1,ui.document.hitboxes().size());assertEquals(Math.floor(p.x/32)*32,ui.document.hitboxes().get(0).x());assertEquals(Math.floor(p.y/32)*32,ui.document.hitboxes().get(0).y());
                assertEquals(0,ui.workspace.getDividerSize());assertEquals(1,ui.document.elements().size());
            });
            await(()->ui.liveTools.preview.state().revision().number()>revision[0]);
            SwingUtilities.invokeAndWait(()->assertEquals(ui.document.hitboxes(),ui.liveTools.preview.state().revision().bundle().maps().get("mapa").hitboxes()));
            var before=new float[1];var tick=new long[1];
            SwingUtilities.invokeAndWait(()->{
                var panel=(DebugPanel)ui.liveTools.game.getRootPane().getClientProperty("livePanel");
                before[0]=ui.liveTools.preview.state().players().entrySet().stream().min(java.util.Map.Entry.comparingByKey()).orElseThrow().getValue().y();tick[0]=ui.liveTools.preview.state().tick();
                panel.getActionMap().get("Wtrue").actionPerformed(null);
            });
            await(()->ui.liveTools.preview.state().tick()>tick[0]+8);
            SwingUtilities.invokeAndWait(()->{
                assertEquals(before[0],ui.liveTools.preview.state().players().entrySet().stream().min(java.util.Map.Entry.comparingByKey()).orElseThrow().getValue().y());
                var box=editToggle(ui.gameHost);assertNotNull(box);box.doClick();assertFalse(box.isSelected());
                var panel=(DebugPanel)ui.liveTools.game.getRootPane().getClientProperty("livePanel");panel.getActionMap().get("Wtrue").actionPerformed(null);
            });
            await(()->ui.liveTools.preview.state().players().entrySet().stream().min(java.util.Map.Entry.comparingByKey()).orElseThrow().getValue().y()<before[0]);
            SwingUtilities.invokeAndWait(()->{
                var panel=(DebugPanel)ui.liveTools.game.getRootPane().getClientProperty("livePanel");panel.getActionMap().get("Wfalse").actionPerformed(null);
            });
        }finally{SwingUtilities.invokeAndWait(ui::dispose);}
    }
}
