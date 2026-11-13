package com.projetok.rpg.recovery;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Candidato imutável a estado recuperável. Não autoriza posse, executa gameplay ou instala estado.
 * Blocos nulos permanecem ausentes para diagnóstico; nunca são preenchidos com valores padrão.
 * Coleções presentes são copiadas, limitadas e não admitem elementos nulos.
 */
public record RecoverableCharacterSnapshot(
        CharacterId characterId, SchemaVersion schemaVersion, StateRevision stateRevision,
        ContentHash contentHash, RulesVersion rulesVersion, Capture capture, Identity identity,
        Progression progression, Attributes attributes, Inventory inventory, List<Equipment> equipment,
        Wallet wallet, List<QuestProgress> quests, List<RewardReceipt> receipts,
        List<EventWatermark> watermarks, List<PendingEntitlement> pendingLoot,
        Resources resources, Cooldowns cooldowns, Location location, Restrictions restrictions) {

    // Orçamentos técnicos deste formato experimental, sem política de retenção ou balanceamento.
    public static final int MAX_ENTRIES = 100, MAX_QUESTS = 64, MAX_OBJECTIVES = 8;
    public static final int MAX_RECEIPTS = 2048, MAX_WATERMARKS = 2048, MAX_PENDING = 256;
    public static final int MAX_RESTRICTIONS = 64, MAX_TEXT = 100;

    /** Copia os blocos de coleção sem converter listas em mapas ou ocultar duplicatas. */
    public RecoverableCharacterSnapshot {
        equipment = copy(equipment, 4);
        quests = copy(quests, MAX_QUESTS);
        receipts = copy(receipts, MAX_RECEIPTS);
        watermarks = copy(watermarks, MAX_WATERMARKS);
        pendingLoot = copy(pendingLoot, MAX_PENDING);
    }

    /** Identificador do personagem, separado de qualquer inteiro de conexão. */
    public record CharacterId(UUID value) {
        /** Lê somente a forma canônica, recusando a forma abreviada aceita por UUID.fromString. */
        public static CharacterId parse(String text) {
            if (text == null || !text.matches("[a-fA-F0-9]{8}(-[a-fA-F0-9]{4}){3}-[a-fA-F0-9]{12}"))
                throw new IllegalArgumentException("UUID de personagem inválido");
            return new CharacterId(UUID.fromString(text));
        }
    }
    /** Versão do documento; não é versão de protocolo ou conteúdo. */
    public record SchemaVersion(int value) { }
    /** Revisão lógica do estado; monotonicidade exige comparação com contexto anterior. */
    public record StateRevision(long value) { }
    /** Hash de conteúdo esperado, sem conceder autoridade por conhecer seu valor. */
    public record ContentHash(String value) {
        /** Limita a representação antes da validação semântica. */
        public ContentHash { value = text(value); }
    }
    /** Versão de regras escolhida pelo chamador confiável. */
    public record RulesVersion(String value) {
        /** Limita o identificador da regra. */
        public RulesVersion { value = text(value); }
    }
    /** Época de mundo, distinta da geração de mob e da revisão documental. */
    public record WorldEpoch(long value) { }
    /** Tick de origem, sem atribuir-lhe significado de relógio durável. */
    public record SourceTick(long value) { }
    /** Origem temporal declarada; nenhuma opção implementa avanço offline. */
    public enum ClockOrigin { MONOTONIC_WORLD_TICKS, UNSPECIFIED }
    /** Captura declarada pelo servidor; validade temporal depende de contexto externo. */
    public record Capture(WorldEpoch worldEpoch, SourceTick sourceTick, Instant capturedAt,
                          ClockOrigin clockOrigin, int clockVersion) { }
    /** Identidade e IDs cosméticos, sem credenciais ou referências executáveis. */
    public record Identity(String name, String race, String appearance, String hair, String clothing) {
        /** Limita strings sem normalizar silenciosamente o candidato. */
        public Identity { name=text(name); race=text(race); appearance=text(appearance); hair=text(hair); clothing=text(clothing); }
    }
    /** Progressão exata e metadados de reset; nível será derivado da XP. */
    public record Progression(long xp, String domain, long domainXpHalfUnits,
                              LocalDate lastReset, boolean resetQuestComplete) {
        /** Limita o identificador; não concede XP ou domínio. */
        public Progression { domain=text(domain); }
    }
    /** Modo de orçamento conhecido ou ainda sem política aprovada. */
    public enum AttributeMode { MANUAL_SESSION, UNSPECIFIED }
    /** Investimentos e checkpoint de concessão; não contém um segundo nível autoritativo. */
    public record Attributes(AttributeMode mode, int strength, int intelligence, int agility,
                             int energy, int unspentPoints, int grantedThroughLevel) { }
    /** Bolsa candidata; cada índice permanece explícito até verificar duplicatas. */
    public record Inventory(int capacity, List<Slot> slots) {
        /** Copia a coleção, mantendo ausências para o validador. */
        public Inventory { slots=copy(slots, MAX_ENTRIES); }
    }
    /** Pilha de item, sem inventar identidade de exemplar. */
    public record Slot(int index, String itemId, int quantity) {
        /** Limita o identificador do catálogo. */
        public Slot { itemId=text(itemId); }
    }
    /** Equipamento já separado da bolsa; validar não executa equip ou unequip. */
    public record Equipment(String slot, String itemId) {
        /** Limita os identificadores. */
        public Equipment { slot=text(slot); itemId=text(itemId); }
    }
    /** Saldo candidato; long permite diagnosticar overflow da faixa int do runtime. */
    public record Wallet(long coins) { }
    /** Catálogo builtin e autoria são espaços de nomes diferentes. */
    public enum QuestNamespace { BUILTIN, AUTHORED }
    /** Identidade versionada de uma missão, sem carregar definição executável. */
    public record QuestKey(QuestNamespace namespace, String id, String version) {
        /** Limita a identidade da definição. */
        public QuestKey { id=text(id); version=text(version); }
    }
    /** Estado de entrega, separado dos eventos e do recibo de pagamento. */
    public enum QuestStatus { ACTIVE, READY, DELIVERED }
    /** Contador de objetivo com identidade explícita dentro da definição. */
    public record ObjectiveProgress(int index, String type, String target, int count) {
        /** Limita identificadores de objetivo. */
        public ObjectiveProgress { type=text(type); target=text(target); }
    }
    /** Progresso de missão; recibo de entrega só é exigido quando entregue. */
    public record QuestProgress(QuestKey key, QuestStatus status,
                                List<ObjectiveProgress> objectives, String deliveryReceiptId) {
        /** Copia profundamente a coleção de records imutáveis. */
        public QuestProgress { objectives=copy(objectives, MAX_OBJECTIVES); deliveryReceiptId=text(deliveryReceiptId); }
    }
    /** Tipos de concessão estruturalmente conhecidos, sem executar pagamento. */
    public enum RewardKind { QUEST, LOOT }
    /** Evidência declarada de concessão, sem prometer idempotência durável. */
    public record RewardReceipt(String id, RewardKind kind, CharacterId beneficiary, String source,
                                WorldEpoch worldEpoch, StateRevision confirmedRevision, String resultHash) {
        /** Limita chave, origem e fingerprint do resultado. */
        public RewardReceipt { id=text(id); source=text(source); resultHash=text(resultHash); }
    }
    /** Tipos de evento processado; não são eventos antigos a reproduzir. */
    public enum QuestEventType { KILL, TALK, COLLECT, VISIT }
    /** Marca de sequência processada por origem e época, separada do recibo econômico. */
    public record EventWatermark(QuestEventType type, String source, WorldEpoch worldEpoch, long sequence) {
        /** Limita o nome da origem. */
        public EventWatermark { source=text(source); }
    }
    /** Direito econômico pendente, referenciando concessão de loot já registrada. */
    public record PendingEntitlement(String id, String receiptId, String itemId, int quantity) {
        /** Limita identidades sem criar ou somar itens. */
        public PendingEntitlement { id=text(id); receiptId=text(receiptId); itemId=text(itemId); }
    }
    /** Vida declarada, validada contra o HP sem reviver no login. */
    public enum LifeState { ALIVE, DEAD }
    /** Recursos absolutos; energia não é normalizada para a HUD. */
    public record Resources(int hp, int energyAbsolute, double regenerationRemainder, LifeState lifeState) { }
    /** Canais sem confundir um slot com a identidade da técnica. */
    public enum CooldownChannel { BASIC_ATTACK, SHUNPO, POTION, DOMAIN_SKILL, TRAVEL }
    /** Deadline decimal exato no relógio declarado; não há conversão para double ou expiração. */
    public record CooldownDeadline(CooldownChannel channel, String skillId, BigDecimal readyAtTick) {
        /** Limita a chave da técnica e o orçamento decimal antes de qualquer comparação. */
        public CooldownDeadline {
            skillId=text(skillId);
            if (readyAtTick!=null && (readyAtTick.precision()>40 || readyAtTick.scale()<0 || readyAtTick.scale()>9))
                throw new IllegalArgumentException("Precisão de deadline fora do formato experimental");
        }
    }
    /** Bloco obrigatório de prazos; ausência não é convertida em recarga zero. */
    public record Cooldowns(int policyVersion, List<CooldownDeadline> deadlines) {
        /** Copia no máximo os nove canais atuais. */
        public Cooldowns { deadlines=copy(deadlines, 9); }
    }
    /** Posição candidata; não representa reserva ou aprovação de colisão. */
    public record Location(String regionId, float x, float y, String spawnRef) {
        /** Limita IDs, preservando spawnRef opcional. */
        public Location { regionId=text(regionId); spawnRef=text(spawnRef); }
    }
    /** Apenas descritores adversos; intangibilidade, dash e sentinela não são representáveis. */
    public enum RestrictionKind { ROOT, STUN, SLOW, BLEED, BURN }
    /** Grau de evidência da captura; UNKNOWN nunca significa ausência de penalidade. */
    public enum RestrictionAssessment { ASSESSED, UNKNOWN }
    /** Descritor restritivo sem pulso de dano, callback ou execução antiga. */
    public record Restriction(RestrictionKind kind, String source, BigDecimal untilTick, int policyVersion) {
        /** Limita a origem e a precisão, sem interpretar sua política de gameplay. */
        public Restriction {
            source=text(source);
            if (untilTick!=null && (untilTick.precision()>40 || untilTick.scale()<0 || untilTick.scale()>9))
                throw new IllegalArgumentException("Precisão de restrição fora do formato experimental");
        }
    }
    /** Bloco obrigatório de restrições; coleção vazia não substitui evidência externa. */
    public record Restrictions(RestrictionAssessment assessment, List<Restriction> entries) {
        /** Copia os descritores, com limite técnico explícito. */
        public Restrictions { entries=copy(entries, MAX_RESTRICTIONS); }
    }

    /** Impõe orçamento antes da cópia; null permanece ausência, nunca coleção vazia implícita. */
    private static <T> List<T> copy(List<T> values, int maximum) {
        if (values==null) return null;
        if (values.size()>maximum) throw new IllegalArgumentException("Coleção excede o formato experimental: "+maximum);
        return List.copyOf(values);
    }

    /** Limita strings sem truncar dados ou inferir valores ausentes. */
    private static String text(String value) {
        if (value!=null && value.length()>MAX_TEXT) throw new IllegalArgumentException("Texto excede "+MAX_TEXT);
        return value;
    }
}
