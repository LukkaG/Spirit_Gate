import com.projetok.multiplayer.*;
import java.awt.*;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.net.*;
import java.nio.file.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import javax.imageio.ImageIO;
import javax.swing.*;

/** Prova isolada da ferramenta real: oito kits, mesma janela, sem acumular clientes ou listeners. */
public final class SkillsLiveSwitchProbe {
    private static final String[] DOMAINS={"water","wind","thunder","fire","darkness_control","bow_long","light","martial","bow_short","darkness_debuff"};
    private static final String[] RACES={"reaper","reaper","reaper","reaper","spirit","aetheri","human","human","aetheri","spirit"};
    private static final int[] ORDER={0,5,8,7,6,1,2,3,4,9,0};
    private SkillsLiveSwitchProbe(){}
    public static void main(String[] args)throws Exception{
        var root=Path.of(args.length==0?".":args[0]).toAbsolutePath().normalize();
        var evidence=Files.createDirectories(root.resolve(".task-checkpoints/SKILLS40/live-switch/evidence"));
        run(root,evidence,true);run(root,evidence,false);
        Files.writeString(evidence.resolve("result.json"),"{\"tenDomains\":true,\"eightSkillKits\":true,\"focusRestored\":true,\"controlsFit640x480\":true,\"sameMainWindow\":true,\"sameHelperWindow\":true,\"boundsPreserved\":true,\"listenersStable\":true,\"oldClientsClosed\":true,\"oldPortsClosed\":true,\"closeWhileSwitching\":true,\"helperAndAlone\":true,\"physicalUserApproval\":false}\n");
        System.out.println("LIVE_SWITCH_PROBE_PASS");
    }
    /** Usa os controles públicos da janela e a entrada main, não um simulador alternativo da troca. */
    private static void run(Path root,Path evidence,boolean withHelper)throws Exception{
        var appFailure=new AtomicReference<Throwable>();var failure=new AtomicReference<Throwable>();
        var app=new Thread(()->{try{SkillsLocalPreview.main(new String[]{root.toString()});}catch(Throwable error){appFailure.set(error);}},"live-switch-app");
        app.setDaemon(true);
        JFrame[] identity=new JFrame[2];Rectangle[] bounds=new Rectangle[2];
        LocalClient[] current=new LocalClient[2],previous=new LocalClient[2];
        int[] index={0},oldPort={0},listeners={-1,-1,-1};boolean[] chosen={false},waiting={false},closing={false};
        Path[] temporary={null};long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(50);
        var timer=new Timer(30,null);
        timer.addActionListener(event->{try{
            if(System.nanoTime()>deadline)throw new AssertionError("Troca não terminou no prazo: kit="+ORDER[index[0]]+", foco="+KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner());
            if(!chosen[0]){
                for(Window window:Window.getWindows())if(window instanceof JDialog d&&d.isShowing()&&"skillsKitDialog".equals(d.getName())){
                    ((JCheckBox)find(d,"skillsPreviewHelper")).setSelected(withHelper);
                    ((JOptionPane)find(d,"skillsKitOptionPane")).setValue(JOptionPane.OK_OPTION);chosen[0]=true;return;
                }
                return;
            }
            JFrame game=null,helper=null;
            for(Window window:Window.getWindows())if(window instanceof JFrame frame&&frame.isShowing()){
                if(frame.getTitle().startsWith("Spirit Gate | Treino —"))game=frame;
                if(frame.getTitle().startsWith("Spirit Gate | Aliado de treino —"))helper=frame;
            }
            if(game==null)return;
            if(closing[0]){
                var pending=(LocalClient)game.getRootPane().getClientProperty("networkClient");
                if(pending!=current[0]){timer.stop();game.dispatchEvent(new WindowEvent(game,WindowEvent.WINDOW_CLOSING));}
                return;
            }
            if(Boolean.TRUE.equals(game.getRootPane().getClientProperty("skillsLiveSwitching")))return;
            var client=(LocalClient)game.getRootPane().getClientProperty("networkClient");
            int kit=ORDER[index[0]];
            if(!ready(client,DOMAINS[kit]))return;
            if(KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner()!=game.getRootPane().getClientProperty("networkPanel"))return;
            if(waiting[0]&&client==current[0])return;
            check(client.characterSelection().race().equals(RACES[kit]),"Raça não acompanhou o kit");
            if(withHelper&&(helper==null||!ready((LocalClient)helper.getRootPane().getClientProperty("networkClient"),"light")))return;
            if(identity[0]==null){
                identity[0]=game;identity[1]=helper;game.setSize(1000,700);bounds[0]=game.getBounds();
                if(helper!=null){helper.setSize(760,540);bounds[1]=helper.getBounds();}
                listeners[0]=game.getWindowListeners().length;listeners[1]=game.getWindowFocusListeners().length;
                listeners[2]=KeyboardFocusManager.getCurrentKeyboardFocusManager().getPropertyChangeListeners("focusOwner").length;
                temporary[0]=Path.of(System.getProperty("spiritgate.rpg.dataDir"));
            }else{
                check(game==identity[0]&&game.isDisplayable(),"Janela principal foi substituída/fechada");
                check(game.getBounds().equals(bounds[0]),"Limites da janela mudaram");
                if(withHelper){check(helper==identity[1],"Janela auxiliar foi substituída");check(helper.getBounds().equals(bounds[1]),"Limites do auxiliar mudaram");}
                check(game.getWindowListeners().length==listeners[0],"Acúmulo de listeners de janela");
                check(game.getWindowFocusListeners().length==listeners[1],"Acúmulo de listeners de foco");
                check(KeyboardFocusManager.getCurrentKeyboardFocusManager().getPropertyChangeListeners("focusOwner").length==listeners[2],"Acúmulo de roteadores de teclado");
                check(previous[0]==null||!previous[0].connected(),"Cliente anterior ficou aberto");
                check(previous[1]==null||!previous[1].connected(),"Auxiliar anterior ficou aberto");
                if(oldPort[0]!=0&&!refused(oldPort[0]))return;
            }
            current[0]=client;current[1]=helper==null?null:(LocalClient)helper.getRootPane().getClientProperty("networkClient");waiting[0]=false;
            if(index[0]==ORDER.length-1){
                capture(game,evidence.resolve(withHelper?"seletor-com-aliado.png":"seletor-sem-aliado.png"));
                game.setSize(640,480);game.validate();
                var button=(JButton)game.getRootPane().getClientProperty("skillsLiveKitApply");
                var buttonBounds=SwingUtilities.convertRectangle(button.getParent(),button.getBounds(),game.getContentPane());
                check(new Rectangle(0,0,game.getContentPane().getWidth(),game.getContentPane().getHeight()).contains(buttonBounds),"Botão de troca cortado em 640x480");
                capture(game,evidence.resolve(withHelper?"seletor-640-com-aliado.png":"seletor-640-sem-aliado.png"));game.setBounds(bounds[0]);game.validate();
                System.out.println("LIVE_SWITCH_KITS_PASS helper="+withHelper);
                // Fecha depois que a nova sessão já foi montada, sem permitir ressuscitar a janela.
                closing[0]=true;((JComboBox<?>)game.getRootPane().getClientProperty("skillsLiveKitChoice")).setSelectedIndex(2);
                ((JButton)game.getRootPane().getClientProperty("skillsLiveKitApply")).doClick();return;
            }
            previous[0]=client;previous[1]=current[1];oldPort[0]=port(client);index[0]++;waiting[0]=true;
            ((JComboBox<?>)game.getRootPane().getClientProperty("skillsLiveKitChoice")).setSelectedIndex(ORDER[index[0]]);
            ((JButton)game.getRootPane().getClientProperty("skillsLiveKitApply")).doClick();
        }catch(Throwable error){failure.set(error);timer.stop();for(Window window:Window.getWindows())window.dispose();}});
        try{
            SwingUtilities.invokeAndWait(timer::start);app.start();app.join(TimeUnit.SECONDS.toMillis(55));
            check(!app.isAlive(),"Processo da ferramenta não encerrou");rethrow(failure.get());rethrow(appFailure.get());
            check(index[0]==ORDER.length-1&&closing[0],"Nem todos os kits foram conferidos");
            check(current[0]!=null&&!current[0].connected(),"Cliente final não encerrou");
            check(!Files.exists(temporary[0]),"Preferências temporárias não foram limpas");
            SwingUtilities.invokeAndWait(()->{for(Window window:Window.getWindows())check(!window.isShowing(),"Janela reapareceu após encerrar");});
        }finally{SwingUtilities.invokeAndWait(()->{timer.stop();for(Window window:Window.getWindows())window.dispose();});if(app.isAlive()){app.interrupt();app.join(3000);}}
    }
    private static boolean ready(LocalClient c,String domain){return c!=null&&c.connected()&&c.playerSheet()!=null&&c.skills()!=null&&!c.revisions().blocked()&&c.playerSheet().progress().domain().equals(domain);}
    private static Component find(Container parent,String name){if(name.equals(parent.getName()))return parent;for(Component c:parent.getComponents()){if(name.equals(c.getName()))return c;if(c instanceof Container nested){var found=find(nested,name);if(found!=null)return found;}}return null;}
    private static int port(LocalClient c)throws Exception{var field=LocalClient.class.getDeclaredField("port");field.setAccessible(true);return field.getInt(c);}
    private static boolean refused(int port)throws Exception{try(var socket=new Socket()){socket.connect(new InetSocketAddress(InetAddress.getLoopbackAddress(),port),100);return false;}catch(ConnectException expected){return true;}}
    private static void capture(JFrame frame,Path path)throws Exception{var image=new BufferedImage(frame.getWidth(),frame.getHeight(),BufferedImage.TYPE_INT_RGB);var g=image.createGraphics();try{frame.printAll(g);}finally{g.dispose();}ImageIO.write(image,"png",path.toFile());}
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    private static void rethrow(Throwable error)throws Exception{if(error instanceof Exception e)throw e;if(error instanceof Error e)throw e;}
}
