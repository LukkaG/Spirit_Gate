package com.projetok.multiplayer;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import java.util.function.LongSupplier;
import javax.imageio.ImageIO;

/** Anima somente habilidades confirmadas pelo servidor; nenhum efeito visual aplica dano. */
public final class NetworkSkillPresentation {
    /** Canvas 64, pivô dos pés 60: o centro visual fica 28 unidades acima do apoio. */
    public static final int PROJECTILE_CENTER_OFFSET=28;
    /** Tempo visual da pegada; não adia dano nem controle autoritativos. */
    static final int BODY_SLAM_GRIP_MILLIS=200;
    private static final int DASH_FADE_MILLIS=220;
    /** Recorte e pivô do efeito, independentes do atlas de personagens. */
    public record Clip(BufferedImage image,int frameWidth,int frameHeight,int columns,int frames,
                       int frameMillis,int drawWidth,int drawHeight,int pivotX,int pivotY) {
        /** Desenha um frame com alfa e amostragem por vizinho mais próximo. */
        void draw(Graphics2D original,int frame,float x,float y) {
            var g=(Graphics2D)original.create();
            try {
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                int sx=frame%columns*frameWidth,sy=frame/columns*frameHeight;
                int left=Math.round(x-pivotX*drawWidth/(float)frameWidth),top=Math.round(y-pivotY*drawHeight/(float)frameHeight);
                g.drawImage(image,left,top,left+drawWidth,top+drawHeight,sx,sy,sx+frameWidth,sy+frameHeight,null);
            } finally {g.dispose();}
        }
        /** Ícone do mesmo asset usado na animação, sem reler o PNG. */
        public javax.swing.Icon icon() {
            return new javax.swing.Icon() {
                public int getIconWidth(){return 24;}
                public int getIconHeight(){return 24;}
                public void paintIcon(Component component,Graphics graphics,int x,int y){
                    var g=(Graphics2D)graphics.create();
                    try {g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                        int frame=Math.min(frames-1,frames/2),sx=frame%columns*frameWidth,sy=frame/columns*frameHeight;
                        g.drawImage(image,x,y,x+24,y+24,sx,sy,sx+frameWidth,sy+frameHeight,null);
                    } finally {g.dispose();}
                }
            };
        }
    }
    private record Active(SkillProtocol.Event event,long began,int duration,double fallbackAngle) {
        Active(SkillProtocol.Event event,long began,int duration){this(event,began,duration,Double.NaN);}
    }
    private final LongSupplier clock;
    private final List<Active> active=new ArrayList<>();
    private final Map<String,DashTrail> dashes=new LinkedHashMap<>();
    private final Map<Integer,Active> lightningFields=new LinkedHashMap<>();
    /** Desenha uma pós-imagem usando os próprios sprites/cosméticos já carregados do personagem. */
    @FunctionalInterface public interface EchoRenderer {void draw(Graphics2D graphics,int player,float x,float y);}
    private EchoRenderer echoRenderer;
    public void echoRenderer(EchoRenderer renderer){echoRenderer=renderer;}
    /** Invisibilidade decorre de eventos aceitos; nenhum cliente decide o estado físico do servidor. */
    public boolean hidden(int player){expire();return lightningFields.containsKey(player);}
    private record TrailPoint(float x,float y,long at,double traveled) {
        TrailPoint(float x,float y,long at){this(x,y,at,0);}
    }
    /** Guarda somente posições já exibidas, sem antecipar o destino planejado pelo servidor. */
    private static final class DashTrail {
        final int player;final String skill;final long began;final int duration;
        final List<TrailPoint> points=new ArrayList<>();
        java.awt.geom.Point2D.Float terminal;boolean closed;
        DashTrail(SkillProtocol.Event event,long began,int duration){player=event.player();skill=event.skill();this.began=began;this.duration=duration;}
    }
    private Map<String,Clip> clips=Map.of();
    private Map<Integer,Protocol.Actor> actors=Map.of();
    private String revision="";
    private String region="";
    private long sequence;
    private boolean reducedMotion;

