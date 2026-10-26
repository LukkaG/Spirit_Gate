package com.projetok.multiplayer;
import java.net.*;
import java.io.*;
import java.time.Duration;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
/** Cliente transitorio; somente recebe snapshots e envia intencoes limitadas. */
public final class LocalClient implements AutoCloseable {
    private com.projetok.studio.AssetCatalog.Result revisionAssets;
    private record TaggedSnapshot(String hash,Protocol.State state){}
    private volatile TaggedSnapshot taggedSnapshot;
    private long revisionTickFloor=-1;
    private ClientRevisionSync revisions;
    public ClientRevisionSync revisions(){return revisions;}
    private final NetworkDiagnostics diagnostics=new NetworkDiagnostics();
    public NetworkDiagnostics diagnostics(){return diagnostics;}
    private final InetAddress selectedSource;private final Socket socket;private final int id;private final AtomicBoolean closed=new AtomicBoolean();
    private final boolean cosmetics,combat,radmin;private volatile String region="forest";private volatile java.util.List<Protocol.QuestProgress> quests=java.util.List.of();private volatile java.util.List<Protocol.CombatEvent> combatEvents=java.util.List.of();private volatile java.util.Map<Integer,Protocol.Appearance> appearances=java.util.Map.of();
    private final InetAddress address;private final int port;private final String publicName;private final boolean lanAllowed;
    private final ArrayBlockingQueue<Protocol.Message> outgoing=new ArrayBlockingQueue<>(32);private final Object changed=new Object();
    private final Thread reader,writer;private long sequence,pingCounter;private volatile long pendingPing,pingStarted;private volatile double latencyMillis=Double.NaN;private volatile Protocol.State state;private volatile String problem="";
    public LocalClient(InetAddress address,int port,String name)throws IOException{
        this(address,port,name,false);
    }
    public static LocalClient lan(InetAddress address,int port,String name)throws IOException{return new LocalClient(address,port,name,true);}
    public static LocalClient cosmetics(InetAddress address,int port,String name,boolean lan)throws IOException{return new LocalClient(address,port,name,lan,true);}
    private LocalClient(InetAddress address,int port,String name,boolean lan)throws IOException{this(address,port,name,lan,false);}
    private LocalClient(InetAddress address,int port,String name,boolean lan,boolean cosmetics)throws IOException{this(address,port,name,lan,cosmetics,false);}
    public static LocalClient combat(InetAddress address,int port,String name,boolean lan)throws IOException{return new LocalClient(address,port,name,lan,true,true);}
    public boolean supportsCombat(){return combat;}
    public String region(){return region;}public java.util.List<Protocol.QuestProgress> quests(){return quests;}
    public java.util.List<Protocol.CombatEvent> combatEvents(){return combatEvents;}
    private LocalClient(InetAddress address,int port,String name,boolean lan,boolean cosmetics,boolean combat)throws IOException{this(address,port,name,lan,cosmetics,combat,false);}
    public static LocalClient radmin(InetAddress address,int port,String name,boolean combat,boolean cosmetics)throws IOException{return new LocalClient(address,port,name,false,cosmetics||combat,combat,true);}
    public static LocalClient radmin(InetAddress address,int port,String name,InetAddress source)throws IOException{return new LocalClient(address,port,name,false,true,true,true,source);}
    private LocalClient(InetAddress address,int port,String name,boolean lan,boolean cosmetics,boolean combat,boolean radmin)throws IOException{
        this(address,port,name,lan,cosmetics,combat,radmin,null);
    }
    public static LocalClient revision(InetAddress address,int port,String name,boolean lan,boolean radmin,InetAddress source,com.projetok.studio.AssetCatalog.Result assets)throws IOException{return new LocalClient(address,port,name,lan,true,true,radmin,source,assets);}
    private LocalClient(InetAddress address,int port,String name,boolean lan,boolean cosmetics,boolean combat,boolean radmin,InetAddress source)throws IOException{
        this(address,port,name,lan,cosmetics,combat,radmin,source,null);
    }
    private LocalClient(InetAddress address,int port,String name,boolean lan,boolean cosmetics,boolean combat,boolean radmin,InetAddress source,com.projetok.studio.AssetCatalog.Result contentAssets)throws IOException{
        this.revisionAssets=contentAssets;this.selectedSource=source;this.cosmetics=cosmetics;this.combat=combat;this.radmin=radmin;if(radmin&&!RadminAccess.address(address))throw new IllegalArgumentException("Destino Radmin exige IPv4 26.x.x.x");
        this.address=address;this.port=port;publicName=name;lanAllowed=lan;
        if(!radmin&&!address.isLoopbackAddress()&&(!lan||!address.isSiteLocalAddress()||address.isAnyLocalAddress()||address.isMulticastAddress()))throw new IllegalArgumentException("Endereco privado LAN exige opcao explicita");new Protocol.Hello(1,name);
        socket=new Socket();try{
            if(radmin)socket.bind(new InetSocketAddress(source==null?RadminAccess.clientSource():RadminAccess.requireLocal(source),0));
            socket.connect(new InetSocketAddress(address,port),2000);socket.setSoTimeout(3000);socket.setTcpNoDelay(true);Protocol.write(socket.getOutputStream(),new Protocol.Hello(contentAssets!=null?Protocol.CONTENT_APPLICATION:combat?Protocol.COMBAT_APPLICATION:cosmetics?Protocol.COSMETIC_APPLICATION:1,name));
            var reply=Protocol.read(socket.getInputStream());if(!(reply instanceof Protocol.Welcome welcome))throw new IOException("Conexao recusada: "+reply);id=welcome.id();
        }catch(IOException|RuntimeException error){try{socket.close();}catch(IOException ignored){}throw error;}
        if(contentAssets!=null)revisions=new ClientRevisionSync(contentAssets,m->{if(!enqueue(m))close();},()->{state=null;taggedSnapshot=null;});
        reader=new Thread(()->{try{while(!closed.get()){var message=Protocol.read(socket.getInputStream());String receivedRevision="";if(revisions!=null&&message instanceof Protocol.Content c){revisions.receive(c);continue;}if(revisions!=null){if(message instanceof Protocol.RevisionState r){if(!revisions.accepts(r.hash())||r.state().session().world().tick()<=revisionTickFloor)continue;receivedRevision=r.hash();message=r.state();}else if(!(message instanceof Protocol.Pong))throw new IOException("Snapshot sem revisao");}if(combat&&message instanceof Protocol.Pong pong){if(pong.nonce()==pendingPing){long elapsed=System.nanoTime()-pingStarted;latencyMillis=elapsed/1_000_000d;if(diagnostics.enabled)diagnostics.rtt.add(elapsed);pendingPing=0;}continue;}Protocol.State next;if(combat&&message instanceof Protocol.CombatState c){if(state==null||c.session().world().tick()>state.tick()){combatEvents=c.events();region=c.region();quests=c.quests();}message=c.session();}if(cosmetics&&message instanceof Protocol.SessionState extended){next=extended.world();if(state!=null&&next.tick()<=state.tick()){if(diagnostics.enabled)diagnostics.staleSnapshots++;continue;}var byId=new java.util.HashMap<Integer,Protocol.Appearance>();extended.appearances().forEach(a->byId.put(a.id(),a));appearances=java.util.Map.copyOf(byId);}else if(message instanceof Protocol.State plain){next=plain;}else throw new IOException("Resposta inesperada");if(state==null||next.tick()>state.tick()){diagnostics.received(System.nanoTime());state=next;if(revisions!=null){revisionTickFloor=Math.max(revisionTickFloor,next.tick());taggedSnapshot=new TaggedSnapshot(receivedRevision,next);}synchronized(changed){changed.notifyAll();}}}}catch(IOException error){if(!closed.get())problem=java.util.Objects.toString(error.getMessage(),"Conexao encerrada");}finally{close();appearances=java.util.Map.of();}},"coop-client-reader-"+id);
        writer=new Thread(()->{try{while(!closed.get()){var next=outgoing.take();long start=System.nanoTime();if(next instanceof Protocol.Ping ping&&ping.nonce()==pendingPing)pingStarted=start;diagnostics.write(socket.getOutputStream(),next);if(diagnostics.enabled){diagnostics.writes.add(System.nanoTime()-start);diagnostics.outboundQueue=outgoing.size();}}}catch(IOException|InterruptedException error){if(!closed.get())problem=java.util.Objects.toString(error.getMessage(),"Conexao encerrada");}finally{close();}},"coop-client-writer-"+id);
        reader.setDaemon(true);writer.setDaemon(true);reader.start();writer.start();
    }
    private boolean enqueue(Protocol.Message message){boolean accepted=outgoing.offer(message);if(diagnostics.enabled){diagnostics.outboundQueue=outgoing.size();if(!accepted)diagnostics.rejectedMessages++;}return accepted;}
    private boolean enqueueCommand(Protocol.Message command){if(revisions!=null){if(revisions.blocked())return false;return enqueue(new Protocol.RevisionCommand(revisions.hash(),command));}return enqueue(command);}
    public int id(){return id;}public Protocol.State snapshot(){if(revisions==null)return state;var tagged=taggedSnapshot;return tagged!=null&&revisions.accepts(tagged.hash())?tagged.state():null;}public boolean connected(){return !closed.get();}public String problem(){return problem;}
    /** Reconexão explícita cria nova sessão efêmera; chamar em worker, nunca na EDT. */
    public LocalClient reconnect()throws IOException{if(connected())throw new IllegalStateException("Encerre a conexão anterior antes de reconectar");return new LocalClient(address,port,publicName,lanAllowed,cosmetics,combat,radmin,selectedSource,revisionAssets);}
    public boolean supportsCosmetics(){return cosmetics;}
    public java.util.Map<Integer,Protocol.Appearance> appearances(){return appearances;}
    public synchronized boolean appearance(String hair,String clothing){if(closed.get()||!cosmetics)return false;return enqueueCommand(new Protocol.AppearanceRequest(++sequence,hair,clothing));}
    public double latencyMillis(){return latencyMillis;}
    public synchronized boolean measureLatency(){if(closed.get()||!combat||pendingPing!=0&&System.nanoTime()-pingStarted<5_000_000_000L)return false;long nonce=++pingCounter;pendingPing=nonce;pingStarted=System.nanoTime();if(!enqueue(new Protocol.Ping(nonce))){pendingPing=0;return false;}return true;}
    public synchronized boolean request(Protocol.WorldAction action,String target){if(closed.get()||!combat||revisions!=null&&revisions.blocked())return false;return enqueueCommand(new Protocol.WorldRequest(++sequence,action,target));}
    public synchronized boolean send(Protocol.Action action,float x,float y){if(closed.get()||revisions!=null&&revisions.blocked())return false;return enqueueCommand(new Protocol.Intent(++sequence,action,x,y));}
    public Protocol.State await(java.util.function.Predicate<Protocol.State> predicate,Duration timeout)throws InterruptedException,TimeoutException{
        long deadline=System.nanoTime()+timeout.toNanos();synchronized(changed){while(true){var visible=snapshot();if(visible!=null&&predicate.test(visible))return visible;long left=deadline-System.nanoTime();if(left<=0||closed.get())throw new TimeoutException("Snapshot esperado ausente: "+problem);TimeUnit.NANOSECONDS.timedWait(changed,left);}}
    }
    public void close(){if(!closed.compareAndSet(false,true))return;if(revisions!=null)revisions.close();appearances=java.util.Map.of();try{socket.close();}catch(IOException ignored){}reader.interrupt();writer.interrupt();synchronized(changed){changed.notifyAll();}}
}
