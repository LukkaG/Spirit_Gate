package com.projetok.multiplayer;

import com.projetok.rpg.Domains;
import java.awt.*;
import javax.swing.*;

/** Seleção de ações existentes, custos e recargas confirmados na ficha V6. */
public final class NetworkHotbar extends JPanel {
    private final JComboBox<String> selected=new JComboBox<>(new String[]{"Ataque racial", "Shunpo ofensivo", "Shunpo defensivo", "Poção"});
    private final JComboBox<String> domains=new JComboBox<>();
    private final JLabel status=new JLabel("Aguardando habilidades");
    private final JButton choose=new JButton("Escolher domínio (nível 15)");
    private final LocalClient client;
    private final NetworkSkillControls domainSkills;
    private GameHudStyle.Button[] skillSlots;
    private JComponent[] reserveSlots;
    private String[] keyNames={"SPACE","SHIFT","Q","1"};
    private float energy=1000;
    private boolean alive=true;
    public void resources(Protocol.State snapshot,int self){var actor=snapshot==null?null:snapshot.actors().stream().filter(a->a.id()==self).findFirst().orElse(null);alive=actor!=null&&actor.hp()>0;var energyView=client.energyView();var profile=energyView.state();energy=actor==null||energyView.pending()?0:profile==null?actor.stamina():profile.currentFor(snapshot,self);}
    public void shortcuts(java.util.Map<String,Integer> keys){keyNames=new String[]{java.awt.event.KeyEvent.getKeyText(keys.get("SPACE")),java.awt.event.KeyEvent.getKeyText(keys.get("SHIFT")),java.awt.event.KeyEvent.getKeyText(keys.get("Q")),java.awt.event.KeyEvent.getKeyText(keys.get("hotbar-use"))};
        if(domainSkills!=null)domainSkills.shortcuts(keys);
        if(skillSlots!=null)for(int i=0;i<4;i++)skillSlots[i].setToolTipText(selected.getItemAt(i)+" — "+keyNames[i]);}
    /** Controles de técnicas existem somente na aplicação que negociou esta extensão. */
    NetworkSkillControls domainSkills(){return domainSkills;}
    public void activateSkill(int slot){if(domainSkills!=null)domainSkills.activate(slot);}
    /** Quatro ações já existentes em slots; domínio e seleção antiga continuam no menu. */
    public JComponent overlayStyle(Runnable returnFocus) {
        removeAll();setOpaque(false);setLayout(null);setName("hudActionBlock");setBorder(BorderFactory.createEmptyBorder(2,4,2,4));
        skillSlots=new GameHudStyle.Button[4];
        reserveSlots=new JComponent[4];
        for(int i=0;i<4;i++){
            var reserve=new JPanel(){@Override protected void paintComponent(Graphics original){var g=GameHudStyle.graphics(original);try{
                g.setColor(new Color(125,181,200,120));g.fillOval(getWidth()/2-1,getHeight()/2-1,3,3);
            }finally{g.dispose();}}};
            reserve.setName("hudReserve"+i);reserve.setOpaque(false);reserve.setFocusable(false);
            reserve.setToolTipText("Espaço reservado para expansão futura — sem ação atribuída");reserve.getAccessibleContext().setAccessibleName("Espaço reservado, sem habilidade atribuída");
            reserveSlots[i]=reserve;
        }
        if(domainSkills==null){add(reserveSlots[0]);add(reserveSlots[1]);}
        var glyphs=new GameHudStyle.Glyph[]{GameHudStyle.Glyph.SWORD,GameHudStyle.Glyph.ADVANCE,GameHudStyle.Glyph.SHIELD,GameHudStyle.Glyph.POTION};
        String[] names={"Ataque racial — SPACE","Shunpo ofensivo — SHIFT — nível 20","Shunpo defensivo — Q — nível 20","Poção — selecionar e usar com 1"};
        for(int i=0;i<4;i++){final int index=i;skillSlots[i]=new GameHudStyle.Button("hudSkill"+i,names[i],glyphs[i],()->{selected.setSelectedIndex(index);activate();returnFocus.run();});add(skillSlots[i]);}
        if(domainSkills==null){add(reserveSlots[2]);add(reserveSlots[3]);}
        else for(var button:domainSkills.buttons())add(button);
        var details=new JPanel(new BorderLayout(8,10));
        var rows=new JPanel(new GridLayout(0,1,4,6));String[] summaries={"Ataque básico livre ou dirigido ao alvo selecionado.","Com alvo: alcance 240. Sem alvo: avanço 70. Custo: 25 energia.","Recua 70 na direção oposta. Compartilha recarga de 3s.","Recupera 50 HP. Usa uma poção confirmada no inventário."};
        for(int i=0;i<4;i++){final int index=i;var row=new JPanel(new BorderLayout(8,2));var icon=new GameHudStyle.Button("skillInfo"+i,"Selecionar "+selected.getItemAt(i),glyphs[i],()->selected.setSelectedIndex(index));icon.setPreferredSize(new Dimension(36,36));row.add(icon,BorderLayout.WEST);var labels=new JPanel(new GridLayout(0,1));labels.add(new JLabel(selected.getItemAt(i)));labels.add(new JLabel("<html>"+summaries[i]+"</html>"));row.add(labels);rows.add(row);}
        if(domainSkills==null)details.add(rows);
        else {var contents=new JPanel();contents.setLayout(new BoxLayout(contents,BoxLayout.Y_AXIS));contents.add(rows);contents.add(Box.createVerticalStrut(14));contents.add(domainSkills.details());details.add(new JScrollPane(contents));}
        var footer=new JPanel();footer.setLayout(new BoxLayout(footer,BoxLayout.Y_AXIS));var action=new JPanel(new BorderLayout(6,0));action.add(selected);var activate=new JButton("Ativar");activate.addActionListener(e->activate());action.add(activate,BorderLayout.EAST);footer.add(action);
        var domain=new JPanel(new BorderLayout(6,0));domain.add(domains);domain.add(choose,BorderLayout.EAST);footer.add(Box.createVerticalStrut(8));footer.add(new JLabel("Domínio: nível 15 · Shunpo: nível 20"));footer.add(domain);footer.add(status);details.add(footer,BorderLayout.SOUTH);
        selected.addActionListener(e->repaint());return details;
    }
    @Override public void doLayout(){
        if(skillSlots==null){super.doLayout();return;}
        if(domainSkills!=null){int gap=2,size=Math.max(24,Math.min(32,(getWidth()-8*gap)/9)),x=(getWidth()-9*size-8*gap)/2;
            for(var component:getComponents()){component.setBounds(x,(getHeight()-size)/2,size,size);x+=size+gap;}return;}
        int gap=4,skillSize=34,reserveSize=20,total=4*skillSize+4*reserveSize+7*gap,x=(getWidth()-total)/2;
        for(var component:getComponents()){int size=component instanceof GameHudStyle.Button?skillSize:reserveSize;component.setBounds(x,(getHeight()-size)/2,size,size);x+=size+gap;}
    }
    @Override protected void paintComponent(Graphics original){
        if(skillSlots==null){super.paintComponent(original);return;}
    }
    public NetworkHotbar(LocalClient client) {
        super(new FlowLayout(FlowLayout.LEFT,6,3));this.client=client;domainSkills=client.skillApplication()?new NetworkSkillControls(client):null;
        add(selected);var use=new JButton("Usar (1)");use.addActionListener(e->activate());add(use);
        var domainNames=new java.util.LinkedHashMap<String,String>();if(client.characterSelection()!=null)Domains.forRace(client.characterSelection().race()).forEach(d->{domains.addItem(d.id());domainNames.put(d.id(),d.name());});domains.putClientProperty("spirit.labels",domainNames);
        add(domains);choose.addActionListener(e->{if(domains.getSelectedItem()!=null)client.gameplay(GameplayProtocol.Operation.CHOOSE_DOMAIN,(String)domains.getSelectedItem());});add(choose);add(status);
        setToolTipText(domainSkills==null?"Ataque: SPACE | Shunpo ofensivo: SHIFT | defensivo: Q":"SPACE: ataque | SHIFT/Q: Shunpo | F5–F9: técnicas do domínio");
    }
    public void activate() {
        switch(selected.getSelectedIndex()) {
            case 0->client.send(Protocol.Action.ATTACK,0,0);
            case 1->client.send(Protocol.Action.LIGHTNING_STEP,0,0);
            case 2->client.send(Protocol.Action.CRYOSTASIS,0,0);
            case 3->client.gameplay(GameplayProtocol.Operation.USE,"potion");
        }
    }
    public void refresh(GameplayProtocol.Sheet sheet) {
        if(domainSkills!=null)domainSkills.refresh(sheet,energy,alive);
        if(sheet==null){status.setText("Habilidades aguardando servidor");choose.setEnabled(false);if(skillSlots!=null)for(var slot:skillSlots){slot.setEnabled(false);slot.visual(false,"—",0);}return;}
        var a=sheet.abilities();var p=sheet.progress();
        status.setText("Ataque "+seconds(a.attackTicks())+" | Shunpo "+(p.level()<20?"nível 20":seconds(a.shunpoTicks()))+" | Poção "+seconds(a.potionTicks()));
        choose.setEnabled(p.level()>=15&&p.domain().equals("none"));
        status.setToolTipText("Shunpo custa 25 energia, recarga compartilhada de 3s. Alcance básico: "+a.basicRange()+". Domínio: "+p.domain());
        if(skillSlots!=null){
            int agility=client.attributes()==null?0:client.attributes().agility();int basicTicks=(int)Math.ceil(com.projetok.domain.combat.AttributeCombatRules.basicInterval(com.projetok.rpg.RpgWorld.BASIC_ATTACK_TICKS,agility));
            int[] ticks={a.attackTicks(),a.shunpoTicks(),a.shunpoTicks(),a.potionTicks()};int[] totals={basicTicks,90,90,60};String[] keys=keyNames;
            for(int i=0;i<4;i++){boolean step=i==1||i==2,locked=step&&p.level()<20,low=step&&energy<250;int potions=sheet.inventory().entrySet().stream().filter(e->e.getKey().endsWith(":potion")).mapToInt(java.util.Map.Entry::getValue).sum();
                var snapshot=client.snapshot();int selectedTarget=client.selectedTarget();var target=snapshot==null?null:snapshot.actors().stream().filter(actor->actor.id()==selectedTarget&&actor.hp()>0).findFirst().orElse(null);var self=snapshot==null?null:snapshot.actors().stream().filter(actor->actor.id()==client.id()).findFirst().orElse(null);
                boolean outOfRange=i==1&&target!=null&&self!=null&&Math.hypot(target.x()-self.x(),target.y()-self.y())>com.projetok.rpg.RpgWorld.SHUNPO_TARGET_RANGE;
                boolean friendly=selectedTarget>0&&selectedTarget<=1_000_000&&(i==0||i==1);
                boolean empty=i==3&&potions==0;skillSlots[i].setEnabled(!locked&&!low&&!empty&&!outOfRange&&!friendly&&ticks[i]==0&&client.connected()&&alive);
                String availability=friendly?"Alvo aliado: selecione um inimigo":locked?"Requer nível 20":low?"Requer 25 energia":outOfRange?"Alvo fora do alcance de "+com.projetok.rpg.RpgWorld.SHUNPO_TARGET_RANGE:empty?"Sem poções":ticks[i]>0?"Recarga "+seconds(ticks[i]):i==1&&target==null?"Avança "+com.projetok.rpg.RpgWorld.SHUNPO_FORWARD_DISTANCE+" na direção atual":"Pronto";
                String tip=selected.getItemAt(i)+" — "+keys[i]+" — "+availability+(i==3?" — "+potions+" poções":"");skillSlots[i].setToolTipText(tip);skillSlots[i].getAccessibleContext().setAccessibleName(tip);
                String key=keys[i].equalsIgnoreCase("SPACE")?"SPC":keys[i].equalsIgnoreCase("SHIFT")?"SHF":keys[i];
                skillSlots[i].visual(selected.getSelectedIndex()==i,locked?"NV20":low?"25 EN":outOfRange?"LONGE":empty?"×0":ticks[i]>0?seconds(ticks[i]):key,ticks[i]/(float)totals[i]);}
        }
    }
    private static String seconds(int ticks){return ticks==0?"pronto":String.format(java.util.Locale.ROOT,"%.1fs",ticks/30.0);}
}
