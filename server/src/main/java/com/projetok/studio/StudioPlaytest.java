package com.projetok.studio;
import com.projetok.rpg.*;
import com.projetok.prototype.*;
import com.projetok.prototype.rpg.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.nio.file.*;
import java.util.*;

/** Cliente de playtest usa RpgRuntime/RpgWorld reais e nao possui operacao de salvar personagem. */
public final class StudioPlaytest {
    private StudioPlaytest(){}
    /** Carrega exportacao e imagens fora da EDT, depois abre uma sessao sintetica. */
    public static void launch(Path bundlePath,Path project)throws Exception {
        var bundle=StudioExport.load(bundlePath);AssetImages images=new AssetImages();for(var a:bundle.assets().assets())images.load(a);
        var player=RpgPlayerSprites.loadOptional(project.resolve("assets/dev/player-base"),250,.5);
        SwingUtilities.invokeLater(()->open(bundle,images,player,bundlePath));
    }
    static void open(StudioExport.Bundle bundle,AssetImages images,RpgPlayerSprites player,Path output) {
        RpgRuntime runtime=new RpgRuntime(new RpgWorld(CharacterIdentity.create("Playtest","reaper","azure"),bundle.scenes()));
        var template=DebugWorld.demo(new java.util.Random(91)).snapshot();
        DebugPanel panel=new DebugPanel(()->{var v=runtime.snapshot();var space=bundle.scenes().maps().get(v.state().region()).space();return RpgLauncher.adapt(v,template,space.obstacles().stream().map(o->new DebugSnapshot.Obstacle(o.x(),o.y(),o.width(),o.height())).toList());});panel.setPhysicalGeometryVisible(false);
        var render=new StudioMapRenderer(bundle,images,runtime::snapshot,player);var feedback=new RpgCombatFeedback();var boss=new RpgBossOverlay();panel.setActorRenderer(render);panel.setWorldUnderlay(g->render.layers(g,false));panel.setWorldOverlay(g->{render.layers(g,true);boss.draw(g);feedback.draw(g);});
        var minimap=new StudioMiniMap(render::document,runtime::snapshot);
        JFrame frame=new JFrame("Spirit Gate | PLAYTEST ISOLADO | "+bundle.start());frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);JPanel root=new JPanel(new BorderLayout());JLabel banner=new JLabel("Sessao sintetica sem saves pessoais | WASD mover | SPACE golpe | Q habilidade | F interagir | R recuperar");root.add(banner,BorderLayout.NORTH);root.add(panel);RpgHud hud=new RpgHud(runtime);root.add(hud,BorderLayout.SOUTH);frame.setContentPane(root);frame.setSize(1260,900);frame.setLocationRelativeTo(null);
        var logs=new ArrayList<String>();logs.add("Inicio: "+bundle.start()+" | runtime real, sem LocalSaveStore");boolean[] held=new boolean[4];String[] keys={"W","S","A","D"};
        var events=new StudioEvents();var interacted=new java.util.concurrent.atomic.AtomicBoolean();var environment=new java.util.concurrent.atomic.AtomicReference<>(EnvironmentPreview.Mode.OFF);
        panel.setWorldOverlay(g->{render.layers(g,true);EnvironmentPreview.draw(g,new Rectangle(0,0,960,640),environment.get(),System.nanoTime());boss.draw(g);feedback.draw(g);});
        JPanel controls=new JPanel(new java.awt.BorderLayout());controls.add(banner);JPanel actions=new JPanel(new FlowLayout(FlowLayout.RIGHT));JToggleButton geometry=new JToggleButton("Colisoes");geometry.addActionListener(e->panel.setPhysicalGeometryVisible(geometry.isSelected()));actions.add(geometry);JButton secondary=new JButton("Shunpo defensivo (nivel 20)");secondary.addActionListener(e->runtime.command(w->w.activate("rpg.secondary")));actions.add(secondary);controls.add(actions,java.awt.BorderLayout.EAST);root.add(controls,java.awt.BorderLayout.NORTH);
        root.add(minimap,BorderLayout.EAST);
        JToggleButton debug=new JToggleButton("Diagnóstico");actions.add(debug);
        for(int i=0;i<4;i++)for(boolean down:new boolean[]{true,false}){final int at=i;String key=keys[i]+down;panel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke((down?"pressed ":"released ")+keys[i]),key);panel.getActionMap().put(key,new AbstractAction(){public void actionPerformed(ActionEvent e){held[at]=down;runtime.input((held[3]?1:0)-(held[2]?1:0),(held[1]?1:0)-(held[0]?1:0));}});}
        RpgLauncher.bind(panel,"SPACE",()->runtime.command(RpgWorld::attack));RpgLauncher.bind(panel,"Q",()->runtime.command(RpgWorld::castAbility));RpgLauncher.bind(panel,"R",()->runtime.command(RpgWorld::returnHome));RpgLauncher.bind(panel,"1",()->runtime.command(w->w.use("potion")));
        RpgLauncher.bind(panel,"F",()->runtime.action(RpgWorld::interact).thenAccept(ok->{if(ok)interacted.set(true);else synchronized(logs){if(logs.size()<1000)logs.add("Interacao recusada pela simulacao");}}));
        long[] lastBlocked={-30};RpgWorld.View[] previous={runtime.snapshot()};
        javax.swing.Timer timer=new javax.swing.Timer(33,e->{var v=runtime.snapshot();if(runtime.failure()!=null){((javax.swing.Timer)e.getSource()).stop();logs.add("Falha: "+runtime.failure());JOptionPane.showMessageDialog(frame,"Simulacao interrompida: "+runtime.failure().getMessage());frame.dispose();return;}if(player!=null)player.update(v);feedback.update(v);boss.update(v);hud.update(v);panel.setCustomHud(debug.isSelected()?RpgDevelopmentDiagnostics.lines(v,render.document().name()):java.util.List.of("PLAYTEST WORLD STUDIO",render.document().name(),"Regiao: "+v.state().region(),"Sem gravacao de personagem","Assets residentes: "+images.bytes()/1024+" KiB","[F] NPC/objeto/portal","[R] Recuperar apos KO"));
            var document=render.document();for(var event:events.update(document,StudioEvents.state(document,v,interacted.getAndSet(false))))switch(event.action()){
                case "MESSAGE"->banner.setText(event.message());case "ENVIRONMENT"->environment.set(EnvironmentPreview.Mode.valueOf(event.target()));
                case "PORTAL"->runtime.action(w->w.travel(StudioExport.key("portal_",document.id()+"/"+event.target()))).thenAccept(ok->{synchronized(logs){if(logs.size()<1000)logs.add("Evento portal "+event.id()+": "+ok);}});
                case "INTERACTION"->runtime.action(RpgWorld::interact).thenAccept(ok->{synchronized(logs){if(logs.size()<1000)logs.add("Evento solicitou interacao proxima "+event.id()+": "+ok);}});default->{}
            }
            if((held[0]||held[1]||held[2]||held[3])&&v.ticks()!=previous[0].ticks()&&v.x()==previous[0].x()&&v.y()==previous[0].y()&&v.ticks()-lastBlocked[0]>=30){lastBlocked[0]=v.ticks();if(logs.size()<1000)logs.add("Movimento bloqueado em "+v.x()+","+v.y());}previous[0]=v;minimap.repaint();panel.repaint();});
        frame.addWindowFocusListener(new WindowAdapter(){public void windowLostFocus(WindowEvent e){Arrays.fill(held,false);runtime.input(0,0);RpgLauncher.resetHeld(panel);}});
        frame.addWindowListener(new WindowAdapter(){public void windowClosed(WindowEvent e){timer.stop();runtime.close();new Thread(()->{try{synchronized(logs){Files.write(output.resolve("playtest-diagnostics.log"),logs);}}catch(Exception error){error.printStackTrace();}},"studio-diagnostics").start();}});
        runtime.start();timer.start();frame.setVisible(true);
    }
    /** Execucao externa verificavel de um bundle exportado, sem fisica alternativa. */
    public static void main(String[] args)throws Exception{if(args.length<1||args.length>2)throw new IllegalArgumentException("Uso: StudioPlaytest pasta-exportada [raiz-projeto]");launch(Path.of(args[0]),args.length==2?Path.of(args[1]):Path.of("").toAbsolutePath());}
}
