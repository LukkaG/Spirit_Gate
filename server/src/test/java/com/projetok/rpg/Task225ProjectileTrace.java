package com.projetok.rpg;

import com.projetok.domain.entity.Position;
import com.projetok.domain.movement.CollisionService;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Sonda isolada da TASK-225: observa o projétil real sem exportar estado privado para a produção. */
public final class Task225ProjectileTrace {
    /** Coordenada de apoio no mundo; a elevação de 28 pixels pertence somente à apresentação. */
    public record Point(float x, float y) { }
    /** Uma amostra após um tick real do executor, com cópia dos eventos autoritativos daquele instante. */
    public record Sample(long tick, Point caster, Point target, Point projectile, int hp,
                         List<RpgSkillEngine.Event> events, boolean contact) {
        public Sample { events = List.copyOf(events); }
    }
    /** Sequência finita e reproduzível, sem IA, rede, saves ou dependência de relógio de parede. */
    public record Trace(List<Sample> samples, RpgSkillEngine.Event launch, RpgSkillEngine.Event impact) {
        public Trace { samples = List.copyOf(samples); }
    }

    private Task225ProjectileTrace() { }

    /** Move o alvo lateralmente cinco unidades por tick a partir do terceiro tick e observa o contato real. */
    public static Trace run() throws ReflectiveOperationException {
        var space = new CollisionService.Space(960, 640, List.of(), List.of());
        var definitions = Map.of("task225_target", "slime|forest|340|300");
        var scenes = new RpgSceneSet("forest", Map.of("forest", new RpgSceneSet.Geometry(space, new Position(140, 300))),
                definitions, Map.of(), Map.of(), Map.of());
        var world = new RpgWorld(CharacterIdentity.create("Lançador controlado", "reaper", "azure"), scenes);
        world.useNetworkFootprint();
        world.awardExperience(RpgProgression.builtin().threshold(15));
        if (!world.chooseDomain("water")) throw new AssertionError("Domínio da sonda não foi admitido");
        var encounter = new SharedRpgEncounter(Map.of("forest", space), definitions, SharedRpgEncounter.RewardPolicy.DISABLED);
        encounter.join(1, world);
        var enemy = world.skillEnemies().get(0);
        enemy.hp = 1_000;
        var engine = new RpgSkillEngine();
        Map<Integer, RpgWorld> players = Map.of(1, world);
        engine.clock(0);
        if (!engine.cast(1, world, new RpgSkillEngine.Cast(1, 1_000_000 + enemy.getId(), enemy.generation, 0, 0), players))
            throw new AssertionError("O lançamento da sonda não foi admitido");
        var launch = engine.events().get(0);
        var samples = new ArrayList<Sample>();
        samples.add(new Sample(0, point(world.skillPosition()), point(enemy.getPosition()), projectile(engine), enemy.hp,
                engine.events(), false));
        // Limite externo independente da permanência da ação; nenhum cenário fica em espera ilimitada.
        for (int tick = 1; tick < 20; tick++) {
            world.tick(0, 0);
            enemy.setPosition(new Position(340, 300 + Math.max(0, tick - 2) * 5));
            // O encontro está conectado para aplicar dano pelo ledger, mas a IA não redefine a posição imposta.
            engine.clock(tick);
            engine.tick(players);
            var impact = engine.events().stream().filter(event -> event.fromX() == event.x() && event.fromY() == event.y())
                    .findFirst().orElse(null);
            Point reached = impact == null ? projectile(engine) : new Point(impact.x(), impact.y());
            samples.add(new Sample(tick, point(world.skillPosition()), point(enemy.getPosition()), reached, enemy.hp,
                    engine.events(), impact != null));
            if (impact != null) return new Trace(samples, launch, impact);
        }
        throw new AssertionError("O projétil não encontrou o alvo em menos de vinte ticks");
    }

    private static Point point(Position position) { return new Point(position.x(), position.y()); }

    /** Reflexão restrita ao teste lê a posição já simulada; não altera a trajetória nem cria um protocolo auxiliar. */
    private static Point projectile(RpgSkillEngine engine) throws ReflectiveOperationException {
        Field pending = RpgSkillEngine.class.getDeclaredField("pending");
        pending.setAccessible(true);
        for (var action : (List<?>) pending.get(engine)) if (action.getClass().getSimpleName().equals("Projectile")) {
            Field point = action.getClass().getDeclaredField("point");
            point.setAccessible(true);
            return point((Position) point.get(action));
        }
        throw new AssertionError("O projétil esperado não está mais pendente antes do contato");
    }
}
