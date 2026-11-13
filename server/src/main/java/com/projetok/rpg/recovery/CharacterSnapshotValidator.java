package com.projetok.rpg.recovery;

import com.projetok.domain.combat.EnergyRules;
import com.projetok.domain.entity.component.StatsComponent;
import com.projetok.domain.progression.LevelingRules;
import com.projetok.rpg.*;
import java.math.BigDecimal;
import java.util.*;
import static com.projetok.rpg.recovery.RecoverableCharacterSnapshot.*;

/**
 * Valida cópias de dados sem I/O, relógio global, estado mutável compartilhado ou instalação.
 * Catálogos que leem recursos devem ser construídos pelo chamador antes de chamar validate.
 */
public final class CharacterSnapshotValidator {
    /** Códigos estáveis para diagnóstico, sem depender de mensagens de exceção de gameplay. */
    public enum Code {
        REQUIRED, SCHEMA, IDENTITY, REVISION, CONTEXT, CAPTURE, PROGRESSION, ATTRIBUTES,
        INVENTORY, DUPLICATE_SLOT, EQUIPMENT, WALLET, QUEST, DUPLICATE_QUEST, OBJECTIVE,
        RECEIPT, DUPLICATE_RECEIPT, WATERMARK, ENTITLEMENT, COOLDOWN, DEADLINE,
        RESOURCES, LOCATION, RESTRICTION
    }
    /** Validações que o contrato puro não pode concluir ou cuja política ainda não foi aprovada. */
    public enum PendingCheck {
        AUTHORIZATION, WORLD_PLACEMENT, DURABLE_COMMIT, CATALOGS, PREVIOUS_REVISION,
        CONTENT_CONTEXT, CAPTURE_CONTEXT, CLOCK_POLICY, ATTRIBUTE_POLICY,
        RESTRICTION_EVIDENCE, RESTRICTION_POLICY
    }
    /** Falha estruturada com caminho de campo, sem incluir o payload inteiro em mensagens. */
    public record Violation(Code code, String path) { }
    /** Resultado imutável; estrutura válida nunca é autorização para ativar personagem. */
    public record Result(List<Violation> violations, Set<PendingCheck> pendingChecks) {
        /** Impede alteração posterior da evidência de validação. */
        public Result { violations=List.copyOf(violations); pendingChecks=Set.copyOf(pendingChecks); }
        /** Consulta somente invariantes verificadas com os dados disponíveis. */
        public boolean structurallyValid() { return violations.isEmpty(); }
        /** Esta entrega não implementa nenhuma transição de ativação. */
        public boolean readyForActivation() { return false; }
    }

    /** Valida o candidato inteiro, preservando ausência de contexto como pendência explícita. */
    public Result validate(RecoverableCharacterSnapshot snapshot, SnapshotValidationContext context) {
        var check=new Check(context);
        check.validate(snapshot);
        return new Result(check.errors,check.pending);
    }

    /** Acumulador local de uma única chamada; nunca guarda referências de estado vivo. */
    private static final class Check {
        final SnapshotValidationContext context;
        final List<Violation> errors=new ArrayList<>();
        final Set<PendingCheck> pending=EnumSet.of(PendingCheck.AUTHORIZATION,
                PendingCheck.WORLD_PLACEMENT,PendingCheck.DURABLE_COMMIT,PendingCheck.CLOCK_POLICY);
        int level;
        Integer maximumEnergy;
        CharacterRaces.Definition race;

        /** Mantém o contexto apenas durante esta chamada. */
        Check(SnapshotValidationContext context) { this.context=context; }

