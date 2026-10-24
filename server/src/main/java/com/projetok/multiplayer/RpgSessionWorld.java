package com.projetok.multiplayer;

import com.projetok.rpg.*;
import com.projetok.domain.entity.*;
import com.projetok.domain.movement.CollisionService;
import java.util.*;
import com.projetok.prototype.rpg.CosmeticCatalog;

/** Região compartilhada experimental; movimento usa RpgWorld no servidor, sem campanha pessoal. */
public final class RpgSessionWorld implements SessionSimulation {
    private final List<String> races;
    private final CosmeticCatalog cosmetics;
    private final Map<Integer,Session> sessions=new TreeMap<>();
    private final CollisionService.Space geometry=new CollisionService.Space(960,640,List.of(new CollisionService.Obstacle(380,180,120,120)),List.of());
    private long tick;
    private final UUID instance=UUID.randomUUID();
    private final SharedRpgEncounter encounter;
    private final int syntheticLevel;
    private static final class Session {
        final RpgWorld world;final GameEntity proxy;final int seat;final Position home;
        long sequence,inputTick,commandTick=-1;int commands;float x,y,fx=1,fy;
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
    public boolean supportsCombat(){return encounter!=null;}
    public boolean supportsCosmetics(){return cosmetics!=null;}
    public boolean appearance(int id,Protocol.AppearanceRequest request){var s=sessions.get(id);if(cosmetics==null||s==null||request.sequence()<=s.sequence)return false;if(s.commandTick!=tick){s.commandTick=tick;s.commands=0;}if(++s.commands>8)return false;var selection=new CosmeticCatalog.Selection(request.hair(),request.clothing());try{cosmetics.validate(selection);}catch(IllegalArgumentException e){return false;}s.sequence=request.sequence();s.appearance=selection;return true;}
    public List<Protocol.Appearance> appearances(){return sessions.entrySet().stream().map(e->new Protocol.Appearance(e.getKey(),race(e.getKey()),e.getValue().appearance.hair(),e.getValue().appearance.clothing())).toList();}
    public boolean join(int id,String name){
        new Protocol.Hello(1,name);if(id<1||id>1_000_000||sessions.containsKey(id)||sessions.size()>=2)return false;
        int seat=sessions.values().stream().anyMatch(s->s.seat==0)?1:0;var race=CharacterRaces.get(races.get(seat));var spawn=new Position(140,seat==0?300:410);
        String region=encounter==null?"village":"forest";
        var spawnBodies=new ArrayList<CollisionService.Body>();
        for(var entry:sessions.entrySet())if(region(entry.getKey()).equals(region)&&entry.getValue().world.snapshot().state().hp()>0)spawnBodies.add(new CollisionService.Body(entry.getValue().proxy,18));
        if(encounter!=null)for(var enemy:encounter.enemies(tick))if(enemy.hp()>0)spawnBodies.add(new CollisionService.Body(new GameEntity(enemy.id(),enemy.name(),new Position(enemy.x(),enemy.y())){},19));
        var spawnSpace=new CollisionService.Space(960,640,geometry.obstacles(),spawnBodies);var probe=new GameEntity(id,name,spawn){};
        Position safe=null;for(float offset:new float[]{0,40,-40,80,-80})if(new CollisionService().isFree(probe,spawn.x()+offset,spawn.y(),18,spawnSpace)){safe=new Position(spawn.x()+offset,spawn.y());break;}
        if(safe==null)return false;spawn=safe;
        var maps=encounter==null?Map.of(region,new RpgSceneSet.Geometry(geometry,spawn)):Map.of("forest",new RpgSceneSet.Geometry(geometry,spawn),"village",new RpgSceneSet.Geometry(geometry,new Position(140,320)));
        var scenes=new RpgSceneSet(region,maps,Map.of(),encounter==null?Map.of():Map.of("elder","village|190|320"),Map.of(),encounter==null?Map.of():Map.of("to_village","forest|140|300|village|140|320","to_forest","village|140|320|forest|140|300"));
        var identity=new CharacterIdentity(UUID.nameUUIDFromBytes((instance+":"+id).getBytes(java.nio.charset.StandardCharsets.UTF_8)),name,race.id(),"azure","village",race.hp(),race.attack());var world=encounter==null?new RpgWorld(identity,scenes):new RpgWorld(identity,scenes,questBook());var proxy=new GameEntity(id,name,spawn){};
        if(encounter!=null){encounter.join(id,world);world.awardExperience(RpgProgression.builtin().threshold(syntheticLevel));}sessions.put(id,new Session(world,proxy,seat));return true;
    }
    public void leave(int id){var s=sessions.remove(id);if(s!=null&&encounter!=null)encounter.leave(s.world);}
    private static QuestBook questBook(){try(var in=RpgSessionWorld.class.getResourceAsStream("/rpg/corujao-v.sgquest")){if(in==null)throw new IllegalStateException("Missao ausente");return QuestDocuments.decode(in.readAllBytes());}catch(java.io.IOException e){throw new IllegalStateException(e);}}
    public String region(int id){var s=sessions.get(id);if(s==null)throw new IllegalArgumentException("Sessao ausente");return s.world.snapshot().state().region();}
    public boolean request(int id,Protocol.WorldRequest request){
        var s=sessions.get(id);if(encounter==null||s==null||request.sequence()<=s.sequence)return false;
        if(s.commandTick!=tick){s.commandTick=tick;s.commands=0;}if(++s.commands>8)return false;s.sequence=request.sequence();
        s.world.setSyntheticBodies(bodies(id));boolean accepted;
        try{accepted=switch(request.action()){
            case RESPAWN->{float x=s.home.x(),y=s.home.y();boolean occupied=sessions.entrySet().stream().anyMatch(e->e.getKey()!=id&&region(e.getKey()).equals("forest")&&e.getValue().world.snapshot().state().hp()>0&&Math.hypot(e.getValue().proxy.getPosition().x()-x,e.getValue().proxy.getPosition().y()-y)<36);occupied|=encounter.enemies(tick).stream().anyMatch(e->e.hp()>0&&Math.hypot(e.x()-x,e.y()-y)<37);yield !occupied&&s.world.returnHome();}case INTERACT->s.world.interactSyntheticNpc();
            case ACCEPT_QUEST->s.world.acceptAuthoredQuest(request.target());case COMPLETE_QUEST->s.world.completeAuthoredQuest(request.target());
            case TRAVEL->{var before=s.world.snapshot();var portal=before.portals().stream().filter(p->p.id().equals(request.target())).findFirst().orElse(null);
                if(portal==null)yield false;boolean blocked=sessions.entrySet().stream().anyMatch(e->e.getKey()!=id&&region(e.getKey()).equals(portal.destination())&&Math.hypot(e.getValue().proxy.getPosition().x()-portal.arrivalX(),e.getValue().proxy.getPosition().y()-portal.arrivalY())<36);
                if(portal.destination().equals("forest"))blocked|=encounter.enemies(tick).stream().anyMatch(e->e.hp()>0&&Math.hypot(e.x()-portal.arrivalX(),e.y()-portal.arrivalY())<37);
                yield !blocked&&s.world.travel(request.target());}
        };}catch(IllegalArgumentException rejected){return false;}
        if(accepted){s.x=s.y=0;var v=s.world.snapshot();s.proxy.setPosition(new Position(v.x(),v.y()));}return accepted;
    }
    public boolean accept(int id,Protocol.Intent intent){
        var session=sessions.get(id);if(session==null||intent.sequence()<=session.sequence)return false;
        if(session.commandTick!=tick){session.commandTick=tick;session.commands=0;}if(++session.commands>8)return false;
        session.sequence=intent.sequence();
        if(encounter!=null&&intent.action()!=Protocol.Action.MOVE){
            if(intent.x()!=0||intent.y()!=0)return false;
            session.world.setSyntheticBodies(bodies(id));
            return switch(intent.action()){case ATTACK->session.world.attack();case LIGHTNING_STEP->session.world.castAbility();case CRYOSTASIS->session.world.castSecondaryAbility();default->false;};
        }
        if(intent.action()!=Protocol.Action.MOVE||Math.abs(intent.x())>1||Math.abs(intent.y())>1)return false;
        session.x=intent.x();session.y=intent.y();session.inputTick=tick;
        if(session.x!=0||session.y!=0){double length=Math.hypot(session.x,session.y);session.fx=(float)(session.x/length);session.fy=(float)(session.y/length);}return true;
    }
    public void step(){
        tick++;
        for(var entry:sessions.entrySet()){
            var s=entry.getValue();s.world.setSyntheticBodies(bodies(entry.getKey()));
            s.world.tick(tick-s.inputTick<=15?s.x:0,tick-s.inputTick<=15?s.y:0);var view=s.world.snapshot();s.proxy.setPosition(new Position(view.x(),view.y()));
        }
        if(encounter!=null)encounter.step(tick);
    }
    private List<CollisionService.Body> bodies(int id){
        var result=new ArrayList<CollisionService.Body>();
        sessions.forEach((other,s)->{if(other!=id&&region(other).equals(region(id))&&s.world.snapshot().state().hp()>0)result.add(new CollisionService.Body(s.proxy,18));});
        if(encounter!=null&&region(id).equals("forest"))for(var e:encounter.enemies(tick))if(e.hp()>0)result.add(new CollisionService.Body(new GameEntity(e.id(),e.name(),new Position(e.x(),e.y())){},19));
        return result;
    }
    /** Metadado da raça atribuído pelo servidor; não representa autenticação ou domínio escolhido. */
    public String race(int id){var s=sessions.get(id);if(s==null)throw new IllegalArgumentException("Sessão ausente");return s.world.snapshot().identity().race();}
    public Protocol.State snapshot(){var actors=new ArrayList<Protocol.Actor>();sessions.forEach((id,s)->{var v=s.world.snapshot();int stamina=v.state().flags().stream().filter(f->f.startsWith("stamina:")).mapToInt(f->Integer.parseInt(f.substring(8))).findFirst().orElse(0);actors.add(new Protocol.Actor(id,v.identity().name(),v.x(),v.y(),v.state().hp(),v.maxHp(),stamina,v.state().coins(),false,s.fx,s.fy,0));});return new Protocol.State(tick,actors);}
    public UUID sessionIdentity(int id){return sessions.get(id).world.snapshot().identity().id();}
    public Protocol.State snapshotFor(int id){if(!sessions.containsKey(id))return new Protocol.State(tick,List.of());return new Protocol.State(tick,snapshot().actors().stream().filter(a->region(a.id()).equals(region(id))).toList());}
    public List<Protocol.Appearance> appearancesFor(int id){if(!sessions.containsKey(id))return List.of();return appearances().stream().filter(a->region(a.id()).equals(region(id))).toList();}
    public Protocol.CombatState combatSnapshotFor(int id){if(!sessions.containsKey(id))return new Protocol.CombatState(new Protocol.SessionState(new Protocol.State(tick,List.of()),List.of()),List.of());var all=combatSnapshot();var actors=new ArrayList<>(snapshotFor(id).actors());if(region(id).equals("forest"))all.session().world().actors().stream().filter(Protocol.Actor::boss).forEach(actors::add);else actors.add(new Protocol.Actor(2_000_001,"Ancia",190,320,1,1,0,0,true));return new Protocol.CombatState(new Protocol.SessionState(new Protocol.State(tick,actors),appearancesFor(id)),region(id).equals("forest")?all.events():List.of(),region(id),sessions.get(id).world.authoredQuestViews().stream().map(q->new Protocol.QuestProgress(q.id(),q.state().name(),q.progress(),q.required())).toList());}
    public Protocol.CombatState combatSnapshot(){
        if(encounter==null)throw new IllegalStateException("Combate indisponivel");
        var actors=new ArrayList<>(snapshot().actors());
        for(var e:encounter.enemies(tick))actors.add(new Protocol.Actor(1_000_000+e.id(),e.name(),e.x(),e.y(),e.hp(),e.maxHp(),0,0,true));
        var events=encounter.events().stream().map(e->new Protocol.CombatEvent(e.sequence(),e.tick(),e.kind(),e.player(),1_000_000+e.enemy(),e.generation(),e.amount())).toList();
        return new Protocol.CombatState(new Protocol.SessionState(new Protocol.State(tick,actors),appearances()),events);
    }
}
