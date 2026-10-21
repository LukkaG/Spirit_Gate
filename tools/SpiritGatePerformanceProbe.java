package com.projetok.prototype.rpg;
import com.projetok.rpg.*;
import com.projetok.prototype.DebugPanel;
import javax.swing.*;
import java.awt.*;
import java.nio.file.*;
/** Mede custos locais; ciclos Swing não equivalem à taxa de atualização do monitor. */
public final class SpiritGatePerformanceProbe {
    /** Publica medidas reais em dados temporários, sem limiar artificial de aprovação. */
    public static void main(String[] args){try{run(args);System.exit(0);}catch(Throwable error){error.printStackTrace();System.exit(1);}}
    static void run(String[] args)throws Exception{
        Path output=Path.of(args[0]);Files.createDirectories(output);Path data=Files.createTempDirectory("corujao3-perf-");System.setProperty("spiritgate.rpg.dataDir",data.toString());
        var world=new RpgWorld();var store=new LocalSaveStore(data.resolve("saves/characters/perf.properties"));store.save(world.save());
        long loadStart=System.nanoTime();for(int i=0;i<100;i++)RpgWorld.restore(store.load());double loadMillis=(System.nanoTime()-loadStart)/100_000_000.0;
        var heap=Runtime.getRuntime();long before=heap.totalMemory()-heap.freeMemory();long started=System.nanoTime();
        SwingUtilities.invokeLater(()->RpgLauncher.play(world,store));SpiritGateWindowsProbe.waitFor(()->{for(Frame frame:Frame.getFrames())if(frame instanceof JFrame f&&f.isShowing()&&f.getTitle().startsWith("Spirit Gate | RPG offline")){SpiritGateWindowsProbe.game=f;return true;}return false;});
        double startupMillis=(System.nanoTime()-started)/1_000_000.0;JFrame game=SpiritGateWindowsProbe.game;
        long heapDelta=(heap.totalMemory()-heap.freeMemory())-before;
        DebugPanel panel=SpiritGateWindowsProbe.find(game,DebugPanel.class,null);var paints=new java.util.concurrent.atomic.AtomicInteger();
        RepaintManager manager=new RepaintManager(){public void paintDirtyRegions(){if(!getDirtyRegion(panel).isEmpty())paints.incrementAndGet();super.paintDirtyRegions();}};
        SwingUtilities.invokeAndWait(()->RepaintManager.setCurrentManager(manager));
        SwingUtilities.invokeAndWait(()->panel.getActionMap().get("Wtrue").actionPerformed(new java.awt.event.ActionEvent(panel,0,"probe")));
        long sampling=System.nanoTime();Thread.sleep(2000);double seconds=(System.nanoTime()-sampling)/1_000_000_000.0;
        SwingUtilities.invokeAndWait(()->panel.getActionMap().get("Wfalse").actionPerformed(new java.awt.event.ActionEvent(panel,0,"probe")));
        double repaintHz=paints.get()/seconds;
        double[] mapCost={0};try(var runtime=new RpgRuntime(new RpgWorld())){SwingUtilities.invokeAndWait(()->{var map=new NavigationMap(runtime);map.setSize(190,180);var image=new java.awt.image.BufferedImage(190,180,2);var g=image.createGraphics();for(int i=0;i<50;i++)map.paint(g);long start=System.nanoTime();for(int i=0;i<500;i++)map.paint(g);mapCost[0]=(System.nanoTime()-start)/500_000.0;g.dispose();});}
        String json="{\n  \"startupWindowMillis\": "+startupMillis+",\n  \"loadAndRestoreMeanMillis100\": "+loadMillis+",\n  \"heapDeltaBytesUnisolated\": "+heapDelta+",\n  \"swingPaintCyclesPerSecondMoving\": "+repaintHz+",\n  \"minimapMeanMicros500Offscreen\": "+mapCost[0]+",\n  \"physicalMonitorFpsMeasured\": false,\n  \"notes\": \"JVM local; heap sem isolamento de GC; pintura Swing, não scanout físico; minimapa offscreen após 50 aquecimentos.\"\n}\n";
        Files.writeString(output.resolve("performance.json"),json);
        SwingUtilities.invokeAndWait(()->game.dispatchEvent(new java.awt.event.WindowEvent(game,java.awt.event.WindowEvent.WINDOW_CLOSING)));SpiritGateWindowsProbe.waitFor(()->!game.isDisplayable());SwingUtilities.invokeAndWait(()->{for(Window window:Window.getWindows())window.dispose();});
    }
}
