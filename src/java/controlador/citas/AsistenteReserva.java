package controlador.citas;

import controlador.ControladorUtil;
import jakarta.servlet.http.HttpServletRequest;
import modelo.dto.AgendaMedicoDTO;
import modelo.dto.CitaDTO;
import modelo.dto.DiaDisponibleDTO;
import modelo.dto.MedicoDTO;
import modelo.dto.OpcionDTO;
import modelo.servicios.CitaService;

import java.time.LocalDate;
import java.util.List;

/**
 * Prepara el ASISTENTE DE RESERVA (citas/reservar.jsp), que comparten
 * recepción, el paciente y la reprogramación. Un paso a la vez y con la
 * siguiente opción ya sugerida, para reservar en pocas interacciones (RNF-09):
 *   especialidad -> (médico opcional) -> día (se elige solo el primero con turnos) -> hora -> confirmar.
 * Si hay una sola especialidad, se elige sola.
 */
public final class AsistenteReserva {

    private AsistenteReserva() {
    }

    /**
     * @param especialidadFija id de la especialidad que no se puede cambiar (reprogramar) o null
     * @param citaQueSeMueve   la cita que se reprograma (o null al reservar)
     */
    public static void cargar(HttpServletRequest request, CitaService servicio, Integer especialidadFija, CitaDTO citaQueSeMueve) {
        List<OpcionDTO> especialidades = servicio.especialidades();
        Integer esp = especialidadElegida(request, especialidades, especialidadFija);
        request.setAttribute("especialidades", especialidades);
        request.setAttribute("espSel", esp);
        request.setAttribute("citaQueSeMueve", citaQueSeMueve);
        request.setAttribute("hoy", servicio.hoy());
        request.setAttribute("horasCancelacion", servicio.horasCancelacion());
        request.setAttribute("ultimoDia", servicio.ultimoDiaReserva());
        request.setAttribute("precios", precios(servicio, pacienteDelAsistente(request, citaQueSeMueve)));
        if (esp != null) {
            cargarTurnos(request, servicio, esp);
        }
    }

    private static Integer especialidadElegida(HttpServletRequest request, List<OpcionDTO> especialidades, Integer fija) {
        Integer esp = fija != null ? fija : ControladorUtil.enteroONulo(request.getParameter("esp"));
        if (esp == null && especialidades.size() == 1) {
            return especialidades.get(0).getId();
        }
        final Integer idEsp = esp;
        return esp != null && especialidades.stream().noneMatch(o -> o.getId() == idEsp) ? null : esp;
    }

    private static Integer pacienteDelAsistente(HttpServletRequest request, CitaDTO citaQueSeMueve) {
        if (citaQueSeMueve != null) {
            return citaQueSeMueve.getIdPaciente();
        }
        Object pacienteSel = request.getAttribute("pacienteSel");
        return pacienteSel instanceof modelo.dto.PacienteDTO ? Integer.valueOf(((modelo.dto.PacienteDTO) pacienteSel).getIdPaciente()) : null;
    }

    private static java.util.Map<Integer, modelo.dto.PrecioConsultaDTO> precios(CitaService servicio, Integer idPaciente) {
        try {
            return servicio.preciosConsulta(idPaciente);
        } catch (RuntimeException e) {
            System.err.println("AsistenteReserva: no se pudieron leer los precios -> " + e.getMessage());
            return java.util.Map.of();
        }
    }

    private static void cargarTurnos(HttpServletRequest request, CitaService servicio, Integer esp) {
        List<MedicoDTO> medicos = servicio.medicos(esp);
        Integer elegido = ControladorUtil.enteroONulo(request.getParameter("medico"));
        Integer medico = elegido != null && medicos.stream().anyMatch(m -> m.getIdMedico() == elegido) ? elegido : null;
        request.setAttribute("medicos", medicos);
        request.setAttribute("medicoSel", medico);
        LocalDate fecha = ControladorUtil.fecha(request.getParameter("fecha"));
        LocalDate desde = ControladorUtil.fecha(request.getParameter("desde"));
        List<DiaDisponibleDTO> dias = servicio.dias(esp, medico, desde == null ? fecha : desde);
        if (fecha == null) {
            fecha = dias.stream().filter(d -> d.getLibres() > 0).map(DiaDisponibleDTO::getFecha).findFirst().orElse(null);
        }
        request.setAttribute("dias", dias);
        request.setAttribute("desde", dias.isEmpty() ? servicio.hoy() : dias.get(0).getFecha());
        request.setAttribute("fechaSel", fecha);
        request.setAttribute("turnos", fecha == null ? List.of() : servicio.turnos(esp, medico, fecha));
    }
}
