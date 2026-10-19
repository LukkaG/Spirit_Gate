package com.projetok.multiplayer;
import com.projetok.domain.entity.*;
import com.projetok.domain.entity.component.*;
import com.projetok.domain.factory.*;
import com.projetok.domain.progression.VitalityService;
import com.projetok.domain.race.RaceRegistry;
import com.projetok.domain.network.PlayerConnection;
import com.projetok.domain.movement.*;
import com.projetok.domain.skill.*;
import com.projetok.domain.resource.StaminaService;
import com.projetok.domain.combat.CombatService;
import com.projetok.domain.event.GameEventBus;
import com.projetok.domain.effect.StatusEffectService;
import com.projetok.domain.effect.StatusEffectType;
import com.projetok.domain.probability.ProbabilityService;
import com.projetok.domain.item.LootTable;
import com.projetok.domain.ai.PassiveBehavior;
import java.util.*;
/** Simulacao experimental transitoria. Nunca importa ou escreve saves offline. */
public final class CoopWorld {
    public static final int BOSS_ID=900_000_001;
    final class Session {
        final Player player;long sequence,inputTick,commandTick=-1,attackReady;int commands,coins,damage;float dx,dy,fx=1,fy;
        final LightningStepService dash;final CryostasisService cryo;
        Session(Player player){this.player=player;dash=new LightningStepService(movement,stamina,collisions,CollisionPolicy.PHASE_THROUGH_ACTORS_AND_SOFT);cryo=new CryostasisService(combat,stamina,status,new ProbabilityService(new Random(17L+player.getId())));}
    }
    private final Map<Integer,Session> sessions=new TreeMap<>();
    private final CollisionService collisions=new CollisionService();
    private final MovementService movement=new MovementService(collisions);
    private final StaminaService stamina=new StaminaService();
    private final StatusEffectService status=new StatusEffectService();
    private final CombatService combat=new CombatService(new GameEventBus());
    private final List<CollisionService.Obstacle> obstacles=List.of(new CollisionService.Obstacle(380,180,120,120));
    private final Mob boss=new MobFactory(new VitalityService()).spawn(BOSS_ID,new MobArchetype(RaceRegistry.withDefaults().get("demon"),"Guardiao Cooperativo",14,8,4,8,null,LootTable.of(),new PassiveBehavior()),new Position(720,320));
    private long tick;
    private long bossAttackReady;private boolean rewarded;
    public boolean join(int id,String name){
        new Protocol.Hello(1,name);if(id<1||id>1_000_000||sessions.containsKey(id)||sessions.size()>=2)return false;
        Position spawn=new Position(140,sessions.values().stream().anyMatch(s->s.player.getPosition().y()<350)?410:300);
        var player=new PlayerFactory(new VitalityService()).create(id,name,new PlayerArchetype(RaceRegistry.withDefaults().get("human"),30,8,14,null,null),spawn,new PlayerConnection(){public void send(Object ignored){}public void disconnect(){}});
        if(!collisions.isFree(player,spawn.x(),spawn.y(),18,space()))return false;sessions.put(id,new Session(player));return true;
    }
    public void leave(int id){sessions.remove(id);}
    public boolean accept(int id,Protocol.Intent intent){
        Session s=sessions.get(id);if(s==null||intent.sequence()<=s.sequence||s.player.isDead())return false;
        if(intent.action()==Protocol.Action.MOVE&&(Math.abs(intent.x())>1||Math.abs(intent.y())>1))return false;
        if(s.commandTick!=tick){s.commandTick=tick;s.commands=0;}if(++s.commands>8)return false;
        s.sequence=intent.sequence();
        return switch(intent.action()){
            case MOVE->{s.dx=intent.x();s.dy=intent.y();s.inputTick=tick;if(s.dx!=0||s.dy!=0){var d=movement.direction(s.dx,s.dy);s.fx=d.x();s.fy=d.y();}yield true;}
            case ATTACK->{if(boss.isDead()||s.dash.active()||s.dash.awaitingLanding()||tick<s.attackReady||distance(s.player.getPosition(),boss.getPosition())>82||!lineClear(s.player))yield false;s.attackReady=tick+12;s.damage+=combat.attack(s.player,boss);reward();yield true;}
            case LIGHTNING_STEP->s.dash.cast(s.player,new SkillCastCommand(LightningStepService.ID,intent.x(),intent.y()),18,space())==LightningStepService.Status.STARTED;
            case CRYOSTASIS->{if(s.dash.active()||s.dash.awaitingLanding())yield false;var result=s.cryo.cast(s.player,new SkillCastCommand(CryostasisService.ID,intent.x(),intent.y()),960,640,targets());s.damage+=result.hits().stream().mapToInt(CryostasisService.Hit::damage).sum();reward();yield result.status()==CryostasisService.Status.CAST;}
        };
    }
    private List<CryostasisService.Target> targets(){return List.of(new CryostasisService.Target(boss,24));}
    private static double distance(Position a,Position b){return Math.hypot(a.x()-b.x(),a.y()-b.y());}
    private boolean lineClear(Player player){Position a=player.getPosition(),b=boss.getPosition();var geometry=new CollisionService.Space(960,640,obstacles,List.of());int steps=Math.max(1,(int)(distance(a,b)/6));for(int i=1;i<steps;i++)if(!collisions.isFree(player,a.x()+(b.x()-a.x())*i/steps,a.y()+(b.y()-a.y())*i/steps,1,geometry))return false;return true;}
    private CollisionService.Space space(){var bodies=new ArrayList<CollisionService.Body>();sessions.values().forEach(s->bodies.add(new CollisionService.Body(s.player,18)));bodies.add(new CollisionService.Body(boss,24));return new CollisionService.Space(960,640,obstacles,bodies);}
    public void step(){
        tick++;var space=space();status.update(boss,1f/30);
        for(Session s:sessions.values()){
            stamina.regenerate(s.player,1f/30);boolean active=s.dash.active()||s.dash.awaitingLanding();s.dash.update(s.player,1f/30,18,space);s.damage+=s.cryo.update(1f/30,targets()).stream().mapToInt(CryostasisService.Hit::damage).sum();
            if(!s.player.isDead()&&!active&&tick-s.inputTick<=15){var d=movement.direction(s.dx,s.dy);movement.moveContinuous(s.player,d.x()*215/30,d.y()*215/30,18,space);}
        }
        reward();updateBoss(space);
    }
    private void reward(){if(!boss.isDead()||rewarded)return;rewarded=true;for(Session s:sessions.values())if(s.damage>0)s.coins=Math.min(1_000_000,s.coins+20);}
    private void updateBoss(CollisionService.Space space){
        if(boss.isDead()||boss.require(StatusEffectsComponent.class).has(StatusEffectType.FREEZE))return;
        var target=sessions.values().stream().filter(s->!s.player.isDead()).min(Comparator.comparingDouble(s->distance(s.player.getPosition(),boss.getPosition()))).orElse(null);
        Position home=new Position(720,320);boolean returning=distance(boss.getPosition(),home)>420;
        Position destination=target==null||returning||distance(target.player.getPosition(),boss.getPosition())>300?home:target.player.getPosition();
        double distance=distance(boss.getPosition(),destination);
        if(destination!=home&&distance<=66&&lineClear(target.player)){if(tick>=bossAttackReady){combat.attack(boss,target.player);bossAttackReady=tick+36;}}
        else if(distance>3){var d=movement.direction(destination.x()-boss.getPosition().x(),destination.y()-boss.getPosition().y());movement.moveContinuous(boss,d.x()*2,d.y()*2,24,space);}
    }
    public Protocol.State snapshot(){var actors=new ArrayList<Protocol.Actor>();for(Session s:sessions.values()){var p=s.player;var hp=p.require(HealthComponent.class);actors.add(new Protocol.Actor(p.getId(),p.getName(),p.getPosition().x(),p.getPosition().y(),hp.current(),hp.max(),p.require(StaminaComponent.class).current(),s.coins,false,s.fx,s.fy,(s.dash.active()?1:0)|(s.dash.awaitingLanding()?2:0)));}var hp=boss.require(HealthComponent.class);actors.add(new Protocol.Actor(BOSS_ID,boss.getName(),boss.getPosition().x(),boss.getPosition().y(),hp.current(),hp.max(),0,0,true,1,0,boss.require(StatusEffectsComponent.class).has(StatusEffectType.FREEZE)?4:0));return new Protocol.State(tick,actors);}
}
