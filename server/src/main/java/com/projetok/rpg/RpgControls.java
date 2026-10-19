package com.projetok.rpg;
import java.util.*;
import java.nio.file.*;
import java.io.*;
/** Atalhos locais por ID estavel; WASD e teclas de sistema reservadas. */
public record RpgControls(Map<String,String> keys) {
    public static final Map<String,String> DEFAULTS=Map.of("attack","SPACE","ability","Q","potion","1","interact","F","inventory","I","journal","J","map","M","save","F5","revive","R");
    public RpgControls {
        keys=Map.copyOf(keys);if(!keys.keySet().equals(DEFAULTS.keySet()))throw new IllegalArgumentException("Acoes de controle invalidas");Set<String> used=new HashSet<>();
        for(String key:keys.values())if(!key.matches("[A-Z0-9]|F[1-9]|F1[0-2]|SPACE")||Set.of("W","A","S","D","F4").contains(key)||!used.add(key))throw new IllegalArgumentException("Atalho reservado, invalido ou repetido: "+key);
    }
    public String key(String action){return keys.get(action);}
    public static RpgControls load(Path file)throws IOException {
        if(!Files.exists(file))return new RpgControls(DEFAULTS);if(Files.size(file)>8192)throw new IOException("Atalhos excessivos");Properties p=new Properties();try(var in=Files.newInputStream(file)){p.load(in);}if(!"1".equals(p.remove("schema")))throw new IOException("Schema de controles invalido");Map<String,String> keys=new TreeMap<>();p.forEach((k,v)->keys.put(k.toString(),v.toString()));return new RpgControls(keys);
    }
    public void save(Path file)throws IOException {
        Path target=file.toAbsolutePath();Files.createDirectories(target.getParent());Properties p=new Properties();p.setProperty("schema","1");keys.forEach(p::setProperty);Path temp=Files.createTempFile(target.getParent(),"controls-",".tmp");try{try(var out=Files.newOutputStream(temp)){p.store(out,"Spirit Gate controls");}load(temp);try{Files.move(temp,target,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}catch(AtomicMoveNotSupportedException e){Files.move(temp,target,StandardCopyOption.REPLACE_EXISTING);}}finally{Files.deleteIfExists(temp);}
    }
}
