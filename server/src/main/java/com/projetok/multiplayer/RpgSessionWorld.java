package com.projetok.multiplayer;

import com.projetok.rpg.*;
import com.projetok.domain.entity.*;
import com.projetok.domain.movement.CollisionService;
import java.util.*;
import com.projetok.prototype.rpg.CosmeticCatalog;

/** Região compartilhada experimental; movimento usa RpgWorld no servidor, sem campanha pessoal. */
public final class RpgSessionWorld implements SessionSimulation {
    // --- Captura interna no owner real; nenhum comando ou retorno no protocolo ---

    private volatile Thread captureOwner;
    private java.util.function.IntPredicate capturePeerActive;

    /** Vincula a capacidade local no primeiro trabalho do executor real do servidor. */
    void bindCaptureOwner(Thread owner, java.util.function.IntPredicate peerActive) {
        if (owner != Thread.currentThread() || captureOwner != null)
            throw new IllegalStateException("Owner de captura já vinculado ou incorreto");
        capturePeerActive = Objects.requireNonNull(peerActive);
        captureOwner = Objects.requireNonNull(owner);
    }

    /** Token opaco ligado ao objeto Session atual; não é credencial, geração ou fence durável. */
    static final class CharacterCaptureRequest {
        private final RpgSessionWorld source;
        private final int participant;
        private final Session session;
        private final com.projetok.studio.LiveRevision content;
        private final String region, domain;

        /** Retém contexto local para detectar troca de sessão mesmo com ID/UUID reutilizado. */
        private CharacterCaptureRequest(RpgSessionWorld source, int participant, Session session) {
            this.source=source; this.participant=participant; this.session=session; content=source.content;
            region=session==null?null:session.world.networkState().region();
            domain=session==null?null:session.world.domainProgression().domain();
        }
    }

    /** Diagnósticos de recusa não carregam dados parciais de personagem. */
    enum CharacterCaptureStatus {
        CAPTURED, WRONG_OWNER, INVALID_REQUEST, ABSENT, REMOVED, REPLACED, DISCONNECTING,
        CONTENT_CHANGED, REGION_CHANGED, DOMAIN_CHANGED, TRANSITION, SKILL_BINDING_MISMATCH, READ_FAILED
    }

    /** Origens distintas; a instância UUID é efêmera, sem época durável ou revisão de personagem. */
    record CharacterCaptureMetadata(UUID sessionWorldInstance, long sessionTick, long worldTick,
            long skillEngineClock, Long contentRevision, String contentHash, Long durableWorldEpoch) { }

    /** Observação interna imutável, sem capacidade de instalar ou persistir o personagem. */
    record CharacterCaptureResult(CharacterCaptureStatus status, int participant, String detail,
            CharacterCaptureMetadata metadata, RpgCharacterObservation observation,
            CosmeticCatalog.Selection cosmetics) {
        /** Falta prova econômica transacional; observação não completa esse contrato. */
        boolean economicHistoryComplete() { return false; }
        /** Esta operação nunca autoriza ativação de estado recuperável. */
        boolean activationAuthorized() { return false; }
    }

    /** Cria a expectativa a partir da sessão real, exclusivamente no owner já vinculado. */
    CharacterCaptureRequest characterCaptureRequest(int participant) {
        if (captureOwner == null || Thread.currentThread() != captureOwner)
            throw new IllegalStateException("Pedido fora do owner real");
        return new CharacterCaptureRequest(this, participant, sessions.get(participant));
    }

    /**
     * Copia estado e timing no mesmo ponto serializado, sem avanço, concessão ou limpeza.
     * A presença do peer é conferida antes e depois para cobrir close entre departures e comandos.
     */
    CharacterCaptureResult captureCharacter(CharacterCaptureRequest request) {
        int id=request==null?0:request.participant;
        if (captureOwner == null || Thread.currentThread() != captureOwner)
            return captureRejected(CharacterCaptureStatus.WRONG_OWNER,id,"owner");
        if (request==null || request.source!=this || id<1)
            return captureRejected(CharacterCaptureStatus.INVALID_REQUEST,id,"request");
        var session=sessions.get(id);
        if (session==null) return captureRejected(request.session==null?CharacterCaptureStatus.ABSENT:
                CharacterCaptureStatus.REMOVED,id,"session");
        if (content!=request.content)
            return captureRejected(CharacterCaptureStatus.CONTENT_CHANGED,id,"content");
        if (session!=request.session)
            return captureRejected(CharacterCaptureStatus.REPLACED,id,"session.binding");
        if (!capturePeerActive.test(id) || disconnectRequested(id))
            return captureRejected(CharacterCaptureStatus.DISCONNECTING,id,"peer");
        if (frozen) return captureRejected(CharacterCaptureStatus.TRANSITION,id,"frozen");
        var state=session.world.networkState();
        if (!Objects.equals(request.region,state.region()))
            return captureRejected(CharacterCaptureStatus.REGION_CHANGED,id,"region");
        if (!Objects.equals(request.domain,session.world.domainProgression().domain()))
            return captureRejected(CharacterCaptureStatus.DOMAIN_CHANGED,id,"domain");
        final RpgCharacterObservation observation;
        try {
            observation=RpgCharacterObservation.read(captureOwner,id,session.world,skills,skillPlayers());
        } catch (IllegalStateException | IllegalArgumentException failure) {
            return captureRejected(CharacterCaptureStatus.READ_FAILED,id,
                    failure.getClass().getSimpleName()+": "+failure.getMessage());
        }
        if (observation.timing().status()==RpgSkillEngine.TimingStatus.INCOMPATIBLE)
            return captureRejected(CharacterCaptureStatus.SKILL_BINDING_MISMATCH,id,"skills.binding");
        if (observation.character().restrictions().transitioning())
            return captureRejected(CharacterCaptureStatus.TRANSITION,id,"world.transition");
        if (!capturePeerActive.test(id) || disconnectRequested(id))
            return captureRejected(CharacterCaptureStatus.DISCONNECTING,id,"peer.afterRead");
        var metadata=new CharacterCaptureMetadata(instance,tick,observation.character().tick(),
                observation.timing().engineClock(),content==null?null:content.number(),
                content==null?null:content.hash(),null);
        return new CharacterCaptureResult(CharacterCaptureStatus.CAPTURED,id,"read-only",metadata,
                observation,session.appearance);
    }

