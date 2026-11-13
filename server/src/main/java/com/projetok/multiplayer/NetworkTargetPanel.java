package com.projetok.multiplayer;

import java.awt.*;
import javax.swing.*;

/** Alvo de combate selecionado, sem prever dano ou alcance autorizado pelo servidor. */
final class NetworkTargetPanel extends JPanel {
    private final JLabel name=new JLabel("Sem alvo"),info=new JLabel("TAB seleciona / troca inimigo");
    private final JProgressBar health=new JProgressBar();
    private final JButton clear=new JButton("×");
    private final JLabel skillHint=new JLabel();
    /** Avaliação local da habilidade inspecionada, sem declarar aceite ou rejeição do servidor. */
    void skillHint(String value){skillHint.setText("<html>"+value.replace("&","&amp;").replace("<","&lt;").replace(">","&gt;")+"</html>");skillHint.setToolTipText(value);skillHint.setVisible(!value.isEmpty());}
    NetworkTargetPanel(Runnable clearTarget){super(new BorderLayout(8,4));setName("hudTarget");setOpaque(false);setBorder(BorderFactory.createEmptyBorder(10,12,10,12));
        var heading=new JPanel(new BorderLayout(6,0));heading.add(name);heading.add(clear,BorderLayout.EAST);clear.setName("hudClearTarget");clear.getAccessibleContext().setAccessibleName("Limpar alvo de combate");clear.setToolTipText("Limpar alvo; o ataque básico continua disponível");clear.addActionListener(e->clearTarget.run());
        add(heading,BorderLayout.NORTH);health.setStringPainted(true);add(health);var descriptions=new JPanel(new GridLayout(0,1,0,3));descriptions.add(info);descriptions.add(skillHint);skillHint.setVisible(false);add(descriptions,BorderLayout.SOUTH);GameHudStyle.skin(this);name.setForeground(GameHudStyle.CYAN);clear.setBorder(BorderFactory.createEmptyBorder(0,6,0,6));
    }
    void refresh(Protocol.State snapshot,int self,int target,int range){var selected=snapshot==null?null:snapshot.actors().stream().filter(a->a.id()==target&&(NetworkSkillPreview.mob(a)&&a.hp()>0||NetworkSkillPreview.ally(a))).findFirst().orElse(null);var player=snapshot==null?null:snapshot.actors().stream().filter(a->a.id()==self).findFirst().orElse(null);
        name.setText(selected==null?"Sem alvo":selected.name());name.setToolTipText(name.getText());clear.setEnabled(selected!=null);health.setVisible(selected!=null);health.setMaximum(selected==null?1:selected.maxHp());health.setValue(selected==null?0:selected.hp());health.setString(selected==null?"":selected.hp()+" / "+selected.maxHp());
        double distance=selected==null||player==null?0:Math.hypot(selected.x()-player.x(),selected.y()-player.y());String status=selected==null?"TAB seleciona / troca alvo":target<=1_000_000?(selected.hp()==0?"Aliado derrotado · Reviver":"Aliado · Cura e limpeza")+" · "+Math.round(distance)+" un.":Math.round(distance)+" un. · Ataque "+range+" · Shunpo "+com.projetok.rpg.RpgWorld.SHUNPO_TARGET_RANGE;
        info.setText(status);info.setToolTipText(selected==null?status+". Shunpo sem alvo avança na direção atual.":status+". Alcance e obstáculos são validados pelo servidor.");getAccessibleContext().setAccessibleDescription(name.getText()+"; "+health.getString()+"; "+status);
        name.setForeground(selected!=null&&NetworkSkillPreview.ally(selected)?selected.hp()==0?new Color(255,196,104):new Color(130,240,183):GameHudStyle.CYAN);
    }
    @Override protected void paintComponent(Graphics original){var g=GameHudStyle.graphics(original);try{GameHudStyle.panel(g,getWidth(),getHeight(),12);}finally{g.dispose();}}
}
