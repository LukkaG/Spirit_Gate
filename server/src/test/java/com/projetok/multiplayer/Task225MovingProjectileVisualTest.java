package com.projetok.multiplayer;

import com.projetok.rpg.Task225ProjectileTrace;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import static org.junit.jupiter.api.Assertions.*;

/** Evidência raster e medições do A-03 com os clips e o desenho reais, sem alterar o contrato de rede. */
class Task225MovingProjectileVisualTest {
    private static final String HASH = "a".repeat(64);
    private static final Color SERVER = new Color(255, 96, 194), CLIENT = new Color(255, 218, 72), TARGET = new Color(91, 238, 151);

    @Test
    @Timeout(15)
    void lateralTargetReproducesIntermediateDivergenceAndTerminalEventDrawsTheRealContact() throws Exception {
        var trace = Task225ProjectileTrace.run();
        var root = Path.of("..").toAbsolutePath().normalize();
        var folder = Files.createDirectories(root.resolve("docs/skills-preview-task225"));
        var clock = new AtomicLong();
        var view = new NetworkSkillPresentation(clock::get);
        view.clips(NetworkSkillPresentation.load(root));
        var probe = new NetworkSkillPresentation(clock::get);
        probe.clips(Map.of("water_1", marker(), "water_1.travel", marker(), "water_1.impact", marker()));
        var csv = new StringBuilder("tick,millis,caster_x,caster_y,target_x,target_y,server_x,server_y,client_x,client_y,error_world,enemy_hp,contact\n");
        var json = new StringBuilder("{\n  \"scenario\":\"water_1 lateral target +5 units/tick after tick 2\",\n  \"networkCapture\":false,\n  \"authoritativeTrace\":\"test-only reflection of Projectile.point\",\n  \"samples\":[\n");
        double maximumError = 0;
        var selected = new LinkedHashSet<>(List.of(0L, 4L, 8L, 10L, trace.impact().tick()));
        var serverHistory = new ArrayList<Task225ProjectileTrace.Point>();
        double terminalError = Double.NaN;
        for (var sample : trace.samples()) {
            clock.set(Math.round(sample.tick() * 1_000.0 / 30));
            var actors = actors(sample);
            var state = state(sample.tick(), sample.events());
            view.actors(actors); probe.actors(actors);
            view.update(state); probe.update(state);
            var visualPoint = visiblePoint(paint(probe));
            assertNotNull(visualPoint, "O desenho real do cliente precisa produzir uma posição mensurável");
            double error = Math.hypot(sample.projectile().x() - visualPoint.x(), sample.projectile().y() - visualPoint.y());
            if (sample.contact()) terminalError = error; else maximumError = Math.max(maximumError, error);
            csv.append(String.format(Locale.ROOT, "%d,%d,%.3f,%.3f,%.3f,%.3f,%.3f,%.3f,%.3f,%.3f,%.3f,%d,%s%n",
                    sample.tick(), clock.get(), sample.caster().x(), sample.caster().y(), sample.target().x(), sample.target().y(),
                    sample.projectile().x(), sample.projectile().y(), visualPoint.x(), visualPoint.y(), error, sample.hp(), sample.contact()));
            if (sample.tick() > 0) json.append(",\n");
            json.append(String.format(Locale.ROOT, "    {\"tick\":%d,\"millis\":%d,\"caster\":[%.3f,%.3f],\"target\":[%.3f,%.3f],\"server\":[%.3f,%.3f],\"client\":[%.3f,%.3f],\"error\":%.3f,\"hp\":%d,\"contact\":%s}",
                    sample.tick(), clock.get(), sample.caster().x(), sample.caster().y(), sample.target().x(), sample.target().y(),
                    sample.projectile().x(), sample.projectile().y(), visualPoint.x(), visualPoint.y(), error, sample.hp(), sample.contact()));
            serverHistory.add(sample.projectile());
            var drawn = paint(view);
            int active = view.activeCount();
            view.update(state);
            assertEquals(active, view.activeCount(), "A repetição do frame não reinicia nem duplica a animação");
            assertArrayEquals(((DataBufferInt) drawn.getRaster().getDataBuffer()).getData(),
                    ((DataBufferInt) paint(view).getRaster().getDataBuffer()).getData(), "Mesmos relógio e eventos devem produzir os mesmos pixels");
            if (selected.contains(sample.tick())) ImageIO.write(annotated(view, sample, visualPoint, error, serverHistory), "png",
                    folder.resolve(String.format(Locale.ROOT, "voo-%02d%s.png", sample.tick(), sample.contact() ? "-contato" : "")).toFile());
        }
        assertTrue(maximumError > 20, "O cenário deve reproduzir uma divergência intermediária mensurável");
        assertTrue(terminalError <= 1.5, "O impacto real deve desenhar a coordenada terminal, admitindo arredondamento raster");
        assertTrue(trace.samples().get(trace.samples().size() - 1).hp() < 1_000);
        assertEquals(2, trace.samples().get(trace.samples().size() - 1).events().size(), "Somente lançamento e contato");
        clock.addAndGet(2_000);
        assertEquals(0, view.activeCount());
        view.update(state(trace.impact().tick(), trace.samples().get(trace.samples().size() - 1).events()));
        assertEquals(0, view.activeCount(), "Evento retido no frame não ressuscita o impacto já expirado");
        json.append(String.format(Locale.ROOT, "\n  ],\n  \"maximumIntermediateError\":%.3f,\n  \"terminalRasterError\":%.3f,\n  \"impactTick\":%d,\n  \"impact\":[%.3f,%.3f]\n}\n",
                maximumError, terminalError, trace.impact().tick(), trace.impact().x(), trace.impact().y()));
        Files.writeString(folder.resolve("trajetoria.csv"), csv, StandardCharsets.UTF_8);
        Files.writeString(folder.resolve("trajetoria.json"), json, StandardCharsets.UTF_8);
        System.out.printf(Locale.ROOT, "TASK225 A-03 maximumIntermediateError=%.3f terminalRasterError=%.3f impactTick=%d impact=(%.3f,%.3f)%n",
                maximumError, terminalError, trace.impact().tick(), trace.impact().x(), trace.impact().y());
    }

