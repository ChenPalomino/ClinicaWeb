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
        Integer esp = especialidadFija != null ? especialidadFija : ControladorUtil.enteroONulo(request.getParameter("esp"));
        if (esp == null && especialidades.size() == 1) {
            esp = especialidades.get(0).getId();
        }
        final Integer idEsp = esp;
        if (esp != null && especialidades.stream().noneMatch(o -> o.getId() == idEsp)) {
            esp = null;     // especialidad sin médicos que atiendan o inexistente
        }
        request.setAttribute("especialidades", especialidades);
        request.setAttribute("espSel", esp);
        request.setAttribute("citaQueSeMueve", citaQueSeMueve);
        request.setAttribute("hoy", servicio.hoy());
        request.setAttribute("horasCancelacion", servicio.horasCancelacion());
        request.setAttribute("ultimoDia", servicio.ultimoDiaReserva());
        if (esp == null) {
            return;
        }
        List<MedicoDTO> medicos = servicio.medicos(esp);
        Integer medico = ControladorUtil.enteroONulo(request.getParameter("medico"));
        final Integer idMed = medico;
        if (medico != null && medicos.stream().noneMatch(m -> m.getIdMedico() == idMed)) {
            medico = null;
        }
        request.setAttribute("medicos", medicos);
        request.setAttribute("medicoSel", medico);

        LocalDate desde = ControladorUtil.fecha(request.getParameter("desde"));
        LocalDate fecha = ControladorUtil.fecha(request.getParameter("fecha"));
        if (desde == null && fecha != null) {
            desde = fecha;
        }
        List<DiaDisponibleDTO> dias = servicio.dias(esp, medico, desde);
        if (fecha == null) {
            // Se sugiere el primer día con turnos libres: un clic menos (Nielsen #7)
            fecha = dias.stream().filter(d -> d.getLibres() > 0).map(DiaDisponibleDTO::getFecha).findFirst().orElse(null);
        }
        List<AgendaMedicoDTO> turnos = fecha == null ? List.of() : servicio.turnos(esp, medico, fecha);
        request.setAttribute("dias", dias);
        request.setAttribute("desde", dias.isEmpty() ? servicio.hoy() : dias.get(0).getFecha());
        request.setAttribute("fechaSel", fecha);
        request.setAttribute("turnos", turnos);
    }
}
