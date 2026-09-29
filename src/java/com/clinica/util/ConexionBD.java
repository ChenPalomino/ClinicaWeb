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
                // Pasamos los NOMBRES de las variables de entorno, no los valores
                String url = System.getenv("DB_URL");
                String usuario = System.getenv("DB_USER");
                String password = System.getenv("DB_PASSWORD");
                
                conexion = DriverManager.getConnection(url, usuario, password);
                System.out.println("Conexión exitosa a la BD de Render.");
            } else {
                // --- CONFIGURACIÓN LOCAL (TU PC / pgAdmin) ---
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