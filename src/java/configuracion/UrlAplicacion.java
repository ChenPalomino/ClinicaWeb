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
        // Detrás de un proxy (Render, Railway...) el protocolo real viene en esta cabecera
        String protocolo = request.getHeader("X-Forwarded-Proto");
        if (protocolo == null || protocolo.isBlank()) {
            protocolo = request.getScheme();
        }
        int puerto = request.getServerPort();
        boolean puertoPorDefecto = ("http".equals(protocolo) && puerto == 80)
                || ("https".equals(protocolo) && (puerto == 443 || puerto == 80));
        return protocolo + "://" + request.getServerName()
                + (puertoPorDefecto ? "" : ":" + puerto)
                + request.getContextPath();
    }
}