    public NetworkSkillPresentation(){this(()->System.nanoTime()/1_000_000);}
    NetworkSkillPresentation(LongSupplier clock){this.clock=Objects.requireNonNull(clock);}
    public void reducedMotion(boolean value){reducedMotion=value;}
    /** Portais descartam animações antigas mesmo se as duas regiões usam a mesma revisão. */
    public void region(String value){if(!region.equals(value)){active.clear();dashes.clear();lightningFields.clear();region=value;}}
    /**
     * Posições recebidas mantêm os efeitos self cast presos ao próprio personagem.
     * Morte ou saída confirmada descarta os efeitos cancelados pelo executor; um snapshot
     * anterior ao lançamento não pode cancelar um evento recebido antes dele pela rede.
     */
    public void actors(Protocol.State value){
        actors=value==null?Map.of():value.actors().stream().collect(java.util.stream.Collectors.toMap(Protocol.Actor::id,a->a));
        active.removeIf(effect->value==null||value.tick()>=effect.event().tick()
                &&(!actors.containsKey(effect.event().player())||actors.get(effect.event().player()).hp()==0));
        lightningFields.keySet().removeIf(id->!actors.containsKey(id)||actors.get(id).hp()==0);
        long now=clock.getAsLong();
        for(var iterator=dashes.values().iterator();iterator.hasNext();){
            var trail=iterator.next();var actor=actors.get(trail.player);
            if(actor==null||actor.hp()==0){iterator.remove();continue;}
            if(trail.closed)continue;
            var points=trail.points;var last=points.isEmpty()?null:points.get(points.size()-1);
            // Uma correção grande/teleporte não desenha uma faixa atravessando o mundo.
            if(last!=null&&Math.hypot(actor.x()-last.x(),actor.y()-last.y())>64){trail.closed=true;continue;}
            if(now>trail.began+trail.duration+200){trail.closed=true;continue;}
            if(trail.terminal!=null&&last!=null){
                double before=trail.terminal.distance(last.x(),last.y()),after=trail.terminal.distance(actor.x(),actor.y());
                if(before<2||after>before+8){trail.closed=true;continue;}
                double dx=actor.x()-last.x(),dy=actor.y()-last.y(),squared=dx*dx+dy*dy;
                if(squared>.001){
                    double amount=((trail.terminal.x-last.x())*dx+(trail.terminal.y-last.y())*dy)/squared;
                    if(amount>=0&&amount<=1&&java.awt.geom.Line2D.ptSegDist(last.x(),last.y(),actor.x(),actor.y(),trail.terminal.x,trail.terminal.y)<2){
                        points.add(new TrailPoint(trail.terminal.x,trail.terminal.y,now,last.traveled()+before));trail.closed=true;continue;
                    }
                }
            }
            if(last==null||Math.hypot(actor.x()-last.x(),actor.y()-last.y())>=.5)
                points.add(new TrailPoint(actor.x(),actor.y(),now,last==null?0:last.traveled()+Math.hypot(actor.x()-last.x(),actor.y()-last.y())));
            if(trail.terminal!=null&&trail.terminal.distance(actor.x(),actor.y())<2)trail.closed=true;
            while(points.size()>2&&points.get(1).at()<now-DASH_FADE_MILLIS)points.remove(0);
            while(points.size()>96)points.remove(0);
        }
    }
    /** Troca um conjunto previamente carregado, sem fazer IO na pintura. */
    public void clips(Map<String,Clip> value){clips=Map.copyOf(value);}
    public Clip clip(String id){return clips.get(id);}
    int activeCount(){expire();return active.size()+dashes.size()+lightningFields.size();}

