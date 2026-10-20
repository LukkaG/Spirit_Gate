package com.projetok.studio;
import java.nio.file.*;
import java.util.*;

/** Exemplo sintetico de dados, sem gerar ou aprovar arte. */
public final class StudioExample {
    private StudioExample(){}
    /** Mapa inicial com spawn, NPC real, colisao e portal local validos. */
    public static WorldDocument document(){var d=WorldDocument.empty("studio_tutorial",960,640);var crop=new WorldDocument.Crop(0,0,32,32);var player=new WorldDocument.Element("player",WorldDocument.Kind.PLAYER,"","",crop,140,320,32,32,"interaction",true,false,"","FRONT",Map.of());var npc=new WorldDocument.Element("elder",WorldDocument.Kind.NPC,"","",crop,190,320,32,32,"interaction",true,false,"elder","FRONT",Map.of());return d.withElements(List.of(player,npc)).withHitboxes(List.of(new WorldDocument.Hitbox("wall",300,200,64,128,true,"collision"))).withPortals(List.of(new WorldDocument.Portal("portal",750,320,d.id(),140,320,"","FRONT")));}
    /** Escreve um exemplo novo; jamais sobrescreve arquivo existente. */
    public static void main(String[] args)throws Exception{if(args.length!=1)throw new IllegalArgumentException("Uso: StudioExample arquivo.sgmap");Path target=Path.of(args[0]);if(Files.exists(target))throw new IllegalArgumentException("Exemplo ja existe");DocumentStore.save(target,document());}
}
