package controlador.recepcion;

import controlador.ControladorUtil;
import controlador.MensajeFlash;
import controlador.citas.AccionesCita;
import controlador.citas.AgendaCitas;
import controlador.citas.AsistenteReserva;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import modelo.citas.FechaTexto;
import modelo.dto.CitaDTO;
import modelo.dto.FiltroBusquedaDTO;
import modelo.dto.PacienteDTO;
import modelo.dto.PaginaDTO;
import modelo.entidades.Usuario;
import modelo.servicios.CitaService;
import modelo.servicios.PacienteService;

import java.io.IOException;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.Optional;

/**
 * Controlador (MVC) de CITAS para ADMISIÓN (PP-01) — Recepción y Administrador.
 *
 *   GET  /recepcion/citas                         -> agenda (hoy por defecto; día / semana / mes)
 *   GET  /recepcion/citas?accion=reservar         -> asistente: paciente -> especialidad -> día -> hora
 *   GET  /recepcion/citas?accion=ver&id=          -> detalle de la cita con sus acciones
 *   GET  /recepcion/citas?accion=reprogramar&id=  -> asistente para elegir el nuevo horario
 *   POST accion=reservar | reprogramar | cancelar | llegada | cobrar   (Post/Redirect/Get)
 */
@WebServlet(name = "CitaController", urlPatterns = {"/recepcion/citas"})
public class CitaController extends HttpServlet {

