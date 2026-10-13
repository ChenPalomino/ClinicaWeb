package evento;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * PATRÓN: Observer  ->  SUJETO (Subject / publicador)
 *
 * Guarda la lista de observadores y les avisa cada vez que un servicio
 * publica un evento. Es único en la aplicación (Singleton) para que todos
 * los servicios publiquen en el mismo lugar.
 *
 * Un observador que falla (por ejemplo, la BD de auditoría no responde) NO
 * interrumpe la operación del usuario ni a los demás observadores.
 */
public final class EventoSubject {

    private static final EventoSubject INSTANCIA = new EventoSubject();

    // Lista segura para varios hilos: Tomcat atiende peticiones en paralelo
    private final List<EventoObserver> observadores = new CopyOnWriteArrayList<>();

    private EventoSubject() {
    }

    public static EventoSubject getInstancia() {
        return INSTANCIA;
    }

    public void suscribir(EventoObserver observador) {
        if (!observadores.contains(observador)) {
            observadores.add(observador);
        }
    }

    public void desuscribir(EventoObserver observador) {
        observadores.remove(observador);
    }

    /** Avisa a todos los observadores suscritos. */
    public void publicar(EventoSistema evento) {
        for (EventoObserver o : observadores) {
            try {
                o.actualizar(evento);
            } catch (RuntimeException e) {
                System.err.println("EventoSubject: " + o.getClass().getSimpleName() + " falló -> " + e.getMessage());
            }
        }
    }

    /** Atajo: publica un evento hecho por un usuario de la sesión (puede ser null en tareas del sistema). */
    public void publicar(TipoEvento tipo, modelo.entidades.Usuario quien, Integer idEntidad, String detalle) {
        publicar(new EventoSistema(tipo, quien == null ? null : quien.getIdUsuario(),
                quien == null ? "sistema" : quien.getNombreUsuario(), idEntidad, detalle));
    }

    int cantidadObservadores() {
        return observadores.size();
    }
}
