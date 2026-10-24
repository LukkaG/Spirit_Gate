package com.projetok.multiplayer;

import com.projetok.prototype.DebugPanel;
import com.projetok.prototype.rpg.*;
import java.awt.image.BufferedImage;
import java.net.*;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import javax.imageio.ImageIO;
import javax.swing.*;

/** Evidência real: servidor proprietário, dois sockets TCP e duas janelas Swing. */
public final class CorujaoFiveDemo {
    private static long captureNanos;
    private static void capture(JFrame frame,Path file)throws Exception{long started=System.nanoTime();SwingUtilities.invokeAndWait(()->{var image=new BufferedImage(frame.getWidth(),frame.getHeight(),2);var g=image.createGraphics();frame.printAll(g);g.dispose();try{ImageIO.write(image,"png",file.toFile());}catch(Exception e){throw new IllegalStateException(e);}});captureNanos+=System.nanoTime()-started;}
    private static void key(JFrame frame,String action)throws Exception{SwingUtilities.invokeAndWait(()->((DebugPanel)frame.getRootPane().getClientProperty("networkPanel")).getActionMap().get(action).actionPerformed(null));}
    private static int hp(Protocol.State state){return state.actors().stream().filter(Protocol.Actor::boss).findFirst().orElseThrow().hp();}
    public static void main(String[] args)throws Exception{
        Path output=Path.of(args[0]);Files.createDirectory(output);JFrame[] frames=new JFrame[2];
        var catalog=CosmeticCatalog.load(Path.of("assets/dev/generated/corujao-iv-v1"));var sprites=RpgPlayerSprites.load(Path.of("assets/dev/player-base"),250,.5);
        var measured=new RpgSessionWorld(List.of("reaper","human"),catalog,true);measured.join(1,"Sintetico A");measured.join(2,"Sintetico B");
        for(int i=0;i<300;i++)measured.step();long[] samples=new long[3000];var cpu=java.lang.management.ManagementFactory.getThreadMXBean();long cpuStart=cpu.isCurrentThreadCpuTimeSupported()?cpu.getCurrentThreadCpuTime():-1;long start=System.nanoTime();
        for(int i=0;i<samples.length;i++){long before=System.nanoTime();measured.step();measured.combatSnapshotFor(1);measured.combatSnapshotFor(2);samples[i]=System.nanoTime()-before;}
        long elapsed=System.nanoTime()-start;long cpuElapsed=cpuStart<0?-1:cpu.getCurrentThreadCpuTime()-cpuStart;Arrays.sort(samples);var bytes=new java.io.ByteArrayOutputStream();Protocol.write(bytes,measured.combatSnapshotFor(1));
        Files.writeString(output.resolve("benchmark.json"),"{\"samples\":3000,\"ownerThreadCpuMillis\":"+(cpuElapsed/1_000_000d)+",\"tickAndTwoSnapshotsMeanMicros\":"+(elapsed/3000d/1000)+",\"p95Micros\":"+(samples[2850]/1000d)+",\"maxMicros\":"+(samples[2999]/1000d)+",\"frameBytes\":"+bytes.size()+",\"entities\":3,\"usedHeapBytes\":"+(Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory())+",\"synthetic\":true}");
        try(var server=new LocalSimulationServer(InetAddress.getLoopbackAddress(),0,new RpgSessionWorld(List.of("reaper","human"),catalog,true))){
            server.start();long connectStart=System.nanoTime();
            try(var a=LocalClient.combat(InetAddress.getLoopbackAddress(),server.port(),"Jogador A",false);var b=LocalClient.combat(InetAddress.getLoopbackAddress(),server.port(),"Jogador B",false)){
                double connectMillis=(System.nanoTime()-connectStart)/1_000_000d;
                a.await(s->s.actors().size()==3,Duration.ofSeconds(3));b.await(s->s.actors().size()==3,Duration.ofSeconds(3));
                SwingUtilities.invokeAndWait(()->{frames[0]=RpgNetworkClientLauncher.open(a,sprites,"localhost:"+server.port(),catalog,new CosmeticCatalog.Selection("hair_short","light_armor"));frames[1]=RpgNetworkClientLauncher.open(b,sprites,"localhost:"+server.port(),catalog,new CosmeticCatalog.Selection("hair_spiky","none"));});
                try{
                    key(frames[1],"Wtrue");b.await(s->s.actors().stream().anyMatch(v->v.id()==b.id()&&v.y()<350),Duration.ofSeconds(3));key(frames[1],"Wfalse");
                    Thread.sleep(150);a.send(Protocol.Action.ATTACK,0,0);var hit=a.await(s->hp(s)<40,Duration.ofSeconds(3));int afterHit=hp(hit);a.measureLatency();a.await(s->!Double.isNaN(a.latencyMillis()),Duration.ofSeconds(3));b.await(s->hp(s)==afterHit,Duration.ofSeconds(3));
                    capture(frames[0],output.resolve("combat-a.png"));capture(frames[1],output.resolve("combat-b.png"));
                    for(int i=0;i<20&&hp(a.snapshot())>0;i++){b.send(Protocol.Action.ATTACK,0,0);a.send(Protocol.Action.ATTACK,0,0);Thread.sleep(430);}
                    a.await(s->hp(s)==0,Duration.ofSeconds(3));b.await(s->hp(s)==0,Duration.ofSeconds(3));capture(frames[0],output.resolve("defeat-a.png"));capture(frames[1],output.resolve("defeat-b.png"));
                    long deaths=a.combatEvents().stream().filter(e->e.kind().equals("DEATH")).count(),rewards=a.combatEvents().stream().filter(e->e.kind().equals("SYNTHETIC_REWARD")).count();if(deaths!=1||rewards!=2)throw new IllegalStateException("Ledger inesperado: "+deaths+" / "+rewards);
                    Files.writeString(output.resolve("result.json"),"{\"realTcpClients\":2,\"serverAuthoritative\":true,\"initialEnemyHp\":40,\"sharedHpAfterAttack\":"+afterHit+",\"finalEnemyHp\":0,\"deathEvents\":"+deaths+",\"syntheticRewardEvents\":"+rewards+",\"rttMillis\":"+a.latencyMillis()+",\"fourCapturesMillis\":"+(captureNanos/1_000_000d)+",\"twoConnectionsMillis\":"+connectMillis+",\"componentCaptures\":true,\"physicalUiApproved\":false,\"remoteRadminTested\":false}");
                }finally{SwingUtilities.invokeAndWait(()->{for(var frame:frames)if(frame!=null)frame.dispose();});}
            }if(server.failure()!=null)throw new IllegalStateException(server.failure());
        }
        System.exit(0);
    }
}
