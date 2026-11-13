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
    private final int minimumEnemyHp;
    private long sequence,currentTick;
    public SharedRpgEncounter(CollisionService.Space space,RewardPolicy policy) {
        this(Map.of("forest",space),Map.of("coop_slime","slime|forest|220|300"),policy);
    }
    public SharedRpgEncounter(Map<String,CollisionService.Space> regions,Map<String,String> definitions,RewardPolicy policy){
        this(regions,definitions,policy,1);
    }
    /** Configura a resistência dos mobs deste encontro efêmero sem alterar o conteúdo compartilhado. */
    public SharedRpgEncounter(Map<String,CollisionService.Space> regions,Map<String,String> definitions,RewardPolicy policy,int minimumEnemyHp){
        this.minimumEnemyHp=minimumEnemyHp;
        this.regions=Map.copyOf(regions);this.definitions=Map.copyOf(definitions);this.space=regions.values().iterator().next();this.policy=Objects.requireNonNull(policy);
        population=new EnemyService(RpgContent.builtin(),space,definitions,r->this.regions.get(r),minimumEnemyHp);
    }
    public SharedRpgEncounter fork(Map<String,CollisionService.Space> next,Map<String,String> enemies){
        var copy=new SharedRpgEncounter(next,enemies,policy,minimumEnemyHp);
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
            players.forEach((w,player)->{
                var state=w.networkState();
                if(!contributors.contains(player)||state.hp()<=0||!state.region().equals(enemy.region))return;
                w.sharedKill(enemy);
                if(policy==RewardPolicy.DISABLED)emit(tick,"REWARD_REFUSED",player,enemy,0);
                else if(w.sharedReward(enemy))emit(tick,"SYNTHETIC_REWARD",player,enemy,3);
            });
            participants.remove(key);
        }
    }
    private void emit(long tick,String kind,int id,EnemyService.Enemy enemy,int amount){events.addLast(new Event(++sequence,tick,kind,id,enemy.getId(),enemy.generation,amount));while(events.size()>16)events.removeFirst();}
    public List<Event> events(){return List.copyOf(events);}
    public List<EnemyService.View> enemies(long tick){return population.views("forest",tick);}
    public List<EnemyService.View> enemies(String region,long tick){return population.views(region,tick);}
    /** Corpos circulares atuais usados pela IA dos inimigos, sem o bloco reservado à caminhada dos jogadores. */
    private List<CollisionService.Body> enemyPlayerBodies(String region){
        var bodies=new ArrayList<CollisionService.Body>();
        players.forEach((world,id)->{
            var state=world.networkState();
            if(state.hp()>0&&state.region().equals(region)&&!world.skillInDash()&&!world.skillIntangible())bodies.add(new CollisionService.Body(
                    new com.projetok.domain.entity.GameEntity(id,"Sintetico",new Position(state.x(),state.y())){},world.physicalRadius()));
        });
        return bodies;
    }
    /**
     * Deslocamentos de skills mantêm o cenário bruto e os contatos circulares próprios dos inimigos.
     * Os outros mobs são consultados na posição atual para que puxões sucessivos não os sobreponham.
     */
    CollisionService.Space skillDisplacementSpace(EnemyService.Enemy moving){
        var raw=regions.get(moving.region);
        if(raw==null)throw new IllegalArgumentException("Região de inimigo ausente do encontro");
        var bodies=enemyPlayerBodies(moving.region);
        for(var other:population.current(moving.region))if(other!=moving&&other.hp>0)bodies.add(new CollisionService.Body(other,19));
        return new CollisionService.Space(raw.width(),raw.height(),raw.obstacles(),bodies);
    }
    /** Apoios atuais usados ao sair de uma fase sem colisão; ignora outros participantes intangíveis. */
    CollisionService.Space playerReentrySpace(RpgWorld moving){
        var region=moving.networkState().region();var bodies=new ArrayList<CollisionService.Body>();
        players.forEach((world,id)->{var state=world.networkState();if(world!=moving&&state.hp()>0&&state.region().equals(region)&&!world.skillIntangible())
            bodies.add(CollisionService.Body.block(new com.projetok.domain.entity.GameEntity(id,"Participante",world.skillPosition()){},world.physicalRadius()));});
        for(var enemy:population.current(region))if(enemy.hp>0)bodies.add(new CollisionService.Body(enemy,19));
        var geometry=moving.skillSpace();return new CollisionService.Space(geometry.width(),geometry.height(),geometry.obstacles(),bodies);
    }
    public void step(long tick){
        currentTick=tick;
        for(var region:regions.keySet())stepRegion(tick,region,regions.get(region));
    }
    private void stepRegion(long tick,String region,CollisionService.Space space){
        var bodies=enemyPlayerBodies(region);
        var dynamic=new CollisionService.Space(space.width(),space.height(),space.obstacles(),bodies);
        population.respawn(tick,region,dynamic);
        for(var enemy:population.current(region)) {
            var target=players.keySet().stream().filter(w->w.networkState().hp()>0&&!w.skillIntangible()&&w.networkState().region().equals(region))
                .min(Comparator.<RpgWorld>comparingDouble(w->{var v=w.networkState();return Math.hypot(v.x()-enemy.getPosition().x(),v.y()-enemy.getPosition().y());})
                    .thenComparingInt(players::get)).orElse(null);
            var targetState=target==null?null:target.networkState();
            Position p=targetState==null?enemy.home:new Position(targetState.x(),targetState.y());
            population.updateSelected(region,tick,p,target!=null,new MovementService(new CollisionService()),dynamic,
                (n,basic)->{if(target!=null){int damage=Math.max(1,n-target.defense());if(basic)target.receiveBasicDamage(damage);else target.receiveDamage(damage);}},q->RpgSight.clear(p,q,space),false,e->e==enemy);
        }
    }
}
