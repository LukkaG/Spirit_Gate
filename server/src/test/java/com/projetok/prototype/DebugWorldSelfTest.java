package com.projetok.prototype;

/**
 * Testes rápidos sem bibliotecas externas, executáveis com javac/java para validar
 * a simulação em ambientes sem Maven. O conjunto JUnit é definido separadamente.
 */
public final class DebugWorldSelfTest {
    private static final float STEP = 1f / 30f;

    private DebugWorldSelfTest() { }

    public static void main(String[] args) {
        validatesSetup();
        validatesNormalizedMovement();
        validatesWallAndObjectCollisions();
        validatesCombatAndCooldown();
        validatesMobAttack();
        validatesDeathEventAndImmutableSnapshot();
        System.out.println("OK: 6 validacoes do mundo 2D passaram.");
    }

    private static void validatesSetup() {
        DebugWorld world = new DebugWorld();
        DebugSnapshot s = world.snapshot();
        check(s.actors().size() == 4, "deve haver 1 jogador e 3 mobs");
        check(s.actors().get(0).player(), "o primeiro ator deve ser jogador");
        check(s.playerHp() == 100 && s.playerMaxHp() == 100, "HP derivado da raca humana");
        check(s.obstacles().size() == 3, "mapa deve possuir tres obstaculos");
    }

    private static void validatesNormalizedMovement() {
        DebugWorld horizontal = new DebugWorld();
        DebugWorld diagonal = new DebugWorld();
        float x0 = horizontal.snapshot().actors().get(0).x();
        horizontal.tick(new DebugInput(false, false, false, true, false), STEP);
        diagonal.tick(new DebugInput(true, false, false, true, false), STEP);
        var h = horizontal.snapshot().actors().get(0);
        var d = diagonal.snapshot().actors().get(0);
        float hDistance = h.x() - x0;
        float dDistance = (float) Math.hypot(d.x() - x0, d.y() - 340);
        check(Math.abs(hDistance - dDistance) < .02f, "diagonal nao pode ser mais rapida");
        check(horizontal.snapshot().ticks() == 1L, "tick deve ser contabilizado");
    }

    private static void validatesWallAndObjectCollisions() {
        DebugWorld world = new DebugWorld();
        for (int i = 0; i < 200; i++) {
            world.tick(new DebugInput(false, false, false, true, false), STEP);
            DebugSnapshot s = world.snapshot();
            DebugSnapshot.ActorView p = s.actors().get(0);
            check(p.x() >= DebugWorld.PLAYER_RADIUS && p.x() <= DebugWorld.WIDTH - DebugWorld.PLAYER_RADIUS,
                    "jogador saiu dos limites horizontais");
            for (DebugSnapshot.Obstacle o : s.obstacles()) {
                float closestX = Math.max(o.x(), Math.min(p.x(), o.x() + o.width()));
                float closestY = Math.max(o.y(), Math.min(p.y(), o.y() + o.height()));
                float distance = (float) Math.hypot(p.x() - closestX, p.y() - closestY);
                check(distance >= DebugWorld.PLAYER_RADIUS - .01f,
                        "jogador atravessou parede: " + distance);
            }
            for (DebugSnapshot.ActorView mob : s.actors()) {
                if (mob.player() || !mob.alive()) continue;
                float distance = (float) Math.hypot(p.x() - mob.x(), p.y() - mob.y());
                check(distance >= p.radius() + mob.radius() - .01f,
                        "jogador atravessou hitbox do mob");
            }
        }
    }

    private static void validatesCombatAndCooldown() {
        DebugWorld world = new DebugWorld();
        boolean inflictedDamage = false;
        DebugSnapshot current = world.snapshot();
        for (int i = 0; i < 360; i++) {
            var player = current.actors().get(0);
            var mob = current.actors().get(1);
            float dx = mob.x() - player.x();
            float dy = mob.y() - player.y();
            DebugInput input = new DebugInput(dy < -4, dy > 4, dx < -4, dx > 4, true);
            world.tick(input, STEP);
            DebugSnapshot next = world.snapshot();
            if (next.actors().get(1).hp() < mob.maxHp()) {
                inflictedDamage = true;
                int oldHp = next.actors().get(1).hp();
                check(oldHp == mob.maxHp() - 15,
                        "dano desarmado deve vir do CombatService e strength/2");
                // Um tick extra não pode ignorar o cooldown, mesmo segurando ESPAÇO.
                world.tick(new DebugInput(false, false, false, false, true), STEP);
                check(world.snapshot().actors().get(1).hp() == oldHp, "cooldown nao respeitado");
                break;
            }
            current = next;
        }
        check(inflictedDamage, "jogador deve conseguir acertar um mob e causar dano");
    }

    private static void validatesMobAttack() {
        DebugWorld world = new DebugWorld();
        boolean hit = false;
        for (int i = 0; i < 210; i++) {
            world.tick(DebugInput.NONE, STEP);
            if (world.snapshot().playerHp() < 100) { hit = true; break; }
        }
        check(hit, "mob deve perseguir jogador e atacar atraves do CombatService");
    }

    private static void validatesDeathEventAndImmutableSnapshot() {
        DebugWorld world = new DebugWorld();
        var snapshot = world.snapshot();
        boolean immutable = false;
        try { snapshot.actors().clear(); }
        catch (UnsupportedOperationException expected) { immutable = true; }
        check(immutable, "a interface nao pode alterar listas do dominio");
        boolean defeated = false;
        for (int i = 0; i < 600; i++) {
            var s = world.snapshot();
            var p = s.actors().get(0);
            var mob = s.actors().get(1);
            float dx = mob.x() - p.x(), dy = mob.y() - p.y();
            boolean moving = Math.hypot(dx, dy) > 49;
            world.tick(new DebugInput(moving && dy < -4, moving && dy > 4,
                    moving && dx < -4, moving && dx > 4, true), STEP);
            if (world.snapshot().defeated() >= 1) { defeated = true; break; }
        }
        check(defeated, "golpes repetidos devem causar morte e evento uma unica vez");
        for (int i = 0; i < 30; i++) world.tick(new DebugInput(false, false, false, false, true), STEP);
        check(world.snapshot().defeated() == 1, "evento de morte nao pode duplicar");
    }

    private static void check(boolean value, String description) {
        if (!value) throw new AssertionError(description);
    }
}
