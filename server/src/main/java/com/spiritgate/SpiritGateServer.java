package com.spiritgate;

import com.esotericsoftware.kryonet.Server;
import java.io.IOException;

public class SpiritGateServer {
    public static void main(String[] args) throws IOException {
        Server server = new Server();
        server.start();
        server.bind(54555, 54777);
        System.out.println("Spirit Gate Server iniciado com sucesso na porta 54555!");
    }
}