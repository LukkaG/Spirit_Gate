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
    final NetworkDiagnostics diagnostics=new NetworkDiagnostics();
    private PreviewServer.State shown;private StudioMapRenderer renderer;private final RemoteSpriteAnimation animation=new RemoteSpriteAnimation();
    private final Set<String> held=new HashSet<>();private final Set<String> secondHeld=new HashSet<>();
    private final JCheckBox debug=new JCheckBox("F2 diagnostico",true);private CosmeticCatalog.Selection selection=CosmeticCatalog.Selection.none();
    private long frames;private boolean closed,loading;private long renderNanos,lastFrame;private double fps;
    public static StudioLiveWindow load(PreviewServer server,Path project,String title)throws Exception{
        var images=new AssetImages();warm(images,server.state().revision());
        var sprites=RpgPlayerSprites.loadOptional(project.resolve("assets/dev/player-base"),250,.5);
        var cosmetics=CosmeticCatalog.load(project.resolve("assets/dev/generated/corujao-iv-v1"));
        var first=new LocalClient(InetAddress.getByName("127.0.0.1"),server.port(),"Studio A");LocalClient second=null;
        try{second=new LocalClient(InetAddress.getByName("127.0.0.1"),server.port(),"Studio B");first.await(s->s.actors().size()==2,java.time.Duration.ofSeconds(5));final LocalClient partner=second;var result=new StudioLiveWindow[1];SwingUtilities.invokeAndWait(()->result[0]=new StudioLiveWindow(server,first,partner,images,sprites,cosmetics,title));return result[0];}catch(Exception e){first.close();if(second!=null)second.close();throw e;}
    }
    private static void warm(AssetImages images,LiveRevision revision)throws Exception{Set<String> ids=new HashSet<>();revision.bundle().maps().values().forEach(d->d.elements().forEach(e->{if(!e.asset().isEmpty())ids.add(e.asset());}));for(var asset:revision.bundle().assets().assets())if(ids.contains(asset.id()))images.load(asset);}
    private StudioLiveWindow(PreviewServer server,LocalClient client,LocalClient partner,AssetImages images,RpgPlayerSprites sprites,CosmeticCatalog cosmetics,String title){
        super(title);this.server=server;this.client=client;this.partner=partner;this.images=images;this.sprites=sprites;this.cosmetics=cosmetics;shown=server.state();
        server.registerRenderer(client.id());server.registerRenderer(partner.id());
        var template=DebugWorld.demo().snapshot();panel=new DebugPanel(()->snapshot(template));panel.setCustomFooter("WASD: jogador 1 | setas: jogador 2 | SPACE: golpe | SHIFT/Q: Shunpo","F: NPC | P: portal | R: respawn | revisao experimental, sem saves pessoais");panel.setActorRenderer(this::actor);
        panel.setNetworkDiagnostics(diagnostics);getRootPane().putClientProperty("livePanel",panel);
        panel.setWorldUnderlay(g->{if(renderer!=null)renderer.layers(g,false);});panel.setWorldOverlay(g->{if(renderer!=null)renderer.layers(g,true);if(debug.isSelected())debug(g);});
        var controls=new JPanel(new FlowLayout(FlowLayout.LEFT));controls.add(debug);var hair=new JComboBox<String>(new String[]{"none","hair_short","hair_spiky"});var clothes=new JComboBox<String>(new String[]{"none","jacket","light_armor"});controls.add(new JLabel("Cosmeticos"));controls.add(hair);controls.add(clothes);ActionListener appearance=e->selection=new CosmeticCatalog.Selection((String)hair.getSelectedItem(),(String)clothes.getSelectedItem());hair.addActionListener(appearance);clothes.addActionListener(appearance);
        setLayout(new BorderLayout());add(banner,BorderLayout.NORTH);add(panel);add(controls,BorderLayout.SOUTH);minimap=new StudioMiniMap(()->renderer==null?shown.revision().bundle().maps().get(shown.revision().bundle().start()):renderer.document(),this::view);add(minimap,BorderLayout.EAST);setSize(1400,830);setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        for(String key:new String[]{"W","A","S","D","UP","DOWN","LEFT","RIGHT"})for(boolean down:new boolean[]{true,false}){String action=key+down;panel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke((down?"pressed ":"released ")+key),action);panel.getActionMap().put(action,new AbstractAction(){public void actionPerformed(ActionEvent e){var set=key.length()==1?held:secondHeld;if(down)set.add(key);else set.remove(key);}});}
        Map.of("SPACE","attack","SHIFT","shunpo","Q","evade","F","interact","P","portal","R","respawn").forEach((key,action)->RpgLauncher.bind(panel,key,()->server.command(client.id(),action)));
        RpgLauncher.bind(panel,"F2",()->debug.setSelected(!debug.isSelected()));
        addWindowFocusListener(new WindowAdapter(){public void windowLostFocus(WindowEvent e){held.clear();secondHeld.clear();client.send(Protocol.Action.MOVE,0,0);partner.send(Protocol.Action.MOVE,0,0);}});
        addWindowListener(new WindowAdapter(){public void windowClosed(WindowEvent e){close();}});
        timer=new javax.swing.Timer(33,e->update());timer.start();setVisible(true);
    }
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
        if(!loading){shown=latest;var v=view();if(v!=null){if(renderer==null||renderer.bundle!=shown.revision().bundle())renderer=new StudioMapRenderer(shown.revision().bundle(),images,this::view,sprites);if(sprites!=null)sprites.update(v);var document=renderer.document();panel.setDepthStructures(document.hitboxes().stream().filter(WorldDocument.Hitbox::solid).map(h->new RpgSessionGeometry.Structure(h.id(),new CollisionService.Obstacle(h.x(),h.y(),h.width(),h.height()),h.id().equals("central_gray")?48:0)).toList());}
            if(++frames%3==0){client.send(Protocol.Action.MOVE,(held.contains("D")?1:0)-(held.contains("A")?1:0),(held.contains("S")?1:0)-(held.contains("W")?1:0));partner.send(Protocol.Action.MOVE,(secondHeld.contains("RIGHT")?1:0)-(secondHeld.contains("LEFT")?1:0),(secondHeld.contains("DOWN")?1:0)-(secondHeld.contains("UP")?1:0));}}
        if(lastFrame!=0)fps=1e9/(start-lastFrame);lastFrame=start;renderNanos=System.nanoTime()-start;
        var v=view();panel.setCustomHud(java.util.List.of("WORLD STUDIO LIVE",shown.paused()?"PAUSADO":"SIMULANDO","Revisao "+shown.revision().number(),shown.revision().hash().substring(0,12),"Perfil "+shown.revision().profile().actorRadius()+" / "+shown.revision().profile().sceneryHalfWidth(),v==null?"Aguardando":String.format(Locale.ROOT,"Posicao %.2f / %.2f",v.x(),v.y()),"Tick "+shown.tick(),String.format(Locale.ROOT,"Tick %.2f ms",shown.tickNanos()/1e6),"Rejeicoes "+shown.rejected(),"Fila "+shown.queued(),String.format(Locale.ROOT,"UI %.2f ms / %.0f FPS",renderNanos/1e6,fps),"Sem gravacao de personagem"));banner.setText(loading?"Sincronizando imagens da nova revisao":"Revisao em jogo: "+shown.revision().number()+" | "+server.port()+" loopback | estado autoritativo sem predicao");panel.repaint();minimap.repaint();
    }
    @Override public void close(){if(closed)return;closed=true;timer.stop();client.close();partner.close();images.clear();if(isDisplayable())dispose();}
}
