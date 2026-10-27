package controlador.admin;

import controlador.ControladorUtil;
import controlador.MensajeFlash;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import modelo.dto.EspecialidadDTO;
import modelo.entidades.Usuario;
import modelo.servicios.AuditoriaService;
import modelo.servicios.EspecialidadService;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

@WebServlet(name = "EspecialidadController", urlPatterns = {"/admin/especialidades"})
public class EspecialidadController extends HttpServlet {

    private static final String RUTA = "/admin/especialidades";
    private static final String VISTA_LISTA = "/WEB-INF/vistas/admin/especialidades/listar.jsp";
    private static final String VISTA_FORM = "/WEB-INF/vistas/admin/especialidades/formulario.jsp";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setAttribute("menuActivo", "especialidades");
        EspecialidadService servicio = new EspecialidadService();
        String accion = request.getParameter("accion");
        try {
            if ("nuevo".equals(accion)) {
                mostrarFormulario(request, response, new EspecialidadDTO(), "", Map.of());
                return;
            }
            if ("editar".equals(accion)) {
                Optional<EspecialidadDTO> e = servicio.obtener(ControladorUtil.entero(request.getParameter("id"), 0));
                if (e.isEmpty()) {
                    MensajeFlash.error(request.getSession(), "La especialidad que buscas no existe.");
                    response.sendRedirect(request.getContextPath() + RUTA);
                    return;
                }
                String precio = e.get().getPrecioConsulta() == null ? "" : e.get().getPrecioConsulta().toPlainString();
                mostrarFormulario(request, response, e.get(), precio, Map.of());
                return;
            }
            request.setAttribute("especialidades", servicio.listar(request.getParameter("q"), request.getParameter("estado")));
        } catch (RuntimeException e) {
            System.err.println("EspecialidadController: " + e.getMessage());
            request.setAttribute("errorDatos", true);
        }
        request.setAttribute("tituloPagina", "Especialidades");
        request.getRequestDispatcher(VISTA_LISTA).forward(request, response);
    }

    private void guardar(HttpServletRequest request, HttpServletResponse response, EspecialidadService servicio, Usuario admin, int id)
            throws ServletException, IOException {
        EspecialidadDTO form = new EspecialidadDTO();
        form.setIdEspecialidad(id);
        form.setNombre(request.getParameter("nombre"));
        form.setDescripcion(request.getParameter("descripcion"));
        form.setEstado(request.getParameter("estado"));
        String precio = request.getParameter("precio");
        EspecialidadService.Guardado g = servicio.guardar(form, precio, admin);
        if (!g.isExito()) {
            request.setAttribute("menuActivo", "especialidades");
            servicio.obtener(id).ifPresent(actual -> {
                form.setMedicosActivos(actual.getMedicosActivos());
                form.setMedicosTotal(actual.getMedicosTotal());
                form.setIdServicioConsulta(actual.getIdServicioConsulta());
                form.setCodigoConsulta(actual.getCodigoConsulta());
            });
            mostrarFormulario(request, response, form, precio == null ? "" : precio, g.errores());
            return;
        }
        if (g.aviso() != null) {
            MensajeFlash.aviso(request.getSession(), g.aviso());
        } else {
            MensajeFlash.exito(request.getSession(), "Especialidad «" + form.getNombre() + "» " + (id == 0 ? "creada." : "guardada.")
                    + (id == 0 ? " Ya puedes asignarla a los médicos en «Médicos y horarios»." : ""));
        }
        response.sendRedirect(request.getContextPath() + RUTA);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        HttpSession session = request.getSession();
        Usuario admin = (Usuario) session.getAttribute("usuarioLogueado");
        EspecialidadService servicio = new EspecialidadService();
        String accion = request.getParameter("accion");
        int id = ControladorUtil.entero(request.getParameter("id"), 0);
        String volver = request.getContextPath() + RUTA + ControladorUtil.regresoListado(request);
        try {
            switch (accion == null ? "" : accion) {
                case "guardar":
                    guardar(request, response, servicio, admin, id);
                    return;
                case "desactivar":
                    ControladorUtil.resultado(session, servicio.desactivar(id, admin),
                            "Especialidad desactivada: ya no aparece al reservar citas ni al configurar médicos.");
                    break;
                case "reactivar":
                    ControladorUtil.resultado(session, servicio.reactivar(id, admin), "Especialidad reactivada.");
                    break;
                default:
                    MensajeFlash.error(session, "Acción no reconocida.");
            }
        } catch (RuntimeException e) {
            System.err.println("EspecialidadController: " + e.getMessage());
            MensajeFlash.error(session, "No se pudo completar la operación. Inténtalo de nuevo en unos minutos.");
        }
        response.sendRedirect(volver);
    }

    private void mostrarFormulario(HttpServletRequest request, HttpServletResponse response, EspecialidadDTO e,
                                   String precio, Map<String, String> errores) throws ServletException, IOException {
        request.setAttribute("especialidadForm", e);
        request.setAttribute("precioForm", precio);
        request.setAttribute("errores", errores);
        if (!e.isNuevo()) {
            request.setAttribute("historial", new AuditoriaService().historial("ESPECIALIDAD", e.getIdEspecialidad()));
        }
        request.setAttribute("tituloPagina", e.isNuevo() ? "Nueva especialidad" : "Editar especialidad");
        request.getRequestDispatcher(VISTA_FORM).forward(request, response);
    }
}
