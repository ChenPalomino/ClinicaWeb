package controlador;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/**
 * Ayudas comunes de los controladores CRUD: leer números, decimales y fechas
 * de un formulario sin que un valor mal escrito provoque un error 500, y
 * mostrar el resultado de una acción con un mensaje (PRG + MensajeFlash).
 */
public final class ControladorUtil {

    private ControladorUtil() {
    }

    public static int entero(String valor, int porDefecto) {
        try {
            return Integer.parseInt(valor == null ? "" : valor.trim());
        } catch (NumberFormatException e) {
            return porDefecto;
        }
    }

    /** null si el campo viene vacío o no es un número (listas "— Ninguno —"). */
    public static Integer enteroONulo(String valor) {
        try {
            return valor == null || valor.isBlank() ? null : Integer.valueOf(valor.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Acepta "80", "80.5" o "80,50". null si está vacío o no es un número. */
    public static BigDecimal decimal(String valor) {
        try {
            return valor == null || valor.isBlank() ? null : new BigDecimal(valor.trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Fecha del <input type="date"> (yyyy-MM-dd); null si está vacía o es inválida. */
    public static LocalDate fecha(String valor) {
        try {
            return valor == null || valor.isBlank() ? null : LocalDate.parse(valor.trim());
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    /** error == null -> mensaje de éxito; si no, el error. */
    public static void resultado(HttpSession session, String error, String exito) {
        if (error == null) {
            MensajeFlash.exito(session, exito);
        } else {
            MensajeFlash.error(session, error);
        }
    }

    /** Conserva búsqueda, filtros y página al volver al listado después de una acción. */
    public static String regresoListado(HttpServletRequest request) {
        String q = request.getParameter("regreso");
        return (q == null || q.isBlank() || !q.matches("[A-Za-z0-9=&%+._-]*")) ? "" : "?" + q;
    }
}
