package com.projetok.domain.probability;

import java.util.random.RandomGenerator;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Sorteios controlados verificam probabilidades sem amostragem estatística instável. */
class ProbabilityServiceTest {
    /** Gerador da biblioteca padrão com sequência explícita e contagem de consumo. */
    private static final class Samples implements RandomGenerator {
        private final double[] values;
        private int consumed;

        /** Guarda as amostras que deverão ser consumidas em ordem. */
        private Samples(double... values) { this.values = values; }

        /** Detecta caminhos inesperados que deixem de usar a amostra double contratada. */
        @Override public long nextLong() { throw new AssertionError("Sorteio long inesperado"); }

        /** Entrega exatamente uma amostra, falhando quando o consumidor sorteia em excesso. */
        @Override public double nextDouble() {
            assertTrue(consumed < values.length, "Não deve haver sorteio extra");
            return values[consumed++];
        }
    }

    /** Chances de zero e cem por cento não consultam o gerador. */
    @Test void zeroECemPorCentoSemSorteio() {
        var samples = new Samples();
        var service = new ProbabilityService(samples);
        assertFalse(service.succeeds(0));
        assertTrue(service.succeeds(1));
        assertEquals(0, samples.consumed);
    }

    /** Comparação é estrita: 0,29 passa para 30%, mas 0,30 falha. */
    @Test void limiteExatoDeTrintaPorCento() {
        var samples = new Samples(0.29, 0.30);
        var service = new ProbabilityService(samples);
        assertTrue(service.succeeds(0.30));
        assertFalse(service.succeeds(0.30));
        assertEquals(2, samples.consumed);
    }

    /** Cada tentativa consome uma nova amostra, sem reutilizar sucesso ou falha anterior. */
    @Test void sorteiosIndependentes() {
        var samples = new Samples(0.29, 0.99, 0, 0.30);
        var service = new ProbabilityService(samples);
        assertTrue(service.succeeds(0.30));
        assertFalse(service.succeeds(0.30));
        assertTrue(service.succeeds(0.30));
        assertFalse(service.succeeds(0.30));
        assertEquals(4, samples.consumed);
    }

    /** Probabilidades inválidas são rejeitadas antes de consultar o gerador. */
    @Test void rejeitaProbabilidadesInvalidas() {
        var samples = new Samples();
        var service = new ProbabilityService(samples);
        for (double invalid : new double[] {-0.01, 1.01, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
            assertThrows(IllegalArgumentException.class, () -> service.succeeds(invalid));
        assertEquals(0, samples.consumed);
        assertThrows(NullPointerException.class, () -> new ProbabilityService(null));
    }

    /** Geradores injetados também devem respeitar o contrato de amostra finita em [0, 1). */
    @Test void rejeitaAmostrasInvalidas() {
        for (double invalid : new double[] {-0.01, 1, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY}) {
            var service = new ProbabilityService(new Samples(invalid));
            assertThrows(IllegalStateException.class, () -> service.succeeds(0.30));
        }
    }
}
