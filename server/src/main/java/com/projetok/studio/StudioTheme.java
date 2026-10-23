package com.projetok.studio;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.*;

/** Paleta do editor; conserva componentes, ações e contratos dos documentos. */
final class StudioTheme {
    static final Color BACKGROUND=new Color(21,25,34), PANEL=new Color(30,36,48),
        FIELD=new Color(16,21,30), TEXT=new Color(225,232,240), MUTED=new Color(163,180,199),
        ACCENT=new Color(88,204,200), SELECTED=new Color(43,81,95);
    private StudioTheme() { }

    /** Aplica cores também aos diálogos Swing criados pelo editor. */
    static void dialogs() {
        for(String key:new String[]{"Panel.background","OptionPane.background","Button.background",
                "ComboBox.background","Spinner.background","FileChooser.background","Menu.background",
                "MenuItem.background","PopupMenu.background","TabbedPane.background"}) UIManager.put(key,PANEL);
        for(String key:new String[]{"Label.foreground","OptionPane.messageForeground","Button.foreground",
                "TextField.foreground","TextArea.foreground","ComboBox.foreground","Menu.foreground",
                "MenuItem.foreground","CheckBox.foreground","TabbedPane.foreground"}) UIManager.put(key,TEXT);
        UIManager.put("TextField.background",FIELD); UIManager.put("TextArea.background",FIELD);
        UIManager.put("TextField.caretForeground",ACCENT);
        UIManager.put("List.background",FIELD); UIManager.put("List.foreground",TEXT);
        UIManager.put("List.selectionBackground",SELECTED); UIManager.put("List.selectionForeground",TEXT);
    }

    /** Estiliza a árvore existente sem substituir ferramentas ou capturar seus eventos. */
    static void apply(Component c) {
        c.setForeground(TEXT); c.setBackground(c instanceof JTextField||c instanceof JTextArea?FIELD:PANEL);
        c.setFont(new Font(Font.DIALOG,Font.PLAIN,12));
        if(c instanceof javax.swing.text.JTextComponent text) text.setCaretColor(ACCENT);
        if(c instanceof AbstractButton button) {
            button.setFocusPainted(true); button.setRolloverEnabled(true);
            button.setMargin(new Insets(5,9,5,9));
            if(button instanceof JButton) button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(66,85,104)),new EmptyBorder(5,8,5,8)));
        }
        if(c instanceof JList<?> list) {list.setSelectionBackground(SELECTED);list.setSelectionForeground(TEXT);}
        if(c instanceof JComponent component && component.getBorder() instanceof TitledBorder title) title.setTitleColor(MUTED);
        if(c instanceof JSplitPane split) {split.setDividerSize(7);split.setContinuousLayout(true);split.setBorder(null);}
        if(c instanceof JScrollPane scroll) scroll.setBorder(new LineBorder(new Color(50,62,78)));
        if(c instanceof JMenu menu) apply(menu.getPopupMenu());
        if(c instanceof Container container) for(Component child:container.getComponents()) apply(child);
    }
}
