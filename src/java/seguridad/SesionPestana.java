package seguridad;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpSession;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

/**
 * Sesión de UNA pestaña del navegador.
 *
 * El navegador tiene una sola cookie de sesión para todas sus pestañas. Para
 * que cada pestaña pueda tener su propia cuenta (admin en una, médico en otra),
 * los datos de cada pestaña se guardan en la sesión real con un prefijo propio:
 *     "pestana.<id>.usuarioLogueado", "pestana.<id>.perfil", ...
 * Todo el resto del sistema sigue usando request.getSession() sin cambios.
 *
 * Seguridad: el identificador de la pestaña NO es una credencial; además hace
 * falta la cookie de sesión (HttpOnly), que solo tiene ese navegador.
 */
public class SesionPestana implements HttpSession {

    static final String PREFIJO = "pestana.";

    private final HttpSession real;
    private final String prefijo;

    SesionPestana(HttpSession real, String idPestana) {
        this.real = real;
        this.prefijo = PREFIJO + (idPestana == null ? "-" : idPestana) + ".";
    }

    /** ¿Alguna pestaña de este navegador inició sesión? (para las fotos de perfil, que no llevan pestaña). */
    static boolean hayUsuarioEnAlgunaPestana(HttpSession real) {
        if (real == null) return false;
        try {
            for (String n : Collections.list(real.getAttributeNames())) {
                if (n.startsWith(PREFIJO) && n.endsWith(".usuarioLogueado")) return true;
            }
        } catch (IllegalStateException e) {
            return false;   // sesión vencida
        }
        return false;
    }

    // ---------- atributos: cada pestaña ve solo los suyos ----------
    @Override public Object getAttribute(String nombre) { return real.getAttribute(prefijo + nombre); }
    @Override public void setAttribute(String nombre, Object valor) { real.setAttribute(prefijo + nombre, valor); }
    @Override public void removeAttribute(String nombre) { real.removeAttribute(prefijo + nombre); }

    @Override
    public Enumeration<String> getAttributeNames() {
        List<String> nombres = new ArrayList<>();
        for (String n : Collections.list(real.getAttributeNames())) {
            if (n.startsWith(prefijo)) nombres.add(n.substring(prefijo.length()));
        }
        return Collections.enumeration(nombres);
    }

    /** "Cerrar sesión" cierra SOLO esta pestaña; las demás cuentas siguen abiertas. */
    @Override
    public void invalidate() {
        for (String n : Collections.list(real.getAttributeNames())) {
            if (n.startsWith(prefijo)) real.removeAttribute(n);
        }
    }

    // ---------- lo demás es de la sesión real ----------
    @Override public long getCreationTime() { return real.getCreationTime(); }
    @Override public String getId() { return real.getId(); }
    @Override public long getLastAccessedTime() { return real.getLastAccessedTime(); }
    @Override public ServletContext getServletContext() { return real.getServletContext(); }
    @Override public void setMaxInactiveInterval(int segundos) { real.setMaxInactiveInterval(segundos); }
    @Override public int getMaxInactiveInterval() { return real.getMaxInactiveInterval(); }
    @Override public boolean isNew() { return real.isNew(); }
}
