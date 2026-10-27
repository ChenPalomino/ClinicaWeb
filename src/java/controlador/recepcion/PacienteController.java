package controlador.recepcion;

import controlador.ControladorUtil;
import controlador.MensajeFlash;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import modelo.dto.FiltroBusquedaDTO;
import modelo.dto.PacienteDTO;
import modelo.entidades.Usuario;
import modelo.servicios.AuditoriaService;
import modelo.servicios.PacienteService;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

/**
 * Controlador (MVC) del CRUD de PACIENTES — paquete controlador.recepcion.
 * Ruta /recepcion/*: la usan Recepción (admisión) y el Administrador
 * (lo controla AutorizacionFilter).
 *
 *   GET  /recepcion/pacientes                    -> listado (búsqueda por DNI, apellidos o historia)
 *   GET  /recepcion/pacientes?accion=nuevo       -> formulario vacío
 *   GET  /recepcion/pacientes?accion=editar&id=  -> formulario con datos
 *   POST accion=guardar | desactivar | reactivar  (Post/Redirect/Get)
 */
@WebServlet(name = "PacienteController", urlPatterns = {"/recepcion/pacientes"})
public class PacienteController extends HttpServlet {

    private static final String RUTA = "/recepcion/pacientes";
    private static final String VISTA_LISTA = "/WEB-INF/vistas/recepcion/pacientes/listar.jsp";
    private static final String VISTA_FORM = "/WEB-INF/vistas/recepcion/pacientes/formulario.jsp";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        PacienteService servicio = new PacienteService();
        String accion = request.getParameter("accion");
        request.setAttribute("menuActivo", "pacientes");
        try {
            if ("nuevo".equals(accion)) {
                PacienteDTO nuevo = new PacienteDTO();
                nuevo.setDni(ControladorUtil.entero(request.getParameter("dni"), 0) > 0 ? request.getParameter("dni") : "");
                mostrarFormulario(request, response, servicio, nuevo, Map.of());
                return;
            }
            if ("editar".equals(accion)) {
                Optional<PacienteDTO> p = servicio.obtener(ControladorUtil.entero(request.getParameter("id"), 0));
                if (p.isEmpty()) {
                    MensajeFlash.error(request.getSession(), "El paciente que buscas no existe.");
                    response.sendRedirect(request.getContextPath() + RUTA);
                    return;
                }
                mostrarFormulario(request, response, servicio, p.get(), Map.of());
                return;
            }
            request.setAttribute("coberturas", servicio.coberturas());
            if (ControladorUtil.busquedaSolicitada(request)) {
                FiltroBusquedaDTO filtro = new FiltroBusquedaDTO(request.getParameter("q"), request.getParameter("estado"),
                        ControladorUtil.enteroONulo(request.getParameter("cobertura")), null,
                        ControladorUtil.entero(request.getParameter("pagina"), 1))
                        .conRango(ControladorUtil.rango(request, "cobertura", "estado"));
                request.setAttribute("filtro", filtro);
                request.setAttribute("pagina", servicio.listar(filtro));
            }
            request.setAttribute("puedeDesactivar",
                    PacienteService.puedeDesactivar((Usuario) request.getSession().getAttribute("usuarioLogueado")));
        } catch (RuntimeException e) {
            System.err.println("PacienteController: " + e.getMessage());
            request.setAttribute("errorDatos", true);
        }
        request.setAttribute("tituloPagina", "Pacientes");
        request.getRequestDispatcher(VISTA_LISTA).forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        HttpSession session = request.getSession();
        Usuario sesion = (Usuario) session.getAttribute("usuarioLogueado");
        PacienteService servicio = new PacienteService();
        String accion = request.getParameter("accion");
        int id = ControladorUtil.entero(request.getParameter("id"), 0);
        String volver = request.getContextPath() + RUTA + ControladorUtil.regresoListado(request);
        try {
            switch (accion == null ? "" : accion) {
                case "guardar": {
                    PacienteDTO p = leerFormulario(request);
                    boolean nuevo = p.isNuevo();
                    Map<String, String> errores = servicio.guardar(p, sesion);
                    if (!errores.isEmpty()) {
                        request.setAttribute("menuActivo", "pacientes");
                        mostrarFormulario(request, response, servicio, p, errores);
                        return;
                    }
                    MensajeFlash.exito(session, nuevo
                            ? "Paciente " + p.getNombreCompleto() + " registrado correctamente."
                            : "Cambios de " + p.getNombreCompleto() + " guardados.");
                    response.sendRedirect(request.getContextPath() + RUTA + "?q=" + p.getDni());
                    return;
                }
                case "desactivar":
                    ControladorUtil.resultado(session, servicio.desactivar(id, sesion),
                            "Paciente desactivado. Su historia se conserva y puedes reactivarlo cuando quieras.");
                    break;
                case "reactivar":
                    ControladorUtil.resultado(session, servicio.reactivar(id, sesion), "Paciente reactivado.");
                    break;
                default:
                    MensajeFlash.error(session, "Acción no reconocida.");
            }
        } catch (RuntimeException e) {
            System.err.println("PacienteController: " + e.getMessage());
            MensajeFlash.error(session, "No se pudo completar la operación. Inténtalo de nuevo en unos minutos.");
        }
        response.sendRedirect(volver);
    }

    private void mostrarFormulario(HttpServletRequest request, HttpServletResponse response, PacienteService servicio,
                                   PacienteDTO p, Map<String, String> errores) throws ServletException, IOException {
        request.setAttribute("pacienteForm", p);
        request.setAttribute("errores", errores);
        request.setAttribute("coberturas", servicio.coberturas());
        request.setAttribute("sexos", servicio.sexos());
        Usuario sesion = (Usuario) request.getSession().getAttribute("usuarioLogueado");
        request.setAttribute("puedeCorregirIdentidad", PacienteService.puedeCorregirIdentidad(sesion));
        if (!p.isNuevo()) {
            request.setAttribute("historial", new AuditoriaService().historial("PACIENTE", p.getIdPaciente()));
        }
        request.setAttribute("tituloPagina", p.isNuevo() ? "Nuevo paciente" : "Editar paciente");
        request.getRequestDispatcher(VISTA_FORM).forward(request, response);
    }

    private static PacienteDTO leerFormulario(HttpServletRequest request) {
        PacienteDTO p = new PacienteDTO();
        p.setIdPaciente(ControladorUtil.entero(request.getParameter("id"), 0));
        p.setIdDatosPersonales(ControladorUtil.entero(request.getParameter("idDatosPersonales"), 0));
        p.setNumeroHistoria(request.getParameter("numeroHistoria"));
        p.setNombres(request.getParameter("nombres"));
        p.setApellidos(request.getParameter("apellidos"));
        p.setDni(request.getParameter("dni"));
        p.setIdSexo(ControladorUtil.enteroONulo(request.getParameter("idSexo")));
        p.setFechaNacimiento(ControladorUtil.fecha(request.getParameter("fechaNacimiento")));
        p.setCorreo(request.getParameter("correo"));
        p.setCelular(request.getParameter("celular"));
        p.setIdCobertura(ControladorUtil.entero(request.getParameter("idCobertura"), 0));
        p.setNumeroAfiliacion(request.getParameter("numeroAfiliacion"));
        p.setObservaciones(request.getParameter("observaciones"));
        p.setEstado(request.getParameter("estado"));
        return p;
    }
}
