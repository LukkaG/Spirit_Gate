package com.projetok.studio;

import com.projetok.multiplayer.*;
import com.projetok.prototype.*;
import com.projetok.prototype.rpg.*;
import com.projetok.rpg.*;
import com.projetok.domain.movement.*;
import java.awt.*;
import java.awt.event.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import javax.swing.*;

/** Resident real RPG renderer. Data, assets and sprites belong only to this window. */
final class StudioLiveWindow extends JFrame implements AutoCloseable {
    private final PreviewServer server;private final LocalClient client,partner;private final AssetImages images;
    private final RpgPlayerSprites sprites;private final CosmeticCatalog cosmetics;
    private final DebugPanel panel;private final JLabel banner=new JLabel();private final javax.swing.Timer timer;
    private StudioMiniMap minimap;
    private WorldStudio editor;
    private JPanel gameControls;
    private final JCheckBox editing=new JCheckBox("Editar mapa no jogo",true);
    private JPanel embeddedHost;private Container embeddedContent;
    final NetworkDiagnostics diagnostics=new NetworkDiagnostics();
    private PreviewServer.State shown;private StudioMapRenderer renderer;private final RemoteSpriteAnimation animation=new RemoteSpriteAnimation();
    private final Set<String> held=new HashSet<>();private final Set<String> secondHeld=new HashSet<>();
    private final JCheckBox debug=new JCheckBox("F2 diagnostico",true);private CosmeticCatalog.Selection selection=CosmeticCatalog.Selection.none();
    private long frames;private boolean closed,loading;private long renderNanos,lastFrame;private double fps;
    public static StudioLiveWindow load(PreviewServer server,Path project,String title)throws Exception{
        return load(server,project,title,null);
    }
    /** Incorpora a mesma sessao autoritativa ao editor sem abrir outra janela. */
    public static StudioLiveWindow load(PreviewServer server,Path project,String title,JPanel host)throws Exception{
        var images=new AssetImages();warm(images,server.state().revision());
        var sprites=RpgPlayerSprites.loadOptional(project.resolve("assets/dev/player-base"),250,.5);
        var cosmetics=CosmeticCatalog.load(project.resolve("assets/dev/generated/corujao-iv-v1"));
        var first=new LocalClient(InetAddress.getByName("127.0.0.1"),server.port(),"Studio A");LocalClient second=null;
        try{second=new LocalClient(InetAddress.getByName("127.0.0.1"),server.port(),"Studio B");first.await(s->s.actors().size()==2,java.time.Duration.ofSeconds(5));final LocalClient partner=second;var result=new StudioLiveWindow[1];SwingUtilities.invokeAndWait(()->result[0]=new StudioLiveWindow(server,first,partner,images,sprites,cosmetics,title,host));return result[0];}catch(Exception e){first.close();if(second!=null)second.close();throw e;}
    }
    private static void warm(AssetImages images,LiveRevision revision)throws Exception{Set<String> ids=new HashSet<>();revision.bundle().maps().values().forEach(d->d.elements().forEach(e->{if(!e.asset().isEmpty())ids.add(e.asset());}));for(var asset:revision.bundle().assets().assets())if(ids.contains(asset.id()))images.load(asset);}
    private StudioLiveWindow(PreviewServer server,LocalClient client,LocalClient partner,AssetImages images,RpgPlayerSprites sprites,CosmeticCatalog cosmetics,String title,JPanel host){
        super(title);this.server=server;this.client=client;this.partner=partner;this.images=images;this.sprites=sprites;this.cosmetics=cosmetics;shown=server.state();
        server.registerRenderer(client.id());server.registerRenderer(partner.id());
        var template=DebugWorld.demo().snapshot();panel=new DebugPanel(()->snapshot(template));panel.setCustomFooter("WASD: jogador 1 | setas: jogador 2 | SPACE: golpe | SHIFT/Q: Shunpo","F: NPC | P: portal | R: respawn | revisao experimental, sem saves pessoais");panel.setActorRenderer(this::actor);
        panel.setPhysicalGeometryVisible(false);panel.setWorldActorsRenderer((g,actors,draw)->{if(renderer!=null)renderer.actors(g,actors,draw);else actors.forEach(draw);});
        panel.setNetworkDiagnostics(diagnostics);getRootPane().putClientProperty("livePanel",panel);
        panel.setWorldUnderlay(g->{if(renderer!=null)renderer.layers(g,false);});panel.setWorldOverlay(g->{if(renderer!=null)renderer.layers(g,true);if(debug.isSelected())debug(g);if(editor!=null&&editing.isSelected())editor.canvas.gameOverlay(g);});
        var controls=new JPanel(new FlowLayout(FlowLayout.LEFT));gameControls=controls;controls.add(debug);var hair=new JComboBox<String>(new String[]{"none","hair_short","hair_spiky"});var clothes=new JComboBox<String>(new String[]{"none","jacket","light_armor"});controls.add(new JLabel("Cosmeticos"));controls.add(hair);controls.add(clothes);ActionListener appearance=e->selection=new CosmeticCatalog.Selection((String)hair.getSelectedItem(),(String)clothes.getSelectedItem());hair.addActionListener(appearance);clothes.addActionListener(appearance);
        setLayout(new BorderLayout());add(banner,BorderLayout.NORTH);add(panel);add(controls,BorderLayout.SOUTH);minimap=new StudioMiniMap(()->renderer==null?shown.revision().bundle().maps().get(shown.revision().bundle().start()):renderer.document(),this::view);add(minimap,BorderLayout.EAST);setSize(1400,830);setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        for(String key:new String[]{"W","A","S","D","UP","DOWN","LEFT","RIGHT"})for(boolean down:new boolean[]{true,false}){String action=key+down;panel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke((down?"pressed ":"released ")+key),action);panel.getActionMap().put(action,new AbstractAction(){public void actionPerformed(ActionEvent e){var set=key.length()==1?held:secondHeld;if(down&&(editor==null||!editing.isSelected()))set.add(key);else set.remove(key);}});}
        Map.of("SPACE","attack","SHIFT","shunpo","Q","evade","F","interact","P","portal","R","respawn").forEach((key,action)->RpgLauncher.bind(panel,key,()->{if(editor==null||!editing.isSelected())server.command(client.id(),action);}));
        RpgLauncher.bind(panel,"F2",()->debug.setSelected(!debug.isSelected()));
        addWindowFocusListener(new WindowAdapter(){public void windowLostFocus(WindowEvent e){held.clear();secondHeld.clear();client.send(Protocol.Action.MOVE,0,0);partner.send(Protocol.Action.MOVE,0,0);}});
        addWindowListener(new WindowAdapter(){public void windowClosed(WindowEvent e){close();}});
        panel.setFocusable(true);
        panel.addMouseListener(new MouseAdapter(){@Override public void mousePressed(MouseEvent e){panel.requestFocusInWindow();}});
        panel.addFocusListener(new FocusAdapter(){@Override public void focusLost(FocusEvent e){releaseInput();}});
        // Comandos so pertencem ao jogo quando seu painel tem o foco.
        var global=panel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        var keys=global.keys();if(keys!=null)for(var key:keys){panel.getInputMap(JComponent.WHEN_FOCUSED).put(key,global.get(key));global.remove(key);}
        timer=new javax.swing.Timer(33,e->update());timer.start();
        if(host==null)setVisible(true);
        else {embeddedHost=host;embeddedContent=getContentPane();setContentPane(new JPanel());host.removeAll();host.add(embeddedContent,BorderLayout.CENTER);host.revalidate();host.repaint();}

    }
    /** Encaminha gestos ? mesma ferramenta usada pelo canvas, convertendo pela camera real. */
    void attachEditor(WorldStudio ui){
        editor=ui;ui.canvas.gameCoordinates=e->{var p=panel.aimAt(e.getX(),e.getY());if(!Double.isFinite(p.x)||!Double.isFinite(p.y))return new Point(-1,-1);return new Point((int)Math.floor(p.x),(int)Math.floor(p.y));};
        var controls=gameControls;controls.add(editing);editing.addActionListener(e->{releaseInput();panel.requestFocusInWindow();});
        var mouse=new MouseAdapter(){
            boolean gesture;
            private boolean allowed(MouseEvent e){var p=ui.canvas.worldPoint(e);return editing.isSelected()&&renderer!=null&&renderer.document().id().equals(ui.document.id())&&p.x>=0&&p.y>=0&&p.x<ui.document.width()&&p.y<ui.document.height();}
            private void pointer(MouseEvent e){ui.canvas.cursor=ui.canvas.worldPoint(e);ui.lastCursor=ui.canvas.cursor;ui.canvas.pointer.accept(ui.canvas.cursor);}
            @Override public void mousePressed(MouseEvent e){if(allowed(e)){releaseInput();pointer(e);gesture=true;ui.canvas.pressed.accept(e);}}
            @Override public void mouseDragged(MouseEvent e){if(gesture&&allowed(e)){pointer(e);ui.canvas.dragged.accept(e);}}
            @Override public void mouseReleased(MouseEvent e){if(gesture){pointer(e);ui.canvas.released.accept(e);gesture=false;}}
            @Override public void mouseMoved(MouseEvent e){if(allowed(e))pointer(e);}
        };panel.addMouseListener(mouse);panel.addMouseMotionListener(mouse);
        // A area edit?vel e o personagem compartilham a camera; o canvas auxiliar fica recolhido.
        ui.workspace.setDividerLocation(0);ui.workspace.setDividerSize(0);controls.revalidate();
    }
    /** Zoom compartilhado pela renderizacao e conversao dos gestos no jogo. */
    void zoom(double factor){panel.setPixelPerfectCamera(true);panel.setCameraZoom(Math.max(.25,Math.min(4,panel.getCameraZoom()*factor)));}
    private void releaseInput(){held.clear();secondHeld.clear();RpgLauncher.resetHeld(panel);client.send(Protocol.Action.MOVE,0,0);partner.send(Protocol.Action.MOVE,0,0);}
    private RpgWorld.View view(){return shown.players().get(client.id());}
    private DebugSnapshot snapshot(DebugSnapshot template){var v=view();if(v==null)return template;var d=shown.revision().bundle().maps().values().stream().filter(m->m.region().equals(v.state().region())).findFirst().orElseThrow();var old=RpgLauncher.adapt(v,template,StudioGeometry.space(d).obstacles().stream().map(o->new DebugSnapshot.Obstacle(o.x(),o.y(),o.width(),o.height())).toList());var actors=new ArrayList<>(old.actors());var b=shown.players().get(partner.id());if(b!=null&&b.state().region().equals(v.state().region()))actors.add(new DebugSnapshot.ActorView(900000,b.identity().name(),b.x(),b.y(),shown.revision().profile().actorRadius(),b.state().hp(),b.maxHp(),false,b.state().hp()>0));
        return new DebugSnapshot(actors,old.obstacles(),old.effects(),old.playerHp(),old.playerMaxHp(),old.defeated(),old.gameOver(),old.ticks(),old.lastEvent(),old.facingX(),old.facingY(),old.cooldown(),old.attack(),old.stamina(),old.maxStamina(),old.cryostasisCooldown(),old.iceZones(),old.statuses(),old.lightningStepActive(),old.lightningStepRemaining(),old.lightningStepCooldown(),old.lightningTrails(),old.lightningStepAwaitingLanding(),old.softObstacles(),old.cryostasisHud(),old.lightningStepHud());}
    private boolean actor(java.awt.Graphics2D g,DebugSnapshot.ActorView a){if(a.id()==1){if(sprites==null)return false;var pose=sprites.pose();return CosmeticRenderer.draw(g,sprites,cosmetics,selection,pose.direction(),pose.frame(),pose.walking(),a.x(),a.y());}if(a.id()==900000){if(sprites==null)return false;var pose=animation.pose(partner.id(),System.nanoTime()/1_000_000);return CosmeticRenderer.draw(g,sprites,cosmetics,selection,pose.direction(),pose.frame(),pose.walking(),a.x(),a.y());}return renderer!=null&&renderer.draw(g,a);}
    private void debug(java.awt.Graphics2D g){var v=view();if(v==null)return;var p=shown.revision().profile();double r=p.actorRadius();g.setColor(Color.CYAN);g.drawLine(Math.round(v.x()-4),Math.round(v.y()),Math.round(v.x()+4),Math.round(v.y()));g.drawLine(Math.round(v.x()),Math.round(v.y()-4),Math.round(v.x()),Math.round(v.y()+4));g.setColor(Color.GREEN);g.draw(new java.awt.geom.Ellipse2D.Double(v.x()-r,v.y()-r,2*r,2*r));g.setColor(Color.ORANGE);g.draw(new java.awt.geom.RoundRectangle2D.Double(v.x()-p.sceneryHalfWidth(),v.y()-r,p.sceneryHalfWidth()*2,r*2,r*2,r*2));if(sprites!=null){var pose=sprites.pose();g.setColor(Color.MAGENTA);g.draw(sprites.visualBounds(pose.direction(),pose.frame(),pose.walking(),v.x(),v.y()));}
        for(var entry:shown.players().entrySet())if(entry.getKey()!=client.id()&&entry.getValue().state().region().equals(v.state().region())){var b=entry.getValue();g.setColor(Color.YELLOW);g.draw(new java.awt.geom.Ellipse2D.Double(b.x()-r,b.y()-r,2*r,2*r));g.drawString("Jogador 2 | contato "+String.format(Locale.ROOT,"%.2f",Math.hypot(b.x()-v.x(),b.y()-v.y())),b.x(),b.y()+18);}
    }
    private void update(){if(closed)return;long start=System.nanoTime();var latest=server.state();if(latest==null||!client.connected()||!partner.connected()){banner.setText("Preview desconectado; use Reiniciar");return;}
        if(client.snapshot()!=null)animation.update(client.snapshot(),start/1_000_000);
        if(!shown.revision().hash().equals(latest.revision().hash())&&!loading){loading=true;client.send(Protocol.Action.MOVE,0,0);partner.send(Protocol.Action.MOVE,0,0);new SwingWorker<Void,Void>(){protected Void doInBackground()throws Exception{warm(images,latest.revision());return null;}protected void done(){loading=false;try{get();if(!closed){shown=latest;renderer=null;}}catch(Exception error){banner.setText("Recursos recusados: "+error.getMessage());}}}.execute();}
        if(!loading){shown=latest;var v=view();if(v!=null){if(renderer==null||renderer.bundle!=shown.revision().bundle())renderer=new StudioMapRenderer(shown.revision().bundle(),images,this::view,sprites);if(sprites!=null)sprites.update(v);}
            if(++frames%3==0){client.send(Protocol.Action.MOVE,(held.contains("D")?1:0)-(held.contains("A")?1:0),(held.contains("S")?1:0)-(held.contains("W")?1:0));partner.send(Protocol.Action.MOVE,(secondHeld.contains("RIGHT")?1:0)-(secondHeld.contains("LEFT")?1:0),(secondHeld.contains("DOWN")?1:0)-(secondHeld.contains("UP")?1:0));}}
        if(lastFrame!=0)fps=1e9/(start-lastFrame);lastFrame=start;renderNanos=System.nanoTime()-start;
        var v=view();panel.setCustomHud(java.util.List.of("WORLD STUDIO LIVE",shown.paused()?"PAUSADO":"SIMULANDO","Revisao "+shown.revision().number(),shown.revision().hash().substring(0,12),"Perfil "+shown.revision().profile().actorRadius()+" / "+shown.revision().profile().sceneryHalfWidth(),v==null?"Aguardando":String.format(Locale.ROOT,"Posicao %.2f / %.2f",v.x(),v.y()),"Tick "+shown.tick(),String.format(Locale.ROOT,"Tick %.2f ms",shown.tickNanos()/1e6),"Rejeicoes "+shown.rejected(),"Fila "+shown.queued(),String.format(Locale.ROOT,"UI %.2f ms / %.0f FPS",renderNanos/1e6,fps),"Sem gravacao de personagem"));banner.setText(loading?"Sincronizando imagens da nova revisao":"Revisao em jogo: "+shown.revision().number()+" | "+server.port()+" loopback | estado autoritativo sem predicao");panel.repaint();minimap.repaint();
    }
    @Override public void close(){if(closed)return;closed=true;timer.stop();releaseInput();if(embeddedHost!=null&&embeddedContent.getParent()==embeddedHost){embeddedHost.remove(embeddedContent);embeddedHost.add(new JLabel("Jogo encerrado. Inicie novamente para testar.",SwingConstants.CENTER));embeddedHost.revalidate();embeddedHost.repaint();}client.close();partner.close();images.clear();if(isDisplayable())dispose();}
}
