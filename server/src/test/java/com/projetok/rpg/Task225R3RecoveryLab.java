package com.projetok.rpg;

import java.io.IOException;
import java.util.*;
import java.util.function.Predicate;

/** Protótipo em memória, exclusivo de teste; não é autenticação nem restauração do multiplayer. */
final class Task225R3RecoveryLab {
    static final Set<String> CLOCKS = Set.of("skill_1", "skill_2", "skill_3", "skill_4", "skill_5",
            "attack", "shunpo", "potion", "portal");

    /** Objetivo sintético versionado, sem aplicar eventos ou executar recompensa ao restaurar. */
    record Quest(String status, List<Integer> objectives, Set<String> receipts) {
        Quest {
            objectives = List.copyOf(objectives); receipts = Set.copyOf(receipts);
            if (!Set.of("ACTIVE", "READY", "COMPLETED").contains(status) || objectives.size() > 8
                    || objectives.stream().anyMatch(n -> n < 0 || n > 100) || receipts.size() > 100)
                throw new IllegalArgumentException("Objetivo fora do contrato sintético");
        }
    }

    /** Envelope candidato; RpgSave é apenas a parte suportada do estado, nunca o documento online completo. */
    record State(int schema, RpgSave character, String content, long worldEpoch, long capturedTick,
                 int energy, int maximumEnergy, Map<String, Integer> attributes,
                 Map<String, Quest> authoredQuests, Map<String, Long> readyAt, Set<String> rewards) {
        State {
            Objects.requireNonNull(character);
            attributes = Map.copyOf(attributes); authoredQuests = Map.copyOf(authoredQuests);
            readyAt = Map.copyOf(readyAt); rewards = Set.copyOf(rewards);
            if (schema != 1 || content == null || !content.matches("[a-f0-9]{64}")
                    || worldEpoch < 1 || capturedTick < 0 || capturedTick > 1_000_000_000L
                    || energy < 0 || energy > maximumEnergy || maximumEnergy < 1 || maximumEnergy > 100_000
                    || attributes.size() > 8 || attributes.values().stream().anyMatch(n -> n < 0 || n > 10000)
                    || authoredQuests.size() > 64 || rewards.size() > 2048 || !readyAt.keySet().equals(CLOCKS)
                    || readyAt.values().stream().anyMatch(n -> n < 0 || n > 1_010_000_000L))
                throw new IllegalArgumentException("Envelope sintético inválido/incompleto");
        }

        /** A recarga pertence ao relógio do owner e não é reiniciada durante readmissão. */
        long remaining(String action, long serverTick) { return Math.max(0, readyAt.get(action) - serverTick); }
    }

    /** Estado transitório deliberadamente ausente do envelope recuperável. */
    record Transient(boolean intangible, int projectiles, int pendingCommands, int animations) {
        static final Transient CLEAN = new Transient(false, 0, 0, 0);
    }

    /** Posse opaca concedida pelo controlador confiável local; nome/IP não participam da validação. */
    static final class Credential {
        private final UUID character;
        private final Object authority;
        private Credential(UUID character, Object authority) { this.character = character; this.authority = authority; }
    }

    /** Handle de admissão vinculado à instância e à geração; nunca serializado ou recebido de socket. */
    static final class Handle {
        private final UUID character;
        private final long generation;
        private final Object authority;
        private Handle(UUID character, long generation, Object authority) {
            this.character = character; this.generation = generation; this.authority = authority;
        }
    }

    /** Ticket opaco com validade e geração de retirada, entregue apenas ao harness local. */
    static final class Ticket {
        private final Object authority;
        private final UUID character;
        private final long generation, expires;
        private Ticket(Object authority, UUID character, long generation, long expires) {
            this.authority = authority; this.character = character; this.generation = generation; this.expires = expires;
        }
    }

    record Enrollment(Credential credential, Handle handle) { }
    record Receipt(UUID operation, UUID character, long generation, State state) { }
    enum Fault { NONE, BEFORE_COMMIT, AFTER_COMMIT }

    /** Registro retido pelo servidor do laboratório; o consumidor nunca envia snapshot de recuperação. */
    private static final class Entry {
        final Credential credential;
        State state;
        Handle active;
        Ticket ticket;
        Receipt receipt;
        Transient transientState;
        long generation = 1;
        Entry(Credential credential, State state, Handle active, Transient transientState) {
            this.credential = credential; this.state = state; this.active = active; this.transientState = transientState;
        }
    }

    private final Object authority = new Object();
    private final Map<UUID, Entry> entries = new HashMap<>();
    private long tick;

