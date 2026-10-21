package com.projetok.prototype.rpg;
import com.projetok.rpg.*;
import javax.swing.*;
import java.awt.*;
/** Diario e representacao da regiao atual; nao revela mapas futuros. */
final class ExplorationWindow extends JDialog {
    private final JTextArea journal=new JTextArea();
    private final NavigationMap navigation;
    private final JTabbedPane tabs=new JTabbedPane();
    private final RpgRuntime runtime;
    private final DefaultListModel<QuestService.View> missions=new DefaultListModel<>();
    private final JList<QuestService.View> selection=new JList<>(missions);
    private java.util.List<QuestService.View> lastMissions=java.util.List.of();
    ExplorationWindow(JFrame owner,RpgRuntime runtime){
        super(owner,"Diario e exploracao",false);this.runtime=runtime;navigation=new NavigationMap(runtime);setDefaultCloseOperation(HIDE_ON_CLOSE);
        journal.setEditable(false);journal.setLineWrap(true);journal.setWrapStyleWord(true);
        selection.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        selection.setCellRenderer((list,value,index,selected,focus)->{JLabel label=new JLabel(value.state()==QuestService.State.UNAVAILABLE?"Missão indisponível":value.name()+" | "+value.state());label.setOpaque(true);label.setBackground(selected?new Color(18,66,85):SpiritTheme.BACKGROUND);label.setForeground(Color.WHITE);label.setBorder(BorderFactory.createLineBorder(focus?Color.WHITE:SpiritTheme.ACCENT));return label;});
        selection.addListSelectionListener(e->journal.setText(selection.getSelectedValue()==null?"Nenhuma missão conhecida":describe(selection.getSelectedValue())));
        JSplitPane diary=new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,new JScrollPane(selection),new JScrollPane(journal));diary.setResizeWeight(.35);
        JPanel regionMap=new JPanel(new BorderLayout());regionMap.add(navigation);JSpinner zoom=new JSpinner(new SpinnerNumberModel(1.0,1.0,3.0,.25));zoom.getAccessibleContext().setAccessibleName("Zoom do mapa");zoom.addChangeListener(e->navigation.zoom((Double)zoom.getValue()));regionMap.add(zoom,BorderLayout.SOUTH);
        tabs.addTab("Diario",diary);tabs.addTab("Mapa atual",regionMap);SpiritTheme.apply(tabs);setContentPane(tabs);setSize(800,600);setLocationRelativeTo(owner);
        getRootPane().registerKeyboardAction(e->setVisible(false),KeyStroke.getKeyStroke("ESCAPE"),JComponent.WHEN_IN_FOCUSED_WINDOW);
        Timer timer=new Timer(200,e->{if(isVisible())refresh();});timer.start();owner.addWindowListener(new java.awt.event.WindowAdapter(){public void windowClosed(java.awt.event.WindowEvent e){timer.stop();dispose();}});
    }
    void showTab(int index){tabs.setSelectedIndex(index);refresh();setVisible(true);}
    /** Detalhes derivados do catálogo; missões bloqueadas não revelam objetivos. */
    static String describe(QuestService.View quest){
        if(quest.state()==QuestService.State.UNAVAILABLE)return "Missão ainda indisponível";
        var reward=new QuestService(RpgContent.builtin()).definition(quest.id());
        var item=new ItemCatalog(RpgContent.builtin()).get(reward.item());
        return quest.name()+" — "+quest.state()+"\n\nProgresso: "+quest.progress()+" / "+quest.required()+"\n"+quest.objective()+"\n\nRecompensa: "+reward.xp()+" XP, "+reward.coins()+" moedas, "+item.name()+" x"+reward.quantity()+"\nEntrega conforme as condições da missão.";
    }
    private void refresh(){var v=runtime.snapshot();
        if(!lastMissions.equals(v.journal())){int index=selection.getSelectedIndex();missions.clear();v.journal().forEach(missions::addElement);lastMissions=v.journal();if(!missions.isEmpty())selection.setSelectedIndex(Math.max(0,Math.min(index,missions.size()-1)));}
        navigation.repaint();
    }
}
