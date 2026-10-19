package com.projetok;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Adaptador JDBC legado: dados de conexão são recebidos do ambiente, jamais do código.
 * O protótipo visual não usa PostgreSQL; este adaptador permanece para o login antigo.
 */
public final class DatabaseManager {
    private DatabaseManager() { }

    /** Abre uma conexão JDBC utilizando credenciais externas ao repositório. */
    public static Connection getConnection() throws SQLException {
        String url = required("SPIRIT_DB_URL");
        String username = required("SPIRIT_DB_USER");
        String password = required("SPIRIT_DB_PASSWORD");
        return DriverManager.getConnection(url, username, password);
    }

    /** Recusa configurações ausentes para evitar conexão inesperada com outro banco. */
    private static String required(String name) throws SQLException {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new SQLException("Variavel de ambiente obrigatoria ausente: " + name);
        }
        return value;
    }
}
