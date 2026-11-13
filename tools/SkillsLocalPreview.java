import com.projetok.multiplayer.GameplayProtocol;
import com.projetok.multiplayer.LocalClient;
import com.projetok.multiplayer.LocalSimulationServer;
import com.projetok.multiplayer.NetworkPlayerVisuals;
import com.projetok.multiplayer.RpgNetworkClientLauncher;
import com.projetok.multiplayer.RpgSessionWorld;
import com.projetok.multiplayer.SessionCharacterSelection;
import com.projetok.prototype.rpg.CosmeticCatalog;
import com.projetok.prototype.rpg.RpgPlayerSprites;
import com.projetok.studio.AssetCatalog;
import com.projetok.studio.WorldContent;
import java.awt.GraphicsEnvironment;
import java.awt.GridLayout;
import java.awt.BorderLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.InetAddress;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.JCheckBox;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;

/** Prévia local de desenvolvimento: servidor sintético de nível 20, sem carregar personagens salvos. */
public final class SkillsLocalPreview {
    /** Usa IDs existentes para conservar a mesma seleção de domínio do jogo. */
    private record Kit(String label,String race,String domain) {
        @Override public String toString(){return label;}
    }
    private record Choice(Kit kit,boolean helper) { }
    private static final Kit[] KITS={
        new Kit("Água — Reaper","reaper","water"),
        new Kit("Vento — Reaper","reaper","wind"),
        new Kit("Raio — Reaper","reaper","thunder"),
        new Kit("Fogo — Reaper","reaper","fire"),
        new Kit("Spirit — Trevas: controle","spirit","darkness_control"),
        new Kit("Aetheri — Arco de longa distância","aetheri","bow_long"),
        new Kit("Luz — Human","human","light"),
        new Kit("Artista marcial — Human","human","martial")
    };
    private static final Kit[] LIVE_KITS=java.util.stream.Stream.concat(java.util.Arrays.stream(KITS),java.util.stream.Stream.of(
        new Kit("Aetheri — Arco de curta distância","aetheri","bow_short"),
        new Kit("Spirit — Trevas: efeitos negativos","spirit","darkness_debuff"))).toArray(Kit[]::new);
    private SkillsLocalPreview(){ }

    /** Mantém o destaque temporário do seletor sem criar outra janela de aplicação. */
    private static final class SelectionFocus extends WindowAdapter {
        private final JDialog dialog;
        private final JComboBox<?> kits;
        SelectionFocus(JDialog dialog,JComboBox<?> kits){this.dialog=dialog;this.kits=kits;}
        @Override public void windowOpened(WindowEvent event){dialog.toFront();dialog.requestFocusInWindow();}
        @Override public void windowActivated(WindowEvent event){dialog.setAlwaysOnTop(false);kits.requestFocusInWindow();}
    }

    /** Seleciona o kit antes de criar sockets; cancelar não inicia a sessão. */
    private static Choice choose()throws Exception {
        var result=new AtomicReference<Choice>();
        SwingUtilities.invokeAndWait(()->{
            var kits=new JComboBox<>(KITS);var helper=new JCheckBox("Abrir também personagem auxiliar para curas e revive",true);
            kits.setName("skillsKitChoice");helper.setName("skillsPreviewHelper");
            var contents=new JPanel(new GridLayout(0,1,6,8));
            contents.add(new JLabel("Treino local: nível 20, sem progresso pessoal."));contents.add(kits);contents.add(helper);
            contents.add(new JLabel("F5–F9: técnicas · TAB: inimigo · SPACE: ataque/sentry"));
            var pane=new JOptionPane(contents,JOptionPane.PLAIN_MESSAGE,JOptionPane.OK_CANCEL_OPTION);pane.setName("skillsKitOptionPane");
            JDialog dialog=pane.createDialog(null,"Spirit Gate | Revisar habilidades");
            dialog.setName("skillsKitDialog");dialog.pack();dialog.setLocationRelativeTo(null);
            // A seleção ganha destaque apenas na abertura, mesmo quando o console recebeu foco.
            dialog.setAlwaysOnTop(true);
            var foregroundLease=new Timer(1500,event->dialog.setAlwaysOnTop(false));foregroundLease.setRepeats(false);
            dialog.addWindowListener(new SelectionFocus(dialog,kits));
            try {
                foregroundLease.start();dialog.setVisible(true);
                if(pane.getValue() instanceof Integer value&&value==JOptionPane.OK_OPTION)
                    result.set(new Choice((Kit)kits.getSelectedItem(),helper.isSelected()));
            } finally {foregroundLease.stop();dialog.setAlwaysOnTop(false);dialog.dispose();}
        });return result.get();
    }

