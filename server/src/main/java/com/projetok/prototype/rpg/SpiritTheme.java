package com.projetok.prototype.rpg;

import javax.swing.*;
import java.awt.*;

/** Paleta e acabamento comuns, sem substituir o comportamento nativo de foco. */
final class SpiritTheme {
    static final Color BACKGROUND = new Color(6,16,29);
    static final Color ACCENT = new Color(112,225,244);
    static volatile boolean reducedMotion;
    private SpiritTheme() { }

    /** Aplica contraste aos componentes existentes sem trocar seus modelos ou ações. */
    static void apply(Component component) {
        component.setBackground(BACKGROUND); component.setForeground(new Color(227,248,255));
        if(component instanceof JTextArea area)area.setCaretColor(ACCENT);
        if(component instanceof JList<?> list) {
            list.setSelectionBackground(new Color(18,66,85));list.setSelectionForeground(Color.WHITE);
        }
        if(component instanceof JProgressBar bar) {bar.setForeground(ACCENT);bar.setStringPainted(true);
            bar.setUI(new javax.swing.plaf.basic.BasicProgressBarUI(){protected Color getSelectionForeground(){return BACKGROUND;}protected Color getSelectionBackground(){return Color.WHITE;}});
        }
        if(component instanceof Container container)for(Component child:container.getComponents())apply(child);
    }

    /** Instala Escape e botão padrão em diálogos temáticos modais ou não modais. */
    static void dialog(JDialog dialog, JPanel content, JButton primary) {
        apply(content);dialog.setContentPane(content);dialog.getRootPane().setDefaultButton(primary);
        dialog.getRootPane().registerKeyboardAction(e->dialog.setVisible(false),KeyStroke.getKeyStroke("ESCAPE"),JComponent.WHEN_IN_FOCUSED_WINDOW);
    }
}
