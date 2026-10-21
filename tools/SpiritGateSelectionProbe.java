package com.projetok.prototype.rpg;

import com.projetok.rpg.*;
import javax.swing.*;
import java.awt.*;
import java.nio.file.*;
import javax.imageio.ImageIO;

/** Probe das janelas Swing reais, dados temporarios e capturas Robot. */
public final class SpiritGateSelectionProbe {
    static Path output;
    interface Check { boolean ok(); }
    static void waitFor(Check check)throws Exception{for(int i=0;i<200;i++){final boolean[] yes={false};SwingUtilities.invokeAndWait(()->yes[0]=check.ok());if(yes[0])return;Thread.sleep(50);}throw new AssertionError("Timeout UI");}
    static JFrame frame(String title){for(Frame f:Frame.getFrames())if(f instanceof JFrame j&&j.isShowing()&&j.getTitle().equals(title))return j;return null;}
    static <T extends Component> T find(Container parent,Class<T> type,String text){for(Component c:parent.getComponents()){if(type.isInstance(c)&&(text==null||c instanceof JButton b&&b.getText().equals(text)))return type.cast(c);if(c instanceof Container child){T found=find(child,type,text);if(found!=null)return found;}}return null;}
    static void click(JFrame f,String text)throws Exception{SwingUtilities.invokeLater(()->{var b=find(f,JButton.class,text);if(b==null||!b.isEnabled())throw new AssertionError(text);b.doClick();});Thread.sleep(180);}
    static void capture(JFrame frame,String name)throws Exception{
        Thread.sleep(250);
        ImageIO.write(new Robot().createScreenCapture(frame.getBounds()),"png",output.resolve(name+".desktop.png").toFile());
        SwingUtilities.invokeAndWait(()->{try{var image=new java.awt.image.BufferedImage(frame.getWidth(),frame.getHeight(),java.awt.image.BufferedImage.TYPE_INT_RGB);var g=image.createGraphics();frame.printAll(g);g.dispose();ImageIO.write(image,"png",output.resolve(name+".png").toFile());}catch(Exception e){throw new RuntimeException(e);}});
    }

    public static void main(String[] args)throws Exception{try{run(args);}catch(Throwable e){e.printStackTrace();System.exit(1);}System.exit(0);}
    static void run(String[] args)throws Exception{
        output=Path.of(args[0]);Files.createDirectories(output);Path data=Files.createTempDirectory("task115-ui-");System.setProperty("spiritgate.rpg.dataDir",data.toString());
        SwingUtilities.invokeAndWait(SpiritGateEntry::open);waitFor(()->frame("Spirit Gate | Portal de entrada")!=null);var entry=frame("Spirit Gate | Portal de entrada");capture(entry,"01-entrada");
        final JFrame initialEntry=entry;SwingUtilities.invokeAndWait(()->{find(initialEntry,JTextField.class,null).setText("Perfil de teste");find(initialEntry,JCheckBox.class,null).setSelected(true);});click(entry,"Entrar");waitFor(()->frame("Spirit Gate | Inicio")!=null);JFrame menu=frame("Spirit Gate | Inicio");final JFrame emptyMenu=menu;waitFor(()->find(emptyMenu,JButton.class,"Criar personagem").isEnabled());capture(menu,"02-vazio");
        click(menu,"Criar personagem");waitFor(()->java.util.Arrays.stream(Window.getWindows()).anyMatch(w->w instanceof JDialog&&w.isShowing()));
        SwingUtilities.invokeLater(()->{for(Window w:Window.getWindows())if(w instanceof JDialog d&&d.isShowing()){find(d,JTextField.class,null).setText("Heroi UI");find(d,JButton.class,"Criar personagem").doClick();}});
        waitFor(()->frame("Spirit Gate | RPG offline")!=null);JFrame game=frame("Spirit Gate | RPG offline");capture(game,"03-personagem-criado");click(game,"Salvar e voltar ao menu");waitFor(()->frame("Spirit Gate | Inicio")!=null);menu=frame("Spirit Gate | Inicio");final JFrame returned=menu;waitFor(()->find(returned,JButton.class,"Jogar").isEnabled());capture(menu,"04-selecao-retorno");
        click(menu,"Jogar");waitFor(()->frame("Spirit Gate | RPG offline")!=null);game=frame("Spirit Gate | RPG offline");click(game,"Salvar e voltar ao menu");waitFor(()->frame("Spirit Gate | Inicio")!=null);
        menu=frame("Spirit Gate | Inicio");click(menu,"Voltar");waitFor(()->frame("Spirit Gate | Portal de entrada")!=null);entry=frame("Spirit Gate | Portal de entrada");final JFrame remembered=entry;waitFor(()->find(remembered,JTextField.class,null).getText().equals("Perfil de teste"));capture(entry,"05-voltar-perfil");
        var repo=new CharacterSaves(data.resolve("saves/characters"));var slot=repo.list().get(0);new LocalSaveStore(slot.file()).save(slot.state());Files.writeString(slot.file(),"corrupt");
        click(entry,"Entrar");waitFor(()->frame("Spirit Gate | Inicio")!=null);menu=frame("Spirit Gate | Inicio");final JFrame invalidMenu=menu;waitFor(()->find(invalidMenu,JButton.class,"Criar personagem").isEnabled());capture(menu,"06-corrompido-backup");
        SwingUtilities.invokeAndWait(()->{var view=(CharacterSelection)invalidMenu.getContentPane();view.list.setSelectedIndex(1);});capture(menu,"07-backup");
        SwingUtilities.invokeAndWait(()->{var view=(CharacterSelection)invalidMenu.getContentPane();view.preview.sprites=null;invalidMenu.setSize(640,480);});capture(menu,"08-fallback-640x480");
        Files.writeString(output.resolve("probe.txt"),"PASS: entrada offline, perfil lembrado, lista vazia, criacao via dialogo, jogar, salvar/retornar, reabrir save, voltar, corrompido, backup, fallback, 640x480.\nDados isolados: "+data+"\nCapturas .png: printAll das janelas Swing reais. .desktop.png: Robot (desktop pode estar bloqueado). Sem aprovacao humana.\n");
        SwingUtilities.invokeAndWait(()->{for(Window w:Window.getWindows())w.dispose();});
    }
}
