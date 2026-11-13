package com.projetok.rpg.economy;

import com.projetok.rpg.*;
import java.io.*;
import java.security.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Contrato experimental somente de teste. Estado, recibo e índices são publicados por um CAS
 * de imagem imutável em memória. Não é banco, autenticação, save ou recuperação de produção.
 */
final class EconomicLaboratory {
    private EconomicLaboratory() { }

    /** Identidades sintéticas distintas; nenhum destes valores comprova posse. */
    record BeneficiaryId(UUID value) { BeneficiaryId { Objects.requireNonNull(value); } }
    /** Identidade global da intenção nesta instância de ledger. */
    record EconomicOperationId(UUID value) { EconomicOperationId { Objects.requireNonNull(value); } }
    /** Concessão autorizada, distinta da tentativa de operação e da origem. */
    record GrantId(UUID value) { GrantId { Objects.requireNonNull(value); } }
    /** Revisão exclusiva do estado econômico, nunca derivada de clocks ou sessão. */
    record EconomicRevision(long value) {
        EconomicRevision { if(value<0)throw new IllegalArgumentException("Revisão negativa"); }
        /** Overflow é recusa, sem wrap ou last-write-wins. */
        EconomicRevision next() { return new EconomicRevision(Math.addExact(value,1)); }
    }
    /** Geração local de admissão do controlador do laboratório. */
    record AuthorityGeneration(long value) { AuthorityGeneration { if(value<1)throw new IllegalArgumentException("Geração inválida"); } }
    /** Época explicitamente fornecida pela fixture; sem equivalência com UUID ou tick. */
    record WorldEpoch(long value) { WorldEpoch { if(value<1)throw new IllegalArgumentException("Época inválida"); } }
    enum SourceKind { QUEST, MOB }
    enum QuestStatus { ACTIVE, READY, COMPLETED }

    /** Identidade de missão; versão de definição não concede uma segunda entrega automaticamente. */
    record QuestIdentity(String namespace,String id) {
        QuestIdentity { identifier(namespace);identifier(id); }
        /** Ordenação canônica somente para fingerprint. */
        String key() { return namespace+"/"+id; }
    }
    /** Origem completa, com geração de mob apenas quando aplicável. */
    record EconomicSource(SourceKind kind,String namespace,String sourceId,String definitionId,
            String definitionVersion,Long mobGeneration,GrantId grant) {
        EconomicSource {
            Objects.requireNonNull(kind);identifier(namespace);identifier(sourceId);identifier(definitionId);
            identifier(definitionVersion);Objects.requireNonNull(grant);
            if(kind==SourceKind.MOB?(mobGeneration==null||mobGeneration<1||mobGeneration>1_000_000_000L):mobGeneration!=null)
                throw new IllegalArgumentException("Geração incompatível com origem");
        }
        /** Missões são deduplicadas por namespace e identidade, independentemente da versão. */
        QuestIdentity quest() { return new QuestIdentity(namespace,sourceId); }
    }
    /** Valores já resolvidos pelo controlador; nenhum RNG é retido ou executado no ledger. */
    record RewardTerms(long xp,int coins,Map<String,Integer> items,Map<String,Integer> consumedItems) {
        RewardTerms {
            if(xp<0||coins<0)throw new IllegalArgumentException("Recompensa negativa");
            items=quantities(items);consumedItems=quantities(consumedItems);
        }
        /** Extrai o contrato real de recompensa e consumo, sem executar missão. */
        static RewardTerms quest(QuestService.Definition definition) {
            var consumed=new TreeMap<String,Integer>();
            for(var objective:definition.objectives())if(objective.type().equals("collect"))
                consumed.merge(objective.target(),objective.count(),Math::addExact);
            return new RewardTerms(definition.xp(),definition.coins(),
                    Map.of(definition.item(),definition.quantity()),consumed);
        }
    }
    /** Oferta confiável da fixture; não vem de socket, nome, IP ou UUID apresentado pelo jogador. */
    record AuthorizedGrant(BeneficiaryId beneficiary,WorldEpoch epoch,EconomicSource source,RewardTerms reward,
            QuestService.Definition questDefinition) {
        AuthorizedGrant {
            Objects.requireNonNull(beneficiary);Objects.requireNonNull(epoch);Objects.requireNonNull(source);Objects.requireNonNull(reward);
            if(source.kind()==SourceKind.QUEST) {
                Objects.requireNonNull(questDefinition);
                QuestService.validateDefinition(RpgContent.builtin(),questDefinition);
                if(!RewardTerms.quest(questDefinition).equals(reward))throw new IllegalArgumentException("Termos da missão divergentes");
            } else if(questDefinition!=null||!reward.consumedItems().isEmpty())
                throw new IllegalArgumentException("Contrato de mob incompatível");
        }
    }

