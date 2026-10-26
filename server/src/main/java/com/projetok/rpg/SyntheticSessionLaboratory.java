package com.projetok.rpg;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

/** Controle local e isolado de laboratório; não é API de login ou autenticação de rede. */
public final class SyntheticSessionLaboratory implements AutoCloseable {
    public static final class Handle {
        private final SyntheticSessionLaboratory owner;
        private final UUID id;
        private final SyntheticOnlineRepository.Grant grant;
        private Handle(SyntheticSessionLaboratory owner,UUID id,SyntheticOnlineRepository.Grant grant){
            this.owner=owner;this.id=id;this.grant=grant;
        }
    }
    private final SyntheticOnlineRepository repository;
    private final Map<UUID,Handle> active=new HashMap<>();
    private boolean closed;
    public SyntheticSessionLaboratory(Path root)throws IOException{repository=new SyntheticOnlineRepository(root);}
    public synchronized Handle create(String race)throws IOException{
        ensureOpen();var grant=repository.create(race);var document=repository.loadForLocalLaboratory(grant);
        var handle=new Handle(this,document.id(),grant);active.put(document.id(),handle);return handle;
    }
    /** UUID é seleção confiável local de fixture, nunca autorização recebida de um cliente. */
    public synchronized Handle resume(UUID id)throws IOException{
        ensureOpen();Objects.requireNonNull(id);
        if(active.containsKey(id))throw new IOException("Personagem sintetico ja possui sessao local");
        var grant=repository.resumeForLocalLaboratory(id);var handle=new Handle(this,id,grant);active.put(id,handle);return handle;
    }
    public synchronized SyntheticOnlineRepository.Document snapshot(Handle handle)throws IOException{
        require(handle);return repository.loadForLocalLaboratory(handle.grant);
    }
    public synchronized SyntheticOnlineRepository.Document save(Handle handle,long base,RpgSave state)throws IOException{
        require(handle);return repository.writeIdenticalRetry(handle.grant,base,state);
    }
    public synchronized void disconnect(Handle handle){require(handle);active.remove(handle.id);}
    private void ensureOpen(){if(closed)throw new IllegalStateException("Laboratorio encerrado");}
    private void require(Handle handle){
        ensureOpen();
        if(handle==null||handle.owner!=this||active.get(handle.id)!=handle)throw new SecurityException("Posse de sessao sintetica invalida");
    }
    @Override public synchronized void close(){closed=true;active.clear();}
}
