package com.projetok.multiplayer;

import com.projetok.rpg.SharedRpgEncounter;
import com.projetok.studio.WorldContent;
import java.net.InetAddress;
import java.time.Duration;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Cenário de progressão/missão com a vida oficial do catálogo, independente do piso de treino.
 * A sessão continua usando revisão, personagens, combate, ledger e dois sockets TCP reais.
 * EnemyDurabilityTest verifica separadamente o piso de 10.000 HP do servidor de conteúdo.
 */
final class Task225R1CombatFixture {
    private Task225R1CombatFixture() { }

    /**
     * Instala o encontro de catálogo antes de admitir qualquer jogador, sem editar HP de entidades.
     * A revisão é a mesma; somente a configuração efêmera do encontro pertence a esta fixture.
     */
    static LocalSimulationServer start() throws Exception {
        var revision = WorldContent.baseline();
        var world = new RpgSessionWorld(List.of("spirit", "human"), SessionCharacterSelectionTest.catalog(), true);
        world.enableRevisions(revision);
        assertEquals(10_000, world.combatSnapshot().session().world().actors().get(0).maxHp(),
                "A fábrica de produção continua configurando o piso de treino");
        var encounter = new SharedRpgEncounter(revision.geometry(), revision.bundle().scenes().enemies(),
                SharedRpgEncounter.RewardPolicy.SYNTHETIC_PARTICIPANTS);
        world.installRevision(new RpgSessionWorld.RevisionCandidate(revision, world.capture().players(), encounter));
        assertEquals(40, world.combatSnapshot().session().world().actors().get(0).maxHp(),
                "O cenário usa a vida oficial do slime do catálogo");
        var server = new LocalSimulationServer(InetAddress.getLoopbackAddress(), 0, world);
        try {
            server.start();
            return server;
        } catch (java.io.IOException error) {
            server.close();
            throw error;
        }
    }

    /**
     * Mata por comandos legítimos de ataque e espera a morte observada, não somente XP ou READY.
     * O limite de seis tentativas cobre a aproximação inicial e três golpes de 18 no slime de 40 HP.
     */
    static Protocol.CombatEvent defeatSlime(LocalSimulationServer server, LocalClient attacker,
                                             LocalClient observer) throws Exception {
        Task201CharacterSessionTest.waitFor(() -> attacker.playerSheet() != null && observer.playerSheet() != null,
                attacker, observer);
        var victim = attacker.snapshot().actors().stream()
                .filter(actor -> actor.id() > 1_000_000 && actor.id() < 2_000_000).findFirst().orElseThrow();
        assertEquals(40, victim.hp());
        assertEquals(40, victim.maxHp());
        assertEquals(0, attacker.playerSheet().progress().xp());
        assertTrue(attacker.selectTarget(victim.id()));
        for (int attempt = 0; attempt < 6 && death(attacker, victim.id()) == null; attempt++) {
            long tick = attacker.snapshot().tick();
            assertTrue(attacker.send(Protocol.Action.ATTACK, 0, 0));
            attacker.await(snapshot -> snapshot.tick() > tick + 12, Duration.ofSeconds(3));
            assertNull(server.failure());
        }
        Task201CharacterSessionTest.waitFor(() -> death(attacker, victim.id()) != null
                && death(observer, victim.id()) != null, attacker, observer);
        var death = death(attacker, victim.id());
        assertEquals(attacker.id(), death.player());
        assertEquals(1, death.generation());
        assertEquals(death, death(observer, victim.id()), "Ambos observam a mesma morte autoritativa");
        assertEquals(0, attacker.snapshot().actors().stream().filter(actor -> actor.id() == victim.id())
                .findFirst().orElseThrow().hp());
        assertEquals(0, observer.snapshot().actors().stream().filter(actor -> actor.id() == victim.id())
                .findFirst().orElseThrow().hp());
        assertEquals(1, events(attacker, "DEATH", death).size());
        var rewards = events(attacker, "SYNTHETIC_REWARD", death);
        assertEquals(1, rewards.size());
        assertEquals(3, rewards.get(0).amount());
        assertEquals(rewards, events(observer, "SYNTHETIC_REWARD", death));
        assertTrue(attacker.combatEvents().stream().filter(event -> event.kind().equals("HIT"))
                .allMatch(event -> event.player() == attacker.id()), "O observador não contribuiu para a morte");
        return death;
    }

    /** Localiza a morte da primeira geração; um respawn não satisfaz este ensaio. */
    private static Protocol.CombatEvent death(LocalClient client, int enemy) {
        return client.combatEvents().stream().filter(event -> event.kind().equals("DEATH")
                && event.enemy() == enemy && event.generation() == 1).findFirst().orElse(null);
    }

    /** Restringe eventos ao mesmo lançador, vítima e geração observados no ledger. */
    static List<Protocol.CombatEvent> events(LocalClient client, String kind, Protocol.CombatEvent death) {
        return client.combatEvents().stream().filter(event -> event.kind().equals(kind)
                && event.player() == death.player() && event.enemy() == death.enemy()
                && event.generation() == death.generation()).toList();
    }
}
