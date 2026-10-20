package controlador.admin;

import controlador.ControladorUtil;
import controlador.MensajeFlash;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import modelo.dto.ParametroClinicaDTO;
import modelo.entidades.Usuario;
import modelo.servicios.AuditoriaService;
import modelo.servicios.ParametroClinicaService;

import java.io.IOException;
import java.util.Map;

/** RF-22: datos de la clínica (RUC, razón social, dirección) e IGV. Solo el administrador (/admin/*). */
@WebServlet(name = "ParametroClinicaController", urlPatterns = {"/admin/parametros"})
public class ParametroClinicaController extends HttpServlet {

    private static final String VISTA = "/WEB-INF/vistas/admin/parametros.jsp";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            mostrar(request, response, new ParametroClinicaService().obtener(), Map.of());
        } catch (RuntimeException e) {
            System.err.println("ParametroClinicaController: " + e.getMessage());
            MensajeFlash.error(request.getSession(), "No pudimos cargar los datos de la clínica.");
            response.sendRedirect(request.getContextPath() + "/panel");
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        ParametroClinicaDTO p = new ParametroClinicaDTO();
        p.setRuc(request.getParameter("ruc"));
        p.setRazonSocial(request.getParameter("razonSocial"));
        p.setNombreComercial(request.getParameter("nombreComercial"));
        p.setDireccion(request.getParameter("direccion"));
        p.setTelefono(request.getParameter("telefono"));
        p.setIgvPorcentaje(ControladorUtil.decimal(request.getParameter("igvPorcentaje")));
        p.setMontoBoletaDni(ControladorUtil.decimal(request.getParameter("montoBoletaDni")));
        p.setHorasCancelacionPaciente(ControladorUtil.entero(request.getParameter("horasCancelacionPaciente"), -1));
        p.setDiasMaximosReserva(ControladorUtil.entero(request.getParameter("diasMaximosReserva"), -1));
        try {
            Map<String, String> errores = new ParametroClinicaService().guardar(p,
                    (Usuario) request.getSession().getAttribute("usuarioLogueado"));
            if (!errores.isEmpty()) {
                mostrar(request, response, p, errores);
                return;
            }
            MensajeFlash.exito(request.getSession(), "Datos de la clínica guardados. Los próximos comprobantes ya los usan.");
        } catch (RuntimeException e) {
            System.err.println("ParametroClinicaController: " + e.getMessage());
            MensajeFlash.error(request.getSession(), "No se pudo guardar. Inténtalo de nuevo en unos minutos.");
        }
        response.sendRedirect(request.getContextPath() + "/admin/parametros");
    }

    private void mostrar(HttpServletRequest request, HttpServletResponse response, ParametroClinicaDTO p,
                         Map<String, String> errores) throws ServletException, IOException {
        request.setAttribute("parametrosForm", p);
        request.setAttribute("errores", errores);
        request.setAttribute("historial", new AuditoriaService().historial("CONFIGURACION", 1));
        request.setAttribute("menuActivo", "parametros");
        request.setAttribute("tituloPagina", "Datos de la clínica");
        request.getRequestDispatcher(VISTA).forward(request, response);
    }
}
