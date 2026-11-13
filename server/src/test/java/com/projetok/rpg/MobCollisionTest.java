package com.projetok.rpg;

import com.projetok.domain.entity.*;
import com.projetok.domain.movement.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Exercita contatos reais da IA e o movimento do alvo da arena. */
class MobCollisionTest {
    private final CollisionService.Space space=new CollisionService.Space(960,640,List.of(),List.of());
    @Test void meleeFindsEscapeAroundFiringSentryWithStationarySolidPlayer() {
        // 27 pontos de contato frontal e obliquo; o alvo nunca muda de direcao.
        for(int angle=120;angle<=240;angle+=15)for(int radius:new int[]{40,50,70}) {
            double a=Math.toRadians(angle);
            float x=300+(float)Math.cos(a)*radius,y=300+(float)Math.sin(a)*radius;
            var service=new EnemyService(RpgContent.builtin(),space,Map.of("a","mob_guardiao|forest|"+x+"|"+y,"b","mob_sentinela|forest|300|300"),r->space);
            var player=new GameEntity(1,"Alvo",new Position(460,300)){};
            var occupied=new CollisionService.Space(960,640,List.of(),List.of(new CollisionService.Body(player,12)));
            var melee=service.current("forest").get(0);var sentry=service.current("forest").get(1);
            for(int tick=1;tick<=240;tick++) {
                service.update("forest",tick,player.getPosition(),true,new MovementService(new CollisionService()),occupied,n->{},q->true,true);
                var p=melee.getPosition();assertTrue(Math.hypot(p.x()-300,p.y()-300)>=38-.001);
                assertTrue(Math.hypot(p.x()-460,p.y()-300)>=31-.001);
                assertEquals(new Position(300,300),sentry.getPosition(),"Sentinela deve continuar atirando parada");
            }
            assertTrue(Math.hypot(melee.getPosition().x()-460,melee.getPosition().y()-300)<=46.01,"Sem saida: angulo="+angle+" raio="+radius);
            assertEquals("ATTACK",melee.behavior);
        }
    }
    @Test void mobGoesAroundWallWithoutCrossingIt() {
        var wall=new CollisionService.Space(960,640,List.of(new CollisionService.Obstacle(380,260,160,60)),List.of());
        var service=new EnemyService(RpgContent.builtin(),wall,Map.of("a","mob_guardiao|forest|300|300"),r->wall);
        var mob=service.current("forest").get(0);var collision=new CollisionService();
        for(int tick=1;tick<=240;tick++) {
            service.update("forest",tick,new Position(570,300),true,new MovementService(collision),wall,n->{},q->true,true);
            assertTrue(collision.isFree(mob,mob.getPosition().x(),mob.getPosition().y(),19,wall));
        }
        assertTrue(mob.getPosition().x()>540,"Deve sair do contato frontal e alcancar o outro lado");
    }
    @Test void sealedPassageDoesNotAllowCrossingOrTeleport() {
        var wall=new CollisionService.Space(960,640,List.of(new CollisionService.Obstacle(380,0,40,640)),List.of());
        var service=new EnemyService(RpgContent.builtin(),wall,Map.of("a","mob_guardiao|forest|350|300"),r->wall);
        var mob=service.current("forest").get(0);
        assertTrue(MobNavigation.route(mob,new Position(450,300),wall).isEmpty());
        for(int tick=1;tick<=60;tick++)service.update("forest",tick,new Position(450,300),true,new MovementService(new CollisionService()),wall,n->{},q->true,true);
        assertTrue(mob.getPosition().x()<=361.001);
    }
    @Test void newMobMovesAtEightyAndCannotCrossAnotherMob() {
        var service=new EnemyService(RpgContent.builtin(),space,Map.of("a","mob_guardiao|forest|200|300","b","mob_predador|forest|260|300"),r->space);
        var blocker=service.current("forest").get(1);blocker.skillRootUntil=1000;
        var movement=new MovementService(new CollisionService());
        service.update("forest",1,new Position(400,300),true,movement,space,n->{},q->true,true);
        assertEquals(200+80/30f,service.current("forest").get(0).getPosition().x(),.001);
        for(int t=2;t<150;t++) {
            service.update("forest",t,new Position(400,300),true,movement,space,n->{},q->true,true);
            var pos=service.current("forest").get(0).getPosition();
            assertTrue(Math.hypot(pos.x()-260,pos.y()-300)>=38-.001);
        }
        var moving=service.current("forest").get(0);
        assertTrue(moving.getPosition().x()>260,"Contorna o bloqueio frontal sem atravessar o corpo");
        blocker.hp=0;
        for(int t=150;t<180;t++)service.update("forest",t,new Position(400,300),true,movement,space,n->{},q->true,true);
        assertTrue(moving.getPosition().x()>260,"Mob derrotado deixa passagem livre");
    }
    @Test void convergingMobsDoNotOverlapPlayerOrEachOther() {
        var service=new EnemyService(RpgContent.builtin(),space,Map.of("a","mob_guardiao|forest|300|280","b","mob_predador|forest|300|320","c","mob_tecela|forest|400|400"),r->space);
        var player=new GameEntity(1,"Alvo",new Position(450,300)){};
        var occupied=new CollisionService.Space(960,640,List.of(),List.of(new CollisionService.Body(player,12)));
        for(int t=1;t<=300;t++) {
            service.update("forest",t,player.getPosition(),true,new MovementService(new CollisionService()),occupied,n->{},q->true,true);
            var mobs=service.current("forest");
            for(int i=0;i<mobs.size();i++) {
                var a=mobs.get(i).getPosition();assertTrue(Math.hypot(a.x()-450,a.y()-300)>=31-.001);
                for(int j=i+1;j<mobs.size();j++){var b=mobs.get(j).getPosition();assertTrue(Math.hypot(a.x()-b.x(),a.y()-b.y())>=38-.001);}
            }
        }
    }
    @Test void previewPlayerCannotCrossWallOrLivingMob() throws Exception {
        var panel=new MobPlaytest();
        for(int t=0;t<100;t++)panel.movePlayer(0,-1);
        assertEquals(431,player(panel).y(),.001,"O Predador bloqueia o primeiro caminho");
        for(int t=0;t<30;t++)panel.movePlayer(1,0);
        for(int t=0;t<18;t++)panel.movePlayer(0,-1);
        for(int t=0;t<30;t++)panel.movePlayer(-1,0);
        for(int t=0;t<20;t++)panel.movePlayer(0,-1);
        assertEquals(332,player(panel).y(),.001);
        for(int t=0;t<55;t++)panel.movePlayer(-1,0);
        for(int t=0;t<17;t++)panel.movePlayer(0,1);
        for(int t=0;t<40;t++)panel.movePlayer(-1,0);
        assertTrue(player(panel).x()>=211-.001,"O alvo nao atravessa a Tecela em 180,400");
    }
    private Position player(MobPlaytest panel)throws Exception {
        var field=MobPlaytest.class.getDeclaredField("player");field.setAccessible(true);return (Position)field.get(panel);
    }
}