    /** Mantém as janelas abertas e troca somente as sessões efêmeras em uma thread de controle. */
    private static final class LivePreview implements AutoCloseable {
        final CosmeticCatalog catalog;
        final RpgPlayerSprites sprites;
        final RpgNetworkClientLauncher.Presentation presentation;
        final AssetCatalog.Result assets=new AssetCatalog.Result(List.of(),List.of(),List.of());
        final JFrame[] frames=new JFrame[2];
        final CountDownLatch closed=new CountDownLatch(1);
        final AtomicReference<Kit> requested=new AtomicReference<>();
        final boolean withHelper;
        LocalSimulationServer server;
        LocalClient main,helper;
        Kit active;
        LivePreview(Path root,boolean withHelper)throws Exception {
            this.withHelper=withHelper;
            catalog=CosmeticCatalog.load(root.resolve("assets/dev/generated/corujao-iv-v1"));
            sprites=RpgPlayerSprites.load(root.resolve("assets/dev/player-base"),250,.5);
            presentation=new RpgNetworkClientLauncher.Presentation(new NetworkPlayerVisuals(root.resolve("assets/dev/reaper"),sprites),catalog.onLogicalCanvas(64,64,32,60,.4));
        }
        /** Instala controles exclusivos da ferramenta, fora da HUD do jogo normal. */
        private void controls(Kit kit){
            var frame=frames[0];var kits=new JComboBox<>(LIVE_KITS);kits.setSelectedItem(kit);kits.setName("skillsLiveKitChoice");
            var apply=new JButton("Trocar kit / reiniciar treino");apply.setName("skillsLiveKitApply");apply.setEnabled(false);
            var notice=new JLabel("Sincronizando o novo treino...");notice.setName("skillsLiveKitStatus");
            var row=new JPanel(new BorderLayout(6,0));row.add(new JLabel("Domínio de treino:"),BorderLayout.WEST);row.add(kits,BorderLayout.CENTER);row.add(apply,BorderLayout.EAST);
            var bar=new JPanel(new BorderLayout(6,3));bar.setBorder(javax.swing.BorderFactory.createEmptyBorder(4,8,4,8));bar.add(row,BorderLayout.CENTER);bar.add(notice,BorderLayout.SOUTH);
            bar.setToolTipText("A troca reinicia apenas o treino temporário, com HP, energia e recargas novos.");
            apply.addActionListener(event->{
                if(requested.compareAndSet(null,(Kit)kits.getSelectedItem())){
                    apply.setEnabled(false);kits.setEnabled(false);notice.setText("Trocando kit...");
                    frame.getRootPane().putClientProperty("skillsLiveSwitching",Boolean.TRUE);
                }
            });
            var game=frame.getContentPane();var wrapper=new JPanel(new BorderLayout());wrapper.add(bar,BorderLayout.NORTH);wrapper.add(game,BorderLayout.CENTER);
            frame.setContentPane(wrapper);frame.getRootPane().putClientProperty("skillsLiveKitApply",apply);
            frame.getRootPane().putClientProperty("skillsLiveKitChoice",kits);frame.getRootPane().putClientProperty("skillsLiveKitStatus",notice);
            frame.getRootPane().putClientProperty("skillsLiveSwitching",Boolean.TRUE);frame.revalidate();frame.repaint();
        }
        /** Prepara os transportes antes de substituir a apresentação e descarta a sessão anterior. */
        void start(Kit kit)throws Exception {
            var oldServer=server;var oldMain=main;var oldHelper=helper;
            var world=new RpgSessionWorld(List.of(kit.race(),"human"),catalog,true,20);world.enableRevisions(WorldContent.baseline());
            var nextServer=new LocalSimulationServer(InetAddress.getLoopbackAddress(),0,world);
            LocalClient nextMain=null,nextHelper=null;boolean mounted=false;
            try{
                nextServer.start();
                nextMain=LocalClient.skillGame(InetAddress.getLoopbackAddress(),nextServer.port(),new SessionCharacterSelection("Treino Local",kit.race(),"none","none"),false,false,null,assets);
                if(withHelper)nextHelper=LocalClient.skillGame(InetAddress.getLoopbackAddress(),nextServer.port(),new SessionCharacterSelection("Aliado de Treino","human","none","none"),false,false,null,assets);
                var client=nextMain;var support=nextHelper;
                SwingUtilities.invokeAndWait(()->{
                    if(closed.getCount()==0)throw new IllegalStateException("A janela de treino foi encerrada");
                    if(frames[0]==null){
                        frames[0]=RpgNetworkClientLauncher.open(client,sprites,"Treino local",catalog,CosmeticCatalog.Selection.none(),f->{},presentation);
                        frames[0].addWindowListener(new WindowAdapter(){@Override public void windowClosed(WindowEvent event){closed.countDown();}});
                    }else RpgNetworkClientLauncher.replacePreview(frames[0],client,sprites,"Treino local",catalog,CosmeticCatalog.Selection.none(),presentation);
                    frames[0].setTitle("Spirit Gate | Treino — "+kit.label());controls(kit);
                    if(support!=null){
                        if(frames[1]==null||!frames[1].isDisplayable()){
                            frames[1]=RpgNetworkClientLauncher.open(support,sprites,"Aliado auxiliar local",catalog,CosmeticCatalog.Selection.none(),f->{},presentation);
                            frames[1].setLocation(100,80);
                        }else RpgNetworkClientLauncher.replacePreview(frames[1],support,sprites,"Aliado auxiliar local",catalog,CosmeticCatalog.Selection.none(),presentation);
                        frames[1].setTitle("Spirit Gate | Aliado de treino — Human/Luz");
                    }
                    frames[0].toFront();
                });
                mounted=true;server=nextServer;main=nextMain;helper=nextHelper;active=kit;
                System.out.println("[Treino] Sincronizando "+kit.label()+"...");
                awaitReady(server,main,helper);chooseDomain(main,kit.domain());if(helper!=null)chooseDomain(helper,"light");
                status("Pronto — HP, energia e recargas renovados");
                System.out.println("[Treino] Kit confirmado: "+kit.label()+"; porta "+server.port());
            }finally{
                if(mounted){if(oldHelper!=null)oldHelper.close();if(oldMain!=null)oldMain.close();if(oldServer!=null)oldServer.close();}
                else{if(nextHelper!=null)nextHelper.close();if(nextMain!=null)nextMain.close();nextServer.close();}
            }
        }
        /** Atualiza a barra sem bloquear a EDT durante rede, carregamento ou sincronização. */
        void status(String text)throws Exception {
            SwingUtilities.invokeAndWait(()->{
                if(frames[0]==null||!frames[0].isDisplayable())return;
                ((JLabel)frames[0].getRootPane().getClientProperty("skillsLiveKitStatus")).setText(text);
                ((JButton)frames[0].getRootPane().getClientProperty("skillsLiveKitApply")).setEnabled(true);
                ((JComboBox<?>)frames[0].getRootPane().getClientProperty("skillsLiveKitChoice")).setEnabled(true);
                frames[0].getRootPane().putClientProperty("skillsLiveSwitching",Boolean.FALSE);
                var frame=frames[0];frame.validate();frame.toFront();
                if(frame.getRootPane().getClientProperty("networkPanel") instanceof javax.swing.JComponent game){
                    game.requestFocusInWindow();
                    // A validação da nova hierarquia pode terminar no próximo evento AWT.
                    SwingUtilities.invokeLater(()->{if(frame.isDisplayable()&&game.isShowing()&&frame.getRootPane().getClientProperty("networkPanel")==game)game.requestFocusInWindow();});
                }
            });
        }
        @Override public void close()throws Exception {
            SwingUtilities.invokeAndWait(()->{for(var frame:frames)if(frame!=null)frame.dispose();});
            if(helper!=null)helper.close();if(main!=null)main.close();if(server!=null)server.close();
        }
    }

