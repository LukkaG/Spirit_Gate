package com.projetok.prototype.rpg;
import com.projetok.rpg.*;
import javax.swing.*;
import java.awt.*;
/** Widgets consomem snapshots; botoes apenas enfileiram intencoes. */
final class RpgHud extends JPanel {
    private final JLabel status=new JLabel();
    private final JTextArea journal=new JTextArea(3,80);
    private final JPanel dialogue=new JPanel(new BorderLayout(4,4));
    private DialogueService.Node last;
    private final RpgRuntime runtime;
    RpgHud(RpgRuntime runtime) {super(new BorderLayout());this.runtime=runtime;add(status,BorderLayout.NORTH);add(dialogue,BorderLayout.CENTER);journal.setEditable(false);add(journal,BorderLayout.SOUTH);}
    void update(RpgWorld.View view) {
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
