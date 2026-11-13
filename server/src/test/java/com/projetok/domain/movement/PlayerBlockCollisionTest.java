package com.projetok.domain.movement;

import com.projetok.domain.entity.GameEntity;
import com.projetok.domain.entity.Position;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Contatos fracionarios e blocos de apoio nao podem prender nem atravessar jogadores. */
class PlayerBlockCollisionTest {
    private final CollisionService collisions=new CollisionService();
    private GameEntity actor(int id,float x,float y){return new GameEntity(id,"Jogador",new Position(x,y)){};}
    private CollisionService.Space space(GameEntity other){return new CollisionService.Space(960,640,List.of(),List.of(CollisionService.Body.block(other,12)));}

    @Test void contatoCircularArredondadoPermiteRecuarSemTravar() {
        // O float historico arredonda d² para 576; a varredura double encontra d²=575.999972.
        var moving=actor(1,116.00000762939453f,200.01840209960938f);
        var other=actor(2,140,200);
        var context=new CollisionService.Space(960,640,List.of(),List.of(new CollisionService.Body(other,12)));
        var before=moving.getPosition();
        assertTrue(collisions.isFree(moving,before.x(),before.y(),12,context));
        assertTrue(Math.pow((double)before.x()-140,2)+Math.pow((double)before.y()-200,2)<576);
        collisions.resolveContinuous(moving,-7,0,12,context);
        assertEquals(before.x()-7,moving.getPosition().x(),.0001,"Sair do contato nao pode ficar preso por ULP");
    }

    @Test void blocoImpedeTunelamentoEmAmbosOsEixosESentidos() {
        var other=actor(2,400,300);
        for(int axis=0;axis<2;axis++)for(int sign:new int[]{-1,1}) {
            var moving=actor(1,axis==0?400-sign*100:400,axis==1?300-sign*100:300);
            collisions.resolveContinuous(moving,axis==0?sign*300:0,axis==1?sign*300:0,12,space(other));
            assertEquals(axis==0?400-sign*24:400,moving.getPosition().x());
            assertEquals(axis==1?300-sign*24:300,moving.getPosition().y());
            assertTrue(collisions.isFree(moving,moving.getPosition().x(),moving.getPosition().y(),12,space(other)));
        }
    }

    @Test void todaFaceFracionariaPermiteDeslizarERecuarSemPenetrar() {
        var other=actor(2,400.125f,300.375f);
        var context=space(other);
        for(int pixel=0;pixel<240;pixel++) {
            float y=other.getPosition().y()-11.95f+pixel*.1f;
            var moving=actor(1,other.getPosition().x()-24,y);
            float x=moving.getPosition().x();
            collisions.resolveContinuous(moving,7,1,12,context);
            assertEquals(x,moving.getPosition().x(),"Bloco deve impedir pressao lateral");
            assertEquals(y+1,moving.getPosition().y(),.0001,"Tangente deve continuar livre");
            assertTrue(collisions.canTraverseSegment(moving,new Position(x,y),moving.getPosition(),12,context));
            collisions.resolveContinuous(moving,-7,0,12,context);
            assertEquals(x-7,moving.getPosition().x(),.0001,"Soltar o contato precisa ser imediato");
        }
        assertEquals(new Position(400.125f,300.375f),other.getPosition());
    }

    @Test void quinaDoBlocoEPassagemEstreitaContinuamSolidas() {
        var other=actor(2,400,300);var context=space(other);
        var moving=actor(1,380,280);
        assertFalse(collisions.isFree(moving,380,280,12,context),"A quina e solida mesmo fora do antigo circulo");
        moving.setPosition(new Position(376,276));
        assertTrue(collisions.canTraverseSegment(moving,new Position(376,276),new Position(376,320),12,context));
        assertFalse(collisions.canTraverseSegment(moving,new Position(376,276),new Position(424,324),12,context));
        var upper=actor(3,400,348);
        var corridor=new CollisionService.Space(960,640,List.of(),List.of(CollisionService.Body.block(other,12),CollisionService.Body.block(upper,12)));
        moving.setPosition(new Position(350,324));
        collisions.resolveContinuous(moving,100,0,12,corridor);
        assertEquals(450,moving.getPosition().x(),"Passagem exata de 24 unidades admite tangencia");
        upper.setPosition(new Position(400,347.9f));moving.setPosition(new Position(350,324));
        collisions.resolveContinuous(moving,100,0,12,corridor);
        assertEquals(376,moving.getPosition().x(),"Passagem menor nao pode ser forcada");
    }
}
