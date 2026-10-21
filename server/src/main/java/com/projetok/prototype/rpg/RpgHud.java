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
    private String lastNpc="";
    private TradeWindow tradeWindow;
    private final RpgRuntime runtime;
    private final JProgressBar hp=new JProgressBar(),stamina=new JProgressBar(0,1000);
    private final JProgressBar boss=new JProgressBar();
    private final JLabel combat=new JLabel();
    private long previousXp=-1,xpUntil;
    private String xpNotice="";
    /** HUD compartilhado com a sessao isolada de autoria. */
    public RpgHud(RpgRuntime runtime) {
        super(new BorderLayout());this.runtime=runtime;
        JPanel bars=new JPanel();bars.setLayout(new BoxLayout(bars,BoxLayout.Y_AXIS));hp.setStringPainted(true);stamina.setStringPainted(true);
        for(JProgressBar bar:new JProgressBar[]{hp,stamina,boss}){bar.setPreferredSize(new Dimension(200,18));bar.setMaximumSize(new Dimension(Integer.MAX_VALUE,18));bar.setAlignmentX(LEFT_ALIGNMENT);}
        bars.add(hp);bars.add(stamina);boss.setStringPainted(true);boss.setVisible(false);bars.add(boss);bars.add(combat);bars.add(status);add(bars,BorderLayout.NORTH);
        add(dialogue,BorderLayout.CENTER);journal.setEditable(false);journal.setLineWrap(true);journal.setWrapStyleWord(true);add(new JScrollPane(journal),BorderLayout.SOUTH);
        setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(SpiritTheme.ACCENT),BorderFactory.createEmptyBorder(4,6,4,6)));
        SpiritTheme.apply(this);hp.getAccessibleContext().setAccessibleName("Vida");stamina.getAccessibleContext().setAccessibleName("Vigor");boss.getAccessibleContext().setAccessibleName("Vida do chefe");
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
        combat.setToolTipText(combat.getText());
        journal.setText(view.journal().stream().map(q->q.state()==QuestService.State.UNAVAILABLE?"Missão indisponível":q.name()+" | "+q.state()+" | "+q.progress()+"/"+q.required()+" - "+q.objective()).collect(java.util.stream.Collectors.joining("\n")));
        status.setText("<html>"+view.identity().name()+" | Nível "+view.level()+" | XP "+view.state().xp()+" | Moedas "+view.state().coins()+"<br>Ataque "+view.attack()+" | Defesa "+view.defense()+" | "+view.state().region()+" | Atalhos na barra superior</html>");
        if(!java.util.Objects.equals(last,view.dialogue())||!lastNpc.equals(view.activeNpc())) {
            last=view.dialogue();lastNpc=view.activeNpc();dialogue.removeAll();
            journal.getParent().getParent().setVisible(last==null);
            if(last!=null) {
                String npc=view.npcs().stream().filter(n->n.id().equals(view.activeNpc())).map(NpcService.View::name).findFirst().orElse(view.activeNpc());
                dialogue.add(new JLabel(npc+" — Escolha uma resposta | Escape: fechar"),BorderLayout.NORTH);
                JTextArea words=new JTextArea(last.text(),2,40);words.setLineWrap(true);words.setWrapStyleWord(true);words.setEditable(false);words.setCaretPosition(0);dialogue.add(new JScrollPane(words),BorderLayout.CENTER);
                JPanel choices=new JPanel(new GridLayout(0,3,4,4));dialogue.add(choices,BorderLayout.SOUTH);
                for(int i=0;i<last.choices().size();i++){int index=i;JButton button=new CharacterSelection.SpiritButton(last.choices().get(i).text(),false);button.setMnemonic('1'+i);button.addActionListener(e->runtime.command(w->w.choose(index)));choices.add(button);}
                JButton close=new CharacterSelection.SpiritButton("Fechar",false);close.addActionListener(e->runtime.command(RpgWorld::closeDialogue));choices.add(close);
                if(view.activeNpc().equals("trader")) {
                    JButton shop=new CharacterSelection.SpiritButton("Comércio",true);shop.addActionListener(e->{var owner=SwingUtilities.getWindowAncestor(this);if(owner instanceof JFrame frame){if(tradeWindow==null)tradeWindow=new TradeWindow(frame,runtime);tradeWindow.setVisible(true);}});choices.add(shop);
                }
                SpiritTheme.apply(dialogue);
            }
            revalidate();repaint();
        }
    }
}
