package com.projetok.studio;
import com.projetok.rpg.*;
import java.util.*;

/** Consulta catalogos reais do RPG; nao duplica loot, dialogos ou IA. */
public final class StudioContent {
    private StudioContent(){}
    /** IDs permitidos para cada marcador. */
    public static List<String> references(WorldDocument.Kind kind) {
        String category=switch(kind){case NPC->"npc";case ENEMY->"enemy";default->"";};
        if(!category.isEmpty())return RpgContent.builtin().all(category).stream().map(RpgContent.Definition::id).sorted().toList();
        if(kind==WorldDocument.Kind.CHEST||kind==WorldDocument.Kind.RESOURCE){var objects=new WorldObjects(RpgContent.builtin(),new ItemCatalog(RpgContent.builtin()));return List.of("village","forest","ruins").stream().flatMap(r->objects.views(r,0,Set.of()).stream()).map(WorldObjects.ObjectView::id).sorted().toList();}
        return List.of("");
    }
    /** Valida referencia e regiao conforme contratos existentes. */
    public static void validate(WorldDocument.Element e,String region) {
        if(!references(e.kind()).contains(e.reference()))throw new IllegalArgumentException("Referencia inexistente: "+e.reference());
        if(e.kind()==WorldDocument.Kind.NPC||e.kind()==WorldDocument.Kind.ENEMY){String kind=e.kind()==WorldDocument.Kind.NPC?"npc":"enemy";if(!RpgContent.builtin().get(kind,e.reference()).region().equals(region))throw new IllegalArgumentException("Referencia pertence a outra regiao de conteudo: "+e.reference());}
    }
}
