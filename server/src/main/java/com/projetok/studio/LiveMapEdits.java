package com.projetok.studio;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;

/** Edições da partida em memória; persistência continua exclusiva do comando explícito de salvar. */
public final class LiveMapEdits {
    private LiveMapEdits(){}
    /** Usa a barreira existente do servidor e clientes, sem escrever histórico ou ponteiros em disco. */
    public static PreviewServer.Ack apply(PublicationTarget target,String expected,LiveRevision next,BooleanSupplier cancelled)throws Exception{
        boolean owned=false;
        try{
            check(cancelled);var reserved=target.reserve(expected,next).get(40,TimeUnit.SECONDS);
            if(!reserved.accepted())return reserved;
            owned=true;check(cancelled);var active=target.activateReserved().get(40,TimeUnit.SECONDS);
            if(!active.accepted())return active;
            check(cancelled);return target.confirmReserved().get(40,TimeUnit.SECONDS);
        }catch(Exception failure){
            if(owned)try{target.abortReserved().get(40,TimeUnit.SECONDS);}catch(Exception recovery){failure.addSuppressed(recovery);}
            throw failure;
        }
    }
    private static void check(BooleanSupplier cancelled){if(cancelled.getAsBoolean())throw new java.util.concurrent.CancellationException("Edição encerrada antes da confirmação");}
    /** Substitui a cena da região escolhida e preserva as conexões das demais regiões. */
    public static LiveRevision replace(LiveRevision base,WorldDocument document,AssetCatalog.Result assets,long number)throws Exception{
        var maps=new TreeMap<>(base.bundle().maps());
        var old=maps.values().stream().filter(d->d.region().equals(document.region())).findFirst().orElseThrow(()->new IllegalArgumentException("A partida não contém esta região: "+document.region()));
        if(!old.id().equals(document.id())&&maps.containsKey(document.id()))throw new IllegalArgumentException("ID já pertence a outro mapa da partida");
        var next=document;
        // Novo mapa vazio troca a arte/física desta cena, conservando ligações já existentes.
        if(!old.id().equals(document.id())&&next.portals().isEmpty())next=next.withPortals(old.portals());
        maps.remove(old.id());maps.put(next.id(),next);
        for(var entry:new ArrayList<>(maps.entrySet())){
            var d=entry.getValue();var portals=d.portals().stream().map(p->p.destination().equals(old.id())?new WorldDocument.Portal(p.id(),p.x(),p.y(),document.id(),p.arrivalX(),p.arrivalY(),p.quest(),p.direction()):p).toList();
            maps.put(entry.getKey(),d.withPortals(portals));
        }
        String start=base.bundle().start().equals(old.id())?document.id():base.bundle().start();
        return LiveRevision.prepare(number,start,maps,assets);
    }
}
