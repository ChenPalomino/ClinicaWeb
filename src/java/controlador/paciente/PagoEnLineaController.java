package controlador.paciente;

import controlador.ControladorUtil;
import controlador.MensajeFlash;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import modelo.cobranza.pago.SolicitudPago;
import modelo.entidades.Usuario;
import modelo.servicios.BilleteraService;
import modelo.servicios.CobranzaService;
import modelo.servicios.ReportePagoService;

import java.io.IOException;
import java.util.Map;

@WebServlet(name = "PagoEnLineaController", urlPatterns = {"/paciente/pagar"})
public class PagoEnLineaController extends HttpServlet {

    private static final String VISTA = "/WEB-INF/vistas/paciente/pagar.jsp";
    private static final String CITAS = "/paciente/citas";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        mostrar(request, response, ControladorUtil.entero(request.getParameter("cita"), 0), new SolicitudPago(), Map.of());
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        HttpSession session = request.getSession();
        int idCita = ControladorUtil.entero(request.getParameter("cita"), 0);
        SolicitudPago datos = new SolicitudPago();
        int idBilletera = ControladorUtil.entero(request.getParameter("idBilletera"), 0);
        datos.setIdBilletera(idBilletera > 0 ? idBilletera : null);
        datos.setNumeroOperacion(request.getParameter("numeroOperacion"));
        datos.setCelular(request.getParameter("celular"));
        datos.setNombrePagador(request.getParameter("nombrePagador"));
        try {
            Map<String, String> errores = new ReportePagoService().informar(idCita, datos, (Usuario) session.getAttribute("usuarioLogueado"));
            if (!errores.isEmpty()) {
                mostrar(request, response, idCita, datos, errores);
                return;
            }
            MensajeFlash.exito(session, "¡Gracias! Recibimos tu aviso de pago. Caja lo revisa y te enviaremos la boleta a tu correo.");
        } catch (RuntimeException e) {
            System.err.println("PagoEnLineaController: " + e.getMessage());
            MensajeFlash.error(session, "No pudimos registrar tu aviso. Si ya pagaste, no vuelvas a pagar: inténtalo en unos minutos o avisa en recepción.");
        }
        response.sendRedirect(request.getContextPath() + CITAS);
    }

    private void mostrar(HttpServletRequest request, HttpServletResponse response, int idCita, SolicitudPago datos,
                         Map<String, String> errores) throws ServletException, IOException {
        try {
            ReportePagoService.Preparacion p = new ReportePagoService().preparar(idCita, (Usuario) request.getSession().getAttribute("usuarioLogueado"));
            if (!p.isExito()) {
                MensajeFlash.aviso(request.getSession(), p.error());
                response.sendRedirect(request.getContextPath() + CITAS);
                return;
            }
            request.setAttribute("preparacion", p);
            request.setAttribute("billeteras", new CobranzaService().billeteras());
            request.setAttribute("configBilletera", new BilleteraService().configuracion());
            request.setAttribute("solicitud", datos);
            request.setAttribute("errores", errores);
            request.setAttribute("menuActivo", "citas");
            request.setAttribute("tituloPagina", "Pagar mi cita");
            request.getRequestDispatcher(VISTA).forward(request, response);
        } catch (RuntimeException e) {
            System.err.println("PagoEnLineaController: " + e.getMessage());
            MensajeFlash.error(request.getSession(), "No pudimos preparar el pago. Inténtalo en unos minutos.");
            response.sendRedirect(request.getContextPath() + CITAS);
        }
    }
}
