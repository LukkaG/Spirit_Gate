package com.projetok.rpg;
import java.util.*;
/** Ciclo de vida persistente; flags de conclusao nunca sao removidas. */
public final class QuestService {
    public enum State { UNAVAILABLE, AVAILABLE, ACTIVE, READY, COMPLETED }
    public record View(String id,String name,State state,int progress,int required,String objective) { }
    public record Objective(String type,String target,int count) { }
    public record Definition(List<Objective> objectives,int xp,int coins,String item,int quantity) { public Definition { objectives=List.copyOf(objectives); } }
    private final RpgContent content;
    private final Map<String,Definition> definitions=new TreeMap<>();
    public QuestService(RpgContent content){
        this.content=content;
        try(var in=QuestService.class.getResourceAsStream("/rpg/quests.properties")) {
            if(in==null)throw new java.io.IOException("Quests ausentes");Properties p=new Properties();p.load(in);
            for(String id:p.stringPropertyNames()) {
                content.get("quest",id);String[] v=p.getProperty(id).split("\\|",-1);if(v.length!=5)throw new IllegalArgumentException("Quest invalida");List<Objective> objectives=new ArrayList<>();
                for(String objective:v[0].split(",")) {
                    String[] o=objective.split(":");if(o.length!=3||!Set.of("kill","collect","talk","visit").contains(o[0]))throw new IllegalArgumentException("Objetivo invalido");int n=Integer.parseInt(o[2]);if(n<1||n>100)throw new IllegalArgumentException("Contagem invalida");
                    content.get(switch(o[0]){case "kill"->"enemy";case "collect"->"item";case "talk"->"npc";default->"region";},o[1]);objectives.add(new Objective(o[0],o[1],n));
                }
                content.get("item",v[3]);int xp=Integer.parseInt(v[1]),coins=Integer.parseInt(v[2]),n=Integer.parseInt(v[4]);if(objectives.size()>8||xp<0||xp>10000||coins<0||coins>10000||n<1||n>99)throw new IllegalArgumentException("Recompensa invalida");definitions.put(id,new Definition(objectives,xp,coins,v[3],n));
            }
        }catch(java.io.IOException e){throw new IllegalStateException(e);}
    }
    public Definition definition(String id){var d=definitions.get(id);if(d==null)throw new IllegalArgumentException("Quest ausente");return d;}
    public int required(String id){return definition(id).objectives().stream().mapToInt(Objective::count).sum();}
    public State state(String id,Map<String,Integer> progress,Set<String> flags) {
        content.get("quest",id);
        if(flags.contains("quest-done:"+id))return State.COMPLETED;
        if(id.equals("sanctuary")&&!flags.contains("quest-done:first_steps"))return State.UNAVAILABLE;
        if(!progress.containsKey(id))return State.AVAILABLE;
        return progress.get(id)>=required(id)?State.READY:State.ACTIVE;
    }
    public boolean accept(String id,Map<String,Integer> progress,Set<String> flags){if(state(id,progress,flags)!=State.AVAILABLE)return false;progress.put(id,0);return true;}
    public boolean complete(String id,Map<String,Integer> progress,Set<String> flags){if(state(id,progress,flags)!=State.READY)return false;flags.add("quest-done:"+id);return true;}
    public void defeat(String enemy,Map<String,Integer> progress,Set<String> flags) {
        event("kill",enemy,"test_"+enemy,progress.getOrDefault("first_steps",0)+1,progress,flags);
    }
    public void event(String type,String target,String source,long sequence,Map<String,Integer> progress,Set<String> flags) {
        if(!source.matches("[a-z][a-z0-9_]{0,39}")||sequence<1)throw new IllegalArgumentException("Evento invalido");String ledger="qe:"+type+":"+source+":";
        for(String f:flags)if(f.startsWith(ledger)&&Long.parseLong(f.substring(ledger.length()))>=sequence)return;
        for(var entry:definitions.entrySet())if(state(entry.getKey(),progress,flags)==State.ACTIVE) {
            var objectives=entry.getValue().objectives();
            for(int i=0;i<objectives.size();i++){var o=objectives.get(i);if(!o.type().equals(type)||!o.target().equals(target))continue;String prefix="qo:"+entry.getKey()+":"+i+":";int count=counter(flags,prefix);flags.removeIf(f->f.startsWith(prefix));flags.add(prefix+Math.min(o.count(),count+1));}
            refreshOne(entry.getKey(),progress,flags,null);
        }
        flags.removeIf(f->f.startsWith(ledger));flags.add(ledger+sequence);
    }
    private int counter(Set<String> flags,String prefix){return flags.stream().filter(f->f.startsWith(prefix)).mapToInt(f->Integer.parseInt(f.substring(prefix.length()))).max().orElse(0);}
    public void refresh(Map<String,Integer> progress,Set<String> flags,InventoryService inventory,String region){
        for(String id:definitions.keySet())if(progress.containsKey(id)&&!flags.contains("quest-done:"+id)) {
            var objectives=definition(id).objectives();
            for(int i=0;i<objectives.size();i++){var o=objectives.get(i);if(o.type().equals("visit")&&o.target().equals(region)){String prefix="qo:"+id+":"+i+":";flags.removeIf(f->f.startsWith(prefix));flags.add(prefix+o.count());}}
            refreshOne(id,progress,flags,inventory);
        }
    }
    private void refreshOne(String id,Map<String,Integer> progress,Set<String> flags,InventoryService inventory){int total=0;var objectives=definition(id).objectives();for(int i=0;i<objectives.size();i++){var o=objectives.get(i);total+=o.type().equals("collect")&&inventory!=null?Math.min(o.count(),inventory.count(o.target())):counter(flags,"qo:"+id+":"+i+":");}progress.put(id,total);}
    public List<View> views(Map<String,Integer> progress,Set<String> flags){return content.all("quest").stream().sorted(Comparator.comparing(RpgContent.Definition::id)).map(q->new View(q.id(),q.name(),state(q.id(),progress,flags),progress.getOrDefault(q.id(),0),required(q.id()),definition(q.id()).objectives().stream().map(o->switch(o.type()){case "kill"->"Derrotar ";case "collect"->"Entregar ";case "talk"->"Conversar com ";default->"Visitar ";}+content.get(switch(o.type()){case "kill"->"enemy";case "collect"->"item";case "talk"->"npc";default->"region";},o.target()).name()+" x"+o.count()).collect(java.util.stream.Collectors.joining("; ")))).toList();}
}
