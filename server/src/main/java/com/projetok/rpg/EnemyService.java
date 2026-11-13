package com.projetok.rpg;
import com.projetok.domain.entity.*;
import com.projetok.domain.movement.*;
import java.util.*;

/** Populacao finita definida em conteudo; estado persistente de cada spawn. */
public final class EnemyService {
    public record Definition(String id,int hp,int attack,int speed,int aggro,int leash,int respawn,int level) {
        public Definition { if(level!=1)throw new IllegalArgumentException("Recompensas de mobs acima do nivel 1 ainda nao definidas"); }
        public Definition(String id,int hp,int attack,int speed,int aggro,int leash,int respawn) {this(id,hp,attack,speed,aggro,leash,respawn,1);}
    }
    public static final class Enemy extends GameEntity {
        final String spawn,region; final Definition definition; final Position home;
        int hp; long generation=1,readyTick,attackTick; String behavior="PATROL";
        // Controles efêmeros das técnicas de sessão; não entram no formato de save.
        long skillRootUntil,skillStunUntil,skillSlowUntil;
        float skillSlowScale=1;
        // Rotas efemeras: nao alteram saves nem o contrato de rede.
        final ArrayDeque<Position> detour=new ArrayDeque<>();
        Position detourTarget;long nextRouteTick;int blockedRouteTicks;
        /** Evita transferir efeitos de uma geração derrotada para o próximo respawn. */
        void clearSkillEffects(){detour.clear();detourTarget=null;nextRouteTick=0;blockedRouteTicks=0;skillRootUntil=skillStunUntil=skillSlowUntil=0;skillSlowScale=1;}
        Enemy(int id,String spawn,String region,Definition definition,Position home,String name) {
            super(id,name,home);this.spawn=spawn;this.region=region;this.definition=definition;this.home=home;hp=definition.hp();
        }
    }
    public record View(int id,String spawn,String name,float x,float y,int hp,int maxHp,String state,boolean boss,RpgBossThreat threat) { }
    private final java.util.function.Function<String,CollisionService.Space> geometry;
    private final List<Enemy> enemies=new ArrayList<>();
    public EnemyService(RpgContent content,CollisionService.Space space) {this(content,space,RpgSceneSet.entries("spawns"),r->space);}
    /** Spawns de autoria usam definicoes e IA existentes, validados na geometria da propria regiao. */
    public EnemyService(RpgContent content,CollisionService.Space space,Map<String,String> spawnData,java.util.function.Function<String,CollisionService.Space> geometry) {
        this(content,space,spawnData,geometry,1);
    }
    /** Aplica um piso de vida exclusivo do encontro, preservando o catálogo e os saves dos demais modos. */
    public EnemyService(RpgContent content,CollisionService.Space space,Map<String,String> spawnData,java.util.function.Function<String,CollisionService.Space> geometry,int minimumHp) {
        if(minimumHp<1||minimumHp>10000)throw new IllegalArgumentException("Piso de vida PvE inválido");
        this.geometry=geometry;
        var catalog=com.projetok.domain.mob.MobCatalog.fromProperties(RpgSceneSet.entries("enemies"));
        Properties spawns=new Properties();spawns.putAll(spawnData); Map<String,Definition> types=new TreeMap<>();
        // O catálogo descreve a espécie; o piso de HP continua exclusivo deste encontro.
        for(var definition:catalog.all()) {
            String id=definition.id();content.get("enemy",id);
            types.put(id,new Definition(id,Math.max(definition.hp(),minimumHp),definition.attack(),definition.speed(),definition.aggro(),definition.leash(),definition.respawn(),definition.level()));
        }
        if(spawns.size()>64)throw new IllegalArgumentException("Populacao excessiva"); int entityId=100;
        for(String spawn:new TreeSet<>(spawns.stringPropertyNames())) {
            if(!spawn.matches("[a-z][a-z0-9_]{0,30}"))throw new IllegalArgumentException("Spawn invalido");
            String[] v=spawns.getProperty(spawn).split("\\|");if(v.length!=4)throw new IllegalArgumentException("Spawn malformado");
            var base=content.get("enemy",v[0]);content.get("region",v[1]);Definition definition=types.get(v[0]);
            if(definition==null||!base.region().equals(v[1]))throw new IllegalArgumentException("Spawn em regiao proibida");
            var enemy=new Enemy(entityId++,spawn,v[1],definition,new Position(Float.parseFloat(v[2]),Float.parseFloat(v[3])),base.name());
            if(!new CollisionService().isFree(enemy,enemy.home.x(),enemy.home.y(),19,geometry.apply(v[1])))throw new IllegalArgumentException("Spawn obstruido");
            enemies.add(enemy);
        }
    }
    List<Enemy> current(String region) { return enemies.stream().filter(e->e.region.equals(region)).toList(); }
    private long viewTick;
    public List<View> views(String region) { return views(region,viewTick); }
    /** Publica aviso com o tick do mundo, inclusive imediatamente apos retomar um save. */
    public List<View> views(String region,long tick) { return current(region).stream().map(e->new View(e.getId(),e.spawn,e.getName(),e.getPosition().x(),e.getPosition().y(),e.hp,e.definition.hp(),e.hp==0?"DEAD":e.behavior,e.definition.id().equals("warden"),e.hp>0&&e.definition.id().equals("warden")&&e.behavior.equals("PREPARE")?new RpgBossThreat(e.getPosition().x(),e.getPosition().y(),e.hp*2<=e.definition.hp()?135:110,Math.max(0,e.attackTick-tick),e.hp*2<=e.definition.hp(),geometry.apply(region)):null)).toList(); }
    void respawn(long tick,String region) {
        for(Enemy e:current(region))if(e.hp==0&&e.definition.respawn()>0&&tick>=e.readyTick){e.clearSkillEffects();e.hp=e.definition.hp();e.generation++;e.setPosition(e.home);e.behavior="PATROL";}
    }
    /** Multiplayer respawn waits for an unoccupied home rather than overlapping a player. */
    void respawn(long tick,String region,CollisionService.Space occupied) {
        for(Enemy e:current(region))if(e.hp==0&&e.definition.respawn()>0&&tick>=e.readyTick
                &&new CollisionService().isFree(e,e.home.x(),e.home.y(),19,occupied)){
            e.clearSkillEffects();e.hp=e.definition.hp();e.generation++;e.setPosition(e.home);e.behavior="PATROL";
        }
    }
    void update(String region,long tick,Position player,boolean alive,MovementService movement,CollisionService.Space space,java.util.function.IntConsumer damage,java.util.function.Predicate<Position> visible,boolean bossUnlocked) {
        updateSelected(region,tick,player,alive,movement,space,damage,visible,bossUnlocked,e->true);
    }
    void updateSelected(String region,long tick,Position player,boolean alive,MovementService movement,CollisionService.Space space,java.util.function.IntConsumer damage,java.util.function.Predicate<Position> visible,boolean bossUnlocked,java.util.function.Predicate<Enemy> selected) {
        updateSelected(region,tick,player,alive,movement,space,(amount,basic)->damage.accept(amount),visible,bossUnlocked,selected);
    }
    @FunctionalInterface interface DamageSink {void hit(int amount,boolean basic);}
    void updateSelected(String region,long tick,Position player,boolean alive,MovementService movement,CollisionService.Space space,DamageSink damage,java.util.function.Predicate<Position> visible,boolean bossUnlocked,java.util.function.Predicate<Enemy> selected) {
        viewTick=tick;
        for(Enemy e:current(region)) {
            if(!selected.test(e))continue;
            if(e.hp==0)continue;
            // ROOT preserva ataques; STUN suspende também a IA ofensiva enquanto durar.
            if(tick<e.skillStunUntil)continue;
            var movementSpace=occupiedSpace(e,space);
            float skillSpeed=tick<e.skillRootUntil?0:e.definition.speed()*(tick<e.skillSlowUntil?e.skillSlowScale:1);
            if(e.definition.id().equals("warden")&&!bossUnlocked){e.behavior="DORMANT";continue;}
            if(e.behavior.equals("DORMANT"))e.behavior="PATROL";
            double d=distance(e.getPosition(),player),homeDistance=distance(e.getPosition(),e.home);
            if(e.definition.id().equals("warden") && alive) {
                boolean enraged=e.hp*2<=e.definition.hp();
                if(e.behavior.equals("PREPARE")) {
                    if(tick>=e.attackTick) {
                        if(d<=(enraged?135:110)&&visible.test(e.getPosition()))damage.hit(e.definition.attack()*(enraged?2:1),false);
                        e.behavior="RECOVER";e.attackTick=tick+45;
                    }
                    continue;
                }
                if(e.behavior.equals("RECOVER")) { if(tick<e.attackTick)continue;e.behavior="CHASE";e.attackTick=tick+45; }
                if(d<=150&&tick>=e.attackTick) { e.behavior="PREPARE";e.attackTick=tick+(enraged?18:30);continue; }
            }
            // O Cero respeita a carga de cada corpo (Alado 220 ms / Sentinela 320 ms); alcance e dano continuam sob autoridade da simulacao.
            if(isCero(e.definition.id()) && alive && homeDistance<=e.definition.leash()) {
                if(e.behavior.equals("PREPARE")) {
                    if(tick>=e.attackTick) {
                        if(d<=240 && visible.test(e.getPosition()))damage.hit(e.definition.attack(),true);
                        e.behavior="ATTACK";e.attackTick=tick+12;
                    }
                    continue;
                }
                if(e.behavior.equals("ATTACK")) {
                    if(tick>=e.attackTick){e.behavior="RECOVER";e.attackTick=tick+14;}
                    continue;
                }
                if(e.behavior.equals("RECOVER")) {
                    if(tick<e.attackTick)continue;
                    e.behavior="CHASE";
                }
                if(d<=240 && visible.test(e.getPosition()) && !e.behavior.equals("RETURN")) {
                    e.behavior="PREPARE";e.attackTick=tick+(e.definition.id().equals("mob_alado")?7:10);continue;
                }
            }
            if(!alive || homeDistance>e.definition.leash() || d>e.definition.aggro()*1.4) e.behavior="RETURN";
            else if(d<=e.definition.aggro()&&visible.test(e.getPosition())&&!e.behavior.equals("RETURN"))e.behavior="CHASE";
            if(e.behavior.equals("RETURN")) {
                if(homeDistance<4){
                    if(!e.definition.id().startsWith("mob_")||new CollisionService().isFree(e,e.home.x(),e.home.y(),19,movementSpace)){e.setPosition(e.home);e.behavior="PATROL";}
                }
                else moveToward(e,e.home,skillSpeed,movement,movementSpace);
            } else if(e.behavior.equals("CHASE")||e.behavior.equals("ATTACK")) {
                if(d<=46 && visible.test(e.getPosition())) {
                    e.behavior="ATTACK";
                    if(tick>=e.attackTick){damage.hit(e.definition.attack(),true);e.attackTick=tick+36;}
                } else {e.behavior="CHASE";moveToward(e,player,skillSpeed,movement,movementSpace);}
            } else {
                Position patrol=new Position(e.home.x()+(tick/90%2==0?24:-24),e.home.y());
                moveToward(e,patrol,skillSpeed*0.3f,movement,movementSpace);
            }
        }
    }
    /** Acrescenta os corpos vivos aos novos mobs, preservando as regras dos arquetipos legados. */
    private CollisionService.Space occupiedSpace(Enemy moving,CollisionService.Space supplied) {
        if(!moving.definition.id().startsWith("mob_"))return supplied;
        var bodies=new ArrayList<>(supplied.bodies());
        for(var other:current(moving.region))if(other!=moving&&other.hp>0&&bodies.stream().noneMatch(b->b.entity()==other))
            bodies.add(new CollisionService.Body(other,19));
        return new CollisionService.Space(supplied.width(),supplied.height(),supplied.obstacles(),bodies);
    }
    /** Identifica somente os dois novos arquetipos de ataque basico Cero. */
    public static boolean isCero(String id){return id.equals("mob_alado")||id.equals("mob_sentinela");}
    private static double distance(Position a,Position b){return Math.hypot(a.x()-b.x(),a.y()-b.y());}
    /** Mantem o movimento direto e busca um desvio apenas quando o novo mob fica bloqueado. */
    private void moveToward(Enemy e,Position target,float speed,MovementService movement,CollisionService.Space space) {
        if(e.definition.id().startsWith("mob_")&&speed>0) {
            var collision=new CollisionService();
            if(e.detourTarget!=null&&distance(e.detourTarget,target)>24)e.detour.clear();
            while(!e.detour.isEmpty()&&distance(e.getPosition(),e.detour.peek())<1)e.detour.remove();
            if(!e.detour.isEmpty()&&!collision.canTraverseSegment(e,e.getPosition(),e.detour.peek(),19,space))e.detour.clear();
            if(!e.detour.isEmpty()){
                Position before=e.getPosition();moveDirect(e,e.detour.peek(),speed,movement,space);
                e.blockedRouteTicks=distance(before,e.getPosition())<speed/30*.2?e.blockedRouteTicks+1:0;
                // Se o trecho continua valido mas a resolucao por eixos nao avanca,
                // abandona o plano em seis ticks e permite buscar outra saida sozinho.
                if(e.blockedRouteTicks>=6){e.detour.clear();e.blockedRouteTicks=0;e.nextRouteTick=viewTick;}
                return;
            }
            Position before=e.getPosition();moveDirect(e,target,speed,movement,space);
            if(distance(before,e.getPosition())<speed/30*.2&&distance(e.getPosition(),target)>4&&viewTick>=e.nextRouteTick) {
                e.detourTarget=target;e.nextRouteTick=viewTick+15;e.blockedRouteTicks=0;
                e.detour.addAll(MobNavigation.route(e,target,space));
            }
            return;
        }
        moveDirect(e,target,speed,movement,space);
    }
    /** Usa a mesma velocidade e varredura para o destino direto e para cada trecho do desvio. */
    private static void moveDirect(Enemy e,Position target,float speed,MovementService movement,CollisionService.Space space) {
        Position p=e.getPosition(),direction=movement.direction(target.x()-p.x(),target.y()-p.y());
        // Limita o ultimo passo ao destino para evitar oscilacao em torno do ponto de patrulha.
        float step=(float)Math.min(speed/30,distance(p,target));
        movement.moveContinuous(e,direction.x()*step,direction.y()*step,19,space);
    }
    Set<String> saveFlags() {
        Set<String> result=new TreeSet<>();
        for(Enemy e:enemies)result.add("mob:"+e.spawn+":"+e.hp+":"+e.getPosition().x()+":"+e.getPosition().y()+":"+e.generation+":"+e.readyTick+":"+e.behavior+":"+e.attackTick);
        return result;
    }
    void restore(Set<String> flags,CollisionService.Space space) {
        Set<String> seen=new HashSet<>();
        for(String f:flags)if(f.startsWith("mob:")) {
            String[] v=f.split(":");if((v.length!=7&&v.length!=9)||!seen.add(v[1]))throw new IllegalArgumentException("Estado de mob invalido");
            Enemy e=enemies.stream().filter(m->m.spawn.equals(v[1])).findFirst().orElseThrow(()->new IllegalArgumentException("Spawn ausente"));
            e.hp=Integer.parseInt(v[2]);e.generation=Long.parseLong(v[5]);e.readyTick=Long.parseLong(v[6]);
            float x=Float.parseFloat(v[3]),y=Float.parseFloat(v[4]);
            if(e.hp<0||e.hp>e.definition.hp()||e.generation<1||e.readyTick<0||!new CollisionService().isFree(e,x,y,19,space))throw new IllegalArgumentException("Estado de inimigo invalido");
            e.setPosition(new Position(x,y));
            if(v.length==9) {
                if(!Set.of("PATROL","CHASE","ATTACK","RETURN","PREPARE","RECOVER","DORMANT").contains(v[7]))throw new IllegalArgumentException("IA invalida");
                e.behavior=v[7];e.attackTick=Long.parseLong(v[8]);if(e.attackTick<0)throw new IllegalArgumentException("Recarga invalida");
            }
        }
    }
}
