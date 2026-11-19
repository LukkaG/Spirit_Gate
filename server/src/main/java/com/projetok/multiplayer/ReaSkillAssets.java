package com.projetok.multiplayer;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import javax.imageio.ImageIO;
import com.projetok.rpg.RpgSkillCatalog;

/** Apresenta as fases REA sem transformar quadros de arte em eventos de combate. */
public final class ReaSkillAssets {
    private ReaSkillAssets(){}

    /** Mantém tempos individuais, linhas desenhadas e pivô originais de uma fase. */
    public record Animation(BufferedImage image,int cell,int rows,int[] durations,int pivotX,int pivotY,
                            boolean loop,String attachment) {
        public Animation {durations=durations.clone();}
        @Override public int[] durations(){return durations.clone();}
        /** Soma a duração visual, sem alterar a duração autoritativa da habilidade. */
        int duration(){return Arrays.stream(durations).sum();}
        /** Seleciona frames em ordem; só fases marcadas como loop podem reiniciar. */
        int frame(long elapsed,boolean reduced){
            if(reduced&&loop)return durations.length/2;
            long at=loop?Math.floorMod(elapsed,duration()):Math.max(0,elapsed);
            for(int i=0;i<durations.length;i++){if(at<durations[i])return i;at-=durations[i];}
            return durations.length-1;
        }
        /** Escolhe a linha N, NE, E, SE, S, SW, W, NW, sem girar ou espelhar a arte. */
        void draw(Graphics2D original,int frame,float x,float y,double angle){
            int row=rows==1?0:Math.floorMod((int)Math.round(angle/(Math.PI/4))+2,8);
            var g=(Graphics2D)original.create();
            try{
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
                int left=Math.round(x)-pivotX,top=Math.round(y)-pivotY;
                g.drawImage(image,left,top,left+cell,top+cell,frame*cell,row*cell,(frame+1)*cell,(row+1)*cell,null);
            }finally{g.dispose();}
        }
    }

    /** Carrega apenas o overlay opcional, validando caminhos e geometria antes de decodificar PNGs. */
    static Map<String,NetworkSkillPresentation.Clip> load(Path root)throws IOException{
        Path folder=root.resolve("assets/skills/rea-v1"),manifest=folder.resolve("manifest.properties");
        if(!Files.isRegularFile(manifest))return Map.of();
        if(Files.size(manifest)>256_000)throw new IOException("Manifesto REA excessivo");
        var p=new Properties(){@Override public synchronized Object put(Object key,Object value){
            if(containsKey(key))throw new IllegalArgumentException("Metadado REA duplicado: "+key);return super.put(key,value);
        }};
        var result=new LinkedHashMap<String,NetworkSkillPresentation.Clip>();
        try(var reader=Files.newBufferedReader(manifest)){p.load(reader);}
        catch(IllegalArgumentException error){throw new IOException("Manifesto REA inválido",error);}
        try{
            for(String key:new TreeSet<>(p.stringPropertyNames()))if(key.endsWith(".path")){
                String id=key.substring(0,key.length()-5),prefix=id+".";
                if(!id.matches("(?:water|wind|thunder|fire|spirit|aetheri|light|martial)_[1-5]\\.rea\\.[a-z0-9_]+"))throw new IOException("Fase REA inválida: "+id);
                Path path=root.resolve(p.getProperty(key)).normalize();
                if(!path.startsWith(folder)||!path.toRealPath().startsWith(folder.toRealPath())||Files.size(path)>16_000_000)throw new IOException("Caminho REA inválido");
                int cell=number(p,prefix+"cell",1,1024),frames=number(p,prefix+"frames",1,128),rows=number(p,prefix+"rows",1,8);
                if(rows!=1&&rows!=8)throw new IOException("Ordem de direções REA inválida");
                int[] durations=Arrays.stream(p.getProperty(prefix+"durations").split(",")).mapToInt(Integer::parseInt).toArray();
                if(durations.length!=frames||Arrays.stream(durations).anyMatch(ms->ms<1||ms>2000))throw new IOException("Tempos REA inválidos");
                try(var input=ImageIO.createImageInputStream(path.toFile())){
                    var readers=ImageIO.getImageReaders(input);if(!readers.hasNext())throw new IOException("PNG REA ilegível");
                    var imageReader=readers.next();
                    try{imageReader.setInput(input);if(imageReader.getWidth(0)>4096||imageReader.getHeight(0)>4096)throw new IOException("Atlas REA excessivo");}
                    finally{imageReader.dispose();}
                }
                var image=ImageIO.read(path.toFile());
                if(image==null||!image.getColorModel().hasAlpha()||image.getWidth()!=cell*frames||image.getHeight()!=cell*rows)throw new IOException("Geometria REA inválida: "+id);
                int px=number(p,prefix+"pivotX",0,cell),py=number(p,prefix+"pivotY",0,cell);
                var animation=new Animation(image,cell,rows,durations,px,py,Boolean.parseBoolean(p.getProperty(prefix+"loop")),p.getProperty(prefix+"attachment"));
                result.put(id,new NetworkSkillPresentation.Clip(image,cell,cell,frames,frames,Math.max(16,animation.duration()/frames),cell,cell,px,py,animation));
            }
            for(var definition:RpgSkillCatalog.all()){
                String id=definition.id();
                var base=find(result,id,primary(id));
                if(base==null)throw new IOException("Habilidade REA incompleta: "+id);
                result.put(id,base);
                var travel=find(result,id,"flight","travel","dash","rush","arrow_flight","flight_1","cut_1_flight","connecting_arc","advancing_tip","fissure_front");
                var impact=find(result,id,"impact","impact_1","cut_1_impact","arrow_impact","tip_impact","eruption","landing","net_appear","contact","contact_1","finish","wave_1");
                if(travel!=null)result.put(id+".travel",travel);
                if(impact!=null)result.put(id+".impact",impact);
            }
        }catch(IllegalArgumentException|NullPointerException error){throw new IOException("Metadados REA inválidos",error);}
        return result;
    }

