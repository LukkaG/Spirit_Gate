package com.projetok.prototype.rpg;

import com.projetok.rpg.RpgWorld;
import java.util.concurrent.*;

/** Ciclo independente; entrada imutavel, snapshots publicados e fechamento idempotente. */
public final class RpgRuntime implements AutoCloseable {
    private final RpgWorld world;
    private record Command(java.util.function.Consumer<RpgWorld> work,Runnable cancel) { }
    private final ArrayBlockingQueue<Command> commands=new ArrayBlockingQueue<>(64);
    private final Runnable beforeTick;
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
    public RpgRuntime(RpgWorld world) { this(world,()->{}); }
    RpgRuntime(RpgWorld world,Runnable beforeTick) { this.world = world;this.beforeTick=beforeTick; view = world.snapshot(); }
    public synchronized void start() {
        if (started || closed) throw new IllegalStateException("Ciclo ja iniciado/encerrado");
        started = true;
        loop.scheduleAtFixedRate(() -> {try{advanceOneTick();}catch(RuntimeException error){error.printStackTrace();}}, 0, 33_333_333, TimeUnit.NANOSECONDS);
    }
    synchronized void advanceOneTick(){
        if(paused||closed||failure!=null)return;
        try {
            beforeTick.run();
            for(int i=0;i<64;i++){var command=commands.poll();if(command==null)break;command.work().accept(world);if(paused||closed)break;}
            if(!paused&&!closed){Input value=input;world.tick(value.x(),value.y());}view=world.snapshot();
            if(paused)cancelQueued();
        } catch(RuntimeException error) {
            failure=error;input=new Input(0,0);cancelQueued();loop.shutdown();throw error;
        }
    }
    private void cancelQueued(){Command command;while((command=commands.poll())!=null)command.cancel().run();}
    public RuntimeException failure(){return failure;}
    public synchronized void input(float x, float y) {
        if (!Float.isFinite(x) || !Float.isFinite(y)) throw new IllegalArgumentException("Entrada invalida");
        if(!closed&&!paused&&failure==null)input=new Input(x,y);
    }
    public RpgWorld.View snapshot() { return view; }
    public synchronized boolean command(java.util.function.Consumer<RpgWorld> command){return enqueue(command,()->{});}
    private synchronized boolean enqueue(java.util.function.Consumer<RpgWorld> command,Runnable cancel){
        java.util.Objects.requireNonNull(command);
        return !closed&&!paused&&failure==null&&commands.offer(new Command(command,cancel));
    }
    public CompletableFuture<Boolean> action(java.util.function.Function<RpgWorld,Boolean> action){
        var result=new CompletableFuture<Boolean>();if(!enqueue(w->{try{result.complete(action.apply(w));}catch(RuntimeException error){result.completeExceptionally(error);}},()->result.complete(false)))result.complete(false);return result;
    }
    public CompletableFuture<Void> save(com.projetok.rpg.LocalSaveStore store) {
        return save(store,false);
    }
    public CompletableFuture<Void> saveAndPause(com.projetok.rpg.LocalSaveStore store){return save(store,true);}
    private CompletableFuture<Void> save(com.projetok.rpg.LocalSaveStore store,boolean pause){
        var capture=new CompletableFuture<com.projetok.rpg.RpgSave>();
        if(!enqueue(w->{try{var state=w.save();paused=pause;if(pause)input=new Input(0,0);capture.complete(state);}catch(RuntimeException error){capture.completeExceptionally(error);throw error;}},
                ()->capture.completeExceptionally(new IllegalStateException("Captura cancelada: simulacao indisponivel"))))capture.completeExceptionally(new IllegalStateException("Simulacao pausada, encerrada ou fila cheia"));
        return capture.thenAcceptAsync(state->{try{store.save(state);}catch(java.io.IOException e){throw new CompletionException(e);}},io).whenComplete((unused,error)->{if(error!=null&&pause&&!closed&&failure==null)paused=false;});
    }
    public synchronized void close() { closed = true;input=new Input(0,0);cancelQueued();loop.shutdownNow();io.shutdown(); }
}
