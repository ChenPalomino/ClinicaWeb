package modelo.repositorio;

import evento.EventoSistema;
import modelo.dto.AuditoriaDTO;
import modelo.dto.FiltroAuditoriaDTO;
import modelo.dto.PaginaDTO;

import java.util.List;

/**
 * PATRÓN: Repository  ->  INTERFAZ de la auditoría (tt_auditoria).
 * Solo registra y consulta: la auditoría no se edita ni se borra.
 */
public interface AuditoriaRepository {

    void registrar(EventoSistema evento);

    default void sincronizarAcciones() {
    }

    PaginaDTO<AuditoriaDTO> listar(FiltroAuditoriaDTO filtro);

    /** Últimos movimientos de un registro (ej. historial de un paciente). */
    List<AuditoriaDTO> ultimosDe(String entidad, int idEntidad, int cantidad);
}
