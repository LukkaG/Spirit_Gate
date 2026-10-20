package com.projetok.studio;
import java.util.*;

/** Autotiling NESW de quatro vizinhos, apenas para combinacoes cadastradas. */
public final class Autotiling {
    /** Regra explicita: 1 norte, 2 leste, 4 sul, 8 oeste; fallback nunca inventa arte. */
    public record Rule(String id,String asset,String sha,WorldDocument.Crop fallback,Map<Integer,WorldDocument.Crop> variants) {
        public Rule {variants=Map.copyOf(variants);if(!WorldDocument.validId(id)||variants.keySet().stream().anyMatch(k->k<0||k>15))throw new IllegalArgumentException("Regra de terreno invalida");}
    }
    private Autotiling(){}
    static String crop(WorldDocument.Crop c){return c.x()+","+c.y()+","+c.width()+","+c.height();}
    static WorldDocument.Crop parse(String s){int[] n=Arrays.stream(s.split(",")).mapToInt(Integer::parseInt).toArray();if(n.length!=4)throw new IllegalArgumentException("Recorte de terreno invalido");return new WorldDocument.Crop(n[0],n[1],n[2],n[3]);}
    /** Persiste apenas configuracao no documento, preservando tilesets originais. */
    public static WorldDocument register(WorldDocument d,Rule rule){Map<String,String> m=new TreeMap<>(d.metadata());String p="terrain."+rule.id()+".";m.put(p+"asset",rule.asset());m.put(p+"sha",rule.sha());m.put(p+"fallback",crop(rule.fallback()));m.put(p+"rules",rule.variants().entrySet().stream().sorted(Map.Entry.comparingByKey()).map(e->e.getKey()+":"+crop(e.getValue())).collect(java.util.stream.Collectors.joining(";")));return d.withMetadata(m);}
    /** Lista terrenos cadastrados sem deduzir layout do PNG. */
    public static List<String> terrains(WorldDocument d){return d.metadata().keySet().stream().filter(k->k.startsWith("terrain.")&&k.endsWith(".asset")).map(k->k.substring(8,k.length()-6)).sorted().toList();}
    /** Reconstroi a configuracao explicitamente salva. */
    public static Rule rule(WorldDocument d,String id){String p="terrain."+id+".";var m=d.metadata();Map<Integer,WorldDocument.Crop> variants=new HashMap<>();String values=m.getOrDefault(p+"rules","");if(!values.isEmpty())for(String value:values.split(";")){var v=value.split(":");variants.put(Integer.parseInt(v[0]),parse(v[1]));}return new Rule(id,m.get(p+"asset"),m.get(p+"sha"),parse(m.get(p+"fallback")),variants);}
    static String cell(String layer,int x,int y){return layer+":"+x+":"+y;}
    /** Mascara documentada, incluindo fronteiras tratadas como ausencia de vizinho. */
    public static int mask(WorldDocument.Element e,Map<String,WorldDocument.Element> cells,int grid){int mask=0;int[][] neighbors={{0,-grid,1},{grid,0,2},{0,grid,4},{-grid,0,8}};String terrain=e.properties().get("terrain");for(int[] n:neighbors){var other=cells.get(cell(e.layer(),e.x()+n[0],e.y()+n[1]));if(other!=null&&Objects.equals(terrain,other.properties().get("terrain")))mask|=n[2];}return mask;}
    /** Atualiza celulas e vizinhos apos um gesto; retorna o mesmo documento se nao houver mudancas. */
    public static WorldDocument refresh(WorldDocument d){if(terrains(d).isEmpty())return d;Map<String,WorldDocument.Element> cells=new HashMap<>();d.elements().stream().filter(e->e.kind()==WorldDocument.Kind.TILE).forEach(e->cells.put(cell(e.layer(),e.x(),e.y()),e));Set<String> lockedLayers=new HashSet<>();d.layers().stream().filter(WorldDocument.Layer::locked).forEach(l->lockedLayers.add(l.id()));Map<String,Rule> rules=new HashMap<>();var next=new ArrayList<WorldDocument.Element>();boolean changed=false;
        for(var e:d.elements()){String id=e.properties().get("terrain");if(id!=null&&e.kind()==WorldDocument.Kind.TILE&&!e.locked()&&!lockedLayers.contains(e.layer())){Rule r=rules.computeIfAbsent(id,key->rule(d,key));var crop=r.variants().getOrDefault(mask(e,cells,d.grid()),r.fallback());if(!crop.equals(e.crop())||!r.asset().equals(e.asset())){e=new WorldDocument.Element(e.id(),e.kind(),r.asset(),r.sha(),crop,e.x(),e.y(),e.width(),e.height(),e.layer(),e.visible(),e.locked(),e.reference(),e.direction(),e.properties());changed=true;}}next.add(e);}return changed?d.withElements(next):d;}
    /** Combina regras cadastradas com o pincel escolhido, sem aprovar revisoes automaticamente. */
    public static WorldDocument.Element brush(WorldDocument d,String id,String layer){var r=rule(d,id);return new WorldDocument.Element("brush",WorldDocument.Kind.TILE,r.asset(),r.sha(),r.fallback(),0,0,d.grid(),d.grid(),layer,true,false,"","FRONT",Map.of("terrain",id));}
    /** Indica combinacoes observadas sem arte propria para diagnostico. */
    public static List<String> missing(WorldDocument d){var result=new ArrayList<String>();Map<String,WorldDocument.Element> cells=new HashMap<>();d.elements().forEach(e->cells.put(cell(e.layer(),e.x(),e.y()),e));for(var e:d.elements())if(e.properties().containsKey("terrain")){var r=rule(d,e.properties().get("terrain"));int mask=mask(e,cells,d.grid());if(!r.variants().containsKey(mask))result.add(e.id()+": mascara "+mask+" usa fallback de "+r.id());}return List.copyOf(result);}
}