    /** Marcador conhecido permite medir o centro que o renderer realmente desenhou, sem duplicar sua fórmula. */
    private static NetworkSkillPresentation.Clip marker() {
        var bitmap = new BufferedImage(5, 5, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 5; y++) for (int x = 0; x < 5; x++) bitmap.setRGB(x, y, 0xff00ffff);
        return new NetworkSkillPresentation.Clip(bitmap, 5, 5, 1, 1, 1_000, 5, 5, 2, 2);
    }

    /** Converte o centro raster ao plano de apoio usado pela colisão, removendo somente o offset visual conhecido. */
    private static Task225ProjectileTrace.Point visiblePoint(BufferedImage image) {
        int left = image.getWidth(), top = image.getHeight(), right = -1, bottom = -1;
        for (int y = 0; y < image.getHeight(); y++) for (int x = 0; x < image.getWidth(); x++)
            if (image.getRGB(x, y) >>> 24 != 0) { left = Math.min(left, x); top = Math.min(top, y); right = Math.max(right, x); bottom = Math.max(bottom, y); }
        return right < 0 ? null : new Task225ProjectileTrace.Point((left + right) / 2f, (top + bottom) / 2f + NetworkSkillPresentation.PROJECTILE_CENTER_OFFSET);
    }

    private static Protocol.State actors(Task225ProjectileTrace.Sample sample) {
        return new Protocol.State(sample.tick(), List.of(
                new Protocol.Actor(1, "Lançador", sample.caster().x(), sample.caster().y(), 100, 100, 1_000, 0, false),
                new Protocol.Actor(1_000_100, "Alvo controlado", sample.target().x(), sample.target().y(), sample.hp(), 1_000, 0, 0, true)));
    }

