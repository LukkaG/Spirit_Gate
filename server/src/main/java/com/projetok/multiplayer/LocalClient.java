package com.projetok.multiplayer;
import java.net.*;
import java.io.*;
import java.time.Duration;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
/** Cliente transitorio; somente recebe snapshots e envia intencoes limitadas. */
public final class LocalClient implements AutoCloseable {
    private final Socket socket;private final int id;private final AtomicBoolean closed=new AtomicBoolean();
    private final boolean cosmetics;private volatile java.util.Map<Integer,Protocol.Appearance> appearances=java.util.Map.of();
    private final InetAddress address;private final int port;private final String publicName;private final boolean lanAllowed;
    private final ArrayBlockingQueue<Protocol.Message> outgoing=new ArrayBlockingQueue<>(32);private final Object changed=new Object();
    private final Thread reader,writer;private long sequence;private volatile Protocol.State state;private volatile String problem="";
    public LocalClient(InetAddress address,int port,String name)throws IOException{
        this(address,port,name,false);
    }
    public static LocalClient lan(InetAddress address,int port,String name)throws IOException{return new LocalClient(address,port,name,true);}
    public static LocalClient cosmetics(InetAddress address,int port,String name,boolean lan)throws IOException{return new LocalClient(address,port,name,lan,true);}
    private LocalClient(InetAddress address,int port,String name,boolean lan)throws IOException{this(address,port,name,lan,false);}
    private LocalClient(InetAddress address,int port,String name,boolean lan,boolean cosmetics)throws IOException{
        this.cosmetics=cosmetics;
        this.address=address;this.port=port;publicName=name;lanAllowed=lan;
        if(!address.isLoopbackAddress()&&(!lan||!address.isSiteLocalAddress()||address.isAnyLocalAddress()||address.isMulticastAddress()))throw new IllegalArgumentException("Endereco privado LAN exige opcao explicita");new Protocol.Hello(1,name);
        socket=new Socket();try{
            socket.connect(new InetSocketAddress(address,port),2000);socket.setSoTimeout(3000);socket.setTcpNoDelay(true);Protocol.write(socket.getOutputStream(),new Protocol.Hello(cosmetics?Protocol.COSMETIC_APPLICATION:1,name));
            var reply=Protocol.read(socket.getInputStream());if(!(reply instanceof Protocol.Welcome welcome))throw new IOException("Conexao recusada: "+reply);id=welcome.id();
        }catch(IOException|RuntimeException error){try{socket.close();}catch(IOException ignored){}throw error;}
        reader=new Thread(()->{try{while(!closed.get()){var message=Protocol.read(socket.getInputStream());Protocol.State next;if(cosmetics&&message instanceof Protocol.SessionState extended){next=extended.world();var byId=new java.util.HashMap<Integer,Protocol.Appearance>();extended.appearances().forEach(a->byId.put(a.id(),a));appearances=java.util.Map.copyOf(byId);}else if(message instanceof Protocol.State plain){next=plain;}else throw new IOException("Resposta inesperada");if(state==null||next.tick()>state.tick()){state=next;synchronized(changed){changed.notifyAll();}}}}catch(IOException error){if(!closed.get())problem=java.util.Objects.toString(error.getMessage(),"Conexao encerrada");}finally{close();appearances=java.util.Map.of();}},"coop-client-reader-"+id);
        writer=new Thread(()->{try{while(!closed.get())Protocol.write(socket.getOutputStream(),outgoing.take());}catch(IOException|InterruptedException error){if(!closed.get())problem=java.util.Objects.toString(error.getMessage(),"Conexao encerrada");}finally{close();}},"coop-client-writer-"+id);
        reader.setDaemon(true);writer.setDaemon(true);reader.start();writer.start();
    }
    public int id(){return id;}public Protocol.State snapshot(){return state;}public boolean connected(){return !closed.get();}public String problem(){return problem;}
    /** Reconexão explícita cria nova sessão efêmera; chamar em worker, nunca na EDT. */
    public LocalClient reconnect()throws IOException{if(connected())throw new IllegalStateException("Encerre a conexão anterior antes de reconectar");return new LocalClient(address,port,publicName,lanAllowed,cosmetics);}
    public boolean supportsCosmetics(){return cosmetics;}
    public java.util.Map<Integer,Protocol.Appearance> appearances(){return appearances;}
    public synchronized boolean appearance(String hair,String clothing){if(closed.get()||!cosmetics)return false;return outgoing.offer(new Protocol.AppearanceRequest(++sequence,hair,clothing));}
    public synchronized boolean send(Protocol.Action action,float x,float y){if(closed.get())return false;return outgoing.offer(new Protocol.Intent(++sequence,action,x,y));}
    public Protocol.State await(java.util.function.Predicate<Protocol.State> predicate,Duration timeout)throws InterruptedException,TimeoutException{
        long deadline=System.nanoTime()+timeout.toNanos();synchronized(changed){while(state==null||!predicate.test(state)){long left=deadline-System.nanoTime();if(left<=0||closed.get())throw new TimeoutException("Snapshot esperado ausente: "+problem);TimeUnit.NANOSECONDS.timedWait(changed,left);}return state;}
    }
    public void close(){if(!closed.compareAndSet(false,true))return;appearances=java.util.Map.of();try{socket.close();}catch(IOException ignored){}reader.interrupt();writer.interrupt();synchronized(changed){changed.notifyAll();}}
}
