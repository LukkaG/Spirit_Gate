package com.projetok.multiplayer;

import com.projetok.rpg.RpgSkillCatalog;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Extensão de habilidades negociada como aplicação 7; os frames V1–V6 permanecem intactos. */
public final class SkillProtocol {
    /** Intenção sem dano, custo ou domínio escolhidos pelo cliente; slot pertence ao domínio do servidor. */
    public record Request(long sequence, int slot, int targetId, long generation, float x, float y)
            implements Protocol.Message {
        public Request {
            if (sequence < 1 || sequence > 1_000_000_000_000L || slot < 1 || slot > 5
                    || targetId < 0 || targetId >= 2_000_000 || generation < 0
                    || generation > 1_000_000_000_000L || !point(x, y))
                throw new IllegalArgumentException("Intenção de habilidade inválida");
        }
    }

    /** Animação confirmada, independente do cálculo autoritativo de colisão, dano e controle. */
    public record Event(long sequence, long tick, int player, String skill,
                        float fromX, float fromY, float x, float y) {
        public Event {
            if (sequence < 1 || tick < 0 || tick > 1_000_000_000L || player < 1 || player > 1_000_000
                    || !point(fromX, fromY) || !point(x, y))
                throw new IllegalArgumentException("Evento de habilidade inválido");
            RpgSkillCatalog.get(skill);
        }
    }

    /** Recargas e eventos da região; sentryShots conserva o campo wire legado com os segundos ativos (4..1). */
    public record State(String hash, long tick, List<Event> events, List<Integer> cooldowns,
                        int sentryShots, boolean windCheckpoint) implements Protocol.Message {
        public State {
            Objects.requireNonNull(hash); events = List.copyOf(events); cooldowns = List.copyOf(cooldowns);
            if (!hash.matches("[a-f0-9]{64}") || tick < 0 || tick > 1_000_000_000L
                    || events.size() > 16 || cooldowns.size() != 5
                    || cooldowns.stream().anyMatch(c -> c < 0 || c > 36_000)
                    || sentryShots < 0 || sentryShots > 5)
                throw new IllegalArgumentException("Estado de habilidades inválido");
            long previous = 0;
            for (var event : events) {
                if (event.sequence() <= previous || event.tick() > tick)
                    throw new IllegalArgumentException("Eventos de habilidades fora de ordem");
                previous = event.sequence();
            }
        }
    }

    private SkillProtocol() { }
    private static boolean point(float x, float y) {
        return Float.isFinite(x) && Float.isFinite(y) && x >= 0 && x <= 960 && y >= 0 && y <= 640;
    }

    /** Serializa apenas os novos campos; o envelope e o limite de frame continuam no Protocol. */
    static void writeState(DataOutputStream out, State state) throws IOException {
        out.writeUTF(state.hash()); out.writeLong(state.tick()); out.writeByte(state.events().size());
        for (var event : state.events()) {
            out.writeLong(event.sequence()); out.writeLong(event.tick()); out.writeInt(event.player());
            out.writeUTF(event.skill()); out.writeFloat(event.fromX()); out.writeFloat(event.fromY());
            out.writeFloat(event.x()); out.writeFloat(event.y());
        }
        for (int cooldown : state.cooldowns()) out.writeInt(cooldown);
        out.writeByte(state.sentryShots()); out.writeBoolean(state.windCheckpoint());
    }

    /** Recusa contagens e textos excessivos antes de alocar coleções ou construir DTOs. */
    static State readState(DataInputStream in) throws IOException {
        String hash = ascii(in, 64); long tick = in.readLong(); int count = in.readUnsignedByte();
        if (count > 16) throw new IOException("Eventos de habilidade excessivos");
        var events = new ArrayList<Event>(count);
        for (int i = 0; i < count; i++) events.add(new Event(in.readLong(), in.readLong(), in.readInt(),
                ascii(in, 31), in.readFloat(), in.readFloat(), in.readFloat(), in.readFloat()));
        var cooldowns = new ArrayList<Integer>(5);
        for (int i = 0; i < 5; i++) cooldowns.add(in.readInt());
        return new State(hash, tick, events, cooldowns, in.readUnsignedByte(), in.readBoolean());
    }

    private static String ascii(DataInputStream in, int maximum) throws IOException {
        int count = in.readUnsignedShort();
        if (count < 1 || count > maximum) throw new IOException("Chave de habilidade excessiva");
        byte[] data = in.readNBytes(count);
        if (data.length != count) throw new EOFException();
        for (byte value : data) if (value < 32 || value > 126) throw new IOException("Chave não ASCII");
        return new String(data, StandardCharsets.US_ASCII);
    }
}
