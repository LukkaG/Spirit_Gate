package com.projetok.prototype;

import com.projetok.domain.command.SimulationCommandQueue;
import java.util.Objects;
import java.util.List;
import java.util.ArrayList;
import com.projetok.domain.skill.SkillCastCommand;

/** Adapta transições locais para um lote determinístico consumido em cada tick. */
final class DebugCommandBuffer {
    private interface Command { }
    private record Input(DebugInput value) implements Command { }
    private record Restart() implements Command { }
    private record Cast(SkillCastCommand value) implements Command { }

    /** Estado do tick, quantidade de novas pressões de E e marcador de reinício. */
    record Frame(DebugInput input, int effortPresses, boolean restart, List<SkillCastCommand> casts) { }

    private final SimulationCommandQueue<Command> queue = new SimulationCommandQueue<>();
    private DebugInput held = DebugInput.NONE;

    /** Publica transição; chamadas repetidas com E mantido não criam novas pressões. */
    void submit(DebugInput input) { queue.submit(new Input(Objects.requireNonNull(input))); }

    /** Ordena reinício junto às entradas, eliminando ações anteriores a ele. */
    void restart() { queue.submit(new Restart()); }

    /** Publica intenção de lançamento no mesmo contrato e fila das demais ações. */
    void cast(SkillCastCommand command) { queue.submit(new Cast(Objects.requireNonNull(command))); }

    /**
     * Executado somente pela thread da simulação. Toque de movimento dura um tick;
     * ataques rápidos respeitam o cooldown e cada pressão de E é contada uma vez.
     */
    Frame drain() {
        boolean up = false, down = false, left = false, right = false;
        boolean attack = false, restart = false;
        int effortPresses = 0;
        List<SkillCastCommand> casts = new ArrayList<>();
        for (Command command : queue.drain()) {
            if (command instanceof Restart) {
                restart = true;
                effortPresses = 0;
                casts.clear();
                up = false; down = false; left = false; right = false;
                attack = false;
            } else if (command instanceof Cast cast) {
                casts.add(cast.value());
            } else if (command instanceof Input input) {
                DebugInput next = input.value();
                if (next.effort() && !held.effort()) effortPresses++;
                up |= next.up() && !held.up(); down |= next.down() && !held.down();
                left |= next.left() && !held.left(); right |= next.right() && !held.right();
                attack |= next.attack() && !held.attack();
                held = next;
            }
        }
        return new Frame(new DebugInput(up || held.up(), down || held.down(),
                left || held.left(), right || held.right(), attack || held.attack(), held.effort()),
                effortPresses, restart, List.copyOf(casts));
    }
}
