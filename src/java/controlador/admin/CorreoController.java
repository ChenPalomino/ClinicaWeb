package controlador.admin;

import controlador.MensajeFlash;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import modelo.entidades.Usuario;
import modelo.servicios.CorreoDiagnosticoService;
import notificacion.BrevoCorreoAdapter;
import notificacion.ResultadoEnvio;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Controlador (MVC) del diagnóstico de correo — solo Administrador.
 *   GET  /admin/correo[?email=]  -> configuración, cuenta Brevo y últimos eventos de entrega
 *   POST accion=probar           -> envía un correo de prueba y muestra la respuesta de Brevo
 */
@WebServlet(name = "CorreoController", urlPatterns = {"/admin/correo"})
public class CorreoController extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        CorreoDiagnosticoService servicio = new CorreoDiagnosticoService();
        String email = request.getParameter("email");
        request.setAttribute("modoReal", servicio.isModoReal());
        request.setAttribute("configuracion", servicio.configuracion());
        request.setAttribute("cuenta", servicio.cuentaBrevo());
        request.setAttribute("emailFiltro", email == null ? "" : email.trim());
        try {
            request.setAttribute("eventos", servicio.ultimosEventos(email));
        } catch (BrevoCorreoAdapter.ErrorDiagnostico e) {
            request.setAttribute("errorEventos", e.getMessage());
            request.setAttribute("eventos", List.of());
        }
        try {
            request.setAttribute("correosPrueba", servicio.usuariosConCorreoDePrueba());
        } catch (RuntimeException e) {
            request.setAttribute("correosPrueba", List.of());
        }
        request.setAttribute("menuActivo", "correo");
        request.setAttribute("tituloPagina", "Correo y notificaciones");
        request.getRequestDispatcher("/WEB-INF/vistas/admin/correo.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        request.setCharacterEncoding("UTF-8");
        Usuario admin = (Usuario) request.getSession().getAttribute("usuarioLogueado");
        String destino = request.getParameter("destinatario");
        ResultadoEnvio r = new CorreoDiagnosticoService().enviarPrueba(destino, admin);
        if (r.isAceptado()) {
            MensajeFlash.exito(request.getSession(), r.getMensaje()
                    + " Revisa la bandeja (y spam). Abajo verás si Brevo lo entregó, bloqueó o rebotó (puede tardar 1 minuto).");
        } else {
            MensajeFlash.error(request.getSession(), "No se envió. " + r.getMensaje());
        }
        String filtro = destino == null ? "" : "?email=" + URLEncoder.encode(destino.trim(), StandardCharsets.UTF_8);
        response.sendRedirect(request.getContextPath() + "/admin/correo" + filtro);
    }
}
