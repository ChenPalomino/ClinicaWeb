package modelo.repositorio;

import modelo.dto.MedicoDTO;
import modelo.dto.OpcionDTO;

import java.util.List;
import java.util.Optional;

/**
 * PATRÓN: Repository  ->  INTERFAZ de los médicos y su horario (RF-03).
 * El servicio de citas depende de esta interfaz, no del SQL: en las pruebas
 * se reemplaza por una versión en memoria (RNF-03).
 */
public interface MedicoRepository {

    /** Todas las cuentas de rol Médico, configuradas o no (pantalla del administrador). */
    List<MedicoDTO> listarTodos();

    int LIMITE_BUSQUEDA = 100;

    default List<MedicoDTO> buscar(String texto, Integer idEspecialidad, String estado) {
        String t = texto == null ? "" : texto.trim().toLowerCase();
        return listarTodos().stream()
                .filter(m -> t.isEmpty() || (m.getNombreCompleto() + " " + m.getNombreUsuario() + " " + m.getCmp()).toLowerCase().contains(t))
                .filter(m -> idEspecialidad == null || m.getIdEspecialidad() == idEspecialidad)
                .filter(m -> estado == null || cumpleEstado(m, estado))
                .limit(LIMITE_BUSQUEDA)
                .collect(java.util.stream.Collectors.toList());
    }

    private static boolean cumpleEstado(MedicoDTO m, String estado) {
        switch (estado) {
            case "ATIENDE": return m.isAtiende();
            case "SIN_CONFIGURAR": return !m.isConfigurado();
            case "PAUSA": return m.isConfigurado() && !m.isActivo();
            default: return false;
        }
    }

    default java.util.Map<Integer, java.math.BigDecimal> preciosConsulta() {
        return java.util.Map.of();
    }

    /** Médicos que reciben citas (configurados, activos y con horario); idEspecialidad null = todos. */
    List<MedicoDTO> listarQueAtienden(Integer idEspecialidad);

    /** Datos del médico de una cuenta (si la cuenta es de rol Médico), aunque aún no esté configurado. */
    Optional<MedicoDTO> buscarPorUsuario(int idUsuario);

    Optional<MedicoDTO> buscarPorId(int idMedico);

    /**
     * Crea o actualiza los datos profesionales y REEMPLAZA el horario semanal, en una transacción.
     * @throws OperacionRechazadaException si el CMP ya lo usa otro médico
     */
    int guardar(MedicoDTO medico, String usuario);

    boolean cambiarEstado(int idMedico, String estado);

    /** Especialidades activas que tienen al menos un médico que atiende. extra = cantidad de médicos. */
    List<OpcionDTO> especialidadesConMedicos();

    /** El servicio "Consulta de ..." del tarifario para esa especialidad (para cobrar la cita). */
    Optional<Integer> servicioDeConsulta(int idEspecialidad);
}
