package com.projetok.prototype.rpg;
import com.projetok.prototype.*;
import com.projetok.rpg.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.*;
import java.util.function.Supplier;
import javax.imageio.ImageIO;
public class VisualProbe {
 static Path out; static JFrame frame; static DebugPanel panel; static Supplier<DebugSnapshot> source;static RpgPlayerSprites sprites;
 static int stage=0,step=0,wait=0;static String held="";static Set<String> captured=new HashSet<>();static java.util.List<String> evidence=new ArrayList<>();
 static void key(String next){if(!held.isEmpty())panel.getActionMap().get(held+"false").actionPerformed(new ActionEvent(panel,0,"probe"));held=next;if(!held.isEmpty())panel.getActionMap().get(held+"true").actionPerformed(new ActionEvent(panel,0,"probe"));}
 static DebugSnapshot.ActorView player(){return source.get().actors().stream().filter(DebugSnapshot.ActorView::player).findFirst().orElseThrow();}
 static void capture(String label)throws Exception{
  var image=new BufferedImage(frame.getWidth(),frame.getHeight(),BufferedImage.TYPE_INT_RGB);var g=image.createGraphics();frame.printAll(g);g.dispose();ImageIO.write(image,"png",out.resolve(label+".png").toFile());
  var p=player();String pose=sprites==null?"GEOMETRY":sprites.frameKey();evidence.add("{\"label\":\""+label+"\",\"frame\":\""+pose+"\",\"x\":"+p.x()+",\"y\":"+p.y()+",\"tick\":"+source.get().ticks()+",\"windowShowing\":"+frame.isShowing()+",\"humanApproved\":false}");System.out.println(label+" "+pose+" x="+p.x()+" y="+p.y());
 }
 @SuppressWarnings("unchecked") public static void main(String[] args)throws Exception{
  out=Path.of(args[0]).toAbsolutePath();Files.createDirectories(out);
  System.setProperty("jpackage.app-path",out.resolve("isolated.exe").toString());System.setProperty(RpgPlayerSprites.ENABLED,args.length>1?args[1]:"true");
  // Assets remain in the actual checkout; load via hook after play, which uses the normal configuration path.
  SwingUtilities.invokeAndWait(()->RpgLauncher.play(new RpgWorld(),new LocalSaveStore(out.resolve("synthetic.properties"))));
  SwingUtilities.invokeAndWait(()->{try{
   frame=Arrays.stream(Frame.getFrames()).filter(f->f instanceof JFrame&&f.isShowing()).map(f->(JFrame)f).findFirst().orElseThrow();
   panel=find(frame);var field=DebugPanel.class.getDeclaredField("source");field.setAccessible(true);source=(Supplier<DebugSnapshot>)field.get(panel);
   field=DebugPanel.class.getDeclaredField("actorRenderer");field.setAccessible(true);sprites=(RpgPlayerSprites)field.get(panel);
   if(sprites==null){capture("geometry-default");finish();return;}
   // Disable only the launcher's automatic two-second capture in this external validation harness.
   new javax.swing.Timer(35,event->{try{advance();}catch(Exception ex){ex.printStackTrace();System.exit(2);}}).start();
  }catch(Exception ex){throw new RuntimeException(ex);}});
 }
 static DebugPanel find(Container c){for(Component child:c.getComponents()){if(child instanceof DebugPanel d)return d;if(child instanceof Container nested){var result=find(nested);if(result!=null)return result;}}return null;}
 static int watchdog;
 static void advance()throws Exception{
  if(++watchdog>1500)throw new IllegalStateException("Visual validation timeout at stage "+stage);
  String[] keys={"S","W","A","D"};String[] dirs={"FRONT","BACK","LEFT","RIGHT"};
  if(stage<4){
   if(step==0){key(keys[stage]);wait=0;step=1;}
   else if(step==1){wait++;String fk=sprites.frameKey();if(sprites.pose().walking()&&captured.add(fk))capture("walk-"+fk);if(wait>18){key("");step=2;wait=0;}}
   else if(++wait>3){capture("idle-"+dirs[stage]);stage++;step=0;}
   return;
  }
  var p=player();
  if(stage==4){if(p.x()<350){key("D");}else{key("W");stage=5;}return;}
  if(stage==5){key("W");if(p.y()<=240){key("D");stage=6;wait=0;}return;}
  if(stage==6)key("D");
  if(stage==6&&++wait>30){capture("collision-solid");key("");finish();}
 }
 static void finish()throws Exception{key("");Files.writeString(out.resolve("visual-evidence.json"),"["+String.join(",",evidence)+"]");System.out.println("VISUAL_PROBE_FINISHED");frame.dispose();System.exit(0);}
}
