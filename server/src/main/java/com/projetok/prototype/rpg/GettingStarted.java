package com.projetok.prototype.rpg;

import com.projetok.rpg.RpgControls;
import javax.swing.*;
import java.awt.*;

/** Ajuda contextual não modal, sem recompensas ou estado persistido. */
final class GettingStarted {
    private GettingStarted() { }
    /** Compõe instruções a partir dos atalhos efetivamente configurados. */
    static String text(RpgControls controls) {
        StringBuilder text=new StringBuilder("BEM-VINDO AO SPIRIT GATE\n\nMovimentação: W A S D\n");
        for(String action:new String[]{"attack","ability","potion","interact","inventory","journal","map","save","revive"})
            text.append(controls.key(action)).append(" — ").append(RpgLauncher.actionLabel(action)).append('\n');
        return text.append("\nVoce comeca como aprendiz, sem dominio. Reaper usa uma espada pequena; Human e Aetheri usam socos; Spirit usa feixes de luz basicos.\n\nMobs e quests concedem experiencia. Um mob nivel 1 concede 3 XP de personagem e, apos escolher dominio, 1,5 XP de dominio.\n\nNo nivel 15, abra Dominios e tecnicas para visualizar e escolher um caminho. A escolha so muda com reset.\nShunpo ofensivo e defensivo exigem nivel 20 em todas as racas.\n\nAproxime-se dos NPCs para interagir. Escape fecha o dialogo.\nSalvar e voltar ao menu preserva o progresso antes de sair.").toString();
    }
    /** Abre ajuda dispensável; o mundo não é modificado pela leitura. */
    static void open(JFrame owner,RpgControls controls) {
        JDialog dialog=new JDialog(owner,"Primeiros passos",false);dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        JPanel root=new JPanel(new BorderLayout(8,8));root.setBorder(BorderFactory.createEmptyBorder(12,12,12,12));
        JTextArea text=new JTextArea(text(controls),15,40);text.setEditable(false);text.setLineWrap(true);text.setWrapStyleWord(true);
        root.add(new JScrollPane(text));JButton close=new CharacterSelection.SpiritButton("Voltar ao jogo",true);close.addActionListener(e->dialog.dispose());root.add(close,BorderLayout.SOUTH);
        SpiritTheme.dialog(dialog,root,close);dialog.setSize(Math.min(560,owner.getWidth()),Math.min(540,owner.getHeight()));dialog.setLocationRelativeTo(owner);dialog.setVisible(true);
        owner.addWindowListener(new java.awt.event.WindowAdapter(){public void windowClosed(java.awt.event.WindowEvent e){dialog.dispose();}});
    }
}