    /** Retorna somente diagnóstico, sem entregar parte de uma cópia recusada. */
    private CharacterCaptureResult captureRejected(CharacterCaptureStatus status,int id,String detail) {
        return new CharacterCaptureResult(status,id,detail,null,null,null);
    }

    // Permite sequências longas de skills dentro do limite de HP já aceito pelo protocolo.
    private static final int CONTENT_ENEMY_MIN_HP=10_000;
    private float playerRadius(){return content==null?12:content.profile().actorRadius();}
    private final List<String> races;
    private final CosmeticCatalog cosmetics;
    private final Map<Integer,Session> sessions=new TreeMap<>();
    private CollisionService.Space geometry=com.projetok.domain.movement.RpgSessionGeometry.raw();
    private long tick;
    private final UUID instance=UUID.randomUUID();
    private SharedRpgEncounter encounter;
    private final RpgSkillEngine skills=new RpgSkillEngine();
    private com.projetok.studio.LiveRevision content;
    private RevisionCoordinator revisions;
    private boolean frozen;
    private final int syntheticLevel;
    private static final class Session {
        final RpgWorld world;final GameEntity proxy;final int seat;Position home;int application=3;
        long sequence,inputTick,commandTick=-1;int commands;float x,y,fx=1,fy;
        int target,allyTarget;long targetGeneration;
        CosmeticCatalog.Selection appearance=CosmeticCatalog.Selection.none();
        Session(RpgWorld world,GameEntity proxy,int seat){this.world=world;this.proxy=proxy;this.seat=seat;this.home=proxy.getPosition();}
    }
    public RpgSessionWorld(){this(List.of("reaper","human"));}
    /** Raças são configuração explícita do servidor, não atributos enviados pelo cliente. */
    public RpgSessionWorld(List<String> races){this(races,null);}
    public RpgSessionWorld(List<String> races,CosmeticCatalog cosmetics){this(races,cosmetics,false);}
    public RpgSessionWorld(List<String> races,CosmeticCatalog cosmetics,boolean combat){this(races,cosmetics,combat,1);}
    public RpgSessionWorld(List<String> races,CosmeticCatalog cosmetics,boolean combat,int syntheticLevel){this(races,cosmetics,combat,syntheticLevel,SharedRpgEncounter.RewardPolicy.SYNTHETIC_PARTICIPANTS);}
    public RpgSessionWorld(List<String> races,CosmeticCatalog cosmetics,boolean combat,int syntheticLevel,SharedRpgEncounter.RewardPolicy rewardPolicy){this.races=List.copyOf(races);this.cosmetics=cosmetics;if(races.size()!=2||syntheticLevel!=1&&syntheticLevel!=20)throw new IllegalArgumentException("Dois perfis sinteticos; nivel 1 ou 20");this.syntheticLevel=syntheticLevel;races.forEach(CharacterRaces::get);encounter=combat?new SharedRpgEncounter(geometry,rewardPolicy):null;}
    public void enableRevisions(com.projetok.studio.LiveRevision initial){enableRevisions(initial,initial.bundle().assets());}
    /** Inicia o mundo multiplayer com mobs resistentes; offline e Coop conservam a vida do catálogo. */
    public void enableRevisions(com.projetok.studio.LiveRevision initial,com.projetok.studio.AssetCatalog.Result assets){
        if(!sessions.isEmpty()||encounter==null)throw new IllegalStateException("Habilite antes de iniciar");
        content=initial;geometry=initial.geometry().get("forest");
        encounter=new SharedRpgEncounter(initial.geometry(),initial.bundle().scenes().enemies(),SharedRpgEncounter.RewardPolicy.SYNTHETIC_PARTICIPANTS,CONTENT_ENEMY_MIN_HP);
        revisions=new RevisionCoordinator(this,initial,assets);
    }
    public String admissionFailure(){return supportsContent()?"Revisao exige cliente V4 ou V5, personagem invalido, publicacao em andamento ou sessao cheia":"Servidor cheio";}
    public void closed(){if(revisions!=null)revisions.shutdown();}
    public RevisionCoordinator revisions(){return revisions;}
    public boolean supportsContent(){return revisions!=null;}
    public boolean supportsCharacterCreation(){return supportsContent();}
    public boolean join(int id,String name,int application){if(Protocol.supportsCharacterApplication(application))return false;if(revisions!=null&&!revisions.admit(id,application))return false;boolean ok=join(id,name);if(!ok&&revisions!=null)revisions.depart(id);if(ok)sessions.get(id).application=application;return ok;}
    /** Valida toda a escolha antes de reservar revisao ou criar um ator no mundo. */
    public boolean join(int id,SessionCharacterSelection selection,int application){
        if(!Protocol.supportsCharacterApplication(application)||!supportsCharacterCreation()||selection==null)return false;
        if(id<1||id>1_000_000||sessions.containsKey(id)||sessions.size()>=2)return false;
        var appearance=selection.cosmetics();
        if(cosmetics==null&&!appearance.equals(CosmeticCatalog.Selection.none()))return false;
        if(cosmetics!=null)try{cosmetics.validate(appearance);}catch(IllegalArgumentException invalid){return false;}
        if(!revisions.admit(id,application))return false;
        boolean ok=false;
        try{
            ok=joinCharacter(id,selection.name(),selection.race(),appearance);
            if(ok)sessions.get(id).application=application;
            return ok;
        }finally{if(!ok)revisions.depart(id);}
    }
    public void contentAck(int id,Protocol.Content message){if(revisions!=null)revisions.ack(id,message);}
    public Protocol.Content nextContent(int id){return revisions==null?null:revisions.next(id);}
    public boolean disconnectRequested(int id){return revisions!=null&&revisions.disconnect(id);}
    public String contentHash(){return content==null?"":content.hash();}
    public void freeze(boolean value){frozen=value;if(value)sessions.values().forEach(s->{s.x=s.y=0;s.target=0;s.allyTarget=0;s.targetGeneration=0;});}
    public record RevisionCandidate(com.projetok.studio.LiveRevision content,Map<Integer,Session> players,SharedRpgEncounter encounter){}
    public RevisionCandidate capture(){return new RevisionCandidate(content,new TreeMap<>(sessions),encounter);}
    public RevisionCandidate prepareRevision(com.projetok.studio.LiveRevision next){
        var population=encounter.fork(next.geometry(),next.bundle().scenes().enemies());var players=new TreeMap<Integer,Session>();
        for(var e:sessions.entrySet()){var old=e.getValue();var world=old.world.forkSyntheticRevision(next.bundle().scenes(),next.profile());var v=world.networkState();var copy=new Session(world,new GameEntity(e.getKey(),v.identity().name(),new Position(v.x(),v.y())){},old.seat);copy.application=old.application;copy.home=next.bundle().scenes().maps().get("forest").spawn();copy.sequence=old.sequence;copy.inputTick=tick-16;copy.fx=old.fx;copy.fy=old.fy;copy.appearance=old.appearance;players.put(e.getKey(),copy);}
        for(var e:players.entrySet()){var v=e.getValue().world.networkState();var bodies=new ArrayList<CollisionService.Body>();for(var other:players.entrySet())if(other.getKey()!=e.getKey()){var o=other.getValue().world.networkState();if(o.hp()>0&&o.region().equals(v.region()))bodies.add(CollisionService.Body.block(other.getValue().proxy,next.profile().actorRadius()));}for(var enemy:population.enemies(v.region(),tick))if(enemy.hp()>0)bodies.add(new CollisionService.Body(new GameEntity(enemy.id(),enemy.name(),new Position(enemy.x(),enemy.y())){},19));if(!e.getValue().world.syntheticPositionFree(bodies))throw new IllegalArgumentException("Jogador em posicao invalida");population.join(e.getKey(),e.getValue().world);}
        return new RevisionCandidate(next,players,population);
    }
    public void installRevision(RevisionCandidate candidate){skills.clear();sessions.clear();sessions.putAll(candidate.players());encounter=candidate.encounter();content=candidate.content();geometry=content.geometry().get("forest");}
    public boolean supportsGameplay(){return supportsCharacterCreation();}
    public boolean supportsSkills(){return supportsGameplay()&&encounter!=null;}
    /** A coleção pertence à thread do mundo; o executor nunca recebe objetos de socket ou UI. */
    private Map<Integer,RpgWorld> skillPlayers(){
        var players=new TreeMap<Integer,RpgWorld>();sessions.forEach((id,s)->players.put(id,s.world));return players;
    }
    /** Revalida aplicação, sequência e geração antes de lançar qualquer habilidade. */
    public boolean skill(int id,SkillProtocol.Request request){
        var s=sessions.get(id);
        if(frozen||s==null||s.application!=Protocol.SKILL_APPLICATION||request.sequence()<=s.sequence)return false;
        if(s.commandTick!=tick){s.commandTick=tick;s.commands=0;}if(++s.commands>8)return false;
        s.sequence=request.sequence();long generation=request.generation();
        if(request.targetId()>1_000_000){
            validateTarget(s);
            if(s.target==0||request.targetId()!=s.target+1_000_000
                    ||generation!=0&&generation!=s.targetGeneration)return false;
            generation=s.targetGeneration;
        }
        s.world.setSyntheticBodies(bodies(id));skills.clock(tick);
        boolean accepted=skills.cast(id,s.world,new RpgSkillEngine.Cast(request.slot(),request.targetId(),
                generation,request.x(),request.y()),skillPlayers());
        if(accepted){var v=s.world.networkState();s.proxy.setPosition(new Position(v.x(),v.y()));
            var facing=s.world.combatFacing();s.fx=facing.x();s.fy=facing.y();}
        return accepted;
    }
    /** Apenas recargas do destinatário são privadas; efeitos visíveis ficam limitados à região. */
    public SkillProtocol.State skillsSnapshotFor(int id){
        var s=sessions.get(id);if(s==null||s.application!=Protocol.SKILL_APPLICATION)
            throw new IllegalArgumentException("Sessão de habilidades indisponível");
        var state=skills.state(id);
        var events=skills.events().stream().filter(e->sessions.containsKey(e.player())
                &&region(e.player()).equals(region(id))).map(e->new SkillProtocol.Event(e.sequence(),e.tick(),
                e.player(),e.skill(),e.fromX(),e.fromY(),e.x(),e.y())).toList();
        return new SkillProtocol.State(contentHash(),tick,events,state.cooldowns(),state.sentryShots(),state.windCheckpoint());
    }
    public GameplayProtocol.State gameplaySnapshotFor(int id){
        var s=sessions.get(id);if(s==null)throw new IllegalArgumentException("Sessao ausente");
        var p=s.world.progressView();
        return new GameplayProtocol.State(contentHash(),combatSnapshotFor(id),new GameplayProtocol.Sheet(s.world.inventory().snapshot(),s.world.equippedItems(),new GameplayProtocol.Progress(p.xp(),p.level(),p.nextLevelXp(),p.attack(),p.defense(),p.domain()),new GameplayProtocol.Abilities(s.world.abilityView().attackTicks(),s.world.abilityView().shunpoTicks(),s.world.abilityView().potionTicks(),s.world.abilityView().basicRange()),s.world.networkMessage()));
    }
    public boolean gameplay(int id,GameplayProtocol.Request request){
        var s=sessions.get(id);if(frozen||s==null||!Protocol.supportsGameplayApplication(s.application)||request.sequence()<=s.sequence)return false;
        if(s.commandTick!=tick){s.commandTick=tick;s.commands=0;}if(++s.commands>8)return false;
        s.sequence=request.sequence();
        try{return switch(request.operation()){
            case EQUIP->s.world.equip(request.target());case UNEQUIP->s.world.unequip(request.target());
            case USE->s.world.use(request.target());case CHOOSE_DOMAIN->{boolean chosen=s.world.chooseDomain(request.target());if(chosen)skills.remove(id);yield chosen;}case COLLECT->{if(!request.target().equals("pending")||s.world.networkState().hp()==0)yield false;s.world.collectLoot();yield true;}
            case SELECT_TARGET->selectTarget(s,request.target());
            case BASIC_ATTACK,SHUNPO_OFFENSIVE->targetAction(id,s,request.operation(),request.target());
            case QUERY_ATTRIBUTES->{if(!request.target().equals("self"))yield false;s.world.manualAttributes();yield true;}
            case QUERY_ENERGY->{if(!request.target().equals("self"))yield false;s.world.manualAttributes();yield true;}
            case ALLOCATE_ATTRIBUTE->s.world.allocateAttribute(request.target());
        };}catch(IllegalArgumentException invalid){return false;}
    }
    public GameplayProtocol.Attributes attributesSnapshotFor(int id){var s=sessions.get(id);if(s==null||!Protocol.supportsGameplayApplication(s.application))throw new IllegalArgumentException("Sessão de atributos indisponível");var a=s.world.manualAttributes();return new GameplayProtocol.Attributes(contentHash(),a.strength(),a.intelligence(),a.agility(),a.level(),a.unspentPoints());}
    public GameplayProtocol.Energy energySnapshotFor(int id,long sequence){var s=sessions.get(id);if(s==null||!Protocol.supportsGameplayApplication(s.application))throw new IllegalArgumentException("Sessão de energia indisponível");s.world.manualAttributes();return new GameplayProtocol.Energy(contentHash(),tick,sequence,s.world.trainedEnergy(),s.world.networkState().stamina(),s.world.maximumEnergy(),com.projetok.domain.combat.EnergyRules.regenerationPermille(s.world.trainedEnergy()));}
    /** O alvo pretendido acompanha a ação: seleção inválida não vira ataque livre ou avanço. */
    private boolean targetAction(int id,Session s,GameplayProtocol.Operation operation,String intended){
        validateTarget(s);boolean free=intended.equals("none");
        if(s.allyTarget!=0)return false;
        if(!free&&(!intended.matches("enemy_[0-9]{7}")||s.target==0||Integer.parseInt(intended.substring(6))!=s.target+1_000_000))return false;
        s.world.setSyntheticBodies(bodies(id));skills.clock(tick);boolean accepted;
        if(s.application==Protocol.SKILL_APPLICATION&&operation==GameplayProtocol.Operation.BASIC_ATTACK&&skills.state(id).sentryShots()>0){
            if(free)return false;
            accepted=skills.sentryAttack(id,s.world,new RpgSkillEngine.Cast(2,s.target+1_000_000,s.targetGeneration,0,0),skillPlayers());
        }else if(s.application==Protocol.SKILL_APPLICATION&&operation==GameplayProtocol.Operation.BASIC_ATTACK&&s.world.skillBowUser()){
            accepted=skills.basicArrow(id,s.world,skillPlayers());
        }else accepted=operation==GameplayProtocol.Operation.BASIC_ATTACK?(free?s.world.attack():s.world.attackTarget(s.target,s.targetGeneration)):(free?s.world.castForwardAbility():s.world.castAbilityTarget(s.target,s.targetGeneration));
        if(!free||accepted){var facing=s.world.combatFacing();s.fx=facing.x();s.fy=facing.y();}if(accepted){var v=s.world.networkState();s.proxy.setPosition(new Position(v.x(),v.y()));}return accepted;
    }
    /** Seleção única: V7 admite aliados vivos ou derrotados sem convertê-los em vítimas de combate. */
    private boolean selectTarget(Session session,String intended){
        if(intended.equals("none")){session.target=0;session.allyTarget=0;session.targetGeneration=0;return true;}
        if(session.world.networkState().hp()<=0)return false;
        if(session.application==Protocol.SKILL_APPLICATION&&intended.matches("ally_[1-9][0-9]{0,6}")){
            int id=Integer.parseInt(intended.substring(5));var ally=sessions.get(id);
            if(ally==null||!ally.world.networkState().region().equals(session.world.networkState().region()))return false;
            session.target=0;session.targetGeneration=0;session.allyTarget=id;return true;
        }
        if(!intended.matches("enemy_[0-9]{7}"))return false;
        int id=Integer.parseInt(intended.substring(6))-1_000_000;var enemy=session.world.combatTarget(id);
        if(enemy==null)return false;
        session.allyTarget=0;session.target=id;session.targetGeneration=enemy.generation();return true;
    }
    public boolean supportsCombat(){return encounter!=null;}
    public boolean supportsCosmetics(){return cosmetics!=null;}
    public boolean appearance(int id,Protocol.AppearanceRequest request){var s=sessions.get(id);if(frozen||cosmetics==null||s==null||request.sequence()<=s.sequence)return false;if(s.commandTick!=tick){s.commandTick=tick;s.commands=0;}if(++s.commands>8)return false;var selection=new CosmeticCatalog.Selection(request.hair(),request.clothing());if(Protocol.supportsCharacterApplication(s.application)&&"reaper".equals(race(id))&&!selection.equals(CosmeticCatalog.Selection.none()))return false;try{cosmetics.validate(selection);}catch(IllegalArgumentException e){return false;}s.sequence=request.sequence();s.appearance=selection;return true;}
    public List<Protocol.Appearance> appearances(){return sessions.entrySet().stream().map(e->new Protocol.Appearance(e.getKey(),race(e.getKey()),e.getValue().appearance.hair(),e.getValue().appearance.clothing())).toList();}
    public boolean join(int id,String name){
        return joinCharacter(id,name,null,CosmeticCatalog.Selection.none());
    }
    private boolean joinCharacter(int id,String name,String selectedRace,CosmeticCatalog.Selection appearance){
        new Protocol.Hello(1,name);if(id<1||id>1_000_000||sessions.containsKey(id)||sessions.size()>=2)return false;
        int seat=sessions.values().stream().anyMatch(s->s.seat==0)?1:0;var race=CharacterRaces.get(selectedRace==null?races.get(seat):selectedRace);var spawn=content==null?new Position(140,seat==0?300:410):content.bundle().scenes().maps().get("forest").spawn();if(content!=null&&seat==1&&spawn.y()+110<=622)spawn=new Position(spawn.x(),spawn.y()+110);
        String region=encounter==null?"village":"forest";
        var spawnBodies=new ArrayList<CollisionService.Body>();
        for(var entry:sessions.entrySet())if(region(entry.getKey()).equals(region)&&entry.getValue().world.networkState().hp()>0&&!entry.getValue().world.skillIntangible())spawnBodies.add(CollisionService.Body.block(entry.getValue().proxy,playerRadius()));
        if(encounter!=null)for(var enemy:encounter.enemies(tick))if(enemy.hp()>0)spawnBodies.add(new CollisionService.Body(new GameEntity(enemy.id(),enemy.name(),new Position(enemy.x(),enemy.y())){},19));
        var spawnSpace=com.projetok.domain.movement.RpgCollisionProfile.scenery(new CollisionService.Space(960,640,geometry.obstacles(),spawnBodies),content==null?com.projetok.domain.movement.SessionCollisionProfile.DEFAULT:content.profile());var probe=new GameEntity(id,name,spawn){};
        Position safe=null;for(float offset:new float[]{0,40,-40,80,-80})if(new CollisionService().isFree(probe,spawn.x()+offset,spawn.y(),playerRadius(),spawnSpace)){safe=new Position(spawn.x()+offset,spawn.y());break;}
        if(safe==null)return false;spawn=safe;
        var maps=encounter==null?Map.of(region,new RpgSceneSet.Geometry(geometry,spawn)):Map.of("forest",new RpgSceneSet.Geometry(geometry,spawn),"village",new RpgSceneSet.Geometry(geometry,new Position(140,320)));
        var scenes=new RpgSceneSet(region,maps,Map.of(),encounter==null?Map.of():Map.of("elder","village|190|320"),Map.of(),encounter==null?Map.of():Map.of("to_village","forest|140|300|village|140|320","to_forest","village|140|320|forest|140|300"));
        var identity=new CharacterIdentity(UUID.nameUUIDFromBytes((instance+":"+id).getBytes(java.nio.charset.StandardCharsets.UTF_8)),name,race.id(),"azure","village",race.hp(),race.attack());if(content!=null){var authored=content.bundle().scenes();var adjusted=new TreeMap<>(authored.maps());adjusted.put("forest",new RpgSceneSet.Geometry(adjusted.get("forest").space(),spawn));scenes=new RpgSceneSet("forest",adjusted,authored.enemies(),authored.npcs(),authored.objects(),authored.portals());}var world=encounter==null?new RpgWorld(identity,scenes):new RpgWorld(identity,scenes,questBook());if(content==null)world.useNetworkFootprint();else world.configureSyntheticCollision(content.profile());var proxy=new GameEntity(id,name,spawn){};
        if(encounter!=null){encounter.join(id,world);world.awardExperience(RpgProgression.builtin().threshold(syntheticLevel));}var session=new Session(world,proxy,seat);session.appearance=appearance;sessions.put(id,session);return true;
    }
    public void leave(int id){skills.remove(id);if(revisions!=null)revisions.depart(id);var s=sessions.remove(id);if(s!=null&&encounter!=null)encounter.leave(s.world);}
    private static QuestBook questBook(){try(var in=RpgSessionWorld.class.getResourceAsStream("/rpg/corujao-v.sgquest")){if(in==null)throw new IllegalStateException("Missao ausente");return QuestDocuments.decode(in.readAllBytes());}catch(java.io.IOException e){throw new IllegalStateException(e);}}
    /** Executada somente pela capacidade administrativa na thread proprietária do servidor. */
    boolean visitEditorRegion(int id,String destination){
        var session=sessions.get(id);if(session==null||content==null||frozen)return false;
        var occupied=new ArrayList<CollisionService.Body>();
        for(var other:sessions.entrySet())if(other.getKey()!=id&&other.getValue().world.networkState().hp()>0&&region(other.getKey()).equals(destination))occupied.add(CollisionService.Body.block(other.getValue().proxy,playerRadius()));
        for(var enemy:encounter.enemies(destination,tick))if(enemy.hp()>0)occupied.add(new CollisionService.Body(new GameEntity(enemy.id(),enemy.name(),new Position(enemy.x(),enemy.y())){},19));
        if(!session.world.enterEditorRegion(destination,occupied))return false;
        session.proxy.setPosition(new Position(session.world.networkState().x(),session.world.networkState().y()));session.inputTick=tick-16;return true;
    }
    public String region(int id){var s=sessions.get(id);if(s==null)throw new IllegalArgumentException("Sessao ausente");return s.world.networkState().region();}
    public boolean request(int id,Protocol.WorldRequest request){
        var s=sessions.get(id);if(frozen||encounter==null||s==null||request.sequence()<=s.sequence)return false;
        if(s.commandTick!=tick){s.commandTick=tick;s.commands=0;}if(++s.commands>8)return false;s.sequence=request.sequence();
        final String target=content!=null&&s.application<4&&request.action()==Protocol.WorldAction.TRAVEL?com.projetok.studio.StudioExport.key("portal_",(request.target().equals("to_village")?"forest/":"village/")+request.target()):request.target();
        s.world.setSyntheticBodies(bodies(id));boolean accepted;
        try{accepted=switch(request.action()){
            case RESPAWN->{float x=s.home.x(),y=s.home.y();boolean occupied=sessions.entrySet().stream().anyMatch(e->e.getKey()!=id&&!e.getValue().world.skillIntangible()&&region(e.getKey()).equals("forest")&&e.getValue().world.networkState().hp()>0&&playerBlocks(e.getValue().proxy.getPosition(),x,y));occupied|=encounter.enemies(tick).stream().anyMatch(e->e.hp()>0&&Math.hypot(e.x()-x,e.y()-y)<playerRadius()+19);yield !occupied&&s.world.returnHome();}case INTERACT->s.world.interactSyntheticNpc();
            case ACCEPT_QUEST->s.world.acceptAuthoredQuest(target);case COMPLETE_QUEST->s.world.completeAuthoredQuest(target);
            case TRAVEL->{var portal=s.world.networkPortals().stream().filter(p->p.id().equals(target)).findFirst().orElse(null);
                if(portal==null)yield false;boolean blocked=sessions.entrySet().stream().anyMatch(e->e.getKey()!=id&&!e.getValue().world.skillIntangible()&&region(e.getKey()).equals(portal.destination())&&playerBlocks(e.getValue().proxy.getPosition(),portal.arrivalX(),portal.arrivalY()));
                if(portal.destination().equals("forest"))blocked|=encounter.enemies(tick).stream().anyMatch(e->e.hp()>0&&Math.hypot(e.x()-portal.arrivalX(),e.y()-portal.arrivalY())<playerRadius()+19);
                yield !blocked&&s.world.travel(target);}
        };}catch(IllegalArgumentException rejected){return false;}
        if(accepted){s.x=s.y=0;if(request.action()==Protocol.WorldAction.TRAVEL||request.action()==Protocol.WorldAction.RESPAWN){skills.remove(id);s.target=0;s.allyTarget=0;s.targetGeneration=0;}var v=s.world.networkState();s.proxy.setPosition(new Position(v.x(),v.y()));}return accepted;
    }
    public boolean accept(int id,Protocol.Intent intent){
        var session=sessions.get(id);if(frozen||session==null||intent.sequence()<=session.sequence)return false;
        if(session.commandTick!=tick){session.commandTick=tick;session.commands=0;}if(++session.commands>8)return false;
        session.sequence=intent.sequence();
        if(encounter!=null&&intent.action()!=Protocol.Action.MOVE){
            if(intent.x()!=0||intent.y()!=0)return false;
            session.world.setSyntheticBodies(bodies(id));
            validateTarget(session);
            if(session.allyTarget!=0&&(intent.action()==Protocol.Action.ATTACK||intent.action()==Protocol.Action.LIGHTNING_STEP))return false;
            // Toda entrada de ataque na aplicação 7 consome os mesmos cinco tiros do sentry.
            if(session.application==Protocol.SKILL_APPLICATION&&intent.action()==Protocol.Action.ATTACK
                    &&(skills.state(id).sentryShots()>0||session.world.skillBowUser()))
                return targetAction(id,session,GameplayProtocol.Operation.BASIC_ATTACK,
                        session.target==0?"none":"enemy_"+(session.target+1_000_000));
            boolean targeted=Protocol.supportsGameplayApplication(session.application);
            boolean accepted=switch(intent.action()){case ATTACK->targeted&&session.target!=0?session.world.attackTarget(session.target,session.targetGeneration):session.world.attack();case LIGHTNING_STEP->targeted?(session.target==0?session.world.castForwardAbility():session.world.castAbilityTarget(session.target,session.targetGeneration)):session.world.castAbility();case CRYOSTASIS->session.world.castSecondaryAbility();default->false;};
            if(targeted&&(session.target!=0||accepted)){var facing=session.world.combatFacing();session.fx=facing.x();session.fy=facing.y();}
            if(accepted){var view=session.world.networkState();session.proxy.setPosition(new Position(view.x(),view.y()));}
            return accepted;
        }
        if(intent.action()!=Protocol.Action.MOVE||Math.abs(intent.x())>1||Math.abs(intent.y())>1)return false;
        session.x=intent.x();session.y=intent.y();session.inputTick=tick;
        // A aplicação de skills usa as quatro direções disponíveis no atlas do personagem.
        if(session.application==Protocol.SKILL_APPLICATION&&session.x!=0&&session.y!=0){
            if(Math.abs(session.x)>Math.abs(session.y))session.y=0;else session.x=0;
        }
        if(session.x!=0||session.y!=0){double length=Math.hypot(session.x,session.y);session.fx=(float)(session.x/length);session.fy=(float)(session.y/length);if(Protocol.supportsGameplayApplication(session.application))session.world.faceSyntheticDirection(session.fx,session.fy);}return true;
    }
    public void step(){
        if(revisions!=null)revisions.step();if(frozen)return;
        tick++;
        for(var entry:sessions.entrySet()){
            var s=entry.getValue();s.world.setSyntheticBodies(bodies(entry.getKey()));
            s.world.tick(tick-s.inputTick<=15?s.x:0,tick-s.inputTick<=15?s.y:0);var view=s.world.networkState();s.proxy.setPosition(new Position(view.x(),view.y()));
        }
        if(encounter!=null)encounter.step(tick);
        if(sessions.values().stream().anyMatch(s->s.application==Protocol.SKILL_APPLICATION)){
            skills.clock(tick);skills.tick(skillPlayers());
            for(var s:sessions.values())if(s.application==Protocol.SKILL_APPLICATION){
                var view=s.world.networkState();s.proxy.setPosition(new Position(view.x(),view.y()));
                var facing=s.world.combatFacing();s.fx=facing.x();s.fy=facing.y();
            }
        }
        sessions.values().forEach(this::validateTarget);
    }
    /** Derrotados continuam selecionáveis para revive; sessão/região inválida limpa o aliado. */
    private void validateTarget(Session session){
        if(session.world.networkState().hp()<=0){session.target=0;session.allyTarget=0;session.targetGeneration=0;return;}
        if(session.allyTarget!=0){var ally=sessions.get(session.allyTarget);
            if(ally==null||!ally.world.networkState().region().equals(session.world.networkState().region()))session.allyTarget=0;
        }
        if(session.target==0)return;
        var target=session.world.combatTarget(session.target);
        if(target==null||target.generation()!=session.targetGeneration){session.target=0;session.targetGeneration=0;}
    }
    /** Mesmo bloco de apoio usado na caminhada, admissao e retorno de portais. */
    private boolean playerBlocks(Position position,float x,float y){return Math.abs((double)position.x()-x)<2*playerRadius()&&Math.abs((double)position.y()-y)<2*playerRadius();}
    private List<CollisionService.Body> bodies(int id){
        var result=new ArrayList<CollisionService.Body>();
        sessions.forEach((other,s)->{if(other!=id&&region(other).equals(region(id))&&s.world.networkState().hp()>0&&!s.world.skillIntangible())result.add(CollisionService.Body.block(s.proxy,playerRadius()));});
        if(encounter!=null)for(var e:encounter.enemies(region(id),tick))if(e.hp()>0)result.add(new CollisionService.Body(new GameEntity(e.id(),e.name(),new Position(e.x(),e.y())){},19));
        return result;
    }
    /** Metadado da raça atribuído pelo servidor; não representa autenticação ou domínio escolhido. */
    public String race(int id){var s=sessions.get(id);if(s==null)throw new IllegalArgumentException("Sessão ausente");return s.world.networkState().identity().race();}
    public Protocol.State snapshot(){var actors=new ArrayList<Protocol.Actor>();sessions.forEach((id,s)->{var v=s.world.networkState();actors.add(new Protocol.Actor(id,v.identity().name(),v.x(),v.y(),v.hp(),v.maxHp(),s.world.normalizedEnergy(),v.coins(),false,s.fx,s.fy,s.world.attackRemaining()>0?1:0));});return new Protocol.State(tick,actors);}
    public UUID sessionIdentity(int id){return sessions.get(id).world.networkState().identity().id();}
    public Protocol.State snapshotFor(int id){if(!sessions.containsKey(id))return new Protocol.State(tick,List.of());return new Protocol.State(tick,snapshot().actors().stream().filter(a->region(a.id()).equals(region(id))).toList());}
    public List<Protocol.Appearance> appearancesFor(int id){if(!sessions.containsKey(id))return List.of();return appearances().stream().filter(a->region(a.id()).equals(region(id))).toList();}
    public Protocol.CombatState combatSnapshotFor(int id){if(!sessions.containsKey(id))return new Protocol.CombatState(new Protocol.SessionState(new Protocol.State(tick,List.of()),List.of()),List.of());var all=combatSnapshot();var actors=new ArrayList<>(snapshotFor(id).actors());if(content==null){if(region(id).equals("forest"))all.session().world().actors().stream().filter(Protocol.Actor::boss).forEach(actors::add);else actors.add(new Protocol.Actor(2_000_001,"Ancia",190,320,1,1,0,0,true));}else{for(var enemy:encounter.enemies(region(id),tick))actors.add(new Protocol.Actor(1_000_000+enemy.id(),enemy.name(),enemy.x(),enemy.y(),enemy.hp(),enemy.maxHp(),0,0,true,1,0,(enemy.state().equals("PREPARE")||enemy.state().equals("ATTACK"))?1:0));var npcs=sessions.get(id).world.networkNpcs();for(int i=0;i<npcs.size();i++){var npc=npcs.get(i);actors.add(new Protocol.Actor(2_000_001+i,npc.name(),npc.x(),npc.y(),1,1,0,0,true));}}return new Protocol.CombatState(new Protocol.SessionState(new Protocol.State(tick,actors),appearancesFor(id)),region(id).equals("forest")?all.events():List.of(),region(id),sessions.get(id).world.authoredQuestViews().stream().map(q->new Protocol.QuestProgress(q.id(),q.state().name(),q.progress(),q.required())).toList());}
    public Protocol.CombatState combatSnapshot(){
        if(encounter==null)throw new IllegalStateException("Combate indisponivel");
        var actors=new ArrayList<>(snapshot().actors());
        for(var e:encounter.enemies(tick))actors.add(new Protocol.Actor(1_000_000+e.id(),e.name(),e.x(),e.y(),e.hp(),e.maxHp(),0,0,true));
        var events=encounter.events().stream().map(e->new Protocol.CombatEvent(e.sequence(),e.tick(),e.kind(),e.player(),1_000_000+e.enemy(),e.generation(),e.amount())).toList();
        return new Protocol.CombatState(new Protocol.SessionState(new Protocol.State(tick,actors),appearances()),events);
    }
}