    /** Deduplica sequências e abandona efeitos da revisão anterior. */
    public void update(SkillProtocol.State state) {
        if(state==null){active.clear();dashes.clear();lightningFields.clear();revision="";sequence=0;return;}
        if(!revision.equals(state.hash())){active.clear();dashes.clear();lightningFields.clear();revision=state.hash();sequence=0;}
        long now=clock.getAsLong();expire();
        for(var event:state.events())if(event.sequence()>sequence){
            sequence=event.sequence();
            double fallbackAngle=Double.NaN;
            var clip=clips.get(event.skill());
            int duration=clip==null?700:Math.max(400,clip.frames()*clip.frameMillis());
            var definition=com.projetok.rpg.RpgSkillCatalog.get(event.skill());
            if(event.skill().equals("thunder_5")){
                long age=Math.max(0,(state.tick()-event.tick())*1000/30);
                if(moving(event)){if(age<5000)lightningFields.put(event.player(),new Active(event,now-age,5000));}
                else {lightningFields.remove(event.player());if(age<650)active.add(new Active(event,now-age,650));}
                continue;
            }
            if(isDash(definition)){acceptDash(event,state.tick(),now,definition);continue;}
            if(travels(definition)&&moving(event))duration=flightMillis(event)+50;
            if(moving(event)&&Set.of("fire_2","fire_4").contains(event.skill()))duration+=definition.mechanics().durationTicks()*1000/30;
            if(travels(definition)&&!moving(event)){
                int matched=matchingLaunch(event,definition);
                if(matched>=0){
                    var previous=active.get(matched).event();
                    if(directionalImpactFallback(definition))fallbackAngle=Math.atan2(previous.y()-previous.fromY(),previous.x()-previous.fromX());
                    if(Set.of("fire_2","fire_4").contains(event.skill())){
                        // A área nasce no fim real do percurso e conserva o chão entre o caster e esse ponto.
                        var ended=new SkillProtocol.Event(previous.sequence(),previous.tick(),previous.player(),previous.skill(),previous.fromX(),previous.fromY(),event.x(),event.y());
                        int flight=flightMillis(ended);active.set(matched,new Active(ended,now-flight,flight+definition.mechanics().durationTicks()*1000/30));
                    } else if(stopsAtImpact(definition))active.remove(matched);
                }
                if(event.skill().equals("fire_2"))continue;
            }
            if(!travels(definition)&&(definition.targeting()==com.projetok.rpg.RpgSkillCatalog.Targeting.SELF||definition.targeting()==com.projetok.rpg.RpgSkillCatalog.Targeting.GROUND))
                duration=Math.max(duration,definition.mechanics().durationTicks()*1000/30);
            if(event.skill().equals("water_3")||event.skill().equals("spirit_4"))duration=Math.max(duration,2000);
            if(Set.of("spirit_1","spirit_5","fire_5","aetheri_3").contains(event.skill()))duration=Math.max(duration,1800);
            if(travels(definition)&&!moving(event)&&clips.containsKey(event.skill()+".impact")){
                var impact=clips.get(event.skill()+".impact");duration=impact.frames()*impact.frameMillis();
            }
            if(event.skill().equals("aetheri_3")){
                var impact=clips.get("aetheri_3.impact");
                duration=moving(event)?com.projetok.rpg.RpgSkillEngine.ARROW_RAIN_FALL_TICKS*1000/30:
                    impact==null?350:impact.frames()*impact.frameMillis();
            }
            if(definition.kit().equals("martial")&&(definition.slot()==1||definition.slot()==2)&&clip!=null)duration=clip.frames()*clip.frameMillis();
            if(definition.mechanics().pattern()==com.projetok.rpg.RpgSkillCatalog.Pattern.BODY_SLAM){
                var impact=clips.get(event.skill()+".impact");
                duration=moving(event)?BODY_SLAM_GRIP_MILLIS:BODY_SLAM_GRIP_MILLIS+(impact==null?350:impact.frames()*impact.frameMillis());
            }
            long age=Math.max(0,(state.tick()-event.tick())*1000/30);
            if(age<duration)active.add(new Active(event,now-age,duration,fallbackAngle));
        }
        while(active.size()>48)active.remove(0);
    }
    private void expire(){
        long now=clock.getAsLong();active.removeIf(effect->now-effect.began()>=effect.duration());
        dashes.values().removeIf(trail->now>trail.began+trail.duration+200+DASH_FADE_MILLIS
                ||trail.closed&&(trail.points.isEmpty()||now-trail.points.get(trail.points.size()-1).at()>=DASH_FADE_MILLIS));
        // Fusível visual para perda definitiva de evento; região, morte e desconexão limpam imediatamente.
        lightningFields.values().removeIf(field->now-field.began()>=field.duration());
    }

    /** Associa o início e o término da investida ao histórico observado do personagem. */
    private void acceptDash(SkillProtocol.Event event,long tick,long now,com.projetok.rpg.RpgSkillCatalog.Definition definition){
        String key=event.player()+":"+event.skill();var trail=dashes.get(key);
        if(trail!=null&&!moving(event)){trail.terminal=new java.awt.geom.Point2D.Float(event.x(),event.y());return;}
        if(trail!=null)return;
        int duration=Math.max(1,definition.mechanics().durationTicks())*1000/30;
        long age=Math.max(0,(tick-event.tick())*1000/30);
        if(age>=duration+200)return;
        // Um término sem lançamento conhecido não inventa um caminho de retorno.
        if(!moving(event))return;
        trail=new DashTrail(event,now-age,duration);
        if(age<=66)trail.points.add(new TrailPoint(event.fromX(),event.fromY(),now-age));
        dashes.put(key,trail);
        while(dashes.size()>16)dashes.remove(dashes.keySet().iterator().next());
    }

    private static boolean isDash(com.projetok.rpg.RpgSkillCatalog.Definition definition){return definition.mechanics().pattern()==com.projetok.rpg.RpgSkillCatalog.Pattern.DASH;}

