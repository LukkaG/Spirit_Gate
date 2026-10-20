package com.projetok.prototype.rpg;
import com.projetok.rpg.*;
import javax.swing.*;
import java.awt.*;
/** Widgets consomem snapshots; botoes apenas enfileiram intencoes. */
public final class RpgHud extends JPanel {
    private final JLabel status=new JLabel();
    private final JTextArea journal=new JTextArea(3,80);
    private final JPanel dialogue=new JPanel(new BorderLayout(4,4));
    private DialogueService.Node last;
    private final RpgRuntime runtime;
    private final JProgressBar hp=new JProgressBar(),stamina=new JProgressBar(0,1000);
    private final JProgressBar boss=new JProgressBar();
    private final JLabel combat=new JLabel();
    private long previousXp=-1,xpUntil;
    private String xpNotice="";
    /** HUD compartilhado com a sessao isolada de autoria. */
    public RpgHud(RpgRuntime runtime) {
        super(new BorderLayout());this.runtime=runtime;
        JPanel bars=new JPanel(new GridLayout(0,1));hp.setStringPainted(true);stamina.setStringPainted(true);
        bars.add(hp);bars.add(stamina);boss.setStringPainted(true);boss.setVisible(false);bars.add(boss);bars.add(combat);bars.add(status);add(bars,BorderLayout.NORTH);
        add(dialogue,BorderLayout.CENTER);journal.setEditable(false);add(new JScrollPane(journal),BorderLayout.SOUTH);
    }
    /** Consome somente valores publicados pelo mundo real. */
    public void update(RpgWorld.View view) {
        var enemy=view.enemies().stream().filter(e->e.boss()&&e.hp()>0).findFirst();boss.setVisible(enemy.isPresent());
        enemy.ifPresent(e->{boss.setMaximum(e.maxHp());boss.setValue(e.hp());boss.setString(e.name()+" "+e.hp()+" / "+e.maxHp()+(e.hp()*2<=e.maxHp()?" | Enfurecido":"")+(e.state().equals("PREPARE")?" | Preparando":e.state().equals("RECOVER")?" | Recuperando":""));});
        var values=RpgCombatStatus.from(view);hp.setMaximum(values.maxHp());hp.setValue(values.hp());hp.setString("HP "+values.hp()+" / "+values.maxHp());
        hp.setForeground(values.lowHp()?new Color(210,85,55):new Color(65,165,100));
        stamina.setValue(values.stamina());stamina.setString("Vigor "+values.stamina()/10+" / 100");
        if(previousXp>=0&&view.state().xp()>previousXp){xpNotice=" | +"+(view.state().xp()-previousXp)+" XP";xpUntil=view.ticks()+90;}
        previousXp=view.state().xp();combat.setText(values.text()+(view.ticks()<xpUntil?xpNotice:"")+" | "+view.message());
        journal.setText(view.journal().stream().map(q->q.name()+" | "+q.state()+" | "+q.progress()+"/"+q.required()+" - "+q.objective()).collect(java.util.stream.Collectors.joining("\n")));
        status.setText("  "+view.identity().name()+" | Nivel "+view.level()+" | XP "+view.state().xp()+" | Moedas "+view.state().coins()+" | Ataque "+view.attack()+" | Defesa "+view.defense()+" | "+view.state().region()+" | Atalhos na barra superior");
        if(!java.util.Objects.equals(last,view.dialogue())) {
            last=view.dialogue();dialogue.removeAll();
            if(last!=null) {
                JTextArea words=new JTextArea(last.text(),2,40);words.setLineWrap(true);words.setWrapStyleWord(true);words.setEditable(false);dialogue.add(words,BorderLayout.NORTH);
                JPanel choices=new JPanel(new GridLayout(0,3,4,4));dialogue.add(choices,BorderLayout.CENTER);
                for(int i=0;i<last.choices().size();i++){int index=i;JButton button=new JButton(last.choices().get(i).text());button.addActionListener(e->runtime.command(w->w.choose(index)));choices.add(button);}
                JButton close=new JButton("Fechar");close.addActionListener(e->runtime.command(RpgWorld::closeDialogue));choices.add(close);
                if(view.activeNpc().equals("trader")) {
                    for(String id:new String[]{"potion","sword","robe"}){var item=new ItemCatalog(RpgContent.builtin()).get(id);JButton buy=new JButton("Comprar "+item.name()+" ("+item.price()+")");buy.addActionListener(e->runtime.command(w->w.trade(id,1,true)));choices.add(buy);}
                    JButton sell=new JButton("Vender cristal (1 moeda)");sell.addActionListener(e->runtime.command(w->w.trade("crystal",1,false)));choices.add(sell);
                }
            }
            revalidate();repaint();
        }
    }
}
