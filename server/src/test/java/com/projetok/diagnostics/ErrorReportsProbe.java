package com.projetok.diagnostics;

import com.projetok.multiplayer.LocalSimulationServer;
import com.projetok.multiplayer.Protocol;
import com.projetok.multiplayer.SessionSimulation;
import com.projetok.prototype.rpg.RpgRuntime;
import com.projetok.rpg.RpgWorld;
import java.awt.EventQueue;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;

/** Processo descartável que impede os testes de modificar stderr e handlers da JVM do JUnit. */
public final class ErrorReportsProbe {
    private ErrorReportsProbe() { }

    /** Executa um cenário real de captura e devolve ao teste o destino da sessão. */
    public static void main(String[] args) throws Exception {
        String scenario = args[0];
        if (scenario.equals("previous")) {
            Thread.setDefaultUncaughtExceptionHandler((thread, error) -> {
                System.err.println("handler-anterior-executado");
                error.printStackTrace();
            });
        }
        Path report = ErrorReports.install("teste-isolado");
        if (!java.util.Objects.equals(report, ErrorReports.install("instalação-repetida"))) {
            throw new IllegalStateException("Instalação não é idempotente");
        }
        System.out.println("REPORT=" + report);
        switch (scenario) {
            case "stderr" -> {
                System.err.println("ação-órfã-漢字");
                new IllegalStateException("stack-direto", new IOException("causa-original")).printStackTrace();
                byte[] unicode = "unicode-fragmentado-ç漢\n".getBytes(StandardCharsets.UTF_8);
                for (byte value : unicode) System.err.write(value);
                System.err.close();
                System.err.println("console-continuou-após-close");
            }
            case "uncaught", "previous" -> {
                Thread thread = new Thread(() -> { throw new IllegalStateException("falha-não-tratada"); }, "thread-de-teste");
                thread.start();
                thread.join();
                System.err.println("depois-do-handler");
            }
            case "parallel" -> {
                var threads = new ArrayList<Thread>();
                for (int worker = 0; worker < 8; worker++) {
                    int id = worker;
                    threads.add(new Thread(() -> {
                        for (int line = 0; line < 25; line++) {
                            System.err.println("THREAD-" + id + "-LINE-" + line + "-FIM");
                            ErrorReports.report("origem-" + id, new IOException("REPORT-" + id + "-" + line + "-FIM"));
                        }
                    }));
                }
                for (Thread thread : threads) thread.start();
                for (Thread thread : threads) thread.join();
            }
            case "fallback" -> {
                System.err.println("erro-com-disco-indisponível");
                ErrorReports.report("falha-recuperável", new IOException("stack-preservado-no-console"));
                System.err.close();
                System.err.println("console-ainda-ativo");
            }
            case "swing" -> {
                EventQueue.invokeLater(() -> { throw new IllegalStateException("falha-na-EDT"); });
                EventQueue.invokeAndWait(() -> System.out.println("EDT-continua-ativa"));
            }
            case "server" -> failServer(report);
            case "rpg" -> failRpg(report);
            case "rotation" -> ErrorReports.report("volume-grande",
                    new IOException("ROTATION-START-" + "á漢".repeat(1024 * 1024) + "-ROTATION-END"));
            case "default" -> System.err.println("erro-na-raiz-descoberta");
            default -> throw new IllegalArgumentException("Cenário desconhecido: " + scenario);
        }
    }

    /** Injeta falha na thread agendada real e verifica que o servidor continua encerrando. */
    private static void failServer(Path report) throws Exception {
        SessionSimulation simulation = new SessionSimulation() {
            public boolean join(int id, String name) { return true; }
            public void leave(int id) { }
            public boolean accept(int id, Protocol.Intent intent) { return false; }
            public void step() { throw new IllegalStateException("falha-tick-servidor"); }
            public Protocol.State snapshot() { return new Protocol.State(0, List.of()); }
        };
        try (var server = new LocalSimulationServer(InetAddress.getLoopbackAddress(), 0, simulation)) {
            server.start();
            waitFor(() -> server.failure() != null && !server.running());
            waitFor(() -> contains(report, "falha-tick-servidor"));
            System.out.println("SERVIDOR-encerrado-com-falha");
        }
    }

    /** Reutiliza o ponto de injeção existente do runtime sem criar uma API pública de teste. */
    private static void failRpg(Path report) throws Exception {
        var constructor = RpgRuntime.class.getDeclaredConstructor(RpgWorld.class, Runnable.class);
        constructor.setAccessible(true);
        Runnable failure = () -> { throw new IllegalStateException("falha-tick-rpg"); };
        try (var runtime = constructor.newInstance(new RpgWorld(), failure)) {
            runtime.start();
            waitFor(() -> runtime.failure() != null);
            waitFor(() -> contains(report, "falha-tick-rpg"));
            if (runtime.command(world -> world.attack())) throw new AssertionError("Runtime aceitou comando após falhar");
            System.out.println("RPG-interrompido-com-falha");
        }
    }

    /** Aguarda apenas as threads locais deste processo, com limite para evidenciar travamentos. */
    private static void waitFor(BooleanSupplier ready) throws Exception {
        long deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(5);
        while (!ready.getAsBoolean()) {
            if (System.nanoTime() > deadline) throw new AssertionError("Tempo de espera esgotado");
            Thread.sleep(10);
        }
    }

    /** Tolera o instante anterior à primeira gravação durante a prova assíncrona. */
    private static boolean contains(Path file, String text) {
        try { return Files.isRegularFile(file) && Files.readString(file).contains(text); }
        catch (IOException error) { return false; }
    }
}