    /** Admite até dois personagens sintéticos, com capacidade local fixa igual à do transporte. */
    synchronized Enrollment enroll(State state, Transient transientState) {
        UUID id = state.character().identity().id();
        if (entries.size() >= 2 || entries.containsKey(id)) throw new IllegalStateException("Registro ocupado");
        clock(state.capturedTick());
        var credential = new Credential(id, authority);
        var handle = new Handle(id, 1, authority);
        entries.put(id, new Entry(credential, state, handle, Objects.requireNonNull(transientState)));
        return new Enrollment(credential, handle);
    }

    /** Congela o estado já autoritativo, libera a participação e elimina apenas transitórios. */
    synchronized Ticket withdraw(Handle handle, long now, long ttl) {
        var entry = active(handle); clock(now);
        if (ttl < 1 || ttl > 1800 || now + ttl > 1_000_000_000L) throw new IllegalArgumentException("Expiração inválida");
        var ticket = new Ticket(authority, handle.character, entry.generation, now + ttl);
        entry.active = null; entry.transientState = Transient.CLEAN; entry.ticket = ticket; entry.receipt = null;
        return ticket;
    }

    /** Valida tudo antes do commit único; falhas de spawn/contexto não consomem o ticket. */
    synchronized Handle recover(Credential credential, Ticket ticket, UUID operation, long now,
                                String content, long worldEpoch, String region, Predicate<State> spawn, Fault fault)
            throws IOException {
        var entry = authenticated(credential);
        if (ticket == null || ticket.authority != authority || entry.ticket != ticket
                || !ticket.character.equals(credential.character) || ticket.generation != entry.generation
                || entry.active != null) throw new SecurityException("Recuperação inválida/consumida");
        Objects.requireNonNull(operation); Objects.requireNonNull(spawn); Objects.requireNonNull(fault); clock(now);
        if (now >= ticket.expires) throw new IOException("Recuperação expirada; estado retido para decisão local");
        var state = entry.state;
        if (!state.content().equals(content) || state.worldEpoch() != worldEpoch
                || !state.character().region().equals(region)) throw new IOException("Contexto mudou");
        if (!spawn.test(state)) throw new IOException("Spawn bloqueado");
        if (fault == Fault.BEFORE_COMMIT) throw new IOException("Falha sintética antes do commit");
        var next = new Handle(credential.character, entry.generation + 1, authority);
        var receipt = new Receipt(operation, credential.character, next.generation, state);
        // Publicação indivisível sob o mesmo monitor: não soma inventário, não repete eventos nem recompensas.
        entry.generation = next.generation; entry.active = next; entry.ticket = null;
        entry.transientState = Transient.CLEAN; entry.receipt = receipt;
        if (fault == Fault.AFTER_COMMIT) throw new IOException("Resposta perdida após commit sintético");
        return next;
    }

    /** Reconciliação de resposta perdida devolve recibo, nunca uma segunda admissão/novo handle. */
    synchronized Receipt receipt(Credential credential, UUID operation) {
        var entry = authenticated(credential);
        if (entry.receipt == null || !entry.receipt.operation().equals(operation))
            throw new SecurityException("Recibo ausente");
        return entry.receipt;
    }

    /** Leitura restrita à posse ativa da geração atual; handles anteriores deixam de funcionar. */
    synchronized State snapshot(Handle handle) { return active(handle).state; }

    /** Consulta transitórios para comprovar que a fase antiga não é transportada para a readmissão. */
    synchronized Transient transientState(Handle handle) { return active(handle).transientState; }

    /** Quantifica admissões locais, sem abrir transporte ou substituir o controle de peers. */
    synchronized int activeCount() { return (int) entries.values().stream().filter(e -> e.active != null).count(); }

    /** Impede reutilizar prova de outra instância/personagem, mesmo com nome e aparência iguais. */
    private Entry authenticated(Credential credential) {
        if (credential == null || credential.authority != authority) throw new SecurityException("Identidade incorreta");
        var entry = entries.get(credential.character);
        if (entry == null || entry.credential != credential) throw new SecurityException("Posse incorreta");
        return entry;
    }

    /** Handle deve ser o objeto exato emitido na admissão corrente, além da geração e autoridade. */
    private Entry active(Handle handle) {
        if (handle == null || handle.authority != authority) throw new SecurityException("Handle incorreto");
        var entry = entries.get(handle.character);
        if (entry == null || entry.active != handle || entry.generation != handle.generation)
            throw new SecurityException("Admissão antiga");
        return entry;
    }

    /** Só existe relógio de servidor no ensaio; não se aceita retrocesso nem época reiniciada. */
    private void clock(long now) {
        if (now < tick || now > 1_000_000_000L) throw new IllegalArgumentException("Relógio inválido");
        tick = now;
    }
}
