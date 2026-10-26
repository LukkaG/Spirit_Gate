package com.projetok.multiplayer;

import com.projetok.prototype.rpg.LauncherSession;
import com.projetok.studio.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.InetAddress;
import java.nio.file.Path;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class Task189DiagnosticsTest {
    @TempDir Path root;
    static NetworkDiagnostics diagnostics(boolean enabled) {
        String old=System.getProperty(NetworkDiagnostics.PROPERTY);
        System.setProperty(NetworkDiagnostics.PROPERTY,Boolean.toString(enabled));
        try { return new NetworkDiagnostics(); }
        finally { if(old==null)System.clearProperty(NetworkDiagnostics.PROPERTY);else System.setProperty(NetworkDiagnostics.PROPERTY,old); }
    }
    @Test void boundedRingAndStructuredSnapshotDoNotExposeInvalidNumbers() throws Exception {
        var d=diagnostics(true);
        assertTrue(d.json().contains("\"tick_p50_ms\":null"));
        for(int i=0;i<100_000;i++)d.ticks.add(i*1000L);
        assertEquals(256,d.ticks.count());
        int index=(Integer)Task187BackpressureTest.field(d.ticks,"index");
        assertTrue(index>=0&&index<256);
        d.ticks.add(-1);
        assertEquals(256,d.ticks.count());
        var out=new ByteArrayOutputStream();
        d.write(out,new Protocol.Ping(1));
        assertEquals(out.size(),d.bytesWritten.get());
        assertEquals(1,d.framesWritten.get());
        assertEquals(new Protocol.Ping(1),Protocol.read(new ByteArrayInputStream(out.toByteArray())));
        String json=d.json();
        assertFalse(json.contains("NaN"));assertFalse(json.contains("Infinity"));assertFalse(json.contains("hash"));
        assertTrue(json.length()<2048);
        System.out.println(json);
    }
    @Test void realPublicationCollectsTickPaintRttQueuesAndReadyActiveWithoutHashes() throws Exception {
        String old=System.getProperty(NetworkDiagnostics.PROPERTY);
        System.setProperty(NetworkDiagnostics.PROPERTY,"true");
        try(var server=LauncherSession.start(InetAddress.getLoopbackAddress(),0,null,root);
            var a=Task183NetworkLabTest.peer(server.port(),"Sintetico A");
            var b=Task183NetworkLabTest.peer(server.port(),"Sintetico B")) {
            WorldRevisionTest.waitFor(()->!a.revisions().blocked()&&!b.revisions().blocked());
            assertTrue(a.measureLatency());
            WorldRevisionTest.waitFor(()->!Double.isNaN(a.latencyMillis()));
            var managed=LauncherSession.managed(server);
            String initial=managed.target().activeHash();
            var next=new WorldRevisionTest().changed(1,500);
            assertTrue(managed.store().publish(next,"",initial,managed.target(),LivePublishing.Fault.NONE).active());
            WorldRevisionTest.waitFor(()->!a.revisions().blocked()&&!b.revisions().blocked());
            var prepared=a.revisions().active();
            var renderer=new StudioMapRenderer(prepared.revision().bundle(),prepared.images(),()->"forest");
            var image=new BufferedImage(960,640,BufferedImage.TYPE_INT_ARGB);
            for(int i=0;i<20;i++) {
                var g=image.createGraphics();long start=System.nanoTime();
                try { renderer.layers(g,false);renderer.layers(g,true); }
                finally {g.dispose();}
                a.diagnostics().paints.add(System.nanoTime()-start);
            }
            assertTrue(server.diagnostics().ticks.count()>0);
            assertTrue(a.diagnostics().rtt.count()>0);
            assertEquals(20,a.diagnostics().paints.count());
            assertTrue(server.diagnostics().outboundQueue<=2);
            assertTrue(server.diagnostics().controlQueue<=8);
            assertTrue(server.diagnostics().commandQueue<=64);
            var metrics=managed.target().metrics();
            assertTrue(metrics.descriptorPayloadBytes()>0);
            assertTrue(metrics.queuedWireBytes()>metrics.descriptorPayloadBytes());
            assertTrue(metrics.activationNanos()>0);
            assertTrue(metrics.transitionNanos()>0);
            assertFalse(managed.target().metricsJson().contains(next.hash()));
            System.out.println(server.diagnostics().json());
            System.out.println(a.diagnostics().json());
            System.out.println(managed.target().metricsJson());
            assertNull(server.failure());
        } finally {if(old==null)System.clearProperty(NetworkDiagnostics.PROPERTY);else System.setProperty(NetworkDiagnostics.PROPERTY,old);}
    }
    @Test void interleavedInformativeCostMeasurementDoesNotChangeWireBytes() throws Exception {
        var disabled=diagnostics(false);var enabled=diagnostics(true);
        var message=new Protocol.State(1,List.of());var sink=OutputStream.nullOutputStream();
        for(int i=0;i<2000;i++){disabled.write(sink,message);enabled.write(sink,message);}
        long[] off=new long[5],on=new long[5];
        for(int round=0;round<5;round++)for(int pass=0;pass<2;pass++) {
            boolean active=(round+pass)%2==0;var d=active?enabled:disabled;long start=System.nanoTime();
            for(int i=0;i<5000;i++)d.write(sink,message);
            (active?on:off)[round]=System.nanoTime()-start;
        }
        Arrays.sort(off);Arrays.sort(on);
        assertEquals(0,disabled.framesWritten.get());
        assertEquals(27000,enabled.framesWritten.get());
        System.out.printf(Locale.ROOT,"TASK189 microbench warmup=2000 rounds=5 framesPerRound=5000 disabledMedianNsPerFrame=%.3f enabledMedianNsPerFrame=%.3f sink=null network=false%n",off[2]/5000.0,on[2]/5000.0);
    }
}
