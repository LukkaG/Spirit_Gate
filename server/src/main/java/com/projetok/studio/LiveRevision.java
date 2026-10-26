package com.projetok.studio;

import com.projetok.domain.movement.*;
import com.projetok.domain.entity.*;
import com.projetok.rpg.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Frozen, bounded, validated content. No mutable editor object or renderer cache. */
public record LiveRevision(long number,String hash,StudioExport.Bundle bundle,SessionCollisionProfile profile) {
    public static byte[] canonical(WorldDocument document)throws java.io.IOException {
        return String.join("\n",new String(DocumentStore.encode(document),StandardCharsets.UTF_8).lines().filter(l->!l.startsWith("#")).sorted().toList()).getBytes(StandardCharsets.UTF_8);
    }
    public static LiveRevision prepare(long number,String start,Map<String,WorldDocument> source,AssetCatalog.Result assets)throws Exception {
        if(number<0||number>1_000_000_000L)throw new IllegalArgumentException("Revisao invalida");
        var maps=StudioExport.closure(start,source);long bytes=0;var digest=java.security.MessageDigest.getInstance("SHA-256");
        for(var id:new TreeSet<>(maps.keySet())){var data=canonical(maps.get(id));bytes+=data.length;if(bytes>4*1024*1024)throw new IllegalArgumentException("Preview limitado a 4 MiB de documentos");digest.update(id.getBytes(StandardCharsets.UTF_8));digest.update((byte)0);digest.update(data);}
        var issues=StudioValidator.validate(maps,assets,true);if(issues.stream().anyMatch(p->p.severity()==StudioProblem.Severity.ERROR))throw new IllegalArgumentException(issues.toString());
        if(maps.values().stream().anyMatch(d->!d.triggers().isEmpty()))throw new IllegalArgumentException("Gatilhos de editor: use o playtest exportado; Live nao executa scripts ou eventos de editor");
        var scenes=StudioExport.scenes(start,maps);var profile=SessionCollisionProfile.read(maps.get(start).metadata());
        var probe=new GameEntity(1,"Validacao",new Position(140,320)){};var collision=new CollisionService();
        for(var geometry:scenes.maps().values()){var p=geometry.spawn();if(!collision.isFree(probe,p.x(),p.y(),profile.actorRadius(),RpgCollisionProfile.scenery(geometry.space(),profile)))throw new IllegalArgumentException("Spawn obstruido pelo perfil");}
        for(var portal:scenes.portals().values()){var f=portal.split("\\|",-1);if(!collision.isFree(probe,Float.parseFloat(f[4]),Float.parseFloat(f[5]),profile.actorRadius(),RpgCollisionProfile.scenery(scenes.maps().get(f[3]).space(),profile)))throw new IllegalArgumentException("Chegada de portal obstruida pelo perfil");}
        var used=new HashSet<String>();maps.values().forEach(d->d.elements().forEach(e->{if(!e.asset().isEmpty())used.add(e.asset());EnvironmentAnimation.sequence(e).ifPresent(s->used.addAll(s.frames()));}));var catalog=new AssetCatalog.Result(assets.assets().stream().filter(a->used.contains(a.id())).map(a->new AssetCatalog.Asset(a.id(),a.path(),a.file(),a.sha(),a.width(),a.height(),a.alpha(),a.category(),null,a.indexedAt())).toList(),List.of(),List.of());
        if(catalog.assets().stream().mapToLong(a->(long)a.width()*a.height()*4).sum()>64L*1024*1024)throw new IllegalArgumentException("Assets ativos excedem cache de preview de 64 MiB");
        return new LiveRevision(number,HexFormat.of().formatHex(digest.digest()),new StudioExport.Bundle(start,Map.copyOf(maps),catalog,scenes),profile);
    }
    public Map<String,CollisionService.Space> geometry(){var result=new TreeMap<String,CollisionService.Space>();bundle.scenes().maps().forEach((r,g)->result.put(r,g.space()));return Map.copyOf(result);}
}
