package com.projetok.rpg;
import java.util.*;
import java.io.*;
/** Arvore declarativa, somente acoes enumeradas e condicoes conhecidas. */
public final class DialogueService {
    public record Choice(String text,String target,String action,String condition) { }
    public record Node(String id,String text,List<Choice> choices) { public Node { choices=List.copyOf(choices); } }
    private final Map<String,Node> nodes;
    public DialogueService() {
        try(InputStream in=DialogueService.class.getResourceAsStream("/rpg/dialogues.properties")) {
            if(in==null)throw new IOException("Dialogos ausentes"); Properties p=new Properties();p.load(in);Map<String,Node> map=new TreeMap<>();
            if(p.size()>64)throw new IllegalArgumentException("Dialogos excessivos");
            for(String key:p.stringPropertyNames()) {
                String[] parts=p.getProperty(key).split("\\|",-1);if(parts.length<2||parts.length>8||parts[0].length()>300)throw new IllegalArgumentException("No invalido");
                List<Choice> choices=new ArrayList<>();
                for(int i=1;i<parts.length;i++) {
                    String[] c=parts[i].split(">",-1);if(c.length!=4||c[0].isBlank()||c[0].length()>80||!Set.of("none","accept_first","claim_first","accept_boss","claim_boss","shop").contains(c[2])||!Set.of("always","first_ready","first_done","boss_ready").contains(c[3]))throw new IllegalArgumentException("Escolha invalida");
                    choices.add(new Choice(c[0],c[1],c[2],c[3]));
                }
                map.put(key,new Node(key,parts[0],choices));
            }
            for(Node node:map.values())for(Choice c:node.choices())if(!c.target().equals("close")&&!map.containsKey(c.target()))throw new IllegalArgumentException("No de destino ausente");
            nodes=Map.copyOf(map);
        }catch(IOException e){throw new IllegalStateException(e);}
    }
    public Node node(String id,java.util.function.Predicate<String> condition) {
        Node n=nodes.get(id);if(n==null)throw new IllegalArgumentException("No ausente: "+id);
        return new Node(n.id(),n.text(),n.choices().stream().filter(c->condition.test(c.condition())).toList());
    }
}
