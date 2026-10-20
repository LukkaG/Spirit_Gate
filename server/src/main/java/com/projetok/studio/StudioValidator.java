package com.projetok.studio;
import java.util.*;

/** Valida conteudo e compatibilidade explicita antes de exportar, sem corrigir arquivos. */
public final class StudioValidator {
    private StudioValidator(){}
    /** Chamador executa fora da EDT, pois revisoes de assets sao conferidas em disco. */
    public static List<StudioProblem> validate(Map<String,WorldDocument> maps,AssetCatalog.Result catalog,boolean runtime) {
        var result=new ArrayList<>(PortalGraph.validate(maps));var assets=catalog.byId();Map<String,String> actual=new HashMap<>();Set<String> regions=new HashSet<>();
        for(var d:maps.values()) {
            try{StudioEvents.validate(d);}catch(IllegalArgumentException ex){result.add(error(d,"events",ex.getMessage(),"Contrato de eventos","Revise gatilhos, destinos e encadeamentos"));}
            if(!EnvironmentPreview.mode(d).equals(EnvironmentPreview.Mode.OFF))result.add(warning(d,"environment","Iluminacao/clima do projeto sao somente previa",EnvironmentPreview.mode(d).name(),"Use evento ENVIRONMENT para o playtest ou mantenha previa no editor"));
            Map<String,Long> npcCounts=d.elements().stream().filter(e->e.kind()==WorldDocument.Kind.NPC).collect(java.util.stream.Collectors.groupingBy(WorldDocument.Element::reference,java.util.stream.Collectors.counting()));
            npcCounts.forEach((id,count)->{if(count>1)result.add(error(d,id,"NPC repetido no documento","Quantidade "+count,"Mantenha uma instancia por ID real"));});
            if(runtime&&(d.width()!=960||d.height()!=640))result.add(error(d,"mapa","Runtime inicial exige 960x640","Dimensoes "+d.width()+"x"+d.height(),"Ajuste o documento ou use somente previa"));
            if(runtime&&!regions.add(d.region()))result.add(error(d,"mapa","Dois mapas usam a mesma regiao de conteudo",d.region(),"Use village, forest e ruins distintos no conjunto exportado"));
            if(runtime&&d.elements().stream().filter(e->e.kind()==WorldDocument.Kind.PLAYER).count()!=1)result.add(error(d,"spawn","Exatamente um spawn de jogador obrigatorio","Contagem de PLAYER","Insira um marcador PLAYER"));
            for(var e:d.elements()) {
                var role=d.layers().stream().filter(l->l.id().equals(e.layer())).findFirst().orElseThrow().role();
                if(role==WorldDocument.Role.COLLISION)result.add(warning(d,e.id(),"Elemento visual na camada de colisao",e.layer(),"Mova a arte para camada visual; use hitbox explicita"));
                try{if(e.kind()==WorldDocument.Kind.NPC)NpcEditing.validate(e);if(e.kind()==WorldDocument.Kind.ENEMY)SpawnAreas.points(d,e);
                    EnvironmentAnimation.sequence(e).ifPresent(s->{for(int i=0;i<s.frames().size();i++){var a=assets.get(s.frames().get(i));if(a==null||!a.sha().equals(s.hashes().get(i)))result.add(error(d,e.id(),"Quadro ambiental ausente ou sem revisao",s.frames().get(i),"Reindexe e revise a sequencia"));else {String hash=actual.computeIfAbsent(a.id(),id->{try{return AssetCatalog.hash(a.file());}catch(Exception ex){return "ilegivel";}});if(!hash.equals(a.sha()))result.add(error(d,e.id(),"Quadro ambiental alterado em disco",a.path(),"Revise explicitamente a sequencia"));}}result.add(warning(d,e.id(),"Animacao ambiental somente na previa; exportacao estatica",s.frames().size()+" quadros","Avalie o quadro base no RPG"));});
                }catch(IllegalArgumentException ex){result.add(error(d,e.id(),ex.getMessage(),e.properties().toString(),"Ajuste pelo editor especifico"));}
                if(!e.asset().isEmpty()) {
                    var asset=assets.get(e.asset());String reason=null;
                    if(asset==null)reason="Sprite inexistente";
                    else try{String hash=actual.computeIfAbsent(asset.id(),id->{try{return AssetCatalog.hash(asset.file());}catch(Exception ex){return "ilegivel";}});if(!e.sha().equals(asset.sha())||!hash.equals(e.sha()))reason="Asset alterado sem revisao";else if((long)e.crop().x()+e.crop().width()>asset.width()||(long)e.crop().y()+e.crop().height()>asset.height())reason="Recorte fora da imagem";}catch(RuntimeException ex){reason=ex.getMessage();}
                    if(reason!=null)result.add(error(d,e.id(),reason,e.asset(),"Reindexe, revise a imagem e ajuste a referencia"));
                }
                if(e.kind()!=WorldDocument.Kind.TILE&&e.kind()!=WorldDocument.Kind.OBJECT) {
                    try{StudioContent.validate(e,d.region());}catch(IllegalArgumentException ex){result.add(error(d,e.id(),ex.getMessage(),e.reference(),"Escolha uma referencia do catalogo da regiao"));}
                    if(!StudioGeometry.free(d,e.x(),e.y(),e.kind()==WorldDocument.Kind.ENEMY?19:18))result.add(error(d,e.id(),"Marcador bloqueado ou fora do espaco fisico",e.x()+","+e.y(),"Mova para um ponto livre"));
                }
            }
            for(String overlap:StudioGeometry.overlaps(d))result.add(new StudioProblem(StudioProblem.Severity.WARNING,d.id(),"collision","Solidos sobrepostos",overlap,"Revise a sobreposicao se nao for intencional"));
            try{for(String missing:Autotiling.missing(d))result.add(warning(d,"terrain","Combinacao de autotiling usa fallback",missing,"Cadastre o recorte correspondente ou aceite o fallback"));}catch(RuntimeException ex){result.add(error(d,"terrain","Regra de terreno invalida",String.valueOf(ex.getMessage()),"Recadastre a regra explicita"));}
        }
        if(runtime){Map<String,String> npcMaps=new HashMap<>();for(var d:maps.values())for(var e:d.elements())if(e.kind()==WorldDocument.Kind.NPC){String other=npcMaps.putIfAbsent(e.reference(),d.id());if(other!=null&&!other.equals(d.id()))result.add(error(d,e.id(),"NPC repetido em mapas exportados",other,"Use uma instancia por ID real no conjunto"));}}
        return List.copyOf(result);
    }
    static StudioProblem error(WorldDocument d,String id,String description,String evidence,String correction){return new StudioProblem(StudioProblem.Severity.ERROR,d.id(),id,description,evidence,correction);}
    static StudioProblem warning(WorldDocument d,String id,String description,String evidence,String correction){return new StudioProblem(StudioProblem.Severity.WARNING,d.id(),id,description,evidence,correction);}
}
