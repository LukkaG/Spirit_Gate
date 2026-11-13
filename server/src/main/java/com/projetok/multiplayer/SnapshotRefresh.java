package com.projetok.multiplayer;

import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

/** Agenda uma atualizacao visual pendente, mesmo quando varios snapshots chegam juntos. */
public final class SnapshotRefresh implements Runnable {
    private final AtomicBoolean pending=new AtomicBoolean();
    private final Executor ui;
    private final Runnable refresh;
    public SnapshotRefresh(Executor ui,Runnable refresh) {
        this.ui=Objects.requireNonNull(ui);this.refresh=Objects.requireNonNull(refresh);
    }
    @Override public void run() {
        if(pending.compareAndSet(false,true)) {
            try {ui.execute(()->{pending.set(false);refresh.run();});}
            catch(RuntimeException failure){pending.set(false);throw failure;}
        }
    }
}
