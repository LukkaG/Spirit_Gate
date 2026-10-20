package com.projetok.studio;
import com.projetok.domain.entity.*;
import com.projetok.domain.movement.*;
import java.util.*;

/** Aproximacao de navegacao amostrada; conectividade nao equivale a jogabilidade. */
public final class NavigationAnalysis {
    /** Grade de amostras com consultas protegidas, sem expor bitsets mutaveis. */
    public static final class Result {
        private final int cell,cols,rows;private final BitSet free,reached;
        Result(int cell,int cols,int rows,BitSet free,BitSet reached){this.cell=cell;this.cols=cols;this.rows=rows;this.free=(BitSet)free.clone();this.reached=(BitSet)reached.clone();}
        /** Tamanho de celula aproximada em pixels. */
        public int cell(){return cell;}
        /** Colunas amostradas. */
        public int cols(){return cols;}
        /** Linhas amostradas. */
        public int rows(){return rows;}
        /** Quantidade de amostras livres fora do componente do spawn. */
        public int isolated(){return free.cardinality()-reached.cardinality();}
        /** Consulta de amostra potencialmente isolada. */
        public boolean isolated(int x,int y){int i=y*cols+x;return free.get(i)&&!reached.get(i);}
        /** Quantidade de amostras fisicamente livres. */
        public int freeCount(){return free.cardinality();}
    }
    private NavigationAnalysis(){}
    /** BFS em quatro vizinhos, celulas >=32, ate 65536 amostras, raio real 18. */
    public static Result analyze(WorldDocument d){int cell=Math.max(32,(Math.max(d.width(),d.height())+255)/256),cols=(d.width()+cell-1)/cell,rows=(d.height()+cell-1)/cell;BitSet free=new BitSet(cols*rows),reached=new BitSet(cols*rows);var physics=new CollisionService();var space=StudioGeometry.space(d);var body=new GameEntity(1,"Amostra",new Position(0,0)){};for(int y=0;y<rows;y++)for(int x=0;x<cols;x++)if(physics.isFree(body,x*cell+cell/2f,y*cell+cell/2f,18,space))free.set(y*cols+x);
        var spawn=d.elements().stream().filter(e->e.kind()==WorldDocument.Kind.PLAYER).findFirst();if(spawn.isPresent()){int start=spawn.get().y()/cell*cols+spawn.get().x()/cell;ArrayDeque<Integer> queue=new ArrayDeque<>();if(free.get(start)){queue.add(start);reached.set(start);}while(!queue.isEmpty()){int i=queue.removeFirst(),x=i%cols,y=i/cols;int[][] offsets={{-1,0},{1,0},{0,-1},{0,1}};for(var offset:offsets){int nx=x+offset[0],ny=y+offset[1],next=ny*cols+nx;if(nx<0||nx>=cols||ny<0||ny>=rows||reached.get(next)||!free.get(next))continue;if(!physics.isFree(body,(x+nx)*cell/2f+cell/2f,(y+ny)*cell/2f+cell/2f,18,space))continue;reached.set(next);queue.add(next);}}}return new Result(cell,cols,rows,free,reached);}
}
