package pruebas.citas;

import evento.EventoObserver;
import evento.EventoSistema;
import evento.EventoSubject;
import evento.NotificadorCitasObserver;
import evento.TipoEvento;
import fakes.CitasFalsas;
import modelo.dto.CitaDTO;
import modelo.dto.ParametroClinicaDTO;
import notificacion.CanalNotificacionCita;
import notificacion.MensajeCita;
import notificacion.NotificacionEmail;
import notificacion.NotificacionSMS;
import org.junit.Before;
import org.junit.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/** RF-09 y RF-21 · Observer: los avisos de la cita salen por cada canal y un canal caído no detiene a los demás. */
public class NotificadorCitasObserverTest {

    private final ParametroClinicaDTO parametros = new ParametroClinicaDTO();
    private final List<String> enviados = new ArrayList<>();
    private CitasFalsas citas;
    private NotificadorCitasObserver notificador;
    private int idCita;

    @Before
    public void preparar() {
        citas = new CitasFalsas();
        CitaDTO c = new CitaDTO();
        c.setIdPaciente(7); c.setIdMedico(1); c.setFecha(LocalDate.of(2026, 10, 29));
        c.setHoraInicio(LocalTime.of(10, 0)); c.setHoraFin(LocalTime.of(10, 30));
        idCita = citas.reservar(c, "recepcion").idCita();
        citas.citas.get(idCita).setNombreMedico("Marco Demo");
        citas.citas.get(idCita).setConsultorio("Consultorio 101");
        CanalNotificacionCita falso = new CanalNotificacionCita() {
            public String getNombre() { return "falso"; }
            public boolean enviar(CitaDTO x, MensajeCita m) { enviados.add(m.getAsunto() + "|" + m.getTextoCorto()); return true; }
        };
        CanalNotificacionCita roto = new CanalNotificacionCita() {
            public String getNombre() { return "roto"; }
            public boolean enviar(CitaDTO x, MensajeCita m) { throw new RuntimeException("proveedor caído"); }
        };
        notificador = new NotificadorCitasObserver(citas, List.of(roto, falso), () -> parametros, Runnable::run);
    }

    private void publicar(TipoEvento tipo, Integer id) {
        notificador.actualizar(new EventoSistema(tipo, 2, "recepcion", id, ""));
    }

    @Test
    public void avisaLaReservaAunqueOtroCanalFalle() {
        publicar(TipoEvento.CITA_RESERVADA, idCita);
        assertEquals(1, enviados.size());
        assertTrue(enviados.get(0).startsWith("Cita reservada"));
        assertTrue(enviados.get(0).split("\\|")[1].length() <= 160);
    }

    @Test
    public void soloAvisanLosEventosQueLeInteresanAlPaciente() {
        publicar(TipoEvento.CITA_LLEGADA, idCita);
        publicar(TipoEvento.USUARIO_CREADO, idCita);
        publicar(TipoEvento.CITA_RESERVADA, null);
        publicar(TipoEvento.CITA_RESERVADA, 999);
        assertTrue(enviados.isEmpty());
        publicar(TipoEvento.CITA_CANCELADA, idCita);
        publicar(TipoEvento.CITA_REPROGRAMADA, idCita);
        assertEquals(2, enviados.size());
        assertTrue(enviados.get(0).startsWith("Cita cancelada"));
    }

    @Test
    public void canalesSinDatosNoEnvianYElCorreoEscapaHtml() {
        CitaDTO sinDatos = new CitaDTO();
        assertFalse(new NotificacionEmail((d, n, a, h) -> true).enviar(sinDatos, null));
        sinDatos.setCelularPaciente("987 654 321");
        CitaDTO c = citas.citas.get(idCita);
        c.setNombrePaciente("<script>alert(1)</script>");
        assertTrue(new NotificacionSMS().enviar(sinDatos, MensajeCita.de(TipoEvento.CITA_RESERVADA, c, parametros, 24, null)));
        assertFalse(MensajeCita.de(TipoEvento.CITA_RESERVADA, c, parametros, 24, null).getHtml().contains("<script"));
    }

    @Test
    public void elSujetoAvisaATodosLosSuscritosYToleraFallas() {
        List<EventoSistema> recibidos = new ArrayList<>();
        EventoObserver bueno = recibidos::add;
        EventoObserver malo = e -> { throw new IllegalStateException("observador roto"); };
        EventoSubject sujeto = EventoSubject.getInstancia();
        sujeto.suscribir(malo);
        sujeto.suscribir(bueno);
        sujeto.suscribir(bueno);
        try {
            sujeto.publicar(TipoEvento.CITA_RESERVADA, null, 5, "detalle");
        } finally {
            sujeto.desuscribir(malo);
            sujeto.desuscribir(bueno);
        }
        assertEquals(1, recibidos.size());
        EventoSistema e = recibidos.get(0);
        assertEquals(TipoEvento.CITA_RESERVADA, e.getTipo());
        assertNull(e.getIdUsuario());
        assertEquals(Integer.valueOf(5), e.getIdEntidad());
        assertNotNull(e.getFecha());
        assertEquals("detalle", e.getDetalle());
        assertEquals("sistema", e.getNombreUsuario());
        assertFalse(TipoEvento.describir("CITA_RESERVADA").isBlank());
        for (TipoEvento t : TipoEvento.values()) {
            assertFalse(t.name(), t.getDescripcion().isBlank());
        }
    }
}