        /** Registra uma falha determinística. */
        void error(Code code,String path) { errors.add(new Violation(code,path)); }
        /** Distingue bloco ausente de bloco presente vazio. */
        boolean required(Object value,String path) {
            if(value!=null)return true;
            error(Code.REQUIRED,path);return false;
        }
        /** Identificadores técnicos são limitados pelo DTO e livres de delimitadores ambíguos. */
        boolean id(String value) { return value!=null&&value.matches("[a-zA-Z0-9][a-zA-Z0-9_.-]{0,99}"); }
        /** Hashes de conteúdo/resultado têm representação hexadecimal canônica. */
        boolean hash(String value) { return value!=null&&value.matches("[a-f0-9]{64}"); }
        /** Não reserva Long.MAX_VALUE como revisão sem sucessor possível. */
        boolean revision(StateRevision revision) { return revision!=null&&revision.value()>=0&&revision.value()<Long.MAX_VALUE; }
        /** Época positiva identifica origem temporal sem autenticar sua autoridade. */
        boolean epoch(WorldEpoch epoch) { return epoch!=null&&epoch.value()>0&&epoch.value()<Long.MAX_VALUE; }
        /** Representação decimal limitada no DTO e sem conversão antecipada para inteiro. */
        boolean deadline(BigDecimal value) {
            return value!=null&&value.signum()>=0&&value.compareTo(BigDecimal.valueOf(Long.MAX_VALUE))<0;
        }

        /** Organiza verificações independentes sem produzir candidato instalado ou normalizado. */
        void validate(RecoverableCharacterSnapshot s) {
            if(!required(s,"snapshot"))return;
            if(required(s.schemaVersion(),"schemaVersion")&&s.schemaVersion().value()!=1)error(Code.SCHEMA,"schemaVersion");
            if(!required(s.characterId(),"characterId")||s.characterId().value()==null)error(Code.IDENTITY,"characterId");
            if(!revision(s.stateRevision()))error(Code.REVISION,"stateRevision");
            if(s.contentHash()==null||!hash(s.contentHash().value()))error(Code.CONTEXT,"contentHash");
            if(s.rulesVersion()==null||!id(s.rulesVersion().value()))error(Code.CONTEXT,"rulesVersion");
            metadata(s);
            identity(s);
            progression(s);
            attributes(s);
            inventory(s);
            economy(s);
            cooldowns(s);
            resources(s);
            location(s);
            restrictions(s);
        }

        /** Compara metadados somente com evidência injetada, sem consultar hora do sistema. */
        void metadata(RecoverableCharacterSnapshot s) {
            if(context==null||context.previousRevision()==null)pending.add(PendingCheck.PREVIOUS_REVISION);
            else if(context.previousRevision()<0||context.previousRevision()==Long.MAX_VALUE
                    ||!revision(s.stateRevision())||s.stateRevision().value()<=context.previousRevision())
                error(Code.REVISION,"stateRevision.previous");
            if(context==null||context.contentHash()==null||context.rulesVersion()==null)pending.add(PendingCheck.CONTENT_CONTEXT);
            else if(!hash(context.contentHash().value())||!id(context.rulesVersion().value())
                    ||!context.contentHash().equals(s.contentHash())||!context.rulesVersion().equals(s.rulesVersion()))
                error(Code.CONTEXT,"content/rules");
            if(!required(s.capture(),"capture"))return;
            var c=s.capture();
            if(!epoch(c.worldEpoch())||c.sourceTick()==null||c.sourceTick().value()<0
                    ||c.sourceTick().value()==Long.MAX_VALUE||c.capturedAt()==null||c.clockOrigin()==null
                    ||c.clockVersion()!=1)error(Code.CAPTURE,"capture");
            if(context==null||context.worldEpoch()==null||context.observedAt()==null)pending.add(PendingCheck.CAPTURE_CONTEXT);
            else if(!epoch(context.worldEpoch())||!context.worldEpoch().equals(c.worldEpoch())
                    ||c.capturedAt()!=null&&c.capturedAt().isAfter(context.observedAt()))error(Code.CAPTURE,"capture.context");
            // Até MONOTONIC_WORLD_TICKS precisa de política aprovada de continuidade/época.
        }

