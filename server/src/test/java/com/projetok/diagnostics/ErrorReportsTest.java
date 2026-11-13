package com.projetok.diagnostics;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

/** Verifica a captura em JVMs isoladas e preserva os globais do executor de testes. */
class ErrorReportsTest {
    @TempDir Path temporary;
    private record Result(Path report, String console, String output) {
        String text() throws Exception { return Files.readString(report, StandardCharsets.UTF_8); }
    }

    @Test void stderrStackUnicodeAndCloseKeepConsoleAndSingleSession() throws Exception {
        Path destination = temporary.resolve("erros personalizados");
        Result result = run("stderr", destination, temporary);
        String text = result.text();
        for (String marker : List.of("ação-órfã-漢字", "stack-direto", "causa-original",
                "unicode-fragmentado-ç漢", "console-continuou-após-close")) {
            assertTrue(text.contains(marker), marker);
            assertTrue(result.console().contains(marker), marker);
        }
        assertEquals(destination, result.report().getParent());
        try (var files = Files.list(destination)) { assertEquals(1, files.count()); }
        assertTrue(text.contains("Codificação: UTF-8"));
    }

    @Test void uncaughtIsRecordedOnceAndRemainsVisibleOnConsole() throws Exception {
        Result result = run("uncaught", temporary.resolve("erros"), temporary);
        assertEquals(1, occurrences(result.text(), "falha-não-tratada"));
        assertEquals(1, occurrences(result.console(), "falha-não-tratada"));
        assertTrue(result.text().contains("thread-de-teste"));
        assertTrue(result.text().contains("depois-do-handler"));
    }

    @Test void previousHandlerIsInvokedWithoutDuplicatingItsStackInFile() throws Exception {
        Result result = run("previous", temporary.resolve("erros"), temporary);
        assertTrue(result.console().contains("handler-anterior-executado"));
        assertEquals(1, occurrences(result.text(), "falha-não-tratada"));
        assertEquals(1, occurrences(result.console(), "falha-não-tratada"));
        assertTrue(result.text().contains("depois-do-handler"));
    }

    @Test void concurrentStderrAndReportsDoNotLoseOrSplitLines() throws Exception {
        Result result = run("parallel", temporary.resolve("erros"), temporary);
        String text = result.text();
        for (int worker = 0; worker < 8; worker++) {
            for (int line = 0; line < 25; line++) {
                assertEquals(1, occurrences(text, "THREAD-" + worker + "-LINE-" + line + "-FIM"));
                assertEquals(1, occurrences(text, "REPORT-" + worker + "-" + line + "-FIM"));
            }
        }
    }

    @Test void unavailableDirectoryWarnsOnceAndKeepsConsoleOpen() throws Exception {
        Path blocked = temporary.resolve("arquivo-no-lugar-da-pasta");
        Files.writeString(blocked, "preservar");
        Result result = run("fallback", blocked, temporary);
        assertEquals("preservar", Files.readString(blocked));
        assertTrue(result.console().contains("erro-com-disco-indisponível"));
        assertTrue(result.console().contains("stack-preservado-no-console"));
        assertTrue(result.console().contains("console-ainda-ativo"));
        assertEquals(1, occurrences(result.console(), "Não foi possível gravar o TXT"));
    }

    @Test void discoversProjectRootWhenStartedInsideServer() throws Exception {
        Path project = temporary.resolve("projeto");
        Path working = Files.createDirectories(project.resolve("server/pasta-interna"));
        Files.writeString(project.resolve("server/pom.xml"), "<project/>");
        Result result = run("default", null, working);
        assertEquals(project.resolve("logs/erros"), result.report().getParent());
        assertTrue(result.text().contains("erro-na-raiz-descoberta"));
    }

    @Test void swingExceptionIsCapturedAndEventQueueContinues() throws Exception {
        Result result = run("swing", temporary.resolve("erros"), temporary);
        assertEquals(1, occurrences(result.text(), "falha-na-EDT"));
        assertTrue(result.output().contains("EDT-continua-ativa"));
    }

