package com.projetok.rpg;
import com.projetok.domain.entity.*;
import com.projetok.domain.movement.*;
import java.util.*;
import java.io.*;

/** Populacao finita definida em conteudo; estado persistente de cada spawn. */
public final class EnemyService {
    public record Definition(String id,int hp,int attack,int speed,int aggro,int leash,int respawn,int level) {
        public Definition { if(level!=1)throw new IllegalArgumentException("Recompensas de mobs acima do nivel 1 ainda nao definidas"); }
        public Definition(String id,int hp,int attack,int speed,int aggro,int leash,int respawn) {this(id,hp,attack,speed,aggro,leash,respawn,1);}
    }
    public static final class Enemy extends GameEntity {
        final String spawn,region; final Definition definition; final Position home;
        int hp; long generation=1,readyTick,attackTick; String behavior="PATROL";
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
        this.geometry=geometry;
        Properties definitions=read("enemies"),spawns=new Properties();spawns.putAll(spawnData); Map<String,Definition> types=new TreeMap<>();
        for(String id:definitions.stringPropertyNames()) {
            String[] v=definitions.getProperty(id).split("\\|");if(v.length!=6&&v.length!=7)throw new IllegalArgumentException("Inimigo invalido");
            content.get("enemy",id); int[] n=Arrays.stream(v).mapToInt(Integer::parseInt).toArray();
            if(n[0]<1||n[0]>10000||n[1]<1||n[1]>100||n[2]<1||n[2]>200||n[3]<40||n[3]>800||n[4]<n[3]||n[4]>900||n[5]<0||n[5]>9000)throw new IllegalArgumentException("Parametros PvE invalidos");
            types.put(id,new Definition(id,n[0],n[1],n[2],n[3],n[4],n[5],n.length==7?n[6]:1));
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
    private static Properties read(String name) {
        try(InputStream in=EnemyService.class.getResourceAsStream("/rpg/"+name+".properties")) {
            if(in==null)throw new IOException("Conteudo ausente");Properties p=new Properties();p.load(in);return p;
        }catch(IOException e){throw new IllegalStateException(e);}
    }
    List<Enemy> current(String region) { return enemies.stream().filter(e->e.region.equals(region)).toList(); }
    private long viewTick;
    public List<View> views(String region) { return views(region,viewTick); }
    /** Publica aviso com o tick do mundo, inclusive imediatamente apos retomar um save. */
    public List<View> views(String region,long tick) { return current(region).stream().map(e->new View(e.getId(),e.spawn,e.getName(),e.getPosition().x(),e.getPosition().y(),e.hp,e.definition.hp(),e.hp==0?"DEAD":e.behavior,e.definition.id().equals("warden"),e.hp>0&&e.behavior.equals("PREPARE")?new RpgBossThreat(e.getPosition().x(),e.getPosition().y(),e.hp*2<=e.definition.hp()?135:110,Math.max(0,e.attackTick-tick),e.hp*2<=e.definition.hp(),geometry.apply(region)):null)).toList(); }
    void respawn(long tick,String region) {
        for(Enemy e:current(region))if(e.hp==0&&e.definition.respawn()>0&&tick>=e.readyTick){e.hp=e.definition.hp();e.generation++;e.setPosition(e.home);e.behavior="PATROL";}
    }
    /** Multiplayer respawn waits for an unoccupied home rather than overlapping a player. */
    void respawn(long tick,String region,CollisionService.Space occupied) {
        for(Enemy e:current(region))if(e.hp==0&&e.definition.respawn()>0&&tick>=e.readyTick
                &&new CollisionService().isFree(e,e.home.x(),e.home.y(),19,occupied)){
            e.hp=e.definition.hp();e.generation++;e.setPosition(e.home);e.behavior="PATROL";
        }
    }
    void update(String region,long tick,Position player,boolean alive,MovementService movement,CollisionService.Space space,java.util.function.IntConsumer damage,java.util.function.Predicate<Position> visible,boolean bossUnlocked) {
        viewTick=tick;
        for(Enemy e:current(region)) {
            if(e.hp==0)continue;
            if(e.definition.id().equals("warden")&&!bossUnlocked){e.behavior="DORMANT";continue;}
            if(e.behavior.equals("DORMANT"))e.behavior="PATROL";
            double d=distance(e.getPosition(),player),homeDistance=distance(e.getPosition(),e.home);
            if(e.definition.id().equals("warden") && alive) {
                boolean enraged=e.hp*2<=e.definition.hp();
                if(e.behavior.equals("PREPARE")) {
                    if(tick>=e.attackTick) {
                        if(d<=(enraged?135:110)&&visible.test(e.getPosition()))damage.accept(e.definition.attack()*(enraged?2:1));
                        e.behavior="RECOVER";e.attackTick=tick+45;
                    }
                    continue;
                }
                if(e.behavior.equals("RECOVER")) { if(tick<e.attackTick)continue;e.behavior="CHASE";e.attackTick=tick+45; }
                if(d<=150&&tick>=e.attackTick) { e.behavior="PREPARE";e.attackTick=tick+(enraged?18:30);continue; }
            }
            if(!alive || homeDistance>e.definition.leash() || d>e.definition.aggro()*1.4) e.behavior="RETURN";
            else if(d<=e.definition.aggro()&&visible.test(e.getPosition())&&!e.behavior.equals("RETURN"))e.behavior="CHASE";
            if(e.behavior.equals("RETURN")) {
                if(homeDistance<4){e.setPosition(e.home);e.behavior="PATROL";}
                else moveToward(e,e.home,e.definition.speed(),movement,space);
            } else if(e.behavior.equals("CHASE")||e.behavior.equals("ATTACK")) {
                if(d<=46 && visible.test(e.getPosition())) {
                    e.behavior="ATTACK";
                    if(tick>=e.attackTick){damage.accept(e.definition.attack());e.attackTick=tick+36;}
                } else {e.behavior="CHASE";moveToward(e,player,e.definition.speed(),movement,space);}
            } else {
                Position patrol=new Position(e.home.x()+(tick/90%2==0?24:-24),e.home.y());
                moveToward(e,patrol,e.definition.speed()*0.3f,movement,space);
            }
        }
    }
    private static double distance(Position a,Position b){return Math.hypot(a.x()-b.x(),a.y()-b.y());}
    private static void moveToward(Enemy e,Position target,float speed,MovementService movement,CollisionService.Space space) {
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
