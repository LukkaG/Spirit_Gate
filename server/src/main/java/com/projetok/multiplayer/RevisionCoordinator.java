package com.projetok.multiplayer;

import com.projetok.studio.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Supplier;

/** Owner-thread state machine. Only the possessed PublicationTarget can publish;
 * public sockets can acknowledge an already offered transaction, never create it. */
public final class RevisionCoordinator implements PublicationTarget {
    public record Metrics(long descriptorPayloadBytes,long queuedWireBytes,long serverPrepareNanos,long waitingClientsNanos,long activationNanos,long transitionNanos,long recoveryNanos){}
    private volatile Metrics metrics=new Metrics(0,0,0,0,0,0,0);
    public Metrics metrics(){return metrics;}
    /** Diagnóstico local sem hashes; valores de duração permanecem em nanossegundos. */
    public String metricsJson(){var m=metrics;return "{\"schema\":1,\"kind\":\"publication\",\"phase\":\""+status.phase().name()
            +"\",\"descriptor_payload_bytes\":"+m.descriptorPayloadBytes()+",\"queued_wire_bytes\":"+m.queuedWireBytes()
            +",\"server_prepare_ns\":"+m.serverPrepareNanos()+",\"waiting_clients_ns\":"+m.waitingClientsNanos()
            +",\"activation_ns\":"+m.activationNanos()+",\"transition_ns\":"+m.transitionNanos()+",\"recovery_ns\":"+m.recoveryNanos()+"}";}
    private long payloadBytes,wireBytes;
    public enum Phase { IDLE,VALIDATING,PREPARING,WAITING_CLIENTS,READY,ACTIVATING,VERIFYING,COMMITTED,ABORTED,RECOVERING }
    public record Status(Phase phase,String hash,String candidate,Map<Integer,String> clients,String detail,long serverPrepareNanos,long startedNanos){public Status{clients=Map.copyOf(clients);}}
    private final long timeout=timeout();
    private static long timeout(){long value=Long.getLong("spiritgate.content.timeoutMillis",25000);if(value<500||value>25000)throw new IllegalArgumentException("Timeout conteudo: 500..25000 ms");return TimeUnit.MILLISECONDS.toNanos(value);}
    private final RpgSessionWorld world;
    private final AssetCatalog.Result assets;
    private record Pending(Runnable work,CompletableFuture<?> result){}
    private final ArrayBlockingQueue<Pending> jobs=new ArrayBlockingQueue<>(16);
    private final Map<Integer,Client> clients=new TreeMap<>();
    private volatile LiveRevision activeContent;
    public LiveRevision revision(){return activeContent;}
    private volatile Status status;
    private volatile Tx transaction;
    private volatile boolean closed;
    private long sequence;
    private boolean published;
    private static final class Client {
        final int application;final ArrayDeque<Protocol.Content> controls=new ArrayDeque<>();
        byte[] transfer;int offset;long tx,deadline;String hash,state="CONNECTED";boolean end,kick,initial;
        Client(int application){this.application=application;}
    }
    private static final class Tx {
        final long id;final LiveRevision next;final RpgSessionWorld.RevisionCandidate before,candidate;
        final CompletableFuture<PreviewServer.Ack> reserve;CompletableFuture<PreviewServer.Ack> activation,abort;
        Phase phase=Phase.WAITING_CLIENTS;final long started=System.nanoTime();long readyAt,activationStart,verifiedAt,recoveryStart,payloadBefore,wireBefore;long deadline;String failure="";
        Tx(long id,LiveRevision next,RpgSessionWorld.RevisionCandidate before,RpgSessionWorld.RevisionCandidate candidate,CompletableFuture<PreviewServer.Ack> reserve,long timeout){this.id=id;this.next=next;this.before=before;this.candidate=candidate;this.reserve=reserve;deadline=System.nanoTime()+timeout;}
    }
    public RevisionCoordinator(RpgSessionWorld world,LiveRevision initial,AssetCatalog.Result assets){this.assets=assets;this.world=world;activeContent=initial;try{published=!initial.hash().equals(WorldContent.baseline().hash());}catch(Exception e){throw new IllegalStateException(e);}status=new Status(Phase.IDLE,initial.hash(),"",Map.of(),"Pronto",0,0);}
    public Status status(){return status;}
    public String activeHash(){return status.hash();}
    public String compatibility(){return "rpg-content-application-4/v1";}
    private PreviewServer.Ack ack(boolean accepted,String message){return new PreviewServer.Ack(accepted,world.capture().content().number(),world.contentHash(),message,0,0);}
    /** Enfileirar e encerrar são atômicos entre si; nenhum futuro fica órfão. */
    private synchronized void queue(Runnable work,CompletableFuture<?> result){if(closed||!jobs.offer(new Pending(work,result)))result.completeExceptionally(new IllegalStateException("Servidor encerrado ou fila cheia"));}
    private CompletableFuture<PreviewServer.Ack> submit(Supplier<PreviewServer.Ack> job){var f=new CompletableFuture<PreviewServer.Ack>();queue(()->{try{f.complete(job.get());}catch(Throwable e){f.completeExceptionally(e);}},f);return f;}
    public CompletableFuture<PreviewServer.Ack> reserve(String expected,LiveRevision next){
        var f=new CompletableFuture<PreviewServer.Ack>();final byte[] bytes;final LiveRevision safe;
        try{bytes=WorldContent.encode(next);safe=WorldContent.decode(bytes,assets);}catch(Exception e){f.completeExceptionally(e);return f;}
        queue(()->{try{
            if(transaction!=null||!world.contentHash().equals(expected)||safe.number()<=world.capture().content().number())throw new IllegalStateException("Revisao base divergente ou publicacao concorrente");
            for(var entry:clients.entrySet())if(entry.getValue().application!=4||!entry.getValue().state.equals("ACTIVE"))throw new IllegalStateException("Cliente "+entry.getKey()+" desatualizado ou ainda nao preparado");
            long start=System.nanoTime();world.freeze(true);var candidate=world.prepareRevision(safe);var tx=new Tx(++sequence,safe,world.capture(),candidate,f,timeout);transaction=tx;tx.payloadBefore=payloadBytes;tx.wireBefore=wireBytes;
            for(var client:clients.values())offer(client,tx.id,safe.hash(),bytes,false);
            status=new Status(Phase.WAITING_CLIENTS,world.contentHash(),safe.hash(),Map.of(),"Preparando clientes",System.nanoTime()-start,start);
        }catch(Exception e){if(transaction==null)world.freeze(clients.values().stream().anyMatch(c->c.initial));f.complete(ack(false,e.getMessage()));}},f);
        return f;
    }
    public CompletableFuture<PreviewServer.Ack> activateReserved(){var result=new CompletableFuture<PreviewServer.Ack>();var queued=submit(()->{
        var tx=transaction;if(tx==null||tx.phase!=Phase.READY)throw new IllegalStateException("Clientes nao preparados");
        world.installRevision(tx.candidate);tx.activationStart=System.nanoTime();tx.phase=Phase.VERIFYING;tx.deadline=System.nanoTime()+timeout;tx.activation=result;
        clients.values().forEach(c->{c.state="ACTIVATING";control(c,Protocol.ContentKind.ACTIVATE,tx.id,tx.next.hash());});return ack(true,"Verificando renderizadores");});
        queued.whenComplete((a,e)->{if(e!=null)result.completeExceptionally(e);});return result;}
    public CompletableFuture<PreviewServer.Ack> confirmReserved(){return submit(()->{
        var tx=transaction;if(tx==null||tx.phase!=Phase.VERIFYING||clients.values().stream().anyMatch(c->!c.state.equals("ACTIVE")))throw new IllegalStateException("Ativacao nao verificada");
        clients.values().forEach(c->control(c,Protocol.ContentKind.RESUME,tx.id,tx.next.hash()));measure(tx);transaction=null;published=true;world.freeze(false);update(Phase.COMMITTED,"Publicacao confirmada nos clientes e servidor");return ack(true,"Publicacao confirmada");});}
    public CompletableFuture<PreviewServer.Ack> abortReserved(){var result=new CompletableFuture<PreviewServer.Ack>();var queued=submit(()->{var tx=transaction;if(tx==null){result.complete(ack(false,"Sem transacao"));return ack(false,"Sem transacao");}tx.abort=result;recover("Cancelada antes do commit");return ack(true,"Recuperando");});queued.whenComplete((a,e)->{if(e!=null)result.completeExceptionally(e);});return result;}
    private void control(Client c,Protocol.ContentKind kind,long id,String hash){if(c.controls.size()>=12){c.kick=true;return;}c.controls.add(new Protocol.Content(kind,id,hash,0,new byte[0]));}
    private void offer(Client c,long id,String hash,byte[] bytes,boolean initial){c.transfer=bytes;c.offset=0;c.end=false;c.tx=id;c.hash=hash;c.state="PREPARING";c.initial=initial;c.deadline=System.nanoTime()+timeout;c.controls.clear();c.controls.add(new Protocol.Content(Protocol.ContentKind.BEGIN,id,hash,bytes.length,(initial?"":world.contentHash()).getBytes(java.nio.charset.StandardCharsets.UTF_8)));}
    public boolean admit(int id,int application){
        if(closed||transaction!=null||published&&application!=4)return false;
        var c=new Client(application);clients.put(id,c);
        if(application==4)try{offer(c,++sequence,world.contentHash(),WorldContent.encode(world.capture().content()),true);world.freeze(true);}catch(Exception e){clients.remove(id);return false;}
        return true;
    }
    public void depart(int id){if(clients.remove(id)==null)return;if(transaction!=null)recover("Cliente "+id+" desconectou durante publicacao");else world.freeze(clients.values().stream().anyMatch(c->c.initial));}
    public boolean disconnect(int id){var c=clients.get(id);return c!=null&&c.kick;}
    public Protocol.Content next(int id){var c=clients.get(id);if(c==null)return null;if(!c.controls.isEmpty())return count(c.controls.poll());if(c.transfer==null)return null;
        if(c.offset<c.transfer.length){int offset=c.offset;int n=Math.min(2800,c.transfer.length-offset);c.offset+=n;return count(new Protocol.Content(Protocol.ContentKind.CHUNK,c.tx,c.hash,offset,Arrays.copyOfRange(c.transfer,offset,offset+n)));}
        if(!c.end){c.end=true;c.transfer=null;return count(new Protocol.Content(Protocol.ContentKind.END,c.tx,c.hash,0,new byte[0]));}return null;
    }
    private Protocol.Content count(Protocol.Content message){wireBytes+=86+message.data().length;if(message.kind()==Protocol.ContentKind.CHUNK)payloadBytes+=message.data().length;return message;}
    private void measure(Tx tx){long now=System.nanoTime();metrics=new Metrics(payloadBytes-tx.payloadBefore,wireBytes-tx.wireBefore,status.serverPrepareNanos(),(tx.readyAt==0?now:tx.readyAt)-tx.started,tx.activationStart==0?0:(tx.verifiedAt==0?now:tx.verifiedAt)-tx.activationStart,now-tx.started,tx.recoveryStart==0?0:now-tx.recoveryStart);}
    public void ack(int id,Protocol.Content message){var c=clients.get(id);if(c==null||c.application!=4||c.tx!=message.transaction()||!Objects.equals(c.hash,message.hash())||message.data().length>240)return;
        if(message.kind()==Protocol.ContentKind.REJECT){if(c.initial){c.kick=true;return;}recover("Cliente "+id+" rejeitou: "+new String(message.data(),java.nio.charset.StandardCharsets.UTF_8));return;}
        if(message.kind()==Protocol.ContentKind.READY&&c.state.equals("PREPARING")&&c.end){c.state="READY";if(c.initial){c.state="ACTIVATING";control(c,Protocol.ContentKind.ACTIVATE,c.tx,c.hash);}}
        else if(message.kind()==Protocol.ContentKind.ACTIVE&&(c.state.equals("ACTIVATING")||c.state.equals("RECOVERING"))){c.state="ACTIVE";if(c.initial){c.initial=false;control(c,Protocol.ContentKind.RESUME,c.tx,c.hash);if(transaction==null)world.freeze(clients.values().stream().anyMatch(p->p.initial));}}
    }
    private void recover(String reason){var tx=transaction;if(tx==null||tx.phase==Phase.RECOVERING)return;world.installRevision(tx.before);tx.phase=Phase.RECOVERING;tx.recoveryStart=System.nanoTime();tx.failure=reason;tx.deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(8);
        clients.values().forEach(c->{c.controls.clear();c.transfer=null;c.state="RECOVERING";c.tx=tx.id;c.hash=tx.before.content().hash();control(c,Protocol.ContentKind.ABORT,c.tx,c.hash);});}
    private void update(Phase phase,String detail){activeContent=world.capture().content();var states=new TreeMap<Integer,String>();clients.forEach((id,c)->states.put(id,"V"+c.application+" "+c.state));status=new Status(phase,world.contentHash(),transaction==null?"":transaction.next.hash(),states,detail,status.serverPrepareNanos(),status.startedNanos());}
    public synchronized void step(){if(closed)return;for(int i=0;i<4;i++){var job=jobs.poll();if(job==null)break;job.work().run();}long now=System.nanoTime();
        for(var c:clients.values())if(c.initial&&now>c.deadline)c.kick=true;
        var tx=transaction;if(tx==null){update(status.phase(),status.detail());return;}
        if(tx.phase!=Phase.RECOVERING&&now>tx.deadline)recover("Timeout preparando/ativando clientes");
        if(tx.phase==Phase.WAITING_CLIENTS&&clients.values().stream().allMatch(c->c.state.equals("READY"))){tx.readyAt=now;tx.phase=Phase.READY;tx.reserve.complete(ack(true,"Todos os clientes READY"));}
        if(tx.phase==Phase.VERIFYING&&clients.values().stream().allMatch(c->c.state.equals("ACTIVE"))&&tx.activation!=null){if(tx.verifiedAt==0)tx.verifiedAt=now;tx.activation.complete(ack(true,"Renderizadores confirmaram ACTIVE"));}
        if(tx.phase==Phase.RECOVERING){if(now>tx.deadline)clients.values().stream().filter(c->!c.state.equals("ACTIVE")).forEach(c->c.kick=true);
            if(clients.values().stream().allMatch(c->c.state.equals("ACTIVE"))){clients.values().forEach(c->control(c,Protocol.ContentKind.RESUME,tx.id,tx.before.content().hash()));measure(tx);transaction=null;world.freeze(false);var result=ack(false,tx.failure);tx.reserve.complete(result);if(tx.activation!=null)tx.activation.complete(result);if(tx.abort!=null)tx.abort.complete(ack(true,"Revisao anterior restaurada"));update(Phase.ABORTED,tx.failure);return;}}
        update(tx.phase,tx.failure.isEmpty()?tx.phase.toString():tx.failure);
    }
    public synchronized void shutdown(){closed=true;var error=new IllegalStateException("Servidor encerrou durante publicacao");for(Pending job;(job=jobs.poll())!=null;)job.result().completeExceptionally(error);var tx=transaction;if(tx!=null){tx.reserve.completeExceptionally(error);if(tx.activation!=null)tx.activation.completeExceptionally(error);if(tx.abort!=null)tx.abort.completeExceptionally(error);}}
}