    /** Reproduz os PNGs no mundo; o trajeto interpolado é apenas apresentação. */
    public void draw(Graphics2D original) {
        expire();long now=clock.getAsLong();drawLightningFields(original,now);drawDashes(original,now);
        for(var effect:active){
            var event=effect.event();var definition=com.projetok.rpg.RpgSkillCatalog.get(event.skill());
            boolean traveling=travels(definition)&&moving(event);
            boolean rain=event.skill().equals("aetheri_3");
            var clip=clips.get(traveling||rain&&moving(event)?event.skill()+".travel":event.skill()+".impact");
            if(clip==null&&!traveling&&travels(definition)&&!Set.of("fire_2","fire_4").contains(event.skill()))clip=clips.get(event.skill()+".travel");
            if(clip==null)clip=clips.get(event.skill());if(clip==null)continue;
            long elapsed=Math.max(0,now-effect.began());
            if(event.skill().equals("thunder_5")){drawFinalCuts(original,event,elapsed);continue;}
            if(definition.mechanics().pattern()==com.projetok.rpg.RpgSkillCatalog.Pattern.BODY_SLAM){
                boolean grip=moving(event);if(!grip&&elapsed<BODY_SLAM_GRIP_MILLIS)continue;
                var phase=clips.get(event.skill()+(grip?".travel":".impact"));if(phase==null)phase=clips.get(event.skill());if(phase==null)continue;
                long age=grip?elapsed:elapsed-BODY_SLAM_GRIP_MILLIS;
                int duration=grip?BODY_SLAM_GRIP_MILLIS:effect.duration()-BODY_SLAM_GRIP_MILLIS;
                phase.draw(original,lifecycleFrame(phase,age,duration,reducedMotion),event.x(),event.y()-(grip?PROJECTILE_CENTER_OFFSET:0));
                continue;
            }
            int frame=reducedMotion?clip.frames()/2:(int)(elapsed/clip.frameMillis())%clip.frames();
            if(!moving(event)&&clips.containsKey(event.skill()+".impact"))frame=Math.min(clip.frames()-1,(int)(elapsed/clip.frameMillis()));
            if(definition.kit().equals("martial")&&(definition.slot()==1||definition.slot()==2))frame=Math.min(clip.frames()-1,(int)(elapsed/clip.frameMillis()));
            if(definition.mechanics().pattern()==com.projetok.rpg.RpgSkillCatalog.Pattern.VORTEX||event.skill().equals("fire_4")&&!moving(event))
                frame=lifecycleFrame(clip,elapsed,effect.duration(),reducedMotion);
            if(event.skill().equals("water_2")||event.skill().equals("martial_3")||event.skill().equals("wind_5"))
                frame=lifecycleFrame(clip,elapsed,effect.duration(),reducedMotion);
            if(event.skill().equals("fire_5")){drawFireRain(original,clip,event,elapsed,reducedMotion);continue;}
            if(rain){drawArrowRain(original,clip,frame,event,elapsed);continue;}
            float x=event.x(),y=event.y();
            var actor=actors.get(event.player());
            if(actor!=null&&(!travels(definition)&&definition.targeting()==com.projetok.rpg.RpgSkillCatalog.Targeting.SELF||definition.mechanics().pattern()==com.projetok.rpg.RpgSkillCatalog.Pattern.DASH||definition.mechanics().pattern()==com.projetok.rpg.RpgSkillCatalog.Pattern.CONE_COMBO)){x=actor.x();y=actor.y();}
            if(traveling){
                float fraction=Math.min(1,elapsed/(float)flightMillis(event));
                x=event.fromX()+(x-event.fromX())*fraction;y=event.fromY()+(y-event.fromY())*fraction;
            }
            if(traveling&&Set.of("fire_2","fire_4").contains(event.skill())){
                var ground=clips.get(event.skill()+".ground");if(ground==null)ground=clips.get("fire_2");
                if(ground!=null)drawFissure(original,ground,event,x,y,elapsed,reducedMotion);
                // O golpe de espada nasce no corpo; a fissura avança no chão, sem uma bola de fogo flutuante.
                var sword=clips.get("fire_1");
                if(elapsed<120&&sword!=null){var cast=(Graphics2D)original.create();
                    try{cast.rotate(Math.atan2(event.y()-event.fromY(),event.x()-event.fromX()),event.fromX(),event.fromY()-PROJECTILE_CENTER_OFFSET);
                        sword.draw(cast,Math.min(sword.frames()-1,(int)(elapsed/sword.frameMillis())),event.fromX(),event.fromY()-PROJECTILE_CENTER_OFFSET);
                    }finally{cast.dispose();}
                }
                continue;
            }
            // Elevação apenas visual: colisões e fissuras conservam seu plano de apoio no servidor.
            if(traveling||travels(definition)&&!Set.of("fire_2","fire_4").contains(event.skill())
                    ||definition.mechanics().pattern()==com.projetok.rpg.RpgSkillCatalog.Pattern.CONE_COMBO
                    ||definition.kit().equals("martial")&&(definition.slot()==1||definition.slot()==2)
                    ||event.skill().equals("water_2"))y-=PROJECTILE_CENTER_OFFSET;
            frame=Math.min(clip.frames()-1,frame);
            // O vendaval conserva lâminas direcionais também no atlas próprio de dissipação.
            boolean directedImpact=!traveling&&Double.isFinite(effect.fallbackAngle())&&(!clips.containsKey(event.skill()+".impact")||event.skill().equals("wind_4"));
            if(traveling||directedImpact||definition.mechanics().pattern()==com.projetok.rpg.RpgSkillCatalog.Pattern.CONE_COMBO
                    ||definition.kit().equals("martial")&&(definition.slot()==1||definition.slot()==2)){
                var directed=(Graphics2D)original.create();
                try {directed.rotate(directedImpact?effect.fallbackAngle():Math.atan2(event.y()-event.fromY(),event.x()-event.fromX()),x,y);clip.draw(directed,frame,x,y);}
                finally {directed.dispose();}
            } else clip.draw(original,frame,x,y);
        }
    }
    /** Recorta cada textura ao segmento observado e deixa o corpo livre; jamais pinta o caminho futuro. */
    private void drawDashes(Graphics2D original,long now){
        for(var trail:dashes.values()){
            var clip=clips.get(trail.skill+".travel");if(clip==null)clip=clips.get(trail.skill);if(clip==null)continue;
            var actor=actors.get(trail.player);if(actor==null||actor.hp()==0)continue;
            if(echoRenderer!=null){
                double lastDistance=0;int count=0;
                for(int i=trail.points.size()-1;i>=0&&count<3;i--){var p=trail.points.get(i);long age=now-p.at();double distance=Math.hypot(actor.x()-p.x(),actor.y()-p.y());
                    if(age>=160||distance<22||distance<lastDistance+22)continue;lastDistance=distance;count++;
                    var echo=(Graphics2D)original.create();try{echo.setComposite(AlphaComposite.SrcOver.derive((float)(.28*(1-age/160.0))));echoRenderer.draw(echo,trail.player,p.x(),p.y());}finally{echo.dispose();}
                }
            }
            int frame=reducedMotion?clip.frames()/2:(int)(Math.max(0,now-trail.began)/clip.frameMillis())%clip.frames();
            for(int index=1;index<trail.points.size();index++){
                var from=trail.points.get(index-1);var to=trail.points.get(index);long age=now-to.at();
                int fade=reducedMotion?80:DASH_FADE_MILLIS;if(age>=fade)continue;
                double length=Math.hypot(to.x()-from.x(),to.y()-from.y());if(length<.5)continue;
                var area=new java.awt.geom.Area(new BasicStroke(28,BasicStroke.CAP_BUTT,BasicStroke.JOIN_ROUND).createStrokedShape(new java.awt.geom.Line2D.Float(from.x(),from.y()-PROJECTILE_CENTER_OFFSET,to.x(),to.y()-PROJECTILE_CENTER_OFFSET)));
                area.subtract(new java.awt.geom.Area(new java.awt.geom.Ellipse2D.Float(actor.x()-15,actor.y()-PROJECTILE_CENTER_OFFSET-15,30,30)));
                var g=(Graphics2D)original.create();
                try{
                    g.clip(area);g.setComposite(AlphaComposite.SrcOver.derive((float)Math.max(0,1-age/(double)fade)));
                    g.translate(from.x(),from.y()-PROJECTILE_CENTER_OFFSET);g.rotate(Math.atan2(to.y()-from.y(),to.x()-from.x()));
                    double scale=64.0/clip.drawWidth();
                    // O espaçamento pertence ao caminho inteiro, evitando reiniciar a textura a cada snapshot.
                    for(double point=16-from.traveled()%32-32;point<length+48;point+=32){var stamp=(Graphics2D)g.create();
                        try{stamp.translate(point,0);stamp.scale(scale,scale);clip.draw(stamp,frame,0,0);}finally{stamp.dispose();}
                    }
                }finally{g.dispose();}
            }
            if(!trail.closed&&now-trail.began<=trail.duration){
                var first=trail.points.isEmpty()?null:trail.points.get(0);var last=trail.points.isEmpty()?null:trail.points.get(trail.points.size()-1);
                if(first!=null&&last!=null&&last.traveled()>0){
                    var cut=clips.get(trail.skill.substring(0,trail.skill.indexOf('_'))+"_1");
                    if(cut!=null){var blade=(Graphics2D)original.create();double angle=Math.atan2(last.y()-first.y(),last.x()-first.x());
                        try{blade.translate(actor.x()+Math.cos(angle)*16,actor.y()-PROJECTILE_CENTER_OFFSET+Math.sin(angle)*16);blade.rotate(angle);double scale=Math.min(1,120.0/cut.drawWidth());blade.scale(scale,scale);
                            cut.draw(blade,lifecycleFrame(cut,Math.max(0,now-trail.began),trail.duration,reducedMotion),0,0);
                        }finally{blade.dispose();}
                    }
                }
            }
        }
    }
    /** Delimita a área durante a fase invisível, sem revelar sprite, nome ou sombra do lançador. */
    private void drawLightningFields(Graphics2D original,long now){
        for(var field:lightningFields.values()){
            float x=field.event().fromX(),y=field.event().fromY();int radius=com.projetok.rpg.RpgSkillCatalog.get("thunder_5").radius();
            var g=(Graphics2D)original.create();try{
                g.setColor(new Color(255,190,45,20));g.fill(new java.awt.geom.Ellipse2D.Float(x-radius,y-radius,radius*2,radius*2));
                g.setColor(new Color(255,211,91,170));g.setStroke(new BasicStroke(2,BasicStroke.CAP_BUTT,BasicStroke.JOIN_ROUND,1,new float[]{9,7},reducedMotion?0:(float)((now-field.began())%16)));
                g.draw(new java.awt.geom.Ellipse2D.Float(x-radius,y-radius,radius*2,radius*2));
            }finally{g.dispose();}
        }
    }
    /** Os quatro cortes cruzam o círculo; a área de dano é decidida inteira pelo servidor. */
    private void drawFinalCuts(Graphics2D original,SkillProtocol.Event event,long elapsed){
        var cut=clips.get("thunder_2");if(cut==null)cut=clips.get("thunder_5");if(cut==null)return;
        double[] angles={-.6,.6,0,Math.PI/2};int diameter=com.projetok.rpg.RpgSkillCatalog.get("thunder_5").radius()*2;
        for(int i=0;i<4;i++){
            long age=elapsed-i*70L;if(age<0||age>=400)continue;var g=(Graphics2D)original.create();
            try{g.translate(event.x(),event.y());g.rotate(angles[i]);g.scale(diameter/(double)cut.drawWidth(),diameter/(double)cut.drawWidth());g.setComposite(AlphaComposite.SrcOver.derive((float)(1-age/400.0)));
                cut.draw(g,lifecycleFrame(cut,age,400,reducedMotion),0,0);
            }finally{g.dispose();}
        }
    }
    /** Distribui nascimento, ápice e dissipação pela vida confirmada, sem reiniciar o ciclo. */
    private static int lifecycleFrame(Clip clip,long elapsed,int duration,boolean reduced){
        int frame=(int)Math.min(clip.frames()-1,elapsed*clip.frames()/Math.max(1,duration));
        // Acessibilidade mantém o ápice estável, conservando as fases de entrada e saída.
        if(reduced&&frame>=4&&frame<clip.frames()-4)frame=clip.frames()/2;
        return frame;
    }
    private static boolean moving(SkillProtocol.Event event){return Math.hypot(event.x()-event.fromX(),event.y()-event.fromY())>.001;}
    /** Em um leque, o contato pertence ao raio que passa pelo impacto, não à ordem de emissão. */
    private int matchingLaunch(SkillProtocol.Event impact,com.projetok.rpg.RpgSkillCatalog.Definition definition){
        int match=-1;double nearest=Double.POSITIVE_INFINITY;
        for(int index=0;index<active.size();index++){
            var launch=active.get(index).event();if(launch.player()!=impact.player()||!launch.skill().equals(impact.skill())||!moving(launch))continue;
            double length=Math.hypot(launch.x()-launch.fromX(),launch.y()-launch.fromY());
            double amount=Math.min(1,Math.max(0,impact.tick()-launch.tick())*com.projetok.rpg.RpgSkillEngine.PROJECTILE_SPEED/Math.max(.001,length));
            double predictedX=launch.fromX()+(launch.x()-launch.fromX())*amount,predictedY=launch.fromY()+(launch.y()-launch.fromY())*amount;
            double distance=java.awt.geom.Line2D.ptSegDist(launch.fromX(),launch.fromY(),launch.x(),launch.y(),impact.x(),impact.y())*4
                    +Math.hypot(predictedX-impact.x(),predictedY-impact.y());
            if(distance<nearest){nearest=distance;match=index;}
        }
        return match;
    }
    private static int flightMillis(SkillProtocol.Event event){return Math.max(1,(int)Math.ceil(Math.hypot(event.x()-event.fromX(),event.y()-event.fromY())/com.projetok.rpg.RpgSkillEngine.PROJECTILE_SPEED*1000/30));}
    /** Somente famílias que desenham o próprio projétil no contato precisam preservar sua orientação. */
    private static boolean directionalImpactFallback(com.projetok.rpg.RpgSkillCatalog.Definition definition){return switch(definition.mechanics().pattern()){
        case PROJECTILE,ARROW,SENTRY,LINE_PROJECTILE,CONE_VOLLEY,KNOCKBACK_CHECKPOINT->true;
        default->false;
    };}
    private static boolean stopsAtImpact(com.projetok.rpg.RpgSkillCatalog.Definition definition){return switch(definition.mechanics().pattern()){
        case PROJECTILE,ARROW,SENTRY,CONE_VOLLEY,CHAIN->true;
        case LINE_PROJECTILE,WEB,BEAM,KNOCKBACK_CHECKPOINT->true;
        case MULTI_HIT->definition.id().equals("wind_2");
        default->false;
    };}
    private static boolean travels(com.projetok.rpg.RpgSkillCatalog.Definition definition){return switch(definition.mechanics().pattern()){
        case PROJECTILE,ARROW,LINE_PROJECTILE,WEB,BEAM,FISSURE,FISSURE_ERUPTION,CONE_VOLLEY,SENTRY,CHAIN,KNOCKBACK_CHECKPOINT,CHECKPOINT_PULL->true;
        case MULTI_HIT->definition.id().equals("wind_2");
        default->false;
    };}
    /** Cinco ondas visuais acompanham os pulsos existentes; cada X nasce e se dissipa sem loop. */
    private static void drawFireRain(Graphics2D original,Clip clip,SkillProtocol.Event event,long elapsed,boolean reduced){
        var mechanics=com.projetok.rpg.RpgSkillCatalog.get("fire_5").mechanics();
        int interval=mechanics.intervalTicks()*1000/30;
        int[] dx={0,-52,52,-38,38},dy={0,-35,35,45,-45};
        for(int wave=0;wave<mechanics.hits();wave++)for(int index=0;index<dx.length;index++){
            long age=elapsed-(long)wave*interval-index*12L;
            int duration=clip.frames()*clip.frameMillis();
            if(age<0||age>=duration)continue;
            int frame=lifecycleFrame(clip,age,duration,reduced);
            clip.draw(original,frame,event.x()+dx[index],event.y()+dy[index]);
        }
    }
    /** A fissura revela somente o chão já percorrido pela ponta, usando os próprios frames do atlas. */
    private static void drawFissure(Graphics2D original,Clip ground,SkillProtocol.Event event,float x,float y,long elapsed,boolean reduced){
        var g=(Graphics2D)original.create();
        try {double length=Math.hypot(x-event.fromX(),y-event.fromY());g.translate(event.fromX(),event.fromY());g.rotate(Math.atan2(event.y()-event.fromY(),event.x()-event.fromX()));
            for(int step=0;step<length;step+=24){
                var section=(Graphics2D)g.create();
                try{
                    section.clip(new java.awt.geom.Rectangle2D.Double(step,-ground.drawHeight()/2.0,Math.min(24,length-step),ground.drawHeight()));
                    long born=(long)(step/com.projetok.rpg.RpgSkillEngine.PROJECTILE_SPEED*1000/30);
                    int frame=reduced?ground.frames()/2:Math.min(ground.frames()-1,(int)(Math.max(0,elapsed-born)/ground.frameMillis()));
                    for(int tile=step/ground.drawWidth()*ground.drawWidth();tile<Math.min(length,step+24);tile+=ground.drawWidth())ground.draw(section,frame,tile,0);
                }finally{section.dispose();}
            }
        } finally {g.dispose();}
    }

