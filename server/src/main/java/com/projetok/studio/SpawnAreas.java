package com.projetok.studio;
import java.awt.Point;
import java.util.*;

/** Expande encontros em pontos deterministas, mantendo parametros de IA/respawn originais. */
public final class SpawnAreas {
    private SpawnAreas(){}
    /** Quantidade e limite simultaneo coincidem no contrato finito do runtime. */
    public static int quantity(WorldDocument.Element e){int n=Integer.parseInt(e.properties().getOrDefault("spawn.quantity","1")),limit=Integer.parseInt(e.properties().getOrDefault("spawn.limit",""+n));if(n<1||n>64||limit!=n)throw new IllegalArgumentException("Quantidade 1..64; limite simultaneo deve coincidir");if(!e.properties().getOrDefault("spawn.respawn","builtin").equals("builtin")||!e.properties().getOrDefault("spawn.activation","builtin").equals("builtin"))throw new IllegalArgumentException("Ativacao e respawn devem usar builtin");return n;}
    /** Pontos em grade de 48 pixels; cada circulo usa a geometria real e raio 19. */
    public static java.util.List<Point> points(WorldDocument d,WorldDocument.Element e){int n=quantity(e);var result=new ArrayList<Point>();if(n==1)result.add(new Point(e.x(),e.y()));else{for(int y=e.y()+24;y<=e.y()+e.height()-24&&result.size()<n;y+=48)for(int x=e.x()+24;x<=e.x()+e.width()-24&&result.size()<n;x+=48)result.add(new Point(x,y));if(result.size()!=n)throw new IllegalArgumentException("Area pequena para quantidade solicitada");}for(var p:result)if(!StudioGeometry.free(d,p.x,p.y,19))throw new IllegalArgumentException("Spawn obstruido em "+p.x+","+p.y);return java.util.List.copyOf(result);}
    /** Chave estavel, preservando chave anterior dos pontos individuais. */
    public static String key(WorldDocument d,WorldDocument.Element e,int index){return StudioExport.key("spawn_",d.id()+"/"+e.id()+(quantity(e)==1?"":"/"+index));}
}
