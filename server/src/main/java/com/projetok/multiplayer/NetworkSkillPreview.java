package com.projetok.multiplayer;

import com.projetok.domain.entity.GameEntity;
import com.projetok.domain.entity.Position;
import com.projetok.domain.movement.CollisionService;
import com.projetok.rpg.RpgSkillCatalog;
import java.util.List;

/** Avalia somente prévias do snapshot local; nenhuma consulta aceita uma conjuração ou aplica combate. */
final class NetworkSkillPreview {
    enum Sight { UNKNOWN, CLEAR, BLOCKED }
    record Hint(boolean targetValid,boolean warning,String text,String badge) { }
    private static final CollisionService COLLISION=new CollisionService();
    private static final GameEntity PROBE=new GameEntity(-1,"Prévia local",new Position(0,0)){};
    private NetworkSkillPreview() { }

    /** Usa apenas cenário da revisão ativa, sem interpretar corpos ou pixels como autoridade. */
    static Sight sight(Protocol.Actor self,float x,float y,CollisionService.Space geometry){
        if(self==null||geometry==null)return Sight.UNKNOWN;
        var scenery=new CollisionService.Space(geometry.width(),geometry.height(),geometry.obstacles(),List.of());
        return COLLISION.canTraverseSegment(PROBE,new Position(self.x(),self.y()),new Position(x,y),1,scenery)?Sight.CLEAR:Sight.BLOCKED;
    }
    static boolean mob(Protocol.Actor actor){return actor!=null&&actor.id()>1_000_000&&actor.id()<2_000_000;}
    static boolean ally(Protocol.Actor actor){return actor!=null&&actor.id()>0&&actor.id()<=1_000_000;}

    /** Distingue destinatários obrigatórios e avisos estimados de alcance/visibilidade. */
    static Hint target(RpgSkillCatalog.Definition skill,Protocol.Actor self,Protocol.Actor selected,int selectedId,CollisionService.Space geometry){
        var pattern=skill.mechanics().pattern();
        if(skill.targeting()==RpgSkillCatalog.Targeting.SELF)return new Hint(true,false,"Ativação sem alvo; servidor valida.","");
        if(skill.targeting()==RpgSkillCatalog.Targeting.LINE||skill.targeting()==RpgSkillCatalog.Targeting.CONE)
            return new Hint(true,false,"Frontal imediato, na direção atual; servidor valida.","");
        if(skill.targeting()==RpgSkillCatalog.Targeting.GROUND)return new Hint(true,false,"Escolha a área; a validade é confirmada pelo servidor.","");
        Protocol.Actor target=selectedId==0&&skill.kit().equals("light")?self:selected;
        boolean revive=pattern==RpgSkillCatalog.Pattern.REVIVE;
        boolean hybrid=skill.damageScope()==RpgSkillCatalog.DamageScope.MOBS_ONLY;
        boolean valid=revive?ally(target)&&self!=null&&target.id()!=self.id()&&target.hp()==0
                :skill.targeting()==RpgSkillCatalog.Targeting.ALLY?ally(target)&&target.hp()>0
                :hybrid?(ally(target)||mob(target))&&target.hp()>0:mob(target)&&target.hp()>0;
        if(!valid)return new Hint(false,true,revive?"Selecione um aliado derrotado.":skill.kit().equals("light")?"Selecione um aliado vivo ou mob compatível.":"Selecione um inimigo vivo no mundo (TAB ou clique).","ALVO");
        if(self==null)return new Hint(false,true,"Aguardando seu personagem.","SYNC");
        double distance=Math.hypot(target.x()-self.x(),target.y()-self.y());
        if(distance>skill.range())return new Hint(true,true,"Fora de alcance (prévia): "+Math.round(distance)+" / "+skill.range()+" un.; servidor valida.","LONGE");
        if(sight(self,target.x(),target.y(),geometry)==Sight.BLOCKED)return new Hint(true,true,"Possível obstáculo na linha de visão (prévia); servidor valida.","LOS");
        if(hybrid&&ally(target)&&target.hp()>=target.maxHp())return new Hint(true,true,"Vida cheia no snapshot; o servidor valida a cura.","CHEIO");
        return new Hint(true,false,geometry==null?"Alvo ao alcance estimado; visibilidade não verificada localmente.":"Alvo ao alcance estimado; servidor valida.","");
    }

    /** O retículo informa bordas e obstáculos estimados sem prometer aceitação do servidor. */
    static Hint ground(Protocol.Actor self,float x,float y,int range,CollisionService.Space geometry){
        float width=geometry==null?960:geometry.width(),height=geometry==null?640:geometry.height();
        if(!Float.isFinite(x)||!Float.isFinite(y)||x<18||y<18||x>width-18||y>height-18)
            return new Hint(false,true,"Ponto fora dos limites da prévia.","LIMITE");
        if(self==null)return new Hint(false,true,"Aguardando seu personagem.","SYNC");
        if(Math.hypot(x-self.x(),y-self.y())>range+.01)return new Hint(false,true,"Área além do alcance estimado.","LONGE");
        var sight=sight(self,x,y,geometry);
        return new Hint(true,sight!=Sight.CLEAR,sight==Sight.BLOCKED?"Área possivelmente obstruída; o servidor decide.":sight==Sight.UNKNOWN?"Geometria indisponível; o servidor decide.":"Prévia dentro do alcance; o servidor decide.",sight==Sight.BLOCKED?"LOS":"");
    }
}
