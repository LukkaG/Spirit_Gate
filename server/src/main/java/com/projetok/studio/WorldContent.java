package com.projetok.studio;

import java.io.*;
import java.util.*;

/** Bounded documents only. Asset IDs resolve exclusively against local catalogs. */
public final class WorldContent {
    public static final int LIMIT=4*1024*1024;
    private WorldContent(){}
    public static LiveRevision baseline()throws Exception {
        var maps=new TreeMap<String,WorldDocument>();
        for(String region:List.of("forest","village")){
            var empty=WorldDocument.empty(region,960,640);var elements=new ArrayList<WorldDocument.Element>();
            elements.add(new WorldDocument.Element("player",WorldDocument.Kind.PLAYER,"","",new WorldDocument.Crop(0,0,32,32),140,region.equals("forest")?300:320,32,32,"interaction",true,false,"","FRONT",Map.of()));
            elements.add(new WorldDocument.Element(region.equals("forest")?"coop_slime":"elder",region.equals("forest")?WorldDocument.Kind.ENEMY:WorldDocument.Kind.NPC,"","",new WorldDocument.Crop(0,0,32,32),region.equals("forest")?220:190,region.equals("forest")?300:320,32,32,"interaction",true,false,region.equals("forest")?"slime":"elder","FRONT",Map.of()));
            var portal=new WorldDocument.Portal(region.equals("forest")?"to_village":"to_forest",140,region.equals("forest")?300:320,region.equals("forest")?"village":"forest",140,region.equals("forest")?320:300,"","FRONT");
            maps.put(region,new WorldDocument(1,region,region,region,960,640,32,empty.layers(),elements,List.of(new WorldDocument.Hitbox("central_gray",380,180,120,120,true,"collision")),List.of(portal),List.of(),Map.of()));
        }
        return LiveRevision.prepare(0,"forest",maps,new AssetCatalog.Result(List.of(),List.of(),List.of()));
    }
    public static byte[] encode(LiveRevision revision)throws Exception{
        if(!revision.bundle().maps().get(revision.bundle().start()).region().equals("forest")||revision.bundle().maps().values().stream().anyMatch(d->!Set.of("forest","village").contains(d.region())))throw new IllegalArgumentException("Publicacao RPG: inicio forest e regioes forest/village");
        if(revision.bundle().maps().values().stream().flatMap(d->d.elements().stream()).anyMatch(e->EnvironmentAnimation.sequence(e).isPresent()))throw new IOException("Animacao de ambiente ainda nao suportada no cliente V4");
        var npcs=new com.projetok.rpg.NpcService(com.projetok.rpg.RpgContent.builtin(),revision.bundle().scenes().npcs());for(String region:List.of("forest","village"))for(var npc:npcs.views(region))new com.projetok.multiplayer.Protocol.Hello(4,npc.name());
        for(var portal:revision.bundle().scenes().portals().keySet())new com.projetok.multiplayer.Protocol.WorldRequest(1,com.projetok.multiplayer.Protocol.WorldAction.TRAVEL,portal);
        for(String region:List.of("forest","village"))for(var npc:npcs.views(region)){var geometry=revision.geometry().get(region);var body=new com.projetok.domain.entity.GameEntity(1,"Validacao",new com.projetok.domain.entity.Position(npc.x(),npc.y())){};if(!new com.projetok.domain.movement.CollisionService().isFree(body,npc.x(),npc.y(),18,geometry))throw new IOException("NPC em posicao invalida");}
        long encoded=0;for(var asset:revision.bundle().assets().assets()){encoded+=java.nio.file.Files.size(asset.file());if(encoded>64L*1024*1024)throw new IOException("Assets preinstalados excedem 64 MiB");}if(revision.bundle().assets().assets().size()>128)throw new IOException("Assets excessivos");
        if(revision.bundle().scenes().enemies().size()+revision.bundle().scenes().npcs().size()>6)throw new IllegalArgumentException("Conteudo excede atores do protocolo");
        var buffer=new ByteArrayOutputStream();var out=new DataOutputStream(buffer);
        out.writeInt(1);out.writeLong(revision.number());out.writeUTF(revision.hash());out.writeUTF(revision.bundle().start());out.writeInt(revision.bundle().maps().size());
        for(var entry:new TreeMap<>(revision.bundle().maps()).entrySet()){byte[] bytes=LiveRevision.canonical(entry.getValue());out.writeInt(bytes.length);out.write(bytes);}
        if(buffer.size()>LIMIT)throw new IOException("Manifesto excessivo");return buffer.toByteArray();
    }
    public static LiveRevision decode(byte[] bytes,AssetCatalog.Result catalog)throws Exception{
        if(bytes.length>LIMIT)throw new IOException("Manifesto excessivo");var in=new DataInputStream(new ByteArrayInputStream(bytes));
        if(in.readInt()!=1)throw new IOException("Contrato de conteudo incompativel");long number=in.readLong();String hash=in.readUTF(),start=in.readUTF();int count=in.readInt();if(count<1||count>2)throw new IOException("Regioes excessivas");var maps=new TreeMap<String,WorldDocument>();
        for(int i=0;i<count;i++){int n=in.readInt();if(n<1||n>LIMIT||n>in.available())throw new IOException("Documento truncado/excessivo");var document=DocumentStore.decode(in.readNBytes(n));if(maps.put(document.id(),document)!=null)throw new IOException("Mapa duplicado");}
        if(in.available()!=0)throw new IOException("Dados excedentes");var result=LiveRevision.prepare(number,start,maps,catalog);
        if(!result.hash().equals(hash))throw new IOException("Hash incorreto");encode(result);return result;
    }
    /** Prepared pixels are owned by one client and never read from disk in paint. */
    public record Prepared(LiveRevision revision,AssetImages images){}
    public static Prepared prepare(byte[] bytes,AssetCatalog.Result catalog)throws Exception{
        var revision=decode(bytes,catalog);var images=new AssetImages();for(var asset:revision.bundle().assets().assets())images.load(asset);return new Prepared(revision,images);
    }
}
