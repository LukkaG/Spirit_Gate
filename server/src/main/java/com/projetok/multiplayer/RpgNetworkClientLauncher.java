package com.projetok.multiplayer;

import com.projetok.prototype.*;
import com.projetok.prototype.rpg.RpgPlayerSprites;
import com.projetok.prototype.rpg.CosmeticCatalog;
import com.projetok.prototype.rpg.CosmeticRenderer;
import java.awt.*;
import java.awt.event.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import javax.swing.*;

/** Cliente RPG experimental: envia intenções e apresenta snapshots, sem simulação autoritativa local. */
public final class RpgNetworkClientLauncher {
    private RpgNetworkClientLauncher() { }
    /** Impede trocar a janela enquanto a confirmacao de encerramento processa a EDT. */
    public static final String CLOSE_CONFIRMATION_PROPERTY="networkCloseConfirmation";
    private static boolean acceptsReconnect(JFrame frame){return frame.isDisplayable()&&!Boolean.TRUE.equals(frame.getRootPane().getClientProperty(CLOSE_CONFIRMATION_PROPERTY));}
    /** Rasters preparados no worker, compartilhados entre previa, jogo e reconexao. */
    public record Presentation(NetworkPlayerVisuals visuals,CosmeticCatalog catalog) { }
    record Options(boolean lan,boolean cosmetics,boolean combat,boolean radmin,String[] connection,CosmeticCatalog.Selection selection) { }
    static Options options(String[] args){boolean lan=false,cosmetics=false,combat=false,radmin=false;String hair="none",clothing="none";var positional=new ArrayList<String>();for(int i=0;i<args.length;i++)switch(args[i]){case "--revisions"->{}case "--lan"->lan=true;case "--radmin"->radmin=true;case "--cosmetics"->cosmetics=true;case "--combat"->combat=true;case "--hair"->{if(++i>=args.length)throw new IllegalArgumentException("Cabelo ausente");hair=args[i];}case "--clothing"->{if(++i>=args.length)throw new IllegalArgumentException("Roupa ausente");clothing=args[i];}default->positional.add(args[i]);}if(positional.size()<3||positional.size()>4)throw new IllegalArgumentException("Uso: host porta nome [captura.png] [--lan] [--cosmetics --hair ID --clothing ID]");var selection=new CosmeticCatalog.Selection(hair,clothing);if(!cosmetics&&!selection.equals(CosmeticCatalog.Selection.none()))throw new IllegalArgumentException("Aparência exige capacidade cosmética");if(lan&&radmin)throw new IllegalArgumentException("Escolha --lan ou --radmin");return new Options(lan,cosmetics,combat,radmin,positional.toArray(String[]::new),selection);}
    public static void main(String[] args){com.projetok.diagnostics.ErrorReports.install("cliente-multiplayer");var options=options(args);boolean allowLan=options.lan(),cosmetics=options.cosmetics();String[] connection=options.connection();String app=System.getProperty("jpackage.app-path");Path root=app==null?Path.of("."):Path.of(app).toAbsolutePath().getParent();
        SwingUtilities.invokeLater(()->{var loading=new JFrame("Spirit Gate | Conectando");loading.add(new JLabel("Carregando recursos temporarios e conectando...",SwingConstants.CENTER));loading.setSize(480,120);loading.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);loading.setLocationRelativeTo(null);loading.setVisible(true);new SwingWorker<Object[],Void>(){protected Object[] doInBackground()throws Exception{var sprites=RpgPlayerSprites.loadOptional(root.resolve("assets/dev/player-base"),250,.5);var catalog=cosmetics?CosmeticCatalog.load(root.resolve("assets/dev/generated/corujao-iv-v1")):null;if(catalog!=null)catalog.validate(options.selection());var client=java.util.Arrays.asList(args).contains("--revisions")?LocalClient.revision(InetAddress.getByName(connection[0]),Integer.parseInt(connection[1]),connection[2],allowLan,options.radmin(),null,com.projetok.studio.AssetCatalog.scan(root,null)):options.radmin()?LocalClient.radmin(InetAddress.getByName(connection[0]),Integer.parseInt(connection[1]),connection[2],options.combat(),cosmetics):options.combat()?LocalClient.combat(InetAddress.getByName(connection[0]),Integer.parseInt(connection[1]),connection[2],allowLan):cosmetics?LocalClient.cosmetics(InetAddress.getByName(connection[0]),Integer.parseInt(connection[1]),connection[2],allowLan):allowLan?LocalClient.lan(InetAddress.getByName(connection[0]),Integer.parseInt(connection[1]),connection[2]):new LocalClient(InetAddress.getByName(connection[0]),Integer.parseInt(connection[1]),connection[2]);return new Object[]{client,sprites,catalog};}protected void done(){try{var values=get();var client=(LocalClient)values[0];var frame=open(client,(RpgPlayerSprites)values[1],connection[0]+":"+connection[1],(CosmeticCatalog)values[2],options.selection());if(connection.length==4){var shot=new javax.swing.Timer(3000,e->{try{var image=new java.awt.image.BufferedImage(frame.getWidth(),frame.getHeight(),2);var g=image.createGraphics();frame.printAll(g);g.dispose();javax.imageio.ImageIO.write(image,"png",Path.of(connection[3]).toFile());var self=client.appearances().get(client.id());java.nio.file.Files.writeString(Path.of(connection[3]+".json"),"{\"session\":"+client.id()+",\"players\":"+(client.snapshot()==null?0:client.snapshot().actors().size())+",\"appearances\":"+client.appearances().size()+",\"hair\":\""+(self==null?"none":self.hair())+"\",\"race\":\""+(self==null?"unknown":self.race())+"\",\"physicalUiApproved\":false}");}catch(Exception error){error.printStackTrace();}finally{frame.dispose();}});shot.setRepeats(false);shot.start();}}catch(Exception e){JOptionPane.showMessageDialog(null,"Conexão recusada: "+e.getMessage());}finally{loading.dispose();}}}.execute();});
    }
    /** Janela reutilizável pelo probe de dois clientes; fechamento libera sockets e timers. */
    public static JFrame open(LocalClient client,RpgPlayerSprites sprites,String endpoint){
        return open(client,sprites,endpoint,null);
    }
    public static JFrame open(LocalClient client,RpgPlayerSprites sprites,String endpoint,CosmeticCatalog catalog){
        return open(client,sprites,endpoint,catalog,CosmeticCatalog.Selection.none());
    }
    public static JFrame open(LocalClient client,RpgPlayerSprites sprites,String endpoint,CosmeticCatalog catalog,CosmeticCatalog.Selection initial){
        return open(client,sprites,endpoint,catalog,initial,frame->{});
    }
    public static JFrame open(LocalClient client,RpgPlayerSprites sprites,String endpoint,CosmeticCatalog catalog,CosmeticCatalog.Selection initial,java.util.function.Consumer<JFrame> onWindow){
        return open(client,sprites,endpoint,catalog,initial,onWindow,null);
    }
    /** Usa os recursos ja verificados pelo fluxo de entrada, sem recarregar imagens na EDT. */
    public static JFrame open(LocalClient client,RpgPlayerSprites sprites,String endpoint,CosmeticCatalog catalog,CosmeticCatalog.Selection initial,java.util.function.Consumer<JFrame> onWindow,Presentation presentation){
        return mount(client,sprites,endpoint,catalog,initial,onWindow,presentation,null);
    }
    /** Troca somente a sessão da ferramenta de treino, conservando a janela e seus limites. */
    public static JFrame replacePreview(JFrame frame,LocalClient client,RpgPlayerSprites sprites,String endpoint,CosmeticCatalog catalog,CosmeticCatalog.Selection initial,Presentation presentation){
        if(!SwingUtilities.isEventDispatchThread()||!frame.isDisplayable())throw new IllegalStateException("Troca de prévia exige janela aberta na EDT");
        if(!(frame.getRootPane().getClientProperty("networkDetach") instanceof Runnable detach))throw new IllegalArgumentException("Janela sem sessão substituível");
        detach.run();
        return mount(client,sprites,endpoint,catalog,initial,f->{},presentation,frame);
    }
    /** Monta a apresentação e mantém um descarte idempotente dos recursos pertencentes à sessão. */
    private static JFrame mount(LocalClient client,RpgPlayerSprites sprites,String endpoint,CosmeticCatalog catalog,CosmeticCatalog.Selection initial,java.util.function.Consumer<JFrame> onWindow,Presentation presentation,JFrame reused){
        var attached=new java.util.concurrent.atomic.AtomicBoolean(true);
        com.projetok.studio.WorldContent.Prepared[] scene={null};com.projetok.studio.StudioMapRenderer[] mapRenderer={null};var timeline=new RemoteTimeline[]{new RemoteTimeline(client.diagnostics())};long[] acceptedTick={-1};
        var template=DebugWorld.demo().snapshot();Protocol.State[] display={new Protocol.State(0,java.util.List.of())};var animation=new RemoteSpriteAnimation();var combatPresentation=new NetworkCombatPresentation();var mobSprites=new com.projetok.prototype.rpg.MobSprites();
        var skillPresentation=client.skillApplication()?new NetworkSkillPresentation():null;
        NetworkSkillControls[] skillControls={null};
        var visuals=presentation==null?new NetworkPlayerVisuals(RpgPlayerSprites.reaperFolder(),sprites):presentation.visuals();
        var displayCatalog=presentation==null?(catalog==null?null:catalog.onLogicalCanvas(64,64,32,60,NetworkPlayerVisuals.LEGACY_SCALE)):presentation.catalog();
        var panel=new DebugPanel(()->NetworkClientLauncher.adapt(display[0],client.id(),template,"RPG local - movimento validado no servidor",1000,scene[0],client.region()));
        panel.setNetworkDiagnostics(client.diagnostics());
        panel.setPixelPerfectCamera(true);panel.setCameraZoom(1.25);panel.setOverlayHud(client.supportsGameplay());
        java.util.List<com.projetok.domain.movement.RpgSessionGeometry.Structure>[] structures=new java.util.List[]{com.projetok.domain.movement.RpgSessionGeometry.structures()};
        com.projetok.studio.WorldDocument[] geometryDocument={null};
        panel.setDepthStructures(structures[0]);
        if(client.revisions()!=null){
            panel.setWorldUnderlay(g->{if(mapRenderer[0]!=null)mapRenderer[0].layers(g,false);});
            client.revisions().renderer(prepared->{var done=new java.util.concurrent.CompletableFuture<Void>();SwingUtilities.invokeLater(()->{try{if(!client.connected())throw new IllegalStateException("Cliente encerrado");scene[0]=prepared;mapRenderer[0]=new com.projetok.studio.StudioMapRenderer(prepared.revision().bundle(),prepared.images(),client::region);display[0]=new Protocol.State(0,java.util.List.of());timeline[0]=new RemoteTimeline(client.diagnostics());acceptedTick[0]=-1;panel.repaint();done.complete(null);}catch(Exception error){done.completeExceptionally(error);}});return done;});
        }
        boolean[] debug={client.diagnostics().enabled};
        panel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("pressed F2"),"network-debug");panel.getActionMap().put("network-debug",new AbstractAction(){public void actionPerformed(ActionEvent e){debug[0]=!debug[0];}});
        panel.setWorldOverlay(g->{if(mapRenderer[0]!=null){mapRenderer[0].layers(g,true);mapRenderer[0].portalHints(g);}combatPresentation.draw(g);mobSprites.effects(g);
            if(skillPresentation!=null)skillPresentation.draw(g);
            if(skillControls[0]!=null)skillControls[0].draw(g);
            if(debug[0])NetworkDebugOverlay.draw(g,client.snapshot(),display[0],sprites,animation,scene[0]==null?com.projetok.domain.movement.SessionCollisionProfile.DEFAULT:scene[0].revision().profile(),structures[0],visuals,client.appearances());});
        panel.setCustomFooter("WASD/setas: mover | Zoom 0,75x / 1x / 1,25x / 2x / 3x | F2: colisao", client.supportsCombat()?"SPACE: ataque | SHIFT/Q: Shunpo (exige nivel 20)":"Combate exige --combat em servidor e cliente.");
        panel.setActorRenderer((g,actor)->{if(skillPresentation!=null&&skillPresentation.hidden(actor.id()))return true;if(!actor.alive())return false;if(actor.id()>1_000_000)return mobSprites.draw(g,actor)||(mapRenderer[0]!=null&&mapRenderer[0].drawNetwork(g,actor));var appearance=client.appearances().get(actor.id());String race=appearance==null?"":appearance.race();var body=visuals.sprites(race);if(body==null)return false;var pose=animation.pose(actor.id(),System.nanoTime()/1_000_000,visuals.frameMillis(race),body.walkFrames());var selection=appearance==null?CosmeticCatalog.Selection.none():new CosmeticCatalog.Selection(appearance.hair(),appearance.clothing());boolean drawn=CosmeticRenderer.draw(g,body,visuals.supportsAccessories(race)?displayCatalog:null,selection,pose.direction(),pose.frame(),pose.walking(),actor.x(),actor.y());if(drawn){g.setColor(Color.WHITE);g.drawString(actor.name(),actor.x()-20,(int)body.visualBounds(pose.direction(),pose.frame(),pose.walking(),actor.x(),actor.y()).getMinY()-8);}return drawn;});
        if(skillPresentation!=null)skillPresentation.echoRenderer((graphics,player,x,y)->{
            var appearance=client.appearances().get(player);String race=appearance==null?"":appearance.race();var body=visuals.sprites(race);if(body==null)return;
            var pose=animation.pose(player,System.nanoTime()/1_000_000,visuals.frameMillis(race),body.walkFrames());
            var selection=appearance==null?CosmeticCatalog.Selection.none():new CosmeticCatalog.Selection(appearance.hair(),appearance.clothing());
            CosmeticRenderer.draw(graphics,body,visuals.supportsAccessories(race)?displayCatalog:null,selection,pose.direction(),pose.frame(),pose.walking(),x,y);
        });
        NetworkGameHud[] gameHud={null};GamePanels[] gamePanels={null};GameInputRouter[] inputRouter={null};NetworkSettingsPanel[] settingsView={null};
        JFrame frame=reused==null?new JFrame("Spirit Gate | RPG LOCAL EXPERIMENTAL | "+client.id()):reused;
        var previousWindowListeners=java.util.Set.of(frame.getWindowListeners());
        var previousFocusListeners=java.util.Set.of(frame.getWindowFocusListeners());
        if(reused!=null){frame.setContentPane(new JPanel(new BorderLayout()));frame.getRootPane().putClientProperty("networkSkillAssetsError",null);frame.getRootPane().putClientProperty("networkReconnectError",null);}
        frame.add(panel);if(reused==null)frame.setSize(1200,800);frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);frame.getRootPane().putClientProperty("networkClient",client);frame.getRootPane().putClientProperty("networkPanel",panel);frame.getRootPane().putClientProperty("networkVisuals",visuals);
        JButton reconnect=new JButton("Reconectar nova sessão");reconnect.setEnabled(false);frame.getRootPane().putClientProperty("networkReconnecting",Boolean.FALSE);
        JPanel controls=new JPanel(new FlowLayout(FlowLayout.LEFT,8,4));controls.add(reconnect);frame.getRootPane().putClientProperty("networkControls",controls);
        double[] zoomValues={.75,1,1.25,2,3};
        var zoom=new JComboBox<String>(new String[]{"0,75x","1x","1,25x","2x","3x"});zoom.setName("cameraZoom");zoom.setSelectedIndex(2);
        zoom.addActionListener(e->{panel.setCameraZoom(zoomValues[zoom.getSelectedIndex()]);panel.requestFocusInWindow();});
        controls.add(new JLabel("Zoom"));controls.add(zoom);frame.add(controls,BorderLayout.NORTH);
        var combatHud=new NetworkCombatHud();frame.add(combatHud,BorderLayout.SOUTH);
        var progressLabel=new NetworkProgressLabel();var hotbar=client.supportsGameplay()?new NetworkHotbar(client):null;var gameplayNotice=new JLabel();var questPanel=client.supportsGameplay()?new NetworkQuestPanel(client):null;
        if(hotbar!=null&&hotbar.domainSkills()!=null){
            skillControls[0]=hotbar.domainSkills();skillControls[0].presentation(skillPresentation);
            frame.getRootPane().putClientProperty("networkSkillControls",skillControls[0]);frame.getRootPane().putClientProperty("networkSkillPresentation",skillPresentation);
            for(int slot=1;slot<=5;slot++){final int selectedSlot=slot;String action="domain-skill-"+slot;
                panel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("pressed F"+(slot+4)),action);
                panel.getActionMap().put(action,new AbstractAction(){public void actionPerformed(ActionEvent event){hotbar.activateSkill(selectedSlot);panel.requestFocusInWindow();}});
            }
            var skillMouse=new MouseAdapter(){
                @Override public void mouseMoved(MouseEvent event){var aim=panel.aimAt(event.getX(),event.getY());skillControls[0].pointer((float)aim.x,(float)aim.y);}
                @Override public void mouseDragged(MouseEvent event){mouseMoved(event);}
                @Override public void mousePressed(MouseEvent event){
                    if(SwingUtilities.isRightMouseButton(event)){if(skillControls[0].cancel())event.consume();return;}
                    if(!SwingUtilities.isLeftMouseButton(event)||gamePanels[0]!=null&&gamePanels[0].modal())return;
                    var aim=panel.aimAt(event.getX(),event.getY());if(skillControls[0].click((float)aim.x,(float)aim.y)){event.consume();return;}
                    var snapshot=client.snapshot();if(snapshot==null||!Double.isFinite(aim.x)||!Double.isFinite(aim.y))return;
                    var actor=snapshot.actors().stream().filter(a->a.id()<2_000_000&&(a.hp()>0||a.id()<=1_000_000)&&Math.abs(aim.x-a.x())<=24&&aim.y>=a.y()-64&&aim.y<=a.y()+14)
                        .min(java.util.Comparator.comparingDouble(a->Math.hypot(aim.x-a.x(),aim.y-(a.y()-24)))).orElse(null);
                    client.selectTarget(actor==null?0:actor.id());panel.requestFocusInWindow();
                }
            };panel.addMouseListener(skillMouse);panel.addMouseMotionListener(skillMouse);
        }
        var inventoryPanel=client.supportsGameplay()?new NetworkInventoryPanel(client):null;
        var characterPanel=client.supportsGameplay()?new NetworkCharacterPanel(client,progressLabel):null;
        var inventoryButton=new JButton("Inventario (I)");if(inventoryPanel!=null)controls.add(inventoryButton);
        boolean compatibleCosmetics=client.characterSelection()==null||!"reaper".equals(client.characterSelection().race());
        if(catalog!=null&&client.supportsCosmetics()&&compatibleCosmetics){catalog.validate(initial);var hair=new JComboBox<String>();var clothing=new JComboBox<String>();hair.addItem("none");clothing.addItem("none");catalog.all("hair").forEach(d->hair.addItem(d.id()));catalog.all("clothing").forEach(d->clothing.addItem(d.id()));hair.setSelectedItem(initial.hair());clothing.setSelectedItem(initial.clothing());client.appearance(initial.hair(),initial.clothing());controls.add(new JLabel("TEMPORÁRIO — cabelo/roupa"));controls.add(hair);controls.add(clothing);java.awt.event.ActionListener update=e->client.appearance((String)hair.getSelectedItem(),(String)clothing.getSelectedItem());hair.addActionListener(update);clothing.addActionListener(update);frame.add(controls,BorderLayout.NORTH);}
        if(client.characterSelection()!=null){String race=client.characterSelection().race();var info=visuals.renderInfo(race);var label=new JLabel(client.characterSelection().name()+" | "+com.projetok.rpg.CharacterRaces.get(race).name()+(info.provisional()?" (aparencia provisoria)":""));label.setToolTipText(info.detail());controls.add(label);}
        if(hotbar!=null){var header=new JPanel(new BorderLayout());header.add(controls,BorderLayout.NORTH);var rows=new JPanel();rows.setLayout(new BoxLayout(rows,BoxLayout.Y_AXIS));rows.add(hotbar);rows.add(questPanel);rows.add(gameplayNotice);header.add(rows);frame.add(header,BorderLayout.NORTH);panel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("pressed 1"),"hotbar-use");panel.getActionMap().put("hotbar-use",new AbstractAction(){public void actionPerformed(ActionEvent e){hotbar.activate();}});}
        reconnect.addActionListener(e->{
            if(client.connected()||!acceptsReconnect(frame)||Boolean.TRUE.equals(frame.getRootPane().getClientProperty("networkReconnecting")))return;
            frame.getRootPane().putClientProperty("networkReconnectError",null);frame.getRootPane().putClientProperty("networkReconnecting",Boolean.TRUE);reconnect.setEnabled(false);
            new SwingWorker<LocalClient,Void>(){
                protected LocalClient doInBackground()throws Exception{return client.reconnect();}
                protected void done(){
                    LocalClient next=null;
                    try{
                        next=get();
                        if(!attached.get()||!acceptsReconnect(frame)){next.close();return;}
                        open(next,sprites,endpoint,catalog,initial,onWindow,presentation);frame.dispose();
                    }catch(Exception error){
                        if(next!=null)next.close();
                        if(acceptsReconnect(frame)){if(gameHud[0]!=null)frame.getRootPane().putClientProperty("networkReconnectError","Reconexão recusada: "+error.getMessage());else JOptionPane.showMessageDialog(frame,"Reconexão recusada: "+error.getMessage());}
                    }finally{frame.getRootPane().putClientProperty("networkReconnecting",Boolean.FALSE);}
                }
            }.execute();
        });
        for(var binding:Map.of("SPACE",Protocol.Action.ATTACK,"SHIFT",Protocol.Action.LIGHTNING_STEP,"Q",Protocol.Action.CRYOSTASIS).entrySet()){panel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("pressed "+binding.getKey()),binding.getKey());panel.getActionMap().put(binding.getKey(),new AbstractAction(){public void actionPerformed(ActionEvent e){if(client.supportsCombat())client.send(binding.getValue(),0,0);}});}
        for(var binding:Map.of("R",Protocol.WorldAction.RESPAWN,"F",Protocol.WorldAction.INTERACT,"E",Protocol.WorldAction.ACCEPT_QUEST,"C",Protocol.WorldAction.COMPLETE_QUEST,"P",Protocol.WorldAction.TRAVEL).entrySet()){panel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("pressed "+binding.getKey()),binding.getKey());panel.getActionMap().put(binding.getKey(),new AbstractAction(){public void actionPerformed(ActionEvent e){String target="coop_slime";if(binding.getValue()==Protocol.WorldAction.TRAVEL){target=client.region().equals("forest")?"to_village":"to_forest";if(scene[0]!=null&&client.snapshot()!=null){var self=client.snapshot().actors().stream().filter(a->a.id()==client.id()).findFirst().orElse(null);if(self==null)return;var document=scene[0].revision().bundle().maps().values().stream().filter(d->d.region().equals(client.region())).findFirst().orElseThrow();var portal=document.portals().stream().filter(p->Math.hypot(p.x()-self.x(),p.y()-self.y())<=65).findFirst().orElse(null);if(portal==null)return;target=com.projetok.studio.StudioExport.key("portal_",document.id()+"/"+portal.id());}}client.request(binding.getValue(),target);}});}
        Set<String> held=new HashSet<>();
        int[] count={0};float[] lastMove={Float.NaN,Float.NaN};long[] lastMoveSent={0};
        Runnable sendMovement=()->{
            boolean aim=skillControls[0]!=null&&skillControls[0].aiming();
            float mx=aim?0:(held.contains("D")||held.contains("RIGHT")?1:0)-(held.contains("A")||held.contains("LEFT")?1:0),my=aim?0:(held.contains("S")||held.contains("DOWN")?1:0)-(held.contains("W")||held.contains("UP")?1:0);
            long now=System.nanoTime();
            if(mx!=lastMove[0]||my!=lastMove[1]||now-lastMoveSent[0]>=100_000_000L){
                if(client.send(Protocol.Action.MOVE,mx,my)){lastMove[0]=mx;lastMove[1]=my;lastMoveSent[0]=now;}
            }
        };
        for(String key:new String[]{"W","A","S","D","UP","DOWN","LEFT","RIGHT"})for(boolean down:new boolean[]{true,false}){String action=key+down;panel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke((down?"pressed ":"released ")+key),action);panel.getActionMap().put(action,new AbstractAction(){public void actionPerformed(ActionEvent e){if(down)held.add(key);else held.remove(key);sendMovement.run();}});}
        if(client.supportsGameplay()) {
            Runnable releaseInput=()->{held.clear();sendMovement.run();};
            var details=hotbar.overlayStyle(panel::requestFocusInWindow);
            var menuContents=new JPanel(new BorderLayout(8,8));menuContents.add(new JLabel("Controles e aparencia da partida"),BorderLayout.NORTH);menuContents.add(new JScrollPane(controls));menuContents.add(gameplayNotice,BorderLayout.SOUTH);
            GamePanels.Panel[] windows=new GamePanels.Panel[6];
            Runnable inventory=()->windows[0].toggle(),openProfile=()->windows[1].toggle(),openJournal=()->windows[2].toggle();
            Runnable openMenu=()->{if(skillControls[0]!=null&&skillControls[0].cancel())return;if(gamePanels[0].anyOpen())gamePanels[0].escape();else windows[3].showPanel();};
            Runnable settings=()->windows[4].toggle();
            gameHud[0]=new NetworkGameHud(panel,combatHud,hotbar,openProfile,inventory,openJournal,openMenu,settings);
            gameHud[0].skills(()->windows[5].toggle());inventoryPanel.equipment(openProfile);
            frame.setContentPane(gameHud[0]);frame.setMinimumSize(new Dimension(640,480));frame.getRootPane().putClientProperty("networkGameHud",gameHud[0]);
            gameHud[0].targeting(()->client.selectTarget(0));
            gamePanels[0]=new GamePanels(gameHud[0],panel);frame.getRootPane().putClientProperty("networkPanels",gamePanels[0]);
            if(skillControls[0]!=null){
                skillControls[0].onAim(()->{if(gamePanels[0].anyOpen())gamePanels[0].escape();if(inputRouter[0]!=null)inputRouter[0].reset();releaseInput.run();panel.requestFocusInWindow();});
                skillControls[0].onAimFinish(()->{if(inputRouter[0]!=null)inputRouter[0].reset();releaseInput.run();});
            }
            windows[0]=gamePanels[0].register("inventory","Inventário",inventoryPanel,560,420,false);windows[0].onClose(inventoryPanel::hideItem);
            windows[0].dismissOverlay(inventoryPanel::dismissItem);
            windows[1]=gamePanels[0].register("profile","Personagem",characterPanel,600,620,false);
            windows[1].onOpen(()->{client.requestAttributes();client.requestEnergy();});
            windows[2]=gamePanels[0].register("journal","Missões",questPanel,470,560,false);
            windows[3]=gamePanels[0].register("menu","Menu da partida",menuContents,760,260,false);
            windows[5]=gamePanels[0].register("skills","Habilidades",details,590,540,false);
            inventoryButton.addActionListener(e->inventory.run());
            panel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("pressed I"),"inventory");panel.getActionMap().put("inventory",new AbstractAction(){public void actionPerformed(ActionEvent e){inventory.run();}});
            for(var binding:Map.of("J",openJournal,"ESCAPE",openMenu,"M",(Runnable)gameHud[0]::toggleMinimap).entrySet()){
                String id="hud-"+binding.getKey();panel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("pressed "+binding.getKey()),id);
                panel.getActionMap().put(id,new AbstractAction(){public void actionPerformed(ActionEvent event){binding.getValue().run();}});
            }
            java.util.function.BooleanSupplier ready=()->client.connected()&&!Boolean.TRUE.equals(frame.getRootPane().getClientProperty(CLOSE_CONFIRMATION_PROPERTY))&&!Boolean.TRUE.equals(frame.getRootPane().getClientProperty("networkReconnecting"))&&(client.revisions()==null||!client.revisions().blocked());
            inputRouter[0]=new GameInputRouter(frame,panel,()->ready.getAsBoolean()&&client.snapshot()!=null&&client.snapshot().actors().stream().anyMatch(a->a.id()==client.id()&&a.hp()>0),gamePanels[0]::modal,releaseInput);
            inputRouter[0].recoveryAvailable(ready);
            inputRouter[0].targeting(client::cycleTarget,()->!gamePanels[0].modal());
            if(skillControls[0]!=null)inputRouter[0].aiming(skillControls[0]::aiming,skillControls[0]::aimKey,skillControls[0]::confirm);
            inputRouter[0].modalRoot(gamePanels[0]::active);frame.getRootPane().putClientProperty("networkInputRouter",inputRouter[0]);gamePanels[0].contextChanged(()->{releaseInput.run();inputRouter[0].reset();if(skillControls[0]!=null)skillControls[0].cancel();});
            Runnable changed=()->{var keys=inputRouter[0].shortcuts();hotbar.shortcuts(keys);questPanel.shortcuts(keys);gameHud[0].shortcuts(keys);inventoryButton.setText("Inventario ("+KeyEvent.getKeyText(keys.get("inventory"))+")");};
            boolean[] preferencesApplied={false};java.util.function.Consumer<com.projetok.rpg.RpgPreferences> applyPreferences=prefs->{preferencesApplied[0]=true;combatHud.reducedMotion(!prefs.environment());combatPresentation.reducedMotion(!prefs.environment());if(skillPresentation!=null)skillPresentation.reducedMotion(!prefs.environment());com.projetok.prototype.rpg.SpiritTheme.setReducedMotion(!prefs.environment());};
            settingsView[0]=new NetworkSettingsPanel(frame,inputRouter[0],com.projetok.prototype.rpg.PreferencesDialog.networkPreferencesFile(),applyPreferences,changed,()->windows[4].hidePanel());
            new SwingWorker<com.projetok.rpg.RpgPreferences,Void>(){protected com.projetok.rpg.RpgPreferences doInBackground()throws Exception{return com.projetok.rpg.RpgPreferences.load(com.projetok.prototype.rpg.PreferencesDialog.networkPreferencesFile());}protected void done(){if(!attached.get()||!frame.isDisplayable()||preferencesApplied[0])return;try{applyPreferences.accept(get());}catch(Exception ignored){/* Preferências inválidas não bloqueiam a conexão. */}}}.execute();
            windows[4]=gamePanels[0].register("settings","Configurações",settingsView[0],620,680,true);windows[4].onOpen(settingsView[0]::begin);windows[4].onClose(settingsView[0]::close);windows[4].canClose(settingsView[0]::canClose);changed.run();
        }
        Runnable refreshSnapshot=()->{
            if(skillPresentation!=null){skillPresentation.region(client.region());skillPresentation.update(client.connected()?client.skills():null);}
            if(client.supportsGameplay())combatPresentation.selection(client.selectedTarget());
            var latest=client.connected()?client.snapshot():new Protocol.State(display[0].tick(),java.util.List.of());long now=System.nanoTime();
            if(latest!=null){if(!client.connected())display[0]=latest;else{if(latest.tick()>acceptedTick[0]){timeline[0].offer(latest,client.diagnostics().receivedNanos(),client.region());acceptedTick[0]=latest.tick();}display[0]=timeline[0].sample(now,client.id(),scene[0]==null?com.projetok.domain.movement.SessionCollisionProfile.DEFAULT.actorRadius():scene[0].revision().profile().actorRadius(),id->skillPresentation!=null&&skillPresentation.hidden(id));animation.update(display[0],now/1_000_000);combatPresentation.update(latest,client.combatEvents(),client.id());mobSprites.update(latest);}}
            // Efeitos acompanham exatamente a posição usada para desenhar o corpo, inclusive no observador.
            if(skillPresentation!=null)skillPresentation.actors(client.connected()?display[0]:null);
        };
        var snapshotSignal=new SnapshotRefresh(SwingUtilities::invokeLater,()->{
            if(frame.isDisplayable()){refreshSnapshot.run();panel.repaint();}
        });
        client.onSnapshot(snapshotSignal);snapshotSignal.run();
        var timer=new javax.swing.Timer(16,e->{
            if(mapRenderer[0]!=null){panel.setPhysicalGeometryVisible(false);panel.setWorldActorsRenderer((g,actors,draw)->mapRenderer[0].actors(g,actors,draw));}
            reconnect.setEnabled(!client.connected()&&acceptsReconnect(frame)&&!Boolean.TRUE.equals(frame.getRootPane().getClientProperty("networkReconnecting")));
            refreshSnapshot.run();if(inputRouter[0]!=null)inputRouter[0].refreshContext();
            if(skillControls[0]!=null)skillControls[0].advanceAim(System.nanoTime());
            if(skillControls[0]!=null){var local=scene[0]==null?null:scene[0].revision().bundle().scenes().maps().get(client.region());skillControls[0].geometry(local==null?null:local.space());}
            ++count[0];sendMovement.run();var energyView=client.energyView();combatHud.energy(energyView.state(),energyView.pending());combatHud.update(client.connected()?client.snapshot():null,client.id(),combatPresentation);if(inventoryPanel!=null)inventoryPanel.update(client.connected()?client.playerSheet():null);if(client.supportsGameplay()){progressLabel.refresh(client.playerSheet());characterPanel.energy(energyView.state(),energyView.pending());characterPanel.refresh(client.connected()?client.playerSheet():null,client.snapshot()==null?null:client.snapshot().actors().stream().filter(actor->actor.id()==client.id()).findFirst().orElse(null));characterPanel.attributes(client.attributes());hotbar.resources(client.connected()?client.snapshot():null,client.id());hotbar.refresh(client.connected()?client.playerSheet():null);questPanel.refresh(client.connected()?client.quests():java.util.List.of());gameplayNotice.setText(frame.getRootPane().getClientProperty("networkReconnectError") instanceof String error?error:client.playerSheet()==null?"Sincronizando":client.playerSheet().notice());}
            var hud=new ArrayList<String>();hud.add("RPG COOPERATIVO EXPERIMENTAL");if(debug[0]){hud.add(endpoint);hud.add("Sessao "+client.id()+" | tick "+display[0].tick());}hud.add(client.connected()?"Conectado":"Desconectado: "+client.problem());
            if(count[0]%60==0){client.measureLatency();if(client.supportsGameplay()){client.requestAttributes();client.requestEnergy();}}hud.add(Double.isNaN(client.latencyMillis())?"RTT: aguardando":String.format(java.util.Locale.ROOT,"RTT: %.1f ms",client.latencyMillis()));hud.add(client.region().equals("forest")?"Bosque e clareira leste":"Vila e mercado");if(client.revisions()!=null){if(debug[0])hud.add("Revisao: "+client.revisions().hash());hud.add(client.revisions().blocked()?"Sincronizando mundo - aguarde":"Mundo sincronizado");if(debug[0])hud.add(client.revisions().problem());}hud.add("Jogadores: "+display[0].actors().stream().filter(a->!a.boss()).count());
            display[0].actors().stream().filter(a->a.id()==client.id()).findFirst().ifPresent(a->hud.add("HP: "+a.hp()+"/"+a.maxHp()));
            display[0].actors().stream().filter(a->a.id()>1_000_000&&a.id()<2_000_000).findFirst().ifPresent(a->hud.add("Inimigo HP: "+a.hp()+"/"+a.maxHp()));
            hud.add(client.supportsCombat()?"SPACE: ataque | SHIFT/Q: Shunpo":"Combate exige --combat");hud.add("WASD/setas: mover | P: portal | R: respawn");hud.add("F: NPC | E/C: aceitar/concluir");
            if(debug[0]){var d=client.diagnostics();hud.add(String.format(Locale.ROOT,"RTT med/p95: %.1f / %.1f ms",d.rtt.percentileMillis(.5),d.rtt.percentileMillis(.95)));double gap=d.snapshotIntervals.percentileMillis(.5);hud.add(String.format(Locale.ROOT,"Snapshot: %.1f Hz | age %.1f ms",1000/gap,d.snapshotAgeMillis(System.nanoTime())));hud.add(String.format(Locale.ROOT,"Buffer: %.1f ms | stale: %d",timeline[0].bufferMillis(),d.staleSnapshots+timeline[0].dropped));hud.add(String.format(Locale.ROOT,"Render: %.1f FPS | paint %.2f ms",1000/d.paintIntervals.percentileMillis(.5),d.paints.percentileMillis(.95)));hud.add("Queue: "+d.outboundQueue+" | resets: "+timeline[0].corrections);hud.add("Tick/ACK: indisponiveis no protocolo");}if(!debug[0])hud.add("Progresso sintetico, sem save pessoal | F2 debug");panel.setCustomHud(hud);
            if(gameHud[0]!=null){var selected=client.characterSelection();String identity=selected==null?"Spirit Gate":selected.name();String status=!client.connected()?"Desconectado - reconecte pelo menu":client.revisions()!=null&&client.revisions().blocked()?"Sincronizando o mundo":client.region().equals("forest")?"Bosque e clareira leste":"Vila e mercado";
                gameHud[0].refresh(client.connected()?client.snapshot():null,client.id(),client.region(),scene[0],identity,status,hud,debug[0]);gameHud[0].target(client.connected()?client.snapshot():null,client.id(),client.selectedTarget(),client.playerSheet()==null?60:client.playerSheet().abilities().basicRange());
                if(skillControls[0]!=null){gameHud[0].skillFeedback(skillControls[0].feedbackText(),skillControls[0].inputHint());gameHud[0].skillTargetHint(skillControls[0].targetHint());}}
            panel.repaint();
        });
        Runnable detach=()->{if(!attached.compareAndSet(true,false))return;timer.stop();if(gamePanels[0]!=null)gamePanels[0].close();if(inputRouter[0]!=null)inputRouter[0].close();client.onSnapshot(null);client.close();};
        frame.addWindowFocusListener(new WindowAdapter(){public void windowLostFocus(WindowEvent e){held.clear();sendMovement.run();if(skillControls[0]!=null)skillControls[0].cancel();}});
        frame.addWindowListener(new WindowAdapter(){public void windowClosed(WindowEvent e){detach.run();}});
        if(inputRouter[0]!=null)inputRouter[0].install();
        var ownedWindows=java.util.Arrays.stream(frame.getWindowListeners()).filter(value->!previousWindowListeners.contains(value)).toList();
        var ownedFocus=java.util.Arrays.stream(frame.getWindowFocusListeners()).filter(value->!previousFocusListeners.contains(value)).toList();
        frame.getRootPane().putClientProperty("networkDetach",(Runnable)()->{detach.run();ownedWindows.forEach(frame::removeWindowListener);ownedFocus.forEach(frame::removeWindowFocusListener);});
        timer.start();frame.revalidate();frame.repaint();frame.setVisible(true);
        if(skillPresentation!=null)new SwingWorker<java.util.Map<String,NetworkSkillPresentation.Clip>,Void>(){
            protected java.util.Map<String,NetworkSkillPresentation.Clip> doInBackground()throws Exception{return NetworkSkillPresentation.load(skillAssetRoot());}
            protected void done(){if(!attached.get()||!frame.isDisplayable())return;try{skillPresentation.clips(get());panel.repaint();}catch(Exception error){frame.getRootPane().putClientProperty("networkSkillAssetsError",error.getMessage());skillControls[0].assetError("não foi possível carregar as animações locais");System.err.println("[Spirit Gate] Efeitos de habilidades indisponíveis: "+error.getMessage());}}
        }.execute();
        onWindow.accept(frame);return frame;
    }
    /** Localiza os assets no checkout ou junto ao executável, sem depender do diretório do Maven. */
    private static Path skillAssetRoot(){
        String executable=System.getProperty("jpackage.app-path");Path candidate=executable==null?Path.of(".").toAbsolutePath():Path.of(executable).toAbsolutePath().getParent();
        for(int level=0;level<4&&candidate!=null;level++,candidate=candidate.getParent())if(Files.isRegularFile(candidate.resolve("assets/skills/manifest.properties")))return candidate;
        return Path.of(".").toAbsolutePath();
    }
}
