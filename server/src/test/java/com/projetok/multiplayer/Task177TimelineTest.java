package com.projetok.multiplayer;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class Task177TimelineTest {
    Protocol.State state(long tick,float x){return new Protocol.State(tick,List.of(new Protocol.Actor(1,"Local",x,100,100,100,100,0,false),new Protocol.Actor(2,"Remote",x,200,100,100,100,0,false)));}
    @Test void jitterBatchesAndGapsHaveBoundedLagAndNoPositionRegression(){
        var d=new NetworkDiagnostics();var timeline=new RemoteTimeline(d);long now=1_000_000_000L;float previous=0;
        for(int i=0;i<100;i++){now+=(i%7==0?180:i%7==1?20:100)*1_000_000L;d.received(now);assertTrue(timeline.offer(state(i*3,20+i),now,"forest"));assertTrue(timeline.bufferMillis()>=25&&timeline.bufferMillis()<=150);
            for(int frame=0;frame<4;frame++){var sample=timeline.sample(now+frame*16_000_000L,1);assertEquals(20+i,sample.actors().get(0).x());float x=sample.actors().get(1).x();assertTrue(x>=previous);assertTrue(x<=20+i);previous=x;}
            assertFalse(timeline.offer(state(Math.max(0,i*3-1),20),now,"forest"));
        }
        assertEquals(100,timeline.dropped);assertEquals(119,timeline.sample(now+10_000_000_000L,1).actors().get(1).x());
    }
    @Test void teleportsRegionChangesAndLongGapsSnapImmediately(){var t=new RemoteTimeline(new NetworkDiagnostics());t.offer(state(1,20),1,"forest");t.offer(state(4,400),100_000_001,"forest");assertEquals(400,t.sample(100_000_001,1).actors().get(1).x());t.offer(state(7,410),200_000_001,"village");assertEquals(410,t.sample(200_000_001,1).actors().get(1).x());t.offer(state(100,420),300_000_001,"village");assertEquals(420,t.sample(300_000_001,1).actors().get(1).x());assertEquals(2,t.corrections);}
    @Test void measuredSamplesAreBoundedAndUsePercentiles(){var s=new NetworkDiagnostics.Samples();for(int i=1;i<=1000;i++)s.add(i*1_000_000L);assertEquals(256,s.count());assertEquals(872,s.percentileMillis(.5));assertEquals(988,s.percentileMillis(.95));}
    @Test void thirtyHertzUsesShortBufferAndNeverExtrapolatesOnNetworkStall(){
        var d=new NetworkDiagnostics();var t=new RemoteTimeline(d);long now=1_000_000_000L;
        for(int i=0;i<40;i++){now+=33_333_333L;d.received(now);t.offer(state(i,100+i),now,"forest");}
        assertEquals(25,t.bufferMillis(),.01);
        var immediate=t.sample(now,1);assertEquals(139,immediate.actors().get(0).x());
        assertTrue(immediate.actors().get(1).x()<139);
        assertEquals(139,t.sample(now+10_000_000_000L,1).actors().get(1).x());
    }
    @Test void delayedFriendDoesNotRenderInsideNewestLocalBlock(){
        var t=new RemoteTimeline(new NetworkDiagnostics());long now=1_000_000_000L;
        t.offer(new Protocol.State(1,List.of(new Protocol.Actor(1,"Local",100,400,100,100,100,0,false),new Protocol.Actor(2,"Friend",124,400,100,100,100,0,false))),now,"forest");
        t.offer(new Protocol.State(4,List.of(new Protocol.Actor(1,"Local",120,400,100,100,100,0,false),new Protocol.Actor(2,"Friend",144,400,100,100,100,0,false))),now+100_000_000L,"forest");
        var result=t.sample(now+100_000_000L,1);
        assertEquals(120,result.actors().get(0).x());assertEquals(144,result.actors().get(1).x());
        assertEquals(1,t.actorCorrections);
    }

    /** A fase invisível autoriza sobreposição e dispensa a correção visual de contato. */
    @Test void intangibleFriendKeepsInterpolationWhileOverlappingLocalBody(){
        for(int intangible:new int[]{1,2}){
            var t=new RemoteTimeline(new NetworkDiagnostics());long now=1_000_000_000L;
            t.offer(new Protocol.State(1,List.of(new Protocol.Actor(1,"Local",100,400,100,100,100,0,false),new Protocol.Actor(2,"Friend",124,400,100,100,100,0,false))),now,"forest");
            t.offer(new Protocol.State(4,List.of(new Protocol.Actor(1,"Local",120,400,100,100,100,0,false),new Protocol.Actor(2,"Friend",144,400,100,100,100,0,false))),now+100_000_000L,"forest");
            var result=t.sample(now+100_000_000L,1,12,id->id==intangible);
            assertTrue(result.actors().get(1).x()<144);assertEquals(0,t.actorCorrections);
        }
    }
}
