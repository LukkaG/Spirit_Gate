package com.projetok.multiplayer;
import java.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
/** TCP local, duas sessoes, nenhuma operacao de socket na thread proprietaria do mundo. */
public final class LocalSimulationServer implements AutoCloseable {
    private final InetAddress bind;private final int requestedPort;private ServerSocket listener;private Thread acceptThread;
    private final AtomicBoolean running=new AtomicBoolean();private final AtomicInteger ids=new AtomicInteger();private final Semaphore capacity=new Semaphore(2);
    private final ConcurrentMap<Integer,Peer> peers=new ConcurrentHashMap<>();private final Set<Integer> departures=ConcurrentHashMap.newKeySet();
    private final ConcurrentMap<Integer,Peer> connections=new ConcurrentHashMap<>();private boolean started;
    private final ArrayBlockingQueue<Runnable> commands=new ArrayBlockingQueue<>(64);private final SessionSimulation world;
    private final ScheduledExecutorService owner=Executors.newSingleThreadScheduledExecutor(r->{Thread t=new Thread(r,"coop-world");t.setDaemon(true);return t;});
    private volatile Protocol.State state;private volatile Throwable failure;
    public LocalSimulationServer(InetAddress bind,int port){this(bind,port,false,new CoopWorld());}
    public LocalSimulationServer(InetAddress bind,int port,SessionSimulation simulation){this(bind,port,false,simulation);}
    private LocalSimulationServer(InetAddress bind,int port,boolean lan,SessionSimulation simulation){if((!bind.isLoopbackAddress()&&(!lan||!bind.isSiteLocalAddress()||bind.isAnyLocalAddress()||bind.isMulticastAddress()))||port<0||port>65535)throw new IllegalArgumentException("Use loopback ou endereco LAN privado explicitamente autorizado");this.bind=bind;requestedPort=port;world=Objects.requireNonNull(simulation);state=world.snapshot();}
    public static LocalSimulationServer lan(InetAddress address,int port){return new LocalSimulationServer(address,port,true,new CoopWorld());}
    public synchronized void start()throws IOException{
        if(started||!running.compareAndSet(false,true))throw new IllegalStateException("Servidor ja iniciado/encerrado");started=true;
        try{listener=new ServerSocket();listener.bind(new InetSocketAddress(bind,requestedPort));}catch(IOException error){running.set(false);if(listener!=null)try{listener.close();}catch(IOException ignored){}owner.shutdownNow();throw error;}
        owner.scheduleAtFixedRate(()->{try{
            for(int id:departures)if(departures.remove(id))world.leave(id);
            for(int i=0;i<64;i++){var command=commands.poll();if(command==null)break;command.run();}
            world.step();state=world.snapshot();if(state.tick()%3==0)for(Peer peer:peers.values()){peer.outgoing.poll();peer.outgoing.offer(peer.cosmetics?new Protocol.SessionState(state,world.appearances()):state);}
        }catch(RuntimeException error){failure=error;close();}},0,33_333_333,TimeUnit.NANOSECONDS);
        acceptThread=new Thread(()->{while(running.get())try{Socket socket=listener.accept();if(!capacity.tryAcquire()){socket.close();continue;}int id=ids.incrementAndGet();if(id>1_000_000){socket.close();capacity.release();continue;}Peer peer=new Peer(id,socket);connections.put(id,peer);peer.start();}catch(IOException error){if(running.get())failure=error;}},"coop-accept");acceptThread.setDaemon(true);acceptThread.start();
    }
    public int port(){if(listener==null)throw new IllegalStateException("Nao iniciado");return listener.getLocalPort();}
    public Protocol.State snapshot(){return state;}public Throwable failure(){return failure;}public boolean running(){return running.get();}
    public int pendingCommands(){return commands.size();}public int connections(){return connections.size();}
    private final class Peer {
        final int id;final Socket socket;final ArrayBlockingQueue<Protocol.Message> outgoing=new ArrayBlockingQueue<>(1);final AtomicBoolean closed=new AtomicBoolean();
        Thread reader,writer;boolean cosmetics;
        Peer(int id,Socket socket){this.id=id;this.socket=socket;}
        void start(){if(!running.get()||closed.get()){closePeer();return;}reader=new Thread(this::read,"coop-reader-"+id);reader.setDaemon(true);reader.start();}
        void read(){try{
            socket.setSoTimeout(2000);socket.setTcpNoDelay(true);
            var hello=Protocol.read(socket.getInputStream());if(!(hello instanceof Protocol.Hello h)||(h.version()!=Protocol.VERSION&&(h.version()!=Protocol.COSMETIC_APPLICATION||!world.supportsCosmetics()))){Protocol.write(socket.getOutputStream(),new Protocol.Error("Protocolo incompativel"));return;}
            cosmetics=h.version()==Protocol.COSMETIC_APPLICATION;var admitted=new CompletableFuture<Boolean>();if(!commands.offer(()->admitted.complete(!closed.get()&&running.get()&&world.join(id,h.name()))))return;
            if(!admitted.get(2,TimeUnit.SECONDS)){Protocol.write(socket.getOutputStream(),new Protocol.Error("Servidor cheio"));return;}
            Protocol.write(socket.getOutputStream(),new Protocol.Welcome(id));peers.put(id,this);socket.setSoTimeout(5000);
            writer=new Thread(()->{try{while(!closed.get())Protocol.write(socket.getOutputStream(),outgoing.take());}catch(IOException|InterruptedException ignored){}finally{closePeer();}},"coop-writer-"+id);writer.setDaemon(true);writer.start();
            long window=System.nanoTime(),deadline=window+TimeUnit.MINUTES.toNanos(30);int count=0;
            while(running.get()&&!closed.get()){
                var message=Protocol.read(socket.getInputStream());long now=System.nanoTime();if(now>deadline)break;if(now-window>1_000_000_000L){window=now;count=0;}if(++count>120)break;
                if(message instanceof Protocol.Bye)break;
                if(message instanceof Protocol.Intent intent){if(!commands.offer(()->world.accept(id,intent)))break;}else if(cosmetics&&message instanceof Protocol.AppearanceRequest request){if(!commands.offer(()->world.appearance(id,request)))break;}else break;
            }
        }catch(IOException|InterruptedException|ExecutionException|TimeoutException ignored){}finally{closePeer();}}
        void closePeer(){if(!closed.compareAndSet(false,true))return;peers.remove(id,this);connections.remove(id,this);departures.add(id);try{socket.close();}catch(IOException ignored){}if(writer!=null)writer.interrupt();if(reader!=null&&reader!=Thread.currentThread())reader.interrupt();capacity.release();}
    }
    public synchronized void close(){started=true;if(!running.getAndSet(false)){owner.shutdownNow();return;}try{listener.close();}catch(IOException ignored){}for(Peer peer:connections.values())peer.closePeer();owner.shutdownNow();commands.clear();if(acceptThread!=null)acceptThread.interrupt();}
    public static void main(String[] args)throws Exception{
        int port=54557,runSeconds=0;InetAddress address=InetAddress.getByName("127.0.0.1");boolean lan=false,rpg=false,cosmetics=false;
        for(int i=0;i<args.length;i++)switch(args[i]){case "--port"->{if(++i>=args.length)throw new IllegalArgumentException("Porta ausente");port=Integer.parseInt(args[i]);}case "--bind"->{if(++i>=args.length)throw new IllegalArgumentException("Endereco ausente");address=InetAddress.getByName(args[i]);}case "--allow-lan"->lan=true;case "--rpg"->rpg=true;case "--cosmetics"->cosmetics=true;case "--run-seconds"->{if(++i>=args.length)throw new IllegalArgumentException("Duracao ausente");runSeconds=Integer.parseInt(args[i]);if(runSeconds<1||runSeconds>60)throw new IllegalArgumentException("Duracao de smoke: 1..60 segundos");}default->throw new IllegalArgumentException("Uso: [--port N] [--bind IP-PRIVADO --allow-lan] [--run-seconds 1..60]");}
        if(cosmetics&&!rpg)throw new IllegalArgumentException("Cosmeticos exigem backend RPG");var catalog=cosmetics?com.projetok.prototype.rpg.CosmeticCatalog.metadata((System.getProperty("jpackage.app-path")==null?java.nio.file.Path.of("."):java.nio.file.Path.of(System.getProperty("jpackage.app-path")).toAbsolutePath().getParent()).resolve("assets/dev/generated/corujao-iv-v1")):null;var server=new LocalSimulationServer(address,port,lan,rpg?new RpgSessionWorld(java.util.List.of("reaper","human"),catalog):new CoopWorld());server.start();Runtime.getRuntime().addShutdownHook(new Thread(server::close));System.out.println("Spirit Gate experimental em "+address.getHostAddress()+":"+server.port()+"; sem autenticacao de contas");if(runSeconds==0)new CountDownLatch(1).await();else{new CountDownLatch(1).await(runSeconds,TimeUnit.SECONDS);server.close();}
    }
}
