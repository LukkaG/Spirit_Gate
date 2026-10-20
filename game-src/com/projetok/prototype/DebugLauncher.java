package com.projetok.prototype;

import javax.swing.AbstractAction;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.HashSet;
import java.util.Set;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import com.projetok.domain.skill.SkillCastCommand;
import com.projetok.domain.skill.CryostasisService;
import com.projetok.domain.skill.LightningStepService;

/**
 * Executável do protótipo visual. Não substitui KServer.main nem o login real.
 * Execução: mvn -q exec:java -Dexec.mainClass=com.projetok.prototype.DebugLauncher
 */
public final class DebugLauncher {
    private DebugLauncher() { }

    public static void main(String[] args) {
        if (args.length == 0) SwingUtilities.invokeLater(DebugLauncher::open);
        else if (args.length == 2 && "--map".equals(args[0])) {
            try {
                LocalArenaMap map = LocalArenaMap.load(java.nio.file.Path.of(args[1]));
                SwingUtilities.invokeLater(() -> open(map));
            } catch (java.io.IOException error) { throw new IllegalArgumentException("Nao foi possivel carregar mapa local", error); }
        } else throw new IllegalArgumentException("Uso: DebugLauncher [--map arquivo.properties]");
    }

    /** Cria a interface no EDT e inicia a simulação em uma thread separada. */
    private static void open() {
        open(null);
    }

    /** Variante explicita de teste: R reutiliza os mesmos dados de mapa validados. */
    private static void open(LocalArenaMap map) {
        DebugRuntime runtime = map == null ? new DebugRuntime() : new DebugRuntime(
                () -> DebugWorld.forMap(map, java.util.random.RandomGenerator.getDefault()));
        DebugPanel panel = new DebugPanel(runtime::snapshot);
        panel.setLocalMap(map);
        panel.setPresentation(runtime::presentation, ValidatedSpriteAtlas.loadOptional(defaultVisualManifest()));
        JFrame frame = new JFrame("Spirit Gate | Arena de Debug 2D");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setContentPane(panel);
        frame.setResizable(true);
        frame.pack();
        frame.setLocationRelativeTo(null);
        Set<String> held = new HashSet<>();
        Runnable releaseInput = configureKeys(panel, runtime, held);
        frame.addWindowListener(new WindowAdapter() {
            @Override public void windowClosed(WindowEvent e) {
                runtime.close();
            }
            @Override public void windowDeactivated(WindowEvent e) {
                releaseInput.run();
            }
        });
        frame.addWindowFocusListener(new WindowAdapter() {
            @Override public void windowLostFocus(WindowEvent e) { releaseInput.run(); }
        });
        runtime.start();
        Timer paintTimer = new Timer(16, e -> panel.repaint());
        paintTimer.start();
        frame.addWindowListener(new WindowAdapter() {
            @Override public void windowClosed(WindowEvent e) { paintTimer.stop(); }
        });
        frame.setVisible(true);
    }

    // --- Atalhos: Key Bindings funcionam melhor que KeyListener ao trocar foco ---

    /** Pacote portatil resolve a arte junto ao EXE, independente do diretorio corrente. */
    static java.nio.file.Path defaultVisualManifest() {
        String launcher = System.getProperty("jpackage.app-path");
        java.nio.file.Path base = launcher == null ? java.nio.file.Path.of("") :
                java.nio.file.Path.of(launcher).toAbsolutePath().getParent();
        return base.resolve("visual/character.properties");
    }

