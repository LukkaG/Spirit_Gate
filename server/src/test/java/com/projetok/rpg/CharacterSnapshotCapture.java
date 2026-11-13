package com.projetok.rpg;

import com.projetok.rpg.recovery.CharacterSnapshotValidator;
import com.projetok.rpg.recovery.SnapshotValidationContext;
import com.projetok.rpg.recovery.RecoverableCharacterSnapshot;
import java.time.Instant;
import java.util.*;
import static com.projetok.rpg.recovery.RecoverableCharacterSnapshot.*;

/**
 * Protótipo exclusivo de teste: lê mundos descartáveis confinados a um owner explícito.
 * Não é entrada administrativa, endpoint multiplayer, save ou exportador integral do runtime.
 */
final class CharacterSnapshotCapture {
    /** Estados completos/de política isolada são reservados ao contrato futuro, não produzidos aqui. */
    enum Outcome { COMPLETE_CANDIDATE, STRUCTURALLY_INVALID, INCOMPLETE_CAPTURE,
        SOURCE_UNAVAILABLE, SOURCE_REMOVED, POLICY_PENDING, INCOMPATIBLE_CONTEXT }
    /** Classifica cada lacuna sem substituir ausência por zero, vazio ou recibo inventado. */
    enum GapKind { INTERNAL_API_REQUIRED, NOT_PRESENT, POLICY_PENDING, SOURCE_UNAVAILABLE }
    /** Evidência de uma lacuna de captura ou política. */
    record Gap(String path, GapKind kind) { }

    /** Metadados explicitamente sintéticos do controlador do laboratório, não descobertos em produção. */
    record LaboratoryContext(UUID runtimeIdentity, CharacterId syntheticIdentity, String region,
            ContentHash content, RulesVersion rules, WorldEpoch epoch, StateRevision revision,
            Instant capturedAt) { }

    /**
     * Participação local controlada pela fixture. Seu flag não comprova admissão de peer em produção.
     * Todos os usos do mundo, inclusive preparação e retirada, devem permanecer no owner declarado.
     */
    static final class Source {
        private final Thread owner=Thread.currentThread();
        private final RpgWorld world;
        private LaboratoryContext context;
        private boolean present=true;
        private CharacterReadContext readContext;
        private RpgSkillEngine engine;
        private int participant;

        /** Vincula somente um mundo criado no próprio owner da fixture. */
        Source(RpgWorld world,LaboratoryContext context) {
            this.world=Objects.requireNonNull(world);this.context=Objects.requireNonNull(context);
            this.readContext=new CharacterReadContext(world);
        }
        /** Invalida a participação da fixture, sem alterar ou retirar qualquer personagem real. */
        void retire() { requireOwner();present=false;readContext.retire(); }
        /** Associa o executor real usado pela participação descartável, antes da leitura. */
        void bindSkills(RpgSkillEngine engine,int participant) {
            requireOwner();this.engine=Objects.requireNonNull(engine);this.participant=participant;
            readContext=new CharacterReadContext(world,engine);
        }
        /** Representa troca de metadados do controlador local entre operações, nunca durante a leitura. */
        void changeContext(LaboratoryContext next) { requireOwner();context=Objects.requireNonNull(next);readContext=new CharacterReadContext(world,engine); }
        /** Impede que o controlador local altere sua evidência fora da thread declarada. */
        private void requireOwner() {
            if(Thread.currentThread()!=owner)throw new IllegalStateException("Operação fora do owner do laboratório");
        }
    }

