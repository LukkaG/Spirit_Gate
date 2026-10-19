package com.projetok.prototype;

import java.lang.reflect.Modifier;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Verifica o contrato usado pelo launcher nativo sem abrir uma janela. */
class DebugLauncherEntrypointTest {
    /** jpackage precisa encontrar um main público, estático e com retorno void. */
    @Test void disponibilizaEntradaCompativelComLauncherNativo() throws Exception {
        var main = DebugLauncher.class.getDeclaredMethod("main", String[].class);
        assertEquals(void.class, main.getReturnType());
        assertTrue(Modifier.isPublic(main.getModifiers()));
        assertTrue(Modifier.isStatic(main.getModifiers()));
    }
}
