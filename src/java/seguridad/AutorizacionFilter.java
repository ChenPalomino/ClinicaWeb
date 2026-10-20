package seguridad;

import controlador.MensajeFlash;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import modelo.entidades.Rol;
import modelo.entidades.Usuario;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

/**
 * PATRÓN: Intercepting Filter  (control de AUTORIZACIÓN)
 *
 * AutenticacionFilter responde "¿quién eres?" (hay sesión).
 * Este filtro responde "¿tienes permiso?" (control de acceso por rol, RNF-06).
 * Las pantallas están separadas por carpeta según el rol que las usa, y cada
 * carpeta tiene su lista de roles permitidos. Aunque alguien escriba la URL a
 * mano, no podrá entrar a una sección que no le corresponde.
 *
 * Agregar una sección nueva = una línea en PERMISOS + su url-pattern en WEB-INF/web.xml.
 */
// Rutas y orden de ejecución: WEB-INF/web.xml
public class AutorizacionFilter implements Filter {

    /** Sección (primer tramo de la URL) -> roles que pueden entrar. El administrador supervisa al personal. */
    static final Map<String, Set<Integer>> PERMISOS = Map.of(
            "/admin",     Set.of(Rol.ADMINISTRADOR),
            "/recepcion", Set.of(Rol.ADMINISTRADOR, Rol.RECEPCIONISTA),
            "/caja",      Set.of(Rol.ADMINISTRADOR, Rol.RECEPCIONISTA),
            "/medico",    Set.of(Rol.ADMINISTRADOR, Rol.MEDICO),
            "/paciente",  Set.of(Rol.PACIENTE)
    );

    /** @return true si el rol puede entrar a la ruta (ej. "/admin/usuarios"). Las rutas no listadas se niegan. */
    static boolean puedeEntrar(String ruta, int idRol) {
        if (ruta == null) return false;
        int fin = ruta.indexOf('/', 1);
        String seccion = fin < 0 ? ruta : ruta.substring(0, fin);
        Set<Integer> roles = PERMISOS.get(seccion);
        return roles != null && roles.contains(idRol);
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;
        HttpSession session = request.getSession(false);
        Usuario usuario = session == null ? null : (Usuario) session.getAttribute("usuarioLogueado");

        if (usuario == null) {
            response.sendRedirect(request.getContextPath() + "/jsp/login.jsp?error=sesion");
            return;
        }
        if (!SesionVigente.revisar(request, response)) {
            return;     // cuenta bloqueada/desactivada/rol cambiado, o pestaña de otra cuenta
        }
        if (!puedeEntrar(request.getServletPath(), usuario.getRolId())) {
            MensajeFlash.aviso(session, "No tienes permiso para entrar a esa sección. Te llevamos a tu inicio.");
            response.sendRedirect(request.getContextPath() + "/panel");
            return;
        }

        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);
        chain.doFilter(req, res);
    }
}
