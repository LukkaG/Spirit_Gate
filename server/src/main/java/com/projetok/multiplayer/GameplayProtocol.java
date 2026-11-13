package com.projetok.multiplayer;

import java.io.*;
import java.util.*;

/** Extensão opt-in V6: ficha privada da sessão, sem caminhos, identidade alheia ou XP enviado. */
public final class GameplayProtocol {
    // Acrescentar operações ao fim mantém os ordinais das mensagens existentes.
    public enum Operation { EQUIP, UNEQUIP, USE, COLLECT, CHOOSE_DOMAIN, SELECT_TARGET, BASIC_ATTACK, SHUNPO_OFFENSIVE, QUERY_ATTRIBUTES, ALLOCATE_ATTRIBUTE, QUERY_ENERGY }
    /** Metadados privados opt-in, mantendo intactos os frames anteriores. Valores em décimos. */
    public record Energy(String hash,long tick,long sequence,int trained,int current,int maximum,int regenerationPermille) implements Protocol.Message {
        public Energy{if(hash==null||!hash.matches("[a-f0-9]{64}")||tick<0||tick>1_000_000_000L||sequence<1||sequence>1_000_000_000_000L||trained<0||trained>10000||current<0||current>maximum||maximum!=com.projetok.domain.combat.EnergyRules.maximum(trained)||regenerationPermille!=com.projetok.domain.combat.EnergyRules.regenerationPermille(trained))throw new IllegalArgumentException("Energia inválida");}
        public int currentFor(Protocol.State snapshot,int self){if(snapshot==null)return 0;var actor=snapshot.actors().stream().filter(a->a.id()==self).findFirst().orElse(null);if(actor==null)return 0;if(snapshot.tick()<=tick)return current;return Math.max(0,Math.min(maximum,Math.round(actor.stamina()*maximum/1000f)));}
    }
    /** Resposta privada opt-in, separada da ficha V6 para preservar seu formato anterior. */
    public record Attributes(String hash,int strength,int intelligence,int agility,int level,int points) implements Protocol.Message {
        public Attributes{if(hash==null||!hash.matches("[a-f0-9]{64}")||strength<0||strength>10000||intelligence<0||intelligence>10000||agility<0||agility>10000||level<1||level>1000||points<0||points>10000)throw new IllegalArgumentException("Atributos inválidos");}
    }
    public record Request(long sequence,Operation operation,String target) implements Protocol.Message {
        public Request {if(sequence<1||sequence>1_000_000_000_000L||operation==null||target==null||!target.matches("[a-z][a-z0-9_]{0,30}"))throw new IllegalArgumentException("Operação inválida");}
    }
    public record Progress(long xp,int level,long nextLevelXp,int attack,int defense,String domain) {
        public Progress {
            if(xp<0||xp>1_000_000||level<1||level>1000||nextLevelXp<xp||nextLevelXp>2_000_000||attack<0||attack>10000||defense<0||defense>1000)throw new IllegalArgumentException("Progressão inválida");
            if(!"none".equals(domain))com.projetok.rpg.Domains.get(domain);
        }
    }
    public record Abilities(int attackTicks,int shunpoTicks,int potionTicks,int basicRange) {
        public Abilities {if(attackTicks<0||attackTicks>12||shunpoTicks<0||shunpoTicks>90||potionTicks<0||potionTicks>60||basicRange<1||basicRange>280)throw new IllegalArgumentException("Recarga inválida");}
    }
    public record Sheet(Map<String,Integer> inventory,Map<String,String> equipment,Progress progress,Abilities abilities,String notice) {
        public Sheet(Map<String,Integer> inventory,Map<String,String> equipment){this(inventory,equipment,new Progress(0,1,60,18,0,"none"));}
        public Sheet(Map<String,Integer> inventory,Map<String,String> equipment,Progress progress){this(inventory,equipment,progress,new Abilities(0,0,0,60),"");}
        public Sheet {
            Objects.requireNonNull(progress);
            Objects.requireNonNull(abilities);Objects.requireNonNull(notice);if(notice.length()>200)throw new IllegalArgumentException("Aviso excessivo");
            inventory=Map.copyOf(inventory);equipment=Map.copyOf(equipment);
            if(inventory.size()>20||equipment.size()>4)throw new IllegalArgumentException("Ficha excessiva");
            var slots=new HashSet<Integer>();
            for(var entry:inventory.entrySet()) {
                if(!entry.getKey().matches("(?:[0-9]|1[0-9]):[a-z][a-z0-9_]{0,30}")||entry.getValue()<1||entry.getValue()>99
                    ||!slots.add(Integer.parseInt(entry.getKey().split(":")[0])))throw new IllegalArgumentException("Pilha inválida");
            }
            equipment.forEach((slot,item)->{if(!Set.of("weapon","torso","legs","accessory").contains(slot)||!item.matches("[a-z][a-z0-9_]{0,30}"))throw new IllegalArgumentException("Equipamento inválido");});
        }
    }
    public record State(String hash,Protocol.CombatState combat,Sheet sheet) implements Protocol.Message {
        public State {if(hash==null||!hash.matches("[a-f0-9]{64}"))throw new IllegalArgumentException("Revisão inválida");Objects.requireNonNull(combat);Objects.requireNonNull(sheet);}
    }
    static void writeSheet(DataOutputStream out,Sheet sheet)throws IOException {
        out.writeByte(sheet.inventory().size());
        for(var entry:new TreeMap<>(sheet.inventory()).entrySet()){out.writeUTF(entry.getKey());out.writeByte(entry.getValue());}
        out.writeByte(sheet.equipment().size());
        for(var entry:new TreeMap<>(sheet.equipment()).entrySet()){out.writeUTF(entry.getKey());out.writeUTF(entry.getValue());}
        var p=sheet.progress();out.writeLong(p.xp());out.writeInt(p.level());out.writeLong(p.nextLevelXp());out.writeInt(p.attack());out.writeInt(p.defense());out.writeUTF(p.domain());
        var a=sheet.abilities();out.writeInt(a.attackTicks());out.writeInt(a.shunpoTicks());out.writeInt(a.potionTicks());out.writeInt(a.basicRange());
        out.writeUTF(sheet.notice());
    }
    private static String key(DataInputStream in,int maximum)throws IOException {
        int length=in.readUnsignedShort();if(length<1||length>maximum)throw new IOException("Texto excessivo");
        byte[] bytes=in.readNBytes(length);if(bytes.length!=length)throw new EOFException();
        for(byte value:bytes)if(value<32||value>126)throw new IOException("Chave inválida");
        return new String(bytes,java.nio.charset.StandardCharsets.US_ASCII);
    }
    static Sheet readSheet(DataInputStream in)throws IOException {
        int n=in.readUnsignedByte();if(n>20)throw new IOException("Inventário excessivo");
        var inventory=new TreeMap<String,Integer>();
        for(int i=0;i<n;i++)if(inventory.put(key(in,34),in.readUnsignedByte())!=null)throw new IOException("Pilha duplicada");
        n=in.readUnsignedByte();if(n>4)throw new IOException("Equipamento excessivo");
        var equipment=new TreeMap<String,String>();
        for(int i=0;i<n;i++)if(equipment.put(key(in,9),key(in,31))!=null)throw new IOException("Slot duplicado");
        var progress=new Progress(in.readLong(),in.readInt(),in.readLong(),in.readInt(),in.readInt(),key(in,31));
        var abilities=new Abilities(in.readInt(),in.readInt(),in.readInt(),in.readInt());
        int size=in.readUnsignedShort();if(size>600)throw new IOException("Aviso excessivo");
        byte[] data=in.readNBytes(size);if(data.length!=size)throw new EOFException();
        var encoded=new ByteArrayOutputStream();var output=new DataOutputStream(encoded);output.writeShort(size);output.write(data);
        String notice=new DataInputStream(new ByteArrayInputStream(encoded.toByteArray())).readUTF();
        return new Sheet(inventory,equipment,progress,abilities,notice);
    }
    private GameplayProtocol() { }
}
