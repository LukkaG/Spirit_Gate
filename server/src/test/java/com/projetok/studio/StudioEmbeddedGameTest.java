package com.projetok.studio;

import java.awt.*;
import java.nio.file.Path;
import java.util.Map;
import javax.swing.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

class StudioEmbeddedGameTest {
    @Test void hitboxDragClampsAndSnapsInBothDirections(){
        var d=WorldDocument.empty("map",960,640);
        assertEquals(new Rectangle(928,608,32,32),CollisionTools.dragBounds(d,new Point(959,639),new Point(1100,800),true));
        assertEquals(new Rectangle(0,0,96,96),CollisionTools.dragBounds(d,new Point(90,90),new Point(-20,-30),true));
        assertEquals(new Rectangle(10,20,1,1),CollisionTools.dragBounds(d,new Point(10,20),new Point(10,20),false));
    }
    @Test void gameLivesInsideEditorHostAndKeysRequireGameFocus()throws Exception{
        assumeFalse(GraphicsEnvironment.isHeadless());
        var d=LiveScenarios.document(0);
        var revision=LiveRevision.prepare(0,d.id(),Map.of(d.id(),d),new AssetCatalog.Result(java.util.List.of(),java.util.List.of(),java.util.List.of()));
        var host=new JPanel(new BorderLayout());
        try(var server=new PreviewServer(revision,"reaper",1)){
            var game=StudioLiveWindow.load(server,Path.of("..").toAbsolutePath(),"Embedded test",host);
            try{SwingUtilities.invokeAndWait(()->{
                assertFalse(game.isVisible());assertEquals(1,host.getComponentCount());
                var panel=(JComponent)game.getRootPane().getClientProperty("livePanel");
                assertTrue(SwingUtilities.isDescendingFrom(panel,host));
                assertNotNull(panel.getInputMap(JComponent.WHEN_FOCUSED).get(KeyStroke.getKeyStroke("pressed W")));
                assertNull(panel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).get(KeyStroke.getKeyStroke("pressed W")));
                assertNotNull(panel.getInputMap(JComponent.WHEN_FOCUSED).get(KeyStroke.getKeyStroke("pressed SPACE")));
            });}finally{SwingUtilities.invokeAndWait(game::close);}
            SwingUtilities.invokeAndWait(()->assertInstanceOf(JLabel.class,host.getComponent(0)));
        }
    }
}
