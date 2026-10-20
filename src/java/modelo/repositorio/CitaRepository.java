package modelo.repositorio;

import modelo.dto.CitaDTO;
import modelo.dto.FiltroCitaDTO;
import modelo.dto.ResumenCitasDTO;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * PATRÓN: Repository  ->  INTERFAZ de las citas (PP-01, RF-04 a RF-10).
 * Los cambios de estado solo se aplican si la cita sigue en el estado
 * esperado (UPDATE ... WHERE estado_cita = ...): si dos personas actúan a la
 * vez, solo la primera lo logra y la segunda recibe "false".
 */
public interface CitaRepository {

    /** Resultado de reservar: id y código (CT-000123). */
    record NuevaCita(int idCita, String codigo) { }

    Optional<CitaDTO> buscarPorId(int idCita);

    /** Citas del rango del filtro, en orden de fecha y hora (máximo FiltroCitaDTO.MAXIMO_FILAS). */
    List<CitaDTO> agenda(FiltroCitaDTO filtro);

    /** Cuántas citas hay por día y estado en el rango (vista de mes: una consulta agrupada, muy liviana). */
    Map<LocalDate, ResumenCitasDTO> resumenPorDia(FiltroCitaDTO filtro);

    /** Turnos ocupados (citas no canceladas): idMedico -> fecha -> horas de inicio. */
    Map<Integer, Map<LocalDate, Set<LocalTime>>> ocupados(Collection<Integer> idMedicos, LocalDate desde, LocalDate hasta);

    /** Citas activas (reservadas o con llegada) del paciente desde una fecha: para evitar choques de horario. */
    List<CitaDTO> activasDelPaciente(int idPaciente, LocalDate desde);

    /** Próximas citas activas (proximas = true) o el historial (proximas = false) del paciente. */
    List<CitaDTO> delPaciente(int idPaciente, boolean proximas, LocalDate hoy, int limite);

    /** Citas activas de un médico desde una fecha (para avisar si el nuevo horario las deja fuera). */
    List<CitaDTO> activasDelMedico(int idMedico, LocalDate desde);

    /** Ficha de paciente de una cuenta del portal. */
    Optional<Integer> idPacienteDeUsuario(int idUsuario);

    /**
     * RF-05. @throws OperacionRechazadaException si otra persona tomó ese turno un instante antes
     * (lo detecta el índice único de la base de datos, RNF-08).
     */
    NuevaCita reservar(CitaDTO cita, String usuario);

    /** RF-06: solo si sigue RESERVADA. @throws OperacionRechazadaException si el nuevo turno ya se ocupó. */
    boolean reprogramar(int idCita, int idMedico, LocalDate fecha, LocalTime inicio, LocalTime fin, String usuario);

    /** RF-07: solo si sigue activa. */
    boolean cancelar(int idCita, String motivo, String usuario);

    /** RF-08: RESERVADA -> CONFIRMADA (el paciente llegó). */
    boolean registrarLlegada(int idCita, String usuario);

    /** RF-10: activa -> ATENDIDA o NO_ASISTIO. */
    boolean registrarAtencion(int idCita, String estado, String nota, String usuario);

    /** Une la cita con su cuenta de atención (cobro en caja). */
    boolean vincularCuenta(int idCita, int idCuenta);
}
