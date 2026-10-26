package com.projetok.multiplayer;

import com.projetok.studio.*;
import java.io.*;
import java.util.concurrent.*;
import java.util.function.*;

/** One bounded transfer/preparation worker per client. No received filesystem path. */
public final class ClientRevisionSync implements AutoCloseable {
    private final AssetCatalog.Result assets;
    private final Consumer<Protocol.Message> send;
    private final Runnable clearSnapshots;
    private final ExecutorService worker=Executors.newSingleThreadExecutor(r->{var t=new Thread(r,"content-client-prepare");t.setDaemon(true);return t;});
    private ByteArrayOutputStream incoming;private boolean preparing;private int expected;private long tx,lastTx;private String offered="";
    private volatile WorldContent.Prepared active,previous,candidate;
    private volatile boolean blocked=true,closed;
    private volatile String problem="",hash="";
    private volatile Function<WorldContent.Prepared,CompletableFuture<Void>> renderer;
    private Protocol.Content pendingActivation;
    private volatile long prepareNanos;
    public ClientRevisionSync(AssetCatalog.Result assets,Consumer<Protocol.Message> send,Runnable clearSnapshots){this.assets=assets;this.send=send;this.clearSnapshots=clearSnapshots;}
    public WorldContent.Prepared active(){return active;}
    public String hash(){return hash;}
    public boolean blocked(){return blocked;}
    public String problem(){return problem;}
    public long prepareNanos(){return prepareNanos;}
    public boolean accepts(String hash){return !blocked&&this.hash.equals(hash);}
    public synchronized void renderer(Function<WorldContent.Prepared,CompletableFuture<Void>> renderer){this.renderer=renderer;if(pendingActivation!=null){var message=pendingActivation;pendingActivation=null;activate(message,message.kind()==Protocol.ContentKind.ABORT);}}
    private void reply(Protocol.ContentKind kind,long transaction,String hash,String reason){byte[] bytes=reason.getBytes(java.nio.charset.StandardCharsets.UTF_8);if(bytes.length>240)bytes=java.util.Arrays.copyOf(bytes,240);send.accept(new Protocol.Content(kind,transaction,hash,0,bytes));}
    public synchronized void receive(Protocol.Content m)throws IOException{
        if(closed)return;
        switch(m.kind()){
            case BEGIN->{if(m.transaction()<=lastTx||incoming!=null||preparing||m.index()<1||m.index()>WorldContent.LIMIT||!new String(m.data(),java.nio.charset.StandardCharsets.UTF_8).equals(hash))throw new IOException("Manifesto fora de ordem/base divergente/excessivo");lastTx=tx=m.transaction();lastTx=Math.max(lastTx,tx);offered=m.hash();expected=m.index();preparing=true;incoming=new ByteArrayOutputStream(Math.min(expected,65536));candidate=null;problem="Preparando revisao";}
            case CHUNK->{if(incoming==null||m.transaction()!=tx||!m.hash().equals(offered)||m.index()!=incoming.size()||incoming.size()+m.data().length>expected)throw new IOException("Bloco truncado/fora de ordem");incoming.writeBytes(m.data());}
            case END->{if(incoming==null||m.transaction()!=tx||!m.hash().equals(offered)||incoming.size()!=expected)throw new IOException("Transferencia incompleta");byte[] bytes=incoming.toByteArray();incoming=null;long id=tx;String hash=offered;
                worker.execute(()->{long started=System.nanoTime();try{var prepared=WorldContent.prepare(bytes,assets);synchronized(this){if(closed||id!=tx||!hash.equals(offered))return;preparing=false;if(!prepared.revision().hash().equals(hash))throw new IOException("Hash divergente");candidate=prepared;prepareNanos=System.nanoTime()-started;problem="READY";reply(Protocol.ContentKind.READY,id,hash,"");}}catch(Exception error){synchronized(this){if(id==tx&&!closed){preparing=false;candidate=null;problem=error.getMessage();reply(Protocol.ContentKind.REJECT,id,hash,problem);}}}});}
            case ACTIVATE->{if(m.transaction()!=tx||!m.hash().equals(offered)||candidate==null)return;blocked=true;activate(m,false);}
            case ABORT->{if(m.transaction()<tx)return;tx=m.transaction();lastTx=Math.max(lastTx,tx);offered=m.hash();preparing=false;incoming=null;candidate=null;blocked=true;activate(m,true);}
            case RESUME->{if(m.transaction()==tx&&m.hash().equals(hash)){blocked=false;previous=null;candidate=null;problem="ACTIVE";}}
            default->throw new IOException("Mensagem de servidor nao permitida");
        }
    }
    private synchronized void activate(Protocol.Content message,boolean rollback){
        WorldContent.Prepared next=rollback?(previous!=null&&previous.revision().hash().equals(message.hash())?previous:active):candidate;
        if(next==null||!next.revision().hash().equals(message.hash())){reply(Protocol.ContentKind.REJECT,message.transaction(),message.hash(),"Cena anterior/candidata indisponivel");return;}
        var install=renderer;if(install==null){pendingActivation=message;return;}
        long id=message.transaction();install.apply(next).whenComplete((v,error)->{synchronized(this){if(closed||id!=tx)return;if(error!=null){problem="Falha de renderer";reply(Protocol.ContentKind.REJECT,id,message.hash(),problem);return;}if(!rollback)previous=active;active=next;hash=next.revision().hash();clearSnapshots.run();problem="ACTIVE; aguardando barreira";reply(Protocol.ContentKind.ACTIVE,id,hash,"");}});
    }
    @Override public synchronized void close(){closed=true;incoming=null;candidate=null;active=null;previous=null;worker.shutdownNow();}
}
