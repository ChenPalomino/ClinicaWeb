package seguridad;

import controlador.MensajeFlash;
import evento.AccesoCambiadoObserver;
import evento.EventoSistema;
import evento.EventoSubject;
import evento.TipoEvento;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import modelo.dto.UsuarioDTO;
import modelo.entidades.Usuario;
import modelo.repositorio.UsuarioRepository;
import modelo.repositorio.UsuarioRepositoryImpl;

import java.io.IOException;
import java.util.Optional;

/**
 * Revisa en cada petición que la sesión siga siendo válida. Lo usan
 * AutenticacionFilter y AutorizacionFilter (patrón Intercepting Filter).
 *
 * 1. CUENTA: si el administrador bloqueó, desactivó o cambió el rol del usuario
 *    mientras estaba conectado, su sesión se cierra en la siguiente acción.
 * 2. PÁGINA ANTIGUA: cada pestaña tiene su propia cuenta (SesionPorPestanaFilter),
 *    pero si en la MISMA pestaña se cerró sesión y se entró con otra cuenta, el
 *    botón "Atrás" podría mostrar un formulario de la cuenta anterior. Cada
 *    formulario envía "_cuenta" (la cuenta que la página muestra); si no
 *    coincide, la acción NO se ejecuta y se avisa.
 */
public final class SesionVigente {

    public static final String PARAMETRO_CUENTA = "_cuenta";
    private static final String ULTIMA_REVISION = "sesion.ultimaRevision";
    private static final String VERSION_ACCESOS = "sesion.versionAccesos";
    // Las consultas (GET) se revisan cada 30 s (menos viajes a la BD = sistema más rápido),
    // o de inmediato si alguien cambió el acceso de una cuenta (AccesoCambiadoObserver).
    // Las acciones (POST) se revisan SIEMPRE, así un usuario bloqueado no puede cambiar nada.
    private static final long INTERVALO_MS = 30_000;

    private SesionVigente() {
    }

    /** @return true si la petición puede continuar; false si ya se respondió (redirección). */
    public static boolean revisar(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession(false);
        Usuario usuario = session == null ? null : (Usuario) session.getAttribute("usuarioLogueado");
        if (usuario == null) {
            return true;    // el filtro que llama decide qué hacer sin sesión
        }
        evento.ContextoPeticion.asegurar(request);
        boolean esAccion = "POST".equalsIgnoreCase(request.getMethod());
        if (esAccion && !pestanaCoincide(request, response, session, usuario)) {
            return false;
        }
        return cuentaVigente(request, response, session, usuario, esAccion);
    }

    private static boolean pestanaCoincide(HttpServletRequest request, HttpServletResponse response,
                                           HttpSession session, Usuario usuario) throws IOException {
        request.setCharacterEncoding("UTF-8");      // antes de leer el primer parámetro
        String cuentaPestana = request.getParameter(PARAMETRO_CUENTA);
        if (cuentaPestana == null || cuentaPestana.equals(String.valueOf(usuario.getIdUsuario()))) {
            return true;
        }
        if (esLlamadaEnSegundoPlano(request)) {
            response.sendError(HttpServletResponse.SC_CONFLICT);   // fetch(): el navegador recarga la pestaña
            return false;
        }
        MensajeFlash.aviso(session, "No se hizo ningún cambio: esa página era de otra cuenta. En esta pestaña "
                + "ahora está abierta la sesión de @" + usuario.getNombreUsuario() + ".");
        response.sendRedirect(request.getContextPath() + "/panel");
        return false;
    }

    private static boolean cuentaVigente(HttpServletRequest request, HttpServletResponse response, HttpSession session,
                                         Usuario usuario, boolean esAccion) throws IOException {
        Long ultima = (Long) session.getAttribute(ULTIMA_REVISION);
        Long versionVista = (Long) session.getAttribute(VERSION_ACCESOS);
        long version = AccesoCambiadoObserver.version();
        long ahora = System.currentTimeMillis();
        boolean sinCambios = versionVista != null && versionVista == version;   // nadie bloqueó/desactivó cuentas
        if (!esAccion && sinCambios && ultima != null && ahora - ultima < INTERVALO_MS) {
            return true;
        }
        String motivo;
        try {
            motivo = motivoDeCierre(new UsuarioRepositoryImpl(), usuario);
        } catch (RuntimeException e) {
            System.err.println("SesionVigente: no se pudo revisar la cuenta -> " + e.getMessage());
            return true;    // si la BD falla momentáneamente no se expulsa al usuario
        }
        if (motivo == null) {
            session.setAttribute(ULTIMA_REVISION, ahora);
            session.setAttribute(VERSION_ACCESOS, version);
            return true;
        }
        EventoSubject.getInstancia().publicar(new EventoSistema(TipoEvento.SESION_EXPULSADA, usuario.getIdUsuario(),
                usuario.getNombreUsuario(), usuario.getIdUsuario(), "Motivo: " + motivo));
        session.invalidate();
        if (esLlamadaEnSegundoPlano(request)) {
            response.setHeader("X-Motivo", motivo);       // la página lo usa para mostrar el mensaje correcto
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
        } else {
            response.sendRedirect(request.getContextPath() + "/jsp/login.jsp?error=" + motivo);
        }
        return false;
    }

    /** null = la sesión sigue válida; si no: "desactivada", "bloqueada" o "rol". Visible para pruebas. */
    static String motivoDeCierre(UsuarioRepository repositorio, Usuario usuario) {
        Optional<UsuarioDTO> actual = repositorio.buscarEstadoAcceso(usuario.getIdUsuario());
        if (actual.isEmpty() || !"Activo".equals(actual.get().getEstado())) {
            return "desactivada";
        }
        if (actual.get().isBloqueado()) {
            return "bloqueada";
        }
        if (actual.get().getIdRol() != usuario.getRolId()) {
            return "rol";
        }
        return null;
    }

    private static boolean esLlamadaEnSegundoPlano(HttpServletRequest request) {
        return "fetch".equals(request.getHeader("X-Requested-With"));
    }
}
