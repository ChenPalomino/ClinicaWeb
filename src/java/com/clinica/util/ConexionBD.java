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

            // Leer las variables de entorno configuradas en Render
            String url = System.getenv("DB_URL");
            String usuario = System.getenv("DB_USER");
            String password = System.getenv("DB_PASSWORD");

            if (url != null && !url.isEmpty()) {
                // --- CONEXIÓN A LA NUBE (Supabase / Render) ---
                conexion = DriverManager.getConnection(url, usuario, password);
                System.out.println("Conexión exitosa a la Base de Datos en la nube.");
            } else {
                // --- CONEXIÓN LOCAL (Tu PC) ---
                url = "jdbc:postgresql://localhost:5432/clinica_db";
                usuario = "postgres";
                password = "60247403.";
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