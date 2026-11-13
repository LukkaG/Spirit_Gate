package com.projetok.prototype.rpg;

import javax.swing.*;
import java.awt.*;

/** Identifica telas de entrada que podem ser descartadas depois do ingresso na partida. */
final class SessionWindowNavigation {
    private static final String KEY = "spiritgate.navigation";
    private SessionWindowNavigation() { }

    static void mark(RootPaneContainer window) {
        window.getRootPane().putClientProperty(KEY, Boolean.TRUE);
    }

    static void enterGame(JFrame game, Window connectionDialog) {
        if (!SwingUtilities.isEventDispatchThread()) throw new IllegalStateException("Transicao exige EDT");
        if (!game.isShowing()) throw new IllegalStateException("Partida ainda nao abriu");
        connectionDialog.dispose();
        for (Window window : Window.getWindows()) {
            if (window != game && window instanceof RootPaneContainer root
                    && Boolean.TRUE.equals(root.getRootPane().getClientProperty(KEY))) {
                window.dispose();
            }
        }
        // Editores, outra partida e aplicativos externos nao sao telas de navegacao.
        game.toFront();
        game.requestFocus();
    }
}
