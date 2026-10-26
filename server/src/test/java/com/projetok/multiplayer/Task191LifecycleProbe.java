package com.projetok.multiplayer;

import com.projetok.prototype.rpg.LauncherSession;
import com.projetok.studio.*;
import java.net.InetAddress;
import java.nio.file.*;
import java.util.concurrent.CompletableFuture;

/** Processo-filho exclusivamente de teste: interrompe a JVM, nunca energia do computador. */
public final class Task191LifecycleProbe {
    public static void main(String[] args) throws Exception {
        Path root=Path.of(args[0]);Files.createDirectories(root);
        try(var server=LauncherSession.start(InetAddress.getLoopbackAddress(),0,null,root);
            var client=Task183NetworkLabTest.peer(server.port(),"Processo sintetico")) {
            WorldRevisionTest.waitFor(()->!client.revisions().blocked(),client);
            var managed=LauncherSession.managed(server);var target=managed.target();
            var first=new WorldRevisionTest().changed(1,500);
            if(!managed.store().publish(first,"",target.activeHash(),target,LivePublishing.Fault.NONE).active())throw new IllegalStateException("Primeiro commit falhou");
            WorldRevisionTest.waitFor(()->!client.revisions().blocked(),client);
            PublicationTarget crash=new PublicationTarget() {
                public String activeHash(){return target.activeHash();}
                public String compatibility(){return target.compatibility();}
                public CompletableFuture<PreviewServer.Ack> reserve(String expected,LiveRevision next){return target.reserve(expected,next);}
                public CompletableFuture<PreviewServer.Ack> abortReserved(){return target.abortReserved();}
                public CompletableFuture<PreviewServer.Ack> confirmReserved(){return target.confirmReserved();}
                public CompletableFuture<PreviewServer.Ack> activateReserved(){return target.activateReserved().thenApply(ack->{
                    if(ack.accepted()){System.out.println("TASK191 process-halt after activation before pointer/commit");System.out.flush();Runtime.getRuntime().halt(23);}
                    return ack;
                });}
            };
            managed.store().publish(new WorldRevisionTest().changed(2,550),first.hash(),first.hash(),crash,LivePublishing.Fault.NONE);
            throw new IllegalStateException("Ponto de interrupcao nao alcancado");
        }
    }
}
