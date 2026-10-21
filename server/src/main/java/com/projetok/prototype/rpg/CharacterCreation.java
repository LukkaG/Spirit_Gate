package com.projetok.prototype.rpg;

import com.projetok.rpg.*;
import javax.swing.*;
import java.awt.*;
import java.nio.file.Path;
import java.util.function.Consumer;

/** Formulário de criação local; validação e atributos pertencem ao domínio. */
final class CharacterCreation extends JPanel {
    final JTextField name = new JTextField(20);
    final JRadioButton guardian = new JRadioButton("Guardian — Investida", true);
    final JRadioButton mystic = new JRadioButton("Mystic — Cura");
    final JComboBox<String> appearance = new JComboBox<>(new String[]{"azure", "amber"});
    final JButton create = new CharacterSelection.SpiritButton("Criar personagem", true);
    final JButton back = new CharacterSelection.SpiritButton("Voltar", false);
    final JLabel error = CharacterSelection.label(" ", 12);
    final JLabel attributes = CharacterSelection.label(" ", 13);
    final CharacterSelection.Preview preview = new CharacterSelection.Preview();

    CharacterCreation(RpgPlayerSprites sprites) {
        super(new BorderLayout(12, 12));
        setBackground(CharacterSelection.INK);
        setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
        add(CharacterSelection.label("SPIRIT GATE | DESPERTAR", 22), BorderLayout.NORTH);
        name.setText(System.getProperty("spiritgate.profile.name", ""));
        name.setBackground(new Color(12, 35, 51)); name.setForeground(Color.WHITE);
        name.setCaretColor(Color.WHITE); name.getAccessibleContext().setAccessibleName("Nome do personagem");
        JPanel form = CharacterSelection.panel(new GridBagLayout());
        JPanel fieldsColumn = new JPanel(); fieldsColumn.setOpaque(false);
        fieldsColumn.setLayout(new BoxLayout(fieldsColumn, BoxLayout.Y_AXIS));
        var constraints = new GridBagConstraints(); constraints.weightx=1; constraints.weighty=1;
        constraints.anchor=GridBagConstraints.NORTHWEST; constraints.fill=GridBagConstraints.HORIZONTAL;
        form.add(fieldsColumn,constraints);
        fieldsColumn.add(CharacterSelection.label("Nome (3–24 caracteres)", 13)); fieldsColumn.add(name);
        ButtonGroup classes = new ButtonGroup(); classes.add(guardian); classes.add(mystic);
        for (JRadioButton option : new JRadioButton[]{guardian, mystic}) {
            option.setOpaque(false); option.setForeground(Color.WHITE); fieldsColumn.add(option);
            option.addActionListener(e -> updatePreview());
        }
        fieldsColumn.add(CharacterSelection.label("Aparência geométrica", 13)); fieldsColumn.add(appearance);
        fieldsColumn.add(CharacterSelection.label("Sprite base compartilhado pelas classes", 11));
        fieldsColumn.add(attributes);
        fieldsColumn.add(preview.orientation());
        for(Component field:fieldsColumn.getComponents()) {
            if(field instanceof JComponent c) {c.setAlignmentX(Component.LEFT_ALIGNMENT);c.setMaximumSize(new Dimension(Integer.MAX_VALUE,c.getPreferredSize().height));}
        }
        appearance.addActionListener(e -> updatePreview());
        JPanel center = new JPanel(new GridLayout(1, 2, 12, 0)); center.setOpaque(false);
        JScrollPane fields = new JScrollPane(form); fields.setBorder(BorderFactory.createEmptyBorder());
        fields.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        center.add(fields); preview.sprites = sprites; center.add(preview); add(center);
        JPanel footer = new JPanel(new BorderLayout(4, 8)); footer.setOpaque(false);
        JPanel buttons = new JPanel(new GridLayout(1, 2, 8, 0)); buttons.setOpaque(false);
        buttons.add(create); buttons.add(back); footer.add(buttons); footer.add(error, BorderLayout.SOUTH);
        add(footer, BorderLayout.SOUTH); updatePreview();
    }

    /** Consulta um mundo temporário, sem escrever saves ou alterar a sessão ativa. */
    void updatePreview() {
        for(JRadioButton option:new JRadioButton[]{guardian,mystic}){
            option.setOpaque(true);option.setBackground(option.isSelected()?new Color(18,66,85):CharacterSelection.INK);
            option.setBorderPainted(true);option.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(option.isSelected()?CharacterSelection.CYAN:new Color(40,103,125),option.isSelected()?2:1),BorderFactory.createEmptyBorder(8,6,8,6)));
            var definition=Archetypes.get(option==guardian?"guardian":"mystic");
            option.setToolTipText((option==guardian?"Combate corpo a corpo; Investida":"Combate à distância; Cura")+" | Custo "+definition.cost()+" vigor | Recarga "+RpgCombatStatus.cooldown(definition.cooldown()));
            option.setMaximumSize(new Dimension(Integer.MAX_VALUE,option.getPreferredSize().height));
        }
        var identity = CharacterIdentity.create("Prévia", guardian.isSelected() ? "guardian" : "mystic", (String) appearance.getSelectedItem());
        var state = new RpgWorld(identity).save();
        preview.slot = new CharacterSaves.Slot(Path.of("preview.properties"), state, "", false);
        var view = RpgWorld.restore(state).snapshot();
        var combat = RpgCombatStatus.from(view);
        attributes.setText("<html>Nível " + view.level() + "<br>HP " + combat.hp() + " / " + combat.maxHp()
                + "<br>Vigor " + combat.stamina()/10 + " / 100<br>Ataque " + view.attack() + " | Defesa " + view.defense() + "</html>");
        attributes.setMaximumSize(new Dimension(Integer.MAX_VALUE, attributes.getPreferredSize().height));
        attributes.revalidate();
        preview.repaint();
    }

    /** Valida com o contrato real e só então entrega a identidade ao fluxo existente. */
    void submit(Consumer<CharacterIdentity> accepted) {
        try {
            var identity = CharacterIdentity.create(name.getText(), guardian.isSelected() ? "guardian" : "mystic", (String) appearance.getSelectedItem());
            error.setText(" "); accepted.accept(identity);
        } catch (IllegalArgumentException failure) {
            error.setText(failure.getMessage()); error.setToolTipText(failure.getMessage()); name.requestFocusInWindow();
        }
    }

    /** Abre diálogo modal com Enter, Escape e fechamento sem criação. */
    static void open(JFrame owner, RpgPlayerSprites sprites, Consumer<CharacterIdentity> accepted) {
        JDialog dialog = new JDialog(owner, "Criar personagem local", true);
        CharacterCreation form = new CharacterCreation(sprites);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE); dialog.setContentPane(form);
        form.create.addActionListener(e -> form.submit(identity -> {dialog.dispose(); accepted.accept(identity);}));
        form.back.addActionListener(e -> dialog.dispose());
        dialog.getRootPane().setDefaultButton(form.create);
        dialog.getRootPane().registerKeyboardAction(e -> dialog.dispose(), KeyStroke.getKeyStroke("ESCAPE"), JComponent.WHEN_IN_FOCUSED_WINDOW);
        dialog.setSize(Math.min(820, owner.getWidth()), Math.min(540, owner.getHeight()));
        dialog.setLocationRelativeTo(owner); SwingUtilities.invokeLater(form.name::requestFocusInWindow); dialog.setVisible(true);
    }
}