    /** Ações econômicas solicitadas; nenhuma contém estado completo para sobrescrever o ledger. */
    sealed interface EconomicAction permits GrantReward,ClaimPending,ConsumeItem,MoveItem { }
    record GrantReward(EconomicSource source,RewardTerms requested) implements EconomicAction {
        GrantReward { Objects.requireNonNull(source);Objects.requireNonNull(requested); }
    }
    record ClaimPending(GrantId grant,EconomicOperationId grantingReceipt) implements EconomicAction {
        ClaimPending { Objects.requireNonNull(grant);Objects.requireNonNull(grantingReceipt); }
    }
    record ConsumeItem(String item,int quantity) implements EconomicAction {
        ConsumeItem { identifier(item);if(quantity<1)throw new IllegalArgumentException("Quantidade inválida"); }
    }
    record MoveItem(int from,int to,int quantity) implements EconomicAction {
        MoveItem { if(from<0||to<0||quantity<1)throw new IllegalArgumentException("Movimento inválido"); }
    }
    /** Intenção imutável; geração/época não substituem o coordenador admitido pela fixture. */
    record EconomicOperation(EconomicOperationId id,BeneficiaryId beneficiary,EconomicRevision expected,
            AuthorityGeneration generation,WorldEpoch epoch,EconomicAction action) {
        EconomicOperation {
            Objects.requireNonNull(id);Objects.requireNonNull(beneficiary);Objects.requireNonNull(expected);
            Objects.requireNonNull(generation);Objects.requireNonNull(epoch);Objects.requireNonNull(action);
        }
    }
    /** Estado de entrega ligado ao recibo; READY não é recompensa confirmada. */
    record QuestEntry(QuestStatus status,EconomicOperationId deliveryReceipt) {
        QuestEntry {
            Objects.requireNonNull(status);
            if((status==QuestStatus.COMPLETED)!=(deliveryReceipt!=null))throw new IllegalArgumentException("Entrega sem referência");
        }
    }
    /** Direito pendente conserva origem, resultado resolvido e referência da concessão original. */
    record PendingEntitlement(GrantId grant,EconomicOperationId grantingReceipt,
            EconomicSource source,Map<String,Integer> items) {
        PendingEntitlement {
            Objects.requireNonNull(grant);Objects.requireNonNull(grantingReceipt);Objects.requireNonNull(source);
            items=quantities(items);
            if(items.isEmpty()||!grant.equals(source.grant()))throw new IllegalArgumentException("Direito sem origem/itens");
        }
    }
    /** Estado econômico imutável, sem RpgSave ou referências a inventário vivo. */
    record EconomicState(BeneficiaryId beneficiary,EconomicRevision revision,long xp,DomainProgression domain,
            int coins,int capacity,Map<String,Integer> inventory,Map<GrantId,PendingEntitlement> pending,
            Map<QuestIdentity,QuestEntry> quests,Map<GrantId,EconomicOperationId> claimed) {
        EconomicState {
            Objects.requireNonNull(beneficiary);Objects.requireNonNull(revision);Objects.requireNonNull(domain);
            if(xp<0||xp>1_000_000||coins<0||coins>1_000_000||capacity<1||capacity>100)
                throw new IllegalArgumentException("Estado econômico fora dos limites");
            inventory=Map.copyOf(inventory);pending=Map.copyOf(pending);quests=Map.copyOf(quests);claimed=Map.copyOf(claimed);
            if(!Collections.disjoint(pending.keySet(),claimed.keySet()))throw new IllegalArgumentException("Direito duplicado");
        }
    }
    /** Resultado histórico exato da operação; não é o estado atual após operações posteriores. */
    record EconomicReceipt(EconomicOperation operation,EconomicRevision before,EconomicRevision after,
            String beforeHash,EconomicState confirmedState,String resultHash) {
        /** Rejeita referências ausentes antes de qualquer publicação ou auditoria. */
        EconomicReceipt {
            Objects.requireNonNull(operation);Objects.requireNonNull(before);Objects.requireNonNull(after);Objects.requireNonNull(confirmedState);
            if(beforeHash==null||resultHash==null||!beforeHash.matches("[a-f0-9]{64}")||!resultHash.matches("[a-f0-9]{64}"))
                throw new IllegalArgumentException("Fingerprint ausente/inválido");
        }
    }
    enum Code {
        VALIDATED,COMMITTED,ALREADY_CONFIRMED,CONFIRMED_BY_LOOKUP,NOT_FOUND,UNCERTAIN,
        INVALID_OPERATION,WRONG_BENEFICIARY,STALE_AUTHORITY,WRONG_EPOCH,STALE_REVISION,
        ID_CONFLICT,DUPLICATE_GRANT,OBSOLETE_SOURCE,QUEST_NOT_READY,ALREADY_DELIVERED,
        MISSING_RIGHT,WRONG_ORIGIN,INVENTORY_FULL,LIMIT_EXCEEDED,RETENTION_LIMIT,
        INVALID_TRANSITION,FOREIGN_VALIDATION,PREPARATION_FAILED,BEFORE_COMMIT_FAILED,HISTORY_INCOMPLETE
    }
    /** Ausência de resposta não é prova de ausência de commit. */
    record EconomicOperationResult(Code code,EconomicReceipt receipt,String detail) {
        /** Confirmação nesta memória, sem alegação de durabilidade após queda de processo. */
        boolean confirmed() { return receipt!=null&&Set.of(Code.COMMITTED,Code.ALREADY_CONFIRMED,Code.CONFIRMED_BY_LOOKUP).contains(code); }
    }
    enum Fault { NONE,PREPARATION,BEFORE_COMMIT,AFTER_COMMIT,RECONCILIATION,DROP_RECEIPT,CORRUPT_RECEIPT }