    /** Cinco flechas por onda chegam aos mesmos apoios onde seus impactos dissipam a energia. */
    private static void drawArrowRain(Graphics2D original,Clip clip,int frame,SkillProtocol.Event event,long elapsed){
        int[] offsetX={-64,-32,0,32,64},offsetY={-20,20,0,-20,20};
        float fraction=Math.min(1,elapsed/(com.projetok.rpg.RpgSkillEngine.ARROW_RAIN_FALL_TICKS*1000f/30));
        for(int i=0;i<offsetX.length;i++){
            float x=event.x()+offsetX[i],y=event.y()+offsetY[i];
            if(moving(event))y-=160*(1-fraction);
            var g=(Graphics2D)original.create();
            try{if(moving(event))g.rotate(Math.PI/2,x,y);clip.draw(g,frame,x,y);}finally{g.dispose();}
        }
    }

    /** Carrega os 40 recortes locais, recusando caminhos externos e geometrias inválidas. */
    public static Map<String,Clip> load(Path root) throws IOException {
        Path absolute=root.toAbsolutePath().normalize(),folder=absolute.resolve("assets/skills");
        Path manifest=folder.resolve("manifest.properties");
        if(Files.size(manifest)>256_000)throw new IOException("Manifesto de habilidades excessivo");
        Properties properties=readProperties(manifest);
        Path refined=folder.resolve("refined-v2/manifest.properties");
        if(Files.isRegularFile(refined)){var combined=new Properties();combined.putAll(properties);combined.putAll(readProperties(refined));properties=combined;}
        Path refinedV3=folder.resolve("refined-v3/manifest.properties");
        if(Files.isRegularFile(refinedV3)){var combined=new Properties();combined.putAll(properties);combined.putAll(readProperties(refinedV3));properties=combined;}
        Path refinedV4=folder.resolve("refined-v4/manifest.properties");
        if(Files.isRegularFile(refinedV4)){var combined=new Properties();combined.putAll(properties);combined.putAll(readProperties(refinedV4));properties=combined;}
        Path refinedV5=folder.resolve("refined-v5/manifest.properties");
        if(Files.isRegularFile(refinedV5)){var combined=new Properties();combined.putAll(properties);combined.putAll(readProperties(refinedV5));properties=combined;}
        Path refinedV6=folder.resolve("refined-v6/manifest.properties");
        if(Files.isRegularFile(refinedV6)){var combined=new Properties();combined.putAll(properties);combined.putAll(readProperties(refinedV6));properties=combined;}
        Path refinedV7=folder.resolve("refined-v7/manifest.properties");
        if(Files.isRegularFile(refinedV7)){var combined=new Properties();combined.putAll(properties);combined.putAll(readProperties(refinedV7));properties=combined;}
        Path refinedV8=folder.resolve("refined-v8/manifest.properties");
        if(Files.isRegularFile(refinedV8)){var combined=new Properties();combined.putAll(properties);combined.putAll(readProperties(refinedV8));properties=combined;}
        var loaded=new LinkedHashMap<String,Clip>();
        var ids=new ArrayList<String>();
        for(String kit:List.of("water","wind","thunder","fire","spirit","aetheri","light","martial"))for(int slot=1;slot<=5;slot++)ids.add(kit+"_"+slot);
        properties.stringPropertyNames().stream().filter(key->key.matches("(?:water|wind|thunder|fire|spirit|aetheri|light|martial)_[1-5]\\.(?:travel|ground|impact)\\.path")).map(key->key.substring(0,key.length()-5)).sorted().forEach(ids::add);
        try {
            for(String id:ids){
                String prefix=id+".";
                String relative=properties.getProperty(prefix+"path",properties.getProperty(prefix+"image"));
                if(relative==null)throw new IOException("Imagem ausente: "+id);
                Path path=absolute.resolve(relative).normalize();
                if(!path.startsWith(folder)||!path.toRealPath().startsWith(folder.toRealPath())||Files.size(path)>16_000_000)throw new IOException("Caminho de efeito inválido: "+id);
                try(var input=ImageIO.createImageInputStream(path.toFile())){
                    var readers=ImageIO.getImageReaders(input);if(!readers.hasNext())throw new IOException("PNG ilegível: "+id);
                    var reader=readers.next();
                    try {reader.setInput(input);if(reader.getWidth(0)>4096||reader.getHeight(0)>4096)throw new IOException("Imagem de efeito excessiva: "+id);}
                    finally {reader.dispose();}
                }
                var bitmap=ImageIO.read(path.toFile());if(bitmap==null)throw new IOException("PNG ilegível: "+id);
                int width=number(properties,prefix+"frameWidth",1,1024),height=number(properties,prefix+"frameHeight",1,1024);
                int frames=number(properties,prefix+"frames",1,128),columns=number(properties,prefix+"columns",1,128);
                int millis=number(properties,prefix+"frameMillis",16,2000),drawWidth=number(properties,prefix+"drawWidth",1,1024),drawHeight=number(properties,prefix+"drawHeight",1,1024);
                int pivotX=number(properties,prefix+"pivotX",0,width),pivotY=number(properties,prefix+"pivotY",0,height);
                if((long)columns*width>bitmap.getWidth()||((frames+columns-1L)/columns)*height>bitmap.getHeight())throw new IOException("Frames fora do atlas: "+id);
                loaded.put(id,new Clip(bitmap,width,height,columns,frames,millis,drawWidth,drawHeight,pivotX,pivotY));
            }
        } catch(IllegalArgumentException error){throw new IOException("Metadados de efeito inválidos",error);}
        return Map.copyOf(loaded);
    }
    /** Cada manifesto recusa duplicação; o V2 pode substituir explicitamente metadados do V1. */
    private static Properties readProperties(Path manifest)throws IOException {
        if(Files.size(manifest)>256_000)throw new IOException("Manifesto de habilidades excessivo");
        var properties=new Properties(){@Override public synchronized Object put(Object key,Object value){
            if(containsKey(key))throw new IllegalArgumentException("Metadado de habilidade duplicado");return super.put(key,value);
        }};
        try(var reader=Files.newBufferedReader(manifest)){properties.load(reader);}
        catch(IllegalArgumentException error){throw new IOException("Manifesto de habilidades inválido",error);}
        return properties;
    }
    private static int number(Properties p,String key,int minimum,int maximum)throws IOException {
        int value=Integer.parseInt(p.getProperty(key));if(value<minimum||value>maximum)throw new IOException("Metadado fora dos limites: "+key);return value;
    }
}
