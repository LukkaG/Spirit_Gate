package com.projetok.multiplayer;

import com.projetok.rpg.*;
import com.projetok.domain.entity.*;
import com.projetok.domain.movement.CollisionService;
import java.util.*;
import com.projetok.prototype.rpg.CosmeticCatalog;

/** Região compartilhada experimental; movimento usa RpgWorld no servidor, sem campanha pessoal. */
public final class RpgSessionWorld implements SessionSimulation {
    private final List<String> races;
    private final CosmeticCatalog cosmetics;
    private final Map<Integer,Session> sessions=new TreeMap<>();
    private final CollisionService.Space geometry=new CollisionService.Space(960,640,List.of(new CollisionService.Obstacle(380,180,120,120)),List.of());
    private long tick;
    private static final class Session {
        final RpgWorld world;final GameEntity proxy;final int seat;
        long sequence,inputTick,commandTick=-1;int commands;float x,y,fx=1,fy;
        CosmeticCatalog.Selection appearance=CosmeticCatalog.Selection.none();
        Session(RpgWorld world,GameEntity proxy,int seat){this.world=world;this.proxy=proxy;this.seat=seat;}
    }
    public RpgSessionWorld(){this(List.of("reaper","human"));}
    /** Raças são configuração explícita do servidor, não atributos enviados pelo cliente. */
    public RpgSessionWorld(List<String> races){this(races,null);}
    public RpgSessionWorld(List<String> races,CosmeticCatalog cosmetics){this.races=List.copyOf(races);this.cosmetics=cosmetics;if(races.size()!=2)throw new IllegalArgumentException("Dois perfis sintéticos obrigatórios");races.forEach(CharacterRaces::get);}
    public boolean supportsCosmetics(){return cosmetics!=null;}
    public boolean appearance(int id,Protocol.AppearanceRequest request){var s=sessions.get(id);if(cosmetics==null||s==null||request.sequence()<=s.sequence)return false;if(s.commandTick!=tick){s.commandTick=tick;s.commands=0;}if(++s.commands>8)return false;var selection=new CosmeticCatalog.Selection(request.hair(),request.clothing());try{cosmetics.validate(selection);}catch(IllegalArgumentException e){return false;}s.sequence=request.sequence();s.appearance=selection;return true;}
    public List<Protocol.Appearance> appearances(){return sessions.entrySet().stream().map(e->new Protocol.Appearance(e.getKey(),race(e.getKey()),e.getValue().appearance.hair(),e.getValue().appearance.clothing())).toList();}
    public boolean join(int id,String name){
        new Protocol.Hello(1,name);if(id<1||id>1_000_000||sessions.containsKey(id)||sessions.size()>=2)return false;
        int seat=sessions.values().stream().anyMatch(s->s.seat==0)?1:0;var race=CharacterRaces.get(races.get(seat));var spawn=new Position(140,seat==0?300:410);
        var scenes=new RpgSceneSet("village",Map.of("village",new RpgSceneSet.Geometry(geometry,spawn)),Map.of(),Map.of(),Map.of(),Map.of());
        var identity=new CharacterIdentity(new UUID(0,id),name,race.id(),"azure","village",race.hp(),race.attack());var world=new RpgWorld(identity,scenes);var proxy=new GameEntity(id,name,spawn){};
        sessions.put(id,new Session(world,proxy,seat));return true;
    }
    public void leave(int id){sessions.remove(id);}
    public boolean accept(int id,Protocol.Intent intent){
        var session=sessions.get(id);if(session==null||intent.sequence()<=session.sequence)return false;
        if(session.commandTick!=tick){session.commandTick=tick;session.commands=0;}if(++session.commands>8)return false;
        session.sequence=intent.sequence();
        if(intent.action()!=Protocol.Action.MOVE||Math.abs(intent.x())>1||Math.abs(intent.y())>1)return false;
        session.x=intent.x();session.y=intent.y();session.inputTick=tick;
        if(session.x!=0||session.y!=0){double length=Math.hypot(session.x,session.y);session.fx=(float)(session.x/length);session.fy=(float)(session.y/length);}return true;
    }
    public void step(){
        tick++;
        for(var entry:sessions.entrySet()){
            var s=entry.getValue();var bodies=sessions.entrySet().stream().filter(e->!e.getKey().equals(entry.getKey())).map(e->new CollisionService.Body(e.getValue().proxy,18)).toList();s.world.setSyntheticBodies(bodies);
            s.world.tick(tick-s.inputTick<=15?s.x:0,tick-s.inputTick<=15?s.y:0);var view=s.world.snapshot();s.proxy.setPosition(new Position(view.x(),view.y()));
        }
    }
    /** Metadado da raça atribuído pelo servidor; não representa autenticação ou domínio escolhido. */
    public String race(int id){var s=sessions.get(id);if(s==null)throw new IllegalArgumentException("Sessão ausente");return s.world.snapshot().identity().race();}
    public Protocol.State snapshot(){var actors=new ArrayList<Protocol.Actor>();sessions.forEach((id,s)->{var v=s.world.snapshot();int stamina=v.state().flags().stream().filter(f->f.startsWith("stamina:")).mapToInt(f->Integer.parseInt(f.substring(8))).findFirst().orElse(0);actors.add(new Protocol.Actor(id,v.identity().name(),v.x(),v.y(),v.state().hp(),v.maxHp(),stamina,v.state().coins(),false,s.fx,s.fy,0));});return new Protocol.State(tick,actors);}
}
