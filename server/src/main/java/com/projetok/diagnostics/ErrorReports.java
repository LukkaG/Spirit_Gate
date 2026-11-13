package com.projetok.diagnostics;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.UUID;

/**
 * Mantém uma cópia local dos erros de cada JVM, sem rede ou alteração da recuperação existente.
 * O console continua disponível e cada sessão recebe um TXT próprio em UTF-8.
 */
public final class ErrorReports {
    public static final String DIRECTORY_PROPERTY = "spiritgate.logs.dir";
    static final int MAX_REPORT_BYTES = 4 * 1024 * 1024;
    private static final DateTimeFormatter FILE_TIME = DateTimeFormatter
            .ofPattern("uuuuMMdd-HHmmss-SSS").withZone(ZoneOffset.UTC);
    private static volatile Session active;

    private ErrorReports() { }

    /**
     * Instala uma única captura de stderr e exceções não tratadas nesta JVM.
     * O caminho retornado é o destino planejado; uma falha de disco é informada no console.
     *
     * @param source componente responsável por iniciar a sessão
     * @return arquivo TXT exclusivo desta sessão, ou {@code null} se o caminho for inválido
     */
    public static synchronized Path install(String source) {
        if (active != null) return active.file;
        PrintStream originalConsole = System.err;
        // Todos os caminhos usam UTF-8, inclusive o fallback em terminais Windows.
        PrintStream console = new PrintStream(new OutputStream() {
            @Override public void write(int value) { originalConsole.write(value); }
            @Override public void write(byte[] bytes, int offset, int length) { originalConsole.write(bytes, offset, length); }
            @Override public void flush() { originalConsole.flush(); }
            @Override public void close() { flush(); }
        }, true, StandardCharsets.UTF_8);
        Path file = null;
        try {
            Path directory = directory(Path.of("").toAbsolutePath());
            String name = "erros-" + FILE_TIME.format(Instant.now()) + "-pid-"
                    + ProcessHandle.current().pid() + "-" + UUID.randomUUID() + ".txt";
            file = directory.resolve(name);
        } catch (InvalidPathException | SecurityException error) {
            console.println("[Spirit Gate] Caminho de logs indisponível: " + error.getMessage());
        }
        Session session = new Session(file, console, source);
        active = session;
        System.setErr(new ReportStream(session));

        // A EDT do Swing também encaminha exceções não tratadas ao handler padrão.
        Thread.UncaughtExceptionHandler previous = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((thread, error) -> {
            session.writeReport("Exceção não tratada | thread " + thread.getName(), error, false);
            session.suppressCapture.set(true);
            try {
                if (previous != null) previous.uncaughtException(thread, error);
                else {
                    console.print("Exception in thread \"" + thread.getName() + "\" ");
                    error.printStackTrace(console);
                }
            } finally {
                session.suppressCapture.remove();
            }
        });
        return file;
    }

    /**
     * Registra uma falha já tratada, preservando seu stack no console e no TXT.
     * Sem instalação prévia, mantém apenas o console e não altera globais durante testes.
     *
     * @param source operação que falhou
     * @param error falha original, inclusive causas e exceções suprimidas
     */
    public static void report(String source, Throwable error) {
        Session session = active;
        if (session != null) session.writeReport(source, error, true);
        else {
            System.err.println("[Spirit Gate] " + source);
            if (error != null) error.printStackTrace(System.err);
        }
    }

    /** Localiza o checkout a partir do diretório de execução, inclusive dentro de server. */
    static Path directory(Path workingDirectory) {
        String configured = System.getProperty(DIRECTORY_PROPERTY);
        if (configured != null && !configured.isBlank()) return Path.of(configured).toAbsolutePath().normalize();
        Path start = workingDirectory.toAbsolutePath().normalize();
        for (Path candidate = start; candidate != null; candidate = candidate.getParent()) {
            if (Files.isRegularFile(candidate.resolve("server/pom.xml"))) {
                return candidate.resolve("logs/erros");
            }
        }
        String executable = System.getProperty("jpackage.app-path");
        Path base = executable == null || executable.isBlank()
                ? start : Path.of(executable).toAbsolutePath().getParent();
        return base.resolve("logs/erros");
    }

    /** Serializa as gravações para impedir interleaving de relatórios de várias threads. */
    private static final class Session {
        private final Path file;
        private final PrintStream console;
        private final String source;
        private final ThreadLocal<Boolean> suppressCapture = ThreadLocal.withInitial(() -> false);
        private boolean failed, initialized, lineStart = true;
        private Path currentFile;
        private int part = 1, writtenBytes;
        private byte[] incompleteUtf8 = new byte[0];

        private Session(Path file, PrintStream console, String source) {
            this.file = file;
            this.console = console;
            this.source = source;
            currentFile = file;
            failed = file == null;
        }

