package controlador.medico;

import controlador.ControladorUtil;
import controlador.MensajeFlash;
import controlador.citas.AccionesCita;
import controlador.citas.AgendaCitas;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import modelo.dto.CitaDTO;
import modelo.dto.MedicoDTO;
import modelo.entidades.Rol;
import modelo.entidades.Usuario;
import modelo.servicios.CitaService;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Controlador (MVC) de "MI AGENDA" del MÉDICO (RF-10).
 *
 *   GET  /medico/agenda                 -> sus citas de hoy (o semana / mes)
 *   GET  /medico/agenda?accion=ver&id=  -> ficha de la cita: datos del paciente, alertas y atención
 *   POST accion=atendida | noasistio     (con una nota opcional)
 *
 * El médico solo ve SU agenda. El administrador puede elegir de qué médico verla.
 */
@WebServlet(name = "AgendaMedicoController", urlPatterns = {"/medico/agenda"})
public class AgendaMedicoController extends HttpServlet {

    private static final String RUTA = "/medico/agenda";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setAttribute("menuActivo", "agenda");
        request.setAttribute("rutaCitas", request.getContextPath() + RUTA);
        CitaService servicio = new CitaService();
        Usuario usuario = (Usuario) request.getSession().getAttribute("usuarioLogueado");
        String titulo = "Mi agenda";
        try {
            if ("ver".equals(request.getParameter("accion"))) {
                Optional<CitaDTO> cita = servicio.obtener(ControladorUtil.entero(request.getParameter("id"), 0));
                if (cita.isEmpty() || !CitaService.puedeVer(cita.get(), usuario)) {
                    MensajeFlash.error(request.getSession(), "Esa cita no existe o no es de tu agenda.");
                    response.sendRedirect(request.getContextPath() + RUTA);
                    return;
                }
                request.setAttribute("cita", cita.get());
                request.setAttribute("acciones", new AccionesCita(cita.get(), usuario, servicio.ahora(), servicio.horasCancelacion()));
                request.setAttribute("hoy", servicio.hoy());
                request.setAttribute("tituloPagina", "Cita " + cita.get().getCodigo());
                request.getRequestDispatcher("/WEB-INF/vistas/citas/detalle.jsp").forward(request, response);
                return;
            }
            Integer idMedico;
            if (usuario.getRolId() == Rol.MEDICO) {
                Optional<MedicoDTO> yo = servicio.medicoDeUsuario(usuario.getIdUsuario());
                request.setAttribute("medicoSesion", yo.orElse(null));
                idMedico = yo.filter(MedicoDTO::isConfigurado).map(MedicoDTO::getIdMedico).orElse(-1);
            } else {
                // Administrador: elige el médico (por defecto el primero que atiende)
                List<MedicoDTO> medicos = servicio.medicos(null);
                Integer elegido = ControladorUtil.enteroONulo(request.getParameter("medico"));
                idMedico = elegido != null ? elegido : medicos.stream().findFirst().map(MedicoDTO::getIdMedico).orElse(-1);
                request.setAttribute("medicosFiltroAdmin", medicos);
                final int sel = idMedico;
                medicos.stream().filter(m -> m.getIdMedico() == sel).findFirst()
                       .ifPresent(m -> request.setAttribute("medicoSesion", m));
                titulo = "Agenda del médico";
            }
            AgendaCitas.cargar(request, servicio, idMedico);
            request.setAttribute("modoAgenda", "medico");
        } catch (RuntimeException e) {
            System.err.println("AgendaMedicoController: " + e.getMessage());
            request.setAttribute("errorDatos", true);
        }
        request.setAttribute("tituloPagina", titulo);
        request.getRequestDispatcher("/WEB-INF/vistas/citas/agenda.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        HttpSession session = request.getSession();
        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
        int id = ControladorUtil.entero(request.getParameter("id"), 0);
        String accion = request.getParameter("accion");
        String regreso = ControladorUtil.regresoListado(request);
        String destino = request.getContextPath() + RUTA + (regreso.isEmpty() ? "?accion=ver&id=" + id : regreso);
        try {
            if ("atendida".equals(accion) || "noasistio".equals(accion)) {
                boolean atendida = "atendida".equals(accion);
                ControladorUtil.resultado(session, new CitaService().registrarAtencion(id, atendida, request.getParameter("nota"), usuario),
                        atendida ? "Atención registrada." : "Se registró que el paciente no asistió.");
            } else {
                MensajeFlash.error(session, "Acción no reconocida.");
            }
        } catch (RuntimeException e) {
            System.err.println("AgendaMedicoController: " + e.getMessage());
            MensajeFlash.error(session, "No se pudo completar la operación. Inténtalo de nuevo en unos minutos.");
        }
        response.sendRedirect(destino);
    }
}