        /** Reutiliza identidade e catálogo puros sem criar UUID ou normalizar o candidato. */
        void identity(RecoverableCharacterSnapshot s) {
            if(!required(s.identity(),"identity"))return;
            var i=s.identity();
            try {
                if(i.race()==null)throw new IllegalArgumentException();
                race=CharacterRaces.get(i.race());
                if(i.name()==null||i.appearance()==null||s.characterId()==null||s.characterId().value()==null)
                    throw new IllegalArgumentException();
                var value=new CharacterIdentity(s.characterId().value(),i.name(),i.race(),i.appearance(),"village",race.hp(),race.attack());
                if(!value.name().equals(i.name()))error(Code.IDENTITY,"identity.name.canonical");
            } catch(IllegalArgumentException invalid) { error(Code.IDENTITY,"identity"); }
            if(!id(i.hair())||!id(i.clothing()))error(Code.IDENTITY,"identity.cosmetics");
            if(context==null||context.cosmeticIds()==null)pending.add(PendingCheck.CATALOGS);
            else if(id(i.hair())&&id(i.clothing())
                    &&(!context.cosmeticIds().contains(i.hair())||!context.cosmeticIds().contains(i.clothing())))
                error(Code.IDENTITY,"identity.cosmetics.catalog");
        }

        /** Reutiliza curva injetada e construção pura do domínio, sem builtin() ou validateFor(). */
        void progression(RecoverableCharacterSnapshot s) {
            if(!required(s.progression(),"progression"))return;
            var p=s.progression();
            if(p.xp()<0||p.xp()>1_000_000)error(Code.PROGRESSION,"progression.xp");
            else if(context==null||context.progression()==null)pending.add(PendingCheck.CATALOGS);
            else level=context.progression().level(p.xp());
            try {
                if(p.domain()==null)throw new IllegalArgumentException();
                var d=new DomainProgression(p.domain(),p.domainXpHalfUnits(),p.lastReset(),p.resetQuestComplete());
                if(d.chosen()&&(race!=null&&!Domains.get(d.domain()).race().equals(race.id())
                        ||level>0&&level<DomainProgression.INITIATION_LEVEL))error(Code.PROGRESSION,"progression.domain");
            } catch(IllegalArgumentException invalid) { error(Code.PROGRESSION,"progression.domain"); }
            // lastReset não deve causar overflow em nextResetDate da regra existente.
            if(p.lastReset()!=null)try { p.lastReset().plusMonths(1); }
            catch(java.time.DateTimeException invalid) { error(Code.PROGRESSION,"progression.lastReset"); }
        }

        /** Soma o orçamento pelas regras existentes, sem applyLevelUp nem concessão de pontos. */
        void attributes(RecoverableCharacterSnapshot s) {
            if(!required(s.attributes(),"attributes"))return;
            var a=s.attributes();
            if(a.mode()==null||a.strength()<0||a.intelligence()<0||a.agility()<0)
                error(Code.ATTRIBUTES,"attributes");
            try {
                new StatsComponent(a.strength(),a.intelligence(),a.agility(),a.energy(),a.grantedThroughLevel(),a.unspentPoints());
                if(a.mode()==AttributeMode.MANUAL_SESSION)maximumEnergy=EnergyRules.maximum(a.energy());
            } catch(IllegalArgumentException invalid) { error(Code.ATTRIBUTES,"attributes.bounds"); }
            if(a.mode()!=AttributeMode.MANUAL_SESSION)pending.add(PendingCheck.ATTRIBUTE_POLICY);
            else if(level>0) {
                long budget=0;var rules=new LevelingRules();
                for(int n=2;n<=level;n++)budget=Math.addExact(budget,rules.pointsForLevel(n));
                long total=(long)a.strength()+a.intelligence()+a.agility()+a.energy()+a.unspentPoints();
                if(a.grantedThroughLevel()!=level||total!=budget)error(Code.ATTRIBUTES,"attributes.budget");
            }
        }

        /** Consulta catálogo já carregado; item ausente é diagnóstico, não fallback fictício. */
        ItemCatalog.Item item(String id,String path) {
            if(!id(id)){error(Code.INVENTORY,path);return null;}
            if(context==null||context.items()==null){pending.add(PendingCheck.CATALOGS);return null;}
            try { return context.items().get(id); }
            catch(IllegalArgumentException invalid) { error(Code.INVENTORY,path);return null; }
        }

