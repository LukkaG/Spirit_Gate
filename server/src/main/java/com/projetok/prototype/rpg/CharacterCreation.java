package com.projetok.prototype.rpg;

import com.projetok.rpg.*;
import javax.swing.*;
import java.awt.*;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

/** Cria uma alma aprendiz: a raça é escolhida agora; o domínio, no nível 15. */
final class CharacterCreation extends JPanel {
    final JTextField name = new JTextField(20);
    final Map<String, JRadioButton> races = new LinkedHashMap<>();
    final JComboBox<String> appearance = new JComboBox<>(new String[]{"azure", "amber"});
    final JButton create = new CharacterSelection.SpiritButton("Criar personagem", true);
    final JButton back = new CharacterSelection.SpiritButton("Voltar", false);
    final JButton techniques = new CharacterSelection.SpiritButton("Conhecer os caminhos", false);
    final JLabel error = CharacterSelection.label(" ", 12);
    final JLabel attributes = CharacterSelection.label(" ", 13);
    final JLabel origin = CharacterSelection.label(" ", 12);
    final CharacterSelection.Preview preview = new CharacterSelection.Preview();

    CharacterCreation(RpgPlayerSprites sprites) {
        super(new BorderLayout(12, 12));
        setBackground(CharacterSelection.INK);
        setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
        JPanel heading = new JPanel(new GridLayout(0, 1, 0, 3)); heading.setOpaque(false);
        heading.add(CharacterSelection.label("SPIRIT GATE | DESPERTAR", 22));
        heading.add(CharacterSelection.label("Uma pequena alma no início de seu caminho. Escolha sua raça.", 12));
        add(heading, BorderLayout.NORTH);
        name.setText(System.getProperty("spiritgate.profile.name", ""));
        name.setBackground(new Color(12, 35, 51)); name.setForeground(Color.WHITE);
        name.setCaretColor(Color.WHITE); name.getAccessibleContext().setAccessibleName("Nome do personagem");
        JPanel fieldsColumn = new JPanel(); fieldsColumn.setOpaque(false);
        fieldsColumn.setLayout(new BoxLayout(fieldsColumn, BoxLayout.Y_AXIS));
        fieldsColumn.add(CharacterSelection.label("Nome (3–24 caracteres)", 13)); fieldsColumn.add(name);
        fieldsColumn.add(Box.createVerticalStrut(8));
        ButtonGroup options = new ButtonGroup();
        for (var race : CharacterRaces.all()) {
            JRadioButton option = new JRadioButton(race.name(), races.isEmpty());
            option.setOpaque(false); option.setForeground(Color.WHITE);
            option.getAccessibleContext().setAccessibleName("Raça " + race.name());
            option.setToolTipText(race.description());
            races.put(race.id(), option); options.add(option); fieldsColumn.add(option);
            option.addActionListener(e -> updatePreview());
        }
        fieldsColumn.add(origin); fieldsColumn.add(techniques);
        fieldsColumn.add(Box.createVerticalStrut(8));
        fieldsColumn.add(CharacterSelection.label("Aparência", 13)); fieldsColumn.add(appearance);
        fieldsColumn.add(attributes); fieldsColumn.add(preview.orientation());
        for (Component field : fieldsColumn.getComponents()) {
            if (field instanceof JComponent c) {
                c.setAlignmentX(Component.LEFT_ALIGNMENT);
                c.setMaximumSize(new Dimension(Integer.MAX_VALUE, c.getPreferredSize().height));
            }
        }
        appearance.addActionListener(e -> updatePreview());
        techniques.addActionListener(e -> DomainPreview.open(SwingUtilities.getWindowAncestor(this), selectedRace(), 1, "none", null));
        JPanel center = new JPanel(new GridLayout(1, 2, 12, 0)); center.setOpaque(false);
        JScrollPane fields = new JScrollPane(fieldsColumn); fields.setBorder(BorderFactory.createEmptyBorder());
        fields.getViewport().setBackground(CharacterSelection.INK);
        fields.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        fields.getVerticalScrollBar().setUnitIncrement(20);
        center.add(fields); preview.sprites = sprites; center.add(preview); add(center);
        JPanel footer = new JPanel(new BorderLayout(4, 8)); footer.setOpaque(false);
        JPanel buttons = new JPanel(new GridLayout(1, 2, 8, 0)); buttons.setOpaque(false);
        buttons.add(create); buttons.add(back); footer.add(buttons); footer.add(error, BorderLayout.SOUTH);
        add(footer, BorderLayout.SOUTH); updatePreview();
    }

