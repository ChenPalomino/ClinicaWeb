package evento;

import configuracion.ConfiguracionClinicaSingleton;
import modelo.dto.CitaDTO;
import modelo.dto.ParametroClinicaDTO;
import modelo.repositorio.CitaRepository;
import modelo.repositorio.CitaRepositoryImpl;
import notificacion.CanalNotificacionCita;
import notificacion.MensajeCita;
import notificacion.NotificacionEmail;
import notificacion.NotificacionSMS;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

/**
 * PATRÓN: Observer  ->  OBSERVADOR CONCRETO "NotificadorCitas" (RF-09).
 *
 * Cuando se reserva, reprograma o cancela una cita, el servicio solo PUBLICA el
 * evento; no sabe que existen correos ni SMS. Este observador lo escucha y
 * avisa al paciente por cada canal (NotificacionEmail, NotificacionSMS).
 * Envía en SEGUNDO PLANO: la reserva responde al instante aunque el proveedor
 * de correo tarde (RNF-05), y si el correo falla, la cita igual queda guardada.
 */
public class NotificadorCitasObserver implements EventoObserver {

    private static final Set<TipoEvento> AVISAN = EnumSet.of(
            TipoEvento.CITA_RESERVADA, TipoEvento.CITA_REPROGRAMADA, TipoEvento.CITA_CANCELADA);

    private final CitaRepository citas;
    private final List<CanalNotificacionCita> canales;
    private final Supplier<ParametroClinicaDTO> parametros;
    private final Executor ejecutor;

    public NotificadorCitasObserver() {
        this(new CitaRepositoryImpl(), List.of(new NotificacionEmail(), new NotificacionSMS()),
                () -> ConfiguracionClinicaSingleton.getInstancia().getParametros(),
                tarea -> CompletableFuture.runAsync(tarea));
    }

    /** Para pruebas: repositorio y canales falsos, y un ejecutor que corre en el mismo hilo. */
    public NotificadorCitasObserver(CitaRepository citas, List<CanalNotificacionCita> canales,
                                    Supplier<ParametroClinicaDTO> parametros, Executor ejecutor) {
        this.citas = citas;
        this.canales = canales;
        this.parametros = parametros;
        this.ejecutor = ejecutor;
    }

    @Override
    public void actualizar(EventoSistema evento) {
        if (!AVISAN.contains(evento.getTipo()) || evento.getIdEntidad() == null) {
            return;
        }
        ejecutor.execute(() -> notificar(evento.getTipo(), evento.getIdEntidad()));
    }

    private void notificar(TipoEvento tipo, int idCita) {
        try {
            Optional<CitaDTO> cita = citas.buscarPorId(idCita);
            if (cita.isEmpty()) {
                return;
            }
            ParametroClinicaDTO p = parametros.get();
            MensajeCita mensaje = MensajeCita.de(tipo, cita.get(), p, p.getHorasCancelacionPaciente(), urlPortal());
            if (mensaje == null) {
                return;
            }
            for (CanalNotificacionCita canal : canales) {
                try {
                    canal.enviar(cita.get(), mensaje);
                } catch (RuntimeException e) {
                    System.err.println("NotificadorCitas: falló el " + canal.getNombre() + " -> " + e.getMessage());
                }
            }
        } catch (RuntimeException e) {
            System.err.println("NotificadorCitas: " + e.getMessage());
        }
    }

    private static String urlPortal() {
        String base = System.getenv("APP_URL");
        return base == null || base.isBlank() ? null : base.replaceAll("/+$", "") + "/paciente/citas";
    }
}