        /** Detecta índices antes de converter; o restore puro opera somente em inventário candidato local. */
        void inventory(RecoverableCharacterSnapshot s) {
            if(required(s.inventory(),"inventory")) {
                var inventory=s.inventory();var seen=new HashSet<Integer>();
                boolean valid=inventory.capacity()>=1&&inventory.capacity()<=MAX_ENTRIES;
                if(!valid)error(Code.INVENTORY,"inventory.capacity");
                if(required(inventory.slots(),"inventory.slots"))for(var slot:inventory.slots()) {
                    if(!seen.add(slot.index())){error(Code.DUPLICATE_SLOT,"inventory.slots");valid=false;}
                    var definition=item(slot.itemId(),"inventory.item");
                    if(slot.index()<0||slot.index()>=inventory.capacity()||slot.quantity()<1||slot.quantity()>99
                            ||definition!=null&&slot.quantity()>definition.maxStack()) {
                        error(Code.INVENTORY,"inventory.slot");valid=false;
                    }
                    if(definition==null)valid=false;
                }
                if(valid&&inventory.slots()!=null&&context!=null&&context.items()!=null) {
                    var data=new TreeMap<String,Integer>();
                    for(var slot:inventory.slots())data.put(slot.index()+":"+slot.itemId(),slot.quantity());
                    InventoryService.restore(context.items(),inventory.capacity(),data);
                }
            }
            if(required(s.equipment(),"equipment")) {
                var seen=new HashSet<String>();
                for(var e:s.equipment()) {
                    var definition=item(e.itemId(),"equipment.item");
                    if(!Set.of("weapon","torso","legs","accessory").contains(Objects.toString(e.slot(),""))
                            ||!seen.add(e.slot())||definition!=null&&(definition.category()!=ItemCatalog.Category.EQUIPMENT
                            ||!definition.slot().equals(e.slot())||level>0&&definition.minimumLevel()>level))
                        error(Code.EQUIPMENT,"equipment.slot");
                }
            }
            if(required(s.wallet(),"wallet")&&(s.wallet().coins()<0||s.wallet().coins()>1_000_000))error(Code.WALLET,"wallet.coins");
        }

        /** Valida recibos antes das referências, sem executar nenhuma concessão. */
        void economy(RecoverableCharacterSnapshot s) {
            var receipts=new HashMap<String,RewardReceipt>();var grants=new HashSet<List<Object>>();
            if(required(s.receipts(),"receipts"))for(var receipt:s.receipts()) {
                if(!id(receipt.id())||receipt.kind()==null||receipt.beneficiary()==null
                        ||receipt.beneficiary().value()==null||!receipt.beneficiary().equals(s.characterId())
                        ||!idSource(receipt.source())||!epoch(receipt.worldEpoch())||!revision(receipt.confirmedRevision())
                        ||revision(s.stateRevision())&&revision(receipt.confirmedRevision())
                        &&receipt.confirmedRevision().value()>s.stateRevision().value()||!hash(receipt.resultHash()))
                    error(Code.RECEIPT,"receipts.entry");
                if(receipts.putIfAbsent(receipt.id(),receipt)!=null)error(Code.DUPLICATE_RECEIPT,"receipts.id");
                var key=Arrays.<Object>asList(receipt.kind(),receipt.source(),receipt.worldEpoch(),receipt.beneficiary());
                if(!grants.add(key))error(Code.DUPLICATE_RECEIPT,"receipts.grant");
            }
            quests(s,receipts);
            var marks=new HashSet<List<Object>>();
            if(required(s.watermarks(),"watermarks"))for(var mark:s.watermarks()) {
                if(mark.type()==null||!id(mark.source())||!epoch(mark.worldEpoch())||mark.sequence()<1
                        ||mark.sequence()==Long.MAX_VALUE||!marks.add(Arrays.asList(mark.type(),mark.source(),mark.worldEpoch())))
                    error(Code.WATERMARK,"watermarks.entry");
            }
            var entitlements=new HashSet<String>();var grantsItems=new HashSet<List<String>>();
            var totals=new HashMap<String,Long>();
            if(required(s.pendingLoot(),"pendingLoot"))for(var entitlement:s.pendingLoot()) {
                item(entitlement.itemId(),"pendingLoot.item");
                var receipt=receipts.get(entitlement.receiptId());
                if(!id(entitlement.id())||!id(entitlement.receiptId())||!entitlements.add(entitlement.id())
                        ||!grantsItems.add(Arrays.asList(entitlement.receiptId(),entitlement.itemId()))
                        ||receipt==null||receipt.kind()!=RewardKind.LOOT||entitlement.quantity()<1||entitlement.quantity()>9999)
                    error(Code.ENTITLEMENT,"pendingLoot.entry");
                long total=Math.addExact(totals.getOrDefault(entitlement.itemId(),0L),(long)entitlement.quantity());
                totals.put(entitlement.itemId(),total);
                if(total>9999)error(Code.ENTITLEMENT,"pendingLoot.total");
            }
        }

