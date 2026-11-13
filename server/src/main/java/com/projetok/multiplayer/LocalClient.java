package com.projetok.multiplayer;
import java.net.*;
import java.io.*;
import java.time.Duration;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
/** Cliente transitorio; somente recebe snapshots e envia intencoes limitadas. */
public final class LocalClient implements AutoCloseable {
    private com.projetok.studio.AssetCatalog.Result revisionAssets;
    private volatile Runnable snapshotListener;
    /** O sinal deve apenas agendar trabalho; nunca executar UI na thread de rede. */
    public void onSnapshot(Runnable listener){snapshotListener=listener;}
    private record TaggedSnapshot(String hash,Protocol.State state){}
    private volatile TaggedSnapshot taggedSnapshot;
    private long revisionTickFloor=-1;
    private ClientRevisionSync revisions;
    public ClientRevisionSync revisions(){return revisions;}
    private final NetworkDiagnostics diagnostics=new NetworkDiagnostics();
    public NetworkDiagnostics diagnostics(){return diagnostics;}
    private final InetAddress selectedSource;private final Socket socket;private final InputStream input;private final OutputStream output;private final int id;private final AtomicBoolean closed=new AtomicBoolean();
    private final boolean cosmetics,combat,radmin;private volatile String region="forest";private volatile java.util.List<Protocol.QuestProgress> quests=java.util.List.of();private volatile java.util.List<Protocol.CombatEvent> combatEvents=java.util.List.of();private volatile java.util.Map<Integer,Protocol.Appearance> appearances=java.util.Map.of();
    private final InetAddress address;private final int port;private final String publicName;private final boolean lanAllowed;
    private final SessionCharacterSelection characterSelection;
    private final boolean gameplay;
    private final boolean skillApplication;
    private volatile SkillProtocol.State skillState;
    /** Expõe somente estados da revisão ativa; eventos repetidos são filtrados pelo renderer. */
    public SkillProtocol.State skills(){var value=skillState;return connected()&&revisions!=null&&!revisions.blocked()&&value!=null&&revisions.accepts(value.hash())?value:null;}
    public boolean skillApplication(){return skillApplication;}
    /** Envia intenção; identidade do domínio, custo, acertos e efeitos pertencem ao servidor. */
    public synchronized boolean castSkill(int slot,int targetId,long generation,float x,float y){
        if(closed.get()||!skillApplication||revisions.blocked())return false;
        return enqueueCommand(new SkillProtocol.Request(++sequence,slot,targetId,generation,x,y));
    }
    private volatile GameplayProtocol.Sheet playerSheet;
    private volatile GameplayProtocol.Attributes attributeState;
    private volatile GameplayProtocol.Energy energyState;
    private volatile long pendingEnergySequence;
    private synchronized void acceptEnergy(GameplayProtocol.Energy value){if(!revisions.accepts(value.hash()))return;var previous=energyState;if(previous==null||!previous.hash().equals(value.hash())||value.sequence()>=previous.sequence()){energyState=value;if(value.sequence()>=pendingEnergySequence)pendingEnergySequence=0;}}
    public boolean energyPending(){return pendingEnergySequence>0;}
    public record EnergyView(GameplayProtocol.Energy state,boolean pending) { }
    public synchronized EnergyView energyView(){var value=energyState;boolean pending=energyPending()||value!=null&&revisions!=null&&!revisions.accepts(value.hash());return new EnergyView(!pending&&connected()&&revisions!=null&&!revisions.blocked()&&value!=null&&revisions.accepts(value.hash())?value:null,pending);}
    public GameplayProtocol.Energy energy(){return energyView().state();}
    public boolean requestEnergy(){return gameplay(GameplayProtocol.Operation.QUERY_ENERGY,"self");}
    public GameplayProtocol.Attributes attributes(){var value=attributeState;return connected()&&revisions!=null&&!revisions.blocked()&&value!=null&&revisions.accepts(value.hash())?value:null;}
    public boolean requestAttributes(){return gameplay(GameplayProtocol.Operation.QUERY_ATTRIBUTES,"self");}
    public synchronized boolean allocateAttribute(String attribute){if(!java.util.Set.of("strength","intelligence","agility","energy").contains(attribute))return false;boolean accepted=gameplay(GameplayProtocol.Operation.ALLOCATE_ATTRIBUTE,attribute);if(accepted){if(attribute.equals("energy"))pendingEnergySequence=sequence;requestAttributes();if(attribute.equals("energy"))requestEnergy();}return accepted;}
    private volatile int selectedTarget;
    private volatile long targetEventFloor;
    private volatile String targetRegion="",targetRevision="";
    /** Seleção única de atores recebidos; aliados derrotados continuam elegíveis para revive na aplicação 7. */
    public synchronized boolean selectTarget(int actorId){
        if(!gameplay||closed.get()||revisions.blocked())return false;
        var snapshot=snapshot();if(actorId!=0&&(snapshot==null||snapshot.actors().stream().noneMatch(a->a.id()==id&&a.hp()>0)||snapshot.actors().stream().noneMatch(a->a.id()==actorId&&(a.id()>1_000_000&&a.id()<2_000_000&&a.hp()>0||skillApplication&&a.id()<=1_000_000))))return false;
        if(!gameplay(GameplayProtocol.Operation.SELECT_TARGET,actorId==0?"none":actorId<=1_000_000?"ally_"+actorId:"enemy_"+actorId))return false;
        targetRegion=region;targetRevision=revisions.hash();targetEventFloor=combatEvents.stream().mapToLong(Protocol.CombatEvent::sequence).max().orElse(0);selectedTarget=actorId;return true;
    }
    public synchronized int selectedTarget(){int target=selectedTarget;var snapshot=snapshot();if(target!=0&&(closed.get()||revisions==null||revisions.blocked()||!region.equals(targetRegion)||!revisions.hash().equals(targetRevision)||snapshot==null||snapshot.actors().stream().noneMatch(a->a.id()==id&&a.hp()>0)||snapshot.actors().stream().noneMatch(a->a.id()==target&&(a.hp()>0||skillApplication&&a.id()<=1_000_000))||target>1_000_000&&combatEvents.stream().anyMatch(e->e.sequence()>targetEventFloor&&e.enemy()==target&&e.kind().equals("DEATH"))))selectedTarget=0;return selectedTarget;}
    public boolean cycleTarget(){var snapshot=snapshot();if(snapshot==null)return false;var self=snapshot.actors().stream().filter(a->a.id()==id&&a.hp()>0).findFirst().orElse(null);if(self==null)return false;
        var candidates=snapshot.actors().stream().filter(a->a.id()>1_000_000&&a.id()<2_000_000&&a.hp()>0||skillApplication&&a.id()<=1_000_000).sorted(java.util.Comparator.<Protocol.Actor>comparingInt(a->a.id()>1_000_000?0:a.id()==id?2:1).thenComparingDouble(a->Math.hypot(a.x()-self.x(),a.y()-self.y())).thenComparingInt(Protocol.Actor::id)).toList();
        if(candidates.isEmpty())return selectTarget(0);int target=selectedTarget(),index=-1;for(int i=0;i<candidates.size();i++)if(candidates.get(i).id()==target)index=i;return selectTarget(candidates.get((index+1)%candidates.size()).id());
    }
    public GameplayProtocol.Sheet playerSheet(){return revisions!=null&&!revisions.blocked()?playerSheet:null;}
    public boolean supportsGameplay(){return gameplay;}
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
    /** Conecta com escolha explicita, sem rebaixar silenciosamente para uma sessao sem personagem. */
    public static LocalClient character(InetAddress address,int port,SessionCharacterSelection selection,boolean lan,boolean radmin,InetAddress source,com.projetok.studio.AssetCatalog.Result assets)throws IOException{
        java.util.Objects.requireNonNull(selection,"personagem");java.util.Objects.requireNonNull(assets,"assets");
        return new LocalClient(address,port,selection.name(),lan,true,true,radmin,source,assets,selection);
    }
    public static LocalClient gameplay(InetAddress address,int port,SessionCharacterSelection selection,boolean lan,boolean radmin,InetAddress source,com.projetok.studio.AssetCatalog.Result assets)throws IOException {
        java.util.Objects.requireNonNull(selection);java.util.Objects.requireNonNull(assets);
        return new LocalClient(address,port,selection.name(),lan,true,true,radmin,source,assets,selection,true);
    }
    /** Negocia as habilidades explicitamente, mantendo gameplay() como entrada V6 compatível. */
    public static LocalClient skillGame(InetAddress address,int port,SessionCharacterSelection selection,boolean lan,boolean radmin,InetAddress source,com.projetok.studio.AssetCatalog.Result assets)throws IOException {
        java.util.Objects.requireNonNull(selection);java.util.Objects.requireNonNull(assets);
        return new LocalClient(address,port,selection.name(),lan,true,true,radmin,source,assets,selection,true,true);
    }
    /** Escolha inicial conservada na reconexao; null identifica os clientes historicos. */
    public SessionCharacterSelection characterSelection(){return characterSelection;}
    private LocalClient(InetAddress address,int port,String name,boolean lan,boolean cosmetics,boolean combat,boolean radmin,InetAddress source)throws IOException{
        this(address,port,name,lan,cosmetics,combat,radmin,source,null);
    }
    private LocalClient(InetAddress address,int port,String name,boolean lan,boolean cosmetics,boolean combat,boolean radmin,InetAddress source,com.projetok.studio.AssetCatalog.Result contentAssets)throws IOException{
        this(address,port,name,lan,cosmetics,combat,radmin,source,contentAssets,null);
    }
    private LocalClient(InetAddress address,int port,String name,boolean lan,boolean cosmetics,boolean combat,boolean radmin,InetAddress source,com.projetok.studio.AssetCatalog.Result contentAssets,SessionCharacterSelection selection)throws IOException{
        this(address,port,name,lan,cosmetics,combat,radmin,source,contentAssets,selection,false);
    }
    private LocalClient(InetAddress address,int port,String name,boolean lan,boolean cosmetics,boolean combat,boolean radmin,InetAddress source,com.projetok.studio.AssetCatalog.Result contentAssets,SessionCharacterSelection selection,boolean gameplay)throws IOException {
        this(address,port,name,lan,cosmetics,combat,radmin,source,contentAssets,selection,gameplay,false);
    }
    private LocalClient(InetAddress address,int port,String name,boolean lan,boolean cosmetics,boolean combat,boolean radmin,InetAddress source,com.projetok.studio.AssetCatalog.Result contentAssets,SessionCharacterSelection selection,boolean gameplay,boolean skillApplication)throws IOException {
        this.gameplay=gameplay;
        this.skillApplication=skillApplication;
        this.characterSelection=selection;
        this.revisionAssets=contentAssets;this.selectedSource=source;this.cosmetics=cosmetics;this.combat=combat;this.radmin=radmin;if(radmin&&!RadminAccess.address(address))throw new IllegalArgumentException("Destino Radmin exige IPv4 26.x.x.x");
        this.address=address;this.port=port;publicName=name;lanAllowed=lan;
        if(!radmin&&!address.isLoopbackAddress()&&(!lan||!address.isSiteLocalAddress()||address.isAnyLocalAddress()||address.isMulticastAddress()))throw new IllegalArgumentException("Endereco privado LAN exige opcao explicita");new Protocol.Hello(1,name);
        socket=new Socket();try{
            if(radmin)socket.bind(new InetSocketAddress(source==null?RadminAccess.clientSource():RadminAccess.requireLocal(source),0));
            socket.connect(new InetSocketAddress(address,port),2000);socket.setSoTimeout(3000);socket.setTcpNoDelay(true);input=new BufferedInputStream(socket.getInputStream(),8192);output=new BufferedOutputStream(socket.getOutputStream(),8192);Protocol.write(output,new Protocol.Hello(selection!=null?(skillApplication?Protocol.SKILL_APPLICATION:gameplay?Protocol.GAMEPLAY_APPLICATION:Protocol.CHARACTER_APPLICATION):contentAssets!=null?Protocol.CONTENT_APPLICATION:combat?Protocol.COMBAT_APPLICATION:cosmetics?Protocol.COSMETIC_APPLICATION:1,name));
            if(selection!=null)Protocol.write(output,new Protocol.CharacterCreate(selection.race(),selection.hair(),selection.clothing()));
            var reply=Protocol.read(input);
            if(!(reply instanceof Protocol.Welcome welcome)){
                if(selection!=null&&reply instanceof Protocol.Error rejected&&"Protocolo incompativel".equals(rejected.reason()))
                    throw new IOException("Servidor desatualizado: atualize o servidor para aceitar a criacao de personagem.");
                throw new IOException("Conexao recusada: "+(selection!=null&&reply instanceof Protocol.Error rejected?rejected.reason():reply));
            }
            id=welcome.id();
        }catch(IOException error){try{socket.close();}catch(IOException ignored){}String message=java.util.Objects.toString(error.getMessage(),"Conexao encerrada");if(selection!=null&&!message.startsWith("Servidor desatualizado:")&&!message.startsWith("Conexao recusada:"))throw new IOException("Nao foi possivel entrar com o personagem. Confira a conexao e atualize o servidor se ele ainda nao oferece criacao de personagem. "+message,error);throw error;}
        catch(RuntimeException error){try{socket.close();}catch(IOException ignored){}throw error;}
        if(contentAssets!=null)revisions=new ClientRevisionSync(contentAssets,m->{if(!enqueue(m))close();},()->{state=null;taggedSnapshot=null;});
        reader=new Thread(()->{try{while(!closed.get()){var message=Protocol.read(input);String receivedRevision="";if(skillApplication&&message instanceof SkillProtocol.State skills){if(revisions.accepts(skills.hash())&&(skillState==null||!skillState.hash().equals(skills.hash())||skills.tick()>=skillState.tick()))skillState=skills;continue;}if(gameplay&&message instanceof GameplayProtocol.Energy energy){acceptEnergy(energy);continue;}if(gameplay&&message instanceof GameplayProtocol.Attributes attributes){if(revisions.accepts(attributes.hash()))attributeState=attributes;continue;}if(revisions!=null&&message instanceof Protocol.Content c){revisions.receive(c);continue;}if(revisions!=null){if(gameplay&&message instanceof GameplayProtocol.State sheetState){if(!revisions.accepts(sheetState.hash())||sheetState.combat().session().world().tick()<=revisionTickFloor)continue;playerSheet=sheetState.sheet();message=new Protocol.RevisionState(sheetState.hash(),sheetState.combat());}if(message instanceof Protocol.RevisionState r){if(!revisions.accepts(r.hash())||r.state().session().world().tick()<=revisionTickFloor)continue;receivedRevision=r.hash();message=r.state();}else if(!(message instanceof Protocol.Pong))throw new IOException("Snapshot sem revisao");}if(combat&&message instanceof Protocol.Pong pong){if(pong.nonce()==pendingPing){long elapsed=System.nanoTime()-pingStarted;latencyMillis=elapsed/1_000_000d;if(diagnostics.enabled)diagnostics.rtt.add(elapsed);pendingPing=0;}continue;}Protocol.State next;if(combat&&message instanceof Protocol.CombatState c){if(state==null||c.session().world().tick()>state.tick()){combatEvents=c.events();region=c.region();quests=c.quests();}message=c.session();}if(cosmetics&&message instanceof Protocol.SessionState extended){next=extended.world();if(state!=null&&next.tick()<=state.tick()){if(diagnostics.enabled)diagnostics.staleSnapshots++;continue;}var byId=new java.util.HashMap<Integer,Protocol.Appearance>();extended.appearances().forEach(a->byId.put(a.id(),a));appearances=java.util.Map.copyOf(byId);}else if(message instanceof Protocol.State plain){next=plain;}else throw new IOException("Resposta inesperada");if(state==null||next.tick()>state.tick()){diagnostics.received(System.nanoTime());state=next;if(revisions!=null){revisionTickFloor=Math.max(revisionTickFloor,next.tick());taggedSnapshot=new TaggedSnapshot(receivedRevision,next);}synchronized(changed){changed.notifyAll();}var listener=snapshotListener;if(listener!=null)listener.run();}}}catch(IOException error){if(!closed.get())problem=java.util.Objects.toString(error.getMessage(),"Conexao encerrada");}finally{close();appearances=java.util.Map.of();}},"coop-client-reader-"+id);
        writer=new Thread(()->{try{while(!closed.get()){var next=outgoing.take();long start=System.nanoTime();if(next instanceof Protocol.Ping ping&&ping.nonce()==pendingPing)pingStarted=start;diagnostics.write(output,next);if(diagnostics.enabled){diagnostics.writes.add(System.nanoTime()-start);diagnostics.outboundQueue=outgoing.size();}}}catch(IOException|InterruptedException error){if(!closed.get())problem=java.util.Objects.toString(error.getMessage(),"Conexao encerrada");}finally{close();}},"coop-client-writer-"+id);
        reader.setDaemon(true);writer.setDaemon(true);reader.start();writer.start();
    }
    private boolean enqueue(Protocol.Message message){boolean accepted=outgoing.offer(message);if(diagnostics.enabled){diagnostics.outboundQueue=outgoing.size();if(!accepted)diagnostics.rejectedMessages++;}return accepted;}
    private boolean enqueueCommand(Protocol.Message command){if(revisions!=null){if(revisions.blocked())return false;return enqueue(new Protocol.RevisionCommand(revisions.hash(),command));}return enqueue(command);}
    public int id(){return id;}public Protocol.State snapshot(){if(revisions==null)return state;var tagged=taggedSnapshot;return tagged!=null&&revisions.accepts(tagged.hash())?tagged.state():null;}public boolean connected(){return !closed.get();}public String problem(){return problem;}
    /** Reconexão explícita cria nova sessão efêmera; chamar em worker, nunca na EDT. */
    public LocalClient reconnect()throws IOException{if(connected())throw new IllegalStateException("Encerre a conexão anterior antes de reconectar");return new LocalClient(address,port,publicName,lanAllowed,cosmetics,combat,radmin,selectedSource,revisionAssets,characterSelection,gameplay,skillApplication);}
    public boolean supportsCosmetics(){return cosmetics;}
    public java.util.Map<Integer,Protocol.Appearance> appearances(){return appearances;}
    public synchronized boolean gameplay(GameplayProtocol.Operation operation,String target){if(closed.get()||!gameplay||revisions.blocked())return false;return enqueueCommand(new GameplayProtocol.Request(++sequence,operation,target));}
    public synchronized boolean appearance(String hair,String clothing){if(closed.get()||!cosmetics)return false;return enqueueCommand(new Protocol.AppearanceRequest(++sequence,hair,clothing));}
    public double latencyMillis(){return latencyMillis;}
    public synchronized boolean measureLatency(){if(closed.get()||!combat||pendingPing!=0&&System.nanoTime()-pingStarted<5_000_000_000L)return false;long nonce=++pingCounter;pendingPing=nonce;pingStarted=System.nanoTime();if(!enqueue(new Protocol.Ping(nonce))){pendingPing=0;return false;}return true;}
    public synchronized boolean request(Protocol.WorldAction action,String target){if(closed.get()||!combat||revisions!=null&&revisions.blocked())return false;return enqueueCommand(new Protocol.WorldRequest(++sequence,action,target));}
    public synchronized boolean send(Protocol.Action action,float x,float y){if(closed.get()||revisions!=null&&revisions.blocked())return false;if(gameplay&&x==0&&y==0&&(action==Protocol.Action.ATTACK||action==Protocol.Action.LIGHTNING_STEP)){int target=selectedTarget();if(target>0&&target<=1_000_000)return false;return gameplay(action==Protocol.Action.ATTACK?GameplayProtocol.Operation.BASIC_ATTACK:GameplayProtocol.Operation.SHUNPO_OFFENSIVE,target==0?"none":"enemy_"+target);}return enqueueCommand(new Protocol.Intent(++sequence,action,x,y));}
    public Protocol.State await(java.util.function.Predicate<Protocol.State> predicate,Duration timeout)throws InterruptedException,TimeoutException{
        long deadline=System.nanoTime()+timeout.toNanos();synchronized(changed){while(true){var visible=snapshot();if(visible!=null&&predicate.test(visible))return visible;long left=deadline-System.nanoTime();if(left<=0||closed.get())throw new TimeoutException("Snapshot esperado ausente: "+problem);TimeUnit.NANOSECONDS.timedWait(changed,left);}}
    }
    public void close(){if(!closed.compareAndSet(false,true))return;snapshotListener=null;skillState=null;if(revisions!=null)revisions.close();appearances=java.util.Map.of();try{socket.close();}catch(IOException ignored){}reader.interrupt();writer.interrupt();synchronized(changed){changed.notifyAll();}}
}
