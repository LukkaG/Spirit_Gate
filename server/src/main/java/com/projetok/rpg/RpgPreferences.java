package com.projetok.rpg;
import java.nio.file.*;
import java.io.*;
import java.util.*;
public record RpgPreferences(int volume,boolean muted,boolean environment) {
    public RpgPreferences{if(volume<0||volume>100)throw new IllegalArgumentException("Volume invalido");}
    public static RpgPreferences defaults(){return new RpgPreferences(40,false,true);}
    public static RpgPreferences load(Path file)throws IOException{
        if(!Files.exists(file))return defaults();if(Files.size(file)>4096)throw new IOException("Preferencias excessivas");Properties p=new Properties();try(var in=Files.newInputStream(file)){p.load(in);}
        if(!"1".equals(p.remove("schema"))||!p.stringPropertyNames().equals(Set.of("volume","muted","environment")))throw new IOException("Preferencias incompativeis");
        for(String key:new String[]{"muted","environment"})if(!Set.of("true","false").contains(p.getProperty(key)))throw new IOException("Booleano invalido");
        return new RpgPreferences(Integer.parseInt(p.getProperty("volume")),Boolean.parseBoolean(p.getProperty("muted")),Boolean.parseBoolean(p.getProperty("environment")));
    }
    public void save(Path file)throws IOException{
        Path target=file.toAbsolutePath();Files.createDirectories(target.getParent());Path temp=Files.createTempFile(target.getParent(),"preferences-",".tmp");
        try{Files.writeString(temp,"schema=1\nvolume="+volume+"\nmuted="+muted+"\nenvironment="+environment+"\n");load(temp);try{Files.move(temp,target,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}catch(AtomicMoveNotSupportedException e){Files.move(temp,target,StandardCopyOption.REPLACE_EXISTING);}}finally{Files.deleteIfExists(temp);}
    }
}
