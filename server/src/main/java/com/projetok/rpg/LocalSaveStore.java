package com.projetok.rpg;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/** IO local fora do tick. Valida antes de substituir e preserva backup integro. */
public final class LocalSaveStore {
    @FunctionalInterface public interface Writer { void write(Path path, byte[] bytes) throws IOException; }
    private final Path file;
    private final Writer writer;
    public LocalSaveStore(Path file) { this(file,Files::write); }
    public LocalSaveStore(Path file, Writer writer) { this.file=file.toAbsolutePath(); this.writer=writer; }
    public Path file() { return file; }
    public synchronized RpgSave load() throws IOException { return validated(Files.readAllBytes(checked(file))); }
    public synchronized RpgSave recoverBackup() throws IOException { return validated(Files.readAllBytes(checked(backup()))); }
    private static RpgSave validated(byte[] bytes)throws IOException{var state=decode(bytes);RpgWorld.restore(state);return state;}
    private Path backup() { return file.resolveSibling(file.getFileName()+".bak"); }
    private static Path checked(Path path) throws IOException {
        if(Files.size(path)>262144) throw new IOException("Save excessivo"); return path;
    }
    public synchronized void save(RpgSave state) throws IOException {
        RpgWorld.restore(state);byte[] data=encode(state); decode(data);
        Files.createDirectories(file.getParent());
        Path temporary=Files.createTempFile(file.getParent(),"offline-",".tmp");
        try {
            writer.write(temporary,data);
            decode(Files.readAllBytes(checked(temporary)));
            if(Files.exists(file)) {
                try { load(); Files.copy(file,backup(),StandardCopyOption.REPLACE_EXISTING); }
                catch(IllegalArgumentException corrupt) { throw new IOException("Save atual invalido; recuperar antes de substituir",corrupt); }
            }
            try { Files.move(temporary,file,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING); }
            catch(AtomicMoveNotSupportedException e) { Files.move(temporary,file,StandardCopyOption.REPLACE_EXISTING); }
        } finally { Files.deleteIfExists(temporary); }
    }
    public static byte[] encode(RpgSave state) throws IOException {
        Properties p=new Properties();
        var i=state.identity();
        p.setProperty("schema","1"); p.setProperty("id",i.id().toString()); p.setProperty("name",i.name());
        p.setProperty("archetype",i.archetype()); p.setProperty("appearance",i.appearance());
        p.setProperty("baseHp",""+i.baseHp()); p.setProperty("baseAttack",""+i.baseAttack());
        p.setProperty("region",state.region()); p.setProperty("x",""+state.x()); p.setProperty("y",""+state.y());
        p.setProperty("hp",""+state.hp()); p.setProperty("xp",""+state.xp()); p.setProperty("coins",""+state.coins()); p.setProperty("ticks",""+state.ticks());
        state.inventory().forEach((k,v)->p.setProperty("inventory."+k,""+v));
        state.equipment().forEach((k,v)->p.setProperty("equipment."+k,v));
        state.quests().forEach((k,v)->p.setProperty("quest."+k,""+v));
        state.flags().forEach(k->p.setProperty("flag."+k,"true"));
        ByteArrayOutputStream out=new ByteArrayOutputStream(); p.store(out,"Spirit Gate offline V1"); return out.toByteArray();
    }
    public static RpgSave decode(byte[] bytes) throws IOException {
        if(bytes.length>262144) throw new IOException("Save excessivo");
        Properties p=new Properties() {
            @Override public synchronized Object put(Object k,Object v) {
                if(containsKey(k)) throw new IllegalArgumentException("Campo duplicado"); return super.put(k,v);
            }
        };
        p.load(new ByteArrayInputStream(bytes));
        try {
            if(!"1".equals(take(p,"schema"))) throw new IllegalArgumentException("Schema de save incompativel");
            CharacterIdentity identity=new CharacterIdentity(UUID.fromString(take(p,"id")),take(p,"name"),take(p,"archetype"),take(p,"appearance"),"village",Integer.parseInt(take(p,"baseHp")),Integer.parseInt(take(p,"baseAttack")));
            String region=take(p,"region"); float x=Float.parseFloat(take(p,"x")),y=Float.parseFloat(take(p,"y"));
            int hp=Integer.parseInt(take(p,"hp")),coins=Integer.parseInt(take(p,"coins")); long xp=Long.parseLong(take(p,"xp")),ticks=Long.parseLong(take(p,"ticks"));
            Map<String,Integer> inventory=new TreeMap<>(),quests=new TreeMap<>(); Map<String,String> equipment=new TreeMap<>(); Set<String> flags=new TreeSet<>();
            for(String key:p.stringPropertyNames()) {
                String v=p.getProperty(key);
                if(key.startsWith("inventory.")) inventory.put(key.substring(10),Integer.parseInt(v));
                else if(key.startsWith("equipment.")) equipment.put(key.substring(10),v);
                else if(key.startsWith("quest.")) quests.put(key.substring(6),Integer.parseInt(v));
                else if(key.startsWith("flag.") && "true".equals(v)) flags.add(key.substring(5));
                else throw new IllegalArgumentException("Campo desconhecido: "+key);
            }
            return new RpgSave(identity,region,x,y,hp,xp,coins,inventory,equipment,quests,flags,ticks);
        } catch(NullPointerException | NumberFormatException e) { throw new IllegalArgumentException("Save incompleto/corrompido",e); }
    }
    private static String take(Properties p,String key) { return Objects.requireNonNull((String)p.remove(key),key); }
}
