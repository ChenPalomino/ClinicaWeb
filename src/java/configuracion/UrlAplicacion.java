package configuracion;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Obtiene la dirección pública de la aplicación para armar los enlaces de
 * los correos (por ejemplo, el enlace "No fui yo").
 *  - Si existe la variable de entorno APP_URL, se usa esa
 *    (ej. https://clinicaweb.onrender.com/ClinicaWeb).
 *  - Si no, se arma con los datos de la petición actual.
 */
public final class UrlAplicacion {

    private UrlAplicacion() {
    }

    public static String base(HttpServletRequest request) {
        String configurada = System.getenv("APP_URL");
        if (configurada != null && !configurada.isBlank()) {
            return configurada.endsWith("/") ? configurada.substring(0, configurada.length() - 1) : configurada;
        }
        String protocolo = protocolo(request);
        int puerto = request.getServerPort();
        return protocolo + "://" + request.getServerName()
                + (puertoPorDefecto(protocolo, puerto) ? "" : ":" + puerto)
                + request.getContextPath();
    }

    /** Detrás de un proxy (Render, Railway...) el protocolo real viene en X-Forwarded-Proto. */
    private static String protocolo(HttpServletRequest request) {
        String protocolo = request.getHeader("X-Forwarded-Proto");
        return protocolo == null || protocolo.isBlank() ? request.getScheme() : protocolo;
    }

    private static boolean puertoPorDefecto(String protocolo, int puerto) {
        return ("http".equals(protocolo) && puerto == 80) || ("https".equals(protocolo) && (puerto == 443 || puerto == 80));
    }
}
