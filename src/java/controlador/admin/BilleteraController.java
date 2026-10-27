package controlador.admin;

import controlador.ControladorUtil;
import controlador.MensajeFlash;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import modelo.dto.BilleteraConfigDTO;
import modelo.entidades.Usuario;
import modelo.servicios.BilleteraService;

import java.io.IOException;
import java.util.Map;

@WebServlet(name = "BilleteraController", urlPatterns = {"/admin/billeteras"})
public class BilleteraController extends HttpServlet {

    private static final String VISTA = "/WEB-INF/vistas/admin/billeteras.jsp";
    private static final String RUTA = "/admin/billeteras";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            mostrar(request, response, null, Map.of());
        } catch (RuntimeException e) {
            System.err.println("BilleteraController: " + e.getMessage());
            MensajeFlash.error(request.getSession(), "No pudimos cargar la configuración de billeteras.");
            response.sendRedirect(request.getContextPath() + "/panel");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        HttpSession session = request.getSession();
        Usuario admin = (Usuario) session.getAttribute("usuarioLogueado");
        BilleteraService servicio = new BilleteraService();
        String accion = request.getParameter("accion") == null ? "" : request.getParameter("accion");
        try {
            switch (accion) {
                case "datos": {
                    BilleteraConfigDTO d = new BilleteraConfigDTO();
                    d.setCelular(request.getParameter("celular"));
                    d.setTitular(request.getParameter("titular"));
                    Map<String, String> errores = servicio.guardarDatos(d, admin);
                    if (!errores.isEmpty()) {
                        mostrar(request, response, d, errores);
                        return;
                    }
                    MensajeFlash.exito(session, "Datos de cobro guardados. Ya se muestran en caja y en el portal del paciente.");
                    break;
                }
                case "qr":
                    ControladorUtil.resultado(session, servicio.guardarQr(request.getParameter("qr"), admin), "QR actualizado.");
                    break;
                case "quitarQr":
                    servicio.quitarQr(admin);
                    MensajeFlash.exito(session, "QR quitado. Los pacientes verán solo el celular.");
                    break;
                case "agregar":
                    ControladorUtil.resultado(session, servicio.agregar(request.getParameter("nombre"), admin), "Billetera agregada.");
                    break;
                case "estado":
                    ControladorUtil.resultado(session, servicio.cambiarEstado(ControladorUtil.entero(request.getParameter("id"), 0),
                            "1".equals(request.getParameter("activar")), admin), "Listo, se actualizó la billetera.");
                    break;
                default:
                    MensajeFlash.aviso(session, "Acción no reconocida.");
            }
        } catch (RuntimeException e) {
            System.err.println("BilleteraController: " + e.getMessage());
            MensajeFlash.error(session, "No se pudo guardar. Inténtalo de nuevo en unos minutos.");
        }
        response.sendRedirect(request.getContextPath() + RUTA);
    }

    private void mostrar(HttpServletRequest request, HttpServletResponse response, BilleteraConfigDTO datos,
                         Map<String, String> errores) throws ServletException, IOException {
        BilleteraService servicio = new BilleteraService();
        BilleteraConfigDTO actual = servicio.configuracion();
        if (datos != null) {
            datos.setTieneQr(actual.isTieneQr());
            datos.setVersionQr(actual.getVersionQr());
        }
        request.setAttribute("config", datos == null ? actual : datos);
        request.setAttribute("billeteras", servicio.billeteras());
        request.setAttribute("errores", errores);
        request.setAttribute("qrMaximoKb", BilleteraService.QR_MAXIMO_BYTES / 1000);
        request.setAttribute("menuActivo", "billeteras");
        request.setAttribute("tituloPagina", "Billeteras y QR");
        request.getRequestDispatcher(VISTA).forward(request, response);
    }
}
