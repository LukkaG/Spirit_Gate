package com.projetok.prototype.rpg;

import com.projetok.multiplayer.Protocol;
import com.projetok.prototype.DebugSnapshot;
import java.awt.image.BufferedImage;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Valida assets reais, tempos do manifesto e limite traseiro do feixe. */
class MobSpritesTest {
    @Test void movementUsesAuthoredDirectionsTimingAndReturnsToIdle() throws Exception {
        for(String type:MobSprites.TYPES)for(int row=0;row<4;row++) {
            var sprites=new MobSprites();String name=Character.toUpperCase(type.charAt(0))+type.substring(1);
            float dx=row==2?-1:row==3?1:0,dy=row==1?-1:row==0?1:0;
            sprites.update(state(1,name,64,120,0));
            sprites.update(state(2,name,64+dx,120+dy,0));
            sprites.update(state(5,name,64+2*dx,120+2*dy,0));
            assertFrame(sprites,type,name,64+2*dx,120+2*dy,row,1,true);
            sprites.update(state(6,name,64+2*dx,120+2*dy,0));
            assertFrame(sprites,type,name,64+2*dx,120+2*dy,row,0,false);
            sprites.update(state(7,name,64+3*dx,120+3*dy,1));
            assertFrame(sprites,type,name,64+3*dx,120+3*dy,row,1,false);
        }
        assertEquals(5,MobSprites.movementColumn("guardiao",599));
        assertEquals(0,MobSprites.movementColumn("guardiao",600));
        assertEquals(5,MobSprites.movementColumn("alado",479));
        assertEquals(0,MobSprites.movementColumn("alado",480));
    }
    private Protocol.State state(long tick,String name,float x,float y,int flags) {
        return new Protocol.State(tick,List.of(new Protocol.Actor(1_000_100,name,x,y,100,100,0,0,true,1,0,flags)));
    }
    /** Compara o quadro desenhado com o PNG original, incluindo pivo, direcao e prioridade do ataque. */
    private void assertFrame(MobSprites sprites,String type,String name,float x,float y,int row,int column,boolean walking) throws Exception {
        String file=walking?(type.equals("alado")?"voar":"andar")+"_384x256.png":"completa_384x256.png";
        java.awt.image.BufferedImage source;
        try(var in=getClass().getResourceAsStream("/rpg/mobs/"+type+"/"+file)){source=javax.imageio.ImageIO.read(in);}
        var expected=new BufferedImage(64,64,BufferedImage.TYPE_INT_ARGB);var g=expected.createGraphics();
        try{g.drawImage(source.getSubimage(column*64,row*64,64,64),0,0,null);}finally{g.dispose();}
        var actual=new BufferedImage(160,200,BufferedImage.TYPE_INT_ARGB);g=actual.createGraphics();
        try{assertTrue(sprites.draw(g,new DebugSnapshot.ActorView(1_000_100,name,x,y,19,100,100,false,true)));}finally{g.dispose();}
        assertArrayEquals(expected.getRGB(0,0,64,64,null,0,64),actual.getRGB((int)x-32,(int)y-(walking?59:60),64,64,null,0,64),type+" direcao "+row);
    }
    @Test void allSixAssetsRenderAndCeroStartsAtTheBall() {
        var sprites=new MobSprites();var image=new BufferedImage(960,640,BufferedImage.TYPE_INT_ARGB);
        var actors=new ArrayList<Protocol.Actor>();actors.add(new Protocol.Actor(1,"Teste",400,184,100,100,0,0,false));
        for(int i=0;i<MobSprites.TYPES.size();i++){
            String type=MobSprites.TYPES.get(i);String name=Character.toUpperCase(type.charAt(0))+type.substring(1);
            actors.add(new Protocol.Actor(1_000_100+i,name,200,200,100,100,0,0,true,1,0,1));
        }
        sprites.update(new Protocol.State(1,actors));
        var g=image.createGraphics();
        try{for(var a:actors.subList(1,actors.size()))assertTrue(sprites.draw(g,new DebugSnapshot.ActorView(a.id(),a.name(),a.x(),a.y(),19,100,100,false,true)));}
        finally{g.dispose();}
        sprites.update(new Protocol.State(12,actors));
        var effects=new BufferedImage(960,640,BufferedImage.TYPE_INT_ARGB);g=effects.createGraphics();
        try{sprites.effects(g);}finally{g.dispose();}
        int visible=0;
        for(int y=0;y<640;y++)for(int x=0;x<960;x++)if((effects.getRGB(x,y)>>>24)!=0){visible++;assertTrue(x>=188,"Nenhum efeito pode nascer atras da bola (origem x=220)");}
        assertTrue(visible>0);
        assertEquals(0,MobSprites.beamLength(320,240));assertEquals(64,MobSprites.beamLength(400,240));assertEquals(240,MobSprites.beamLength(700,240));
        assertEquals(2,MobSprites.attackColumn(319));assertEquals(3,MobSprites.attackColumn(320));
    }
    @Test void arenaPaintsWithoutDisplayOrExternalServices() throws Exception {
        javax.swing.SwingUtilities.invokeAndWait(()->{
            var panel=new com.projetok.rpg.MobPlaytest();panel.setSize(960,640);
            var image=new BufferedImage(960,640,BufferedImage.TYPE_INT_ARGB);var g=image.createGraphics();
            try{panel.paint(g);}finally{g.dispose();}
        });
    }
}
