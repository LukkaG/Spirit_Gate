package com.projetok.rpg;

import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Detecta recursos ausentes no classpath antes de inicializar uma sessão de jogo. */
class ResourceLifecycleTest {
    /** Usa os arquivos reais empacotados pelo lifecycle, sem fallback para a árvore de fontes. */
    @Test void nativeResourcesAreAvailableAndWorldStarts() throws Exception {
        for (String name : List.of("content.properties", "corujao-v.sgquest", "dialogues.properties",
                "enemies.properties", "experimental-player.properties", "items.properties",
                "loot.properties", "npcs.properties", "objects.properties", "portals.properties",
                "progression.properties", "quests.properties", "reaper-player.properties", "spawns.properties")) {
            try (var input = RpgWorld.class.getResourceAsStream("/rpg/" + name)) {
                assertNotNull(input, "Recurso ausente no classpath: " + name);
                assertTrue(input.readAllBytes().length > 0, name);
            }
        }
        assertNotNull(new RpgWorld().snapshot());
        assertFalse(RpgSceneSet.entries("enemies").isEmpty());
    }
}
