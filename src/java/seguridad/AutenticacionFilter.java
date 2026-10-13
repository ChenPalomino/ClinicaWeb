package seguridad;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.Set;

/**
 * PATRÓN: Intercepting Filter  (control de AUTENTICACIÓN)
 * Propósito: revisar TODAS las peticiones a /jsp/* y /panel ANTES de que lleguen a la
 * página. Si la página es privada y no hay sesión, redirige al login.
 * Así ninguna página privada necesita repetir el "if (user == null)".
 *
 * También evita que, después de cerrar sesión, el botón "Atrás" del
 * navegador muestre el menú guardado en caché.
 */
// Rutas y orden de ejecución: WEB-INF/web.xml
public class AutenticacionFilter implements Filter {

    private static final Set<String> PAGINAS_PUBLICAS = Set.of(
            "/jsp/login.jsp",
            "/jsp/registro.jsp",
            "/jsp/bloqueado.jsp",
            "/jsp/recuperar.jsp",
            "/jsp/verificar-codigo.jsp",
            "/jsp/nueva-contrasena.jsp"
    );

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        String ruta = request.getRequestURI().substring(request.getContextPath().length());
        HttpSession session = request.getSession(false);
        boolean logueado = session != null && session.getAttribute("usuarioLogueado") != null;

        if (PAGINAS_PUBLICAS.contains(ruta)) {
            // Si ya inició sesión y vuelve al login, lo mandamos al menú
            if (logueado && ruta.equals("/jsp/login.jsp")) {
                response.sendRedirect(request.getContextPath() + "/panel");
                return;
            }
            chain.doFilter(req, res);
            return;
        }

        // Fotos de perfil: basta con que alguna pestaña de este navegador tenga sesión
        // (las etiquetas <img> no llevan el identificador de la pestaña)
        if (ruta.equals("/foto")) {
            if (logueado || SesionPorPestanaFilter.hayUsuarioEnAlgunaPestana(request)) {
                chain.doFilter(req, res);
            } else {
                response.sendError(HttpServletResponse.SC_NOT_FOUND);
            }
            return;
        }

        if (!logueado) {
            // Pestaña nueva (sin identificador): login limpio. Pestaña que sí tenía sesión: "tu sesión terminó"
            boolean pestanaNueva = SesionPorPestanaFilter.idPestana(request) == null;
            response.sendRedirect(request.getContextPath() + "/jsp/login.jsp" + (pestanaNueva ? "" : "?error=sesion"));
            return;
        }

        // ¿La cuenta sigue activa? (bloqueada, desactivada o con otro rol -> se cierra la sesión de la pestaña)
        if (!SesionVigente.revisar(request, response)) {
            return;
        }

        // Página privada: no guardar en caché
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);
        chain.doFilter(req, res);
    }
}