        /** Copia bytes UTF-8 sem recodificar fragmentos de caracteres multibyte. */
        private synchronized void write(byte[] bytes, int offset, int length) {
            console.write(bytes, offset, length);
            if (suppressCapture.get() || failed || length == 0) return;
            var output = new ByteArrayOutputStream();
            for (int index = offset; index < offset + length; index++) {
                if (lineStart) {
                    output.writeBytes(("[" + Instant.now() + "] [stderr] ")
                            .getBytes(StandardCharsets.UTF_8));
                    lineStart = false;
                }
                output.write(bytes[index]);
                if (bytes[index] == '\n') lineStart = true;
            }
            append(output.toByteArray());
        }

        /** Escreve um bloco completo; a saída usa o console original para evitar duplicação. */
        private synchronized void writeReport(String origin, Throwable error, boolean printConsole) {
            var text = new StringWriter();
            var writer = new PrintWriter(text);
            writer.println("[" + Instant.now() + "] [" + origin + "]");
            if (error != null) error.printStackTrace(writer);
            writer.flush();
            String report = text.toString();
            if (printConsole) console.print(report);
            if (!lineStart) report = "\n" + report;
            append(report.getBytes(StandardCharsets.UTF_8));
            lineStart = true;
        }

        /**
         * Acrescenta bytes e abre partes de até 4 MiB sem apagar relatórios anteriores.
         * Caracteres UTF-8 permanecem completos dentro de cada parte, inclusive em escritas fragmentadas.
         */
        private void append(byte[] bytes) {
            if (failed) return;
            try {
                byte[] data = new byte[incompleteUtf8.length + bytes.length];
                System.arraycopy(incompleteUtf8, 0, data, 0, incompleteUtf8.length);
                System.arraycopy(bytes, 0, data, incompleteUtf8.length, bytes.length);
                int complete = completeUtf8Length(data);
                incompleteUtf8 = Arrays.copyOfRange(data, complete, data.length);
                int offset = 0;
                while (offset < complete) {
                    initializePart();
                    int length = Math.min(complete - offset, MAX_REPORT_BYTES - writtenBytes);
                    if (offset + length < complete) {
                        while (length > 0 && (data[offset + length] & 0xc0) == 0x80) length--;
                    }
                    if (length == 0) {
                        nextPart();
                        continue;
                    }
                    try (var output = Files.newOutputStream(currentFile, StandardOpenOption.APPEND)) {
                        output.write(data, offset, length);
                    }
                    writtenBytes += length;
                    offset += length;
                    if (offset < complete) nextPart();
                }
            } catch (IOException | SecurityException error) {
                failed = true;
                incompleteUtf8 = new byte[0];
                console.println("[Spirit Gate] Não foi possível gravar o TXT de erros em " + currentFile
                        + ": " + error.getMessage() + ". O console continua ativo.");
            }
        }

        /** Cria cada parte com seu próprio cabeçalho legível e identifica a sessão original. */
        private void initializePart() throws IOException {
            if (initialized) return;
            Files.createDirectories(currentFile.getParent());
            String header = "Spirit Gate | relatório local de erros\nSessão: " + file.getFileName()
                    + "\nAbertura: " + Instant.now() + "\nPID: " + ProcessHandle.current().pid()
                    + "\nComponente: " + source + "\nParte: " + part + "\nCodificação: UTF-8\n\n";
            byte[] bytes = header.getBytes(StandardCharsets.UTF_8);
            Files.write(currentFile, bytes, StandardOpenOption.CREATE_NEW);
            writtenBytes = bytes.length;
            initialized = true;
        }

        /** Seleciona um novo nome na mesma sessão sem renomear ou sobrescrever arquivos. */
        private void nextPart() {
            part++;
            String name = file.getFileName().toString();
            currentFile = file.resolveSibling(name.substring(0, name.length() - 4) + "-parte-" + part + ".txt");
            initialized = false;
        }

        /** Retém no máximo os três bytes finais de um caractere que ainda não terminou. */
        private static int completeUtf8Length(byte[] bytes) {
            if (bytes.length == 0) return 0;
            int leading = bytes.length - 1;
            while (leading > 0 && (bytes[leading] & 0xc0) == 0x80) leading--;
            int first = bytes[leading] & 0xff;
            int expected = first < 0x80 ? 1 : (first & 0xe0) == 0xc0 ? 2 : (first & 0xf0) == 0xe0 ? 3 : 4;
            return bytes.length - leading < expected ? leading : bytes.length;
        }
    }

    /** Tee que mantém o stderr original aberto mesmo se uma biblioteca fechar System.err. */
    private static final class ReportStream extends PrintStream {
        private ReportStream(Session session) {
            super(new OutputStream() {
                @Override public void write(int value) {
                    byte[] bytes = {(byte) value};
                    session.write(bytes, 0, bytes.length);
                }
                @Override public void write(byte[] bytes, int offset, int length) {
                    session.write(bytes, offset, length);
                }
                @Override public void flush() { session.console.flush(); }
                @Override public void close() { flush(); }
            }, true, StandardCharsets.UTF_8);
        }

        @Override public void close() { flush(); }
    }
}