    /** Valida limites numéricos sem corrigir silenciosamente metadados incorretos. */
    private static int number(Properties p,String key,int minimum,int maximum)throws IOException{
        int value=Integer.parseInt(p.getProperty(key));if(value<minimum||value>maximum)throw new IOException("Metadado REA fora dos limites: "+key);return value;
    }
    /** Define o ícone e o recorte principal de cada família, excluindo os corpos marciais. */
    private static String[] primary(String id){return switch(id){
        case "water_2"->new String[]{"cast"};
        case "water_3","water_4","fire_3","fire_5","spirit_1","spirit_5"->new String[]{"formation"};
        case "water_5"->new String[]{"rush"};
        case "wind_3","thunder_4"->new String[]{"dash"};
        case "wind_5"->new String[]{"self_pulse"};
        case "wind_4"->new String[]{"travel"};
        case "wind_2"->new String[]{"cut_1_flight"};
        case "thunder_2"->new String[]{"travel"};
        case "thunder_3"->new String[]{"connecting_arc"};
        case "thunder_5"->new String[]{"intangible_disturbance"};
        case "fire_2"->new String[]{"tile_open"};
        case "fire_4"->new String[]{"eruption"};
        case "spirit_4"->new String[]{"prison_appear"};
        case "aetheri_2"->new String[]{"stance_activate"};
        case "aetheri_3"->new String[]{"wave_1"};
        case "aetheri_5"->new String[]{"flight_1"};
        case "light_1"->new String[]{"heal"};
        case "light_2"->new String[]{"healing_wave"};
        case "light_3"->new String[]{"invocation"};
        case "light_4"->new String[]{"purification"};
        case "light_5"->new String[]{"superior_heal"};
        case "martial_1"->new String[]{"contact"};
        case "martial_2","martial_5"->new String[]{"contact_1"};
        case "martial_3"->new String[]{"rupture"};
        case "martial_4"->new String[]{"landing"};
        default->new String[]{"flight","launch"};
    };}
    /** Localiza uma fase pela prioridade explícita, sem depender da ordem do manifesto. */
    private static NetworkSkillPresentation.Clip find(Map<String,NetworkSkillPresentation.Clip> clips,String id,String... phases){
        for(String phase:phases){var clip=clips.get(id+".rea."+phase);if(clip!=null)return clip;}return null;
    }
    /** Duração do contato e da dissipação; não interfere com o voo nem agenda acertos. */
    static int impactDuration(Map<String,NetworkSkillPresentation.Clip> clips,String id){
        var impact=clips.get(id+".impact");var fade=find(clips,id,"dissipation");
        return (impact==null||impact.animation()==null?0:impact.animation().duration())+(fade==null?0:fade.animation().duration());
    }
    /** Garante espaço para fases inteiras de ações únicas, preservando tempos de áreas e voos. */
    static int duration(Map<String,NetworkSkillPresentation.Clip> clips,SkillProtocol.Event event,int previous,boolean travels){
        String id=event.skill();
        if(id.equals("light_3"))return java.util.stream.Stream.of("invocation","convergence","revival","dissipation").mapToInt(p->find(clips,id,p).animation().duration()).sum();
        if(id.equals("fire_4")&&Math.hypot(event.x()-event.fromX(),event.y()-event.fromY())<.001)
            return java.util.stream.Stream.of("eruption_charge","eruption","eruption_fade").mapToInt(p->find(clips,id,p).animation().duration()).sum();
        if(id.equals("martial_3"))return java.util.stream.Stream.of("rupture","settle","dissipation").mapToInt(p->find(clips,id,p).animation().duration()).sum();
        if(id.equals("water_2")||id.startsWith("light_"))return Math.max(previous,clips.get(id).animation().duration());
        if(id.equals("martial_4"))return Math.max(previous,NetworkSkillPresentation.BODY_SLAM_GRIP_MILLIS+find(clips,id,"landing").animation().duration());
        if(id.equals("spirit_2")&&Math.hypot(event.x()-event.fromX(),event.y()-event.fromY())<.001)return Math.max(previous,2000);
        return previous;
    }
    /** Desenha uma fase no tempo original, sem reiniciar fases de entrada ou encerramento. */
    private static void phase(Map<String,NetworkSkillPresentation.Clip> clips,Graphics2D g,String id,String phase,long at,float x,float y,double angle,boolean reduced){
        var clip=find(clips,id,phase);if(clip==null||at<0)return;
        var animation=clip.animation();if(!animation.loop()&&at>=animation.duration())return;
        animation.draw(g,animation.frame(at,reduced),x,y,angle);
    }
    /** Conserva entrada, sustentação e saída dentro da vida visual confirmada pelo executor. */
    private static void lifecycle(Map<String,NetworkSkillPresentation.Clip> clips,Graphics2D g,String id,String begin,String hold,String end,long elapsed,int duration,float x,float y,double angle,boolean reduced){
        var a=find(clips,id,begin);var b=find(clips,id,hold);var c=find(clips,id,end);
        int enter=a==null?0:a.animation().duration(),exit=c==null?0:c.animation().duration();
        double scale=Math.min(1,duration/(double)Math.max(1,enter+exit));
        int first=(int)(enter*scale),last=(int)(exit*scale);
        if(elapsed<first)phase(clips,g,id,begin,(long)(elapsed/scale),x,y,angle,reduced);
        else if(c!=null&&elapsed>=duration-last)phase(clips,g,id,end,(long)((elapsed-duration+last)/scale),x,y,angle,reduced);
        else if(b!=null)phase(clips,g,id,hold,elapsed-first,x,y,angle,reduced);
    }