    /** Abre clientes reais; somente o fechamento da janela principal encerra o servidor local. */
    public static void main(String[] args)throws Exception {
        if(GraphicsEnvironment.isHeadless())throw new IllegalStateException("A prévia precisa de uma sessão gráfica do Windows.");
        Path root=(args.length==0?Path.of("."):Path.of(args[0])).toAbsolutePath().normalize();
        if(!Files.isRegularFile(root.resolve("assets/skills/manifest.properties")))throw new IllegalArgumentException("Execute tools/testar-skills.cmd no checkout que contém assets/skills.");
        System.out.println("[Prévia 1/6] Abrindo o seletor de habilidades...");
        Choice choice=choose();if(choice==null){System.out.println("Prévia cancelada.");return;}
        Path temporaryParent=Files.createDirectories(root.resolve(".task-checkpoints/SKILLS40/preview-data"));
        Path temporary=Files.createTempDirectory(temporaryParent,"session-");
        String previous=System.getProperty("spiritgate.rpg.dataDir");System.setProperty("spiritgate.rpg.dataDir",temporary.toString());
        LivePreview live=null;
        try {
            System.out.println("[Prévia 2/6] Carregando os sprites locais de "+choice.kit().label()+"...");
            live=new LivePreview(root,choice.helper());
            System.out.println("[Prévia 3/6] Preparando o servidor local...");
            System.out.println("[Prévia 4/6] Abrindo a janela do jogo...");
            System.out.println("[Prévia 5/6] Sincronizando os personagens e escolhendo os domínios...");
            live.start(choice.kit());
            System.out.println("[Prévia 6/6] Use o seletor no topo para trocar o kit sem fechar a janela.");
            System.out.println("Kit: "+choice.kit().label()+". Preferências temporárias: "+temporary);
            while(!live.closed.await(100,TimeUnit.MILLISECONDS)){
                Kit next=live.requested.getAndSet(null);
                if(next!=null)try{live.start(next);}catch(Exception error){
                    if(live.closed.getCount()==0)break;
                    System.err.println("[Treino] Troca recusada: "+error.getMessage());live.status("Falha na troca — tente novamente");
                }
                if(live.server.failure()!=null)throw new IllegalStateException("Falha no servidor de treino",live.server.failure());
            }
        } catch(Exception error){
            if(live!=null&&live.closed.getCount()==0)return;
            System.err.println("Falha na prévia: "+error.getMessage());
            var owner=live==null?null:live.frames[0];
            SwingUtilities.invokeAndWait(()->JOptionPane.showMessageDialog(owner,"Não foi possível manter a prévia: "+error.getMessage(),"Treino de habilidades",JOptionPane.ERROR_MESSAGE));
            throw error;
        } finally {
            if(live!=null)live.close();
            if(previous==null)System.clearProperty("spiritgate.rpg.dataDir");else System.setProperty("spiritgate.rpg.dataDir",previous);
            removeOwnTemporaryPreferences(temporaryParent,temporary);
        }
    }

