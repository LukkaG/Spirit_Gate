package com.projetok.studio;

import java.util.concurrent.CompletableFuture;

/** Possession of this local Java reference authorizes only the selected server. */
public interface PublicationTarget {
    String activeHash();
    default String compatibility(){return "studio-live-local-v1";}
    CompletableFuture<PreviewServer.Ack> reserve(String expected,LiveRevision next);
    CompletableFuture<PreviewServer.Ack> activateReserved();
    CompletableFuture<PreviewServer.Ack> confirmReserved();
    CompletableFuture<PreviewServer.Ack> abortReserved();
}
