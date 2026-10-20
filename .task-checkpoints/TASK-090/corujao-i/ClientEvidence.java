package com.projetok.prototype.rpg;
import com.projetok.rpg.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;

/** Sonda externa: janelas reais com fixtures sinteticas, sem editar saves pessoais. */
public final class ClientEvidence {
    static Path output;static String mode;static RpgRuntime runtime;static JFrame frame;static int ticks,stage;static boolean attackCaptured;
    static RpgHud hud(Container root){for(Component c:root.getComponents()){if(c instanceof RpgHud h)return h;if(c instanceof Container p){var h=hud(p);if(h!=null)return h;}}return null;}
    static JButton button(Container root,String text){for(Component c:root.getComponents()){if(c instanceof JButton b&&b.getText().equals(text))return b;if(c instanceof Container p){var b=button(p,text);if(b!=null)return b;}}return null;}
    static void capture(Window w,String name)throws Exception {var image=new BufferedImage(w.getWidth(),w.getHeight(),BufferedImage.TYPE_INT_RGB);var g=image.createGraphics();w.printAll(g);g.dispose();ImageIO.write(image,"png",output.resolve(name+".png").toFile());}
    static void findClient()throws Exception {frame=Arrays.stream(Frame.getFrames()).filter(f->f instanceof JFrame&&f.isShowing()&&hud((JFrame)f)!=null).map(f->(JFrame)f).findFirst().orElseThrow();var field=RpgHud.class.getDeclaredField("runtime");field.setAccessible(true);runtime=(RpgRuntime)field.get(hud(frame));}
    public static void main(String[] args)throws Exception {
        output=Path.of(args[0]).toAbsolutePath();mode=args[1];Files.createDirectories(output);
        System.setProperty("jpackage.app-path",output.resolve("isolated.exe").toString());System.setProperty("spiritgate.rpg.dataDir",output.resolve("data").toString());System.setProperty(RpgPlayerSprites.ENABLED,"true");
        var initial=new RpgWorld();var save=initial.save();
        if(mode.equals("boss")){var flags=new TreeSet<String>();for(String f:save.flags()){if(f.startsWith("mob:ruins_guard_")){var p=f.split(":");p[2]="0";f=String.join(":",p);}flags.add(f);}save=new RpgSave(save.identity(),"ruins",650,320,save.hp(),save.xp(),save.coins(),save.inventory(),save.equipment(),save.quests(),flags,save.ticks());}
        var world=RpgWorld.restore(save);Path file=output.resolve("data/saves/characters/"+save.identity().id()+".properties");
        SwingUtilities.invokeAndWait(()->RpgLauncher.play(world,new LocalSaveStore(file)));
        SwingUtilities.invokeAndWait(()->{try{findClient();var timer=new javax.swing.Timer(45,e->{try{step();}catch(Exception error){error.printStackTrace();System.exit(2);}});timer.start();}catch(Exception error){throw new RuntimeException(error);}});
    }
    static void step()throws Exception {
        if(++ticks>350)throw new IllegalStateException("Timeout evidence "+mode+" stage "+stage);
        if(mode.equals("failure")){
            if(stage==0){stage=1;runtime.command(w->{throw new IllegalStateException("Falha controlada de evidencia");});}
            else for(Window w:Window.getWindows())if(w instanceof JDialog d&&d.isShowing()&&d.getTitle().equals("Falha da simulacao")){
                capture(d,"runtime-failure-dialog");capture(frame,"runtime-failure-client");Files.writeString(output.resolve("failure.json"),"{\"failurePublished\":"+(runtime.failure()!=null)+",\"dialogShowing\":true,\"humanApproved\":false}");System.exit(0);
            }
            return;
        }
        if(mode.equals("ko")) {
            if(stage==0){stage=1;runtime.command(w->w.receiveDamage(10000));}
            else if(stage==1&&runtime.snapshot().state().hp()==0&&ticks>4){capture(frame,"ko");stage=2;runtime.command(w->w.returnHome());}
            else if(stage==2&&runtime.snapshot().state().hp()>0&&ticks>7){capture(frame,"ko-recovered");System.exit(0);}
            return;
        }
        if(stage<2){
            var boss=runtime.snapshot().enemies().stream().filter(EnemyService.View::boss).findFirst().orElseThrow();
            if(stage==0&&boss.state().equals("PREPARE")){capture(frame,"boss-prepare");stage=1;runtime.command(w->w.activate("rpg.secondary"));}
            else if(stage==1&&!attackCaptured&&boss.hp()<boss.maxHp()) {capture(frame,"attack-feedback");attackCaptured=true;}
            if(stage==1&&boss.state().equals("RECOVER")){capture(frame,"boss-recover-hit");stage=2;frame.dispatchEvent(new WindowEvent(frame,WindowEvent.WINDOW_CLOSING));}
        }else if(stage==2){
            for(Frame f:Frame.getFrames())if(f instanceof JFrame menu&&menu.isShowing()&&menu.getTitle().equals("Spirit Gate | Inicio")){
                JButton resume=button(menu,"Continuar selecionado");if(resume!=null&&resume.isEnabled()){capture(menu,"saved-menu");stage=3;resume.doClick();return;}
            }
        }else if(stage==3){
            for(Frame f:Frame.getFrames())if(f instanceof JFrame client&&client.isShowing()&&hud(client)!=null){findClient();capture(frame,"resumed-client");Files.writeString(output.resolve("lifecycle.json"),"{\"prepared\":true,\"attackFeedback\":"+attackCaptured+",\"recover\":true,\"savedMenu\":true,\"resumed\":true,\"humanApproved\":false}");System.exit(0);}
        }
    }
}
