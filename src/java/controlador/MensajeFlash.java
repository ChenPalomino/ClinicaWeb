package controlador;

import jakarta.servlet.http.HttpSession;

/**
 * Mensaje que se muestra UNA sola vez después de una redirección
 * (patrón Post/Redirect/Get): "Usuario creado", "Cuenta desbloqueada"...
 * Se guarda en la sesión y la plantilla lo borra al mostrarlo.
 */
public class MensajeFlash {

    private static final String CLAVE = "mensajeFlash";

    private final String tipo;    // success | danger | warning | info  (clases de Bootstrap)
    private final String texto;

    private MensajeFlash(String tipo, String texto) {
        this.tipo = tipo;
        this.texto = texto;
    }

    public String getTipo() { return tipo; }
    public String getTexto() { return texto; }

    public String getIcono() {
        switch (tipo) {
            case "success": return "fas fa-check-circle";
            case "danger":  return "fas fa-exclamation-triangle";
            case "warning": return "fas fa-exclamation-circle";
            default:        return "fas fa-info-circle";
        }
    }

    public static void exito(HttpSession session, String texto) { session.setAttribute(CLAVE, new MensajeFlash("success", texto)); }
    public static void error(HttpSession session, String texto) { session.setAttribute(CLAVE, new MensajeFlash("danger", texto)); }
    public static void aviso(HttpSession session, String texto) { session.setAttribute(CLAVE, new MensajeFlash("warning", texto)); }

    /** Devuelve el mensaje pendiente (o null) y lo elimina de la sesión. */
    public static MensajeFlash tomar(HttpSession session) {
        if (session == null) {
            return null;
        }
        MensajeFlash m = (MensajeFlash) session.getAttribute(CLAVE);
        session.removeAttribute(CLAVE);
        return m;
    }
}
