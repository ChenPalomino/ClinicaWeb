package com.clinica.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConexionBD {

    private static ConexionBD instancia;
    private Connection conexion;

    private ConexionBD() {
        try {
            Class.forName("org.postgresql.Driver");

            // Verificamos si Render nos está pasando la variable de entorno
            String urlEnv = System.getenv("DB_URL");

            if (urlEnv != null && !urlEnv.isEmpty()) {
                // --- CONFIGURACIÓN PARA RENDER (NUBE) ---
                String url = System.getenv("jdbc:postgresql://dpg-datqviek1f9s739b5m50-a.oregon-postgres.render.com:5432/clinica_db_m97p");
                String usuario = System.getenv("clinica_db_m97p_user");
                String password = System.getenv("kQJalEqqPubZgsV2cjDM3yIIhyYLn3FI");
                conexion = DriverManager.getConnection(url, usuario, password);
                System.out.println("Conexión exitosa a la BD de Render.");
            } else {
                // --- CONFIGURACIÓN LOCAL (TU PC / pgAdmin) ---
                // Cambia 'clinica_db', 'postgres' y tu contraseña por los tuyos reales
                String url = "jdbc:postgresql://localhost:5432/clinica_db";
                String usuario = "postgres";
                String password = "60247403.";
                conexion = DriverManager.getConnection(url, usuario, password);
                System.out.println("Conexión exitosa a la BD local.");
            }
        } catch (ClassNotFoundException | SQLException e) {
            System.err.println("Error al conectar a la Base de Datos: " + e.getMessage());
        }
    }

    public static synchronized ConexionBD getInstancia() {
        if (instancia == null) {
            instancia = new ConexionBD();
        }
        try {
            if (instancia.conexion == null || instancia.conexion.isClosed()) {
                instancia = new ConexionBD();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return instancia;
    }

    public Connection getConexion() {
        return conexion;
    }
}
