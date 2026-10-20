package com.projetok.studio;
import com.projetok.rpg.EnvironmentCycle;
import java.awt.*;

/** Iluminacao e clima somente visuais, sem consulta ou alteracao da fisica. */
public final class EnvironmentPreview {
    /** Modos cadastrados e opcionais de apresentacao. */
    public enum Mode {OFF,DAY,NIGHT,DUSK,RAIN,FOG,INDOOR,CYCLE}
    private EnvironmentPreview(){}
    /** Pinta overlay barato limitado ao viewport, usando tempo externo monotono. */
    public static void draw(Graphics2D g,Rectangle area,Mode mode,long nanos){Color color=switch(mode){case NIGHT->new Color(8,15,55,145);case DUSK->new Color(135,55,25,75);case FOG->new Color(210,220,230,80);case INDOOR->new Color(8,5,15,175);case RAIN->new Color(20,35,65,60);case CYCLE->new Color(10,15,40,Math.round((1-EnvironmentCycle.light(Math.max(0,nanos/33_333_333),true))*255));default->null;};if(color!=null){g.setColor(color);g.fillRect(area.x,area.y,area.width,area.height);}if(mode==Mode.RAIN){g.setColor(new Color(175,200,230,120));int offset=(int)(Math.max(0,nanos)/20_000_000%48);int count=0;for(int y=area.y-48;y<area.y+area.height&&count<4096;y+=48)for(int x=area.x-48;x<area.x+area.width&&count++<4096;x+=48)g.drawLine(x+offset,y+offset,x+offset-5,y+offset+12);}}
    /** Configuracao salva no projeto, sem impacto no bundle de jogo. */
    public static Mode mode(WorldDocument d){try{return Mode.valueOf(d.metadata().getOrDefault("preview.environment","OFF"));}catch(IllegalArgumentException ex){return Mode.OFF;}}
}
