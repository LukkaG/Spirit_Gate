package com.projetok.domain.command;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Objects;

/** Fila FIFO entre produtores e uma thread proprietária da simulação, sem rede ou GUI. */
public final class SimulationCommandQueue<T> {
    private final ArrayDeque<T> pending = new ArrayDeque<>();

    /** Publica um comando sem acessar entidades. */
    public synchronized void submit(T command) {
        pending.addLast(Objects.requireNonNull(command));
    }

    /** Transfere atomicamente o lote atual; comandos posteriores ficam para o próximo tick. */
    public synchronized List<T> drain() {
        List<T> result = List.copyOf(pending);
        pending.clear();
        return result;
    }
}
