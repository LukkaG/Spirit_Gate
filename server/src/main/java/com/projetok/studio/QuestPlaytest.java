package com.projetok.studio;

import com.projetok.rpg.*;
import com.projetok.prototype.*;
import com.projetok.prototype.rpg.*;
import java.awt.*;
import java.awt.event.*;
import java.nio.file.Path;
import java.util.*;
import javax.swing.*;

/** Playtest de missões pelo runtime real, com perfil sintético e sem controles de save. */
final class QuestPlaytest {
    private QuestPlaytest() { }
    static void launch(Component owner,QuestBook book,Path project){
        book.requireValid();var race=new JComboBox<>(CharacterRaces.all().stream().map(CharacterRaces.Definition::id).toArray(String[]::new));var level=new JSpinner(new SpinnerNumberModel(1,1,100,1));var domain=new JComboBox<String>();
        Runnable domains=()->{domain.removeAllItems();domain.addItem("");Domains.forRace((String)race.getSelectedItem()).forEach(d->domain.addItem(d.id()));};race.addActionListener(e->domains.run());domains.run();
        JPanel form=new JPanel(new GridLayout(0,2,5,5));form.add(new JLabel("Raça sintética"));form.add(race);form.add(new JLabel("Nível sintético"));form.add(level);form.add(new JLabel("Domínio (somente nível 15+)"));form.add(domain);
        if(JOptionPane.showConfirmDialog(owner,form,"Perfil isolado — nenhum save pessoal",JOptionPane.OK_CANCEL_OPTION)!=JOptionPane.OK_OPTION)return;
        String selectedRace=(String)race.getSelectedItem(),selectedDomain=(String)domain.getSelectedItem();int selectedLevel=(Integer)level.getValue();
        if(!selectedDomain.isEmpty()&&selectedLevel<DomainProgression.INITIATION_LEVEL)throw new IllegalArgumentException("Escolha de domínio exige nível 15");
        new SwingWorker<RpgWorld,Void>(){
            protected RpgWorld doInBackground(){var world=new RpgWorld(CharacterIdentity.create("Playtest",""+selectedRace,"azure"),null,book);world.awardExperience(RpgProgression.builtin().threshold(selectedLevel));if(!selectedDomain.isEmpty()&&!world.chooseDomain(selectedDomain))throw new IllegalStateException("Domínio recusado pela progressão");return world;}
            protected void done(){try{open(get(),book);}catch(Exception error){JOptionPane.showMessageDialog(owner,error.getCause()==null?error.getMessage():error.getCause().getMessage(),"Playtest recusado",JOptionPane.ERROR_MESSAGE);}}
        }.execute();
    }
    static JFrame open(RpgWorld world,QuestBook book){
        return open(world,book,null,null);
    }
    /** Apresentação opcional do bundle, mantendo a mesma sessão de quests e física. */
    static JFrame open(RpgWorld world,QuestBook book,StudioExport.Bundle bundle,AssetImages images){
        if(!world.isSynthetic())throw new IllegalArgumentException("Perfil pessoal não pode entrar no playtest");
        RpgRuntime runtime=new RpgRuntime(world);var template=DebugWorld.demo(new Random(91)).snapshot();var panel=new DebugPanel(()->{var v=runtime.snapshot();var obstacles=bundle==null?java.util.List.of(new DebugSnapshot.Obstacle(380,180,120,120)):bundle.scenes().maps().get(v.state().region()).space().obstacles().stream().map(o->new DebugSnapshot.Obstacle(o.x(),o.y(),o.width(),o.height())).toList();return RpgLauncher.adapt(v,template,obstacles);});
        JFrame frame=new JFrame("Spirit Gate | QUEST PLAYTEST SINTÉTICO");frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);frame.setSize(1250,850);
        frame.getRootPane().putClientProperty("syntheticRuntime",runtime);
        if(bundle!=null){var render=new StudioMapRenderer(bundle,images,runtime::snapshot,null);panel.setActorRenderer(render);panel.setWorldUnderlay(g->render.layers(g,false));panel.setWorldOverlay(g->render.layers(g,true));var minimap=new StudioMiniMap(render::document,runtime::snapshot);frame.add(minimap,BorderLayout.EAST);panel.addPropertyChangeListener(e->minimap.repaint());}
        JComboBox<String> quests=new JComboBox<>(book.quests().stream().map(QuestBook.Draft::id).toArray(String[]::new));JButton accept=new JButton("Aceitar no NPC"),complete=new JButton("Concluir no NPC");JLabel status=new JLabel("WASD mover | F conversar | SPACE atacar | Q Shunpo (20+) | sem gravação V2");JTextArea journal=new JTextArea(7,30);journal.setEditable(false);
        JPanel top=new JPanel();top.add(quests);top.add(accept);top.add(complete);accept.addActionListener(e->runtime.action(w->w.acceptAuthoredQuest((String)quests.getSelectedItem())).thenAccept(ok->SwingUtilities.invokeLater(()->status.setText(ok?"Missão aceita":"Acesso recusado: confira NPC, raça, nível, domínio e dependências"))));complete.addActionListener(e->runtime.action(w->w.completeAuthoredQuest((String)quests.getSelectedItem())).thenAccept(ok->SwingUtilities.invokeLater(()->status.setText(ok?"Recompensa aplicada uma vez":"Conclusão recusada: confira objetivos e NPC"))));
        JToggleButton debug=new JToggleButton("Diagnóstico");top.add(debug);
        JButton checkpoint=new JButton("Checkpoint em memória"),resume=new JButton("Retomar cópia");top.add(checkpoint);top.add(resume);var saved=new java.util.concurrent.atomic.AtomicReference<RpgWorld>();
        checkpoint.addActionListener(e->runtime.action(w->{saved.set(w.forkSynthetic());return true;}).whenComplete((ok,error)->SwingUtilities.invokeLater(()->status.setText(error==null&&Boolean.TRUE.equals(ok)?"Checkpoint sintético em memória criado":"Checkpoint indisponível"))));
        resume.addActionListener(e->{var copy=saved.get();if(copy==null){status.setText("Crie um checkpoint em memória primeiro.");return;}resume.setEnabled(false);new SwingWorker<RpgWorld,Void>(){protected RpgWorld doInBackground(){return copy.forkSynthetic();}protected void done(){try{open(get(),book);}catch(Exception error){status.setText("Retomada recusada: "+error.getMessage());}finally{resume.setEnabled(true);}}}.execute();});
        JPanel bottom=new JPanel(new BorderLayout());bottom.add(new JScrollPane(journal));bottom.add(status,BorderLayout.SOUTH);frame.add(top,BorderLayout.NORTH);frame.add(panel);frame.add(bottom,BorderLayout.SOUTH);
        boolean[] held=new boolean[4];String[] keys={"W","S","A","D"};for(int i=0;i<4;i++)for(boolean down:new boolean[]{true,false}){final int at=i;String key=keys[i]+down;panel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke((down?"pressed ":"released ")+keys[i]),key);panel.getActionMap().put(key,new AbstractAction(){public void actionPerformed(ActionEvent e){held[at]=down;runtime.input((held[3]?1:0)-(held[2]?1:0),(held[1]?1:0)-(held[0]?1:0));}});}
        RpgLauncher.bind(panel,"F",()->runtime.command(RpgWorld::interact));RpgLauncher.bind(panel,"SPACE",()->runtime.command(RpgWorld::attack));RpgLauncher.bind(panel,"Q",()->runtime.command(RpgWorld::castAbility));
        var views=new java.util.concurrent.atomic.AtomicReference<java.util.List<QuestService.View>>(java.util.List.of());
        javax.swing.Timer timer=new javax.swing.Timer(100,e->{runtime.command(w->views.set(w.authoredQuestViews()));journal.setText(views.get().stream().map(q->q.name()+" | "+q.state()+" | "+q.progress()+"/"+q.required()+" | "+q.objective()).collect(java.util.stream.Collectors.joining("\n")));var v=runtime.snapshot();panel.setCustomHud(debug.isSelected()?RpgDevelopmentDiagnostics.lines(v,"padrao"):java.util.List.of("QUEST PLAYTEST SINTÉTICO",v.identity().race()+" | nível "+v.level(),"XP: "+v.state().xp(),"Domínio: "+DomainProgression.read(v.state().flags()).domain(),"Sem gravação V2",v.message()));frame.repaint();});
        frame.addWindowFocusListener(new WindowAdapter(){public void windowLostFocus(WindowEvent e){Arrays.fill(held,false);runtime.input(0,0);}});frame.addWindowListener(new WindowAdapter(){public void windowClosed(WindowEvent e){timer.stop();runtime.close();}});runtime.start();timer.start();frame.setVisible(true);return frame;
    }
}
