package controlador.caja;

import controlador.ControladorUtil;
import controlador.MensajeFlash;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import modelo.entidades.Usuario;
import modelo.servicios.CierreCajaService;

import java.io.IOException;

/**
 * RF-17: cierre de caja diario.
 *   GET  /caja/cierre  -> lo cobrado hoy por el usuario + historial de cierres
 *   POST accion=cerrar (efectivoContado, observacion)
 */
@WebServlet(name = "CierreCajaController", urlPatterns = {"/caja/cierre"})
public class CierreCajaController extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Usuario usuario = (Usuario) request.getSession().getAttribute("usuarioLogueado");
        CierreCajaService servicio = new CierreCajaService();
        try {
            request.setAttribute("resumen", servicio.resumenDeHoy(usuario));
            request.setAttribute("historial", servicio.historial(usuario));
        } catch (RuntimeException e) {
            System.err.println("CierreCajaController: " + e.getMessage());
            request.setAttribute("errorDatos", true);
        }
        request.setAttribute("menuActivo", "cierre");
        request.setAttribute("tituloPagina", "Cierre de caja");
        request.getRequestDispatcher("/WEB-INF/vistas/caja/cierre.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        HttpSession session = request.getSession();
        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
        if ("cerrar".equals(request.getParameter("accion"))) {
            try {
                ControladorUtil.resultado(session, new CierreCajaService().cerrar(usuario,
                        ControladorUtil.decimal(request.getParameter("efectivoContado")), request.getParameter("observacion")),
                        "Caja cerrada. El resumen quedó registrado y ya no puedes cobrar con tu usuario hasta mañana.");
            } catch (RuntimeException e) {
                System.err.println("CierreCajaController: " + e.getMessage());
                MensajeFlash.error(session, "No se pudo cerrar la caja. Inténtalo de nuevo en unos minutos.");
            }
        }
        response.sendRedirect(request.getContextPath() + "/caja/cierre");
    }
}
