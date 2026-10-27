package controlador;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import modelo.entidades.Rol;
import menu.MenuFactory;
import modelo.entidades.Usuario;
import modelo.servicios.CierreCajaService;
import modelo.dto.FiltroCitaDTO;
import modelo.dto.MedicoDTO;
import modelo.servicios.CitaService;
import modelo.servicios.CobranzaService;
import modelo.servicios.IndicadorService;
import modelo.servicios.ReportePagoService;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Optional;

/**
 * Controlador (MVC) de la página de inicio del dashboard (/panel).
 * Cada rol ve SU propio inicio: la estrategia del rol (MenuFactory -> MenuStrategy)
 * indica qué vista usar, así este controlador no necesita un if por cada rol.
 * Cada inicio muestra lo que ese rol necesita HOY (Nielsen #1, visibilidad del estado):
 *   - Administrador: indicadores de usuarios y citas del día
 *   - Recepción: agenda del día de todos los médicos y su caja
 *   - Médico: sus pacientes de hoy, quién está en sala de espera
 *   - Paciente: su próxima cita (o una invitación clara a reservar)
 * Solo se consulta el DÍA de hoy (o las próximas citas del paciente): la portada es liviana.
 */
@WebServlet(name = "InicioController", urlPatterns = {"/panel"})
public class InicioController extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        Usuario usuario = (Usuario) request.getSession().getAttribute("usuarioLogueado");
        if (usuario.getRolId() == Rol.ADMINISTRADOR) {
            try {
                request.setAttribute("indicadores", new IndicadorService().catalogo());
                request.setAttribute("porCobrar", new CobranzaService().contarPendientes());
                request.setAttribute("pagosInformados", new ReportePagoService().contarPendientes());
            } catch (RuntimeException e) {
                System.err.println("InicioController: " + e.getMessage());
                request.setAttribute("errorDatos", true);
            }
        }
        if (usuario.getRolId() == Rol.RECEPCIONISTA) {
            try {
                request.setAttribute("cajaHoy", new CierreCajaService().resumenDeHoy(usuario));
                request.setAttribute("porCobrar", new CobranzaService().contarPendientes());
            } catch (RuntimeException e) {
                System.err.println("InicioController: " + e.getMessage());
            }
        }
        try {
            cargarCitas(request, usuario);
        } catch (RuntimeException e) {
            System.err.println("InicioController (citas): " + e.getMessage());
            request.setAttribute("errorCitas", true);
        }
        request.setAttribute("tituloPagina", "Inicio");
        request.setAttribute("rutaCitas", request.getContextPath() + rutaCitas(usuario.getRolId()));
        request.setAttribute("menuActivo", "inicio");
        request.getRequestDispatcher(MenuFactory.crear(usuario.getRolId()).getVistaInicio()).forward(request, response);
    }

    private static String rutaCitas(int rol) {
        if (rol == Rol.MEDICO) return "/medico/agenda";
        if (rol == Rol.PACIENTE) return "/paciente/citas";
        return "/recepcion/citas";
    }

    /** Lo que cada rol ve de sus citas en la portada. */
    private void cargarCitas(HttpServletRequest request, Usuario usuario) {
        CitaService citas = new CitaService();
        LocalDate hoy = citas.hoy();
        request.setAttribute("hoy", hoy);
        request.setAttribute("ahora", citas.ahora());
        switch (usuario.getRolId()) {
            case Rol.PACIENTE: {
                Optional<Integer> idPaciente = citas.pacienteDeUsuario(usuario.getIdUsuario());
                if (idPaciente.isPresent()) {
                    request.setAttribute("proximas", citas.proximasDelPaciente(idPaciente.get(), 3));
                    request.setAttribute("atendidas", citas.historialDelPaciente(idPaciente.get(), 50).stream()
                            .filter(c -> c.isAtendida()).count());
                    request.setAttribute("horasCancelacion", citas.horasCancelacion());
                }
                break;
            }
            case Rol.MEDICO: {
                Optional<MedicoDTO> yo = citas.medicoDeUsuario(usuario.getIdUsuario());
                request.setAttribute("medicoSesion", yo.orElse(null));
                if (yo.isPresent() && yo.get().isConfigurado()) {
                    FiltroCitaDTO f = new FiltroCitaDTO(hoy, hoy, yo.get().getIdMedico(), null, null, null);
                    request.setAttribute("citasHoy", citas.agenda(f));
                    request.setAttribute("resumenHoy", citas.resumen(f));
                }
                break;
            }
            default: {
                FiltroCitaDTO f = new FiltroCitaDTO(hoy, hoy, null, null, null, null);
                request.setAttribute("resumenHoy", citas.resumen(f));
                if (usuario.getRolId() == Rol.RECEPCIONISTA) {
                    request.setAttribute("citasHoy", citas.agenda(f));
                }
            }
        }
    }
}
