package controlador;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import modelo.dto.RangoFechasDTO;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Set;

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

    /**
     * Rango de fechas de registro de un listado (parámetros "desde" y "hasta").
     * Sin fechas ni texto buscado = últimos 30 días (ver RangoFechasDTO).
     */
    public static RangoFechasDTO rango(HttpServletRequest request, String... otrosFiltros) {
        boolean hayBusqueda = conValor(request, "q");
        for (String nombre : otrosFiltros) {
            hayBusqueda = hayBusqueda || conValor(request, nombre);
        }
        return RangoFechasDTO.de(fecha(request.getParameter("desde")), fecha(request.getParameter("hasta")),
                hayBusqueda, LocalDate.now());
    }

    private static final Set<String> PARAMETROS_SIN_FILTRO = Set.of("_t", "accion", "regreso");

    public static boolean busquedaSolicitada(HttpServletRequest request) {
        for (String nombre : request.getParameterMap().keySet()) {
            if (!PARAMETROS_SIN_FILTRO.contains(nombre)) {
                return true;
            }
        }
        return false;
    }

    private static boolean conValor(HttpServletRequest request, String nombre) {
        String v = request.getParameter(nombre);
        return v != null && !v.isBlank();
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
