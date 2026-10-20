package com.projetok.studio;

import java.util.*;

/** Documento imutavel de autoria V1; pixels de mundo independem do tamanho do PNG. */
public record WorldDocument(int schema,String id,String name,String region,int width,int height,int grid,
        List<Layer> layers,List<Element> elements,List<Hitbox> hitboxes,List<Portal> portals,
        List<Trigger> triggers,Map<String,String> metadata) {
    /** Papel visual ou logico; colisao nao depende da visibilidade de uma camada. */
    public enum Role { GROUND, DETAIL, BELOW, OBJECTS, ABOVE, ROOF, COLLISION, INTERACTION }
    /** Tipos explicitamente suportados pela autoria, sujeitos ao adaptador de exportacao. */
    public enum Kind { TILE, OBJECT, NPC, CHEST, RESOURCE, ENEMY, PLAYER, POINT }
    /** Camada ordenada, com bloqueio apenas de edicao. */
    public record Layer(String id,String name,Role role,int order,boolean visible,boolean locked) { }
    /** Recorte de origem, sem transformar nem regravar o PNG. */
    public record Crop(int x,int y,int width,int height) {
        public Crop {if(x<0||y<0||width<1||height<1||(long)x+width>8192||(long)y+height>8192)throw new IllegalArgumentException("Recorte invalido");}
    }
    /** Elemento visual ou marcador de entidade real; propriedades sao dados, nunca codigo. */
    public record Element(String id,Kind kind,String asset,String sha,Crop crop,int x,int y,int width,int height,
            String layer,boolean visible,boolean locked,String reference,String direction,Map<String,String> properties) {
        public Element {properties=Map.copyOf(properties);Objects.requireNonNull(crop);if(reference.length()>80||(!asset.isEmpty()&&!asset.matches("[a-f0-9]{64}"))||(!sha.isEmpty()&&!sha.matches("[a-f0-9]{64}"))||properties.size()>32||properties.entrySet().stream().anyMatch(e->e.getKey().length()>64||e.getValue().length()>4096))throw new IllegalArgumentException("Propriedades/referencia excessivas ou invalidas");}
        /** Copia posicionada com identificador proprio para duplicacao segura. */
        public Element at(String nextId,int nx,int ny){return new Element(nextId,kind,asset,sha,crop,nx,ny,width,height,layer,visible,locked,reference,direction,properties);}
    }
    /** Retangulo fisico explicito; transitavel e anotacao, nao um furo em outro solido. */
    public record Hitbox(String id,int x,int y,int width,int height,boolean solid,String layer) { }
    /** Conexao por ID de documento; destino e chegada sao validados em conjunto. */
    public record Portal(String id,int x,int y,String destination,int arrivalX,int arrivalY,String quest,String direction) { }
    /** Gatilho com acao cadastrada; referencia a outro gatilho permite diagnosticar ciclos. */
    public record Trigger(String id,String when,String action,String target,String message,int x,int y,int width,int height,boolean once) { }
    public WorldDocument {
        if(schema!=1||!validId(id)||name==null||name.isBlank()||name.length()>100||!Set.of("village","forest","ruins").contains(region))throw new IllegalArgumentException("Identidade/schema de mapa invalido");
        if(width<32||height<32||width>8192||height>8192||grid<1||grid>256)throw new IllegalArgumentException("Limites do mapa invalidos");
        layers=List.copyOf(layers);elements=List.copyOf(elements);hitboxes=List.copyOf(hitboxes);portals=List.copyOf(portals);triggers=List.copyOf(triggers);metadata=Map.copyOf(metadata);
        if(metadata.entrySet().stream().anyMatch(e->e.getKey().length()>64||e.getValue().length()>4096))throw new IllegalArgumentException("Metadados excessivos");
        if(layers.isEmpty()||layers.size()>32||elements.size()>100000||hitboxes.size()>4096||portals.size()>64||triggers.size()>256||metadata.size()>128)throw new IllegalArgumentException("Contagens do mapa invalidas");
        Set<String> layerIds=new HashSet<>(),ids=new HashSet<>();
        for(var l:layers)if(!validId(l.id())||l.name()==null||l.name().isBlank()||l.name().length()>80||l.role()==null||l.order()<0||l.order()>100||!layerIds.add(l.id()))throw new IllegalArgumentException("Camada invalida/duplicada");
        for(var e:elements){unique(ids,e.id());bounds(e.x(),e.y(),e.width(),e.height(),width,height);if(!layerIds.contains(e.layer())||e.kind()==null||!Set.of("FRONT","BACK","LEFT","RIGHT").contains(e.direction())||(!e.asset().isEmpty()&&!e.sha().matches("[a-f0-9]{64}")))throw new IllegalArgumentException("Elemento invalido");}
        for(var h:hitboxes){unique(ids,h.id());bounds(h.x(),h.y(),h.width(),h.height(),width,height);if(!layerIds.contains(h.layer()))throw new IllegalArgumentException("Camada fisica ausente");}
        for(var p:portals){unique(ids,p.id());bounds(p.x(),p.y(),1,1,width,height);if(!validId(p.destination())||p.arrivalX()<0||p.arrivalY()<0)throw new IllegalArgumentException("Portal invalido");}
        for(var t:triggers){unique(ids,t.id());bounds(t.x(),t.y(),t.width(),t.height(),width,height);if(t.message().length()>1000)throw new IllegalArgumentException("Mensagem excessiva");}
    }
    /** Novo mapa em branco, com papeis iniciais e grade 32. */
    public static WorldDocument empty(String id,int width,int height) {
        List<Layer> layers=new ArrayList<>();for(Role r:Role.values())layers.add(new Layer(r.name().toLowerCase(Locale.ROOT),r.name(),r,layers.size(),true,false));
        return new WorldDocument(1,id,id,"village",width,height,32,layers,List.of(),List.of(),List.of(),List.of(),Map.of());
    }
    /** Substitui elementos em uma operacao imutavel. */
    public WorldDocument withElements(List<Element> next){return new WorldDocument(schema,id,name,region,width,height,grid,layers,next,hitboxes,portals,triggers,metadata);}
    /** Substitui camadas, conservando referencias e validando-as. */
    public WorldDocument withLayers(List<Layer> next){return new WorldDocument(schema,id,name,region,width,height,grid,next,elements,hitboxes,portals,triggers,metadata);}
    /** Substitui hitboxes sem deduzir colisao da imagem. */
    public WorldDocument withHitboxes(List<Hitbox> next){return new WorldDocument(schema,id,name,region,width,height,grid,layers,elements,next,portals,triggers,metadata);}
    /** Substitui conexoes entre documentos. */
    public WorldDocument withPortals(List<Portal> next){return new WorldDocument(schema,id,name,region,width,height,grid,layers,elements,hitboxes,next,triggers,metadata);}
    /** Substitui eventos cadastrados. */
    public WorldDocument withTriggers(List<Trigger> next){return new WorldDocument(schema,id,name,region,width,height,grid,layers,elements,hitboxes,portals,next,metadata);}
    /** Substitui apenas dados auxiliares versionados. */
    public WorldDocument withMetadata(Map<String,String> next){return new WorldDocument(schema,id,name,region,width,height,grid,layers,elements,hitboxes,portals,triggers,next);}
    /** Ordenacao estavel inclusive quando duas camadas usam o mesmo indice. */
    public List<Layer> orderedLayers(){return layers.stream().sorted(Comparator.comparingInt(Layer::order).thenComparing(Layer::id)).toList();}
    /** Identificadores legiveis e limitados, sem caminhos ou comandos. */
    public static boolean validId(String id){return id!=null&&id.matches("[a-z][a-z0-9_-]{0,79}");}
    private static void unique(Set<String> ids,String id){if(!validId(id)||!ids.add(id))throw new IllegalArgumentException("ID vazio/duplicado: "+id);}
    private static void bounds(int x,int y,int w,int h,int mw,int mh){if(x<0||y<0||w<1||h<1||(long)x+w>mw||(long)y+h>mh)throw new IllegalArgumentException("Elemento fora do mapa");}
}
