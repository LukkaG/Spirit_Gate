package com.projetok.prototype.rpg;

import com.projetok.prototype.*;
import javax.swing.*;
import java.awt.event.*;
import java.util.List;

/** Entrada RPG explicita. A arena permanece acessivel pelo DebugLauncher original. */
public final class RpgLauncher {
    private RpgLauncher() { }
    public static void main(String[] args) {
        if(args.length==2&&args[0].equals("--verify-install")) {
            LauncherSelfCheck.write(java.nio.file.Path.of(args[1]));return;
        }
        if(args.length>0 && args[0].equals("--experimental-player-sprites")) {
            System.setProperty(RpgPlayerSprites.ENABLED,"true");args=java.util.Arrays.copyOfRange(args,1,args.length);
        }
        if(args.length==3&&args[0].equals("--smoke")){System.setProperty("spiritgate.smoke.name",args[1]);System.setProperty("spiritgate.smoke.output",args[2]);}
        else if(args.length!=0)throw new IllegalArgumentException("Uso: RpgLauncher [--experimental-player-sprites] [--smoke nome captura.png]");
        SwingUtilities.invokeLater(()->{if(System.getProperty("spiritgate.smoke.name")!=null)open();else SpiritGateEntry.open();});
    }
    static void open() {
        String name = System.getProperty("spiritgate.smoke.name");
        if(name==null)name=JOptionPane.showInputDialog(null, "Nome do personagem (3 a 24 caracteres)", "Criar personagem", JOptionPane.QUESTION_MESSAGE);
        if (name == null) return;
        com.projetok.rpg.CharacterIdentity identity;
        try { identity = com.projetok.rpg.CharacterIdentity.create(name, "reaper", "azure"); }
        catch (IllegalArgumentException error) { JOptionPane.showMessageDialog(null, error.getMessage()); open(); return; }
        play(new com.projetok.rpg.RpgWorld(identity),new com.projetok.rpg.LocalSaveStore(RpgMenu.dataHome().resolve("saves/characters/"+identity.id()+".properties")));
    }
    static void play(com.projetok.rpg.RpgWorld world,com.projetok.rpg.LocalSaveStore store) {
        play(world,store,false);
    }
    static void play(com.projetok.rpg.RpgWorld world,com.projetok.rpg.LocalSaveStore store,boolean introduction) {
        RpgRuntime runtime = new RpgRuntime(world);
        var preferencesFile=RpgMenu.dataHome().resolve("saves/preferences.properties");
        com.projetok.rpg.RpgPreferences[] preferences={com.projetok.rpg.RpgPreferences.defaults()};
        try{preferences[0]=com.projetok.rpg.RpgPreferences.load(preferencesFile);}catch(Exception error){JOptionPane.showMessageDialog(null,"Preferencias invalidas preservadas; usando padrao: "+error.getMessage());}
        RpgAudio audio=new RpgAudio(preferences[0]);audio.play(RpgAudio.Cue.UI);
        SpiritTheme.reducedMotion=!preferences[0].environment();AmbientFeedback ambientFeedback=new AmbientFeedback(audio::play);ambientFeedback.update(runtime.snapshot());
        DebugSnapshot template = DebugWorld.demo().snapshot();
        DebugPanel panel = new DebugPanel(() -> adapt(runtime.snapshot(), template));
        RpgPlayerSprites playerSprites=RpgPlayerSprites.configured();
        if(playerSprites!=null) { playerSprites.update(runtime.snapshot());panel.setActorRenderer(playerSprites); }
        RpgCombatFeedback feedback=new RpgCombatFeedback();feedback.update(runtime.snapshot());RpgBossOverlay bossOverlay=new RpgBossOverlay();bossOverlay.update(runtime.snapshot());panel.setWorldOverlay(g->{bossOverlay.draw(g);feedback.draw(g);});
        panel.setLocalMap(RpgMaps.region("village"),false);
        JFrame frame = new JFrame("Spirit Gate | RPG offline"+(playerSprites==null?"":" | SPRITES EXPERIMENTAIS"));
        java.beans.PropertyChangeListener audioFocus=e->{boolean internal=false;for(java.awt.Window active=(java.awt.Window)e.getNewValue();active!=null;active=active.getOwner())if(active==frame)internal=true;audio.suspended(!internal);};
        java.awt.KeyboardFocusManager.getCurrentKeyboardFocusManager().addPropertyChangeListener("activeWindow",audioFocus);
        JPanel root=new JPanel(new java.awt.BorderLayout());RpgHud hud=new RpgHud(runtime);root.add(panel,java.awt.BorderLayout.CENTER);root.add(hud,java.awt.BorderLayout.SOUTH);
        frame.setContentPane(root); frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        InventoryWindow inventory=new InventoryWindow(frame,runtime);
        ExplorationWindow exploration=new ExplorationWindow(frame,runtime);
        NavigationMap navigation=new NavigationMap(runtime);root.add(navigation,java.awt.BorderLayout.EAST);
        java.nio.file.Path controlsFile=RpgMenu.dataHome().resolve("saves/controls.properties");
        com.projetok.rpg.RpgControls[] controls={new com.projetok.rpg.RpgControls(com.projetok.rpg.RpgControls.DEFAULTS)};
        try{controls[0]=com.projetok.rpg.RpgControls.load(controlsFile);}catch(Exception error){JOptionPane.showMessageDialog(frame,"Atalhos invalidos preservados; usando padrao: "+error.getMessage());}
        java.util.Map<String,Runnable> actions=new java.util.TreeMap<>();
        actions.put("attack",()->runtime.command(w->w.activate("rpg.attack")));actions.put("ability",()->runtime.command(w->w.activate("rpg.ability")));actions.put("potion",()->runtime.command(w->w.activate("item.potion")));actions.put("interact",()->runtime.command(w->w.interact()));actions.put("revive",()->runtime.command(w->w.returnHome()));
        actions.put("inventory",()->inventory.setVisible(!inventory.isVisible()));actions.put("journal",()->exploration.showTab(0));actions.put("map",()->exploration.showTab(1));actions.put("save",()->runtime.save(store).whenComplete((unused,error)->SwingUtilities.invokeLater(()->JOptionPane.showMessageDialog(frame,error==null?"Progresso salvo":"Falha ao salvar: "+error.getMessage()))));
        Runnable bindActions=()->actions.forEach((id,action)->bind(panel,controls[0].key(id),action));bindActions.run();
        boolean amber=runtime.snapshot().identity().appearance().equals("amber");
        panel.setActorPalette(id->id==1?(amber?new java.awt.Color(230,177,65):new java.awt.Color(65,190,220)):id>=500?new java.awt.Color(90,200,160):null);
        JButton menuButton=new JButton("Salvar e voltar ao menu");menuButton.addActionListener(e->frame.dispatchEvent(new WindowEvent(frame,WindowEvent.WINDOW_CLOSING)));JPanel toolbar=new JPanel(new java.awt.GridLayout(0,4,4,4));toolbar.add(menuButton);root.add(toolbar,java.awt.BorderLayout.NORTH);
        java.util.Map<String,JButton> hotbar=new java.util.TreeMap<>();
        for(String action:new String[]{"attack","ability","potion","inventory","journal","map"}){JButton button=new JButton(actionLabel(action)+" ["+controls[0].key(action)+"]");button.addActionListener(e->{audio.play(RpgAudio.Cue.UI);actions.get(action).run();});toolbar.add(button);hotbar.put(action,button);}
        var secondary=com.projetok.rpg.RpgSecondaryAbility.forArchetype(world.snapshot().identity().archetype());
        JButton secondaryButton=new JButton(secondary.name());
        secondaryButton.setToolTipText("Requer nivel 20. Custo "+secondary.cost()+" vigor; recarga compartilhada "+RpgCombatStatus.cooldown(secondary.cooldown()));
        hotbar.get("ability").setToolTipText("Shunpo ofensivo: requer nivel 20 e alvo visivel; recarga compartilhada com a esquiva.");
        secondaryButton.addActionListener(e->runtime.command(w->w.activate("rpg.secondary")));toolbar.add(secondaryButton);
        JButton punchButton=new JButton("Soco desarmado");
        punchButton.addActionListener(e->runtime.command(w->w.activate("rpg.unarmed")));toolbar.add(punchButton);
        JButton domainsButton=new JButton("Dominios e tecnicas");
        domainsButton.addActionListener(e->{runtime.input(0,0);var v=runtime.snapshot();
            DomainPreview.open(frame,v.identity().race(),v.level(),com.projetok.rpg.DomainProgression.read(v.state().flags()).domain(),
                selected->runtime.action(w->w.chooseDomain(selected)).thenAccept(ok->SwingUtilities.invokeLater(()->{
                    if(!ok)JOptionPane.showMessageDialog(frame,"Escolha recusada. Confira nivel, raca e dominio atual.");
                    else runtime.save(store).whenComplete((unused,error)->SwingUtilities.invokeLater(()->JOptionPane.showMessageDialog(frame,
                        error==null?"Dominio escolhido e salvo.":"Dominio escolhido. Falha ao salvar: "+error.getMessage())));
                })));});toolbar.add(domainsButton);
        JButton resetButton=new JButton("Reset de dominio");
        resetButton.addActionListener(e->JOptionPane.showMessageDialog(frame,world.resetRequirements(),"Reset de dominio",JOptionPane.INFORMATION_MESSAGE));toolbar.add(resetButton);
        JButton configure=new JButton("Atalhos");configure.addActionListener(e->ControlsDialog.open(frame,controls[0],controlsFile,next->{for(String key:controls[0].keys().values())for(boolean down:new boolean[]{true,false}){panel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).remove(KeyStroke.getKeyStroke((down?"pressed ":"released ")+key));panel.getActionMap().remove(key+down);}resetHeld(panel);controls[0]=next;bindActions.run();hotbar.forEach((id,button)->button.setText(actionLabel(id)+" ["+next.key(id)+"]"));}));toolbar.add(configure);
        JButton settings=new CharacterSelection.SpiritButton("Opções",false);settings.addActionListener(e->PreferencesDialog.open(frame,preferences[0],preferencesFile,next->{preferences[0]=next;audio.preferences(next);audio.play(RpgAudio.Cue.UI);}));toolbar.add(settings);
        JButton help=new CharacterSelection.SpiritButton("Ajuda",false);help.addActionListener(e->GettingStarted.open(frame,controls[0]));toolbar.add(help);
        boolean[] showNavigation={true};JButton minimap=new CharacterSelection.SpiritButton("Minimapa",false);minimap.addActionListener(e->{showNavigation[0]=!showNavigation[0];navigation.setVisible(showNavigation[0]&&frame.getWidth()>=800);});toolbar.add(minimap);
        frame.addComponentListener(new java.awt.event.ComponentAdapter(){public void componentResized(java.awt.event.ComponentEvent e){navigation.setVisible(showNavigation[0]&&frame.getWidth()>=800);}});
        frame.addWindowListener(new WindowAdapter(){public void windowClosing(WindowEvent e){runtime.input(0,0);inventory.setVisible(false);exploration.setVisible(false);frame.setEnabled(false);runtime.saveAndPause(store).whenComplete((unused,error)->SwingUtilities.invokeLater(()->{if(error!=null){frame.setEnabled(true);JOptionPane.showMessageDialog(frame,"Falha ao salvar; sessao preservada: "+error.getMessage());}else frame.dispose();}));}});
        frame.pack(); frame.setLocationRelativeTo(null);
        var screen=java.awt.Toolkit.getDefaultToolkit().getScreenSize();frame.setSize(Math.min(1260,screen.width-60),Math.min(900,screen.height-80));frame.setLocationRelativeTo(null);
        boolean[] held = new boolean[4];
        bind(panel,"ESCAPE",()->runtime.command(w->w.closeDialogue()));
        String[] keys = {"W", "S", "A", "D"};
        for (int i = 0; i < keys.length; i++) {
            final int key = i;
            for (boolean down : new boolean[]{true, false}) {
                String id = keys[i] + down;
                panel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke((down ? "pressed " : "released ") + keys[i]), id);
                panel.getActionMap().put(id, new AbstractAction() {
                    public void actionPerformed(ActionEvent e) { held[key] = down; runtime.input((held[3]?1:0)-(held[2]?1:0), (held[1]?1:0)-(held[0]?1:0)); }
                });
            }
        }
        String[] displayedRegion={"village"};
        com.projetok.rpg.RpgWorld.View[] previous={runtime.snapshot()};
        Timer timer = new Timer(33, e -> {
            if(runtime.failure()!=null) {
                ((Timer)e.getSource()).stop();runtime.close();frame.setEnabled(false);
                JOptionPane.showMessageDialog(frame,"A simulacao foi interrompida: "+runtime.failure().getMessage()+"\nO progresso apos o ultimo salvamento nao foi confirmado. Voltando ao menu.","Falha da simulacao",JOptionPane.ERROR_MESSAGE);
                frame.dispose();return;
            }
            var view=runtime.snapshot();var before=previous[0];
            ambientFeedback.update(view);SpiritTheme.reducedMotion=!preferences[0].environment();
            feedback.update(view);bossOverlay.update(view);
            if(playerSprites!=null) playerSprites.update(view);
            if(view.state().hp()<before.state().hp())audio.play(RpgAudio.Cue.HURT);
            else if(view.state().hp()>before.state().hp())audio.play(RpgAudio.Cue.HEAL);
            if(view.level()>before.level())audio.play(RpgAudio.Cue.LEVEL);
            if(view.state().region().equals(before.state().region())&&view.enemies().stream().mapToInt(com.projetok.rpg.EnemyService.View::hp).sum()<before.enemies().stream().mapToInt(com.projetok.rpg.EnemyService.View::hp).sum())audio.play(RpgAudio.Cue.HIT);
            if(view.ticks()/900>before.ticks()/900)audio.play(RpgAudio.Cue.AMBIENT);previous[0]=view;
            var keysNow=controls[0];
            secondaryButton.setEnabled(RpgCombatStatus.from(view).abilityReady());
            hotbar.get("ability").setEnabled(RpgCombatStatus.from(view).abilityReady());
            var domain=com.projetok.rpg.DomainProgression.read(view.state().flags());
            panel.setCustomHud(java.util.List.of("SPIRIT GATE",view.identity().name()+" | "+com.projetok.rpg.CharacterRaces.get(view.identity().race()).name(),"Nivel "+view.level()+" | XP "+view.state().xp(),"Dominio: "+(domain.chosen()?com.projetok.rpg.Domains.get(domain.domain()).name()+" | XP "+domain.xpText():"Aprendiz - escolha no nivel 15"),"HP "+view.state().hp()+" / "+view.maxHp(),"Ataque "+view.attack()+" | Defesa "+view.defense(),"Moedas "+view.state().coins(),"Regiao: "+view.state().region(),"["+keysNow.key("interact")+"] NPC / portal / objeto","["+keysNow.key("attack")+"] Ataque basico","["+keysNow.key("ability")+"] Shunpo (nivel 20)","["+keysNow.key("potion")+"] Pocao de vida","["+keysNow.key("inventory")+"] Inventario / equipamentos","["+keysNow.key("journal")+"] Diario | ["+keysNow.key("map")+"] Mapa","["+keysNow.key("save")+"] Salvar progresso","["+keysNow.key("revive")+"] Recuperar se morto"));
            panel.setAmbientLight(com.projetok.rpg.EnvironmentCycle.light(view.ticks(),preferences[0].environment()));
            if(!displayedRegion[0].equals(view.state().region())){displayedRegion[0]=view.state().region();panel.setLocalMap(RpgMaps.region(displayedRegion[0]),false);}
            panel.repaint();hud.update(view);navigation.repaint();
        });
        frame.addWindowListener(new WindowAdapter() { public void windowClosed(WindowEvent e) { timer.stop(); runtime.close();audio.close();java.awt.KeyboardFocusManager.getCurrentKeyboardFocusManager().removePropertyChangeListener("activeWindow",audioFocus);if(System.getProperty("spiritgate.smoke.output")==null)RpgMenu.open(); } });
        frame.addWindowFocusListener(new WindowAdapter() { public void windowLostFocus(WindowEvent e) {java.util.Arrays.fill(held, false);resetHeld(panel); runtime.input(0, 0); } });
        for(java.awt.Component control:toolbar.getComponents())if(control instanceof JButton button&&button.getToolTipText()==null)button.setToolTipText(button.getText());
        runtime.start(); timer.start(); frame.setVisible(true);
        if(introduction)GettingStarted.open(frame,controls[0]);
        if(System.getProperty("spiritgate.smoke.output")!=null){Timer capture=new Timer(2000,e->{
            java.awt.Rectangle bounds=frame.getBounds();String output=System.getProperty("spiritgate.smoke.output");
            var componentImage=new java.awt.image.BufferedImage(frame.getWidth(),frame.getHeight(),java.awt.image.BufferedImage.TYPE_INT_RGB);var graphics=componentImage.createGraphics();frame.printAll(graphics);graphics.dispose();
            var state=runtime.snapshot();boolean showing=frame.isShowing();
            new Thread(()->{try{var image=new java.awt.Robot().createScreenCapture(bounds);javax.imageio.ImageIO.write(image,"png",new java.io.File(output));javax.imageio.ImageIO.write(componentImage,"png",new java.io.File(output+".components.png"));java.nio.file.Files.writeString(java.nio.file.Path.of(output+".json"),"{\"windowShowing\":"+showing+",\"ticks\":"+state.ticks()+",\"region\":\""+state.state().region()+"\",\"componentCapture\":true,\"humanApproval\":false}");}catch(Exception error){error.printStackTrace();}finally{SwingUtilities.invokeLater(frame::dispose);}},"rpg-smoke").start();
        });capture.setRepeats(false);capture.start();}
    }
    static DebugSnapshot adapt(com.projetok.rpg.RpgWorld.View v, DebugSnapshot t) {
        return adapt(v,t,List.of(new DebugSnapshot.Obstacle(380,180,120,120)));
    }
    /** Adapta a mesma simulacao com geometria explicita de sessao de autoria. */
    public static DebugSnapshot adapt(com.projetok.rpg.RpgWorld.View v,DebugSnapshot t,List<DebugSnapshot.Obstacle> obstacles) {
        var actors=new java.util.ArrayList<DebugSnapshot.ActorView>();
        actors.add(new DebugSnapshot.ActorView(1,v.identity().name(),v.x(),v.y(),18,v.state().hp(),v.maxHp(),true,v.state().hp()>0));
        for(var enemy:v.enemies())actors.add(new DebugSnapshot.ActorView(enemy.id(),enemy.name(),enemy.x(),enemy.y(),19,enemy.hp(),enemy.maxHp(),false,enemy.hp()>0));
        int npcId=500;for(var npc:v.npcs())actors.add(new DebugSnapshot.ActorView(npcId++,npc.name(),npc.x(),npc.y(),16,1,1,false,true));
        for(var portal:v.portals())actors.add(new DebugSnapshot.ActorView(npcId++,"Portal: "+portal.destination(),portal.x(),portal.y(),22,1,1,false,true));
        for(var object:v.objects())if(object.available())actors.add(new DebugSnapshot.ActorView(npcId++,object.id().contains("chest")?"Bau do aprendiz":object.id().contains("herbs")?"Ervas curativas":"Reserva de pocoes",object.x(),object.y(),15,1,1,false,true));
        java.util.List<DebugSnapshot.IceZoneView> warnings=List.of(); // Aviso RPG ocluido pelo overlay do chefe.
        return new DebugSnapshot(actors,
            obstacles,List.of(),v.state().hp(),v.maxHp(),0,v.state().hp()==0,v.ticks(),v.message(),1,0,0,null,
            100,100,0,warnings,List.of(),false,0,0,List.of(),false,List.of(),t.cryostasisHud(),t.lightningStepHud());
    }
    /** Binding compartilhado que evita repeticao automatica de comandos. */
    public static void bind(JComponent component,String key,Runnable action) {
        @SuppressWarnings("unchecked") java.util.Set<String> existing=(java.util.Set<String>)component.getClientProperty("rpg.held");
        if(existing==null){existing=new java.util.HashSet<>();component.putClientProperty("rpg.held",existing);}final java.util.Set<String> held=existing;
        for(boolean down:new boolean[]{true,false}) {
            String id=key+down;
            component.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke((down?"pressed ":"released ")+key),id);
            component.getActionMap().put(id,new AbstractAction(){public void actionPerformed(ActionEvent e){if(down){if(held.add(key))action.run();}else held.remove(key);}});
        }
    }
    /** Limpa teclas mantidas ao perder foco. */
    public static void resetHeld(JComponent component){Object held=component.getClientProperty("rpg.held");if(held instanceof java.util.Set<?> keys)keys.clear();}
    static String actionLabel(String action){return switch(action){case "attack"->"Ataque basico";case "ability"->"Shunpo ofensivo";case "potion"->"Pocao";case "inventory"->"Inventario";case "journal"->"Diario";case "map"->"Mapa";case "interact"->"Interagir";case "save"->"Salvar";case "revive"->"Recuperar";default->action;};}
}
