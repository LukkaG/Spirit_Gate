package com.projetok.prototype.rpg;

import com.projetok.rpg.*;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.Arc2D;
import java.util.function.Consumer;

/** Demonstrações visuais isoladas: não concedem XP, técnicas nem alteram o personagem. */
final class DomainPreview extends JPanel {
    final JComboBox<Domains.Definition> domains;
    final DefaultListModel<String> techniqueModel = new DefaultListModel<>();
    final JList<String> techniques = new JList<>(techniqueModel);
    final JLabel status = CharacterSelection.label(" ", 12);
    final JTextArea description = new JTextArea();
    final JButton choose = new CharacterSelection.SpiritButton("Escolher domínio", true);
    final JButton close = new CharacterSelection.SpiritButton("Voltar", false);
    final TechniqueScene scene = new TechniqueScene();
    private final boolean canChoose;

    DomainPreview(String race, int level, String currentDomain, boolean selectionAllowed) {
        super(new BorderLayout(10, 10)); setBackground(CharacterSelection.INK);
        setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
        canChoose = selectionAllowed && level >= DomainProgression.INITIATION_LEVEL && currentDomain.equals(DomainProgression.NONE);
        JPanel heading = new JPanel(new GridLayout(0, 1, 0, 4)); heading.setOpaque(false);
        heading.add(CharacterSelection.label("CAMINHOS | " + CharacterRaces.get(race).name(), 22));
        heading.add(CharacterSelection.label("Prévias de conceito das técnicas em desenvolvimento", 12)); add(heading, BorderLayout.NORTH);
        domains = new JComboBox<>(Domains.forRace(race).toArray(Domains.Definition[]::new));
        domains.setRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean selected, boolean focus) {
                return super.getListCellRendererComponent(list, value instanceof Domains.Definition d ? d.name() : "", index, selected, focus);
            }
        });
        domains.getAccessibleContext().setAccessibleName("Caminho para visualizar");
        JPanel content = new JPanel(new BorderLayout(8, 8)); content.setOpaque(false); content.add(domains, BorderLayout.NORTH);
        JPanel previewArea = new JPanel(new BorderLayout(8, 8)); previewArea.setOpaque(false); previewArea.add(scene);
        techniques.setBackground(new Color(8, 25, 40)); techniques.setForeground(Color.WHITE);
        techniques.setSelectionBackground(new Color(18, 66, 85)); techniques.setSelectionForeground(Color.WHITE);
        techniques.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        techniques.getAccessibleContext().setAccessibleName("Técnica para visualizar");
        techniques.setCellRenderer(new DefaultListCellRenderer() {
            @Override public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean selected, boolean focus) {
                JLabel line = (JLabel) super.getListCellRendererComponent(list, value, index, selected, focus);
                String text = value.toString(); int separator = text.indexOf(':');
                line.setText(separator < 0 ? text : text.substring(0, separator)); line.setToolTipText(text); return line;
            }
        });
        JScrollPane techniqueList = new JScrollPane(techniques); techniqueList.setPreferredSize(new Dimension(220, 72));
        previewArea.add(techniqueList, BorderLayout.SOUTH); content.add(previewArea);
        description.setEditable(false); description.setLineWrap(true); description.setWrapStyleWord(true);
        description.setOpaque(false); description.setForeground(CharacterSelection.CYAN); description.setFont(new Font("SansSerif", Font.PLAIN, 12));
        description.setRows(4);
        JScrollPane explanation = new JScrollPane(description); explanation.setBorder(BorderFactory.createEmptyBorder());
        explanation.setPreferredSize(new Dimension(400, 68)); explanation.getViewport().setBackground(CharacterSelection.INK);
        content.add(explanation, BorderLayout.SOUTH); add(content);
        JPanel footer = new JPanel(new BorderLayout(0, 8)); footer.setOpaque(false);
        JPanel buttons = new JPanel(new GridLayout(1, 2, 8, 0)); buttons.setOpaque(false);
        buttons.add(choose); buttons.add(close); footer.add(buttons, BorderLayout.NORTH);
        status.setText(canChoose ? "Escolha permanente. Outro caminho exige reset de domínio."
            : !currentDomain.equals(DomainProgression.NONE) ? "Domínio já escolhido. Para mudar, será necessário um reset."
            : "A iniciação e a escolha de domínio ficam disponíveis no nível 15.");
        footer.add(status, BorderLayout.SOUTH); add(footer, BorderLayout.SOUTH);
        choose.setEnabled(canChoose); choose.setVisible(selectionAllowed);
        domains.addActionListener(e -> updateDomain());
        techniques.addListSelectionListener(e -> updateTechnique()); updateDomain();
    }

    String selectedDomain() { return ((Domains.Definition) domains.getSelectedItem()).id(); }
    boolean canChoose() { return canChoose; }
    private void updateDomain() {
        var domain = (Domains.Definition) domains.getSelectedItem();
        scene.domain = domain.id(); techniqueModel.clear(); domain.techniquePreviews().forEach(techniqueModel::addElement);
        if (!techniqueModel.isEmpty()) techniques.setSelectedIndex(0); scene.repaint();
    }
    private void updateTechnique() {
        var domain = (Domains.Definition) domains.getSelectedItem();
        scene.technique = Math.max(0, techniques.getSelectedIndex());
        description.setText(domain.description() + "\n" + (techniques.getSelectedValue() == null ? "" : techniques.getSelectedValue()));
        scene.repaint();
    }
    static String formatXp(long halfUnits) { return halfUnits / 2 + (halfUnits % 2 == 0 ? "" : ",5"); }

    static void open(Window owner, String race, int characterLevel, String currentDomain, Consumer<String> selected) {
        JDialog dialog = new JDialog(owner, "Spirit Gate | Iniciação", Dialog.ModalityType.APPLICATION_MODAL);
        DomainPreview form = new DomainPreview(race, characterLevel, currentDomain, selected != null);
        dialog.setContentPane(form); dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        form.close.addActionListener(e -> dialog.dispose());
        form.choose.addActionListener(e -> {
            if (!form.canChoose || selected == null) return;
            try { selected.accept(form.selectedDomain()); dialog.dispose(); }
            catch (IllegalArgumentException | IllegalStateException failure) { form.status.setText(failure.getMessage()); }
        });
        dialog.getRootPane().registerKeyboardAction(e -> dialog.dispose(), KeyStroke.getKeyStroke("ESCAPE"), JComponent.WHEN_IN_FOCUSED_WINDOW);
        Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
        dialog.setSize(Math.min(780, screen.width - 40), Math.min(620, screen.height - 60));
        dialog.setLocationRelativeTo(owner); dialog.setVisible(true);
    }

    static final class TechniqueScene extends JPanel {
        String domain = "water"; int technique;
        final Timer animation = new Timer(40, e -> { if (!SpiritTheme.reducedMotion) repaint(); });
        TechniqueScene() { setPreferredSize(new Dimension(400, 220)); setMinimumSize(new Dimension(100, 80)); setBackground(new Color(5, 19, 33)); }
        @Override public void addNotify() { super.addNotify(); animation.start(); }
        @Override public void removeNotify() { animation.stop(); super.removeNotify(); }
        @Override protected void paintComponent(Graphics original) {
            super.paintComponent(original); Graphics2D g = (Graphics2D) original.create();
            try {
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                double phase = SpiritTheme.reducedMotion ? .55 : (System.nanoTime() / 1_000_000_000.0 % 2.4) / 2.4;
                g.scale(getWidth() / 600.0, getHeight() / 260.0);
                g.setColor(new Color(28, 64, 79)); for (int x = 0; x < 600; x += 40) g.drawLine(x, 0, x, 260);
                for (int y = 0; y < 260; y += 40) g.drawLine(0, y, 600, y);
                boolean closeRange = domain.equals("martial") || domain.equals("bow_short");
                int source = closeRange ? 300 : 120, target = 450, y = 145;
                actor(g, source, y, CharacterSelection.CYAN); actor(g, target, y, new Color(239, 150, 126));
                g.setStroke(new BasicStroke(5, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                int projectile = source + (int) ((target - source) * phase);
                switch (domain) {
                    case "water" -> {
                        g.setColor(new Color(81, 192, 255));
                        g.draw(new Arc2D.Double(source - 40, y - 85, 145 + phase * 170, 150, -70 + phase * 40, 230, Arc2D.OPEN));
                        if (technique == 0) g.drawOval(source - 30, y - 60, 60, 100);
                    }
                    case "thunder" -> {
                        g.setColor(new Color(255, 229, 84)); int[] xs = {source, source + 90, source + 65, projectile, projectile - 20, target};
                        int[] ys = {y - 20, y - 35, y - 10, y - 40, y - 12, y - 25}; g.drawPolyline(xs, ys, xs.length);
                    }
                    case "fire" -> {
                        for (int i = 0; i < 9; i++) {
                            int x = source + i * 36; int height = 20 + (int) (Math.sin(phase * 9 + i) * 12);
                            g.setColor(new Color(255, 100 + i * 10, 40, 210)); g.fillPolygon(new int[]{x - 13, x, x + 13}, new int[]{y + 15, y - height, y + 15}, 3);
                        }
                    }
                    case "wind" -> {
                        g.setColor(new Color(177, 248, 224)); for (int i = 0; i < 3; i++) g.drawArc(projectile - 25 - i * 18, y - 65 + i * 20, 60, 75, -60, 140);
                    }
                    case "light" -> {
                        g.setColor(new Color(255, 239, 170));
                        if (technique == 0) { g.drawLine(source + 18, y - 20, projectile, y - 20); g.fillOval(projectile - 8, y - 28, 16, 16); }
                        else if (technique == 1) { g.drawLine(source - 17, y - 95, source + 17, y - 95); g.drawLine(source, y - 112, source, y - 78); g.drawOval(source - 35, y - 60, 70, 110); }
                        else g.drawRoundRect(source + 25, y - 65, 18, 110, 12, 12);
                    }
                    case "martial" -> {
                        g.setColor(new Color(255, 209, 145)); for (int i = 0; i < 3; i++) g.fillOval(source + 35 + (int) (phase * 90), y - 40 + i * 20, 18, 18);
                    }
                    case "bow_long", "bow_short" -> {
                        g.setColor(new Color(156, 218, 255)); g.drawArc(source - 4, y - 58, 38, 74, -80, 160); g.drawLine(source + 18, y - 57, source + 18, y + 15);
                        int count = domain.equals("bow_short") ? 3 : 1;
                        for (int i = 0; i < count; i++) { int yy = y - 20 + (i - count / 2) * 22; g.drawLine(projectile - 32, yy, projectile, yy); g.drawLine(projectile - 9, yy - 6, projectile, yy); g.drawLine(projectile - 9, yy + 6, projectile, yy); }
                    }
                    case "darkness_debuff" -> {
                        g.setColor(new Color(190, 120, 247)); g.drawOval(target - 35, y - 65, 70, 100);
                        for (int i = 0; i < 4; i++) { int yy = y - 65 + i * 22; g.drawLine(target - 12, yy, target + 12, yy + 15); g.drawLine(target + 12, yy, target - 12, yy + 15); }
                    }
                    case "darkness_control" -> {
                        g.setColor(new Color(140, 95, 208, 100)); g.fillOval(target - 105, y - 20, 210, 75); g.setColor(new Color(190, 120, 247));
                        g.drawOval(target - 105, y - 20, 210, 75); for (int i = 0; i < 5; i++) g.drawLine(target - 45 + i * 22, y - 70, target - 45 + i * 22, y + 25);
                        actor(g, target + 75, y + 10, CharacterSelection.CYAN);
                    }
                    default -> { }
                }
            } finally { g.dispose(); }
        }
        private static void actor(Graphics2D g, int x, int y, Color color) {
            g.setColor(color); g.fillOval(x - 10, y - 58, 20, 20); g.fillRoundRect(x - 14, y - 35, 28, 42, 9, 9);
            g.setStroke(new BasicStroke(5)); g.drawLine(x - 7, y, x - 12, y + 25); g.drawLine(x + 7, y, x + 12, y + 25);
        }
    }
}
