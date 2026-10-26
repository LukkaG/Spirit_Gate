package com.projetok.studio;
import com.projetok.rpg.*;
import com.projetok.domain.entity.Position;
import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Exportacao opt-in verificavel: documentos, PNGs por hash e configuracao do RPG real. */
public final class StudioExport {
    /** Bundle carregado sem saves pessoais ou substituicao dos mapas padrao. */
    public record Bundle(String start,Map<String,WorldDocument> maps,AssetCatalog.Result assets,RpgSceneSet scenes) { }
    private StudioExport(){}
    /** Apenas mapas alcancaveis participam do primeiro bundle de ate tres regioes. */
    public static Map<String,WorldDocument> closure(String start,Map<String,WorldDocument> all) {
        Map<String,WorldDocument> result=new LinkedHashMap<>();ArrayDeque<String> queue=new ArrayDeque<>();queue.add(start);
        while(!queue.isEmpty()){String id=queue.removeFirst();if(result.containsKey(id))continue;var d=all.get(id);if(d==null)throw new IllegalArgumentException("Mapa referenciado ausente: "+id);result.put(id,d);if(result.size()>3)throw new IllegalArgumentException("Runtime atual suporta ate tres regioes de conteudo");d.portals().forEach(p->queue.add(p.destination()));}return Map.copyOf(result);
    }
    /** Identificador deterministico de spawn/portal, sem executar ou usar caminhos do documento. */
    public static String key(String prefix,String value){try{return prefix+HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8))).substring(0,20);}catch(java.security.NoSuchAlgorithmException impossible){throw new IllegalStateException(impossible);}}
    /** Monta configuracoes por injecao, conservando regras dos servicos de dominio. */
    public static RpgSceneSet scenes(String start,Map<String,WorldDocument> maps) {
        var limits=StudioRuntimeLimits.diagnostics(maps.values());if(!limits.isEmpty())throw new IllegalArgumentException(String.join("\n",limits));
        Map<String,RpgSceneSet.Geometry> geometry=new LinkedHashMap<>();Map<String,String> enemies=new TreeMap<>(),npcs=new TreeMap<>(),objects=new TreeMap<>(),portals=new TreeMap<>();
        var builtinObjects=new WorldObjects(RpgContent.builtin(),new ItemCatalog(RpgContent.builtin()));var prototypes=new ArrayList<WorldObjects.ObjectView>();for(String region:List.of("village","forest","ruins"))prototypes.addAll(builtinObjects.views(region,0,Set.of()));
        for(var d:maps.values()) {
            StudioEvents.validate(d);
            var spawn=d.elements().stream().filter(e->e.kind()==WorldDocument.Kind.PLAYER).findFirst().orElseThrow(()->new IllegalArgumentException("Spawn ausente: "+d.id()));
            if(!StudioGeometry.free(d,spawn.x(),spawn.y(),18))throw new IllegalArgumentException("Spawn obstruído ou fora dos limites: "+d.id());
            if(geometry.put(d.region(),new RpgSceneSet.Geometry(StudioGeometry.space(d),new Position(spawn.x(),spawn.y())))!=null)throw new IllegalArgumentException("Regiao repetida");
            for(var e:d.elements())switch(e.kind()) {
                case ENEMY->{var points=SpawnAreas.points(d,e);for(int i=0;i<points.size();i++){var p=points.get(i);String id=SpawnAreas.key(d,e,i);if(enemies.put(id,e.reference()+"|"+d.region()+"|"+p.x+"|"+p.y)!=null||enemies.size()>64)throw new IllegalArgumentException("Spawn duplicado ou populacao acima de 64");}}
                case NPC->{if(npcs.put(e.reference(),NpcEditing.definition(e,d.region()))!=null)throw new IllegalArgumentException("NPC repetido: "+e.reference());}
                case CHEST,RESOURCE->{var prototype=prototypes.stream().filter(o->o.id().equals(e.reference())).findFirst().orElseThrow();String id=key("obj_",d.id()+"/"+e.id());for(char c='0';c<='9';c++)id=id.replace(c,(char)('g'+c-'0'));if(objects.put(id,d.region()+"|"+e.x()+"|"+e.y()+"|"+prototype.item()+"|"+prototype.amount()+"|"+prototype.respawn())!=null)throw new IllegalArgumentException("Objeto duplicado");}
                default->{}
            }
            for(var p:d.portals()){var target=maps.get(p.destination());portals.put(key("portal_",d.id()+"/"+p.id()),d.region()+"|"+p.x()+"|"+p.y()+"|"+target.region()+"|"+p.arrivalX()+"|"+p.arrivalY()+"|"+p.quest());}
        }
        var result=new RpgSceneSet(maps.get(start).region(),geometry,enemies,npcs,objects,portals);
        // Construtores reais recusam referencias/posicoes incompatíveis antes de qualquer exportacao.
        var validator=new RpgWorld(CharacterIdentity.create("Validador","reaper","azure"),result);if(maps.get(start).metadata().containsKey("collision.radius"))validator.configureSyntheticCollision(com.projetok.domain.movement.SessionCollisionProfile.read(maps.get(start).metadata()));return result;
    }
    /** Cria diretorio novo e publica manifesto por ultimo; nunca sobrescreve projeto existente. */
    public static Path write(Path output,String start,Map<String,WorldDocument> all,AssetCatalog.Result catalog)throws IOException {
        try {
            var maps=closure(start,all);var issues=StudioValidator.validate(maps,catalog,true);if(issues.stream().anyMatch(p->p.severity()==StudioProblem.Severity.ERROR))throw new IOException(issues.toString());scenes(start,maps);
            Path root=output.toAbsolutePath();Files.createDirectories(root.getParent());Files.createDirectory(root);Files.createDirectory(root.resolve("images"));Properties manifest=new Properties();manifest.setProperty("schema","1");manifest.setProperty("start",start);manifest.setProperty("map.count",""+maps.size());int n=0;
            for(var d:maps.values()){Path file=root.resolve(d.id()+".sgmap");DocumentStore.save(file,EnvironmentAnimation.staticExport(d));manifest.setProperty("map."+n+".id",d.id());manifest.setProperty("map."+n+".sha",AssetCatalog.hash(file));n++;}
            Set<String> used=new LinkedHashSet<>();maps.values().forEach(d->d.elements().forEach(e->{if(!e.asset().isEmpty())used.add(e.asset());}));manifest.setProperty("asset.count",""+used.size());n=0;var assets=catalog.byId();
            for(String id:used){var a=assets.get(id);Path dest=root.resolve("images/"+a.sha()+".png");if(!Files.exists(dest))Files.copy(a.file(),dest);if(!AssetCatalog.hash(dest).equals(a.sha()))throw new IOException("Imagem mudou durante exportacao");manifest.setProperty("asset."+n+".id",id);manifest.setProperty("asset."+n+".sha",a.sha());n++;}
            try(var out=Files.newBufferedWriter(root.resolve("world.properties"))){manifest.store(out,"Spirit Gate Studio export V1 - sessao sem saves");}load(root);return root;
        }catch(IllegalArgumentException error){throw new IOException("Exportacao recusada: "+error.getMessage(),error);}
    }
    private static Path local(Path root,String relative)throws IOException {Path file=root.resolve(relative);if(Files.isSymbolicLink(file)||!file.toRealPath().startsWith(root.toRealPath()))throw new IOException("Caminho exportado fora do bundle");return file;}
    private static String take(Properties p,String key){String v=(String)p.remove(key);if(v==null)throw new IllegalArgumentException("Campo ausente: "+key);return v;}
    /** Le manifesto limitado, confere hashes e instancia configuracao validada do mundo real. */
    public static Bundle load(Path root)throws IOException {
        try {
            Properties p=new Properties(){@Override public synchronized Object put(Object k,Object v){if(containsKey(k))throw new IllegalArgumentException("Campo duplicado");return super.put(k,v);}};Path manifest=local(root,"world.properties");if(Files.size(manifest)>1_000_000)throw new IOException("Manifesto excessivo");try(var reader=Files.newBufferedReader(manifest)){p.load(reader);}if(!take(p,"schema").equals("1"))throw new IOException("Versao de exportacao desconhecida");String start=take(p,"start");int count=Integer.parseInt(take(p,"map.count"));if(count<1||count>3)throw new IOException("Numero de regioes invalido");Map<String,WorldDocument> maps=new LinkedHashMap<>();
            for(int i=0;i<count;i++){String id=take(p,"map."+i+".id"),sha=take(p,"map."+i+".sha");if(!WorldDocument.validId(id))throw new IOException("ID de mapa invalido");Path file=local(root,id+".sgmap");if(!AssetCatalog.hash(file).equals(sha))throw new IOException("Mapa alterado apos exportacao");var d=DocumentStore.load(file);if(!d.id().equals(id)||maps.put(id,d)!=null)throw new IOException("ID divergente/duplicado");}
            count=Integer.parseInt(take(p,"asset.count"));if(count<0||count>4096)throw new IOException("Assets excessivos");var assets=new ArrayList<AssetCatalog.Asset>();Set<String> ids=new HashSet<>();
            for(int i=0;i<count;i++){String id=take(p,"asset."+i+".id"),sha=take(p,"asset."+i+".sha");if(!sha.matches("[a-f0-9]{64}")||!ids.add(id))throw new IOException("Revisao/ID invalido");assets.add(AssetCatalog.verified(local(root,"images/"+sha+".png"),id,sha));}
            if(!p.isEmpty()||!maps.containsKey(start))throw new IOException("Manifesto inconsistente");var catalog=new AssetCatalog.Result(assets,List.of(),List.of());var issues=StudioValidator.validate(maps,catalog,true);if(issues.stream().anyMatch(e->e.severity()==StudioProblem.Severity.ERROR))throw new IOException(issues.toString());return new Bundle(start,Map.copyOf(maps),catalog,scenes(start,maps));
        }catch(IllegalArgumentException error){throw new IOException("Bundle invalido: "+error.getMessage(),error);}
    }
}