    String selectedRace() {
        return races.entrySet().stream().filter(e -> e.getValue().isSelected()).map(Map.Entry::getKey).findFirst().orElseThrow();
    }

    /** Consulta um mundo temporário, sem escrever saves ou alterar a sessão ativa. */
    void updatePreview() {
        races.forEach((id, option) -> {
            option.setOpaque(true); option.setBackground(option.isSelected() ? new Color(18, 66, 85) : CharacterSelection.INK);
            option.setBorderPainted(true);
            option.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(option.isSelected() ? CharacterSelection.CYAN : new Color(40, 103, 125), option.isSelected() ? 2 : 1), BorderFactory.createEmptyBorder(5, 6, 5, 6)));
            option.setMaximumSize(new Dimension(Integer.MAX_VALUE, option.getPreferredSize().height));
        });
        var race = CharacterRaces.get(selectedRace());
        origin.setText("<html>Aprendiz • " + race.startingAttackName() + "<br>Sem domínio • Escolha no nível 15</html>");
        origin.setToolTipText(race.description());
        origin.setMaximumSize(new Dimension(Integer.MAX_VALUE, origin.getPreferredSize().height));
        var identity = CharacterIdentity.create("Prévia", race.id(), (String) appearance.getSelectedItem());
        var state = new RpgWorld(identity).save();
        preview.slot = new CharacterSaves.Slot(Path.of("preview.properties"), state, "", false);
        var view = RpgWorld.restore(state).snapshot();
        var combat = RpgCombatStatus.from(view);
        attributes.setText("<html>Nível " + view.level() + "<br>HP " + combat.hp() + " / " + combat.maxHp()
                + "<br>Vigor " + combat.stamina() / 10 + " / 100<br>Ataque " + view.attack() + " | Defesa " + view.defense() + "</html>");
        attributes.setMaximumSize(new Dimension(Integer.MAX_VALUE, attributes.getPreferredSize().height));
        attributes.revalidate(); preview.repaint();
    }

    void submit(Consumer<CharacterIdentity> accepted) {
        try {
            var identity = CharacterIdentity.create(name.getText(), selectedRace(), (String) appearance.getSelectedItem());
            error.setText(" "); accepted.accept(identity);
        } catch (IllegalArgumentException failure) {
            error.setText(failure.getMessage()); error.setToolTipText(failure.getMessage()); name.requestFocusInWindow();
        }
    }

    static void open(JFrame owner, RpgPlayerSprites sprites, Consumer<CharacterIdentity> accepted) {
        JDialog dialog = new JDialog(owner, "Criar personagem local", true);
        CharacterCreation form = new CharacterCreation(sprites);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE); dialog.setContentPane(form);
        form.create.addActionListener(e -> form.submit(identity -> { dialog.dispose(); accepted.accept(identity); }));
        form.back.addActionListener(e -> dialog.dispose());
        dialog.getRootPane().setDefaultButton(form.create);
        dialog.getRootPane().registerKeyboardAction(e -> dialog.dispose(), KeyStroke.getKeyStroke("ESCAPE"), JComponent.WHEN_IN_FOCUSED_WINDOW);
        dialog.setSize(Math.min(820, owner.getWidth()), Math.min(620, owner.getHeight()));
        dialog.setLocationRelativeTo(owner); SwingUtilities.invokeLater(form.name::requestFocusInWindow); dialog.setVisible(true);
    }
}
