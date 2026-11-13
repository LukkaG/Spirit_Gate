package com.projetok.prototype.rpg;

import com.projetok.multiplayer.Protocol;
import com.projetok.prototype.DebugSnapshot;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.*;
import javax.imageio.ImageIO;

/** Anima os mobs importados e ancora o Cero na boca, sem produzir dano no cliente. */
public final class MobSprites {
    public static final java.util.List<String> TYPES=java.util.List.of("guardiao","alado","rastreador","tecela","predador","sentinela");
    private final Map<String,BufferedImage> sheets=new HashMap<>();
    private final Map<String,BufferedImage> movementSheets=new HashMap<>();
    private final Map<Integer,Pose> poses=new HashMap<>();
    private final BufferedImage beam,charge,tip,impact;
    private long tick=-1;
    private static final class Pose {
        String type; float x,y,tx,ty; int row; long start=-100,walkStart; boolean attacking,moving;
    }

    /** Carrega os PNGs uma unica vez; dimensoes invalidas impedem uso silencioso de arte quebrada. */
    public MobSprites() {
        for(String type:TYPES) {
            sheets.put(type,load(type+"/completa_384x256.png",384,256));
            movementSheets.put(type,load(type+(type.equals("alado")?"/voar_384x256.png":"/andar_384x256.png"),384,256));
        }
        var fx=load("cero/cero_direita_256x256.png",256,256);
        charge=fx.getSubimage(0,0,256,64);tip=fx.getSubimage(0,64,256,64);beam=fx.getSubimage(0,128,256,64);impact=fx.getSubimage(0,192,256,64);
    }
    private static BufferedImage load(String path,int width,int height) {
        try(var in=MobSprites.class.getResourceAsStream("/rpg/mobs/"+path)) {
            if(in==null)throw new IOException("Sprite ausente: "+path);
            var image=ImageIO.read(in);
            if(image==null||image.getWidth()!=width||image.getHeight()!=height)throw new IOException("Dimensoes invalidas: "+path);
            return image;
        }catch(IOException error){throw new IllegalStateException("Falha ao carregar mob",error);}
    }
    /** Consome apenas estados confirmados; elimina atores antigos ao trocar de regiao. */
    public void update(Protocol.State state) {
        if(state.tick()<=tick)return;
        for(var actor:state.actors()) {
            String type=actor.name().toLowerCase(Locale.ROOT);
            if(!sheets.containsKey(type))continue;
            boolean known=poses.containsKey(actor.id());
            var p=poses.computeIfAbsent(actor.id(),id->new Pose());
            float dx=actor.x()-p.x,dy=actor.y()-p.y;
            boolean moving=known&&Math.hypot(dx,dy)>.1;
            if(moving&&(!p.moving||p.attacking))p.walkStart=state.tick();
            p.moving=moving;
            if(p.moving)p.row=row(dx,dy);
            boolean attacking=(actor.flags()&1)!=0&&actor.hp()>0;
            if(attacking&&(!p.attacking||(!type.equals("alado")&&!type.equals("sentinela")&&state.tick()-p.start>=36))) {
                p.start=state.tick();
                var target=state.actors().stream().filter(a->a.id()<1_000_000&&a.hp()>0)
                    .min(Comparator.comparingDouble(a->Math.hypot(a.x()-actor.x(),a.y()-actor.y()))).orElse(null);
                if(target!=null){p.tx=target.x();p.ty=target.y()-24;p.row=row(p.tx-actor.x(),p.ty-(actor.y()-40));}
            }
            p.type=type;p.x=actor.x();p.y=actor.y();p.attacking=attacking;
        }
        poses.keySet().retainAll(state.actors().stream().filter(a->a.hp()>0).map(Protocol.Actor::id).toList());tick=state.tick();
    }
    private static int row(float dx,float dy){return Math.abs(dx)>Math.abs(dy)?(dx<0?2:3):(dy<0?1:0);}
    /** Desenha em pixels nativos; o pivô de cada corpo fica no centro da base 64x64. */
    public boolean draw(Graphics2D original,DebugSnapshot.ActorView actor) {
        var p=poses.get(actor.id());if(p==null||!actor.alive())return false;
        long elapsed=(tick-p.start)*1000/30;
        boolean walking=p.moving&&!p.attacking;
        int column=p.attacking?(p.type.equals("sentinela")?attackColumn(elapsed):shortAttackColumn(elapsed)):
            walking?movementColumn(p.type,(tick-p.walkStart)*1000/30):0;
        var g=(Graphics2D)original.create();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            int x=Math.round(actor.x()),y=Math.round(actor.y());
            int pivotY=walking?59:60;
            g.drawImage((walking?movementSheets:sheets).get(p.type),x-32,y-pivotY,x+32,y-pivotY+64,column*64,p.row*64,column*64+64,p.row*64+64,null);
            g.setColor(Color.WHITE);g.drawString(actor.name(),x-30,y-65);
        }finally{g.dispose();}
        return true;
    }
    /** Respeita os cinco tempos do manifesto: 80, 240, 120, 180 e 100 ms. */
    static int attackColumn(long millis){return millis<80?1:millis<320?2:millis<440?3:millis<620?4:5;}
    /** Seis quadros em loop; voo a 80 ms e caminhada a 100 ms, conforme movimento.json. */
    static int movementColumn(String type,long millis){return (int)(millis/(type.equals("alado")?80:100)%6);}
    private static int shortAttackColumn(long millis){return millis<80?1:millis<220?2:millis<310?3:millis<460?4:millis<560?5:0;}
    /** A bola e a origem do feixe compartilham o mesmo ponto, sempre a frente da boca. */
    public void effects(Graphics2D original) {
        for(var p:poses.values())if(p.start>=0&&(p.type.equals("alado")||p.type.equals("sentinela"))) {
            long elapsed=(tick-p.start)*1000/30;int chargeMillis=p.type.equals("alado")?220:320;
            double ox=p.x+(p.row==2?-20:p.row==3?20:0),oy=p.y-40+(p.row==0?10:p.row==1?-12:0);
            double angle=Math.atan2(p.ty-oy,p.tx-ox),distance=Math.min(240,Math.hypot(p.tx-ox,p.ty-oy));
            long arrival=chargeMillis+(long)Math.ceil(distance/.8);
            if(elapsed>=arrival+320)continue;
            var g=(Graphics2D)original.create();
            try {
                g.translate(ox,oy);g.rotate(angle);
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                int frame=(int)(elapsed/80)%4;
                if(elapsed>=chargeMillis&&elapsed<arrival) {
                    double length=beamLength(elapsed-chargeMillis+320,distance);
                    // Recorta na origem: nenhum pixel do feixe pode surgir atras da bola.
                    var forward=(Graphics2D)g.create();
                    try {
                        forward.clip(new Rectangle(0,-32,(int)Math.ceil(length),64));
                        for(int x=0;x<length;x+=32)forward.drawImage(beam,x-32,-32,x+32,32,frame*64,0,frame*64+64,64,null);
                    }finally{forward.dispose();}
                    var head=(Graphics2D)g.create();
                    try {
                        head.clip(new Rectangle(0,-32,(int)Math.ceil(length)+32,64));
                        head.drawImage(tip,(int)length-32,-32,(int)length+32,32,frame*64,0,frame*64+64,64,null);
                    }finally{head.dispose();}
                }
                if(elapsed<arrival) {
                    int ball=(int)Math.min(3,elapsed*4/chargeMillis);
                    g.drawImage(charge,-32,-32,32,32,ball*64,0,ball*64+64,64,null);
                } else {
                    int hit=(int)Math.min(3,(elapsed-arrival)/80);
                    g.drawImage(impact,(int)distance-32,-32,(int)distance+32,32,hit*64,0,hit*64+64,64,null);
                }
            }finally{g.dispose();}
        }
    }
    /** Faz a ponta partir da bola e avancar, em vez de aparecer atras do corpo. */
    static double beamLength(long elapsed,double distance){return Math.max(0,Math.min(distance,(elapsed-320)*.8));}
}
