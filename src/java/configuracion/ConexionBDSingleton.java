package configuracion;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * PATRÓN: Singleton (creacional)
 * Propósito: garantizar UNA sola instancia que administra la conexión a la
 * base de datos (Supabase en producción / PostgreSQL local en desarrollo).
 *
 * Bloques del patrón:
 *  1. Atributo estático privado 'instancia' (volatile para hilos).
 *  2. Constructor privado: nadie puede hacer 'new ConexionBDSingleton()'.
 *  3. Método estático getInstancia() con Double-Checked Locking.
 *  4. getConexion(): devuelve la conexión y la RECONECTA si se cayó,
 *     sin crear otra instancia del Singleton.
 *
 * Seguridad: las credenciales NO están en el código. Se leen de variables
 * de entorno (DB_URL, DB_USER, DB_PASSWORD). Si DB_URL no existe se usa la
 * BD local, pero la contraseña igual se lee de DB_PASSWORD.
 */
public class ConexionBDSingleton {

    private static final String URL_LOCAL = "jdbc:postgresql://localhost:5432/clinica_db";
    private static final String USUARIO_LOCAL = "postgres";

    // 1. Instancia única
    private static volatile ConexionBDSingleton instancia;
    private Connection conexion;

    // 2. Constructor privado
    private ConexionBDSingleton() {
        conectar();
    }

    // 3. Punto de acceso global (Double-Checked Locking)
    public static ConexionBDSingleton getInstancia() {
        if (instancia == null) {
            synchronized (ConexionBDSingleton.class) {
                if (instancia == null) {
                    instancia = new ConexionBDSingleton();
                }
            }
        }
        return instancia;
    }

    /** Crea una conexión física nueva leyendo las variables de entorno. */
    private Connection crearConexion() throws SQLException {
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            throw new SQLException("No se encontró el driver de PostgreSQL", e);
        }
        String url = System.getenv("DB_URL");
        String usuario = System.getenv("DB_USER");
        String password = System.getenv("DB_PASSWORD");

        if (url == null || url.isBlank()) {
            url = URL_LOCAL;
            usuario = (usuario == null || usuario.isBlank()) ? USUARIO_LOCAL : usuario;
        }
        if (password == null) {
            System.err.println("ConexionBDSingleton: falta la variable de entorno DB_PASSWORD.");
        }
        return DriverManager.getConnection(url, usuario, password);
    }

    private void conectar() {
        try {
            conexion = crearConexion();
            String url = System.getenv("DB_URL");
            System.out.println("ConexionBDSingleton: usando BD " + (url == null || url.isBlank() ? "local." : "en la nube."));
        } catch (SQLException e) {
            conexion = null;
            System.err.println("ConexionBDSingleton: error al conectar -> " + e.getMessage());
        }
    }

    // 4. Obtiene la conexión compartida; si está cerrada o 'muerta' (Supabase
    //    corta las conexiones inactivas) la vuelve a abrir. Sigue siendo la misma instancia.
    public synchronized Connection getConexion() throws SQLException {
        if (conexion == null || conexion.isClosed() || !conexion.isValid(3)) {
            conectar();
        }
        if (conexion == null) {
            throw new SQLException("No se pudo establecer conexión con la base de datos.");
        }
        return conexion;
    }

    /**
     * 5. Conexión NUEVA y exclusiva para operaciones de varios pasos que deben
     *    ser atómicas (transacciones: o se guarda todo o nada). Viene con
     *    autoCommit desactivado. Quien la pide debe hacer commit/rollback y
     *    CERRARLA (try-with-resources). La conexión compartida no se toca.
     */
    public Connection abrirConexionTransaccional() throws SQLException {
        Connection nueva = crearConexion();
        nueva.setAutoCommit(false);
        return nueva;
    }
}