    /** Proposta opaca, criada apenas após regras aprovarem a transição sobre uma revisão exata. */
    static final class ValidatedOperation {
        private final MemoryLedger ledger;
        private final Coordinator coordinator;
        private final EconomicOperation operation;
        private final EconomicState before,after;
        private ValidatedOperation(MemoryLedger ledger,Coordinator coordinator,EconomicOperation operation,
                EconomicState before,EconomicState after) {
            this.ledger=ledger;this.coordinator=coordinator;this.operation=operation;this.before=before;this.after=after;
        }
        /** Estado proposto imutável; não indica confirmação. */
        EconomicState proposedState() { return after; }
    }
    record Preparation(ValidatedOperation validated,EconomicOperationResult result) { }
    /** Chaves econômicas incluem beneficiário e origem, além da época/geração quando aplicáveis. */
    record ConcessionKey(BeneficiaryId beneficiary,String scope,String source,WorldEpoch epoch,Long generation) { }
    record SpawnKey(BeneficiaryId beneficiary,WorldEpoch epoch,String namespace,String spawn) { }
    record Entry(EconomicState opening,EconomicState state,AuthorityGeneration generation,WorldEpoch epoch) {
        /** Contexto e estado são obrigatórios; imagem incompleta não entra no CAS. */
        Entry {
            Objects.requireNonNull(opening);Objects.requireNonNull(state);Objects.requireNonNull(generation);Objects.requireNonNull(epoch);
            if(!opening.beneficiary().equals(state.beneficiary())||state.revision().value()<opening.revision().value())
                throw new IllegalArgumentException("Abertura e estado divergentes");
        }
    }
    /** Uma única referência contém todos os componentes da transação experimental. */
    record LedgerImage(Map<BeneficiaryId,Entry> entries,Map<EconomicOperationId,EconomicReceipt> receipts,
            Map<ConcessionKey,EconomicOperationId> concessions,Map<SpawnKey,Long> watermarks) {
        LedgerImage { entries=Map.copyOf(entries);receipts=Map.copyOf(receipts);concessions=Map.copyOf(concessions);watermarks=Map.copyOf(watermarks); }
    }

    /** Controlador sintético vinculado pela fixture; não cria conta, credencial ou token de login. */
    static final class Coordinator {
        private final MemoryLedger ledger;
        private final BeneficiaryId beneficiary;
        private final AuthorityGeneration generation;
        private final WorldEpoch epoch;
        private Coordinator(MemoryLedger ledger,BeneficiaryId beneficiary,Entry entry) {
            this.ledger=ledger;this.beneficiary=beneficiary;generation=entry.generation();epoch=entry.epoch();
        }
        /** Primeiro consulta o recibo pelo ID; nunca reaplica recompensa por resposta perdida. */
        EconomicOperationResult execute(EconomicOperation operation,Fault fault) {
            var prepared=ledger.prepare(this,operation,fault);
            return prepared.validated()==null?prepared.result():ledger.commit(prepared.validated(),fault);
        }
        /** Consulta/reconciliação pura; falha deixa o resultado incerto sem disparar concessão. */
        EconomicOperationResult reconcile(EconomicOperationId id,Fault fault) {
            var image=ledger.image.get();var guard=ledger.guard(this,null,image);
            if(guard!=null)return result(guard);
            if(fault==Fault.RECONCILIATION)return result(Code.UNCERTAIN);
            if(!ledger.historyValid(image))return result(Code.HISTORY_INCOMPLETE);
            var receipt=image.receipts().get(Objects.requireNonNull(id));
            if(receipt==null)return result(Code.NOT_FOUND);
            if(!receipt.operation().beneficiary().equals(beneficiary))return result(Code.WRONG_BENEFICIARY);
            return new EconomicOperationResult(Code.CONFIRMED_BY_LOOKUP,receipt,"Histórico confirmado nesta memória");
        }
    }