    private static final String RUTA = "/recepcion/citas";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setAttribute("menuActivo", "citas");
        request.setAttribute("rutaCitas", request.getContextPath() + RUTA);
        CitaService servicio = new CitaService();
        String accion = request.getParameter("accion");
        Usuario usuario = usuario(request);
        try {
            if ("reservar".equals(accion)) {
                cargarPaciente(request);
                AsistenteReserva.cargar(request, servicio, null, null);
                vista(request, response, "Nueva cita", "/WEB-INF/vistas/citas/reservar.jsp");
                return;
            }
            if ("ver".equals(accion) || "reprogramar".equals(accion)) {
                Optional<CitaDTO> cita = servicio.obtener(ControladorUtil.entero(request.getParameter("id"), 0));
                if (cita.isEmpty()) {
                    MensajeFlash.error(request.getSession(), "La cita que buscas no existe.");
                    response.sendRedirect(request.getContextPath() + RUTA);
                    return;
                }
                AccionesCita acciones = new AccionesCita(cita.get(), usuario, servicio.ahora(), servicio.horasCancelacion());
                if ("reprogramar".equals(accion)) {
                    if (!acciones.isReprogramar()) {
                        MensajeFlash.aviso(request.getSession(), "Esta cita ya no se puede reprogramar.");
                        response.sendRedirect(request.getContextPath() + RUTA + "?accion=ver&id=" + cita.get().getIdCita());
                        return;
                    }
                    AsistenteReserva.cargar(request, servicio, cita.get().getIdEspecialidad(), cita.get());
                    vista(request, response, "Reprogramar " + cita.get().getCodigo(), "/WEB-INF/vistas/citas/reservar.jsp");
                    return;
                }
                request.setAttribute("cita", cita.get());
                request.setAttribute("acciones", acciones);
                request.setAttribute("hoy", servicio.hoy());
                request.setAttribute("motivosCancelacion", CitaService.MOTIVOS_CANCELACION);
                vista(request, response, "Cita " + cita.get().getCodigo(), "/WEB-INF/vistas/citas/detalle.jsp");
                return;
            }
            AgendaCitas.cargar(request, servicio, null);
            request.setAttribute("modoAgenda", "recepcion");
            request.setAttribute("motivosCancelacion", CitaService.MOTIVOS_CANCELACION);
            request.setAttribute("horasCancelacion", servicio.horasCancelacion());
        } catch (RuntimeException e) {
            System.err.println("CitaController: " + e.getMessage());
            request.setAttribute("errorDatos", true);
        }
        vista(request, response, "Citas", "/WEB-INF/vistas/citas/agenda.jsp");
    }

    /** Paso 1 del asistente: el paciente elegido, o los resultados de la búsqueda (uno solo = se elige solo). */
    private void cargarPaciente(HttpServletRequest request) {
        PacienteService pacientes = new PacienteService();
        Integer idPaciente = ControladorUtil.enteroONulo(request.getParameter("paciente"));
        if (idPaciente == null) {
            String q = request.getParameter("q");
            if (q != null && !q.isBlank()) {
                PaginaDTO<PacienteDTO> encontrados = pacientes.listar(new FiltroBusquedaDTO(q, "ACTIVO", null, null, 1));
                if (encontrados.getTotal() == 1) {
                    idPaciente = encontrados.getElementos().get(0).getIdPaciente();     // RNF-09: un clic menos
                } else {
                    request.setAttribute("pacientesEncontrados", encontrados);
                }
            }
        }
        if (idPaciente != null) {
            Optional<PacienteDTO> p = pacientes.obtener(idPaciente);
            if (p.isPresent() && p.get().isActivo()) {
                request.setAttribute("pacienteSel", p.get());
            }
        }
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
        String verCita = ctx + RUTA + "?accion=ver&id=" + id;
        String regreso = ControladorUtil.regresoListado(request);
        String destino = regreso.isEmpty() ? verCita : ctx + RUTA + regreso;
        try {
            switch (accion == null ? "" : accion) {
                case "reservar":
                    destino = reservar(request, servicio, usuario);
                    break;
                case "reprogramar":
                    destino = reprogramar(request, servicio, usuario, id, verCita);
                    break;
                case "cancelar":
                    ControladorUtil.resultado(session, servicio.cancelar(id, motivo(request), usuario),
                            "Cita cancelada. El turno quedó libre para otro paciente.");
                    break;
                case "llegada":
                    ControladorUtil.resultado(session, servicio.registrarLlegada(id, usuario),
                            "Llegada registrada: el paciente figura «en sala de espera» en la agenda del médico.");
                    break;
                case "cobrar":
                    destino = cobrar(session, servicio.cobrar(id, usuario), ctx, destino);
                    break;
                default:
                    MensajeFlash.error(session, "Acción no reconocida.");
                    destino = ctx + RUTA;
            }
        } catch (RuntimeException e) {
            System.err.println("CitaController: " + e.getMessage());
            MensajeFlash.error(session, "No se pudo completar la operación. Inténtalo de nuevo en unos minutos.");
        }
        response.sendRedirect(destino);
    }

    private static String reservar(HttpServletRequest request, CitaService servicio, Usuario usuario) {
        String base = request.getContextPath() + RUTA;
        int idPaciente = ControladorUtil.entero(request.getParameter("paciente"), 0);
        CitaService.Resultado r = servicio.reservar(idPaciente, ControladorUtil.entero(request.getParameter("medico"), 0),
                ControladorUtil.fecha(request.getParameter("fecha")), hora(request), request.getParameter("motivo"), usuario);
        if (!r.isExito()) {
            MensajeFlash.error(request.getSession(), r.error());
            return base + "?accion=reservar&paciente=" + idPaciente + volverAlAsistente(request);
        }
        CitaDTO c = servicio.obtener(r.idCita()).orElseThrow();
        MensajeFlash.exito(request.getSession(), "Cita " + r.codigo() + " reservada para " + c.getNombrePaciente() + ", "
                + FechaTexto.paraFrase(c.getFecha(), servicio.hoy()) + " a las " + FechaTexto.hora(c.getHoraInicio())
                + avisoCorreo(c));
        return base + "?accion=ver&id=" + r.idCita();
    }

    private static String reprogramar(HttpServletRequest request, CitaService servicio, Usuario usuario, int id, String verCita) {
        String error = servicio.reprogramar(id, ControladorUtil.entero(request.getParameter("medico"), 0),
                ControladorUtil.fecha(request.getParameter("fecha")), hora(request), usuario);
        if (error != null) {
            MensajeFlash.error(request.getSession(), error);
            return request.getContextPath() + RUTA + "?accion=reprogramar&id=" + id + volverAlAsistente(request);
        }
        CitaDTO c = servicio.obtener(id).orElseThrow();
        MensajeFlash.exito(request.getSession(), "Cita reprogramada para " + FechaTexto.paraFrase(c.getFecha(), servicio.hoy())
                + " a las " + FechaTexto.hora(c.getHoraInicio()) + avisoCorreo(c));
        return verCita;
    }

    private static String cobrar(HttpSession session, CitaService.Cobro cobro, String ctx, String destino) {
        if (!cobro.isExito()) {
            MensajeFlash.error(session, cobro.error());
            return destino;
        }
        if (cobro.aviso() != null) {
            MensajeFlash.aviso(session, cobro.aviso());
        }
        return ctx + "/caja/cuentas?accion=ver&id=" + cobro.idCuenta();
    }

    // ---------------------------------------------------------------------

    static String avisoCorreo(CitaDTO c) {
        return c.getCorreoPaciente().isBlank() ? " — el paciente no tiene correo registrado: entrégale el código " + c.getCodigo() + "."
                : " — le enviamos la confirmación a su correo.";
    }

    /** Motivo elegido de la lista o escrito en "Otro motivo". */
    public static String motivo(HttpServletRequest request) {
        String elegido = request.getParameter("motivo");
        String otro = request.getParameter("motivoOtro");
        return "OTRO".equals(elegido) || elegido == null || elegido.isBlank() ? otro : elegido;
    }

    public static LocalTime hora(HttpServletRequest request) {
        try {
            String h = request.getParameter("hora");
            return h == null || h.isBlank() ? null : LocalTime.parse(h.trim());
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    /** Si la reserva falla, se vuelve al asistente con la misma especialidad y fecha. */
    public static String volverAlAsistente(HttpServletRequest request) {
        StringBuilder sb = new StringBuilder();
        for (String p : new String[]{"esp", "fecha"}) {
            String v = request.getParameter(p);
            if (v != null && v.matches("[0-9-]{1,10}")) {
                sb.append('&').append(p).append('=').append(v);
            }
        }
        return sb.toString();
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
