import java.awt.*;
import java.awt.event.WindowEvent;
import java.nio.file.*;
import javax.swing.*;

/** Loaded only by the test harness with exactly the updater's Java launch options. */
public class UpdatableRadminUiProbe {
    static JButton find(Container root,String text){for(var c:root.getComponents()){if(c instanceof JButton b&&b.getText().equals(text))return b;if(c instanceof Container nested){var found=find(nested,text);if(found!=null)return found;}}return null;}
    static JFrame frame(String title){for(var f:Frame.getFrames())if(f instanceof JFrame j&&j.isShowing()&&j.getTitle().equals(title))return j;return null;}
    static void capture(JFrame frame,Path path)throws Exception{var im=new java.awt.image.BufferedImage(frame.getWidth(),frame.getHeight(),2);var g=im.createGraphics();frame.printAll(g);g.dispose();javax.imageio.ImageIO.write(im,"png",path.toFile());}
    public static void main(String[] args)throws Exception {
        Path output=Path.of(System.getenv("SPIRITGATE_PROBE_OUTPUT"));Files.createDirectories(output);
        com.projetok.prototype.rpg.RpgLauncher.main(new String[0]);
        long deadline=System.nanoTime()+15_000_000_000L;JFrame[] entry={null};
        try{
            while(entry[0]==null&&System.nanoTime()<deadline){SwingUtilities.invokeAndWait(()->{for(var f:Frame.getFrames())if(f instanceof JFrame j&&j.isShowing()&&find(j,"Entrar em partida")!=null){entry[0]=j;break;}});Thread.sleep(30);}
            if(entry[0]==null)throw new AssertionError("Entry did not open");
            boolean guest=Boolean.getBoolean("spiritgate.guest");
            SwingUtilities.invokeAndWait(()->{try{
                var host=find(entry[0],"Hospedar via Radmin VPN");var join=find(entry[0],"Entrar em partida");
                if(host==null||host.isEnabled()==guest||join==null||!join.isEnabled())throw new AssertionError("Wrong role or missing Radmin controls");
                capture(entry[0],output.resolve("entry.png"));
                if(!guest){host.doClick();var panel=frame("Spirit Gate | Hospedar partida");if(panel==null)throw new AssertionError("Host panel missing");capture(panel,output.resolve("host.png"));panel.dispatchEvent(new WindowEvent(panel,WindowEvent.WINDOW_CLOSING));}
                join.doClick();var panel=frame("Spirit Gate | Entrar no mundo");if(panel==null)throw new AssertionError("Join panel missing");capture(panel,output.resolve("join.png"));panel.dispatchEvent(new WindowEvent(panel,WindowEvent.WINDOW_CLOSING));
            }catch(Exception error){throw new RuntimeException(error);}});
            Files.writeString(output.resolve("result.json"),"{\"ok\":true,\"guest\":"+guest+",\"radminRemoteTested\":false,\"humanApproved\":false}");
        }finally{SwingUtilities.invokeAndWait(()->{for(var f:Frame.getFrames())f.dispose();});}
    }
}