    /** Ledger descartável; não grava arquivo nem reutiliza o repositório de RpgSave. */
    static final class MemoryLedger {
        private final AtomicReference<LedgerImage> image;
        private final Map<GrantId,AuthorizedGrant> grants;
        private final ItemCatalog items=new ItemCatalog(RpgContent.builtin());
        private final int receiptLimit;

        /** Recebe apenas marcos de abertura sintéticos, nunca flags antigas ou saves pessoais. */
        MemoryLedger(Map<BeneficiaryId,EconomicState> opening,Map<GrantId,AuthorizedGrant> grants,
                WorldEpoch epoch,int receiptLimit) {
            if(receiptLimit<1||opening.isEmpty())throw new IllegalArgumentException("Orçamento/abertura inválidos");
            this.receiptLimit=receiptLimit;this.grants=Map.copyOf(grants);
            this.grants.forEach((id,grant)->{if(!id.equals(grant.source().grant()))throw new IllegalArgumentException("Concessão divergente");});
            var entries=new HashMap<BeneficiaryId,Entry>();
            opening.forEach((id,state)->{
                if(!id.equals(state.beneficiary())||!state.pending().isEmpty()||!state.claimed().isEmpty()
                        ||state.quests().values().stream().anyMatch(q->q.status()==QuestStatus.COMPLETED))
                    throw new IllegalArgumentException("Abertura sem histórico suficiente");
                InventoryService.restore(items,state.capacity(),state.inventory());
                entries.put(id,new Entry(state,state,new AuthorityGeneration(1),Objects.requireNonNull(epoch)));
            });
            image=new AtomicReference<>(new LedgerImage(entries,Map.of(),Map.of(),Map.of()));
        }
        /** Somente o controlador confiável da fixture chama esta fábrica de vínculo local. */
        Coordinator coordinator(BeneficiaryId beneficiary) {
            var entry=image.get().entries().get(beneficiary);
            if(entry==null)throw new IllegalArgumentException("Beneficiário não admitido pelo laboratório");
            return new Coordinator(this,beneficiary,entry);
        }
        /** Observação imutável de teste, sem instalar snapshots antigos. */
        LedgerImage snapshot() { return image.get(); }

        /** Troca explícita de contexto sintético, sem alterar revisão/saldos econômicos. */
        void nextAdmission(BeneficiaryId beneficiary,WorldEpoch epoch) {
            while(true) {
                var before=image.get();var old=Objects.requireNonNull(before.entries().get(beneficiary));
                var entries=new HashMap<>(before.entries());
                entries.put(beneficiary,new Entry(old.opening(),old.state(),
                        new AuthorityGeneration(Math.addExact(old.generation().value(),1)),Objects.requireNonNull(epoch)));
                if(image.compareAndSet(before,new LedgerImage(entries,before.receipts(),before.concessions(),before.watermarks())))return;
            }
        }

        /** Valida vínculo atual antes de olhar idempotência; UUID sozinho não é uma autorização. */
        private Code guard(Coordinator coordinator,EconomicOperation operation,LedgerImage current) {
            if(coordinator==null||coordinator.ledger!=this)return Code.FOREIGN_VALIDATION;
            var entry=current.entries().get(coordinator.beneficiary);
            if(entry==null)return Code.WRONG_BENEFICIARY;
            if(!entry.generation().equals(coordinator.generation))return Code.STALE_AUTHORITY;
            if(!entry.epoch().equals(coordinator.epoch))return Code.WRONG_EPOCH;
            if(operation!=null) {
                if(!operation.beneficiary().equals(coordinator.beneficiary))return Code.WRONG_BENEFICIARY;
                if(!operation.generation().equals(coordinator.generation))return Code.STALE_AUTHORITY;
                if(!operation.epoch().equals(coordinator.epoch))return Code.WRONG_EPOCH;
            }
            return null;
        }
        /** Revalida ID, origem e revisão sem executar novamente o plano de recompensa. */
        private EconomicOperationResult precondition(Coordinator coordinator,EconomicOperation operation,LedgerImage current) {
            var guard=guard(coordinator,operation,current);if(guard!=null)return result(guard);
            if(operation==null)return result(Code.INVALID_OPERATION);
            if(!historyValid(current))return result(Code.HISTORY_INCOMPLETE);
            var receipt=current.receipts().get(operation.id());
            if(receipt!=null)return receipt.operation().equals(operation)?
                    new EconomicOperationResult(Code.ALREADY_CONFIRMED,receipt,"Consulta idempotente pelo ID"):result(Code.ID_CONFLICT);
            var entry=current.entries().get(operation.beneficiary());
            var key=concession(operation);
            if(key!=null&&current.concessions().containsKey(key))return result(operation.action() instanceof GrantReward reward
                    &&reward.source().kind()==SourceKind.QUEST?Code.ALREADY_DELIVERED:Code.DUPLICATE_GRANT);
            if(operation.action() instanceof GrantReward reward) {
                var grant=grants.get(reward.source().grant());
                if(grant==null||!grant.beneficiary().equals(operation.beneficiary())||!grant.source().equals(reward.source())
                        ||!grant.reward().equals(reward.requested()))return result(Code.INVALID_OPERATION);
                if(!grant.epoch().equals(operation.epoch()))return result(Code.WRONG_EPOCH);
                if(current.concessions().containsKey(grantIdentity(operation,reward.source())))return result(Code.DUPLICATE_GRANT);
                if(reward.source().kind()==SourceKind.MOB) {
                    var watermark=current.watermarks().get(spawn(operation,reward.source()));
                    if(watermark!=null&&reward.source().mobGeneration()<=watermark)return result(Code.OBSOLETE_SOURCE);
                }
            }
            if(!entry.state().revision().equals(operation.expected()))return result(Code.STALE_REVISION);
            if(current.receipts().size()>=receiptLimit)return result(Code.RETENTION_LIMIT);
            return null;
        }

