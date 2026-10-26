package com.projetok.studio;

import com.projetok.domain.entity.*;
import com.projetok.domain.movement.*;
import com.projetok.multiplayer.*;
import com.projetok.rpg.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** Owned loopback game server. Editor controls are an unforgeable in-process
 * capability/queue, never encoded in Protocol or accepted from player sockets. */
public final class PreviewServer implements AutoCloseable,PublicationTarget {
    public enum Status { DESLIGADO,INICIANDO,PRONTO,SINCRONIZANDO,SIMULANDO,ERRO,ENCERRANDO }
    public enum EditKind { FULL_SYNC,TILE,LAYER,OBSTACLE,OBJECT,NPC,PORTAL,SPAWN,PROFILE }
    public record Edit(UUID session,long baseRevision,long revision,EditKind type,String reference,LiveRevision content){
        public Edit{Objects.requireNonNull(session);Objects.requireNonNull(type);Objects.requireNonNull(content);if(reference==null||reference.length()>80||revision!=content.number()||revision<0)throw new IllegalArgumentException("Evento invalido");}
    }
    public record Ack(boolean accepted,long revision,String hash,String message,long validationNanos,long applyNanos){}
    public record State(UUID session,Status status,LiveRevision revision,Map<Integer,RpgWorld.View> players,boolean paused,long tick,long tickNanos,int queued,long rejected){public State{players=Map.copyOf(players);}}
    private final UUID session=UUID.randomUUID();private final Backend backend;private final LocalSimulationServer server;
    private final Path temporary;private final AtomicBoolean closed=new AtomicBoolean();
    private volatile State state;private volatile Status status=Status.INICIANDO;
    private final Set<Integer> renderers=ConcurrentHashMap.newKeySet();
    public PreviewServer(LiveRevision initial,String race,int level)throws Exception{
        CharacterRaces.get(race);if(level!=1&&level!=20)throw new IllegalArgumentException("Nivel de teste: 1 ou 20");temporary=Files.createTempDirectory("spiritgate-preview-");backend=new Backend(initial,race,level);state=new State(session,Status.INICIANDO,initial,Map.of(),false,0,0,0,0);
        server=new LocalSimulationServer(InetAddress.getByName("127.0.0.1"),0,backend);try{server.start();status=Status.PRONTO;}catch(Exception error){server.close();Files.deleteIfExists(temporary);throw error;}
    }
    public String activeHash(){return state().revision().hash();}
    public UUID session(){return session;}public int port(){return server.port();}public Path temporary(){return temporary;}
    /** Receiver validation is called from the editor worker, never the EDT. */
    public CompletableFuture<Ack> patch(UUID session,long base,long number,DraftPatch patch,AssetCatalog.Result assets)throws Exception{
        var before=backend.revision;if(!this.session.equals(session)||before.number()!=base)return CompletableFuture.completedFuture(new Ack(false,before.number(),before.hash(),"Sessao/base divergente; solicite snapshot completo",0,0));
        var maps=new TreeMap<>(before.bundle().maps());var document=maps.get(patch.mapId());if(document==null)throw new IllegalArgumentException("Mapa ausente");maps.put(patch.mapId(),patch.apply(document));long started=System.nanoTime();var next=LiveRevision.prepare(number,before.bundle().start(),maps,assets);long validation=System.nanoTime()-started;
        return edit(new Edit(session,base,number,EditKind.OBJECT,patch.mapId(),next)).thenApply(a->new Ack(a.accepted(),a.revision(),a.hash(),a.message(),validation,a.applyNanos()));
    }
    public State state(){return state;}public Throwable failure(){return server.failure();}
    public void registerRenderer(int player){renderers.add(player);}
    public CompletableFuture<Ack> reserve(String expected,LiveRevision next){return submit(()->{
        if(backend.reserved!=null||next.number()<=backend.revision.number()||!backend.revision.hash().equals(expected))return backend.ack(false,"Destino ocupado ou revisao desatualizada",0,0);
        if(!renderers.containsAll(backend.players.keySet()))return backend.ack(false,"Cliente sem contrato de mapa/revisao Live",0,0);
        var candidate=backend.prepare(next);backend.reserved=new Reservation(backend.revision,new TreeMap<>(backend.players),backend.encounter,next,candidate,backend.paused);backend.paused=true;return backend.ack(true,"Destino reservado e validado",0,0);
    });}
    public CompletableFuture<Ack> activateReserved(){return submit(()->{var r=backend.reserved;if(r==null)throw new IllegalStateException("Sem reserva");backend.install(r.next,r.prepared);return backend.ack(true,"Ativacao confirmada; aguardando commit",0,0);});}
    public CompletableFuture<Ack> confirmReserved(){return submit(()->{var r=backend.reserved;if(r==null)throw new IllegalStateException("Sem reserva");backend.paused=r.paused;backend.reserved=null;return backend.ack(true,"Publicacao confirmada",0,0);});}
    public CompletableFuture<Ack> abortReserved(){return submit(()->{var r=backend.reserved;if(r!=null){backend.players.clear();backend.players.putAll(r.beforePlayers);backend.encounter=r.beforeEncounter;backend.revision=r.before;backend.paused=r.paused;backend.reserved=null;}return backend.ack(r!=null,r!=null?"Reserva revertida":"Sem reserva; transacao ja finalizada",0,0);});}
    public CompletableFuture<Ack> edit(Edit edit){
        if(!session.equals(edit.session()))return CompletableFuture.completedFuture(new Ack(false,backend.revision.number(),backend.revision.hash(),"Sessao de preview incorreta",0,0));
        status=Status.SINCRONIZANDO;return submit(()->backend.apply(edit));
    }
    public CompletableFuture<Ack> pause(boolean value){return submit(()->{if(backend.reserved!=null)return backend.ack(false,"Destino reservado",0,0);backend.paused=value;backend.players.values().forEach(p->{p.x=p.y=0;});return backend.ack(true,"Pausa: "+value,0,0);});}
    public CompletableFuture<Ack> spawn(int id){return submit(()->{if(backend.reserved!=null)return backend.ack(false,"Destino reservado",0,0);var p=backend.players.get(id);if(p==null)throw new IllegalArgumentException("Jogador ausente");p.world.setSyntheticBodies(backend.bodies(id,backend.players,backend.encounter));p.world.restoreSyntheticSpawn();return backend.ack(true,"Spawn restaurado",0,0);});}
    public CompletableFuture<Ack> reset(String race,int level){return submit(()->{if(backend.reserved!=null)throw new IllegalStateException("Publicacao em andamento");backend.reset(race,level);return backend.ack(true,"Perfis sinteticos reiniciados",0,0);});}
    public CompletableFuture<Ack> command(int id,String action){return submit(()->{if(backend.reserved!=null||backend.paused)return backend.ack(false,"Destino reservado",0,0);var p=backend.players.get(id);if(p==null)throw new IllegalArgumentException("Jogador ausente");p.world.setSyntheticBodies(backend.bodies(id,backend.players,backend.encounter));boolean ok=switch(action){case "attack"->p.world.attack();case "shunpo"->p.world.castAbility();case "evade"->p.world.castSecondaryAbility();case "interact"->p.world.interact();case "respawn"->{var home=backend.revision.bundle().scenes().maps().get(backend.revision.bundle().scenes().initialRegion()).spawn();yield !backend.occupied(id,backend.revision.bundle().scenes().initialRegion(),home.x(),home.y())&&p.world.returnHome();}case "portal"->{var v=p.world.snapshot();var near=v.portals().stream().filter(t->Math.hypot(t.x()-v.x(),t.y()-v.y())<=65).findFirst();if(near.isEmpty())yield false;var target=near.get();boolean occupied=backend.players.entrySet().stream().anyMatch(e->e.getKey()!=id&&e.getValue().world.snapshot().state().region().equals(target.destination())&&Math.hypot(e.getValue().world.snapshot().x()-target.arrivalX(),e.getValue().world.snapshot().y()-target.arrivalY())<2*backend.revision.profile().actorRadius());occupied|=backend.encounter.enemies(target.destination(),backend.tick).stream().anyMatch(e->e.hp()>0&&Math.hypot(e.x()-target.arrivalX(),e.y()-target.arrivalY())<backend.revision.profile().actorRadius()+19);yield !occupied&&p.world.travel(target.id());}default->throw new IllegalArgumentException("Comando nao cadastrado");};return backend.ack(ok,ok?"Acao aceita":"Acao recusada pelo motor",0,0);});}
    private interface Work{Ack run()throws Exception;}
    private record Pending(Work work,CompletableFuture<Ack> result){}
    private synchronized CompletableFuture<Ack> submit(Work work){var future=new CompletableFuture<Ack>();if(closed.get()||!server.running()||!backend.commands.offer(new Pending(work,future)))future.completeExceptionally(new IllegalStateException("Preview encerrado ou fila cheia"));return future;}
    @Override public synchronized void close(){if(!closed.compareAndSet(false,true))return;status=Status.ENCERRANDO;server.close();for(Pending p;(p=backend.commands.poll())!=null;)p.result.completeExceptionally(new IllegalStateException("Preview encerrado"));try{Files.deleteIfExists(temporary);}catch(Exception ignored){}status=Status.DESLIGADO;}
    private static final class Player {RpgWorld world;long sequence,inputTick;float x,y;Player(RpgWorld w){world=w;}}
    private record Prepared(Map<Integer,Player> players,SharedRpgEncounter encounter){}
    private record Reservation(LiveRevision before,Map<Integer,Player> beforePlayers,SharedRpgEncounter beforeEncounter,LiveRevision next,Prepared prepared,boolean paused){}
    private final class Backend implements SessionSimulation {
        final ArrayBlockingQueue<Pending> commands=new ArrayBlockingQueue<>(16);final Map<Integer,Player> players=new TreeMap<>();
        volatile LiveRevision revision;SharedRpgEncounter encounter;long tick,rejected;boolean paused;String race;int level;Reservation reserved;
        Backend(LiveRevision initial,String race,int level){this.revision=initial;check(race,level);this.race=race;this.level=level;encounter=new SharedRpgEncounter(initial.geometry(),initial.bundle().scenes().enemies(),SharedRpgEncounter.RewardPolicy.DISABLED);}
        void check(String race,int level){CharacterRaces.get(race);if(level!=1&&level!=20)throw new IllegalArgumentException("Nivel de teste: 1 ou 20");}
        Ack ack(boolean ok,String text,long validation,long apply){return new Ack(ok,revision.number(),revision.hash(),text,validation,apply);}
        public boolean join(int id,String name){if(reserved!=null||players.size()==2||players.containsKey(id))return false;var w=new RpgWorld(CharacterIdentity.create(name,race,"azure"),revision.bundle().scenes());w.configureSyntheticCollision(revision.profile());w.awardExperience(RpgProgression.builtin().threshold(level));
            var candidate=new Player(w);players.put(id,candidate);try{var bodies=bodies(id,players,encounter);if(!w.syntheticPositionFree(bodies)){var v=w.snapshot();var scenes=revision.bundle().scenes();boolean found=false;for(float dx:new float[]{40,-40,80,-80}){var old=scenes.maps().get(v.state().region());var maps=new TreeMap<>(scenes.maps());maps.put(v.state().region(),new RpgSceneSet.Geometry(old.space(),new Position(v.x()+dx,v.y())));var adjusted=new RpgSceneSet(scenes.initialRegion(),maps,scenes.enemies(),scenes.npcs(),scenes.objects(),scenes.portals());var placed=new RpgWorld(w.snapshot().identity(),adjusted);placed.configureSyntheticCollision(revision.profile());placed.awardExperience(RpgProgression.builtin().threshold(level));if(placed.syntheticPositionFree(bodies)){candidate.world=placed;found=true;break;}}if(!found){players.remove(id);return false;}}
                encounter.join(id,candidate.world);status=paused?Status.PRONTO:Status.SIMULANDO;return true;
            }catch(RuntimeException e){players.remove(id);return false;}}
        public void leave(int id){if(!players.containsKey(id))return;if(reserved!=null){var r=reserved;players.clear();players.putAll(r.beforePlayers);encounter=r.beforeEncounter;revision=r.before;paused=r.paused;reserved=null;}var p=players.remove(id);renderers.remove(id);if(p!=null)encounter.leave(p.world);}
        public boolean accept(int id,Protocol.Intent intent){var p=players.get(id);if(p==null||intent.sequence()<=p.sequence||intent.action()!=Protocol.Action.MOVE||Math.abs(intent.x())>1||Math.abs(intent.y())>1)return false;p.sequence=intent.sequence();p.x=intent.x();p.y=intent.y();p.inputTick=tick;return true;}
        List<CollisionService.Body> bodies(int id,Map<Integer,Player> source,SharedRpgEncounter population){var result=new ArrayList<CollisionService.Body>();var local=source.get(id).world.snapshot();source.forEach((other,p)->{var v=p.world.snapshot();if(other!=id&&v.state().hp()>0&&v.state().region().equals(local.state().region()))result.add(new CollisionService.Body(new GameEntity(other,"Sintetico",new Position(v.x(),v.y())){},p.world.physicalRadius()));});for(var enemy:population.enemies(local.state().region(),tick))if(enemy.hp()>0)result.add(new CollisionService.Body(new GameEntity(enemy.id(),enemy.name(),new Position(enemy.x(),enemy.y())){},19));return result;}
        boolean occupied(int id,String region,float x,float y){return players.entrySet().stream().anyMatch(e->{var v=e.getValue().world.snapshot();return e.getKey()!=id&&v.state().hp()>0&&v.state().region().equals(region)&&Math.hypot(v.x()-x,v.y()-y)<2*revision.profile().actorRadius();})||encounter.enemies(region,tick).stream().anyMatch(e->e.hp()>0&&Math.hypot(e.x()-x,e.y()-y)<revision.profile().actorRadius()+19);}
        Prepared prepare(LiveRevision next){var nextEncounter=encounter.fork(next.geometry(),next.bundle().scenes().enemies());var candidates=new TreeMap<Integer,Player>();for(var p:players.entrySet())candidates.put(p.getKey(),new Player(p.getValue().world.forkSyntheticRevision(next.bundle().scenes(),next.profile())));
                for(var p:candidates.entrySet())if(!p.getValue().world.syntheticPositionFree(bodies(p.getKey(),candidates,nextEncounter)))throw new IllegalArgumentException("Revisao causaria sobreposicao de atores");
                for(var p:candidates.entrySet()){p.getValue().sequence=players.get(p.getKey()).sequence;nextEncounter.join(p.getKey(),p.getValue().world);}
                return new Prepared(candidates,nextEncounter);
        }
        void install(LiveRevision next,Prepared prepared){players.clear();players.putAll(prepared.players);encounter=prepared.encounter;revision=next;status=paused?Status.PRONTO:Status.SIMULANDO;}
        Ack apply(Edit edit){long started=System.nanoTime();if(reserved!=null||edit.revision()<=revision.number()||edit.type()!=EditKind.FULL_SYNC&&edit.baseRevision()!=revision.number()){rejected++;return ack(false,"Revisao fora de ordem ou destino reservado; sincronize estado completo",0,0);}var next=edit.content();
            try{install(next,prepare(next));return ack(true,"Revisao aplicada na fronteira de tick",0,System.nanoTime()-started);
            }catch(RuntimeException error){rejected++;status=Status.ERRO;return ack(false,error.getMessage(),0,System.nanoTime()-started);}
        }
        void reset(String race,int level){check(race,level);var names=new TreeMap<Integer,String>();players.forEach((id,p)->names.put(id,p.world.snapshot().identity().name()));players.clear();encounter=new SharedRpgEncounter(revision.geometry(),revision.bundle().scenes().enemies(),SharedRpgEncounter.RewardPolicy.DISABLED);this.race=race;this.level=level;names.forEach(this::join);}
        public void step(){long started=System.nanoTime();for(int i=0;i<4;i++){var job=commands.poll();if(job==null)break;try{job.result.complete(job.work.run());}catch(Exception error){job.result.completeExceptionally(error);}}
            if(!paused){tick++;for(var p:players.entrySet()){p.getValue().world.setSyntheticBodies(bodies(p.getKey(),players,encounter));p.getValue().world.tick(tick-p.getValue().inputTick<=15?p.getValue().x:0,tick-p.getValue().inputTick<=15?p.getValue().y:0);}encounter.step(tick);}
            var views=new TreeMap<Integer,RpgWorld.View>();players.forEach((id,p)->views.put(id,p.world.snapshot()));state=new State(session,status,revision,views,paused,tick,System.nanoTime()-started,commands.size(),rejected);
        }
        public Protocol.State snapshot(){return new Protocol.State(tick,players.entrySet().stream().map(p->{var v=p.getValue().world.snapshot();return new Protocol.Actor(p.getKey(),v.identity().name(),v.x(),v.y(),v.state().hp(),v.maxHp(),0,0,false);}).toList());}
    }
}