        /** Chave de recibo admite separadores somente no namespace da origem, com comprimento limitado. */
        boolean idSource(String source) { return source!=null&&source.matches("[a-zA-Z0-9][a-zA-Z0-9_:.-]{0,99}"); }

        /** Relaciona missão, definição, objetivos e entrega; não chama QuestService.complete. */
        void quests(RecoverableCharacterSnapshot s,Map<String,RewardReceipt> receipts) {
            var seen=new HashSet<List<Object>>();
            if(!required(s.quests(),"quests"))return;
            for(var quest:s.quests()) {
                var key=quest.key();
                if(key==null||key.namespace()==null||!id(key.id())||!id(key.version())||quest.status()==null) {
                    error(Code.QUEST,"quests.key");continue;
                }
                if(!seen.add(Arrays.asList(key.namespace(),key.id())))error(Code.DUPLICATE_QUEST,"quests.key");
                QuestService.Definition definition=null;
                if(context==null||context.quests()==null)pending.add(PendingCheck.CATALOGS);
                else {definition=context.quests().get(key);if(definition==null)error(Code.QUEST,"quests.definition");}
                boolean complete=true;var indices=new HashSet<Integer>();
                if(required(quest.objectives(),"quests.objectives")) {
                    if(quest.objectives().isEmpty())error(Code.OBJECTIVE,"quests.objectives");
                    if(definition!=null&&quest.objectives().size()!=definition.objectives().size())error(Code.OBJECTIVE,"quests.objectives.count");
                    for(var objective:quest.objectives()) {
                        if(!indices.add(objective.index())||objective.index()<0||objective.index()>=MAX_OBJECTIVES
                                ||objective.count()<0||objective.count()>100
                                ||!Set.of("kill","talk","visit","collect").contains(Objects.toString(objective.type(),""))
                                ||!id(objective.target()))error(Code.OBJECTIVE,"quests.objective");
                        if(definition!=null&&objective.index()>=0&&objective.index()<definition.objectives().size()) {
                            var expected=definition.objectives().get(objective.index());
                            if(!expected.type().equals(objective.type())||!expected.target().equals(objective.target())
                                    ||objective.count()>expected.count())error(Code.OBJECTIVE,"quests.objective.definition");
                            complete&=objective.count()==expected.count();
                        }
                    }
                }
                if(definition!=null&&quest.status()!=null&&(quest.status()!=QuestStatus.ACTIVE)!=complete)
                    error(Code.QUEST,"quests.status");
                if(quest.status()==QuestStatus.DELIVERED) {
                    var receipt=receipts.get(quest.deliveryReceiptId());
                    String source=key.namespace().name().toLowerCase(Locale.ROOT)+":"+key.id()+":"+key.version();
                    if(receipt==null||receipt.kind()!=RewardKind.QUEST||!source.equals(receipt.source()))error(Code.RECEIPT,"quests.delivery");
                } else if(quest.deliveryReceiptId()!=null)error(Code.RECEIPT,"quests.delivery.unexpected");
            }
        }