        /** Prepara somente cópias com regras reais; não publica estado validado como confirmado. */
        Preparation prepare(Coordinator coordinator,EconomicOperation operation,Fault fault) {
            if(fault==Fault.RECONCILIATION)return new Preparation(null,result(Code.UNCERTAIN));
            var current=image.get();var refused=precondition(coordinator,operation,current);
            if(refused!=null)return new Preparation(null,refused);
            var before=current.entries().get(operation.beneficiary()).state();
            try {
                var after=transition(before,operation,current);
                if(fault==Fault.PREPARATION)return new Preparation(null,result(Code.PREPARATION_FAILED));
                return new Preparation(new ValidatedOperation(this,coordinator,operation,before,after),result(Code.VALIDATED));
            } catch (Rejected failure) {
                return new Preparation(null,result(failure.code));
            } catch (ArithmeticException failure) {
                return new Preparation(null,result(Code.LIMIT_EXCEEDED));
            } catch (IllegalArgumentException | IllegalStateException failure) {
                return new Preparation(null,new EconomicOperationResult(Code.INVALID_OPERATION,null,failure.getMessage()));
            }
        }

        /** Publica estado, recibo, deduplicação e watermark pelo mesmo CAS de imagem. */
        EconomicOperationResult commit(ValidatedOperation validated,Fault fault) {
            if(validated==null||validated.ledger!=this)return result(Code.FOREIGN_VALIDATION);
            var operation=validated.operation;
            while(true) {
                var before=image.get();var refused=precondition(validated.coordinator,operation,before);
                if(refused!=null)return refused;
                var entry=before.entries().get(operation.beneficiary());
                if(!entry.state().equals(validated.before))return result(Code.STALE_REVISION);
                if(fault==Fault.BEFORE_COMMIT)return result(Code.BEFORE_COMMIT_FAILED);
                var entries=new HashMap<>(before.entries());
                entries.put(operation.beneficiary(),new Entry(entry.opening(),validated.after,entry.generation(),entry.epoch()));
                var receipts=new HashMap<>(before.receipts());
                var receipt=new EconomicReceipt(operation,validated.before.revision(),validated.after.revision(),
                        stateHash(validated.before),validated.after,receiptHash(operation,validated.before,validated.after));
                if(fault==Fault.CORRUPT_RECEIPT)receipt=new EconomicReceipt(operation,receipt.before(),receipt.after(),
                        receipt.beforeHash(),receipt.confirmedState(),"0".repeat(64));
                if(fault!=Fault.DROP_RECEIPT)receipts.put(operation.id(),receipt);
                var concessions=new HashMap<>(before.concessions());var key=concession(operation);
                if(key!=null)concessions.put(key,operation.id());
                if(operation.action() instanceof GrantReward reward)concessions.put(grantIdentity(operation,reward.source()),operation.id());
                var watermarks=new HashMap<>(before.watermarks());
                if(operation.action() instanceof GrantReward reward&&reward.source().kind()==SourceKind.MOB)
                    watermarks.put(spawn(operation,reward.source()),reward.source().mobGeneration());
                var candidate=new LedgerImage(entries,receipts,concessions,watermarks);
                if(!historyValid(candidate))return result(Code.INVALID_TRANSITION);
                if(image.compareAndSet(before,candidate))return fault==Fault.AFTER_COMMIT?result(Code.UNCERTAIN):
                        new EconomicOperationResult(Code.COMMITTED,receipt,"Confirmado somente em memória");
            }
        }