    /** Substitui somente a apresentação das habilidades importadas; eventos e alvos seguem no servidor. */
    static boolean draw(Map<String,NetworkSkillPresentation.Clip> clips,Graphics2D g,SkillProtocol.Event event,long at,int duration,Map<Integer,Protocol.Actor> actors,double fallback,boolean reduced,boolean travels){
        return draw(clips,g,event,at,duration,actors,fallback,reduced,travels,1);
    }
    /** Recebe a posição da onda/contato na sequência local à habilidade, nunca no contador global da rede. */
    static boolean draw(Map<String,NetworkSkillPresentation.Clip> clips,Graphics2D g,SkillProtocol.Event event,long at,int duration,Map<Integer,Protocol.Actor> actors,double fallback,boolean reduced,boolean travels,int variant){
        String id=event.skill();var base=clips.get(id);if(base==null||base.animation()==null)return false;
        var definition=RpgSkillCatalog.get(id);var actor=actors.get(event.player());
        boolean moving=Math.hypot(event.x()-event.fromX(),event.y()-event.fromY())>.001;
        double angle=moving?Math.atan2(event.y()-event.fromY(),event.x()-event.fromX()):Double.isFinite(fallback)?fallback:actor==null?0:Math.atan2(actor.facingY(),actor.facingX());
        float x=event.x(),y=event.y();
        if(actor!=null&&(definition.targeting()==RpgSkillCatalog.Targeting.SELF||definition.mechanics().pattern()==RpgSkillCatalog.Pattern.CONE_COMBO)){x=actor.x();y=actor.y();}
        if(id.equals("fire_2")||id.equals("fire_4")){
            double distance=Math.hypot(event.x()-event.fromX(),event.y()-event.fromY());
            double reached=Math.min(distance,at*com.projetok.rpg.RpgSkillEngine.PROJECTILE_SPEED*30/1000.0);
            if(moving)for(int step=0;step<reached;step+=24){
                long born=(long)(step/com.projetok.rpg.RpgSkillEngine.PROJECTILE_SPEED*1000/30);
                lifecycle(clips,g,id,id.equals("fire_2")?"tile_open":"fissure_open",id.equals("fire_2")?"smolder_loop":"fissure_open",id.equals("fire_2")?"dissipation":"line_cooling",at-born,Math.max(1,duration-(int)born),
                        (float)(event.fromX()+Math.cos(angle)*step),(float)(event.fromY()+Math.sin(angle)*step),angle,reduced);
            }
            if(id.equals("fire_4")&&!moving)lifecycle(clips,g,id,"eruption_charge","eruption","eruption_fade",at,duration,x,y,angle,reduced);
            return true;
        }
        if(id.equals("aetheri_3")){
            String wave="wave_"+variant;
            var animation=find(clips,id,wave).animation();
            int contact=2; // Os dois primeiros quadros são queda; o terceiro é contato na arte entregue.
            long local=moving?Math.min(animation.durations[0]+animation.durations[1]-1,at*(animation.durations[0]+animation.durations[1])/400):at+animation.durations[0]+animation.durations[1];
            if(moving||animation.frame(local,false)>=contact)phase(clips,g,id,wave,local,x,y,0,reduced);
            return true;
        }
        if(id.equals("fire_5")){
            int interval=definition.mechanics().intervalTicks()*1000/30;
            boolean drawing=false;
            for(int wave=1;wave<=5;wave++){
                long local=at-(wave-1L)*interval;var a=find(clips,id,"wave_"+wave).animation();
                if(local>=0&&local<a.duration()){phase(clips,g,id,"wave_"+wave,local,x,y,0,reduced);drawing=true;}
            }
            if(!drawing)lifecycle(clips,g,id,"formation","target_boundary","dissipation",at,duration,x,y,0,reduced);
            return true;
        }
        if(id.equals("thunder_5")){
            for(int cut=1;cut<=4;cut++)phase(clips,g,id,"area_flash_"+cut,at-(cut-1)*70L,x,y,0,reduced);
            phase(clips,g,id,"reappear_sparks",at,x,y-28,0,reduced);return true;
        }
        String[] cycle=switch(id){
            case "water_3"->new String[]{"formation","prison_loop","release"};
            case "water_4","fire_3"->new String[]{"formation","vortex_loop","dissipation"};
            case "spirit_1"->new String[]{"formation","pool_loop","dissolution"};
            case "spirit_4"->new String[]{"prison_appear","prison_loop","prison_release"};
            case "spirit_5"->new String[]{"formation","ground_loop","restoration"};
            case "martial_3"->new String[]{"rupture","settle","dissipation"};
            case "spirit_2"->moving?null:new String[]{"net_appear","net_loop","net_release"};
            case "aetheri_2"->!moving&&!Double.isFinite(fallback)&&actor!=null&&Math.hypot(x-actor.x(),y-actor.y())<8?new String[]{"stance_activate","stance_loop","stance_cancel"}:null;
            default->null;
        };
        if(cycle!=null){lifecycle(clips,g,id,cycle[0],cycle[1],cycle[2],at,duration,x,y,angle,reduced);return true;}
        if(id.equals("light_3")){
            long local=at;for(String stage:new String[]{"invocation","convergence","revival","dissipation"}){
                var clip=find(clips,id,stage);if(local<clip.animation().duration()){phase(clips,g,id,stage,local,x,y,0,reduced);break;}local-=clip.animation().duration();
            }return true;
        }
        if(id.startsWith("light_")){
            boolean hostile=actors.values().stream().anyMatch(a->a.boss()&&Math.hypot(a.x()-event.x(),a.y()-event.y())<2);
            String stage=switch(id){case "light_1"->hostile?"hostile_impact":"heal";case "light_2"->"healing_wave";case "light_4"->"purification";default->hostile?"hostile_burst":"superior_heal";};
            phase(clips,g,id,stage,at,x,y-(hostile?28:0),0,reduced);return true;
        }
        if(id.equals("water_2")){phase(clips,g,id,"cast",at,x,y,angle,reduced);return true;}
        if(id.equals("martial_4")){
            if(!moving)phase(clips,g,id,"landing",Math.max(0,at-NetworkSkillPresentation.BODY_SLAM_GRIP_MILLIS),x,y,0,reduced);return true;
        }
        if(id.startsWith("martial_")){
            int interval=Math.max(1,definition.mechanics().intervalTicks()*1000/30);
            int index=id.equals("martial_5")?Math.min(6,(int)(at/interval)+1):variant;
            long local=id.equals("martial_5")?at-(index-1L)*interval:at;
            String contact=id.equals("martial_1")?"contact":"contact_"+index;
            phase(clips,g,id,contact,local,x,y-28,angle,reduced);
            phase(clips,g,id,"air_release",local,x,y-28,angle,reduced);
            if(local>=100&&local<250)phase(clips,g,id,"air_bullet",local-100,(float)(x+Math.cos(angle)*Math.min(40,(local-100)*.2)),(float)(y-28+Math.sin(angle)*Math.min(40,(local-100)*.2)),angle,reduced);
            if(local>=250)phase(clips,g,id,"air_dissipation",local-250,(float)(x+Math.cos(angle)*40),(float)(y-28+Math.sin(angle)*40),angle,reduced);
            return true;
        }
        if(id.equals("wind_5")){
            phase(clips,g,id,"self_pulse",at,x,y,0,reduced);
            if(moving){
                double length=Math.hypot(event.x()-event.fromX(),event.y()-event.fromY());
                float fraction=(float)Math.min(1,at*com.projetok.rpg.RpgSkillEngine.PROJECTILE_SPEED*30/(1000*length));
                float px=event.fromX()+(event.x()-event.fromX())*fraction,py=event.fromY()+(event.y()-event.fromY())*fraction;
                phase(clips,g,id,"inward_pull",at,px,py-28,angle,reduced);
                if(fraction>=1)phase(clips,g,id,"arrival_ring",at-(long)(length/com.projetok.rpg.RpgSkillEngine.PROJECTILE_SPEED*1000/30),x,y,0,reduced);
            }
            return true;
        }
        if(travels&&moving){
            var launch=find(clips,id,"launch","charge_release","arrow_release","launch_"+variant,"cut_"+variant+"_launch");
            if(launch!=null){var a=launch.animation();if(at<a.duration())a.draw(g,a.frame(at,reduced),event.fromX(),event.fromY()-28,angle);}
            float fraction=Math.min(1,at/(float)Math.max(1,Math.ceil(Math.hypot(event.x()-event.fromX(),event.y()-event.fromY())/com.projetok.rpg.RpgSkillEngine.PROJECTILE_SPEED*1000/30)));
            x=event.fromX()+(event.x()-event.fromX())*fraction;y=event.fromY()+(event.y()-event.fromY())*fraction;
            var travel=find(clips,id,"flight","travel","arrow_flight","flight_"+variant,"cut_"+variant+"_flight","connecting_arc");
            if(travel!=null){var a=travel.animation();a.draw(g,a.frame(at,reduced),x,y-28,angle);}
        }else{
            var impact=find(clips,id,"impact","arrow_impact","cut_"+variant+"_impact","contact","impact_1");
            if(impact!=null){var a=impact.animation();if(at<a.duration())a.draw(g,a.frame(at,reduced),x,y-28,angle);else phase(clips,g,id,"dissipation",at-a.duration(),x,y-28,angle,reduced);}
        }
        return true;
    }

    /** Usa os frames de rastro e avanço nas posições já observadas, após a pintura do personagem. */
    static void dash(Map<String,NetworkSkillPresentation.Clip> clips,Graphics2D g,String id,String stage,long at,float x,float y,double angle,boolean reduced){
        if(stage.equals("dash"))stage=id.equals("water_5")?"rush":"dash";
        var clip=find(clips,id,stage);if(clip==null)return;
        var a=clip.animation();a.draw(g,a.frame(at,reduced),x,y,angle);
    }
    /** A área intangível usa o próprio asset; a ocultação continua vinculada ao evento recebido. */
    static boolean lightning(Map<String,NetworkSkillPresentation.Clip> clips,Graphics2D g,SkillProtocol.Event event,long at,boolean reduced){
        var clip=find(clips,"thunder_5","intangible_disturbance");if(clip==null)return false;
        phase(clips,g,"thunder_5","intangible_disturbance",at,event.fromX(),event.fromY(),0,reduced);
        phase(clips,g,"thunder_5","vanish_sparks",at,event.fromX(),event.fromY()-28,0,reduced);return true;
    }
}
