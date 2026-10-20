package com.projetok.rpg;
import java.util.*;
import java.io.*;
import com.projetok.domain.entity.Position;
public final class PortalService {
    public record Portal(String id,String region,float x,float y,String destination,float arrivalX,float arrivalY,String quest) {
        /** Construtor legado sem condicao adicional. */
        public Portal(String id,String region,float x,float y,String destination,float arrivalX,float arrivalY){this(id,region,x,y,destination,arrivalX,arrivalY,"");}
    }
    private final Map<String,Portal> portals=new TreeMap<>();
    public PortalService(RpgContent content) {this(content,RpgSceneSet.entries("portals"));}
    /** Rotas de autoria opcionais; condicao adicional significa missao aceita. */
    public PortalService(RpgContent content,Map<String,String> definitions) {
        if(definitions.size()>64)throw new IllegalArgumentException("Portais excessivos");
        for(String id:definitions.keySet()) {
            String[] v=definitions.get(id).split("\\|",-1);if(v.length!=6&&v.length!=7)throw new IllegalArgumentException("Portal invalido");content.get("region",v[0]);content.get("region",v[3]);
            float x=Float.parseFloat(v[1]),y=Float.parseFloat(v[2]),ax=Float.parseFloat(v[4]),ay=Float.parseFloat(v[5]);
            for(float n:new float[]{x,y,ax,ay})if(!Float.isFinite(n)||n<18||n>942)throw new IllegalArgumentException("Coordenada invalida");
            if(y>622||ay>622)throw new IllegalArgumentException("Portal fora do mapa");String quest=v.length==7?v[6]:"";if(!quest.isEmpty())content.get("quest",quest);portals.put(id,new Portal(id,v[0],x,y,v[3],ax,ay,quest));
        }
    }
    public List<Portal> views(String region){return portals.values().stream().filter(p->p.region().equals(region)).toList();}
    public Optional<Portal> nearest(String region,Position p){return views(region).stream().filter(v->Math.hypot(v.x()-p.x(),v.y()-p.y())<=65).findFirst();}
    public Portal get(String id){var p=portals.get(id);if(p==null)throw new IllegalArgumentException("Portal ausente");return p;}
}
