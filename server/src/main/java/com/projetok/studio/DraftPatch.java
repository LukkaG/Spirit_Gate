package com.projetok.studio;

import java.util.*;
import java.util.function.Function;

/** Incremental element/collider/portal delta. Full snapshots are a separate recovery path. */
public record DraftPatch(String mapId,String baseHash,Header header,
        List<WorldDocument.Element> elements,Set<String> removedElements,List<String> elementOrder,
        List<WorldDocument.Hitbox> hitboxes,Set<String> removedHitboxes,List<String> hitboxOrder,
        List<WorldDocument.Portal> portals,Set<String> removedPortals,List<String> portalOrder) {
    public record Header(String name,String region,int width,int height,int grid,List<WorldDocument.Layer> layers,Map<String,String> metadata){public Header{layers=List.copyOf(layers);metadata=Map.copyOf(metadata);}}
    public DraftPatch{
        if(!WorldDocument.validId(mapId)||baseHash==null||!baseHash.matches("[a-f0-9]{64}"))throw new IllegalArgumentException("Base de patch invalida");
        elements=List.copyOf(elements);removedElements=Set.copyOf(removedElements);elementOrder=List.copyOf(elementOrder);
        hitboxes=List.copyOf(hitboxes);removedHitboxes=Set.copyOf(removedHitboxes);hitboxOrder=List.copyOf(hitboxOrder);
        portals=List.copyOf(portals);removedPortals=Set.copyOf(removedPortals);portalOrder=List.copyOf(portalOrder);
        if(elements.size()+removedElements.size()>8192||hitboxes.size()+removedHitboxes.size()>4096||portals.size()+removedPortals.size()>64||elementOrder.size()>100000||hitboxOrder.size()>4096||portalOrder.size()>64)throw new IllegalArgumentException("Patch excessivo; use sincronizacao completa");
    }
    public static String hash(WorldDocument d)throws Exception{return HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(LiveRevision.canonical(d)));}
    private static <T> Map<String,T> index(List<T> list,Function<T,String> key){var map=new LinkedHashMap<String,T>();list.forEach(v->map.put(key.apply(v),v));return map;}
    private static <T> List<T> changed(List<T> before,List<T> after,Function<T,String> key){var old=index(before,key);return after.stream().filter(v->!v.equals(old.get(key.apply(v)))).toList();}
    private static <T> Set<String> removed(List<T> before,List<T> after,Function<T,String> key){var ids=new HashSet<>(index(before,key).keySet());ids.removeAll(index(after,key).keySet());return ids;}
    public static DraftPatch between(WorldDocument before,WorldDocument after)throws Exception{
        if(!before.id().equals(after.id())||!before.triggers().equals(after.triggers()))throw new IllegalArgumentException("Mudanca estrutural exige snapshot completo");
        return new DraftPatch(after.id(),hash(before),new Header(after.name(),after.region(),after.width(),after.height(),after.grid(),after.layers(),after.metadata()),
                changed(before.elements(),after.elements(),WorldDocument.Element::id),removed(before.elements(),after.elements(),WorldDocument.Element::id),after.elements().stream().map(WorldDocument.Element::id).toList(),
                changed(before.hitboxes(),after.hitboxes(),WorldDocument.Hitbox::id),removed(before.hitboxes(),after.hitboxes(),WorldDocument.Hitbox::id),after.hitboxes().stream().map(WorldDocument.Hitbox::id).toList(),
                changed(before.portals(),after.portals(),WorldDocument.Portal::id),removed(before.portals(),after.portals(),WorldDocument.Portal::id),after.portals().stream().map(WorldDocument.Portal::id).toList());
    }
    private static <T> List<T> merge(List<T> old,List<T> changed,Set<String> removed,List<String> order,Function<T,String> key){var map=index(old,key);removed.forEach(map::remove);changed.forEach(v->map.put(key.apply(v),v));if(order.size()!=map.size()||new HashSet<>(order).size()!=order.size()||!map.keySet().equals(new HashSet<>(order)))throw new IllegalArgumentException("Referencias de patch inconsistentes");return order.stream().map(map::get).toList();}
    public WorldDocument apply(WorldDocument before)throws Exception{
        if(!mapId.equals(before.id())||!baseHash.equals(hash(before)))throw new IllegalArgumentException("Hash base mudou; solicite snapshot completo");
        return new WorldDocument(1,mapId,header.name(),header.region(),header.width(),header.height(),header.grid(),header.layers(),merge(before.elements(),elements,removedElements,elementOrder,WorldDocument.Element::id),merge(before.hitboxes(),hitboxes,removedHitboxes,hitboxOrder,WorldDocument.Hitbox::id),merge(before.portals(),portals,removedPortals,portalOrder,WorldDocument.Portal::id),before.triggers(),header.metadata());
    }
    public int changedRecords(){return elements.size()+removedElements.size()+hitboxes.size()+removedHitboxes.size()+portals.size()+removedPortals.size();}
}
