package com.projetok.multiplayer;

import com.projetok.studio.WorldContent;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Verifica limites do wire e duas conexões reais sem conceder autoridade ao cliente. */
class SkillProtocolTest {
    private static final String HASH="a".repeat(64);
    private static List<Integer> zero(){return List.of(0,0,0,0,0);}
    private static byte[] encode(Protocol.Message message)throws Exception{
        var out=new ByteArrayOutputStream();Protocol.write(out,message);return out.toByteArray();
    }
    private static Protocol.Message decode(byte[] bytes)throws Exception{return Protocol.read(new ByteArrayInputStream(bytes));}

    @Test void newFramesRoundTripAndLegacyBytesStayIdentical()throws Exception{
        var cast=new SkillProtocol.Request(8,3,1_000_100,4,140,300);
        var event=new SkillProtocol.Event(1,20,1,"water_3",140,300,250,300);
        var state=new SkillProtocol.State(HASH,20,List.of(event),List.of(0,0,170,0,0),0,false);
        for(var message:List.of(cast,state,new Protocol.RevisionCommand(HASH,cast)))
            assertEquals(message,decode(encode(message)));
        assertArrayEquals(new byte[]{0,0,0,2,1,5},encode(new Protocol.Bye()));
        assertArrayEquals(new byte[]{0,0,0,16,1,18,0,0,0,0,0,0,0,1,0,4,115,101,108,102},
                encode(new GameplayProtocol.Request(1,GameplayProtocol.Operation.EQUIP,"self")));
        assertEquals(6,Protocol.GAMEPLAY_APPLICATION);assertEquals(7,Protocol.SKILL_APPLICATION);
        assertTrue(Protocol.supportsCharacterApplication(7));assertTrue(Protocol.supportsContentApplication(7));
    }

    @Test void malformedIntentAndStateAreRejectedBeforeSimulation()throws Exception{
        assertThrows(IllegalArgumentException.class,()->new SkillProtocol.Request(1,0,0,0,140,300));
        assertThrows(IllegalArgumentException.class,()->new SkillProtocol.Request(1,6,0,0,140,300));
        assertThrows(IllegalArgumentException.class,()->new SkillProtocol.Request(1,1,2_000_001,0,140,300));
        assertThrows(IllegalArgumentException.class,()->new SkillProtocol.Request(1,1,0,0,Float.NaN,300));
        assertThrows(IllegalArgumentException.class,()->new SkillProtocol.Request(1,1,0,0,961,300));
        assertThrows(IllegalArgumentException.class,()->new SkillProtocol.Request(1,1,1_000_100,-1,140,300));
        var event=new SkillProtocol.Event(1,20,1,"water_3",140,300,250,300);
        assertThrows(IllegalArgumentException.class,()->new SkillProtocol.State(HASH,20,List.of(event,event),zero(),0,false));
        assertThrows(IllegalArgumentException.class,()->new SkillProtocol.State(HASH,19,List.of(event),zero(),0,false));
        assertThrows(IllegalArgumentException.class,()->new SkillProtocol.State(HASH,20,List.of(),List.of(0),0,false));
        assertThrows(IllegalArgumentException.class,()->new SkillProtocol.State(HASH,20,List.of(),zero(),6,false));
        var malformed=encode(new SkillProtocol.Request(1,1,0,0,140,300));malformed[14]=6;
        assertThrows(IOException.class,()->decode(malformed));
    }

    private static LocalClient connect(LocalSimulationServer server,String name,String race)throws Exception{
        var client=LocalClient.skillGame(InetAddress.getLoopbackAddress(),server.port(),
                new SessionCharacterSelection(name,race,"none","none"),false,false,null,Task201CharacterSessionTest.EMPTY);
        client.revisions().renderer(p->CompletableFuture.completedFuture(null));return client;
    }

    @Test void twoSkillClientsSeeConfirmedEffectsAndPrivateCooldowns()throws Exception{
        var world=new RpgSessionWorld(List.of("reaper","human"),SessionCharacterSelectionTest.catalog(),true,20);
        world.enableRevisions(WorldContent.baseline());
        try(var server=new LocalSimulationServer(InetAddress.getLoopbackAddress(),0,world)){
            server.start();try(var a=connect(server,"Skill Reaper","reaper");var b=connect(server,"Skill Human","human")){
                Task201CharacterSessionTest.ready(a,b);
                Task201CharacterSessionTest.waitFor(()->a.skills()!=null&&b.skills()!=null&&a.playerSheet()!=null,a,b);
                assertTrue(a.skillApplication());assertEquals(zero(),a.skills().cooldowns());
                assertTrue(a.castSkill(2,0,0,140,300));long before=a.snapshot().tick();
                Task201CharacterSessionTest.waitFor(()->a.snapshot().tick()>before+3,a,b);
                assertEquals(zero(),a.skills().cooldowns(),"Sem domínio não deve gastar nem iniciar recarga");
                a.gameplay(GameplayProtocol.Operation.CHOOSE_DOMAIN,"water");
                Task201CharacterSessionTest.waitFor(()->a.playerSheet().progress().domain().equals("water"),a,b);
                assertTrue(a.castSkill(2,0,0,140,300));
                Task201CharacterSessionTest.waitFor(()->a.skills().cooldowns().get(1)>0
                        &&b.skills().events().stream().anyMatch(e->e.player()==a.id()&&e.skill().equals("water_2")),a,b);
                assertEquals(zero(),b.skills().cooldowns());
                assertTrue(a.skills().events().stream().allMatch(e->e.tick()<=a.skills().tick()));
                assertNull(server.failure());
            }
        }
    }

