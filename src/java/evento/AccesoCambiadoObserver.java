package evento;

import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

/**
 * PATRÓN: Observer  ->  OBSERVADOR CONCRETO
 *
 * Se entera cuando el administrador o la recepción cambian el ACCESO de una
 * cuenta (la bloquea, la desactiva, le cambia el rol...). En ese momento sube
 * un contador de "versión de accesos"; SesionVigente ve que cambió y revisa en
 * la base de datos las sesiones abiertas EN SU SIGUIENTE CLIC, sin esperar
 * al intervalo normal de revisión. Así el sistema es rápido (no consulta la BD
 * en cada página) y a la vez seguro (la expulsión es inmediata).
 */
public class AccesoCambiadoObserver implements EventoObserver {

    private static final Set<TipoEvento> CAMBIAN_ACCESO = EnumSet.of(
            TipoEvento.USUARIO_EDITADO,
            TipoEvento.USUARIO_DESACTIVADO,
            TipoEvento.USUARIO_BLOQUEADO,
            TipoEvento.PACIENTE_DESACTIVADO);

    private static final AtomicLong VERSION = new AtomicLong();

    @Override
    public void actualizar(EventoSistema evento) {
        if (CAMBIAN_ACCESO.contains(evento.getTipo())) {
            VERSION.incrementAndGet();
        }
    }

    /** Cambia cada vez que se modifica el acceso de alguna cuenta. */
    public static long version() {
        return VERSION.get();
    }
}