    /** Aguarda todos os personagens da prévia antes de enviar ações, preservando a barreira do servidor. */
    private static void awaitReady(LocalSimulationServer server,LocalClient... clients)throws Exception {
        long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(15);
        while(System.nanoTime()<deadline){
            if(server.failure()!=null)throw new IllegalStateException("Falha no mundo de treino",server.failure());
            boolean ready=true;
            for(var client:clients)if(client!=null){
                if(!client.connected())throw new IllegalStateException("Personagem desconectado durante a sincronização: "+client.problem());
                if(client.revisions().blocked()||client.snapshot()==null||client.playerSheet()==null||client.skills()==null)ready=false;
            }
            if(ready)return;
            Thread.sleep(25);
        }
        throw new IllegalStateException("A sessão de treino não terminou a sincronização de todos os personagens.");
    }
    /** Escolhe o domínio pelo mesmo pedido validado usado na interface normal. */
    private static void chooseDomain(LocalClient client,String domain)throws Exception {
        if(!client.gameplay(GameplayProtocol.Operation.CHOOSE_DOMAIN,domain))throw new IllegalStateException("Não foi possível enviar a escolha de domínio.");
        long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(5);
        while(System.nanoTime()<deadline){
            if(!client.connected())throw new IllegalStateException("Personagem desconectado ao escolher domínio: "+client.problem());
            var sheet=client.playerSheet();
            if(sheet!=null&&domain.equals(sheet.progress().domain()))return;
            Thread.sleep(25);
        }
        throw new IllegalStateException("O servidor não confirmou o domínio de treino: "+domain+".");
    }
    /** Limpa apenas os três caminhos próprios; arquivos inesperados são preservados para inspeção. */
    private static void removeOwnTemporaryPreferences(Path parent,Path temporary)throws Exception {
        Path checked=temporary.toRealPath();if(!checked.getParent().equals(parent.toRealPath())||!checked.getFileName().toString().startsWith("session-"))throw new IllegalStateException("Diretório temporário fora da área de treino.");
        Files.deleteIfExists(checked.resolve("saves/preferences.properties"));
        try {Files.deleteIfExists(checked.resolve("saves"));Files.deleteIfExists(checked);}
        catch(DirectoryNotEmptyException preserved){System.out.println("Arquivos adicionais preservados em "+checked);}
    }
}
