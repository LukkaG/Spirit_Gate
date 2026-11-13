package com.projetok.multiplayer;

import java.awt.*;
import javax.swing.*;

/** Diário da missão multiplayer instalada; operações sempre validadas no NPC pelo servidor. */
public final class NetworkQuestPanel extends JPanel {
    private final JLabel progress=new JLabel("Converse com a Anciã na vila");
    private final DefaultListModel<Protocol.QuestProgress> model=new DefaultListModel<>();
    private final JList<Protocol.QuestProgress> list=new JList<>(model);
    private final JTextArea description=new JTextArea(4,20);
    private final JProgressBar objective=new JProgressBar();
    private final JButton talk=new JButton("Conversar (F)"),accept=new JButton("Aceitar (E)"),complete=new JButton("Entregar (C)");
    public void shortcuts(java.util.Map<String,Integer> keys){talk.setText("Conversar ("+java.awt.event.KeyEvent.getKeyText(keys.get("F"))+")");accept.setText("Aceitar ("+java.awt.event.KeyEvent.getKeyText(keys.get("E"))+")");complete.setText("Entregar ("+java.awt.event.KeyEvent.getKeyText(keys.get("C"))+")");}
    public NetworkQuestPanel(LocalClient client) {
        super(new BorderLayout(8,10));
        list.setName("questList");list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);list.putClientProperty("spirit.customRenderer",Boolean.TRUE);list.getAccessibleContext().setAccessibleName("Missões recebidas do servidor");
        list.setCellRenderer(new DefaultListCellRenderer(){@Override public Component getListCellRendererComponent(JList<?> l,Object value,int i,boolean selected,boolean focus){var label=(JLabel)super.getListCellRendererComponent(l,value,i,selected,focus);var q=(Protocol.QuestProgress)value;label.setText("<html>"+name(q.id())+"<br><small>"+state(q.state())+" · "+q.progress()+"/"+q.required()+"</small></html>");label.setBorder(BorderFactory.createEmptyBorder(12,12,12,12));label.setForeground(GameHudStyle.TEXT);label.setBackground(new Color(18,67,86,220));label.setOpaque(selected);return label;}});
        list.addListSelectionListener(e->{if(!e.getValueIsAdjusting())selection();});add(new JScrollPane(list));
        description.setName("questDescription");description.setEditable(false);description.setLineWrap(true);description.setWrapStyleWord(true);description.setFocusable(false);objective.setStringPainted(true);objective.setForeground(GameHudStyle.CYAN);objective.getAccessibleContext().setAccessibleName("Progresso confirmado da missão selecionada");
        var actions=new JPanel(new GridLayout(1,3,8,0));talk.addActionListener(e->client.request(Protocol.WorldAction.INTERACT,"elder"));actions.add(talk);
        accept.addActionListener(e->client.request(Protocol.WorldAction.ACCEPT_QUEST,"coop_slime"));actions.add(accept);
        complete.addActionListener(e->client.request(Protocol.WorldAction.COMPLETE_QUEST,"coop_slime"));actions.add(complete);
        var south=new JPanel(new BorderLayout(6,8));var details=new JPanel(new BorderLayout(4,6));details.add(description,BorderLayout.NORTH);details.add(objective);details.add(progress,BorderLayout.SOUTH);south.add(details);south.add(actions,BorderLayout.SOUTH);add(south,BorderLayout.SOUTH);selection();
        setToolTipText("Missão individual: apenas contribuições confirmadas avançam o objetivo. Recompensa: 1 poção, uma vez, na Anciã da vila.");
    }
    public void refresh(java.util.List<Protocol.QuestProgress> quests) {
        var received=quests.stream().filter(q->!q.state().equals("UNAVAILABLE")).toList();
        if(java.util.Collections.list(model.elements()).equals(received))return;
        String selected=list.getSelectedValue()==null?null:list.getSelectedValue().id();model.clear();received.forEach(model::addElement);
        for(int i=0;i<model.size();i++)if(model.get(i).id().equals(selected)){list.setSelectedIndex(i);break;}
        if(list.getSelectedIndex()<0&&!model.isEmpty())list.setSelectedIndex(0);selection();
    }
    static String name(String id){return id.equals("coop_slime")?"Encontro cooperativo":id;}
    static String state(String state){return switch(state){case "AVAILABLE"->"Disponível na Anciã";case "ACTIVE"->"Em andamento";case "READY"->"Volte à Anciã para entregar";case "COMPLETED"->"Concluída — recompensa recebida";default->"Indisponível";};}
    private void selection(){var q=list.getSelectedValue();boolean known=q!=null&&q.id().equals("coop_slime");talk.setEnabled(known);accept.setEnabled(known&&q.state().equals("AVAILABLE"));complete.setEnabled(known&&q.state().equals("READY"));
        progress.setText(q==null?"Nenhuma missão disponível nesta sessão":state(q.state()));description.setText(q==null?"Converse com os personagens do mundo para descobrir objetivos.":known?"Derrote 1 slime com contribuição confirmada. Retorne à Anciã na vila para receber 1 poção, uma única vez.":"Objetivo recebido do servidor. Detalhes adicionais não disponíveis.");objective.setMaximum(q==null?1:q.required());objective.setValue(q==null?0:q.progress());objective.setString(q==null?"Sem objetivo":q.progress()+" / "+q.required());}
}
