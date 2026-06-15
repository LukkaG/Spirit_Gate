package com.spiritgate;

import java.io.IOException;

import com.esotericsoftware.kryonet.Server;

public class SpiritGateServer {
    public static void main(String[] args) throws IOException {
        Server server = new Server();
        server.start();
        server.bind(54555, 54777);
        System.out.println("Spirit Gate Server iniciado com sucesso na porta 54555!");
    }
}