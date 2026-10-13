package evento;

import modelo.repositorio.AuditoriaRepository;
import modelo.repositorio.AuditoriaRepositoryImpl;

/**
 * PATRÓN: Observer  ->  OBSERVADOR CONCRETO de auditoría.
 * Cada evento publicado se guarda en tt_auditoria (quién, qué, cuándo, desde dónde).
 */
public class AuditoriaObserver implements EventoObserver {

    private final AuditoriaRepository auditoriaRepository;

    public AuditoriaObserver() {
        this(new AuditoriaRepositoryImpl());
    }

    public AuditoriaObserver(AuditoriaRepository auditoriaRepository) {
        this.auditoriaRepository = auditoriaRepository;
    }

    @Override
    public void actualizar(EventoSistema evento) {
        auditoriaRepository.registrar(evento);
    }
}
