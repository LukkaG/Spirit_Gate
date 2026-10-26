package com.projetok.prototype.rpg;
import com.projetok.multiplayer.*;
import java.net.*;
import java.util.List;

/** Shared launcher factory; loopback is reserved for regression and package smoke. */
public final class LauncherSession {
    private LauncherSession() { }
    public record Managed(RevisionCoordinator target,com.projetok.studio.LivePublishing store,com.projetok.studio.LiveRevision initial){}
    private static final java.util.Map<LocalSimulationServer,Managed> managed=java.util.Collections.synchronizedMap(new java.util.WeakHashMap<>());
    public static Managed managed(LocalSimulationServer server){if(Boolean.getBoolean("spiritgate.guest"))throw new IllegalStateException("Convidado nao recebe capacidade administrativa");var result=managed.get(server);if(result==null||!server.running())throw new IllegalStateException("Selecione servidor local gerenciado ativo");return result;}
    public static com.projetok.studio.WorldStudio openStudio(LocalSimulationServer server,java.nio.file.Path root){var selected=managed(server);var studio=new com.projetok.studio.WorldStudio(root);studio.authorizePublication(selected.target(),selected.store());studio.setVisible(true);return studio;}
    public static java.nio.file.Path revisionRoot(java.nio.file.Path root,int port){String data=System.getProperty("spiritgate.rpg.dataDir");return (data==null?root:java.nio.file.Path.of(data)).resolve("studio-projects/game-revisions-"+port);}
    public static LocalSimulationServer start(InetAddress address,int port,CosmeticCatalog catalog,java.nio.file.Path root)throws Exception {
        if(Boolean.getBoolean("spiritgate.guest"))throw new IllegalStateException("Modo convidado nao hospeda/publica");
        var assets=com.projetok.studio.AssetCatalog.scan(root,null);var store=new com.projetok.studio.LivePublishing(revisionRoot(root,port));
        if(java.nio.file.Files.exists(store.root().resolve("pending.properties")))throw new IllegalStateException("Publicacao interrompida: recuperar journal antes de hospedar");
        var initial=store.current().isEmpty()?com.projetok.studio.WorldContent.baseline():store.active(0,assets);
        var world=new RpgSessionWorld(List.of("reaper","human"),catalog,true);world.enableRevisions(initial,assets);
        var server=address.isLoopbackAddress()?new LocalSimulationServer(address,port,world):LocalSimulationServer.radmin(address,port,world);
        try{server.start();managed.put(server,new Managed(world.revisions(),store,initial));return server;}catch(Exception e){server.close();throw e;}
    }
    public static LocalSimulationServer start(InetAddress address,int port,CosmeticCatalog catalog)throws Exception {
        return start(address,port,catalog,java.nio.file.Path.of("."));
    }
    /** Explicit private host entry for local/VPN scripts; editor remains in this JVM. */
    public static void main(String[] args)throws Exception{
        java.nio.file.Path root=java.nio.file.Path.of(".");int port=54557;InetAddress address=InetAddress.getLoopbackAddress();
        for(int i=0;i<args.length;i++)switch(args[i]){case "--project"->root=java.nio.file.Path.of(args[++i]);case "--port"->port=Integer.parseInt(args[++i]);case "--bind"->address=InetAddress.getByName(args[++i]);default->throw new IllegalArgumentException("Uso: --project P --port N --bind IP");}
        var project=root.toAbsolutePath();var server=start(address,port,CosmeticCatalog.load(project.resolve("assets/dev/generated/corujao-iv-v1")),project);Runtime.getRuntime().addShutdownHook(new Thread(server::close));
        javax.swing.SwingUtilities.invokeLater(()->{
            var frame=new javax.swing.JFrame("Spirit Gate | Servidor RPG V4 local gerenciado");var panel=new javax.swing.JPanel(new java.awt.BorderLayout());var state=new javax.swing.JTextArea(8,60);state.setEditable(false);state.setLineWrap(true);state.setWrapStyleWord(true);panel.add(state);
            var studio=new javax.swing.JButton("Autorizar World Studio neste servidor");panel.add(studio,java.awt.BorderLayout.SOUTH);studio.addActionListener(e->{if(javax.swing.JOptionPane.showConfirmDialog(frame,"Autorizar Studio local? Somente Salvar aplica conteudo.","Autorizar",javax.swing.JOptionPane.YES_NO_OPTION)==javax.swing.JOptionPane.YES_OPTION)openStudio(server,project);});
            var timer=new javax.swing.Timer(250,e->{if(!server.running()){studio.setEnabled(false);state.setText("Servidor interrompido: "+server.failure());return;}var status=managed(server).target().status();state.setText("Porta "+server.port()+" | "+server.players()+" / 2 jogadores\n"+status.phase()+"\n"+status.hash()+"\n"+status.clients()+"\n"+status.detail());});timer.start();frame.setContentPane(panel);frame.pack();frame.setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);frame.addWindowListener(new java.awt.event.WindowAdapter(){public void windowClosing(java.awt.event.WindowEvent e){if(javax.swing.JOptionPane.showConfirmDialog(frame,"Encerrar o servidor e seus participantes?","Encerrar",javax.swing.JOptionPane.YES_NO_OPTION)==javax.swing.JOptionPane.YES_OPTION){timer.stop();server.close();frame.dispose();}}});frame.setVisible(true);
        });
    }
}