        /** Calcula uma única transição candidata; todos os objetos de inventário são descartáveis. */
        private EconomicState transition(EconomicState before,EconomicOperation operation,LedgerImage current) {
            var revision=before.revision().next();var inventory=InventoryService.restore(items,before.capacity(),before.inventory());
            long xp=before.xp();int coins=before.coins();var domain=before.domain();
            var pending=new HashMap<>(before.pending());var quests=new HashMap<>(before.quests());var claimed=new HashMap<>(before.claimed());
            if(operation.action() instanceof GrantReward request) {
                var grant=grants.get(request.source().grant());var terms=grant.reward();
                long nextXp=Math.addExact(xp,terms.xp());int nextCoins=Math.addExact(coins,terms.coins());
                if(nextXp>1_000_000||nextCoins>1_000_000)throw new Rejected(Code.LIMIT_EXCEEDED);
                if(grant.source().kind()==SourceKind.QUEST) {
                    var quest=quests.get(grant.source().quest());
                    if(quest==null||quest.status()==QuestStatus.ACTIVE)throw new Rejected(Code.QUEST_NOT_READY);
                    if(quest.status()==QuestStatus.COMPLETED)throw new Rejected(Code.ALREADY_DELIVERED);
                    for(var required:terms.consumedItems().entrySet())if(inventory.count(required.getKey())<required.getValue())
                        throw new Rejected(Code.QUEST_NOT_READY);
                    // O serviço real só trabalha sobre a bolsa descartável desta preparação.
                    QuestRewards.Plan plan;
                    try{plan=QuestRewards.prepare(inventory,coins,xp,domain,grant.questDefinition());}
                    catch(IllegalStateException full){throw new Rejected(Code.INVENTORY_FULL);}
                    if(plan.xp()!=nextXp||plan.coins()!=nextCoins)throw new Rejected(Code.LIMIT_EXCEEDED);
                    inventory=plan.inventory();xp=plan.xp();coins=plan.coins();domain=plan.domain();
                    quests.put(grant.source().quest(),new QuestEntry(QuestStatus.COMPLETED,operation.id()));
                } else {
                    xp=nextXp;coins=nextCoins;domain=domain.award(terms.xp());
                    var copy=inventory.copy();boolean fits=true;
                    for(var item:new TreeMap<>(terms.items()).entrySet())if(!copy.add(item.getKey(),item.getValue())){fits=false;break;}
                    if(fits)inventory=copy;
                    else pending.put(grant.source().grant(),new PendingEntitlement(grant.source().grant(),operation.id(),grant.source(),terms.items()));
                }
            } else if(operation.action() instanceof ClaimPending claim) {
                var right=pending.get(claim.grant());if(right==null)throw new Rejected(Code.MISSING_RIGHT);
                if(!right.grantingReceipt().equals(claim.grantingReceipt())||!current.receipts().containsKey(claim.grantingReceipt()))
                    throw new Rejected(Code.WRONG_ORIGIN);
                for(var item:new TreeMap<>(right.items()).entrySet())if(!inventory.add(item.getKey(),item.getValue()))throw new Rejected(Code.INVENTORY_FULL);
                pending.remove(claim.grant());claimed.put(claim.grant(),operation.id());
            } else if(operation.action() instanceof ConsumeItem consume) {
                if(!inventory.remove(consume.item(),consume.quantity()))throw new Rejected(Code.INVALID_OPERATION);
            } else if(operation.action() instanceof MoveItem move) {
                if(!inventory.move(move.from(),move.to(),move.quantity()))throw new Rejected(Code.INVALID_OPERATION);
            }
            return new EconomicState(before.beneficiary(),revision,xp,domain,coins,before.capacity(),inventory.snapshot(),pending,quests,claimed);
        }