    /** Identidade efêmera observada, mantida separada do CharacterId sintético fornecido. */
    record IdentityObservation(UUID ephemeralId,String name,String race,String appearance) { }
    /** Recursos de apresentação; exactState também conserva a fração real do runtime. */
    record ResourceObservation(int hp,int absoluteEnergy,LifeState life) { }
    /** Valores efetivos de apresentação; os investimentos brutos ficam em exactState. */
    record AttributeObservation(int effectiveAgility,int trainedEnergy,int maximumEnergy) { }
    /** Somente contadores de apresentação: não são deadlines exatos exportáveis. */
    record RemainingView(int basicAttackTicks,int shunpoTicks,int potionTicks) { }
    /** Resumo legado de apresentação; não participa da conversão do ledger nem de recibos. */
    record QuestSummary(String id,String status,int progress,int required,String description) { }
    /** Valores imutáveis observados, sem guardar mundo, inventário vivo ou engine. */
    record Observations(IdentityObservation identity,ResourceObservation resources,
            AttributeObservation attributes,RemainingView remaining,List<QuestSummary> authoredSummaries, CharacterReadState exactState, RpgSkillEngine.TimingRead skillTiming) {
        /** Copia a lista de resumos, cujos elementos também são records imutáveis. */
        Observations { authoredSummaries=List.copyOf(authoredSummaries); }
    }
    /** Resultado sem operação de instalação; candidate/observations são nulos quando a fonte é recusada. */
    record CaptureResult(Outcome outcome,RecoverableCharacterSnapshot candidate,Observations observations,
            List<Gap> gaps,CharacterSnapshotValidator.Result validation) {
        /** Preserva o diagnóstico sem expor a coleção mutável usada na construção. */
        CaptureResult { gaps=List.copyOf(gaps); }
    }

    /**
     * Captura uma única operação no owner. Não avança tick, materializa atributos, monta save,
     * consulta cliente, executa habilidade, resolve missão ou faz I/O.
     */
    CaptureResult capture(Source source,LaboratoryContext expected,SnapshotValidationContext validationContext) {
        if(source==null||Thread.currentThread()!=source.owner)
            return rejected(Outcome.SOURCE_UNAVAILABLE,"source.owner/presence");
        if(!source.present)return rejected(Outcome.SOURCE_REMOVED,"source.removed");
        if(expected==null||!source.context.equals(expected))return rejected(Outcome.INCOMPATIBLE_CONTEXT,"context");
        var world=source.world;
        var before=world.networkState();
        final CharacterReadState exact;
        final RpgSkillEngine.TimingRead timing;
        try {
            exact=world.readCharacterState(source.readContext);
            timing=source.engine==null?null:source.engine.readTiming(source.participant,world,
                    Map.of(source.participant,world),source.readContext);
        } catch(IllegalStateException|IllegalArgumentException failure) {
            return rejected(Outcome.INCOMPATIBLE_CONTEXT,"read.context");
        }
        if(timing!=null && timing.status()==RpgSkillEngine.TimingStatus.INCOMPATIBLE)
            return rejected(Outcome.INCOMPATIBLE_CONTEXT,"skills.binding");
        if(!Objects.equals(before.identity().id(),expected.runtimeIdentity())||!Objects.equals(before.region(),expected.region()))
            return rejected(Outcome.INCOMPATIBLE_CONTEXT,"identity/region");

        // Leituras observacionais existentes, todas no mesmo ponto do owner sem executar comandos.
        var domain=exact.domain();
        var stacks=exact.slots();
        var slots=new ArrayList<Slot>();
        for(int index=0;index<stacks.size();index++) {
            var stack=stacks.get(index);
            if(stack!=null)slots.add(new Slot(index,stack.id(),stack.quantity()));
        }
        var equipment=new ArrayList<Equipment>();
        new TreeMap<>(exact.equipment()).forEach((slot,item)->equipment.add(new Equipment(slot,item)));
        var abilities=world.abilityView();
        var summaries=world.authoredQuestViews().stream().map(q->new QuestSummary(q.id(),q.state().name(),
                q.progress(),q.required(),q.objective())).toList();
        var observations=new Observations(new IdentityObservation(before.identity().id(),before.identity().name(),
                before.identity().race(),before.identity().appearance()),
                new ResourceObservation(before.hp(),before.stamina(),before.hp()==0?LifeState.DEAD:LifeState.ALIVE),
                new AttributeObservation(world.skillAgility(),world.trainedEnergy(),world.maximumEnergy()),
                new RemainingView(abilities.attackTicks(),abilities.shunpoTicks(),abilities.potionTicks()),summaries,exact,timing);

        // Guardas observacionais complementam o confinamento; não substituem exclusão de escritores.
        if(!source.present||!source.context.equals(expected)||!before.equals(world.networkState()))
            return rejected(Outcome.INCOMPATIBLE_CONTEXT,"capture.changed");

        var candidate=new RecoverableCharacterSnapshot(expected.syntheticIdentity(),new SchemaVersion(1),
                expected.revision(),expected.content(),expected.rules(),
                new Capture(expected.epoch(),new SourceTick(before.ticks()),expected.capturedAt(),ClockOrigin.MONOTONIC_WORLD_TICKS,1),
                null,new Progression(exact.xp(),domain.domain(),domain.xpHalfUnits(),domain.lastReset(),domain.resetQuestComplete()),
                attributes(exact),new Inventory(exact.capacity(),slots),equipment,new Wallet(before.coins()),
                null,null,null,null,new Resources(exact.hp(),exact.energy(),exact.regenerationRemainder(),
                        exact.hp()==0?LifeState.DEAD:LifeState.ALIVE),null,new Location(before.region(),before.x(),before.y(),null),null);
        var validation=new CharacterSnapshotValidator().validate(candidate,validationContext);
        var outcome=validation.violations().stream().anyMatch(v->v.code()!=CharacterSnapshotValidator.Code.REQUIRED)
                ?Outcome.STRUCTURALLY_INVALID:Outcome.INCOMPLETE_CAPTURE;
        return new CaptureResult(outcome,candidate,observations,gaps(exact,timing),validation);
    }