    /** Liga teclas ao estado de entrada; acessível aos testes sem criar JFrame. */
    static Runnable configureKeys(DebugPanel panel, DebugRuntime runtime, Set<String> held) {
        InputMap inputs = panel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap actions = panel.getActionMap();
        float[] aim = {Float.NaN, Float.NaN};
        panel.addMouseMotionListener(new MouseAdapter() {
            @Override public void mouseMoved(MouseEvent event) { aim[0] = event.getX(); aim[1] = event.getY(); }
            @Override public void mouseDragged(MouseEvent event) { mouseMoved(event); }
        });
        panel.addMouseListener(new MouseAdapter() {
            @Override public void mouseExited(MouseEvent event) { aim[0] = Float.NaN; aim[1] = Float.NaN; }
        });
        bindKey(inputs, "Q", "press_Q", "release_Q");
        actions.put("press_Q", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent event) {
                if (held.add("Q")) {
                    var target=panel.aimAt(aim[0],aim[1]);
                    runtime.cast(new SkillCastCommand(CryostasisService.ID, (float)target.x, (float)target.y));
                }
            }
        });
        actions.put("release_Q", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent event) { held.remove("Q"); }
        });
        bindKey(inputs, "SHIFT", "press_SHIFT", "release_SHIFT");
        actions.put("press_SHIFT", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent event) {
                if (held.add("SHIFT")) {
                    var target=panel.aimAt(aim[0],aim[1]);
                    runtime.cast(new SkillCastCommand(LightningStepService.ID, (float)target.x, (float)target.y));
                }
            }
        });
        actions.put("release_SHIFT", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent event) { held.remove("SHIFT"); }
        });
        for (String key : new String[] {"W", "A", "S", "D", "SPACE", "E"}) {
            bindKey(inputs, key, "press_" + key, "release_" + key);
            actions.put("press_" + key, new AbstractAction() {
                @Override public void actionPerformed(ActionEvent e) {
                    held.add(key);
                    sendInput(runtime, held);
                }
            });
            actions.put("release_" + key, new AbstractAction() {
                @Override public void actionPerformed(ActionEvent e) {
                    held.remove(key);
                    sendInput(runtime, held);
                }
            });
        }
        bindKey(inputs, "H", "hitboxes", null);
        bindKey(inputs, "F1", "help", null);
        bindKey(inputs, "F2", "diagnostic", null);
        actions.put("diagnostic", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                var chooser=new javax.swing.JFileChooser();
                chooser.setDialogTitle("Salvar diagnostico local - revise o JSON antes de compartilhar");
                chooser.setSelectedFile(new java.io.File("SpiritGate-diagnostico.zip"));
                if(chooser.showSaveDialog(panel)!=javax.swing.JFileChooser.APPROVE_OPTION)return;
                try {
                    DiagnosticReport.export(chooser.getSelectedFile().toPath(),runtime.snapshot(),
                            panel.getWidth(),panel.getHeight(),panel.hasLocalMap(),DiagnosticReport.Stage.ARENA,null);
                    javax.swing.JOptionPane.showMessageDialog(panel,"ZIP local criado. Revise diagnostico.json antes de compartilhar.");
                } catch(java.io.IOException | IllegalArgumentException error) {
                    javax.swing.JOptionPane.showMessageDialog(panel,"Nao foi possivel criar o ZIP. Escolha um nome novo em pasta gravavel.");
                }
            }
        });
        actions.put("help", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { panel.toggleHelp(); }
        });
        actions.put("hitboxes", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) {
                panel.setShowHitboxes(!panel.isShowHitboxes());
            }
        });
        bindKey(inputs, "R", "restart", null);
        actions.put("restart", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { runtime.restart(); }
        });
        return () -> {
            held.clear();aim[0]=Float.NaN;aim[1]=Float.NaN;
            runtime.setInput(DebugInput.NONE);
        };
    }

    /** Preserva os controles quando Shift aparece como modificador no evento Swing. */
    private static void bindKey(InputMap inputs, String key, String press, String release) {
        for (String prefix : new String[] {"", "shift "}) {
            inputs.put(KeyStroke.getKeyStroke(prefix + "pressed " + key), press);
            if (release != null) inputs.put(KeyStroke.getKeyStroke(prefix + "released " + key), release);
        }
    }

    private static void sendInput(DebugRuntime runtime, Set<String> held) {
        runtime.setInput(new DebugInput(held.contains("W"), held.contains("S"),
                held.contains("A"), held.contains("D"), held.contains("SPACE"), held.contains("E")));
    }
}