    @Test void versionSixCanPlayBesideSevenWithoutReceivingSkillFrames()throws Exception{
        var world=new RpgSessionWorld(List.of("reaper","human"),SessionCharacterSelectionTest.catalog(),true,20);
        world.enableRevisions(WorldContent.baseline());
        try(var server=new LocalSimulationServer(InetAddress.getLoopbackAddress(),0,world)){
            server.start();try(var a=connect(server,"Skill Novo","reaper");var b=Task207InventoryTest.connect(server,"Skill Legado","human")){
                Task201CharacterSessionTest.ready(a,b);
                Task201CharacterSessionTest.waitFor(()->a.skills()!=null&&b.playerSheet()!=null,a,b);
                assertFalse(b.skillApplication());assertFalse(b.castSkill(1,0,0,140,300));assertNull(b.skills());
                a.gameplay(GameplayProtocol.Operation.CHOOSE_DOMAIN,"water");
                Task201CharacterSessionTest.waitFor(()->a.playerSheet().progress().domain().equals("water"),a,b);
                a.castSkill(2,0,0,140,300);
                Task201CharacterSessionTest.waitFor(()->a.skills().cooldowns().get(1)>0,a,b);
                assertTrue(b.requestAttributes());Task201CharacterSessionTest.waitFor(()->b.attributes()!=null,a,b);
                assertTrue(b.connected());assertNull(b.skills());assertNull(server.failure());
            }
        }
    }

    @Test void coopBackendRefusesSkillNegotiation()throws Exception{
        try(var server=new LocalSimulationServer(InetAddress.getLoopbackAddress(),0)){
            server.start();try(var socket=new Socket(InetAddress.getLoopbackAddress(),server.port())){
                socket.setSoTimeout(2000);Protocol.write(socket.getOutputStream(),new Protocol.Hello(7,"Skill Negada"));
                assertInstanceOf(Protocol.Error.class,Protocol.read(socket.getInputStream()));
            }
            assertNull(server.failure());
        }
    }

    @Test void lateJoinUsesGlobalEventTimeAndKeepsExistingCooldowns()throws Exception{
        var world=new RpgSessionWorld(List.of("reaper","human"),SessionCharacterSelectionTest.catalog(),true,20);
        world.enableRevisions(WorldContent.baseline());
        try(var server=new LocalSimulationServer(InetAddress.getLoopbackAddress(),0,world)){
            server.start();try(var a=connect(server,"Skill Antigo","reaper")){
                Task201CharacterSessionTest.ready(a);
                Task201CharacterSessionTest.waitFor(()->a.skills()!=null&&a.playerSheet()!=null,a);
                a.gameplay(GameplayProtocol.Operation.CHOOSE_DOMAIN,"water");
                Task201CharacterSessionTest.waitFor(()->a.playerSheet().progress().domain().equals("water"),a);
                a.castSkill(4,0,0,0,0);
                Task201CharacterSessionTest.waitFor(()->a.skills().cooldowns().get(3)>0,a);
                long initial=a.snapshot().tick();
                Task201CharacterSessionTest.waitFor(()->a.snapshot().tick()>initial+20,a);
                try(var b=connect(server,"Skill Recente","human")){
                    Task201CharacterSessionTest.ready(a,b);
                    Task201CharacterSessionTest.waitFor(()->b.skills()!=null&&b.playerSheet()!=null,a,b);
                    assertTrue(a.skills().cooldowns().get(3)>0,"Admissão não deve apagar recargas existentes");
                    b.gameplay(GameplayProtocol.Operation.CHOOSE_DOMAIN,"light");
                    Task201CharacterSessionTest.waitFor(()->b.playerSheet().progress().domain().equals("light"),a,b);
                    long joined=b.snapshot().tick();b.castSkill(2,0,0,0,0);
                    Task201CharacterSessionTest.waitFor(()->b.skills().cooldowns().get(1)>0
                            &&a.skills().events().stream().anyMatch(e->e.player()==b.id()&&e.skill().equals("light_2")),a,b);
                    var event=a.skills().events().stream().filter(e->e.player()==b.id()&&e.skill().equals("light_2")).findFirst().orElseThrow();
                    assertTrue(event.tick()>=joined,"Evento do recém-chegado deve usar o relógio global");
                    assertTrue(b.skills().cooldowns().get(1)<=com.projetok.rpg.RpgSkillCatalog.get("light_2").cooldownTicks());
                    assertNull(server.failure());
                }
            }
        }
    }
}
