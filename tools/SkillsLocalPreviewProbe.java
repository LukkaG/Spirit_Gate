import com.projetok.multiplayer.LocalClient;
import com.projetok.multiplayer.LocalSimulationServer;
import com.projetok.multiplayer.NetworkSkillPresentation;
import com.projetok.multiplayer.RpgSessionWorld;
import com.projetok.multiplayer.SessionCharacterSelection;
import com.projetok.studio.AssetCatalog;
import com.projetok.studio.WorldContent;
import java.awt.Component;
import java.awt.Container;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import javax.imageio.ImageIO;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/** Prova gráfica isolada: testa o seletor e uma inicialização real sem interagir com outro processo Java. */
public final class SkillsLocalPreviewProbe {
    private static Path output;
    private static final String[] DOMAINS={"water","wind","thunder","fire","darkness_control","bow_long","light","martial"};
    private static final String[] KITS={"water","wind","thunder","fire","spirit","aetheri","light","martial"};
    private SkillsLocalPreviewProbe(){ }

    /** Executa a ferramenta somente neste JVM; o fechamento conserva o caminho normal de cleanup. */
    public static void main(String[] args)throws Exception {
        Path root=(args.length==0?Path.of("."):Path.of(args[0])).toAbsolutePath().normalize();
        output=Files.createDirectories(root.resolve(".task-checkpoints/SKILLS40/preview-domain-fix/evidence"));
        selector(false);selector(true);barrier();
        for(int kit=0;kit<KITS.length;kit++)boot(root,kit,true);
        boot(root,0,false);
        Files.writeString(output.resolve("result.json"),"{\"selectorVisible\":true,\"eightKits\":true,\"cancel\":true,\"selection\":true,\"twoClientBarrier\":true,\"allEightKitsWithHelper\":true,\"waterWithoutHelper\":true,\"bothDomainsConfirmed\":true,\"bothSkillStates\":true,\"realSprites\":true,\"allServersClosed\":true,\"physicalUserApproval\":false}\n");
        System.out.println("SKILLS_LOCAL_PREVIEW_PROBE_PASS");
    }

    /** Confirma oito opções, limites visíveis e retorno correto tanto em Cancelar quanto em OK. */
    private static void selector(boolean confirm)throws Exception {
        var failure=new AtomicReference<Throwable>();var observed=new AtomicBoolean();long until=System.nanoTime()+TimeUnit.SECONDS.toNanos(8);
        var timer=new Timer(50,null);
        timer.addActionListener(event->{
            try {
                JDialog dialog=dialog();
                if(dialog==null){if(System.nanoTime()>until)throw new AssertionError("O seletor não ficou visível");return;}
                check(dialog.isShowing()&&dialog.isModal(),"O seletor precisa ser modal e visível");
                Rectangle screen=dialog.getGraphicsConfiguration().getBounds();check(screen.contains(dialog.getBounds()),"Seletor fora da tela");
                var kits=(JComboBox<?>)find(dialog,"skillsKitChoice");var helper=(JCheckBox)find(dialog,"skillsPreviewHelper");
                check(kits!=null&&kits.getItemCount()==8,"As oito opções de kit precisam estar disponíveis");check(helper!=null,"Opção de aliado ausente");
                if(confirm){kits.setSelectedIndex(2);helper.setSelected(false);}
                capture(dialog,output.resolve(confirm?"selector-selection.png":"selector-visible.png"));
                observed.set(true);timer.stop();((JOptionPane)find(dialog,"skillsKitOptionPane")).setValue(confirm?JOptionPane.OK_OPTION:JOptionPane.CANCEL_OPTION);
            } catch(Throwable error){failure.set(error);timer.stop();disposeOwnWindows();}
        });
        Method method=SkillsLocalPreview.class.getDeclaredMethod("choose");method.setAccessible(true);
        Object result;
        SwingUtilities.invokeAndWait(timer::start);
        try {result=method.invoke(null);}finally{SwingUtilities.invokeAndWait(timer::stop);}
        rethrow(failure.get());check(observed.get(),"Nenhum seletor foi observado");
        if(!confirm)check(result==null,"Cancelar não deve iniciar a prévia");
        else {
            check(result!=null,"OK deve retornar a seleção");
            Method kit=result.getClass().getDeclaredMethod("kit"),helper=result.getClass().getDeclaredMethod("helper");kit.setAccessible(true);helper.setAccessible(true);
            Object selected=kit.invoke(result);Method domain=selected.getClass().getDeclaredMethod("domain");domain.setAccessible(true);
            check("thunder".equals(domain.invoke(selected)),"O kit escolhido precisa conservar o domínio real");check(Boolean.FALSE.equals(helper.invoke(result)),"A opção de auxiliar precisa ser respeitada");
        }
        SwingUtilities.invokeAndWait(()->{for(Window window:Window.getWindows())if(window instanceof JDialog dialog&&"skillsKitDialog".equals(dialog.getName()))check(!dialog.isDisplayable()&&!dialog.isAlwaysOnTop(),"O seletor precisa liberar seus recursos");});
    }