        /** Confere todos os canais e IDs de skill reais, sem calcular ou reduzir restante de recarga. */
        void cooldowns(RecoverableCharacterSnapshot s) {
            if(!required(s.cooldowns(),"cooldowns"))return;
            if(s.cooldowns().policyVersion()!=1)error(Code.COOLDOWN,"cooldowns.policyVersion");
            var requiredKeys=new HashSet<String>(Set.of("BASIC_ATTACK","SHUNPO","POTION","TRAVEL"));
            if(s.progression()!=null&&s.progression().domain()!=null)try {
                RpgSkillCatalog.forDomain(s.progression().domain()).forEach(skill->requiredKeys.add("DOMAIN_SKILL:"+skill.id()));
            } catch(IllegalArgumentException invalid) { error(Code.COOLDOWN,"cooldowns.domain"); }
            var seen=new HashSet<String>();
            if(!required(s.cooldowns().deadlines(),"cooldowns.deadlines"))return;
            for(var d:s.cooldowns().deadlines()) {
                String key=d.channel()==null?"":d.channel().name();
                if(d.channel()==CooldownChannel.DOMAIN_SKILL)key+=":"+d.skillId();
                else if(d.skillId()!=null)error(Code.COOLDOWN,"cooldowns.unexpectedSkill");
                if(!seen.add(key)||!requiredKeys.contains(key))error(Code.COOLDOWN,"cooldowns.channel");
                if(!deadline(d.readyAtTick()))error(Code.DEADLINE,"cooldowns.readyAtTick");
            }
            if(!seen.equals(requiredKeys))error(Code.COOLDOWN,"cooldowns.required");
        }

        /** Recursos são apenas conferidos; nenhuma cura, regeneração ou respawn é executado. */
        void resources(RecoverableCharacterSnapshot s) {
            if(!required(s.resources(),"resources"))return;
            var r=s.resources();
            if(r.hp()<0||r.energyAbsolute()<0||!Double.isFinite(r.regenerationRemainder())
                    ||r.regenerationRemainder()<0||r.regenerationRemainder()>=1||r.lifeState()==null
                    ||r.lifeState()==LifeState.DEAD&&r.hp()!=0||r.lifeState()==LifeState.ALIVE&&r.hp()==0)
                error(Code.RESOURCES,"resources");
            if(race!=null&&level>0&&context!=null&&context.progression()!=null) {
                long max=(long)race.hp()+(long)(level-1)*context.progression().hpPerLevel();
                if(r.hp()>max)error(Code.RESOURCES,"resources.hp");
            }
            if(maximumEnergy!=null) {
                int max=maximumEnergy;
                if(r.energyAbsolute()>max||r.energyAbsolute()==max&&r.regenerationRemainder()!=0)
                    error(Code.RESOURCES,"resources.energy");
            }
        }

        /** Região e limites são dados declarativos; ocupação/spawn permanecem pendentes. */
        void location(RecoverableCharacterSnapshot s) {
            if(!required(s.location(),"location"))return;
            var l=s.location();
            if(!id(l.regionId())||!Float.isFinite(l.x())||!Float.isFinite(l.y())||l.x()<0||l.y()<0
                    ||l.spawnRef()!=null&&!id(l.spawnRef()))error(Code.LOCATION,"location");
            if(context==null||context.regions()==null)pending.add(PendingCheck.CATALOGS);
            else if(id(l.regionId())) {
                var bounds=context.regions().get(l.regionId());
                if(bounds==null||!bounds.contains(l.x(),l.y()))error(Code.LOCATION,"location.region");
            }
        }

        /** Evidência ausente não remove penalidade; descritor válido ainda exige política de produto. */
        void restrictions(RecoverableCharacterSnapshot s) {
            if(!required(s.restrictions(),"restrictions"))return;
            var r=s.restrictions();var seen=new HashSet<RestrictionKind>();var sources=new HashSet<List<Object>>();
            if(r.assessment()==null)error(Code.RESTRICTION,"restrictions.assessment");
            if(r.assessment()!=RestrictionAssessment.ASSESSED)pending.add(PendingCheck.RESTRICTION_EVIDENCE);
            if(required(r.entries(),"restrictions.entries"))for(var entry:r.entries()) {
                if(entry.kind()==null||!id(entry.source())||!deadline(entry.untilTick())||entry.policyVersion()!=1
                        ||!sources.add(Arrays.asList(entry.kind(),entry.source())))error(Code.RESTRICTION,"restrictions.entry");
                seen.add(entry.kind());pending.add(PendingCheck.RESTRICTION_POLICY);
            }
            if(context==null||context.requiredRestrictions()==null)pending.add(PendingCheck.RESTRICTION_EVIDENCE);
            else if(!seen.containsAll(context.requiredRestrictions()))error(Code.RESTRICTION,"restrictions.required");
        }
    }
}
