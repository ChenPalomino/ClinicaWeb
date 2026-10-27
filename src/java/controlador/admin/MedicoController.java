package controlador.admin;

import controlador.ControladorUtil;
import controlador.MensajeFlash;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import modelo.dto.HorarioDTO;
import modelo.dto.MedicoDTO;
import modelo.entidades.Usuario;
import modelo.servicios.AuditoriaService;
import modelo.servicios.MedicoService;

import java.io.IOException;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Controlador (MVC) de MÉDICOS Y HORARIOS (RF-03) — solo Administrador.
 *
 *   GET  /admin/medicos                      -> lista de cuentas de rol Médico y su estado
 *   GET  /admin/medicos?accion=editar&u=ID   -> especialidad, CMP, consultorio, duración y horario semanal
 *   POST accion=guardar | pausar | reanudar
 *
 * Las cuentas de los médicos se crean en "Usuarios" (rol Médico); aquí se completan sus datos de agenda.
 */
@WebServlet(name = "MedicoController", urlPatterns = {"/admin/medicos"})
public class MedicoController extends HttpServlet {

    private static final String RUTA = "/admin/medicos";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setAttribute("menuActivo", "medicos");
        MedicoService servicio = new MedicoService();
        try {
            if ("editar".equals(request.getParameter("accion"))) {
                Optional<MedicoDTO> m = servicio.obtenerPorUsuario(ControladorUtil.entero(request.getParameter("u"), 0));
                if (m.isEmpty()) {
                    MensajeFlash.error(request.getSession(), "Esa cuenta no existe o no es de un médico.");
                    response.sendRedirect(request.getContextPath() + RUTA);
                    return;
                }
                mostrarFormulario(request, response, servicio, m.get(), Map.of());
                return;
            }
            request.setAttribute("especialidades", servicio.especialidades());
            if (ControladorUtil.busquedaSolicitada(request)) {
                String estado = request.getParameter("estado");
                request.setAttribute("medicos", servicio.buscar(request.getParameter("q"),
                        ControladorUtil.enteroONulo(request.getParameter("especialidad")),
                        estado == null || estado.isBlank() ? null : estado));
            }
        } catch (RuntimeException e) {
            System.err.println("MedicoController: " + e.getMessage());
            request.setAttribute("errorDatos", true);
        }
        request.setAttribute("tituloPagina", "Médicos y horarios");
        request.getRequestDispatcher("/WEB-INF/vistas/admin/medicos/listar.jsp").forward(request, response);
    }

    private void mostrarFormulario(HttpServletRequest request, HttpServletResponse response, MedicoService servicio,
                                   MedicoDTO medico, Map<String, String> errores) throws ServletException, IOException {
        request.setAttribute("medico", medico);
        request.setAttribute("errores", errores);
        request.setAttribute("especialidades", servicio.especialidades());
        if (medico.isConfigurado()) {
            request.setAttribute("historial", new AuditoriaService().historial("MEDICO", medico.getIdMedico()));
        }
        request.setAttribute("menuActivo", "medicos");
        request.setAttribute("tituloPagina", "Horario de " + medico.getNombreCompleto());
        request.getRequestDispatcher("/WEB-INF/vistas/admin/medicos/formulario.jsp").forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        HttpSession session = request.getSession();
        Usuario admin = (Usuario) session.getAttribute("usuarioLogueado");
        MedicoService servicio = new MedicoService();
        String accion = request.getParameter("accion");
        // "u" desde el formulario; "id" desde la ventana de confirmación común (pausar / reanudar)
        int idUsuario = ControladorUtil.entero(request.getParameter("u"), ControladorUtil.entero(request.getParameter("id"), 0));
        String volver = request.getContextPath() + RUTA + ControladorUtil.regresoListado(request);
        try {
            if ("guardar".equals(accion)) {
                Optional<MedicoDTO> actual = servicio.obtenerPorUsuario(idUsuario);
                if (actual.isEmpty()) {
                    MensajeFlash.error(session, "Esa cuenta no existe o no es de un médico.");
                    response.sendRedirect(request.getContextPath() + RUTA);
                    return;
                }
                MedicoDTO form = leer(request, actual.get());
                MedicoService.Guardado g = servicio.guardar(form, admin);
                if (!g.isExito()) {
                    mostrarFormulario(request, response, servicio, form, g.errores());
                    return;
                }
                if (g.citasFueraDeHorario() > 0) {
                    MensajeFlash.aviso(session, "Horario guardado. Atención: " + g.citasFueraDeHorario()
                            + (g.citasFueraDeHorario() == 1 ? " cita ya reservada quedó" : " citas ya reservadas quedaron")
                            + " fuera del nuevo horario. Revísalas en Citas y reprográmalas si hace falta.");
                } else {
                    MensajeFlash.exito(session, "Datos y horario de " + form.getNombreCompleto()
                            + " guardados. Ya puede recibir citas en esos horarios.");
                }
                volver = request.getContextPath() + RUTA + "?q=" + java.net.URLEncoder.encode(
                        actual.get().getNombreUsuario(), java.nio.charset.StandardCharsets.UTF_8);
            } else if ("pausar".equals(accion) || "reanudar".equals(accion)) {
                boolean activar = "reanudar".equals(accion);
                ControladorUtil.resultado(session, servicio.cambiarEstado(idUsuario, activar, admin),
                        activar ? "El médico vuelve a recibir citas." : "Citas pausadas: el médico ya no aparece para nuevas reservas. Sus citas ya reservadas se mantienen.");
            } else {
                MensajeFlash.error(session, "Acción no reconocida.");
            }
        } catch (RuntimeException e) {
            System.err.println("MedicoController: " + e.getMessage());
            MensajeFlash.error(session, "No se pudo guardar. Inténtalo de nuevo en unos minutos.");
        }
        response.sendRedirect(volver);
    }

    /** Lee el formulario: datos profesionales + hasta 2 bloques por día (d1_on, d1_i1, d1_f1, d1_i2, d1_f2...). */
    private static MedicoDTO leer(HttpServletRequest request, MedicoDTO base) {
        MedicoDTO m = new MedicoDTO();
        m.setIdMedico(base.getIdMedico());
        m.setIdUsuario(base.getIdUsuario());
        m.setNombres(base.getNombres());
        m.setApellidos(base.getApellidos());
        m.setNombreUsuario(base.getNombreUsuario());
        m.setUsuarioActivo(base.isUsuarioActivo());
        m.setEstado(base.getEstado());
        m.setIdEspecialidad(ControladorUtil.entero(request.getParameter("idEspecialidad"), 0));
        m.setCmp(request.getParameter("cmp"));
        m.setConsultorio(request.getParameter("consultorio"));
        m.setDuracionCita(ControladorUtil.entero(request.getParameter("duracionCita"), 0));
        List<HorarioDTO> bloques = new ArrayList<>();
        for (int d = 1; d <= 7; d++) {
            if (request.getParameter("d" + d + "_on") == null) {
                continue;
            }
            for (int b = 1; b <= MedicoService.MAXIMO_BLOQUES_POR_DIA; b++) {
                String i = request.getParameter("d" + d + "_i" + b);
                String f = request.getParameter("d" + d + "_f" + b);
                boolean vacio = (i == null || i.isBlank()) && (f == null || f.isBlank());
                if (!vacio) {
                    bloques.add(new HorarioDTO(d, hora(i), hora(f)));
                }
            }
        }
        m.setHorarios(bloques);
        return m;
    }

    private static LocalTime hora(String v) {
        try {
            return v == null || v.isBlank() ? null : LocalTime.parse(v.trim());
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
