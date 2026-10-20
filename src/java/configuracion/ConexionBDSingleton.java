package configuracion;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.atomic.AtomicBoolean;

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
 *  5. abrirConexionTransaccional(): conexión exclusiva para transacciones,
 *     reutilizada desde un pequeño grupo (no se abre una nueva cada vez).
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
    private long ultimoUso;                                       // última vez que se entregó la conexión
    private static final long INACTIVIDAD_MAXIMA_MS = 30_000;     // pasado este tiempo se comprueba antes de usarla

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
    //    RENDIMIENTO: comprobar la conexión (isValid) es un viaje de ida y vuelta a
    //    Supabase; antes se hacía en CADA consulta. Ahora solo si estuvo sin usarse
    //    más de 30 segundos (que es cuando el servidor podría haberla cortado).
    public synchronized Connection getConexion() throws SQLException {
        long ahora = System.currentTimeMillis();
        boolean inactiva = ahora - ultimoUso > INACTIVIDAD_MAXIMA_MS;
        if (conexion == null || conexion.isClosed() || (inactiva && !conexion.isValid(3))) {
            conectar();
        }
        if (conexion == null) {
            throw new SQLException("No se pudo establecer conexión con la base de datos.");
        }
        ultimoUso = ahora;
        return conexion;
    }

    /**
     * 5. Conexión EXCLUSIVA para operaciones de varios pasos que deben ser
     *    atómicas (transacciones: o se guarda todo o nada, RNF-08). Viene con
     *    autoCommit desactivado. Quien la pide hace commit/rollback y la CIERRA
     *    (try-with-resources). La conexión compartida no se toca.
     *
     *    RENDIMIENTO: abrir una conexión con Supabase (red + cifrado + clave)
     *    tarda bastante. Por eso, al "cerrarla", la conexión no se destruye:
     *    se deshace lo que no se confirmó y vuelve a un pequeño grupo de
     *    conexiones libres para la siguiente transacción.
     */
    public Connection abrirConexionTransaccional() throws SQLException {
        Connection fisica = tomarLibre();
        if (fisica == null) {
            fisica = crearConexion();
        }
        fisica.setAutoCommit(false);
        return envolver(fisica);
    }

    // ---------- grupo de conexiones transaccionales libres ----------
    private static final int MAXIMO_LIBRES = 4;
    private final Deque<Connection> libres = new ArrayDeque<>();

    private Connection tomarLibre() {
        while (true) {
            Connection libre;
            synchronized (libres) {
                libre = libres.pollFirst();
            }
            if (libre == null) {
                return null;
            }
            try {
                // Se comprueba SIEMPRE antes de una transacción (un viaje corto a la BD,
                // mucho más barato que abrir una conexión nueva): así un pago nunca falla
                // por una conexión que el servidor cerró mientras esperaba en el grupo.
                if (!libre.isClosed() && libre.isValid(3)) {
                    return libre;
                }
            } catch (SQLException e) {
                // conexión dañada: se descarta y se prueba con la siguiente
            }
            cerrarSinError(libre);
        }
    }

    /** Al cerrar la conexión que recibió el repositorio, la física vuelve al grupo. */
    private Connection envolver(Connection fisica) {
        AtomicBoolean cerrada = new AtomicBoolean(false);
        return (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(), new Class<?>[]{Connection.class},
                (proxy, metodo, args) -> {
                    switch (metodo.getName()) {
                        case "close":
                            if (cerrada.compareAndSet(false, true)) {
                                devolver(fisica);
                            }
                            return null;
                        case "isClosed":
                            return cerrada.get() || fisica.isClosed();
                        default:
                            if (cerrada.get()) {
                                throw new SQLException("La conexión ya fue cerrada.");
                            }
                            try {
                                return metodo.invoke(fisica, args);
                            } catch (InvocationTargetException e) {
                                throw e.getCause();
                            }
                    }
                });
    }

    private void devolver(Connection fisica) {
        try {
            if (fisica.isClosed()) {
                return;
            }
            fisica.rollback();          // descarta lo que no se confirmó (nunca queda una transacción a medias)
            synchronized (libres) {
                if (libres.size() < MAXIMO_LIBRES) {
                    libres.addFirst(fisica);
                    return;
                }
            }
        } catch (SQLException e) {
            // conexión dañada: no se reutiliza
        }
        cerrarSinError(fisica);
    }

    private static void cerrarSinError(Connection c) {
        try {
            c.close();
        } catch (SQLException e) {
            // ya estaba cerrada o la red se cortó: nada que hacer
        }
    }
}
