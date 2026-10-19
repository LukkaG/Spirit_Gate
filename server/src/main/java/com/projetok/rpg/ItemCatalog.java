package com.projetok.rpg;
import java.util.*;
import java.io.*;

/** Regras de itens locais: raridade e apenas apresentacao. Peso omitido no V1. */
public final class ItemCatalog {
    public enum Category { EQUIPMENT, CONSUMABLE, MATERIAL, QUEST }
    public enum Rarity { COMMON, UNCOMMON, RARE }
    public record Item(String id,String name,String description,Category category,Rarity rarity,int price,
                       int maxStack,String slot,int power,int healing,int minimumLevel) {
        public Item {
            if(!id.matches("[a-z][a-z0-9_-]{0,39}") || name.isBlank() || description.isBlank() || description.length()>256
                || price<0 || price>10000 || maxStack<1 || maxStack>99 || power<0 || power>100 || healing<0 || healing>1000 || minimumLevel<1 || minimumLevel>20)
                throw new IllegalArgumentException("Item invalido");
            if(category==Category.EQUIPMENT && (!Set.of("weapon","torso","legs","accessory").contains(slot) || maxStack!=1)) throw new IllegalArgumentException("Equipamento invalido");
            if(category!=Category.EQUIPMENT && !slot.equals("none")) throw new IllegalArgumentException("Slot incompativel");
            if((category==Category.CONSUMABLE)!=(healing>0)) throw new IllegalArgumentException("Efeito incompativel");
        }
    }
    private final Map<String,Item> items;
    public ItemCatalog(RpgContent content) {
        Map<String,Item> map=new TreeMap<>();
        try(InputStream in=ItemCatalog.class.getResourceAsStream("/rpg/items.properties")) {
            if(in==null) throw new IOException("Itens ausentes");
            Properties p=new Properties() { public synchronized Object put(Object k,Object v) { if(containsKey(k)) throw new IllegalArgumentException("Item duplicado"); return super.put(k,v); } };
            byte[] bytes=in.readNBytes(65537); if(bytes.length>65536) throw new IllegalArgumentException("Itens excessivos");
            p.load(new ByteArrayInputStream(bytes));
            if(!"1".equals(p.remove("schema")) || p.size()>256) throw new IllegalArgumentException("Schema de itens invalido");
            for(String id:p.stringPropertyNames()) {
                String[] v=p.getProperty(id).split("\\|",-1); if(v.length!=9) throw new IllegalArgumentException("Campos de item invalidos");
                var base=content.get("item",id);
                map.put(id,new Item(id,base.name(),v[0],Category.valueOf(v[1]),Rarity.valueOf(v[2]),base.value(),Integer.parseInt(v[3]),v[4],Integer.parseInt(v[5]),Integer.parseInt(v[6]),Integer.parseInt(v[7])));
                if(!v[8].equals("local")) throw new IllegalArgumentException("Restricao desconhecida");
            }
            if(map.size()!=content.all("item").size()) throw new IllegalArgumentException("Catalogos divergentes");
        } catch(IOException e) { throw new IllegalStateException(e); }
        items=Map.copyOf(map);
    }
    public Item get(String id) { Item item=items.get(id); if(item==null) throw new IllegalArgumentException("Item inexistente: "+id); return item; }
    public Collection<Item> all() { return items.values(); }
}