        /** Confere cadeia, fingerprint e referências sem RNG, recompensa ou replay de gameplay. */
        boolean historyValid(LedgerImage candidate) {
            for(var entry:candidate.entries().entrySet()) {
                var value=entry.getValue();var cursor=value.opening();
                if(!entry.getKey().equals(cursor.beneficiary())||!entry.getKey().equals(value.state().beneficiary()))return false;
                var chain=candidate.receipts().values().stream().filter(r->r.operation().beneficiary().equals(entry.getKey()))
                        .sorted(Comparator.comparingLong(r->r.after().value())).toList();
                for(var receipt:chain) {
                    if(!receipt.before().equals(cursor.revision())||receipt.before().value()==Long.MAX_VALUE
                            ||receipt.after().value()!=receipt.before().value()+1||!receipt.after().equals(receipt.confirmedState().revision())
                            ||!receipt.beforeHash().equals(stateHash(cursor))||!receipt.resultHash().equals(receiptHash(receipt.operation(),cursor,receipt.confirmedState()))
                            ||!receipt.operation().expected().equals(receipt.before())
                            ||!entry.getKey().equals(receipt.confirmedState().beneficiary()))return false;
                    cursor=receipt.confirmedState();
                }
                if(!cursor.equals(value.state()))return false;
                for(var quest:cursor.quests().entrySet())if(quest.getValue().status()==QuestStatus.COMPLETED) {
                    var delivery=candidate.receipts().get(quest.getValue().deliveryReceipt());
                    if(delivery==null||!delivery.operation().beneficiary().equals(cursor.beneficiary())
                            ||!(delivery.operation().action() instanceof GrantReward reward)||reward.source().kind()!=SourceKind.QUEST
                            ||!reward.source().quest().equals(quest.getKey())
                            ||!quest.getValue().equals(delivery.confirmedState().quests().get(quest.getKey())))return false;
                }
                for(var right:cursor.pending().values()) {
                    var origin=candidate.receipts().get(right.grantingReceipt());
                    if(origin==null||!origin.operation().beneficiary().equals(cursor.beneficiary())
                            ||!(origin.operation().action() instanceof GrantReward reward)||!reward.source().equals(right.source())
                            ||!reward.requested().items().equals(right.items())
                            ||!right.equals(origin.confirmedState().pending().get(right.grant())))return false;
                }
                for(var claim:cursor.claimed().entrySet()) {
                    var receipt=candidate.receipts().get(claim.getValue());
                    if(receipt==null||!(receipt.operation().action() instanceof ClaimPending action)
                            ||!action.grant().equals(claim.getKey())||!receipt.operation().beneficiary().equals(cursor.beneficiary())
                            ||!candidate.receipts().containsKey(action.grantingReceipt()))return false;
                }
            }
            var expectedConcessions=new HashMap<ConcessionKey,EconomicOperationId>();var expectedWatermarks=new HashMap<SpawnKey,Long>();
            for(var entry:candidate.receipts().entrySet()) {
                var operation=entry.getValue().operation();if(!entry.getKey().equals(operation.id())||!candidate.entries().containsKey(operation.beneficiary()))return false;
                var key=concession(operation);if(key!=null&&expectedConcessions.put(key,operation.id())!=null)return false;
                if(operation.action() instanceof GrantReward reward&&expectedConcessions.put(grantIdentity(operation,reward.source()),operation.id())!=null)return false;
                if(operation.action() instanceof GrantReward reward&&reward.source().kind()==SourceKind.MOB)
                    expectedWatermarks.merge(spawn(operation,reward.source()),reward.source().mobGeneration(),Math::max);
            }
            return expectedConcessions.equals(candidate.concessions())&&expectedWatermarks.equals(candidate.watermarks());
        }
    }

    /** Falha de regra explícita da preparação, sem publicação parcial. */
    private static final class Rejected extends RuntimeException {
        final Code code;
        Rejected(Code code) { super(code.name());this.code=code; }
    }
    /** Resultado sem recibo não declara uma confirmação. */
    private static EconomicOperationResult result(Code code) { return new EconomicOperationResult(code,null,code.name()); }
    /** Concessão de missão é vitalícia neste experimento; mobs exigem época e geração. */
    private static ConcessionKey concession(EconomicOperation operation) {
        if(operation.action() instanceof GrantReward reward) {
            var source=reward.source();return new ConcessionKey(operation.beneficiary(),source.kind()+":"+source.namespace(),
                    source.sourceId(),source.kind()==SourceKind.MOB?operation.epoch():null,source.mobGeneration());
        }
        if(operation.action() instanceof ClaimPending claim)return new ConcessionKey(operation.beneficiary(),"claim",claim.grant().value().toString(),null,null);
        return null;
    }
    /** Watermark de morte é separado do ID da operação e da identidade da concessão. */
    private static SpawnKey spawn(EconomicOperation operation,EconomicSource source) {
        return new SpawnKey(operation.beneficiary(),operation.epoch(),source.namespace(),source.sourceId());
    }
    /** O mesmo direito não pode reaparecer com outro ID de operação ou em outra época. */
    private static ConcessionKey grantIdentity(EconomicOperation operation,EconomicSource source) {
        return new ConcessionKey(operation.beneficiary(),"grant-id",source.grant().value().toString(),null,null);
    }
    /** Restringe tamanho/alfabeto para o contrato sintético, sem normalizar identidades silenciosamente. */
    private static void identifier(String value) {
        if(value==null||!value.matches("[a-z][a-z0-9_.-]{0,63}"))throw new IllegalArgumentException("Identificador inválido");
    }
    /** Copia quantidades; ausência de loot é mapa vazio somente após resolução explícita. */
    private static Map<String,Integer> quantities(Map<String,Integer> values) {
        var copy=Map.copyOf(values);copy.forEach((id,n)->{identifier(id);if(n<1||n>9999)throw new IllegalArgumentException("Quantidade inválida");});return copy;
    }

