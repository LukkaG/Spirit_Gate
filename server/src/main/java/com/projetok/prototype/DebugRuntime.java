package com.projetok.prototype;

import com.projetok.domain.skill.SkillCastCommand;

import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Laço fixo a 30 ticks/segundo: somente sua thread acessa o mundo mutável.
 * Swing apenas publica entrada e lê snapshots completos (sem condições de corrida).
 */
public final class DebugRuntime implements AutoCloseable {
    private final ScheduledExecutorService gameThread = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "spirit-gate-simulation");
        thread.setDaemon(true);
        return thread;
    });
    private final DebugCommandBuffer commands = new DebugCommandBuffer();
    private final Supplier<DebugWorld> worldFactory;
    private volatile DebugWorld world;
    private volatile DebugSnapshot lastSnapshot;
    private final DirectionalAnimation animations = new DirectionalAnimation();
    private volatile DirectionalAnimation.Presentation presentation;

    /** Inicializa a arena demonstrativa usada pela janela e pelos reinícios. */
    public DebugRuntime() {
        this(DebugWorld::demo);
    }

    /** Permite sorteios reproduzíveis nos testes, inclusive depois do reinício. */
    DebugRuntime(Supplier<DebugWorld> worldFactory) {
        this.worldFactory = Objects.requireNonNull(worldFactory, "worldFactory");
        world = Objects.requireNonNull(worldFactory.get(), "world");
        lastSnapshot = world.snapshot();
        presentation = animations.advance(lastSnapshot, java.util.List.of(), 0);
    }

    /** Inicializa o loop local sem banco de dados ou serviço de rede. */
    public void start() {
        gameThread.scheduleAtFixedRate(() -> {
            try {
                advanceOneTick();
            } catch (RuntimeException exception) {
                System.err.println("[Spirit Gate] Falha no tick de debug:");
                exception.printStackTrace();
                throw exception; // falha visível; não finge que o mundo continua funcionando
            }
        }, 0, 33_333_333L, TimeUnit.NANOSECONDS);
    }

    /**
     * Processa o mesmo tick usado pelo agendador, sempre em uma única thread proprietária.
     * Testes do pacote podem avançá-lo sem iniciar o agendador nem consultar relógio real.
     */
    void advanceOneTick() {
        DebugCommandBuffer.Frame frame = commands.drain();
        if (frame.restart()) {
            animations.reset();
            world = Objects.requireNonNull(worldFactory.get(), "world");
            // O primeiro deslocamento apos R deve partir do spawn, como na abertura.
            animations.advance(world.snapshot(), java.util.List.of(), 0);
            world.synchronizeEffort(frame.input().effort());
        }
        world.tickQueued(frame.input(), 1f / 30f, frame.effortPresses(), frame.casts());
        lastSnapshot = world.snapshot();
        presentation = animations.advance(lastSnapshot, world.animationSignals(), 1d / 30d);
    }

    /** Recebe entrada do controlador sem modificar entidades fora do loop. */
    public void setInput(DebugInput value) {
        commands.submit(value);
    }

    /** Solicita a criação de uma nova arena na thread que possui o mundo. */
    public void restart() {
        commands.restart();
    }

    /** Transporta intenção imutável; entidades são alteradas somente no próximo tick. */
    public void cast(SkillCastCommand command) { commands.cast(command); }

    /** Último estado imutável publicado pelo loop, próprio para leitura no EDT. */
    public DebugSnapshot snapshot() {
        return presentation.world();
    }

    /** Mundo e animacoes do mesmo tick publicados em uma unica referencia imutavel. */
    public DirectionalAnimation.Presentation presentation() { return presentation; }

    @Override
    public void close() {
        gameThread.shutdownNow();
    }
}