    @Test void scheduledServerFailureStillClosesTheServerAndWritesItsStack() throws Exception {
        Result result = run("server", temporary.resolve("erros"), temporary);
        assertTrue(result.text().contains("Falha no ciclo do servidor"));
        assertEquals(1, occurrences(result.text(), "falha-tick-servidor"));
        assertTrue(result.output().contains("SERVIDOR-encerrado-com-falha"));
    }

    @Test void scheduledRpgFailureStillStopsCommandsAndWritesItsStack() throws Exception {
        Result result = run("rpg", temporary.resolve("erros"), temporary);
        assertTrue(result.text().contains("Falha no ciclo RPG"));
        assertEquals(1, occurrences(result.text(), "falha-tick-rpg"));
        assertTrue(result.output().contains("RPG-interrompido-com-falha"));
    }

    @Test void largeReportRotatesWithoutDeletingPartsOrSplittingUnicode() throws Exception {
        Path directory = temporary.resolve("erros");
        run("rotation", directory, temporary);
        List<Path> parts;
        try (var files = Files.list(directory)) { parts = files.sorted().toList(); }
        assertEquals(2, parts.size());
        long unicodeCharacters = 0;
        var complete = new StringBuilder();
        for (Path part : parts) {
            assertTrue(Files.size(part) <= ErrorReports.MAX_REPORT_BYTES, part.toString());
            String text = Files.readString(part, StandardCharsets.UTF_8);
            unicodeCharacters += text.chars().filter(character -> character == '漢').count();
            complete.append(text);
        }
        assertEquals(1024 * 1024, unicodeCharacters);
        assertEquals(1, occurrences(complete.toString(), "ROTATION-START"));
        assertEquals(1, occurrences(complete.toString(), "ROTATION-END"));
    }

    /** Executa cada captura fora da JVM do JUnit, inclusive quando muda o diretório corrente. */
    private Result run(String scenario, Path destination, Path workingDirectory) throws Exception {
        String suffix = UUID.randomUUID().toString();
        Path output = temporary.resolve("stdout-" + suffix + ".txt");
        Path console = temporary.resolve("console-" + suffix + ".txt");
        Path java = Path.of(System.getProperty("java.home"), "bin",
                System.getProperty("os.name").startsWith("Windows") ? "java.exe" : "java");
        String classpath = Path.of(ErrorReports.class.getProtectionDomain().getCodeSource().getLocation().toURI())
                + File.pathSeparator
                + Path.of(ErrorReportsProbe.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        var command = new ArrayList<>(List.of(java.toString(), "-Djava.awt.headless=true"));
        if (destination != null) command.add("-D" + ErrorReports.DIRECTORY_PROPERTY + "=" + destination);
        command.addAll(List.of("-cp", classpath, ErrorReportsProbe.class.getName(), scenario));
        Process child = new ProcessBuilder(command).directory(workingDirectory.toFile())
                .redirectOutput(output.toFile()).redirectError(console.toFile()).start();
        try {
            assertTrue(child.waitFor(20, TimeUnit.SECONDS), "Processo de teste não encerrou: " + scenario);
            String printed = Files.readString(output, StandardCharsets.UTF_8);
            String errors = Files.readString(console, StandardCharsets.UTF_8);
            assertEquals(0, child.exitValue(), errors);
            String path = printed.lines().filter(line -> line.startsWith("REPORT="))
                    .findFirst().orElseThrow().substring("REPORT=".length());
            return new Result(Path.of(path), errors, printed);
        } finally {
            if (child.isAlive()) child.destroyForcibly();
        }
    }

    /** Conta marcadores completos para revelar duplicações do tee com o handler global. */
    private static int occurrences(String text, String marker) {
        int count = 0, position = 0;
        while ((position = text.indexOf(marker, position)) >= 0) {
            count++;
            position += marker.length();
        }
        return count;
    }
}