    /** Impede que o cliente já pronto selecione domínio enquanto o segundo mantém a barreira inicial. */
    private static void barrier()throws Exception {
        var world=new RpgSessionWorld(List.of("reaper","human"),null,true,20);world.enableRevisions(WorldContent.baseline());
        // Mantém o relógio inicial em zero até as admissões e o protocolo liberarem a barreira.
        world.freeze(true);
        var empty=new AssetCatalog.Result(List.of(),List.of(),List.of());var held=new CompletableFuture<Void>();var rendererCalled=new AtomicBoolean();
        Thread waiter=null;
        try(var server=new LocalSimulationServer(InetAddress.getLoopbackAddress(),0,world)){
            server.start();
            try(var main=LocalClient.skillGame(InetAddress.getLoopbackAddress(),server.port(),new SessionCharacterSelection("Barreira Vento","reaper","none","none"),false,false,null,empty);
                var helper=LocalClient.skillGame(InetAddress.getLoopbackAddress(),server.port(),new SessionCharacterSelection("Barreira Aliado","human","none","none"),false,false,null,empty)){
                main.revisions().renderer(prepared->CompletableFuture.completedFuture(null));helper.revisions().renderer(prepared->{rendererCalled.set(true);return held;});
                long until=System.nanoTime()+TimeUnit.SECONDS.toNanos(10);
                while((main.revisions().blocked()||main.playerSheet()==null||main.skills()==null||!rendererCalled.get())&&System.nanoTime()<until)Thread.sleep(10);
                check(!main.revisions().blocked()&&main.playerSheet()!=null&&main.skills()!=null,"O cliente principal precisa estar pronto antes do auxiliar");
                check(rendererCalled.get()&&helper.revisions().blocked(),"A revisão do auxiliar precisa continuar retida");
                check(server.snapshot().tick()==0,"A barreira inicial precisa manter o relógio congelado");
                Method ready=SkillsLocalPreview.class.getDeclaredMethod("awaitReady",LocalSimulationServer.class,LocalClient[].class);ready.setAccessible(true);
                var pending=new FutureTask<Void>(()->{ready.invoke(null,server,new LocalClient[]{main,helper});return null;});
                waiter=new Thread(pending,"preview-barrier-waiter");waiter.start();
                long entered=System.nanoTime()+TimeUnit.SECONDS.toNanos(3);
                while(!insideWait(waiter)&&!pending.isDone()&&System.nanoTime()<entered)Thread.sleep(5);
                check(insideWait(waiter),"A espera conjunta precisa alcançar awaitReady");
                try {pending.get(250,TimeUnit.MILLISECONDS);throw new AssertionError("A espera encerrou antes de liberar o renderer do auxiliar");}
                catch(TimeoutException expected){/* Aguardar o segundo cliente é o resultado obrigatório. */}
                check(main.playerSheet().progress().domain().equals("none"),"Nenhum domínio deve ser antecipado durante a barreira");
                held.complete(null);pending.get(12,TimeUnit.SECONDS);
                Method choose=SkillsLocalPreview.class.getDeclaredMethod("chooseDomain",LocalClient.class,String.class);choose.setAccessible(true);
                choose.invoke(null,main,"wind");choose.invoke(null,helper,"light");
                check(main.playerSheet()!=null&&"wind".equals(main.playerSheet().progress().domain()),"Vento não confirmado após liberar a barreira");
                check(helper.playerSheet()!=null&&"light".equals(helper.playerSheet().progress().domain()),"Luz não confirmada após liberar a barreira");
                check(main.skills()!=null&&helper.skills()!=null,"Ambos precisam receber o estado de habilidades");check(server.failure()==null,"O servidor de barreira falhou");
                Files.writeString(output.resolve("barrier.json"),"{\"mainReadyWhileHelperBlocked\":true,\"initialTick\":0,\"waitedBeforeRelease\":true,\"windConfirmed\":true,\"lightConfirmed\":true}\n");
            }
        } finally {held.complete(null);if(waiter!=null&&waiter.isAlive()){waiter.interrupt();waiter.join(2000);}}
    }
    private static boolean insideWait(Thread thread){for(StackTraceElement frame:thread.getStackTrace())if(frame.getClassName().equals("SkillsLocalPreview")&&frame.getMethodName().equals("awaitReady"))return true;return false;}

