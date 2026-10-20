package com.projetok.rpg;
import java.nio.file.*;
import java.io.*;
import java.util.*;
/** Catalogo de saves locais, sem apagar entradas corrompidas. Chamar fora do EDT/tick. */
public final class CharacterSaves {
    public record Slot(Path file,RpgSave state,String problem,boolean backup) {
        public String toString(){return state==null?file.getFileName()+" - invalido: "+problem:state.identity().name()+" | "+state.identity().archetype()+" | XP "+state.xp()+(backup?" | BACKUP RECUPERAVEL":"");}
    }
    private final Path folder;
    public CharacterSaves(Path folder){this.folder=folder.toAbsolutePath();}
    public Path newFile(CharacterIdentity identity){return folder.resolve(identity.id()+".properties");}
    public List<Slot> list() throws IOException {
        if(!Files.exists(folder))return List.of();List<Slot> result=new ArrayList<>();
        try(var files=Files.list(folder)) {
            var candidates=files.filter(p->Files.isRegularFile(p,LinkOption.NOFOLLOW_LINKS)&&p.getFileName().toString().endsWith(".properties")).sorted().limit(101).toList();
            if(candidates.size()>100)throw new IOException("Mais de 100 saves; organize a pasta antes de continuar");
            for(Path file:candidates)try{var state=new LocalSaveStore(file).load();RpgWorld.restore(state);result.add(new Slot(file,state,"",false));}
            catch(IOException|IllegalArgumentException invalid) {
                result.add(new Slot(file,null,invalid.getMessage(),false));
                try{var backup=new LocalSaveStore(file).recoverBackup();RpgWorld.restore(backup);result.add(new Slot(file,backup,"",true));}catch(IOException|IllegalArgumentException ignored){ }
            }
        }
        return List.copyOf(result);
    }
    public Path recoveryFile(CharacterIdentity identity){return folder.resolve(identity.id()+"-recovery-"+UUID.randomUUID()+".properties");}
}
