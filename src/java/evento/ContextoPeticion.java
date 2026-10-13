package evento;

/**
 * Dirección IP de la petición que se está atendiendo, para que los eventos
 * la registren sin pasarla por todos los métodos. La coloca ContextoPeticionFilter
 * al empezar cada petición y la borra al terminar.
 */
public final class ContextoPeticion {

    private static final ThreadLocal<String> IP = new ThreadLocal<>();

    private ContextoPeticion() {
    }

    public static void iniciar(String ip) { IP.set(ip); }

    /** IP del cliente (detrás de un proxy, la primera de X-Forwarded-For), máximo 45 caracteres. */
    public static String ipDe(jakarta.servlet.http.HttpServletRequest request) {
        String reenviada = request.getHeader("X-Forwarded-For");
        String ip = reenviada == null || reenviada.isBlank() ? request.getRemoteAddr() : reenviada.split(",")[0].trim();
        return ip.length() > 45 ? ip.substring(0, 45) : ip;
    }

    /** Si aún no se registró la IP de esta petición (el orden de los filtros no está garantizado), la registra. */
    public static void asegurar(jakarta.servlet.http.HttpServletRequest request) {
        if (IP.get() == null) {
            IP.set(ipDe(request));
        }
    }
    public static void limpiar() { IP.remove(); }
    public static String ip() { return IP.get(); }
}
