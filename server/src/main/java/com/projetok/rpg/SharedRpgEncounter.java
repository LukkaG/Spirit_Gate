package com.projetok.rpg;

import com.projetok.domain.entity.Position;
import com.projetok.domain.movement.*;
import java.util.*;

/** Encontro efêmero, confinado à thread do mundo. Nenhuma escrita em saves. */
public final class SharedRpgEncounter {
    public enum RewardPolicy { DISABLED, SYNTHETIC_PARTICIPANTS }
    public record Event(long sequence,long tick,String kind,int player,int enemy,long generation,int amount) { }
    private final EnemyService population;
    private final CollisionService.Space space;
    private final Map<String,CollisionService.Space> regions;
    private final Map<String,String> definitions;
    private final Map<RpgWorld,Integer> players=new LinkedHashMap<>();
    private final Map<String,Set<Integer>> participants=new HashMap<>();
    private final ArrayDeque<Event> events=new ArrayDeque<>();
    private final RewardPolicy policy;
    private long sequence,currentTick;
    public SharedRpgEncounter(CollisionService.Space space,RewardPolicy policy) {
        this(Map.of("forest",space),Map.of("coop_slime","slime|forest|220|300"),policy);
    }
    public SharedRpgEncounter(Map<String,CollisionService.Space> regions,Map<String,String> definitions,RewardPolicy policy){
        this.regions=Map.copyOf(regions);this.definitions=Map.copyOf(definitions);this.space=regions.values().iterator().next();this.policy=Objects.requireNonNull(policy);
        population=new EnemyService(RpgContent.builtin(),space,definitions,r->this.regions.get(r));
    }
    public SharedRpgEncounter fork(Map<String,CollisionService.Space> next,Map<String,String> enemies){
        var copy=new SharedRpgEncounter(next,enemies,policy);
        for(String region:next.keySet()){
            var flags=population.saveFlags().stream().filter(f->{String id=f.split(":")[1];return definitions.containsKey(id)&&Objects.equals(definitions.get(id),enemies.get(id))&&enemies.get(id).split("\\|")[1].equals(region);}).collect(java.util.stream.Collectors.toSet());
            copy.population.restore(flags,next.get(region));
        }
        copy.currentTick=currentTick;copy.sequence=sequence;copy.events.addAll(events);
        participants.forEach((id,ids)->copy.participants.put(id,new TreeSet<>(ids)));
        return copy;
    }
    public void join(int id,RpgWorld world){if(players.size()>=2||players.containsValue(id))throw new IllegalArgumentException("Participante invalido");world.attachEncounter(this,population);players.put(world,id);}
    public void leave(RpgWorld world){players.remove(world);}
    void hit(RpgWorld world,EnemyService.Enemy enemy,int damage,long ignoredLocalTick) {
        long tick=currentTick;
        Integer id=players.get(world);if(id==null||damage<=0)return;
        String key=enemy.spawn+":"+enemy.generation;
        var contributors=participants.computeIfAbsent(key,k->new TreeSet<>());contributors.add(id);
        emit(tick,"HIT",id,enemy,damage);
        if(enemy.hp==0){
            enemy.readyTick=tick+enemy.definition.respawn();emit(tick,"DEATH",id,enemy,0);
            for(int contributor:contributors)emit(tick,policy==RewardPolicy.DISABLED?"REWARD_REFUSED":"SYNTHETIC_REWARD",contributor,enemy,policy==RewardPolicy.DISABLED?0:3);
            players.forEach((w,player)->{if(contributors.contains(player))w.sharedKill(enemy);});
            participants.remove(key);
        }
    }
    private void emit(long tick,String kind,int id,EnemyService.Enemy enemy,int amount){events.addLast(new Event(++sequence,tick,kind,id,enemy.getId(),enemy.generation,amount));while(events.size()>16)events.removeFirst();}
    public List<Event> events(){return List.copyOf(events);}
    public List<EnemyService.View> enemies(long tick){return population.views("forest",tick);}
    public List<EnemyService.View> enemies(String region,long tick){return population.views(region,tick);}
    public void step(long tick){
        currentTick=tick;
        for(var region:regions.keySet())stepRegion(tick,region,regions.get(region));
    }
    private void stepRegion(long tick,String region,CollisionService.Space space){
        var bodies=players.keySet().stream().filter(w->w.snapshot().state().region().equals(region)&&w.snapshot().state().hp()>0).map(w->{var v=w.snapshot();return new CollisionService.Body(new com.projetok.domain.entity.GameEntity(players.get(w),"Sintetico",new Position(v.x(),v.y())){},w.physicalRadius());}).toList();
        var dynamic=new CollisionService.Space(space.width(),space.height(),space.obstacles(),bodies);
        population.respawn(tick,region,dynamic);
        // A região demonstrativa contém um único inimigo; escolhe alvo estável por distância/ordem de entrada.
        var visible=population.views(region);if(visible.isEmpty())return;var enemy=visible.get(0);
        var target=players.keySet().stream().filter(w->w.snapshot().state().hp()>0&&w.snapshot().state().region().equals(region)).min(Comparator.comparingDouble(w->{var v=w.snapshot();return Math.hypot(v.x()-enemy.x(),v.y()-enemy.y());})).orElse(null);
        Position p=target==null?new Position(220,300):new Position(target.snapshot().x(),target.snapshot().y());
        population.update(region,tick,p,target!=null,new MovementService(new CollisionService()),dynamic,
            n->{if(target!=null)target.receiveDamage(Math.max(1,n-target.defense()));},q->RpgSight.clear(p,q,space),false);
    }
}
