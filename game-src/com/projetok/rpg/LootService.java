package com.projetok.rpg;
import java.util.*;
import java.util.random.RandomGenerator;
import java.io.*;

/** Tabela local e ledger por geracao de spawn; repeticoes antigas sao rejeitadas. */
public final class LootService {
    public record Drop(String item,double chance,int minimum,int maximum) {
        public Drop { if(!Double.isFinite(chance)||chance<0||chance>1||minimum<1||maximum<minimum||maximum>99) throw new IllegalArgumentException("Drop invalido"); }
    }
    private final Map<String,Drop> table=new TreeMap<>();
    public LootService(RpgContent content,ItemCatalog items) {
        try(InputStream in=LootService.class.getResourceAsStream("/rpg/loot.properties")) {
            if(in==null) throw new IOException("Loot ausente"); Properties p=new Properties(); p.load(in);
            if(!"1".equals(p.remove("schema"))) throw new IllegalArgumentException("Schema loot invalido");
            for(String enemy:p.stringPropertyNames()) {
                content.get("enemy",enemy); String[] v=p.getProperty(enemy).split("\\|",-1);
                if(v.length!=4) throw new IllegalArgumentException("Drop malformado"); items.get(v[0]);
                table.put(enemy,new Drop(v[0],Double.parseDouble(v[1]),Integer.parseInt(v[2]),Integer.parseInt(v[3])));
            }
        } catch(IOException e) { throw new IllegalStateException(e); }
    }
    public boolean award(String spawn,long generation,String enemy,InventoryService inventory,Set<String> flags,RandomGenerator random) {
        if(!spawn.matches("[a-z][a-z0-9_-]{0,39}")||generation<1||generation>1_000_000_000L) throw new IllegalArgumentException("Evento invalido");
        Drop drop=table.get(enemy); if(drop==null) throw new IllegalArgumentException("Tabela ausente");
        String prefix="defeat:"+spawn+":";
        for(String f:flags) if(f.startsWith(prefix) && Long.parseLong(f.substring(prefix.length()))>=generation) return false;
        Map<String,Integer> pending=pending(flags);
        int amount=random.nextDouble()<drop.chance()?random.nextInt(drop.minimum(),drop.maximum()+1):0;
        InventoryService copy=inventory.copy();
        if(amount>0 && !copy.add(drop.item(),amount)) {
            int total=pending.getOrDefault(drop.item(),0)+amount;
            if(total>9999) return false;
            pending.put(drop.item(),total);
        }
        inventory.replace(copy); flags.removeIf(f->f.startsWith(prefix)||f.startsWith("pending:"));
        flags.add(prefix+generation); pending.forEach((id,n)->flags.add("pending:"+id+":"+n)); return true;
    }
    public static Map<String,Integer> pending(Set<String> flags) {
        Map<String,Integer> result=new TreeMap<>();
        for(String f:flags) if(f.startsWith("pending:")) { String[] v=f.split(":"); result.put(v[1],Integer.parseInt(v[2])); }
        return result;
    }
    public void collect(InventoryService inventory,Set<String> flags) {
        for(var e:pending(flags).entrySet()) if(inventory.add(e.getKey(),e.getValue())) flags.remove("pending:"+e.getKey()+":"+e.getValue());
    }
}
