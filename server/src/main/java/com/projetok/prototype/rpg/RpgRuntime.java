package com.projetok.prototype.rpg;

import com.projetok.rpg.RpgWorld;
import java.util.concurrent.*;

/** Ciclo independente; entrada imutavel, snapshots publicados e fechamento idempotente. */
public final class RpgRuntime implements AutoCloseable {
    private final RpgWorld world;
    private final ArrayBlockingQueue<java.util.function.Consumer<RpgWorld>> commands=new ArrayBlockingQueue<>(64);
    private final ExecutorService io=Executors.newSingleThreadExecutor(r->{Thread t=new Thread(r,"spirit-gate-rpg-io");t.setDaemon(true);return t;});
    private final ScheduledExecutorService loop = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "spirit-gate-rpg"); t.setDaemon(true); return t;
    });
    private record Input(float x,float y) { }
    private volatile Input input=new Input(0,0);
    private volatile RpgWorld.View view;
    private volatile boolean paused;
    private volatile RuntimeException failure;
    private boolean started, closed;
    public RpgRuntime() { this(new RpgWorld()); }
    public RpgRuntime(RpgWorld world) { this.world = world; view = world.snapshot(); }
    public synchronized void start() {
        if (started || closed) throw new IllegalStateException("Ciclo ja iniciado/encerrado");
        started = true;
        loop.scheduleAtFixedRate(() -> {try{advanceOneTick();}catch(RuntimeException error){failure=error;loop.shutdown();error.printStackTrace();}}, 0, 33_333_333, TimeUnit.NANOSECONDS);
    }
    void advanceOneTick(){
        if(paused)return;
        for(int i=0;i<64;i++){var command=commands.poll();if(command==null)break;command.accept(world);if(paused)break;}
        if(!paused){Input value=input;world.tick(value.x(),value.y());}view=world.snapshot();
    }
    public RuntimeException failure(){return failure;}
    public void input(float x, float y) {
        if (!Float.isFinite(x) || !Float.isFinite(y)) throw new IllegalArgumentException("Entrada invalida");
        input=new Input(x,y);
    }
    public RpgWorld.View snapshot() { return view; }
    public synchronized boolean command(java.util.function.Consumer<RpgWorld> command){return !closed&&failure==null&&commands.offer(java.util.Objects.requireNonNull(command));}
    public CompletableFuture<Boolean> action(java.util.function.Function<RpgWorld,Boolean> action){
        var result=new CompletableFuture<Boolean>();if(!command(w->{try{result.complete(action.apply(w));}catch(RuntimeException error){result.completeExceptionally(error);}}))result.complete(false);return result;
    }
    public CompletableFuture<Void> save(com.projetok.rpg.LocalSaveStore store) {
        return save(store,false);
    }
    public CompletableFuture<Void> saveAndPause(com.projetok.rpg.LocalSaveStore store){return save(store,true);}
    private CompletableFuture<Void> save(com.projetok.rpg.LocalSaveStore store,boolean pause){
        var capture=new CompletableFuture<com.projetok.rpg.RpgSave>();
        if(!command(w->{capture.complete(w.save());paused=pause;}))capture.completeExceptionally(new IllegalStateException("Simulacao encerrada ou fila cheia"));
        return capture.thenAcceptAsync(state->{try{store.save(state);}catch(java.io.IOException e){throw new CompletionException(e);}},io).whenComplete((unused,error)->{if(error!=null&&pause)paused=false;});
    }
    public synchronized void close() { closed = true; loop.shutdownNow();io.shutdown(); }
}
