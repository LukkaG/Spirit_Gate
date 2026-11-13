package com.projetok.rpg;

import com.projetok.domain.entity.GameEntity;
import com.projetok.domain.entity.Position;
import com.projetok.domain.movement.CollisionService;
import java.util.*;

/** Desvio limitado para mobs bloqueados; cada aresta respeita a colisao continua existente. */
final class MobNavigation {
    private static final int GRID=24,MAX_VISITS=1800;
    private record Cell(int x,int y){}
    private record Node(Cell cell,double cost,double score){}
    private MobNavigation(){}

    /** Busca somente no cenario atual; destino ocupado permite aproximacao para ataque, nunca sobreposicao. */
    static List<Position> route(GameEntity actor,Position target,CollisionService.Space space) {
        var collision=new CollisionService();var origin=actor.getPosition();
        if(!collision.isFree(actor,origin.x(),origin.y(),19,space))return List.of();
        var open=new PriorityQueue<Node>(Comparator.comparingDouble(Node::score).thenComparingInt(n->n.cell().x()).thenComparingInt(n->n.cell().y()));
        var costs=new HashMap<Cell,Double>();var parents=new HashMap<Cell,Cell>();
        var start=new Cell(0,0);costs.put(start,0.0);open.add(new Node(start,0,distance(origin,target)));
        boolean targetFree=collision.isFree(actor,target.x(),target.y(),19,space);
        // A visao ate o alvo ignora apenas o proprio viajante e o corpo no destino.
        // Demais atores continuam bloqueando; as arestas sempre usam o espaco completo.
        var sightSpace=new CollisionService.Space(space.width(),space.height(),space.obstacles(),
            space.bodies().stream().filter(b->b.entity()!=actor&&!b.entity().getPosition().equals(target)).toList());
        int visits=0;
        while(!open.isEmpty()&&visits++<MAX_VISITS) {
            var node=open.remove();if(node.cost()>costs.getOrDefault(node.cell(),Double.MAX_VALUE))continue;
            var p=point(origin,node.cell());
            boolean direct=targetFree&&collision.canTraverseSegment(actor,p,target,19,space);
            if(direct||(!targetFree&&distance(p,target)<=42&&RpgSight.clear(p,target,sightSpace))) {
                var result=new ArrayList<Position>();var cell=node.cell();
                while(!cell.equals(start)){result.add(point(origin,cell));cell=parents.get(cell);}
                Collections.reverse(result);if(direct)result.add(target);return result;
            }
            for(int dx=-1;dx<=1;dx++)for(int dy=-1;dy<=1;dy++)if(dx!=0||dy!=0) {
                var next=new Cell(node.cell().x()+dx,node.cell().y()+dy);var end=point(origin,next);
                double cost=node.cost()+GRID*Math.hypot(dx,dy);
                if(cost>=costs.getOrDefault(next,Double.MAX_VALUE)||!collision.canTraverseSegment(actor,p,end,19,space))continue;
                costs.put(next,cost);parents.put(next,node.cell());open.add(new Node(next,cost,cost+distance(end,target)));
            }
        }
        return List.of();
    }
    private static Position point(Position origin,Cell cell){return new Position(origin.x()+cell.x()*GRID,origin.y()+cell.y()*GRID);}
    private static double distance(Position a,Position b){return Math.hypot(a.x()-b.x(),a.y()-b.y());}
}
