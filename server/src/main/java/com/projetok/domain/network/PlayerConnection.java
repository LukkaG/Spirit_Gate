package com.projetok.domain.network;

/**
 * Porta entre o domínio e a camada de rede. A implementação concreta (que fala com
 * Kryonet) vive em infrastructure/network — o domínio nunca importa Kryonet diretamente.
 */
public interface PlayerConnection {
    void send(Object packet);
    void disconnect();
}
