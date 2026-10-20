package com.projetok.prototype.rpg;

import com.projetok.prototype.*;
import com.projetok.rpg.*;
import javax.swing.*;
import javax.imageio.*;
import javax.imageio.metadata.*;
import javax.imageio.stream.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.*;
import java.util.function.Supplier;

/** External evidence harness. Uses real bindings and real Swing components, no world injection. */
public class RefinementVisualProbe {
    static JFrame frame;static DebugPanel panel;static RpgPlayerSprites sprites;static Supplier<DebugSnapshot> source;
    static Path output;static int duration,stage=-1;static double scale;static long startTick;static String held="";
    static Set<String> captured=new HashSet<>();static java.util.List<String> records=new ArrayList<>();
    static java.util.List<BufferedImage> sequence=new ArrayList<>();static java.util.List<Long> times=new ArrayList<>();
    static javax.swing.Timer timer;static int watchdog;
    static void press(String key){
        if(!held.isEmpty()&&!held.equals(key))panel.getActionMap().get(held+"false").actionPerformed(new ActionEvent(panel,0,"visual"));
        held=key;if(!key.isEmpty())panel.getActionMap().get(key+"true").actionPerformed(new ActionEvent(panel,0,"visual"));
    }
    static BufferedImage capture(){
        var image=new BufferedImage(frame.getWidth(),frame.getHeight(),BufferedImage.TYPE_INT_RGB);
        var g=image.createGraphics();frame.printAll(g);g.dispose();return image;
    }
    static void record(String label,BufferedImage image)throws Exception{
        ImageIO.write(image,"png",output.resolve(label+".png").toFile());
        var p=source.get().actors().stream().filter(DebugSnapshot.ActorView::player).findFirst().orElseThrow();
        records.add("{\"label\":\""+label+"\",\"key\":\""+sprites.frameKey()+"\",\"tick\":"+source.get().ticks()+",\"x\":"+p.x()+",\"y\":"+p.y()+",\"scale\":"+scale+",\"frameMillis\":"+duration+",\"windowShowing\":"+frame.isShowing()+",\"humanApproved\":false}");
        System.out.println(label+" "+sprites.frameKey()+" scale="+scale+" duration="+duration);
    }
    @SuppressWarnings("unchecked") public static void main(String[] args)throws Exception{
        output=Path.of(args[0]).toAbsolutePath();scale=Double.parseDouble(args[1]);duration=Integer.parseInt(args[2]);
        Files.createDirectories(output);System.setProperty("jpackage.app-path",output.resolve("isolated.exe").toString());
        System.setProperty("spiritgate.rpg.dataDir",output.resolve("data").toString());
        System.setProperty(RpgPlayerSprites.ENABLED,"true");System.setProperty(RpgPlayerSprites.SCALE,""+scale);
        System.setProperty(RpgPlayerSprites.FRAME_MILLIS,""+duration);
        SwingUtilities.invokeAndWait(()->RpgLauncher.play(new RpgWorld(),new LocalSaveStore(output.resolve("synthetic.properties"))));
        SwingUtilities.invokeAndWait(()->{try{
            frame=Arrays.stream(Frame.getFrames()).filter(f->f instanceof JFrame&&f.isShowing()).map(f->(JFrame)f).findFirst().orElseThrow();
            panel=find(frame);var field=DebugPanel.class.getDeclaredField("source");field.setAccessible(true);source=(Supplier<DebugSnapshot>)field.get(panel);
            field=DebugPanel.class.getDeclaredField("actorRenderer");field.setAccessible(true);sprites=(RpgPlayerSprites)field.get(panel);
            if(sprites==null)throw new IllegalStateException("Sprites not enabled");
            timer=new javax.swing.Timer(40,e->{try{advance();}catch(Exception ex){ex.printStackTrace();System.exit(2);}});timer.start();
        }catch(Exception ex){throw new RuntimeException(ex);}});
    }
    static DebugPanel find(Container parent){for(Component child:parent.getComponents()){
        if(child instanceof DebugPanel p)return p;if(child instanceof Container c){var found=find(c);if(found!=null)return found;}
    }return null;}
    static void advance()throws Exception{
        if(++watchdog>700)throw new IllegalStateException("Visual harness timeout");
        String[] keys={"S","W","A","D"};String[] directions={"FRONT","BACK","LEFT","RIGHT"};
        if(stage==-1){record("initial-idle-FRONT",capture());stage=0;startTick=source.get().ticks();press(keys[0]);return;}
        if(stage>=4){finish();return;}
        long elapsed=source.get().ticks()-startTick;
        if(elapsed<25){
            press(keys[stage]);String key=sprites.frameKey();BufferedImage image=null;
            if(sprites.pose().walking()&&captured.add(key)){image=capture();record("walk-"+key,image);}
            if(stage==0){if(image==null)image=capture();sequence.add(image);times.add(System.nanoTime());}
        }else if(elapsed<31){press("");}
        else{record("idle-"+directions[stage],capture());stage++;startTick=source.get().ticks();if(stage<4)press(keys[stage]);}
    }
    static void gif()throws Exception{
        var writer=ImageIO.getImageWritersByFormatName("gif").next();
        try(var stream=new FileImageOutputStream(output.resolve("walk-FRONT-real.gif").toFile())){
            writer.setOutput(stream);writer.prepareWriteSequence(null);
            for(int i=0;i<sequence.size();i++){
                var image=sequence.get(i);var metadata=writer.getDefaultImageMetadata(new ImageTypeSpecifier(image),writer.getDefaultWriteParam());
                String format=metadata.getNativeMetadataFormatName();var tree=(IIOMetadataNode)metadata.getAsTree(format);
                var control=(IIOMetadataNode)tree.getElementsByTagName("GraphicControlExtension").item(0);
                long nanos=i+1<times.size()?times.get(i+1)-times.get(i):40_000_000L;
                control.setAttribute("delayTime",""+Math.max(1,Math.round(nanos/10_000_000.0)));
                control.setAttribute("disposalMethod","none");control.setAttribute("transparentColorFlag","FALSE");
                metadata.setFromTree(format,tree);writer.writeToSequence(new IIOImage(image,null,metadata),writer.getDefaultWriteParam());
            }
            writer.endWriteSequence();
        }finally{writer.dispose();}
    }
    static void finish()throws Exception{
        timer.stop();press("");gif();
        Files.writeString(output.resolve("visual-evidence.json"),"["+String.join(",",records)+"]");
        Files.writeString(output.resolve("sequence-timing.json"),"{\"samples\":"+sequence.size()+",\"relativeNanos\":"+times.stream().map(t->t-times.get(0)).toList()+",\"humanApproved\":false}");
        System.out.println("FINISHED captures="+records.size()+" sequenceSamples="+sequence.size());frame.dispose();System.exit(0);
    }
}
