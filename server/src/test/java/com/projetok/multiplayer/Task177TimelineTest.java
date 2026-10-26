package com.projetok.multiplayer;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class Task177TimelineTest {
    Protocol.State state(long tick,float x){return new Protocol.State(tick,List.of(new Protocol.Actor(1,"Local",x,100,100,100,100,0,false),new Protocol.Actor(2,"Remote",x,200,100,100,100,0,false)));}
    @Test void jitterBatchesAndGapsHaveBoundedLagAndNoPositionRegression(){
        var d=new NetworkDiagnostics();var timeline=new RemoteTimeline(d);long now=1_000_000_000L;float previous=0;
        for(int i=0;i<100;i++){now+=(i%7==0?180:i%7==1?20:100)*1_000_000L;d.received(now);assertTrue(timeline.offer(state(i*3,20+i),now,"forest"));assertTrue(timeline.bufferMillis()>=50&&timeline.bufferMillis()<=150);
            for(int frame=0;frame<4;frame++){var sample=timeline.sample(now+frame*16_000_000L,1);assertEquals(20+i,sample.actors().get(0).x());float x=sample.actors().get(1).x();assertTrue(x>=previous);assertTrue(x<=20+i);previous=x;}
            assertFalse(timeline.offer(state(Math.max(0,i*3-1),20),now,"forest"));
        }
        assertEquals(100,timeline.dropped);assertEquals(119,timeline.sample(now+10_000_000_000L,1).actors().get(1).x());
    }
    @Test void teleportsRegionChangesAndLongGapsSnapImmediately(){var t=new RemoteTimeline(new NetworkDiagnostics());t.offer(state(1,20),1,"forest");t.offer(state(4,400),100_000_001,"forest");assertEquals(400,t.sample(100_000_001,1).actors().get(1).x());t.offer(state(7,410),200_000_001,"village");assertEquals(410,t.sample(200_000_001,1).actors().get(1).x());t.offer(state(100,420),300_000_001,"village");assertEquals(420,t.sample(300_000_001,1).actors().get(1).x());assertEquals(2,t.corrections);}
    @Test void measuredSamplesAreBoundedAndUsePercentiles(){var s=new NetworkDiagnostics.Samples();for(int i=1;i<=1000;i++)s.add(i*1_000_000L);assertEquals(256,s.count());assertEquals(872,s.percentileMillis(.5));assertEquals(988,s.percentileMillis(.95));}
}
