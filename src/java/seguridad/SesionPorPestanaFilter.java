package seguridad;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;
import java.util.regex.Pattern;

/**
 * PATRÓN: Intercepting Filter (+ Decorator de la petición y la respuesta)
 *
 * Permite UNA CUENTA POR PESTAÑA. Cada pestaña tiene un identificador propio
 * (lo crea js/pestana.js y viaja en el parámetro "_t" de enlaces y formularios).
 * Este filtro:
 *   0. Si una página llega SIN identificador, primero se le asigna uno (pestaña nueva = login).
 *   1. Envuelve la petición: getSession() devuelve la SesionPestana de esa pestaña.
 *   2. Envuelve la respuesta: cada sendRedirect conserva el "_t" de la pestaña.
 * Así, entrar como médico en otra pestaña NO cambia la cuenta de la primera.
 *
 * Debe ejecutarse ANTES que los demás filtros (orden definido en WEB-INF/web.xml).
 */
public class SesionPorPestanaFilter implements Filter {

    public static final String PARAMETRO = "_t";
    private static final Pattern ID_VALIDO = Pattern.compile("^[a-z0-9]{6,32}$");

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) req;
        if (request.getCharacterEncoding() == null) {
            request.setCharacterEncoding("UTF-8");      // antes de leer el primer parámetro
        }
        String id = request.getParameter(PARAMETRO);
        String idPestana = id != null && ID_VALIDO.matcher(id).matches() ? id : null;
        HttpServletResponse response = (HttpServletResponse) res;
        if (idPestana == null && esNavegacion(request)) {
            identificarPestana(request, response);
            return;
        }
        chain.doFilter(new PeticionPestana(request, idPestana),
                new RespuestaPestana(response, request.getContextPath(), idPestana));
    }

    /**
     * Una página pedida SIN identificador (pestaña nueva, dirección escrita a mano o
     * enlace de un correo) no muestra nada todavía: se responde una página mínima que
     * asigna el identificador de la pestaña y vuelve a pedir la MISMA dirección.
     * Así nunca se dibuja una página con la cuenta de otra pestaña.
     */
    private static void identificarPestana(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String ctx = request.getContextPath();
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setContentType("text/html;charset=UTF-8");
        response.getWriter().write("<!DOCTYPE html><html lang=\"es\"><head><meta charset=\"UTF-8\">"
                + "<title>Clínica Modelo San Martín</title>"
                + "<script src=\"" + ctx + "/js/pestana.js\"></script></head>"
                + "<body><noscript>Para usar el sistema activa JavaScript en tu navegador.</noscript></body></html>");
    }

    /** ¿Es una página que el usuario abre (no un estilo, script, imagen o llamada en segundo plano)? */
    private static boolean esNavegacion(HttpServletRequest request) {
        if (!"GET".equals(request.getMethod()) || "fetch".equals(request.getHeader("X-Requested-With"))) {
            return false;
        }
        String ruta = request.getRequestURI().substring(request.getContextPath().length());
        return !(ruta.startsWith("/css/") || ruta.startsWith("/js/") || ruta.startsWith("/img/")
                || ruta.equals("/foto") || ruta.equals("/qr-pago") || ruta.equals("/favicon.ico") || ESTATICO.matcher(ruta).find());
    }

    private static final Pattern ESTATICO = Pattern.compile("\\.(css|js|png|jpe?g|gif|svg|ico|webp|woff2?|ttf|map)$");

    /** Identificador de la pestaña de esta petición, o null si llegó sin él (pestaña nueva o enlace externo). */
    public static String idPestana(HttpServletRequest request) {
        String id = request.getParameter(PARAMETRO);
        return id != null && ID_VALIDO.matcher(id).matches() ? id : null;
    }

    /** ¿Hay alguna cuenta abierta en cualquier pestaña de este navegador? */
    public static boolean hayUsuarioEnAlgunaPestana(HttpServletRequest request) {
        HttpServletRequest original = request;
        while (original instanceof HttpServletRequestWrapper) {
            original = (HttpServletRequest) ((HttpServletRequestWrapper) original).getRequest();
        }
        return SesionPestana.hayUsuarioEnAlgunaPestana(original.getSession(false));
    }

    /** La petición entrega la sesión de SU pestaña. */
    private static final class PeticionPestana extends HttpServletRequestWrapper {
        private final String idPestana;

        PeticionPestana(HttpServletRequest request, String idPestana) {
            super(request);
            this.idPestana = idPestana;
        }

        @Override
        public HttpSession getSession(boolean crear) {
            HttpSession real = super.getSession(crear);
            return real == null ? null : new SesionPestana(real, idPestana);
        }

        @Override
        public HttpSession getSession() {
            return getSession(true);
        }
    }

    /** Las redirecciones dentro de la aplicación conservan la pestaña. */
    private static final class RespuestaPestana extends HttpServletResponseWrapper {
        private final String contexto;
        private final String idPestana;

        RespuestaPestana(HttpServletResponse response, String contexto, String idPestana) {
            super(response);
            this.contexto = contexto;
            this.idPestana = idPestana;
        }

        @Override
        public void sendRedirect(String ubicacion) throws IOException {
            super.sendRedirect(conPestana(ubicacion));
        }

        private String conPestana(String u) {
            boolean interna = u != null && (u.startsWith(contexto + "/") || (!u.contains("://") && !u.startsWith("/")));
            if (idPestana == null || !interna || u.contains(PARAMETRO + "=")) {
                return u;
            }
            int ancla = u.indexOf('#');
            String base = ancla < 0 ? u : u.substring(0, ancla);
            String resto = ancla < 0 ? "" : u.substring(ancla);
            return base + (base.contains("?") ? "&" : "?") + PARAMETRO + "=" + idPestana + resto;
        }
    }
}
