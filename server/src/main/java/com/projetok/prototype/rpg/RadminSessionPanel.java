package com.projetok.prototype.rpg;

import com.projetok.multiplayer.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.net.*;
import java.nio.file.*;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.Callable;

/** Integrated private session controller. Only owns servers created by this window. */
public final class RadminSessionPanel extends JPanel {
    private final JComboBox<String> source=new JComboBox<>();
    private final JTextField host=new JTextField("26.",18),port=new JTextField("54557",6),name=new JTextField("Viajante",18);
    private final JLabel status=new JLabel("Servidor desligado"),vpn=new JLabel("Verificando adaptadores...");
    private final JButton start,connect,join;
    private LocalSimulationServer server;
    private LocalClient client;
    private JFrame game;
    private boolean busy,closed;
    private final Timer timer;
    private final JFrame frame;
    private final boolean hosting;
    private record Resources(CosmeticCatalog catalog,RpgPlayerSprites sprites) { }
    public static void open(boolean hosting){if(hosting&&Boolean.getBoolean("spiritgate.guest"))throw new IllegalStateException("Modo convidado nao hospeda");new RadminSessionPanel(hosting);}
    private RadminSessionPanel(boolean hosting){
        super(new GridBagLayout());this.hosting=hosting;frame=new JFrame("Spirit Gate | "+(hosting?"Hospedar partida":"Entrar no mundo"));
        setBackground(new Color(5,12,24));setBorder(BorderFactory.createEmptyBorder(20,20,20,20));
        GridBagConstraints c=new GridBagConstraints();c.gridx=0;c.weightx=1;c.fill=GridBagConstraints.HORIZONTAL;c.insets=new Insets(5,4,5,4);
        row(c,new JLabel(hosting?"HOSPEDAR PARTIDA":"ENTRAR NO MUNDO"));row(c,vpn);row(c,new JLabel("Adaptador / meu IPv4 (selecione explicitamente)"));row(c,source);
        if(!hosting){row(c,new JLabel("IP do anfitrião"));row(c,host);}
        row(c,new JLabel("Porta | capacidade: 2 jogadores"));row(c,port);row(c,new JLabel("Nome público sintético"));row(c,name);row(c,status);
        start=button(c,"Iniciar servidor",()->start());join=button(c,"Entrar na própria partida",()->connect(true));connect=button(c,"Conectar",()->connect(false));
        if(hosting){button(c,"World Studio - autorizar publicacao local",()->{try{if(server==null)throw new IllegalStateException("Inicie o servidor primeiro");if(JOptionPane.showConfirmDialog(frame,"Autorizar este World Studio a publicar somente neste servidor local? Edits nao sao aplicados ate Salvar.","Autorizar Studio",JOptionPane.YES_NO_OPTION)==JOptionPane.YES_OPTION)LauncherSession.openStudio(server,RpgMenu.home());}catch(Exception error){status.setText(error.getMessage());}});
        button(c,"Recuperar publicacao interrompida (servidor desligado)",()->{if(server!=null)return;if(JOptionPane.showConfirmDialog(frame,"Recuperar ponteiro anterior antes de hospedar? O journal sera preservado.","Recuperar",JOptionPane.YES_NO_OPTION)==JOptionPane.YES_OPTION)work(()->{new com.projetok.studio.LivePublishing(LauncherSession.revisionRoot(RpgMenu.home(),port(port.getText()))).recoverPreviousPointer();return "Ponteiro anterior recuperado";},status::setText);});}
        start.setVisible(hosting);join.setVisible(hosting);connect.setVisible(!hosting);
        button(c,"Copiar IP",()->{String ip=(String)source.getSelectedItem();if(ip!=null)Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new java.awt.datatransfer.StringSelection(ip),null);});
        button(c,"Diagnosticar / atualizar adaptadores",()->inspect());
        button(c,"Abrir Radmin VPN",()->work(()->{Path executable=radminExecutable();if(executable==null)throw new IllegalStateException("Radmin não instalado nos caminhos conhecidos. Instale pelo site oficial; crie ou entre na mesma rede pelo aplicativo.");new ProcessBuilder(executable.toString()).start();return "Radmin aberto";},v->status.setText(v)));
        button(c,"Site oficial Radmin VPN",()->work(()->{Desktop.getDesktop().browse(URI.create("https://www.radmin-vpn.com/"));return "Site oficial aberto";},status::setText));
        button(c,"Desconectar",()->{if(game!=null)game.dispose();if(client!=null)client.close();status.setText("Sessão encerrada");});
        JButton stop=button(c,"Encerrar servidor",()->{if(confirmStop())stop();});stop.setVisible(hosting);
        button(c,"Voltar",()->leave());
        frame.setContentPane(new JScrollPane(this));frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);frame.addWindowListener(new WindowAdapter(){public void windowClosing(WindowEvent e){leave();}});
        frame.setSize(640,780);frame.setMinimumSize(new Dimension(420,400));frame.setLocationRelativeTo(null);frame.setVisible(true);
        timer=new Timer(300,e->{start.setEnabled(!busy&&server==null);join.setEnabled(!busy&&server!=null&&server.running());connect.setEnabled(!busy);source.setEnabled(!busy&&server==null);port.setEditable(!busy&&server==null);if(server!=null){if(!server.running())status.setText("Servidor interrompido: "+server.failure());else if(!busy)status.setText("Servidor pronto — jogadores conectados: "+server.players()+" / 2 | "+source.getSelectedItem()+":"+server.port());}else if(client!=null&&!client.connected()&&!busy)status.setText("Conexão interrompida: "+client.problem());});timer.start();inspect();
    }
    private void row(GridBagConstraints c,JComponent component){c.gridy++;if(component instanceof JLabel)component.setForeground(new Color(190,235,245));if(component instanceof JTextField f)SpiritGateEntry.styleField(f);add(component,c);}
    private JButton button(GridBagConstraints c,String text,Runnable action){JButton b=new CharacterSelection.SpiritButton(text,false);b.addActionListener(e->action.run());row(c,b);return b;}
    private <T> void work(Callable<T> task,java.util.function.Consumer<T> success){if(busy||closed)return;busy=true;new SwingWorker<T,Void>(){protected T doInBackground()throws Exception{return task.call();}protected void done(){busy=false;try{T value=get();if(closed){if(value instanceof AutoCloseable resource)resource.close();return;}success.accept(value);}catch(Exception error){Throwable cause=error;while(cause.getCause()!=null)cause=cause.getCause();status.setText(cause instanceof BindException?"Porta ocupada — escolha outra porta":cause instanceof SocketTimeoutException||cause instanceof java.util.concurrent.TimeoutException?"Tempo limite excedido":cause instanceof ConnectException?"Servidor indisponível":String.valueOf(cause.getMessage()));}}}.execute();}
    private void inspect(){work(RadminAccess::inspect,adapters->{source.removeAllItems();StringBuilder details=new StringBuilder();for(var adapter:adapters){details.append(adapter.name()).append(adapter.active()?" ativo":" inativo").append("; ");if(adapter.active())for(var ip:adapter.addresses())source.addItem(ip.getHostAddress());}source.setSelectedIndex(-1);vpn.setText(adapters.isEmpty()?"Radmin não detectado — use o aplicativo e o site oficial":adapters.stream().noneMatch(RadminAccess.Adapter::active)?"Adaptador encontrado, mas inativo":source.getItemCount()==0?"Adaptador ativo sem IPv4 adequado":source.getItemCount()>1?"Múltiplos IPv4 — selecione a interface":"Selecione o IPv4 Radmin");vpn.setToolTipText(details+"Adaptador não garante mesma rede VPN. O handshake confirma o jogo.");});}
    private InetAddress selected()throws Exception{Object value=source.getSelectedItem();if(value==null)throw new IllegalArgumentException("Selecione um IPv4 Radmin ativo");return RadminAccess.requireLocal(ipv4(value.toString()));}
    public static InetAddress ipv4(String value)throws Exception{if(!value.matches("(?:[0-9]{1,3}\\.){3}[0-9]{1,3}"))throw new IllegalArgumentException("IPv4 inválido");for(String part:value.split("\\."))if(Integer.parseInt(part)>255)throw new IllegalArgumentException("IPv4 inválido");return InetAddress.getByName(value);}
    public static int port(String value){try{int p=Integer.parseInt(value);if(p>=1&&p<=65535)return p;}catch(NumberFormatException ignored){}throw new IllegalArgumentException("Porta deve ser 1..65535");}
    private Resources resources()throws Exception{return new Resources(CosmeticCatalog.load(RpgMenu.home().resolve("assets/dev/generated/corujao-iv-v1")),RpgPlayerSprites.loadOptional(RpgMenu.home().resolve("assets/dev/player-base"),250,.5));}
    private void start(){if(server!=null||busy)return;try{InetAddress ip=selected();int p=port(port.getText());status.setText("Iniciando servidor...");work(()->{var r=resources();var next=LauncherSession.start(ip,p,r.catalog(),RpgMenu.home());try{if(!next.running()||next.port()!=p)throw new IllegalStateException("Bind não confirmado");return next;}catch(Exception e){next.close();throw e;}},next->{server=next;status.setText("Servidor pronto — aguardando jogadores");});}catch(Exception e){status.setText(e.getMessage());}}
    private void connect(boolean own){if(busy||client!=null&&client.connected())return;try{InetAddress local=selected(),destination=own?local:ipv4(host.getText().strip());if(!RadminAccess.address(destination))throw new IllegalArgumentException("Destino exige IPv4 Radmin 26.x.x.x");int p=own?server.port():port(port.getText());String publicName=name.getText().strip();new Protocol.Hello(3,publicName);status.setText("Conectando TCP / aguardando handshake V4...");work(()->{var resources=resources();var next=LocalClient.revision(destination,p,publicName,false,true,local,com.projetok.studio.AssetCatalog.scan(RpgMenu.home(),null));SwingUtilities.invokeLater(()->{if(!closed)status.setText("TCP alcancavel - handshake V4 aprovado; aguardando snapshot");});try{if(next.revisions()==null)next.await(s->s.actors().stream().anyMatch(a->a.id()==next.id()),Duration.ofSeconds(4));return new Connection(next,resources,destination.getHostAddress()+":"+p);}catch(Exception e){next.close();throw e;}},result->{client=result.client();status.setText("Handshake V4 aprovado — sessão estabelecida");game=RpgNetworkClientLauncher.open(client,result.resources().sprites(),result.endpoint(),result.resources().catalog(),CosmeticCatalog.Selection.none(),nextFrame->{if(closed){nextFrame.dispose();return;}game=nextFrame;client=(LocalClient)nextFrame.getRootPane().getClientProperty("networkClient");});});}catch(Exception e){status.setText(e.getMessage());}}
    private record Connection(LocalClient client,Resources resources,String endpoint) implements AutoCloseable{public void close(){client.close();}}
    private boolean confirmStop(){return server==null||JOptionPane.showConfirmDialog(frame,"Encerrar servidor e desconectar participantes?","Encerrar sessão",JOptionPane.YES_NO_OPTION)==JOptionPane.YES_OPTION;}
    private void stop(){if(server!=null){server.close();server=null;}status.setText("Servidor desligado");}
    private void leave(){if(!confirmStop())return;closed=true;timer.stop();if(game!=null)game.dispose();if(client!=null)client.close();stop();frame.dispose();}
    private static Path radminExecutable(){for(String env:List.of("ProgramFiles(x86)","ProgramFiles")){String base=System.getenv(env);if(base!=null){Path p=Path.of(base,"Radmin VPN","RvRvpnGui.exe");if(Files.isRegularFile(p))return p;}}return null;}
}
