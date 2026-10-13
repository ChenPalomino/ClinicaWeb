package evento;

/**
 * PATRÓN: Observer (de comportamiento)  ->  INTERFAZ DEL OBSERVADOR
 *
 * Cualquier clase que quiera "enterarse" de lo que pasa en el sistema
 * implementa esta interfaz y se suscribe al EventoSubject. Los servicios
 * publican eventos sin saber quién los escucha:
 *   - AuditoriaObserver  -> guarda el evento en tt_auditoria (RF-21, RNF-10).
 *   - (Etapa de citas)   -> NotificacionCitaObserver avisará por correo (RF-09).
 * Agregar una reacción nueva = una clase nueva, sin tocar los servicios.
 */
public interface EventoObserver {

    void actualizar(EventoSistema evento);
}
