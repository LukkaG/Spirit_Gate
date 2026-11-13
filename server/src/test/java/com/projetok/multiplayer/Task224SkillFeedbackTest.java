package com.projetok.multiplayer;

import com.projetok.rpg.RpgSkillCatalog;
import com.projetok.domain.movement.CollisionService;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Prévia e feedback nunca se tornam aceitação ou rejeição autoritativa fictícia. */
class Task224SkillFeedbackTest {
    private static final String HASH="a".repeat(64);
    private static Protocol.Actor actor(int id,float x,float y,int hp){return new Protocol.Actor(id,"Ator "+id,x,y,hp,100,1000,0,false);}
    private static SkillProtocol.State state(long tick,List<SkillProtocol.Event> events,int cooldown,int sentry){return new SkillProtocol.State(HASH,tick,events,List.of(cooldown,0,0,0,0),sentry,false);}

    @Test void targetKindsRejectNpcsDeadMobsAndMissingSelectionButAllowSupport(){
        var self=actor(1,100,300,80);var friend=actor(2,140,300,50);var dead=actor(2,140,300,0);
        assertFalse(NetworkSkillPreview.target(RpgSkillCatalog.get("water_1"),self,null,0,null).targetValid());
        assertFalse(NetworkSkillPreview.target(RpgSkillCatalog.get("water_1"),self,actor(2_000_001,140,300,100),2_000_001,null).targetValid());
        assertFalse(NetworkSkillPreview.target(RpgSkillCatalog.get("water_1"),self,actor(1_000_001,140,300,0),1_000_001,null).targetValid());
        assertFalse(NetworkSkillPreview.target(RpgSkillCatalog.get("water_1"),self,friend,2,null).targetValid());
        assertTrue(NetworkSkillPreview.target(RpgSkillCatalog.get("light_1"),self,friend,2,null).targetValid());
        assertTrue(NetworkSkillPreview.target(RpgSkillCatalog.get("light_1"),self,null,0,null).targetValid());
        assertTrue(NetworkSkillPreview.target(RpgSkillCatalog.get("light_3"),self,dead,2,null).targetValid());
        assertFalse(NetworkSkillPreview.target(RpgSkillCatalog.get("light_3"),self,friend,2,null).targetValid());
        assertFalse(NetworkSkillPreview.target(RpgSkillCatalog.get("light_3"),self,null,0,null).targetValid());
        assertFalse(NetworkSkillPreview.target(RpgSkillCatalog.get("light_1"),self,null,2,null).targetValid(),"ID sem ator não vira autocura");
    }
    @Test void distanceAndSightAreWarningsAndDoNotPretendToRejectTheCast(){
        var self=actor(1,100,300,80);var target=actor(1_000_001,200,300,100);
        var space=new CollisionService.Space(960,640,List.of(new CollisionService.Obstacle(150,250,1,100)),List.of());
        var blocked=NetworkSkillPreview.target(RpgSkillCatalog.get("water_1"),self,target,target.id(),space);
        assertTrue(blocked.targetValid());assertTrue(blocked.warning());assertEquals("LOS",blocked.badge());
        var far=actor(1_000_001,500,300,100);var distant=NetworkSkillPreview.target(RpgSkillCatalog.get("water_1"),self,far,far.id(),null);
        assertTrue(distant.targetValid());assertEquals("LONGE",distant.badge());assertTrue(distant.text().contains("prévia"));
        assertEquals(NetworkSkillPreview.Sight.UNKNOWN,NetworkSkillPreview.sight(self,200,300,null));
    }
    @Test void allSelfAndSmartcastSkillsIgnoreAnUnrelatedSelectedActor(){
        var self=actor(1,100,300,100);
        for(var skill:RpgSkillCatalog.all())if(List.of(RpgSkillCatalog.Targeting.SELF,RpgSkillCatalog.Targeting.LINE,RpgSkillCatalog.Targeting.CONE).contains(skill.targeting()))
            assertTrue(NetworkSkillPreview.target(skill,self,null,2_000_001,null).targetValid(),skill.id());
    }
    @Test void smartcastPreviewKeepsAllFourAuthoritativeFacingDirections(){
        for(float[] heading:new float[][]{{1,0},{-1,0},{0,1},{0,-1}}){
            var actor=new Protocol.Actor(1,"Direção",140,300,100,100,1000,0,false,heading[0],heading[1],0);
            var point=NetworkSkillControls.facingPoint(actor,100);
            assertEquals(140+heading[0]*100,point.x,.001);assertEquals(300+heading[1]*100,point.y,.001);
        }
    }
    @Test void groundBoundsAndUnknownOrObstructedGeometryAreExplicit(){
        var self=actor(1,100,300,100);var space=new CollisionService.Space(960,640,List.of(new CollisionService.Obstacle(150,250,1,100)),List.of());
        assertFalse(NetworkSkillPreview.ground(self,1000,300,300,space).targetValid());
        assertFalse(NetworkSkillPreview.ground(self,Float.NaN,300,300,space).targetValid());
        assertTrue(NetworkSkillPreview.ground(self,200,300,300,space).warning());
        assertTrue(NetworkSkillPreview.ground(self,200,300,300,null).text().contains("indisponível"));
        assertFalse(NetworkSkillPreview.ground(self,200,400,300,new CollisionService.Space(960,640,List.of(),List.of())).warning());
    }
    @Test void queuedIntentWaitsAndTimeoutIsUnknownNeverARejection(){
        var view=new NetworkSkillSubmission();var before=state(10,List.of(),0,0);
        view.sent(RpgSkillCatalog.get("water_1"),before,1,0,true);assertEquals(NetworkSkillSubmission.Status.WAITING,view.status());
        assertTrue(view.message().contains("aguardando"));view.refresh(before,3_000_000_000L);
        assertEquals(NetworkSkillSubmission.Status.UNKNOWN,view.status());assertTrue(view.message().contains("desconhecido"));
        assertFalse(view.message().contains("rejeitada"));
    }
    @Test void onlyNewMatchingAuthoritativeDataChangesTheObservedFeedback(){
        var view=new NetworkSkillSubmission();var old=new SkillProtocol.Event(1,9,1,"water_1",100,300,200,300);
        view.sent(RpgSkillCatalog.get("water_1"),state(10,List.of(old),0,0),1,0,true);
        view.refresh(state(11,List.of(old,new SkillProtocol.Event(2,11,2,"water_1",100,300,200,300)),0,0),1);
        assertEquals(NetworkSkillSubmission.Status.WAITING,view.status());
        var fresh=new SkillProtocol.Event(3,11,1,"water_1",100,300,200,300);view.refresh(state(11,List.of(old,fresh),0,0),2);
        assertEquals(NetworkSkillSubmission.Status.EVENT_RECEIVED,view.status());assertTrue(view.message().contains("Evento"));
        assertFalse(view.message().contains("aceita"));
    }
    @Test void cooldownIsObservedStateAndDisconnectClearPreventsCrossSessionMatches(){
        var view=new NetworkSkillSubmission();view.sent(RpgSkillCatalog.get("water_1"),state(10,List.of(),0,0),1,0,true);
        view.refresh(state(11,List.of(),45,0),1);assertEquals(NetworkSkillSubmission.Status.STATE_RECEIVED,view.status());
        assertTrue(view.message().contains("Recarga"));view.clear();assertEquals(NetworkSkillSubmission.Status.NONE,view.status());
        view.sent(RpgSkillCatalog.get("water_1"),state(11,List.of(),0,0),1,2,false);assertEquals(NetworkSkillSubmission.Status.NOT_SENT,view.status());
    }
    @Test void sentryStateDoesNotClaimWhetherAnExpiryOrCancelWasAccepted(){
        var view=new NetworkSkillSubmission();view.sent(RpgSkillCatalog.get("aetheri_2"),state(10,List.of(),0,3),1,0,true);
        view.refresh(state(11,List.of(),0,0),1);assertEquals(NetworkSkillSubmission.Status.STATE_RECEIVED,view.status());
        assertTrue(view.message().contains("não está mais ativa"));assertFalse(view.message().contains("cancelamento aceito"));
    }
}
