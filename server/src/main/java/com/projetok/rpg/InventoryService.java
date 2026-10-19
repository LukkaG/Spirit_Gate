package com.projetok.rpg;
import java.util.*;

/** Transacoes sobre copias: falha nunca altera o inventario publicado. */
public final class InventoryService {
    public record Stack(String id,int quantity) { }
    private final ItemCatalog catalog;
    private final Stack[] slots;
    public InventoryService(ItemCatalog catalog,int capacity) {
        if(capacity<1||capacity>100) throw new IllegalArgumentException("Capacidade invalida");
        this.catalog=catalog; slots=new Stack[capacity];
    }
    public int capacity() { return slots.length; }
    public List<Stack> slots() { return Collections.unmodifiableList(Arrays.asList(slots.clone())); }
    public int count(String id) { catalog.get(id); return Arrays.stream(slots).filter(Objects::nonNull).filter(s->s.id().equals(id)).mapToInt(Stack::quantity).sum(); }
    public boolean add(String id,int amount) {
        validate(id,amount); Stack[] copy=slots.clone(); int remaining=amount,max=catalog.get(id).maxStack();
        for(int i=0;i<copy.length && remaining>0;i++) if(copy[i]!=null && copy[i].id().equals(id)) {
            int n=Math.min(remaining,max-copy[i].quantity()); copy[i]=new Stack(id,copy[i].quantity()+n); remaining-=n;
        }
        for(int i=0;i<copy.length && remaining>0;i++) if(copy[i]==null) {
            int n=Math.min(remaining,max); copy[i]=new Stack(id,n); remaining-=n;
        }
        if(remaining>0) return false; System.arraycopy(copy,0,slots,0,slots.length); return true;
    }
    public boolean remove(String id,int amount) {
        validate(id,amount); if(count(id)<amount) return false; int remaining=amount;
        for(int i=0;i<slots.length && remaining>0;i++) if(slots[i]!=null && slots[i].id().equals(id)) {
            int n=Math.min(remaining,slots[i].quantity()),left=slots[i].quantity()-n;
            slots[i]=left==0?null:new Stack(id,left); remaining-=n;
        }
        return true;
    }
    public boolean move(int from,int to,int amount) {
        index(from);index(to); if(amount<1||from==to||slots[from]==null||slots[from].quantity()<amount) return false;
        Stack a=slots[from],b=slots[to];
        if(b!=null && (!b.id().equals(a.id()) || b.quantity()+amount>catalog.get(a.id()).maxStack())) return false;
        slots[to]=new Stack(a.id(),amount+(b==null?0:b.quantity()));
        slots[from]=a.quantity()==amount?null:new Stack(a.id(),a.quantity()-amount); return true;
    }
    private void index(int i) { if(i<0||i>=slots.length) throw new IllegalArgumentException("Slot inexistente"); }
    private void validate(String id,int n) { catalog.get(id); if(n<1||n>9999) throw new IllegalArgumentException("Quantidade invalida"); }
    public Map<String,Integer> snapshot() {
        Map<String,Integer> result=new TreeMap<>(); for(int i=0;i<slots.length;i++) if(slots[i]!=null) result.put(i+":"+slots[i].id(),slots[i].quantity()); return Map.copyOf(result);
    }
    public InventoryService copy() { return restore(catalog,slots.length,snapshot()); }
    public void replace(InventoryService other) {
        if(other.capacity()!=capacity()) throw new IllegalArgumentException("Capacidades diferentes");
        System.arraycopy(other.slots,0,slots,0,slots.length);
    }
    public static InventoryService restore(ItemCatalog catalog,int capacity,Map<String,Integer> data) {
        InventoryService result=new InventoryService(catalog,capacity);
        for(var entry:data.entrySet()) {
            String[] key=entry.getKey().split(":",-1); if(key.length!=2) throw new IllegalArgumentException("Slot persistente invalido");
            int slot=Integer.parseInt(key[0]); result.index(slot); int n=entry.getValue();
            if(result.slots[slot]!=null||n<1||n>catalog.get(key[1]).maxStack()) throw new IllegalArgumentException("Pilha persistente invalida");
            result.slots[slot]=new Stack(key[1],n);
        }
        return result;
    }
}