    // --- Fingerprints canônicos: integridade experimental, nunca autenticação ---

    @FunctionalInterface private interface Encoding { void write(DataOutputStream out)throws IOException; }
    /** SHA-256 de representação binária determinística, independente da ordem de Map.copyOf. */
    private static String digest(Encoding encoding) {
        try {var bytes=new ByteArrayOutputStream();var out=new DataOutputStream(bytes);encoding.write(out);out.flush();
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes.toByteArray()));
        } catch(IOException|NoSuchAlgorithmException impossible) {throw new AssertionError(impossible);}
    }
    /** Fingerprint do estado absoluto, sem reconstruir recompensas. */
    static String stateHash(EconomicState state) {return digest(out->writeState(out,state));}
    /** Inclui intenção inteira, base e resultado: IDs iguais com conteúdo distinto não são retries. */
    private static String receiptHash(EconomicOperation operation,EconomicState before,EconomicState after) {
        return digest(out->{writeOperation(out,operation);out.writeUTF(stateHash(before));writeState(out,after);});
    }
    /** Serialização local do resultado; não é wire nem formato persistente aprovado. */
    private static void writeState(DataOutputStream out,EconomicState state)throws IOException {
        out.writeUTF(state.beneficiary().value().toString());out.writeLong(state.revision().value());out.writeLong(state.xp());out.writeInt(state.coins());
        out.writeUTF(state.domain().domain());out.writeLong(state.domain().xpHalfUnits());out.writeUTF(state.domain().lastReset()==null?"":state.domain().lastReset().toString());out.writeBoolean(state.domain().resetQuestComplete());
        out.writeInt(state.capacity());writeQuantities(out,state.inventory());
        var rights=state.pending().values().stream().sorted(Comparator.comparing(r->r.grant().value())).toList();out.writeInt(rights.size());
        for(var right:rights){out.writeUTF(right.grant().value().toString());out.writeUTF(right.grantingReceipt().value().toString());writeSource(out,right.source());writeQuantities(out,right.items());}
        var quests=state.quests().entrySet().stream().sorted(Comparator.comparing(e->e.getKey().key())).toList();out.writeInt(quests.size());
        for(var quest:quests){out.writeUTF(quest.getKey().key());out.writeUTF(quest.getValue().status().name());out.writeUTF(quest.getValue().deliveryReceipt()==null?"":quest.getValue().deliveryReceipt().value().toString());}
        var claims=state.claimed().entrySet().stream().sorted(Comparator.comparing(e->e.getKey().value())).toList();out.writeInt(claims.size());
        for(var claim:claims){out.writeUTF(claim.getKey().value().toString());out.writeUTF(claim.getValue().value().toString());}
    }
    /** Codifica intenção e todos os contextos sem usar toString de mapas. */
    private static void writeOperation(DataOutputStream out,EconomicOperation operation)throws IOException {
        out.writeUTF(operation.id().value().toString());out.writeUTF(operation.beneficiary().value().toString());out.writeLong(operation.expected().value());
        out.writeLong(operation.generation().value());out.writeLong(operation.epoch().value());
        if(operation.action() instanceof GrantReward grant){out.writeUTF("grant");writeSource(out,grant.source());
            out.writeLong(grant.requested().xp());out.writeInt(grant.requested().coins());writeQuantities(out,grant.requested().items());writeQuantities(out,grant.requested().consumedItems());}
        else if(operation.action() instanceof ClaimPending claim){out.writeUTF("claim");out.writeUTF(claim.grant().value().toString());out.writeUTF(claim.grantingReceipt().value().toString());}
        else if(operation.action() instanceof ConsumeItem consume){out.writeUTF("consume");out.writeUTF(consume.item());out.writeInt(consume.quantity());}
        else if(operation.action() instanceof MoveItem move){out.writeUTF("move");out.writeInt(move.from());out.writeInt(move.to());out.writeInt(move.quantity());}
    }
    /** Mantém definição, concessão e geração como campos distintos. */
    private static void writeSource(DataOutputStream out,EconomicSource source)throws IOException {
        out.writeUTF(source.kind().name());out.writeUTF(source.namespace());out.writeUTF(source.sourceId());out.writeUTF(source.definitionId());
        out.writeUTF(source.definitionVersion());out.writeBoolean(source.mobGeneration()!=null);if(source.mobGeneration()!=null)out.writeLong(source.mobGeneration());out.writeUTF(source.grant().value().toString());
    }
    /** Ordena mapas antes de codificar tamanho, chave e valor. */
    private static void writeQuantities(DataOutputStream out,Map<String,Integer> values)throws IOException {
        out.writeInt(values.size());for(var entry:new TreeMap<>(values).entrySet()){out.writeUTF(entry.getKey());out.writeInt(entry.getValue());}
    }
}
