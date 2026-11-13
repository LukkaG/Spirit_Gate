package com.projetok.prototype.rpg;

import com.projetok.multiplayer.LocalSimulationServer;
import com.projetok.multiplayer.RpgNetworkClientLauncher;
import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.nio.file.Path;

/** Transfere a posse do servidor local para a partida, inclusive durante reconexoes. */
final class SessionGameLifecycle {
    private final LocalSimulationServer ownedServer;
    private final Path project;
    private JFrame current;
    private boolean closed;

    SessionGameLifecycle(LocalSimulationServer ownedServer, Path project) {
        this.ownedServer = ownedServer;
        this.project = project;
    }

    void adopt(JFrame frame) {
        if (!SwingUtilities.isEventDispatchThread()) throw new IllegalStateException("Transicao exige EDT");
        if (closed) { frame.dispose(); return; }
        current = frame;
        if (ownedServer != null) {
            frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
            Object controls = frame.getRootPane().getClientProperty("networkControls");
            if (controls instanceof JPanel panel) {
                JButton studio = new JButton("Painel admin");
                studio.addActionListener(e -> {
                    var menu=new JPopupMenu();var editor=new JMenuItem("Editor de mapas");
                    editor.addActionListener(action->{
                        try { LauncherSession.openStudioInGame(ownedServer, project, frame); }
                        catch (Exception error) { JOptionPane.showMessageDialog(frame, error.getMessage()); }
                    });menu.add(editor);menu.show(studio,0,studio.getHeight());
                });
                panel.add(studio);
                JButton stop = new JButton("Encerrar partida");
                stop.addActionListener(e -> frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING)));
                panel.add(stop);
                panel.revalidate();
            }
        }
        frame.addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent event) {
                if (ownedServer == null || current != frame || closed
                        || Boolean.TRUE.equals(frame.getRootPane().getClientProperty(RpgNetworkClientLauncher.CLOSE_CONFIRMATION_PROPERTY))) return;
                // O dialogo modal continua processando done() de workers na EDT.
                frame.getRootPane().putClientProperty(RpgNetworkClientLauncher.CLOSE_CONFIRMATION_PROPERTY, Boolean.TRUE);
                try {
                    if (JOptionPane.showConfirmDialog(frame,
                            "Encerrar servidor e desconectar participanteso", "Encerrar sessao",
                            JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) frame.dispose();
                } finally {
                    frame.getRootPane().putClientProperty(RpgNetworkClientLauncher.CLOSE_CONFIRMATION_PROPERTY, Boolean.FALSE);
                }
            }
            @Override public void windowClosed(WindowEvent event) {
                // Na reconexao o callback adota a janela nova antes de descartar a antiga.
                if (current != frame || closed) return;
                closed = true;
                if (ownedServer != null) ownedServer.close();
            }
        });
    }
}
