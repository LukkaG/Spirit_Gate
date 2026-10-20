package com.projetok.studio;
import com.projetok.rpg.*;
import java.util.*;

/** Eventos declarativos limitados; nenhum compilador, script ou comando de sistema. */
public final class StudioEvents {
    /** Entrada somente de leitura extraida do runtime real. */
    public record State(String map,int x,int y,Set<String> nearbyNpcs,Set<String> completedQuests,boolean interacted){public State{nearbyNpcs=Set.copyOf(nearbyNpcs);completedQuests=Set.copyOf(completedQuests);}}
    /** Acoes explicitamente permitidas; CHAIN aponta para outro evento validado. */
    public static final Set<String> ACTIONS=Set.of("MESSAGE","ENVIRONMENT","PORTAL","INTERACTION","CHAIN");
    private final Set<String> fired=new HashSet<>(),active=new HashSet<>();
    private final ArrayDeque<WorldDocument.Trigger> pending=new ArrayDeque<>();private String map="";
    /** Verifica referencias e rejeita ciclos inclusive em eventos desconectados. */
    public static void validate(WorldDocument d){Map<String,WorldDocument.Trigger> triggers=new HashMap<>();d.triggers().forEach(t->triggers.put(t.id(),t));for(var t:d.triggers()){
        if(!ACTIONS.contains(t.action()))throw new IllegalArgumentException("Acao nao permitida: "+t.action());String when=t.when();if(when.startsWith("NEAR_NPC:")){String id=when.substring(9);if(d.elements().stream().noneMatch(e->e.kind()==WorldDocument.Kind.NPC&&e.reference().equals(id)))throw new IllegalArgumentException("NPC do evento ausente");}else if(when.startsWith("QUEST_DONE:"))RpgContent.builtin().get("quest",when.substring(11));else if(!Set.of("ENTER","AREA","INTERACT","MANUAL").contains(when))throw new IllegalArgumentException("Gatilho nao permitido: "+when);
        switch(t.action()){case "ENVIRONMENT"->EnvironmentPreview.Mode.valueOf(t.target());case "PORTAL"->{if(d.portals().stream().noneMatch(p->p.id().equals(t.target())))throw new IllegalArgumentException("Portal do evento ausente");}case "CHAIN"->{if(!triggers.containsKey(t.target()))throw new IllegalArgumentException("Evento encadeado ausente");}case "INTERACTION"->{if(!t.target().isEmpty())throw new IllegalArgumentException("INTERACTION solicita a interacao proxima do runtime; destino deve estar vazio");}default->{}}
    }Set<String> done=new HashSet<>(),visiting=new HashSet<>();for(String id:triggers.keySet())visit(id,triggers,done,visiting);}
    private static void visit(String id,Map<String,WorldDocument.Trigger> all,Set<String> done,Set<String> visiting){if(done.contains(id))return;if(!visiting.add(id))throw new IllegalArgumentException("Ciclo de eventos: "+id);var t=all.get(id);if(t.action().equals("CHAIN"))visit(t.target(),all,done,visiting);visiting.remove(id);done.add(id);}
    /** Emite no maximo oito acoes por atualizacao; repeticao exige nova borda de entrada. */
    public java.util.List<WorldDocument.Trigger> update(WorldDocument d,State state){if(!map.equals(d.id())){map=d.id();active.clear();pending.clear();}for(var t:d.triggers()){
        String key=d.id()+"/"+t.id();boolean match=switch(t.when()){case "ENTER"->true;case "AREA"->state.x()>=t.x()&&state.y()>=t.y()&&state.x()<t.x()+t.width()&&state.y()<t.y()+t.height();case "INTERACT"->state.interacted();default->t.when().startsWith("NEAR_NPC:")?state.nearbyNpcs().contains(t.when().substring(9)):t.when().startsWith("QUEST_DONE:")&&state.completedQuests().contains(t.when().substring(11));};if(match){if(active.add(key)&&(!t.once()||!fired.contains(key))&&pending.size()<256)pending.add(t);}else active.remove(key);
    }var result=new ArrayList<WorldDocument.Trigger>();Map<String,WorldDocument.Trigger> all=new HashMap<>();d.triggers().forEach(t->all.put(t.id(),t));for(int budget=0;budget<8&&!pending.isEmpty();budget++){var t=pending.removeFirst();String key=d.id()+"/"+t.id();if(t.once()&&!fired.add(key))continue;if(t.action().equals("CHAIN")){var next=all.get(t.target());if(next!=null&&pending.size()<256)pending.add(next);}else result.add(t);}return java.util.List.copyOf(result);}
    /** Converte snapshot sem alterar quests ou progresso. */
    public static State state(WorldDocument d,RpgWorld.View v,boolean interacted){var npcs=new HashSet<String>();v.npcs().stream().filter(n->Math.hypot(n.x()-v.x(),n.y()-v.y())<=70).forEach(n->npcs.add(n.id()));var quests=new HashSet<String>();v.state().flags().stream().filter(f->f.startsWith("quest-done:")).forEach(f->quests.add(f.substring(11)));return new State(d.id(),(int)v.x(),(int)v.y(),npcs,quests,interacted);}
}
