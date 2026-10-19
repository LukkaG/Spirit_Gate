package com.projetok.multiplayer;
import org.junit.jupiter.api.Test;
import java.net.InetAddress;
import java.time.Duration;
import static org.junit.jupiter.api.Assertions.*;
/** Combate completo por TCP real, sem acesso direto ao mundo autoritativo. */
class NetworkFightIntegrationTest {
    static Protocol.Actor actor(Protocol.State state,int id){return state.actors().stream().filter(a->a.id()==id).findFirst().orElseThrow();}
    private void control(LocalClient client,Protocol.State state,boolean first){var p=actor(state,client.id());var boss=actor(state,CoopWorld.BOSS_ID);double distance=Math.hypot(p.x()-boss.x(),p.y()-boss.y());boolean ready=state.actors().stream().filter(a->!a.boss()).allMatch(a->Math.hypot(a.x()-boss.x(),a.y()-boss.y())<=80);if(distance<=80){client.send(Protocol.Action.MOVE,0,0);if(ready)client.send(Protocol.Action.ATTACK,0,0);}else{float x=p.x()<530?550:first?boss.x()-60:boss.x(),y=p.x()<530?(first?345:410):first?boss.y():Math.min(600,boss.y()+60);client.send(Protocol.Action.MOVE,Math.abs(x-p.x())<5?0:Math.signum(x-p.x()),Math.abs(y-p.y())<5?0:Math.signum(y-p.y()));}}
    @Test void twoClientsDefeatSharedBossAndCannotDuplicateReward()throws Exception{
        try(var server=new LocalSimulationServer(InetAddress.getLoopbackAddress(),0)){server.start();try(var one=new LocalClient(InetAddress.getLoopbackAddress(),server.port(),"Coop Um");var two=new LocalClient(InetAddress.getLoopbackAddress(),server.port(),"Coop Dois")){
            Protocol.State state=one.await(s->s.actors().stream().filter(a->!a.boss()).count()==2,Duration.ofSeconds(3));
            for(int i=0;i<180&&actor(state,CoopWorld.BOSS_ID).hp()>0;i++){control(one,state,true);control(two,state,false);long tick=state.tick();state=one.await(s->s.tick()>tick,Duration.ofSeconds(3));}
            assertEquals(0,actor(state,CoopWorld.BOSS_ID).hp(),state.toString());assertEquals(20,actor(state,one.id()).coins(),state.toString());assertEquals(20,actor(state,two.id()).coins(),state.toString());
            for(int i=0;i<4;i++){one.send(Protocol.Action.ATTACK,0,0);two.send(Protocol.Action.ATTACK,0,0);}long done=state.tick();state=one.await(s->s.tick()>=done+6,Duration.ofSeconds(3));assertEquals(20,actor(state,one.id()).coins());assertEquals(20,actor(state,two.id()).coins());assertNull(server.failure());System.out.println("COOP_TCP_REAL bossHp=0 rewards=20,20 tick="+state.tick());
        }}
    }
}
