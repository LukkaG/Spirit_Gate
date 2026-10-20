package com.projetok.studio;
import java.awt.*;
import java.util.*;

/** Operacoes puras e limitadas de pintura; nenhuma grava no disco. */
public final class EditorOperations {
    private EditorOperations(){}
    static void editable(WorldDocument d,String layer){if(d.layers().stream().noneMatch(l->l.id().equals(layer)&&l.visible()&&!l.locked()))throw new IllegalArgumentException("Camada oculta, bloqueada ou ausente");}
    static String id(){return "e_"+UUID.randomUUID().toString().replace("-","");}
    /** Posiciona preservando dimensoes reais ou tamanho de tile escolhido explicitamente. */
    public static WorldDocument pencil(WorldDocument d,WorldDocument.Element brush,int x,int y,boolean tile) {
        editable(d,brush.layer());int px=tile?Math.floorDiv(x,d.grid())*d.grid():x,py=tile?Math.floorDiv(y,d.grid())*d.grid():y;
        int w=tile?d.grid():brush.width(),h=tile?d.grid():brush.height();if(px<0||py<0||px+w>d.width()||py+h>d.height())return d;
        if(tile&&d.elements().stream().anyMatch(e->e.layer().equals(brush.layer())&&e.x()==px&&e.y()==py&&e.locked()))throw new IllegalArgumentException("Tile bloqueado");
        var placed=new WorldDocument.Element(id(),tile?WorldDocument.Kind.TILE:brush.kind(),brush.asset(),brush.sha(),brush.crop(),px,py,w,h,brush.layer(),true,false,brush.reference(),brush.direction(),brush.properties());
        var next=new ArrayList<>(d.elements());next.removeIf(e->tile&&e.kind()==WorldDocument.Kind.TILE&&e.layer().equals(brush.layer())&&e.x()==px&&e.y()==py&&!e.locked());next.add(placed);return d.withElements(next);
    }
    /** Borracha respeita bloqueio da camada e dos objetos. */
    public static WorldDocument erase(WorldDocument d,String layer,int x,int y){editable(d,layer);return d.withElements(d.elements().stream().filter(e->!e.layer().equals(layer)||e.locked()||!new Rectangle(e.x(),e.y(),e.width(),e.height()).contains(x,y)).toList());}
    /** Preenchimento retangular atomico, limitado a 4096 celulas. */
    public static WorldDocument rectangle(WorldDocument d,WorldDocument.Element brush,Point a,Point b){int grid=d.grid(),x0=Math.max(0,Math.min(a.x,b.x)/grid),y0=Math.max(0,Math.min(a.y,b.y)/grid),x1=Math.min(d.width()/grid-1,Math.max(a.x,b.x)/grid),y1=Math.min(d.height()/grid-1,Math.max(a.y,b.y)/grid);if((long)(x1-x0+1)*(y1-y0+1)>4096)throw new IllegalArgumentException("Retangulo excede 4096 celulas");for(int y=y0;y<=y1;y++)for(int x=x0;x<=x1;x++)d=pencil(d,brush,x*grid,y*grid,true);return d;}
    private static String signature(WorldDocument.Element e){return e==null?"":e.asset()+":"+e.crop()+":"+e.properties().getOrDefault("terrain","");}
    /** Balde primeiro calcula a area, recusando o excesso antes de modificar qualquer celula. */
    public static WorldDocument flood(WorldDocument d,WorldDocument.Element brush,int wx,int wy) {
        editable(d,brush.layer());int cols=d.width()/d.grid(),rows=d.height()/d.grid(),sx=wx/d.grid(),sy=wy/d.grid();if(wx<0||wy<0||sx>=cols||sy>=rows)return d;
        Map<Integer,WorldDocument.Element> cells=new HashMap<>();for(var e:d.elements())if(e.kind()==WorldDocument.Kind.TILE&&e.layer().equals(brush.layer()))cells.put(e.y()/d.grid()*cols+e.x()/d.grid(),e);
        int start=sy*cols+sx;String target=signature(cells.get(start));if(target.equals(signature(brush)))return d;
        Set<Integer> visited=new LinkedHashSet<>();ArrayDeque<Integer> queue=new ArrayDeque<>();queue.add(start);
        while(!queue.isEmpty()){int cell=queue.removeFirst();if(visited.contains(cell)||!signature(cells.get(cell)).equals(target)||(cells.containsKey(cell)&&cells.get(cell).locked()))continue;visited.add(cell);if(visited.size()>4096)throw new IllegalArgumentException("Balde excede 4096 celulas; use selecao menor");int x=cell%cols,y=cell/cols;if(x>0)queue.add(cell-1);if(x+1<cols)queue.add(cell+1);if(y>0)queue.add(cell-cols);if(y+1<rows)queue.add(cell+cols);}
        for(int cell:visited)d=pencil(d,brush,cell%cols*d.grid(),cell/cols*d.grid(),true);return d;
    }
    /** Copia elementos selecionados com posicoes relativas. */
    public static java.util.List<WorldDocument.Element> copy(WorldDocument d,Set<String> selected){var elements=d.elements().stream().filter(e->selected.contains(e.id())).toList();int x=elements.stream().mapToInt(WorldDocument.Element::x).min().orElse(0),y=elements.stream().mapToInt(WorldDocument.Element::y).min().orElse(0);return elements.stream().map(e->e.at(e.id(),e.x()-x,e.y()-y)).toList();}
    /** Cola com IDs novos e validacao atomica de limites. */
    public static WorldDocument paste(WorldDocument d,java.util.List<WorldDocument.Element> clipboard,int x,int y){var next=new ArrayList<>(d.elements());for(var e:clipboard){editable(d,e.layer());next.add(e.at(id(),e.x()+x,e.y()+y));}return d.withElements(next);}
}
