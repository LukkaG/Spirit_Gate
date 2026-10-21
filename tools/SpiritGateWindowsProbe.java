package com.projetok.prototype.rpg;
import com.projetok.rpg.*;
import javax.swing.*;
import java.awt.*;
import java.nio.file.*;
import javax.imageio.ImageIO;
/** Exercita janelas reais em diretório sintético, inclusive salvar e continuar. */
public final class SpiritGateWindowsProbe {
    static Path output;
    static JFrame game;
    interface Check{boolean ok();}
    static void waitFor(Check condition)throws Exception{for(int i=0;i<400;i++){boolean[] ok={false};SwingUtilities.invokeAndWait(()->ok[0]=condition.ok());if(ok[0])return;Thread.sleep(50);}throw new AssertionError("Timeout de janela");}
    static <T extends Component>T find(Container root,Class<T> type,String text){for(Component c:root.getComponents()){if(type.isInstance(c)&&(text==null||c instanceof JButton b&&b.getText().equals(text)))return type.cast(c);if(c instanceof Container nested){T result=find(nested,type,text);if(result!=null)return result;}}return null;}
    static void capture(Window window,String file)throws Exception{SwingUtilities.invokeAndWait(()->{window.validate();var image=new java.awt.image.BufferedImage(window.getWidth(),window.getHeight(),2);var g=image.createGraphics();window.printAll(g);g.dispose();try{ImageIO.write(image,"png",output.resolve(file+".png").toFile());}catch(Exception e){throw new RuntimeException(e);}});}
    static void click(String text)throws Exception{SwingUtilities.invokeAndWait(()->{JButton button=find(game,JButton.class,text);if(button==null)throw new AssertionError(text);button.doClick();});Thread.sleep(200);}
    static JDialog dialog(String title){for(Window w:Window.getWindows())if(w instanceof JDialog d&&d.isShowing()&&d.getTitle().equals(title))return d;return null;}
    /** Usa somente arquivos temporários e capturas printAll sem aprovação física implícita. */
    public static void main(String[] args){try{run(args);System.exit(0);}catch(Throwable e){e.printStackTrace();System.exit(1);}}
    static void run(String[] args)throws Exception{
        output=Path.of(args[0]);Files.createDirectories(output);Path data=Files.createTempDirectory("corujao3-windows-");System.setProperty("spiritgate.rpg.dataDir",data.toString());
        var world=new RpgWorld();world.inventory().add("sword",1);world.interact();var store=new LocalSaveStore(data.resolve("saves/characters/probe.properties"));
        SwingUtilities.invokeLater(()->RpgLauncher.play(world,store));
        waitFor(()->{for(Frame frame:Frame.getFrames())if(frame instanceof JFrame f&&f.isShowing()&&f.getTitle().startsWith("Spirit Gate | RPG offline")){game=f;return true;}return false;});
        for(var size:new int[][]{{1000,620},{800,600},{640,480}}){SwingUtilities.invokeAndWait(()->game.setSize(size[0],size[1]));capture(game,"npc-hud-"+size[0]+"x"+size[1]);}
        click("Inventario [I]");waitFor(()->dialog("Inventario e equipamento")!=null);JDialog inventory=dialog("Inventario e equipamento");
        SwingUtilities.invokeAndWait(()->find(inventory,JList.class,null).setSelectedIndex(0));capture(inventory,"inventario");SwingUtilities.invokeAndWait(()->inventory.setVisible(false));
        click("Diario [J]");waitFor(()->dialog("Diario e exploracao")!=null);JDialog diary=dialog("Diario e exploracao");capture(diary,"diario");SwingUtilities.invokeAndWait(()->diary.setVisible(false));
        click("Mapa [M]");waitFor(()->diary.isVisible());capture(diary,"mapa-regional");SwingUtilities.invokeAndWait(()->find(diary,JSpinner.class,null).setValue(2.0));capture(diary,"mapa-zoom2");SwingUtilities.invokeAndWait(()->diary.setVisible(false));
        SwingUtilities.invokeAndWait(()->game.dispatchEvent(new java.awt.event.WindowEvent(game,java.awt.event.WindowEvent.WINDOW_CLOSING)));
        waitFor(()->!game.isDisplayable());waitFor(()->java.util.Arrays.stream(Frame.getFrames()).anyMatch(f->f.isShowing()&&f.getTitle().equals("Spirit Gate | Inicio")));
        if(!Files.isRegularFile(store.file()))throw new AssertionError("Save não escrito");
        Files.writeString(output.resolve("probe.txt"),"PASS: NPC real, HUD 1000x620/800x600/640x480, inventário, diário, salvar e encerrar. Capturas printAll, sem homologação física. Dados sintéticos: "+data+"\n");
        SwingUtilities.invokeAndWait(()->{for(Window window:Window.getWindows())window.dispose();});
    }
}
