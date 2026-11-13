package com.projetok.studio;

import java.util.*;

/** Expansão finita em documentos nativos, usando exclusivamente arte existente. */
public final class CorujaoSevenWorld {
    private static final String ART="assets/sprites/karakura-v3-extraidos-2026-10-18/";
    public static LiveRevision initial(AssetCatalog.Result assets)throws Exception {
        var original=WorldContent.baseline();var maps=new TreeMap<String,WorldDocument>();
        for(var d:original.bundle().maps().values()) {
            var elements=new ArrayList<>(d.elements());var solids=new ArrayList<>(d.hitboxes());
            for(int y=0;y<640;y+=64)for(int x=0;x<960;x+=64)
                art(elements,assets,"grass_"+x+"_"+y,"grama.png",x,y,64,64,"ground",null);
            for(int x=128;x<896;x+=64) {
                art(elements,assets,"path_"+x,"terra.png",x,320,64,64,"detail",null);
                art(elements,assets,"path_south_"+x,"terra.png",x,512,64,64,"detail",null);
            }
            if(d.region().equals("forest")) {
                elements.add(marker("east_wolf",WorldDocument.Kind.ENEMY,"wolf",760,420));
                tree(elements,solids,assets,"east_tree",600,60);
                tree(elements,solids,assets,"west_tree",40,40);
                tree(elements,solids,assets,"south_tree",500,380);
            } else {
                elements.add(marker("market_trader",WorldDocument.Kind.NPC,"trader",640,340));
                art(elements,assets,"market_house","loja.png",560,40,234,217,"objects",null);
                solids.add(new WorldDocument.Hitbox("market_walls",580,180,190,65,true,"collision"));
                tree(elements,solids,assets,"garden_tree",680,380);
            }
            var portals=new ArrayList<>(d.portals());
            portals.add(new WorldDocument.Portal("east_crossing",840,540,d.region().equals("forest")?"village":"forest",840,540,"","FRONT"));
            maps.put(d.id(),new WorldDocument(1,d.id(),d.region().equals("forest")?"Bosque e clareira leste":"Vila e mercado",d.region(),960,640,32,d.layers(),elements,solids,portals,List.of(),
                Map.of("art_source","existing","cycle","corujao-vii","points_of_interest",d.region().equals("forest")?"Clareira leste: lobos; passagem sul":"Ancia: missao; mercado: Nilo; passagem sul")));
        }
        var result=LiveRevision.prepare(0,"forest",maps,assets);WorldContent.encode(result);return result;
    }
    private static WorldDocument.Element marker(String id,WorldDocument.Kind kind,String reference,int x,int y) {
        return new WorldDocument.Element(id,kind,"","",new WorldDocument.Crop(0,0,32,32),x,y,32,32,"interaction",true,false,reference,"FRONT",Map.of());
    }
    private static void tree(List<WorldDocument.Element> elements,List<WorldDocument.Hitbox> solids,AssetCatalog.Result assets,String id,int x,int y) {
        art(elements,assets,id+"_canopy","arvore-01.png",x,y,134,185,"above",new WorldDocument.Crop(0,0,134,185));
        art(elements,assets,id+"_trunk","arvore-01.png",x,y+185,134,32,"objects",new WorldDocument.Crop(0,185,134,32));
        solids.add(new WorldDocument.Hitbox(id+"_foot",x+54,y+190,26,25,true,"collision"));
    }
    private static void art(List<WorldDocument.Element> elements,AssetCatalog.Result catalog,String id,String file,int x,int y,int width,int height,String layer,WorldDocument.Crop crop) {
        var asset=catalog.assets().stream().filter(a->a.path().equals(ART+file)).findFirst().orElse(null);
        // Testes com catálogo vazio conservam a geometria e marcadores, sem inventar imagens.
        if(asset==null)return;
        // Os recortes de terreno incluem borda de seleção; o interior evita uma grade escura.
        if(crop==null)crop=file.equals("grama.png")||file.equals("terra.png")?
            new WorldDocument.Crop(4,4,56,56):new WorldDocument.Crop(0,0,asset.width(),asset.height());
        elements.add(new WorldDocument.Element(id,WorldDocument.Kind.OBJECT,asset.id(),asset.sha(),crop,x,y,width,height,layer,true,false,"","FRONT",Map.of("art_source","existing")));
    }
    private CorujaoSevenWorld() { }
}
