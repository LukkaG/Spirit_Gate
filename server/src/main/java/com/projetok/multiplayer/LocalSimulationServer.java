package com.projetok.multiplayer;
import java.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
/** TCP local, duas sessoes, nenhuma operacao de socket na thread proprietaria do mundo. */
public final class LocalSimulationServer implements AutoCloseable {
    private final boolean radmin;private final InetAddress bind;private final int requestedPort;private ServerSocket listener;private Thread acceptThread;
    private final AtomicBoolean running=new AtomicBoolean();private final AtomicInteger ids=new AtomicInteger();private final Semaphore capacity=new Semaphore(2);
    private final ConcurrentMap<Integer,Peer> peers=new ConcurrentHashMap<>();private final Set<Integer> departures=ConcurrentHashMap.newKeySet();
    private final ConcurrentMap<Integer,Peer> connections=new ConcurrentHashMap<>();private boolean started;
    private final ArrayBlockingQueue<Runnable> commands=new ArrayBlockingQueue<>(64);private final SessionSimulation world;
    private final ScheduledExecutorService owner=Executors.newSingleThreadScheduledExecutor(r->{Thread t=new Thread(r,"coop-world");t.setDaemon(true);return t;});
    private final NetworkDiagnostics diagnostics=new NetworkDiagnostics();
    private long lastTickStart;
    private final int snapshotTicks=snapshotTicks();
    private static int snapshotTicks(){int hz=Integer.getInteger("spiritgate.network.snapshotHz",10);if(hz!=10&&hz!=15)throw new IllegalArgumentException("snapshotHz: 10 or 15");return 30/hz;}
    public NetworkDiagnostics diagnostics(){return diagnostics;}
    private volatile Protocol.State state;private volatile Throwable failure;
    public LocalSimulationServer(InetAddress bind,int port){this(bind,port,false,new CoopWorld());}
    public LocalSimulationServer(InetAddress bind,int port,SessionSimulation simulation){this(bind,port,false,simulation);}
    private LocalSimulationServer(InetAddress bind,int port,boolean lan,SessionSimulation simulation){this(bind,port,lan,simulation,false);}
    private LocalSimulationServer(InetAddress bind,int port,boolean lan,SessionSimulation simulation,boolean radmin){this.radmin=radmin;if(radmin)RadminAccess.requireLocal(bind);if((!radmin&&!bind.isLoopbackAddress()&&(!lan||!bind.isSiteLocalAddress()||bind.isAnyLocalAddress()||bind.isMulticastAddress()))||port<0||port>65535)throw new IllegalArgumentException("Use loopback ou endereco LAN privado explicitamente autorizado");this.bind=bind;requestedPort=port;world=Objects.requireNonNull(simulation);state=world.snapshot();}
    public static LocalSimulationServer radmin(InetAddress address,int port,SessionSimulation simulation){return new LocalSimulationServer(address,port,false,simulation,true);}
    public static LocalSimulationServer lan(InetAddress address,int port){return new LocalSimulationServer(address,port,true,new CoopWorld());}
    public synchronized void start()throws IOException{
        if(started||!running.compareAndSet(false,true))throw new IllegalStateException("Servidor ja iniciado/encerrado");started=true;
        try{listener=new ServerSocket();listener.bind(new InetSocketAddress(bind,requestedPort));}catch(IOException error){running.set(false);if(listener!=null)try{listener.close();}catch(IOException ignored){}owner.shutdownNow();throw error;}
        owner.scheduleAtFixedRate(()->{long tickStart=System.nanoTime();if(diagnostics.enabled){if(lastTickStart!=0)diagnostics.tickIntervals.add(tickStart-lastTickStart);lastTickStart=tickStart;diagnostics.commandQueue=commands.size();}try{
            for(int id:departures)if(departures.remove(id))world.leave(id);
            for(int i=0;i<64;i++){var command=commands.poll();if(command==null)break;command.run();}
            if(diagnostics.enabled){diagnostics.outboundQueue=peers.values().stream().mapToInt(p->p.outgoing.size()).sum();diagnostics.controlQueue=peers.values().stream().mapToInt(p->p.controls.size()).sum();}
            world.step();state=world.snapshot();for(Peer peer:peers.values()){if(world.disconnectRequested(peer.id)){peer.closePeer();continue;}if(peer.content)for(int i=0;i<4&&peer.controls.remainingCapacity()>1;i++){var message=world.nextContent(peer.id);if(message==null)break;peer.controls.offer(message);peer.signal();}}if(diagnostics.enabled&&state.tick()%300==0)System.out.println(diagnostics.json());if(state.tick()%snapshotTicks==0)for(Peer peer:peers.values()){if(peer.outgoing.poll()!=null&&diagnostics.enabled)diagnostics.replacedSnapshots++;peer.outgoing.offer(peer.content?new Protocol.RevisionState(world.contentHash(),world.combatSnapshotFor(peer.id)):peer.combat?world.combatSnapshotFor(peer.id):peer.cosmetics?new Protocol.SessionState(world.snapshotFor(peer.id),world.appearancesFor(peer.id)):world.snapshotFor(peer.id));peer.signal();}
        }catch(RuntimeException error){failure=error;close();}finally{if(diagnostics.enabled)diagnostics.ticks.add(System.nanoTime()-tickStart);}},0,33_333_333,TimeUnit.NANOSECONDS);
        acceptThread=new Thread(()->{while(running.get())try{Socket socket=listener.accept();if(radmin&&!RadminAccess.address(socket.getInetAddress())){socket.close();continue;}if(!capacity.tryAcquire()){socket.close();continue;}int id=ids.incrementAndGet();if(id>1_000_000){socket.close();capacity.release();continue;}Peer peer=new Peer(id,socket);connections.put(id,peer);peer.start();}catch(IOException error){if(running.get())failure=error;}},"coop-accept");acceptThread.setDaemon(true);acceptThread.start();
    }
    public int port(){if(listener==null)throw new IllegalStateException("Nao iniciado");return listener.getLocalPort();}
    public Protocol.State snapshot(){return state;}public Throwable failure(){return failure;}public boolean running(){return running.get();}
    public int pendingCommands(){return commands.size();}public int connections(){return connections.size();}
    public int players(){return peers.size();}
    private final class Peer {
        final Semaphore ready=new Semaphore(0);
        synchronized void signal(){if(ready.availablePermits()==0)ready.release();}
        final ArrayBlockingQueue<Protocol.Message> controls=new ArrayBlockingQueue<>(4);
        final int id;final Socket socket;final ArrayBlockingQueue<Protocol.Message> outgoing=new ArrayBlockingQueue<>(1);final AtomicBoolean closed=new AtomicBoolean();
        Thread reader,writer;boolean cosmetics,combat,content;
        Peer(int id,Socket socket){this.id=id;this.socket=socket;}
        void start(){if(!running.get()||closed.get()){closePeer();return;}reader=new Thread(this::read,"coop-reader-"+id);reader.setDaemon(true);reader.start();}
        void read(){try{
            socket.setSoTimeout(2000);socket.setTcpNoDelay(true);
            var hello=Protocol.read(socket.getInputStream());if(!(hello instanceof Protocol.Hello h)||(h.version()!=Protocol.VERSION&&(h.version()!=Protocol.COSMETIC_APPLICATION||!world.supportsCosmetics())&&(h.version()!=Protocol.COMBAT_APPLICATION||!world.supportsCombat())&&(h.version()!=Protocol.CONTENT_APPLICATION||!world.supportsContent()))){Protocol.write(socket.getOutputStream(),new Protocol.Error("Protocolo incompativel"));return;}
            content=h.version()==Protocol.CONTENT_APPLICATION;combat=h.version()==Protocol.COMBAT_APPLICATION||content;cosmetics=h.version()==Protocol.COSMETIC_APPLICATION||combat&&world.supportsCosmetics();var admitted=new CompletableFuture<Boolean>();if(!commands.offer(()->admitted.complete(!closed.get()&&running.get()&&world.join(id,h.name(),h.version()))))return;
            if(!admitted.get(2,TimeUnit.SECONDS)){Protocol.write(socket.getOutputStream(),new Protocol.Error(world.admissionFailure()));return;}
            Protocol.write(socket.getOutputStream(),new Protocol.Welcome(id));peers.put(id,this);socket.setSoTimeout(5000);
            writer=new Thread(()->{try{while(!closed.get()){ready.acquire();ready.drainPermits();var next=controls.poll();if(next==null)next=outgoing.poll();if(next!=null){long start=System.nanoTime();diagnostics.write(socket.getOutputStream(),next);if(diagnostics.enabled)diagnostics.writes.add(System.nanoTime()-start);}if(!controls.isEmpty()||!outgoing.isEmpty())signal();}}catch(IOException|InterruptedException ignored){}finally{closePeer();}},"coop-writer-"+id);writer.setDaemon(true);writer.start();
            long window=System.nanoTime(),deadline=window+TimeUnit.MINUTES.toNanos(30);int count=0;
            while(running.get()&&!closed.get()){
                var message=Protocol.read(socket.getInputStream());long now=System.nanoTime();if(now>deadline)break;if(now-window>1_000_000_000L){window=now;count=0;}if(++count>120)break;
                if(message instanceof Protocol.Bye)break;
                if(content&&message instanceof Protocol.Content c){if(!commands.offer(()->world.contentAck(id,c)))break;continue;}
                if(combat&&message instanceof Protocol.Ping ping){if(!controls.offer(new Protocol.Pong(ping.nonce())))break;signal();continue;}
                if(content){if(!(message instanceof Protocol.RevisionCommand revision))break;message=revision.command();String hash=revision.hash();var command=message;if(!commands.offer(()->{if(!hash.equals(world.contentHash()))return;if(command instanceof Protocol.Intent intent)world.accept(id,intent);else if(command instanceof Protocol.WorldRequest request)world.request(id,request);else if(command instanceof Protocol.AppearanceRequest request)world.appearance(id,request);}))break;continue;}
                if(message instanceof Protocol.Intent intent){if(world.supportsCombat()&&!combat&&intent.action()!=Protocol.Action.MOVE)break;if(!commands.offer(()->world.accept(id,intent)))break;}else if(combat&&message instanceof Protocol.WorldRequest request){if(!commands.offer(()->world.request(id,request)))break;}else if(cosmetics&&message instanceof Protocol.AppearanceRequest request){if(!commands.offer(()->world.appearance(id,request)))break;}else break;
            }
        }catch(IOException|InterruptedException|ExecutionException|TimeoutException ignored){if(diagnostics.enabled&&!closed.get()&&running.get())diagnostics.transportFailures.incrementAndGet();}finally{closePeer();}}
        void closePeer(){if(!closed.compareAndSet(false,true))return;peers.remove(id,this);connections.remove(id,this);departures.add(id);try{socket.close();}catch(IOException ignored){}if(writer!=null)writer.interrupt();if(reader!=null&&reader!=Thread.currentThread())reader.interrupt();capacity.release();}
    }
    public synchronized void close(){started=true;world.closed();if(!running.getAndSet(false)){owner.shutdownNow();return;}try{listener.close();}catch(IOException ignored){}for(Peer peer:connections.values())peer.closePeer();owner.shutdownNow();commands.clear();if(acceptThread!=null)acceptThread.interrupt();}
    public static void main(String[] args)throws Exception{
        int port=54557,runSeconds=0,syntheticLevel=1;InetAddress address=InetAddress.getByName("127.0.0.1");boolean lan=false,rpg=false,cosmetics=false,combat=false,radmin=false,noRewards=false;
        for(int i=0;i<args.length;i++)switch(args[i]){case "--synthetic-level"->{if(++i>=args.length)throw new IllegalArgumentException("Nivel ausente");syntheticLevel=Integer.parseInt(args[i]);}case "--port"->{if(++i>=args.length)throw new IllegalArgumentException("Porta ausente");port=Integer.parseInt(args[i]);}case "--bind"->{if(++i>=args.length)throw new IllegalArgumentException("Endereco ausente");address=InetAddress.getByName(args[i]);}case "--allow-lan"->lan=true;case "--radmin-bind"->{if(++i>=args.length)throw new IllegalArgumentException("IPv4 Radmin ausente");address=InetAddress.getByName(args[i]);radmin=true;}case "--no-synthetic-rewards"->noRewards=true;case "--rpg"->rpg=true;case "--combat"->{rpg=true;combat=true;}case "--cosmetics"->cosmetics=true;case "--run-seconds"->{if(++i>=args.length)throw new IllegalArgumentException("Duracao ausente");runSeconds=Integer.parseInt(args[i]);if(runSeconds<1||runSeconds>60)throw new IllegalArgumentException("Duracao de smoke: 1..60 segundos");}default->throw new IllegalArgumentException("Uso: [--port N] [--bind IP-PRIVADO --allow-lan] [--run-seconds 1..60]");}
        if(radmin&&lan)throw new IllegalArgumentException("Escolha Radmin ou LAN privada");if(cosmetics&&!rpg)throw new IllegalArgumentException("Cosmeticos exigem backend RPG");var catalog=cosmetics?com.projetok.prototype.rpg.CosmeticCatalog.metadata((System.getProperty("jpackage.app-path")==null?java.nio.file.Path.of("."):java.nio.file.Path.of(System.getProperty("jpackage.app-path")).toAbsolutePath().getParent()).resolve("assets/dev/generated/corujao-iv-v1")):null;var server=new LocalSimulationServer(address,port,lan,rpg?new RpgSessionWorld(java.util.List.of("reaper","human"),catalog,combat,syntheticLevel,noRewards?com.projetok.rpg.SharedRpgEncounter.RewardPolicy.DISABLED:com.projetok.rpg.SharedRpgEncounter.RewardPolicy.SYNTHETIC_PARTICIPANTS):new CoopWorld(),radmin);server.start();Runtime.getRuntime().addShutdownHook(new Thread(server::close));System.out.println("Spirit Gate experimental em "+address.getHostAddress()+":"+server.port()+"; sem autenticacao de contas");if(runSeconds==0)new CountDownLatch(1).await();else{new CountDownLatch(1).await(runSeconds,TimeUnit.SECONDS);server.close();}
    }
}
