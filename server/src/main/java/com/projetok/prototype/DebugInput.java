package com.projetok.prototype;

/**
 * Estado de entrada de um cliente local, coletado pela interface e lido pelo loop.
 * Nenhuma classe de domínio depende de Swing ou de eventos de teclado.
 */
public record DebugInput(boolean up, boolean down, boolean left, boolean right, boolean attack,
                         boolean effort) {
    /** Mantém compatibilidade com entradas anteriores, sem esforço físico. */
    public DebugInput(boolean up, boolean down, boolean left, boolean right, boolean attack) {
        this(up, down, left, right, attack, false);
    }
    public static final DebugInput NONE = new DebugInput(false, false, false, false, false);
}