    /** Inicia main com o kit indicado, aguarda os dois clientes e fecha ambos pelo fluxo normal. */
    private static void boot(Path root,int kit,boolean withHelper)throws Exception {
        var failure=new AtomicReference<Throwable>();var appFailure=new AtomicReference<Throwable>();var verified=new AtomicBoolean();
        var clientReference=new AtomicReference<LocalClient>();var helperReference=new AtomicReference<LocalClient>();var temporary=new AtomicReference<Path>();var port=new AtomicInteger();
        long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(35);var timer=new Timer(50,null);var chosen=new AtomicBoolean();
        timer.addActionListener(event->{
            try {
                if(System.nanoTime()>deadline)throw new AssertionError("A prévia não confirmou mundo e domínio no prazo");
                for(Window window:Window.getWindows())if(window instanceof JDialog dialog&&dialog.isShowing()&&"Treino de habilidades".equals(dialog.getTitle()))throw new AssertionError("A ferramenta exibiu erro ao iniciar "+KITS[kit]+" com auxiliar="+withHelper);
                if(!chosen.get()){
                    var chooser=dialog();if(chooser==null)return;
                    var kits=(JComboBox<?>)find(chooser,"skillsKitChoice");kits.setSelectedIndex(kit);((JCheckBox)find(chooser,"skillsPreviewHelper")).setSelected(withHelper);
                    chosen.set(true);((JOptionPane)find(chooser,"skillsKitOptionPane")).setValue(JOptionPane.OK_OPTION);return;
                }
                JFrame game=null,allyWindow=null;
                for(Window window:Window.getWindows())if(window instanceof JFrame frame&&frame.isShowing()){
                    if(frame.getTitle().startsWith("Spirit Gate | Treino —"))game=frame;
                    else if(frame.getTitle().startsWith("Spirit Gate | Aliado de treino —"))allyWindow=frame;
                }
                if(game==null)return;
                Object value=game.getRootPane().getClientProperty("networkClient");if(!(value instanceof LocalClient client))return;
                var sheet=client.playerSheet();var effects=(NetworkSkillPresentation)game.getRootPane().getClientProperty("networkSkillPresentation");
                if(sheet==null||!sheet.progress().domain().equals(DOMAINS[kit])||client.skills()==null||effects==null||effects.clip(KITS[kit]+"_1")==null)return;
                if(withHelper){
                    if(allyWindow==null)return;
                    var ally=(LocalClient)allyWindow.getRootPane().getClientProperty("networkClient");var allyEffects=(NetworkSkillPresentation)allyWindow.getRootPane().getClientProperty("networkSkillPresentation");
                    if(ally==null||ally.playerSheet()==null||!"light".equals(ally.playerSheet().progress().domain())||ally.skills()==null||allyEffects==null||allyEffects.clip("light_1")==null)return;
                    check(ally.playerSheet().progress().level()==20&&ally.connected(),"O auxiliar real precisa estar pronto no nível 20");helperReference.set(ally);
                }
                check(sheet.progress().level()==20,"A prévia precisa começar no nível 20");check(client.connected(),"Cliente real não conectado");
                var field=LocalClient.class.getDeclaredField("port");field.setAccessible(true);port.set(field.getInt(client));clientReference.set(client);
                temporary.set(Path.of(System.getProperty("spiritgate.rpg.dataDir")));
                String label="preview-"+KITS[kit]+(withHelper?"-helper":"-alone");capture(game,output.resolve(label+"-main.png"));if(withHelper)capture(allyWindow,output.resolve(label+"-ally.png"));
                verified.set(true);timer.stop();game.dispatchEvent(new WindowEvent(game,WindowEvent.WINDOW_CLOSING));
            } catch(Throwable error){failure.set(error);timer.stop();disposeOwnWindows();}
        });
        var app=new Thread(()->{try{SkillsLocalPreview.main(new String[]{root.toString()});}catch(Throwable error){appFailure.set(error);}},"skills-preview-probe-app");app.setDaemon(true);
        try {
            SwingUtilities.invokeAndWait(timer::start);app.start();app.join(TimeUnit.SECONDS.toMillis(40));
            check(!app.isAlive(),"main da prévia não encerrou");rethrow(failure.get());rethrow(appFailure.get());check(verified.get(),"O mundo não foi observado pronto");
            check(clientReference.get()!=null&&!clientReference.get().connected(),"O cliente precisa fechar junto à janela");
            check(!withHelper||helperReference.get()!=null&&!helperReference.get().connected(),"O auxiliar precisa fechar junto à janela principal");
            check(temporary.get()!=null&&!Files.exists(temporary.get()),"A pasta temporária própria precisa ser limpa");
            boolean refused=false;
            try(var socket=new Socket()){socket.connect(new InetSocketAddress(InetAddress.getLoopbackAddress(),port.get()),500);}
            catch(java.net.ConnectException expected){refused=true;}
            check(refused,"O servidor local continuou aceitando conexões após encerrar main");
            System.out.println("PREVIEW_BOOT_PASS kit="+KITS[kit]+" helper="+withHelper);
        } finally {
            SwingUtilities.invokeAndWait(()->{timer.stop();disposeOwnWindows();});
            if(app.isAlive()){app.interrupt();app.join(TimeUnit.SECONDS.toMillis(3));}
        }
    }

    /** Localiza somente o seletor criado neste processo de teste. */
    private static JDialog dialog(){for(Window window:Window.getWindows())if(window instanceof JDialog dialog&&dialog.isShowing()&&"skillsKitDialog".equals(dialog.getName()))return dialog;return null;}
    private static Component find(Container parent,String name){if(name.equals(parent.getName()))return parent;for(Component child:parent.getComponents()){if(name.equals(child.getName()))return child;if(child instanceof Container nested){var found=find(nested,name);if(found!=null)return found;}}return null;}
    /** Captura o componente Swing real, sem desenhar uma interface alternativa para o teste. */
    private static void capture(Window window,Path path)throws Exception {var pixels=new BufferedImage(window.getWidth(),window.getHeight(),BufferedImage.TYPE_INT_RGB);var graphics=pixels.createGraphics();try{window.printAll(graphics);}finally{graphics.dispose();}ImageIO.write(pixels,"png",path.toFile());}
    private static void disposeOwnWindows(){for(Window window:Window.getWindows())window.dispose();}
    private static void check(boolean condition,String message){if(!condition)throw new AssertionError(message);}
    private static void rethrow(Throwable error)throws Exception {if(error instanceof Exception exception)throw exception;if(error instanceof Error failure)throw failure;}
}
