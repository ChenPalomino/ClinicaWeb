package controlador.paciente;

import controlador.ControladorUtil;
import controlador.MensajeFlash;
import controlador.citas.AccionesCita;
import controlador.citas.AsistenteReserva;
import controlador.recepcion.CitaController;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import modelo.citas.FechaTexto;
import modelo.citas.ReglasCita;
import modelo.dto.CitaDTO;
import modelo.entidades.Usuario;
import modelo.servicios.CitaService;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Controlador (MVC) de "MIS CITAS" del PACIENTE (portal).
 *
 *   GET  /paciente/citas                          -> próximas citas + historial
 *   GET  /paciente/citas?accion=reservar          -> asistente: especialidad -> día -> hora -> confirmar
 *   GET  /paciente/citas?accion=reprogramar&id=   -> elegir el nuevo horario
 *   GET  /paciente/citas?accion=ver&id=           -> detalle
 *   POST accion=reservar | reprogramar | cancelar
 *
 * El paciente solo ve y cambia SUS citas, y solo hasta 24 h antes (parámetro de la clínica).
 */
@WebServlet(name = "MisCitasController", urlPatterns = {"/paciente/citas"})
public class MisCitasController extends HttpServlet {

    private static final String RUTA = "/paciente/citas";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String accion = request.getParameter("accion");
        request.setAttribute("menuActivo", "reservar".equals(accion) ? "reservar" : "citas");
        request.setAttribute("rutaCitas", request.getContextPath() + RUTA);
        CitaService servicio = new CitaService();
        Usuario usuario = usuario(request);
        try {
            Optional<Integer> idPaciente = servicio.pacienteDeUsuario(usuario.getIdUsuario());
            if (idPaciente.isEmpty()) {
                request.setAttribute("sinFicha", true);
                vista(request, response, "Mis citas", "/WEB-INF/vistas/paciente/citas.jsp");
                return;
            }
            if ("reservar".equals(accion)) {
                request.setAttribute("pacienteSel", servicio.paciente(idPaciente.get()).orElse(null));
                request.setAttribute("modoPaciente", true);
                AsistenteReserva.cargar(request, servicio, null, null);
                vista(request, response, "Reservar cita", "/WEB-INF/vistas/citas/reservar.jsp");
                return;
            }
            if ("ver".equals(accion) || "reprogramar".equals(accion)) {
                Optional<CitaDTO> cita = servicio.obtener(ControladorUtil.entero(request.getParameter("id"), 0));
                if (cita.isEmpty() || !CitaService.puedeVer(cita.get(), usuario)) {
                    MensajeFlash.error(request.getSession(), "No encontramos esa cita.");
                    response.sendRedirect(request.getContextPath() + RUTA);
                    return;
                }
                AccionesCita acciones = new AccionesCita(cita.get(), usuario, servicio.ahora(), servicio.horasCancelacion());
                if ("reprogramar".equals(accion)) {
                    if (!acciones.isReprogramar()) {
                        String motivo = acciones.getBloqueoPaciente();
                        MensajeFlash.aviso(request.getSession(), motivo != null ? motivo : "Esta cita ya no se puede cambiar.");
                        response.sendRedirect(request.getContextPath() + RUTA);
                        return;
                    }
                    request.setAttribute("modoPaciente", true);
                    AsistenteReserva.cargar(request, servicio, cita.get().getIdEspecialidad(), cita.get());
                    vista(request, response, "Cambiar mi cita", "/WEB-INF/vistas/citas/reservar.jsp");
                    return;
                }
                request.setAttribute("cita", cita.get());
                request.setAttribute("acciones", acciones);
                request.setAttribute("hoy", servicio.hoy());
                request.setAttribute("motivosCancelacion", CitaService.MOTIVOS_CANCELACION);
                vista(request, response, "Cita " + cita.get().getCodigo(), "/WEB-INF/vistas/citas/detalle.jsp");
                return;
            }
            List<CitaDTO> proximas = servicio.proximasDelPaciente(idPaciente.get(), 20);
            request.setAttribute("proximas", proximas);
            request.setAttribute("historial", servicio.historialDelPaciente(idPaciente.get(), 30));
            request.setAttribute("hoy", servicio.hoy());
            request.setAttribute("ahora", servicio.ahora());
            request.setAttribute("horasCancelacion", servicio.horasCancelacion());
            request.setAttribute("motivosCancelacion", CitaService.MOTIVOS_CANCELACION);
            request.setAttribute("puedeReservarMas", proximas.stream()
                    .filter(c -> c.getInicio().isAfter(servicio.ahora())).count() < ReglasCita.MAXIMO_PENDIENTES_PORTAL);
        } catch (RuntimeException e) {
            System.err.println("MisCitasController: " + e.getMessage());
            request.setAttribute("errorDatos", true);
        }
        vista(request, response, "Mis citas", "/WEB-INF/vistas/paciente/citas.jsp");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        HttpSession session = request.getSession();
        Usuario usuario = usuario(request);
        CitaService servicio = new CitaService();
        String accion = request.getParameter("accion");
        int id = ControladorUtil.entero(request.getParameter("id"), 0);
        String ctx = request.getContextPath();
        String destino = ctx + RUTA;
        try {
            switch (accion == null ? "" : accion) {
                case "reservar": {
                    int idPaciente = servicio.pacienteDeUsuario(usuario.getIdUsuario()).orElse(0);
                    CitaService.Resultado r = servicio.reservar(idPaciente, ControladorUtil.entero(request.getParameter("medico"), 0),
                            ControladorUtil.fecha(request.getParameter("fecha")), CitaController.hora(request),
                            request.getParameter("motivo"), usuario);
                    if (r.isExito()) {
                        CitaDTO c = servicio.obtener(r.idCita()).orElseThrow();
                        MensajeFlash.exito(session, "¡Listo! Tu cita quedó reservada para "
                                + FechaTexto.paraFrase(c.getFecha(), servicio.hoy()) + " a las " + FechaTexto.hora(c.getHoraInicio())
                                + " con Dr(a). " + c.getNombreMedico()
                                + (c.getCorreoPaciente().isBlank() ? "." : ". Te enviamos la confirmación a tu correo."));
                    } else {
                        MensajeFlash.error(session, r.error());
                        destino = ctx + RUTA + "?accion=reservar" + CitaController.volverAlAsistente(request);
                    }
                    break;
                }
                case "reprogramar": {
                    String error = servicio.reprogramar(id, ControladorUtil.entero(request.getParameter("medico"), 0),
                            ControladorUtil.fecha(request.getParameter("fecha")), CitaController.hora(request), usuario);
                    if (error == null) {
                        CitaDTO c = servicio.obtener(id).orElseThrow();
                        MensajeFlash.exito(session, "Tu cita cambió para " + FechaTexto.paraFrase(c.getFecha(), servicio.hoy())
                                + " a las " + FechaTexto.hora(c.getHoraInicio()) + " con Dr(a). " + c.getNombreMedico() + ".");
                    } else {
                        MensajeFlash.error(session, error);
                        destino = ctx + RUTA + "?accion=reprogramar&id=" + id + CitaController.volverAlAsistente(request);
                    }
                    break;
                }
                case "cancelar":
                    ControladorUtil.resultado(session, servicio.cancelar(id, CitaController.motivo(request), usuario),
                            "Tu cita fue cancelada. ¡Gracias por avisar! Así otro paciente puede usar ese turno.");
                    break;
                default:
                    MensajeFlash.error(session, "Acción no reconocida.");
            }
        } catch (RuntimeException e) {
            System.err.println("MisCitasController: " + e.getMessage());
            MensajeFlash.error(session, "No se pudo completar la operación. Inténtalo de nuevo en unos minutos.");
        }
        response.sendRedirect(destino);
    }

    private static Usuario usuario(HttpServletRequest request) {
        return (Usuario) request.getSession().getAttribute("usuarioLogueado");
    }

    private static void vista(HttpServletRequest request, HttpServletResponse response, String titulo, String jsp)
            throws ServletException, IOException {
        request.setAttribute("tituloPagina", titulo);
        request.getRequestDispatcher(jsp).forward(request, response);
    }
}
