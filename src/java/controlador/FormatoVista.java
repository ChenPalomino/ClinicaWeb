package controlador;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;

/** Formatos de presentación usados en las vistas JSP del dashboard. */
public final class FormatoVista {

    private FormatoVista() {
    }

    /** 11/10/2026 14:35, o "Nunca" si no hay fecha. */
    public static String fechaHora(Timestamp fecha) {
        return fecha == null ? "Nunca" : new SimpleDateFormat("dd/MM/yyyy HH:mm").format(fecha);
    }

    public static String fecha(Timestamp fecha) {
        return fecha == null ? "—" : new SimpleDateFormat("dd/MM/yyyy").format(fecha);
    }

    /** 1,250.50 (montos en soles con 2 decimales). */
    public static String soles(BigDecimal monto) {
        return monto == null ? "0.00" : String.format(java.util.Locale.US, "%,.2f", monto);
    }

    /** Color de la etiqueta del rol (clases de Bootstrap). */
    public static String colorRol(int idRol) {
        switch (idRol) {
            case 1: return "badge-dark";
            case 2: return "badge-info";
            case 3: return "badge-primary";
            default: return "badge-light border";
        }
    }

    /** Texto del motivo de bloqueo para el tooltip del listado. */
    public static String motivoBloqueo(String motivo) {
        if ("INTENTOS".equals(motivo)) return "3 contraseñas incorrectas (se libera sola en 24 h o con código)";
        if ("SEGURIDAD".equals(motivo)) return "El usuario reportó un cambio no autorizado (\"No fui yo\")";
        if ("ADMINISTRADOR".equals(motivo)) return "Bloqueado manualmente por un administrador";
        return "Bloqueado";
    }
}
