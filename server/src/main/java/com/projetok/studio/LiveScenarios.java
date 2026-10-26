package com.projetok.studio;
import java.util.*;

/** Synthetic fixtures using real hitboxes/spawns, never writes a project. */
public final class LiveScenarios {
    public static final List<String> NAMES=List.of("Lado a lado","Parede alta","Corredor","Diagonal","Spawn","Shunpo","Obstaculo cinza direita","Roupas e cabelos");
    private LiveScenarios(){}
    public static WorldDocument document(int scenario){
        if(scenario<0||scenario>=NAMES.size())throw new IllegalArgumentException("Cenario invalido");
        var d=WorldDocument.empty("live_lab_"+scenario,960,640);int x=scenario==6?550:140,y=scenario==6?240:320;
        d=new WorldDocument(1,d.id(),NAMES.get(scenario),scenario==5?"forest":"village",960,640,32,d.layers(),List.of(),List.of(),List.of(),List.of(),Map.of());
        var elements=new ArrayList<WorldDocument.Element>();elements.add(new WorldDocument.Element("player",WorldDocument.Kind.PLAYER,"","",new WorldDocument.Crop(0,0,32,32),x,y,32,32,"interaction",true,false,"","FRONT",Map.of()));
        if(scenario==5)elements.add(new WorldDocument.Element("slime",WorldDocument.Kind.ENEMY,"","",new WorldDocument.Crop(0,0,32,32),240,320,32,32,"interaction",true,false,"slime","FRONT",Map.of()));
        var walls=new ArrayList<WorldDocument.Hitbox>();
        if(scenario==2){walls.add(new WorldDocument.Hitbox("left",300,100,80,400,true,"collision"));walls.add(new WorldDocument.Hitbox("right",424,100,80,400,true,"collision"));}
        else if(scenario==1||scenario==3||scenario==6)walls.add(new WorldDocument.Hitbox("central_gray",380,180,120,120,true,"collision"));
        else if(scenario==5)walls.add(new WorldDocument.Hitbox("thin",190,180,1,110,true,"collision"));
        return d.withElements(elements).withHitboxes(walls);
    }
}
