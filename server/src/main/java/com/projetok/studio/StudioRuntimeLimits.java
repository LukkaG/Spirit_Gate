package com.projetok.studio;

import java.util.*;

/** Explicita limites atuais do adaptador sem alterar o formato do mapa ou do save. */
public final class StudioRuntimeLimits {
    private StudioRuntimeLimits() { }
    /** Mapas maiores continuam editáveis; execução recusa dimensões que o snapshot V2 não representa. */
    public static List<String> diagnostics(Collection<WorldDocument> maps){
        List<String> errors=new ArrayList<>();Set<String> regions=new HashSet<>();
        if(maps.isEmpty()||maps.size()>3)errors.add("O runtime atual suporta uma a três regiões de produção por sessão.");
        for(var map:maps){
            if(map.width()!=960||map.height()!=640)errors.add(map.id()+": mapa editável, mas playtest RPG exige 960x640; expansão de persistência ainda não autorizada.");
            if(!regions.add(map.region()))errors.add(map.id()+": região repetida no bundle: "+map.region());
        }
        return List.copyOf(errors);
    }
}
