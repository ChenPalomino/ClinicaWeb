package modelo.servicios;

import modelo.dto.AuditoriaDTO;
import modelo.dto.FiltroAuditoriaDTO;
import modelo.dto.PaginaDTO;
import modelo.repositorio.AuditoriaRepository;
import modelo.repositorio.AuditoriaRepositoryImpl;

import java.util.List;

/** Consulta del registro de auditoría (RF-21). El registro lo escribe AuditoriaObserver. */
public class AuditoriaService {

    private final AuditoriaRepository auditoriaRepository;

    public AuditoriaService() {
        this(new AuditoriaRepositoryImpl());
    }

    public AuditoriaService(AuditoriaRepository auditoriaRepository) {
        this.auditoriaRepository = auditoriaRepository;
    }

    public PaginaDTO<AuditoriaDTO> listar(FiltroAuditoriaDTO filtro) {
        return auditoriaRepository.listar(filtro);
    }

    /** Historial corto de un registro; si falla, la pantalla igual se muestra (lista vacía). */
    public List<AuditoriaDTO> historial(String entidad, int idEntidad) {
        try {
            return auditoriaRepository.ultimosDe(entidad, idEntidad, 5);
        } catch (RuntimeException e) {
            System.err.println("AuditoriaService: " + e.getMessage());
            return List.of();
        }
    }
}
