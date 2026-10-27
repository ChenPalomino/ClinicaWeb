package controlador;

import configuracion.UrlAplicacion;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import modelo.servicios.RecuperacionService;
import modelo.servicios.RecuperacionService.ResultadoVerificacion;

import java.io.IOException;
import java.util.Map;

/**
 * Controlador (MVC) de la recuperación de contraseña en 3 pasos:
 *   POST accion=solicitar  -> paso 1: pide el código
 *   POST accion=reenviar   -> vuelve a enviar el código
 *   POST accion=verificar  -> paso 2: valida el código
 *   POST accion=cambiar    -> paso 3: guarda la nueva contraseña
 *   GET  accion=reportar   -> enlace "No fui yo" del correo
 *
 * Entre pasos se guarda en la sesión qué cuenta se está recuperando.
 */
@WebServlet(name = "RecuperacionController", urlPatterns = {"/RecuperacionController"})
public class RecuperacionController extends HttpServlet {

    public static final String SESION_IDENTIFICADOR = "recuperacionIdentificador";
    public static final String SESION_ID_VERIFICADO = "recuperacionIdVerificado";
    public static final String SESION_VERIFICADO_EN = "recuperacionVerificadoEn";
    private static final long MINUTOS_PARA_CAMBIAR = 10;

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");
        String accion = request.getParameter("accion");
        String jsp = request.getContextPath() + "/jsp/";
        HttpSession session = request.getSession(true);
        RecuperacionService servicio = new RecuperacionService();

        try {
            switch (accion == null ? "" : accion) {
                case "solicitar":
                case "reenviar":
                    solicitar(request, response, servicio, "reenviar".equals(accion));
                    return;
                case "verificar":
                    verificar(request, response, servicio);
                    return;
                case "cambiar":
                    cambiar(request, response, servicio);
                    return;
                default:
                    response.sendRedirect(jsp + "recuperar.jsp");
            }
        } catch (RuntimeException e) {
            System.err.println("RecuperacionController: " + e.getMessage());
            response.sendRedirect(jsp + "recuperar.jsp?error=sistema");
        }
    }

    private static void solicitar(HttpServletRequest request, HttpServletResponse response, RecuperacionService servicio,
                                  boolean reenviar) throws IOException {
        String jsp = request.getContextPath() + "/jsp/";
        HttpSession session = request.getSession(true);
        String identificador = reenviar ? (String) session.getAttribute(SESION_IDENTIFICADOR)
                                        : limpiar(request.getParameter("identificador"));
        if (identificador == null || identificador.isEmpty() || identificador.length() > 100) {
            response.sendRedirect(jsp + "recuperar.jsp?error=vacio");
            return;
        }
        session.setAttribute(SESION_IDENTIFICADOR, identificador);
        switch (servicio.solicitarCodigo(identificador)) {
            case ESPERAR:
                response.sendRedirect(jsp + "verificar-codigo.jsp?aviso=esperar");
                break;
            case ERROR_ENVIO:
                response.sendRedirect(jsp + "recuperar.jsp?error=envio");
                break;
            default:
                response.sendRedirect(jsp + "verificar-codigo.jsp?aviso=" + (reenviar ? "reenviado" : "enviado"));
        }
    }

    private static void verificar(HttpServletRequest request, HttpServletResponse response, RecuperacionService servicio)
            throws IOException {
        String jsp = request.getContextPath() + "/jsp/";
        HttpSession session = request.getSession(true);
        String identificador = (String) session.getAttribute(SESION_IDENTIFICADOR);
        if (identificador == null) {
            response.sendRedirect(jsp + "recuperar.jsp?error=sesion");
            return;
        }
        ResultadoVerificacion r = servicio.verificarCodigo(identificador, request.getParameter("codigo"));
        switch (r.getEstado()) {
            case VALIDO:
                session.removeAttribute(SESION_IDENTIFICADOR);
                session.setAttribute(SESION_ID_VERIFICADO, r.getIdUsuario());
                session.setAttribute(SESION_VERIFICADO_EN, System.currentTimeMillis());
                response.sendRedirect(jsp + "nueva-contrasena.jsp");
                break;
            case INCORRECTO:
                response.sendRedirect(jsp + "verificar-codigo.jsp?error=incorrecto&restantes=" + r.getIntentosRestantes());
                break;
            case SIN_INTENTOS:
                response.sendRedirect(jsp + "verificar-codigo.jsp?error=intentos");
                break;
            default:
                response.sendRedirect(jsp + "verificar-codigo.jsp?error=expirado");
        }
    }

    private static void cambiar(HttpServletRequest request, HttpServletResponse response, RecuperacionService servicio)
            throws ServletException, IOException {
        String jsp = request.getContextPath() + "/jsp/";
        HttpSession session = request.getSession(true);
        Integer idUsuario = idVerificadoVigente(session);
        if (idUsuario == null) {
            response.sendRedirect(jsp + "recuperar.jsp?error=sesion");
            return;
        }
        Map<String, String> errores = servicio.cambiarContrasena(idUsuario,
                request.getParameter("password"), request.getParameter("confirmarPassword"), UrlAplicacion.base(request));
        if (errores.isEmpty()) {
            session.removeAttribute(SESION_ID_VERIFICADO);
            session.removeAttribute(SESION_VERIFICADO_EN);
            response.sendRedirect(jsp + "login.jsp?recuperacion=ok");
            return;
        }
        request.setAttribute("errores", errores);
        request.getRequestDispatcher("/jsp/nueva-contrasena.jsp").forward(request, response);
    }

    /** Enlace "No fui yo" que llega por correo. */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String jsp = request.getContextPath() + "/jsp/";
        if (!"reportar".equals(request.getParameter("accion"))) {
            response.sendRedirect(jsp + "recuperar.jsp");
            return;
        }
        try {
            int idUsuario = Integer.parseInt(request.getParameter("u"));
            if (new RecuperacionService().reportarCambio(idUsuario, request.getParameter("t"))) {
                response.sendRedirect(jsp + "bloqueado.jsp?motivo=reporte");
                return;
            }
        } catch (RuntimeException e) {
            System.err.println("RecuperacionController (reportar): " + e.getMessage());
        }
        response.sendRedirect(jsp + "login.jsp?error=enlace");
    }

    /** Devuelve el id verificado si el paso 2 se completó hace menos de 10 minutos. */
    public static Integer idVerificadoVigente(HttpSession session) {
        if (session == null) {
            return null;
        }
        Integer id = (Integer) session.getAttribute(SESION_ID_VERIFICADO);
        Long en = (Long) session.getAttribute(SESION_VERIFICADO_EN);
        if (id == null || en == null || System.currentTimeMillis() - en > MINUTOS_PARA_CAMBIAR * 60_000) {
            return null;
        }
        return id;
    }

    private static String limpiar(String valor) {
        return valor == null ? null : valor.trim();
    }
}
