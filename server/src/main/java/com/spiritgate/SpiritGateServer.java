package com.spiritgate;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.esotericsoftware.kryonet.Connection; // Importante: Importar Connection da rede
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;

public class SpiritGateServer {

    public static void main(String[] args) throws IOException {
        // Teste de conexão com banco
        try (java.sql.Connection conn = DatabaseManager.getConnection()) {
            System.out.println("Conexão com o banco de dados estabelecida com sucesso!");
        } catch (SQLException e) {
            System.err.println("Erro ao conectar no banco: " + e.getMessage());
        }

        Server server = new Server();
        Network.register(server);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Parando servidor...");
            server.stop();
        }));

        server.addListener(new Listener() {
            @Override
            public void received(Connection connection, Object object) {
                if (object instanceof Network.LoginRequest) {
                    Network.LoginRequest request = (Network.LoginRequest) object;

                    // Aqui chamamos o método que está lá embaixo
                    if (validarUsuario(request.username, request.password)) {
                        System.out.println("Usuário " + request.username + " logou!");
                        // connection.sendTCP(new Network.LoginSuccess()); // Adicione a classe LoginSuccess no Network
                    } else {
                        System.out.println("Login falhou para " + request.username);
                    }
                }
            }
        });

        server.start();
        server.bind(54555, 54777);
        System.out.println("Spirit Gate Server iniciado com sucesso na porta 54555!");
    }

    // O método de validação fica aqui, limpo e organizado
    public static boolean validarUsuario(String username, String password) {
        String query = "SELECT password_hash FROM users WHERE username = ?";

        try (java.sql.Connection conn = DatabaseManager.getConnection(); PreparedStatement pstmt = conn.prepareStatement(query)) {

            pstmt.setString(1, username);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    String hashNoBanco = rs.getString("password_hash");
                    return password.equals(hashNoBanco);
                }
            }
        } catch (SQLException e) {
            System.err.println("Erro ao consultar o banco: " + e.getMessage());
        }
        return false;
    }
}
