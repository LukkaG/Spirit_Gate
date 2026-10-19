package com.projetok;

import java.io.IOException;

import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Listener;

public class Kclient {

    public static void main(String[] args) throws IOException {
        Client client = new Client();
        client.start();
        Network.register(client);
        client.connect(5000, "localhost", 54555, 54777);
        client.addListener(new Listener() {
            public void received(com.esotericsoftware.kryonet.Connection connection, Object object) {
                if (object instanceof Network.LoginSuccess) {
                    System.out.println("SUCESSO: O servidor aceitou o login!");
                } else if (object instanceof Network.LoginFailed) {
                    System.out.println("FALHA: " + ((Network.LoginFailed) object).motivo);
                }
            }
        });
        Network.LoginRequest login = new Network.LoginRequest();
        login.username = "Charles";
        login.password = "12345";
        client.sendTCP(login);
    }
}
