package controlador;

import configuracion.UrlAplicacion;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import modelo.entidades.Usuario;
import modelo.servicios.RecuperacionService;
import seguridad.AutenticacionException;
import seguridad.AutenticacionProxy;
import seguridad.SesionPorPestanaFilter;

import java.io.IOException;

/**
 * Controlador (MVC) del inicio y cierre de sesión.
 * Recibe el formulario, delega TODO al AutenticacionProxy y decide a qué
 * vista redirigir. No contiene SQL ni reglas de negocio.
 */
@WebServlet(name = "AutenticacionController", urlPatterns = {"/AutenticacionController"})
public class AutenticacionController extends HttpServlet {

    private static final int MINUTOS_SESION = 30;

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");
        String nombreUsuario = request.getParameter("nombreUsuario");
        String password = request.getParameter("password");
        // Pestaña desde la que ingresó (paciente / personal) para volver a la misma si hay error
        String tipo = "personal".equals(request.getParameter("tipoAcceso")) ? "personal" : "paciente";
        String login = request.getContextPath() + "/jsp/login.jsp?tipo=" + tipo;
        String jsp = request.getContextPath() + "/jsp/";

        // Sin identificador de pestaña (JavaScript desactivado o página muy antigua) no se inicia sesión:
        // la cuenta quedaría compartida por todas las pestañas nuevas del navegador.
        if (SesionPorPestanaFilter.idPestana(request) == null) {
            response.sendRedirect(login + "&error=pestana");
            return;
        }

        try {
            Usuario usuario = new AutenticacionProxy().intentarAcceso(nombreUsuario, password);

            // Se limpian los datos que tuviera ESTA pestaña (las demás pestañas no se tocan)
            // y se cambia el identificador de la sesión (evita el robo de sesión / session fixation).
            HttpSession session = request.getSession(true);
            session.invalidate();
            request.changeSessionId();
            session.setAttribute("usuarioLogueado", usuario);
            session.setMaxInactiveInterval(MINUTOS_SESION * 60);
            // Nombre, foto y datos para mostrar en el dashboard (en vez del DNI)
            PerfilController.cargarEnSesion(session, usuario.getIdUsuario());

            response.sendRedirect(request.getContextPath() + "/panel");

        } catch (AutenticacionException e) {
            switch (e.getMotivo()) {
                case DATOS_INCOMPLETOS:
                    response.sendRedirect(login + "&error=vacio");
                    break;
                case NO_EXISTE:
                    response.sendRedirect(login + "&error=noexiste");
                    break;
                case CREDENCIALES_INVALIDAS:
                    response.sendRedirect(login + "&error=credenciales&intentos=" + e.getIntentos());
                    break;
                case BLOQUEADO:
                    // "Se cierra la ventana": se elimina la sesión y se muestra la pantalla de bloqueo
                    cerrarSesion(request);
                    if (e.isRecienBloqueado()) {
                        avisarBloqueoPorCorreo(e.getIdUsuario(), request);
                    }
                    response.sendRedirect(jsp + "bloqueado.jsp?min=" + e.getMinutosRestantes());
                    break;
                case BLOQUEADO_SEGURIDAD:
                    cerrarSesion(request);
                    response.sendRedirect(jsp + "bloqueado.jsp?motivo=seguridad");
                    break;
                default:
                    response.sendRedirect(login + "&error=sistema");
            }
        } catch (RuntimeException e) {
            // Falla técnica (por ejemplo, la base de datos no responde)
            System.err.println("AutenticacionController: " + e.getMessage());
            response.sendRedirect(login + "&error=sistema");
        }
    }

    /** GET ?accion=salir -> cierra la sesión de ESTA pestaña (las demás siguen abiertas). Cualquier otro GET va al login. */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String jsp = request.getContextPath() + "/jsp/";
        if ("salir".equals(request.getParameter("accion"))) {
            cerrarSesion(request);
            response.sendRedirect(jsp + "login.jsp?salida=ok");
            return;
        }
        response.sendRedirect(jsp + "login.jsp");
    }

    private static void cerrarSesion(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            Object quien = session.getAttribute("usuarioLogueado");
            if (quien instanceof Usuario) {
                evento.EventoSubject.getInstancia().publicar(evento.TipoEvento.CIERRE_SESION, (Usuario) quien,
                        ((Usuario) quien).getIdUsuario(), null);
            }
            session.invalidate();
        }
    }

    /** El correo de aviso nunca debe impedir mostrar la pantalla de bloqueo. */
    private static void avisarBloqueoPorCorreo(int idUsuario, HttpServletRequest request) {
        try {
            new RecuperacionService().avisarBloqueo(idUsuario, UrlAplicacion.base(request));
        } catch (RuntimeException ex) {
            System.err.println("AutenticacionController: no se pudo avisar el bloqueo -> " + ex.getMessage());
        }
    }
}
