
package com.clinica.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 *
 * @author ASUS
 */
public class ConexionBD {

    // 1. Instancia estática privada (Patrón Singleton)
    private static ConexionBD instancia;
    private Connection conexion;

    // Credenciales de PostgreSQL
    private final String URL = "jdbc:postgresql://localhost:5432/clinica_db";
    private final String USER = "postgres";
    // REEMPLAZA "tu_contraseña" POR LA CLAVE QUE USAS EN PGADMIN4
    private final String PASSWORD = "60247403.";

    // 2. Constructor privado para evitar instanciación externa (Patrón Singleton)
    private ConexionBD() {
        try {
            // Cargar el driver de PostgreSQL
            Class.forName("org.postgresql.Driver");
            this.conexion = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("Conexión exitosa a PostgreSQL.");
        } catch (ClassNotFoundException | SQLException e) {
            System.err.println("Error en la conexión a BD: " + e.getMessage());
        }
    }

    // 3. Método estático público para obtener la única instancia (Patrón Singleton)
    public static ConexionBD getInstancia() {
        if (instancia == null) {
            instancia = new ConexionBD();
        }
        return instancia;
    }

    // Método para exponer el objeto Connection al DAO
    public Connection getConexion() {
        return conexion;
    }

    // Método para cerrar la conexión
    public void cerrarConexion() {
        try {
            if (conexion != null && !conexion.isClosed()) {
                conexion.close();
                instancia = null; // Resetear el singleton al cerrar
            }
        } catch (SQLException e) {
            System.err.println("Error al cerrar conexión: " + e.getMessage());
        }
    }

}