    /** Converte somente modo manual conhecido; o bruto desabilitado permanece nas observações. */
    private Attributes attributes(CharacterReadState state) {
        if(!state.manualAttributesEnabled())return null;
        var raw=state.rawAttributes();
        return new Attributes(AttributeMode.MANUAL_SESSION,raw.strength(),raw.intelligence(),raw.agility(),
                raw.energy(),raw.unspentPoints(),raw.level());
    }

    /** Recusa contexto/fonte sem produzir candidato parcial a partir de dados de outra participação. */
    private CaptureResult rejected(Outcome outcome,String path) {
        return new CaptureResult(outcome,null,null,List.of(new Gap(path,GapKind.SOURCE_UNAVAILABLE)),null);
    }

    /** Lacunas comprovadas no checkout; não são registros econômicos nem valores de personagem. */
    private List<Gap> gaps(CharacterReadState exact,RpgSkillEngine.TimingRead timing) {
        var gaps=new ArrayList<>(List.of(
                new Gap("characterId.authority",GapKind.POLICY_PENDING),
                new Gap("capture.runtimeContext",GapKind.INTERNAL_API_REQUIRED),
                new Gap("identity.cosmetics",GapKind.INTERNAL_API_REQUIRED),
                new Gap("cooldowns.policy/clock",GapKind.POLICY_PENDING),
                new Gap("quests.definitionVersion/deliveryProof",GapKind.NOT_PRESENT),
                new Gap("receipts",GapKind.NOT_PRESENT),
                new Gap("watermarks.durableEpoch",GapKind.NOT_PRESENT),
                new Gap("pendingLoot",GapKind.NOT_PRESENT),
                new Gap("restrictions.externalEvidence",GapKind.NOT_PRESENT),
                new Gap("restrictions.policy",GapKind.POLICY_PENDING)));
        if(!exact.manualAttributesEnabled())gaps.add(new Gap("attributes.mode",GapKind.POLICY_PENDING));
        if(timing==null || timing.status()!=RpgSkillEngine.TimingStatus.PRESENT)
            gaps.add(new Gap("cooldowns.enginePresence",timing==null?GapKind.INTERNAL_API_REQUIRED:GapKind.NOT_PRESENT));
        return List.copyOf(gaps);
    }
}
