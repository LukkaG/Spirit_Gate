package com.projetok.multiplayer;

import com.projetok.domain.movement.SessionCollisionProfile;
import com.projetok.prototype.rpg.LauncherSession;
import com.projetok.studio.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.lang.management.ManagementFactory;
import java.net.InetAddress;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.BooleanSupplier;
import javax.imageio.ImageIO;

/** Soak opt-in local; nenhuma conta, campanha ou imagem original é carregada. */
public final class Task190SoakProbe {
    record Fixture(Map<String,WorldDocument> maps,AssetCatalog.Result assets,int bytes) { }
    record Sample(long heap,int threads,double tick,double paint,double rtt,int out,int command,int control) { }

    static void require(boolean ok,String reason) {if(!ok)throw new AssertionError(reason);}
    static void await(BooleanSupplier condition,int seconds) throws Exception {
        long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(seconds);
        while(!condition.getAsBoolean()&&System.nanoTime()<end)Thread.sleep(10);
        require(condition.getAsBoolean(),"Condição excedeu "+seconds+" s");
    }
    static Map<String,WorldDocument> maps(AssetCatalog.Asset asset,int lastLength) throws Exception {
        var maps=new TreeMap<>(WorldContent.baseline().bundle().maps());
        for(String id:List.of("forest","village")) {
            var d=maps.get(id);var metadata=new TreeMap<String,String>();
            for(int i=0;i<120;i++)metadata.put("soak_padding_"+i,"x".repeat(4096));
            if(id.equals("forest"))metadata.putAll(new SessionCollisionProfile("soak-base",1,12,22).metadata());
            var elements=new ArrayList<>(d.elements());
            if(id.equals("forest")) {
                elements.set(1,elements.get(1).at(elements.get(1).id(),700,80));
                for(int i=1;i<5;i++)elements.add(new WorldDocument.Element("soak_enemy_"+i,WorldDocument.Kind.ENEMY,"","",
                    new WorldDocument.Crop(0,0,32,32),700,80+i*100,32,32,"interaction",true,false,"slime","FRONT",Map.of()));
                for(int i=0;i<17;i++) {
                    var properties=new TreeMap<String,String>();
                    for(int key=0;key<32;key++)properties.put("soak_"+key,"y".repeat(i==16?lastLength:4096));
                    elements.add(new WorldDocument.Element("soak_tile_"+i,WorldDocument.Kind.TILE,asset.id(),asset.sha(),
                        new WorldDocument.Crop(0,0,32,32),32+i*32,32,32,32,"ground",true,false,"","FRONT",properties));
                }
            }
            maps.put(id,d.withMetadata(metadata).withElements(elements));
        }
        return maps;
    }
    static int canonicalSize(Map<String,WorldDocument> maps) throws Exception {
        int total=0;for(var d:maps.values())total+=LiveRevision.canonical(d).length;return total;
    }
    static Fixture fixture(Path root) throws Exception {
        Files.createDirectories(root.resolve("assets"));
        var image=new BufferedImage(32,32,BufferedImage.TYPE_INT_ARGB);
        var g=image.createGraphics();g.setColor(new java.awt.Color(20,100,45));g.fillRect(0,0,32,32);g.dispose();
        ImageIO.write(image,"png",root.resolve("assets/soak.png").toFile());
        var assets=AssetCatalog.scan(root,null);require(assets.assets().size()==1,"Um asset sintético local");
        var base=WorldContent.baseline();
        int header=WorldContent.encode(base).length-canonicalSize(base.bundle().maps());
        int zero=canonicalSize(maps(assets.assets().get(0),0))+header;
        int last=(int)(((long)WorldContent.LIMIT-2048-zero)*3/(4*32));
        require(last>=0&&last<=4096,"Padding calculado dentro do limite por campo: "+last);
        var maps=maps(assets.assets().get(0),last);
        var revision=LiveRevision.prepare(1,"forest",maps,assets);
        int bytes=WorldContent.encode(revision).length;
        require(bytes<=WorldContent.LIMIT&&bytes>=WorldContent.LIMIT-8192,"Descritor próximo de 4 MiB: "+bytes);
        require(revision.bundle().scenes().enemies().size()+revision.bundle().scenes().npcs().size()==6,"Seis atores adicionais");
        var excessive=new TreeMap<>(maps);var metadata=new TreeMap<>(maps.get("forest").metadata());
        metadata.put("soak_overflow","z".repeat(4096));excessive.put("forest",maps.get("forest").withMetadata(metadata));
        boolean rejected=false;
        try {WorldContent.encode(LiveRevision.prepare(2,"forest",excessive,assets));}
        catch(IllegalArgumentException|IOException expected) {
            require(expected.getMessage().contains("4 MiB")||expected.getMessage().contains("excessivo"),expected.toString());rejected=true;
        }
        require(rejected,"Conteúdo acima do limite deve ser recusado");
        System.out.println("TASK190 fixture bytes="+bytes+" extraActors=6 assets=1 pngTransferred=false overflowRejected=true");
        return new Fixture(maps,assets,bytes);
    }
    static LiveRevision variant(Fixture fixture,long number,boolean edited) throws Exception {
        var maps=new TreeMap<>(fixture.maps());
        if(edited) {
            var d=maps.get("forest");var metadata=new TreeMap<>(d.metadata());
            metadata.putAll(new SessionCollisionProfile("soak-edit",1,10,24).metadata());
            maps.put("forest",d.withMetadata(metadata).withHitboxes(List.of(new WorldDocument.Hitbox("central_gray",500,180,120,120,true,"collision"))));
        }
        return LiveRevision.prepare(number,"forest",maps,fixture.assets());
    }
    static double percentile(double[] values,double q) {
        double[] finite=Arrays.stream(values).filter(Double::isFinite).sorted().toArray();
        return finite.length==0?Double.NaN:finite[Math.min(finite.length-1,(int)Math.ceil(q*finite.length)-1)];
    }
    static String number(double value){return Double.isFinite(value)?Double.toString(value):"null";}
    static String summary(String name,List<Sample> samples,long duration,List<Long> publications,int bytes) {
        return "{\"epoch\":\""+name+"\",\"duration_seconds\":"+duration/1e9+",\"samples\":"+samples.size()
            +",\"descriptor_bytes\":"+bytes+",\"clients\":2,\"extra_actors\":6,\"heap_p50_bytes\":"+number(percentile(samples.stream().mapToDouble(s->s.heap()).toArray(),.5))
            +",\"heap_p95_bytes\":"+number(percentile(samples.stream().mapToDouble(s->s.heap()).toArray(),.95))
            +",\"threads_p95\":"+number(percentile(samples.stream().mapToDouble(s->s.threads()).toArray(),.95))
            +",\"tick_window_p95_ms\":"+number(percentile(samples.stream().mapToDouble(s->s.tick()).toArray(),.95))
            +",\"paint_p95_ms\":"+number(percentile(samples.stream().mapToDouble(s->s.paint()).toArray(),.95))
            +",\"rtt_window_p95_ms\":"+number(percentile(samples.stream().mapToDouble(s->s.rtt()).toArray(),.95))
            +",\"publications\":"+publications.size()+",\"publication_p95_ms\":"+number(percentile(publications.stream().mapToDouble(v->v/1e6).toArray(),.95))+"}";
    }
    static String epoch(Path root,String name) throws Exception {
        Files.createDirectory(root);var fixture=fixture(root);
        String prior=System.getProperty(NetworkDiagnostics.PROPERTY);System.setProperty(NetworkDiagnostics.PROPERTY,"true");
        try(var server=LauncherSession.start(InetAddress.getLoopbackAddress(),0,null,root)) {
            var managed=LauncherSession.managed(server);var initial=variant(fixture,1,false);
            require(managed.store().publish(initial,"",managed.target().activeHash(),managed.target(),LivePublishing.Fault.NONE).active(),"Publicação inicial sem clientes");
            try(var a=LocalClient.revision(InetAddress.getLoopbackAddress(),server.port(),"Soak A",false,false,null,fixture.assets());
                var b=LocalClient.revision(InetAddress.getLoopbackAddress(),server.port(),"Soak B",false,false,null,fixture.assets())) {
                a.revisions().renderer(p->CompletableFuture.completedFuture(null));b.revisions().renderer(p->CompletableFuture.completedFuture(null));
                var stop=new AtomicBoolean();var measuring=new AtomicBoolean();var error=new AtomicReference<Throwable>();
                List<Sample> samples=Collections.synchronizedList(new ArrayList<>());
                var observer=new Thread(()->{
                    var image=new BufferedImage(960,640,BufferedImage.TYPE_INT_ARGB);long nextPing=0;
                    try {while(!stop.get()) {
                        if(System.nanoTime()>=nextPing){a.measureLatency();b.measureLatency();nextPing=System.nanoTime()+1_000_000_000L;}
                        double paint=Double.NaN;
                        if(!a.revisions().blocked()&&a.revisions().active()!=null) {
                            var prepared=a.revisions().active();var renderer=new StudioMapRenderer(prepared.revision().bundle(),prepared.images(),()->"forest");
                            var graphics=image.createGraphics();long start=System.nanoTime();
                            try {renderer.layers(graphics,false);renderer.layers(graphics,true);}finally{graphics.dispose();}
                            paint=(System.nanoTime()-start)/1e6;
                        }
                        if(measuring.get()&&samples.size()<4096)samples.add(new Sample(Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory(),
                            ManagementFactory.getThreadMXBean().getThreadCount(),server.diagnostics().ticks.percentileMillis(.95),paint,
                            a.diagnostics().rtt.percentileMillis(.95),server.diagnostics().outboundQueue,server.pendingCommands(),server.diagnostics().controlQueue));
                        Thread.sleep(100);
                    }}catch(Throwable failure){error.set(failure);}
                },"task190-observer");observer.setDaemon(true);observer.start();
                List<Long> publications=new ArrayList<>();long start=0;
                try {
                    await(()->!a.revisions().blocked()&&!b.revisions().blocked()&&a.snapshot()!=null&&b.snapshot()!=null,40);
                    require(a.connected()&&b.connected(),"Clientes iniciais conectados: "+a.problem()+" / "+b.problem());
                    start=System.nanoTime();measuring.set(true);long revision=2;int cycle=0;long heartbeat=0;
                    while(System.nanoTime()-start<TimeUnit.SECONDS.toNanos(150)) {
                        long elapsed=System.nanoTime()-start;
                        if(cycle<2&&elapsed>=TimeUnit.SECONDS.toNanos(20+cycle*60)) {
                            for(boolean edited:List.of(true,false)) {
                                var next=variant(fixture,revision++,edited);long transaction=System.nanoTime();
                                var result=managed.store().publish(next,managed.store().current(),managed.target().activeHash(),managed.target(),LivePublishing.Fault.NONE);
                                require(result.active(),result.message());publications.add(System.nanoTime()-transaction);
                                await(()->!a.revisions().blocked()&&!b.revisions().blocked(),30);
                                require(next.hash().equals(a.revisions().hash())&&next.hash().equals(b.revisions().hash()),"Hashes após commit/rollback");
                                require(next.hash().equals(managed.store().current()),"Ponteiro confirmado");
                            }
                            cycle++;
                        }
                        require(a.connected()&&b.connected(),"Cliente perdeu conexão: "+a.problem()+" / "+b.problem());
                        require(server.failure()==null,"Servidor sem falha");
                        require(error.get()==null,"Observer sem falha: "+error.get());
                        require(server.pendingCommands()<=64&&server.connections()<=2,"Filas/sessões limitadas");
                        if(elapsed-heartbeat>=TimeUnit.SECONDS.toNanos(10)){heartbeat=elapsed;System.out.println("TASK190 epoch="+name+" seconds="+elapsed/1e9+" cycles="+cycle+" samples="+samples.size());}
                        Thread.sleep(100);
                    }
                    require(cycle==2,"Dois pares de publicação/rollback por período");
                } finally {stop.set(true);observer.interrupt();observer.join(2000);require(!observer.isAlive(),"Observer encerrado");}
                require(error.get()==null||error.get() instanceof InterruptedException,"Observer: "+error.get());
                long duration=System.nanoTime()-start;
                try(var writer=Files.newBufferedWriter(root.resolve("samples.csv"))) {
                    writer.write("heap_bytes,threads,tick_window_p95_ms,paint_ms,rtt_window_p95_ms,out_queue,command_queue,control_queue\n");
                    for(var sample:samples)writer.write(sample.heap()+","+sample.threads()+","+sample.tick()+","+sample.paint()+","+sample.rtt()+","+sample.out()+","+sample.command()+","+sample.control()+"\n");
                }
                String result=summary(name,samples,duration,publications,fixture.bytes());
                Files.writeString(root.resolve("summary.json"),result);System.out.println(result);return result;
            }
        } finally {if(prior==null)System.clearProperty(NetworkDiagnostics.PROPERTY);else System.setProperty(NetworkDiagnostics.PROPERTY,prior);}
    }
    public static void main(String[] args) throws Exception {
        require(args.length==1,"Destino novo obrigatório");Path out=Path.of(args[0]).toAbsolutePath().normalize();Files.createDirectory(out);
        System.out.println("TASK190 maxHeapBytes="+Runtime.getRuntime().maxMemory()+" epochSeconds=150 epochs=2 sampleLimit=4096");
        String baseline=epoch(out.resolve("baseline"),"baseline");String followup=epoch(out.resolve("followup"),"followup");
        Files.writeString(out.resolve("comparison.json"),"{\"status\":\"PASS\",\"controlled_same_configuration\":true,\"performance_improvement_claimed\":false,\"epochs\":["+baseline+","+followup+"]}");
        System.out.println("TASK190 PASS epochs=2 totalMeasuredSeconds>=300");
    }
}