    private static SkillProtocol.State state(long tick, List<com.projetok.rpg.RpgSkillEngine.Event> events) {
        return new SkillProtocol.State(HASH, tick, events.stream().map(event -> new SkillProtocol.Event(event.sequence(), event.tick(), event.player(),
                event.skill(), event.fromX(), event.fromY(), event.x(), event.y())).toList(), List.of(0, 0, 0, 0, 0), 0, false);
    }

    private static BufferedImage paint(NetworkSkillPresentation view) {
        var image = new BufferedImage(960, 640, BufferedImage.TYPE_INT_ARGB);
        var graphics = image.createGraphics();
        try { view.draw(graphics); } finally { graphics.dispose(); }
        return image;
    }

    /** Sobrepõe apenas instrumentos de diagnóstico ao desenho dos clips originais; nenhuma linha é um pacote novo. */
    private static BufferedImage annotated(NetworkSkillPresentation view, Task225ProjectileTrace.Sample sample,
                                          Task225ProjectileTrace.Point visual, double error,
                                          List<Task225ProjectileTrace.Point> history) {
        var image = new BufferedImage(960, 640, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        try {
            g.setColor(new Color(12, 22, 39)); g.fillRect(0, 0, 960, 640);
            g.setColor(new Color(25, 42, 61));
            for (int x = 0; x < 960; x += 32) g.drawLine(x, 110, x, 540);
            for (int y = 112; y < 540; y += 32) g.drawLine(0, y, 960, y);
            g.setColor(new Color(255, 218, 72, 90));
            g.setStroke(new BasicStroke(1, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1, new float[] {7, 5}, 0));
            g.drawLine(140, 272, 340, 272);
            g.setStroke(new BasicStroke(2)); g.setColor(SERVER);
            for (int i = 1; i < history.size(); i++) g.drawLine(Math.round(history.get(i - 1).x()), Math.round(history.get(i - 1).y() - 28),
                    Math.round(history.get(i).x()), Math.round(history.get(i).y() - 28));
            g.setColor(new Color(79, 191, 246)); g.fillOval(130, 262, 20, 20);
            g.setColor(TARGET); g.drawOval(Math.round(sample.target().x()) - 19, Math.round(sample.target().y()) - 47, 38, 38);
            view.draw(g);
            mark(g, sample.projectile(), SERVER, 11); mark(g, visual, CLIENT, 7);
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 20)); g.setColor(Color.WHITE);
            g.drawString("Spirit Gate | TASK-225 | A-03", 28, 36);
            g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 16));
            g.drawString(String.format(Locale.ROOT, "tick %d | %s | erro %.2f unidades", sample.tick(), sample.contact() ? "CONTATO AUTORITATIVO" : "VOO", error), 28, 65);
            g.drawString("Clips originais desenhados por NetworkSkillPresentation; cenário técnico controlado.", 28, 90);
            g.setColor(SERVER); g.drawString("Rosa: ponto e caminho reais do servidor (sonda do teste)", 28, 565);
            g.setColor(CLIENT); g.drawString("Amarelo: centro efetivamente desenhado pelo cliente; reta inicial tracejada", 28, 590);
            g.setColor(TARGET); g.drawString("Verde: alvo lateral (+5 unidades/tick a partir do terceiro tick)", 28, 615);
        } finally { g.dispose(); }
        return image;
    }

    private static void mark(Graphics2D g, Task225ProjectileTrace.Point point, Color color, int radius) {
        int x = Math.round(point.x()), y = Math.round(point.y()) - NetworkSkillPresentation.PROJECTILE_CENTER_OFFSET;
        g.setColor(color); g.drawOval(x - radius, y - radius, radius * 2, radius * 2);
        g.drawLine(x - radius - 3, y, x + radius + 3, y); g.drawLine(x, y - radius - 3, x, y + radius + 3);
    }
}
