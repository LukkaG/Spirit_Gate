package com.projetok.studio;
import java.util.*;

/** Transformacoes atomicas de selecoes, com limites e bloqueios preservados. */
public final class GroupEditing {
    private GroupEditing(){}
    private static void guard(WorldDocument d,Set<String> ids){for(var e:d.elements())if(ids.contains(e.id())){EditorOperations.editable(d,e.layer());if(e.locked())throw new IllegalArgumentException("Objeto bloqueado: "+e.id());}for(var b:d.hitboxes())if(ids.contains(b.id()))EditorOperations.editable(d,b.layer());}
    /** Move elementos e colisoes selecionados; validacao ocorre antes do retorno. */
    public static WorldDocument move(WorldDocument d,Set<String> ids,int dx,int dy){guard(d,ids);var elements=d.elements().stream().map(e->ids.contains(e.id())?e.at(e.id(),Math.addExact(e.x(),dx),Math.addExact(e.y(),dy)):e).toList();var boxes=d.hitboxes().stream().map(b->ids.contains(b.id())?new WorldDocument.Hitbox(b.id(),Math.addExact(b.x(),dx),Math.addExact(b.y(),dy),b.width(),b.height(),b.solid(),b.layer()):b).toList();return d.withElements(elements).withHitboxes(boxes);}
    /** Alinha ou distribui centros; exige ao menos dois objetos editaveis. */
    public static WorldDocument arrange(WorldDocument d,Set<String> ids,boolean horizontal,boolean distribute){guard(d,ids);var chosen=new ArrayList<>(d.elements().stream().filter(e->ids.contains(e.id())).toList());if(chosen.size()<2)throw new IllegalArgumentException("Selecione dois objetos");chosen.sort(Comparator.comparingInt(e->horizontal?e.x():e.y()));int min=horizontal?chosen.get(0).x():chosen.get(0).y(),max=horizontal?chosen.get(chosen.size()-1).x():chosen.get(chosen.size()-1).y();Map<String,WorldDocument.Element> replacements=new HashMap<>();for(int i=0;i<chosen.size();i++){var e=chosen.get(i);int at=distribute?min+(int)((long)(max-min)*i/(chosen.size()-1)):min;replacements.put(e.id(),e.at(e.id(),horizontal?at:e.x(),horizontal?e.y():at));}return d.withElements(d.elements().stream().map(e->replacements.getOrDefault(e.id(),e)).toList());}
    /** Bloqueia ou desbloqueia explicitamente objetos, sem desbloquear camadas. */
    public static WorldDocument lock(WorldDocument d,Set<String> ids,boolean locked){for(var e:d.elements())if(ids.contains(e.id()))EditorOperations.editable(d,e.layer());return d.withElements(d.elements().stream().map(e->ids.contains(e.id())?new WorldDocument.Element(e.id(),e.kind(),e.asset(),e.sha(),e.crop(),e.x(),e.y(),e.width(),e.height(),e.layer(),e.visible(),locked,e.reference(),e.direction(),e.properties()):e).toList());}
    /** Remove selecao de objetos e colisoes, respeitando bloqueios. */
    public static WorldDocument delete(WorldDocument d,Set<String> ids){guard(d,ids);return d.withElements(d.elements().stream().filter(e->!ids.contains(e.id())).toList()).withHitboxes(d.hitboxes().stream().filter(e->!ids.contains(e.id())).toList());}
    /** Altera ordem dentro das camadas sem alterar suas prioridades. */
    public static WorldDocument order(WorldDocument d,Set<String> ids,boolean front){guard(d,ids);var next=new ArrayList<WorldDocument.Element>();d.elements().stream().filter(e->ids.contains(e.id())!=front).forEach(next::add);d.elements().stream().filter(e->ids.contains(e.id())==front).forEach(next::add);return d.withElements(next);}
}
