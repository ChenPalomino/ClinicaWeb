package pruebas.servicios;

import evento.EventoObserver;
import evento.EventoSistema;
import evento.EventoSubject;
import evento.TipoEvento;
import fakes.CitasFalsas;
import fakes.CuentasFalsas;
import fakes.MedicosFalsos;
import fakes.PacientesFalsos;
import fakes.PagosFalsos;
import modelo.dto.*;
import modelo.entidades.Usuario;
import modelo.servicios.CitaService;
import modelo.servicios.CobranzaService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static fakes.Datos.*;
import static org.junit.Assert.*;

/** RF-04 a RF-10 · Servicio de citas con repositorios en memoria y reloj fijo (martes 20/10/2026 10:15). */
public class CitaServiceTest {

    private final LocalDateTime[] ahora = {LocalDateTime.of(2026, 10, 20, 10, 15)};
    private final LocalDate hoy = ahora[0].toLocalDate();
    private final LocalDate miercoles = hoy.plusDays(1);
    private final LocalDate domingo = LocalDate.of(2026, 10, 25);
    private final Usuario recepcion = usuario("recepcion", 2, 2);
    private final Usuario paciente = usuario("ana", 40, 4);
    private final Usuario otroPaciente = usuario("luis", 41, 4);
    private final Usuario medico1 = usuario("medico", 30, 3);
    private final Usuario medico2 = usuario("medico2", 31, 3);
    private final List<EventoSistema> eventos = new ArrayList<>();
    private final EventoObserver captura = eventos::add;
    private CitasFalsas citas;
    private CuentasFalsas cuentas;
    private CitaService servicio;

    @Before
    public void preparar() {
        MedicosFalsos medicos = new MedicosFalsos();
        medicos.lista.add(medico(1, 30, 1, 30));
        medicos.lista.add(medico(2, 31, 1, 20));
        MedicoDTO pausado = medico(3, 32, 1, 30);
        pausado.setEstado("Inactivo");
        medicos.lista.add(pausado);
        citas = new CitasFalsas();
        cuentas = new CuentasFalsas();
        cuentas.items.add(item(1, "Consulta de Medicina General", "SERVICIO", "80.00", null, true));
        PagosFalsos pagos = new PagosFalsos();
        pagos.cuentas = cuentas;
        ParametroClinicaDTO parametros = new ParametroClinicaDTO();
        CobranzaService caja = new CobranzaService(cuentas, pagos, () -> parametros, () -> hoy);
        servicio = new CitaService(citas, medicos, new PacientesFalsos(), () -> parametros, () -> ahora[0], () -> caja);
        EventoSubject.getInstancia().suscribir(captura);
    }

    @After
    public void limpiar() {
        EventoSubject.getInstancia().desuscribir(captura);
    }

    private CitaService.Resultado reservar(LocalDate fecha, int medico, int hora, Usuario quien) {
        return servicio.reservar(7, medico, fecha, LocalTime.of(hora, 0), "", quien);
    }

    @Test
    public void disponibilidadPorDiaYTurnos() {
        List<DiaDisponibleDTO> dias = servicio.dias(1, null, hoy);
        assertEquals(14, dias.size());
        assertEquals(hoy, dias.get(0).getFecha());
        assertTrue(dias.stream().filter(d -> d.getFecha().equals(domingo)).allMatch(d -> !d.isAtiende() && d.getLibres() == 0));
        assertTrue(dias.stream().filter(d -> d.getFecha().equals(miercoles)).allMatch(d -> d.getLibres() == 45));
        assertEquals(2, servicio.turnos(1, null, miercoles).size());
        assertEquals(1, servicio.turnos(1, 2, miercoles).size());
        assertTrue(servicio.turnos(1, null, hoy.minusDays(1)).isEmpty());
        assertEquals(2, servicio.medicos(1).size());
        assertFalse(servicio.especialidades().isEmpty());
    }

    @Test
    public void recepcionReservaYSePublicaElEvento() {
        CitaService.Resultado r = servicio.reservar(7, 1, miercoles, LocalTime.of(9, 0), "Dolor de cabeza", recepcion);
        assertTrue(r.isExito());
        assertEquals("CT-000001", r.codigo());
        assertTrue(eventos.stream().anyMatch(e -> e.getTipo() == TipoEvento.CITA_RESERVADA && e.getIdEntidad() == r.idCita()));
        CitaDTO c = citas.citas.get(r.idCita());
        assertEquals("RECEPCION", c.getCanal());
        assertEquals(LocalTime.of(9, 30), c.getHoraFin());
    }

    @Test
    public void validacionesDeLaReserva() {
        reservar(miercoles, 1, 9, recepcion);
        assertFalse(reservar(miercoles, 1, 9, recepcion).isExito());
        assertTrue(reservar(miercoles, 2, 9, recepcion).error().contains("ya tiene una cita"));
        assertFalse(reservar(miercoles, 1, 16, recepcion).isExito());
        assertFalse(servicio.reservar(7, 2, miercoles, LocalTime.of(9, 10), "", recepcion).isExito());
        assertFalse(reservar(domingo, 2, 9, recepcion).isExito());
        assertTrue(reservar(hoy.minusDays(1), 2, 9, recepcion).error().contains("pasó"));
        assertTrue(reservar(hoy.plusDays(61), 2, 9, recepcion).error().contains("hasta el"));
        assertFalse(reservar(hoy, 2, 8, recepcion).isExito());
        assertTrue(reservar(miercoles, 3, 10, recepcion).error().contains("no está recibiendo"));
        assertFalse(servicio.reservar(8, 2, miercoles, LocalTime.of(10, 0), "", recepcion).isExito());
        assertFalse(servicio.reservar(7, 2, miercoles, LocalTime.of(11, 0), "x".repeat(251), recepcion).isExito());
        assertFalse(reservar(miercoles, 2, 11, medico1).isExito());
        assertTrue(reservar(miercoles, 2, 11, otroPaciente).error().contains("para ti"));
    }

    @Test
    public void carreraPorElMismoTurnoSeInformaSinError() {
        citas.simularCarrera = true;
        CitaService.Resultado r = reservar(hoy.plusDays(2), 2, 11, recepcion);
        assertFalse(r.isExito());
        assertTrue(r.error().contains("otra persona"));
    }

    @Test
    public void portalPermiteHastaTresPendientes() {
        CitaService.Resultado p1 = reservar(hoy.plusDays(2), 2, 9, paciente);
        assertTrue(p1.isExito());
        assertTrue(citas.citas.get(p1.idCita()).isDelPortal());
        assertTrue(reservar(hoy.plusDays(3), 2, 9, paciente).isExito());
        assertTrue(reservar(hoy.plusDays(6), 2, 10, paciente).isExito());
        assertTrue(reservar(hoy.plusDays(7), 2, 9, paciente).error().contains("3 citas pendientes"));
        assertTrue(reservar(hoy.plusDays(7), 2, 9, recepcion).isExito());
    }

    @Test
    public void reprogramacionDelPacienteYDeRecepcion() {
        CitaService.Resultado manana = reservar(miercoles, 1, 9, recepcion);
        CitaService.Resultado p1 = reservar(hoy.plusDays(2), 2, 9, paciente);
        assertTrue(servicio.reprogramar(manana.idCita(), 1, hoy.plusDays(7), LocalTime.of(9, 0), paciente).contains("llama a recepción"));
        assertNotNull(servicio.reprogramar(p1.idCita(), 2, hoy.plusDays(7), LocalTime.of(10, 0), otroPaciente));
        assertTrue(servicio.reprogramar(p1.idCita(), 2, hoy.plusDays(2), LocalTime.of(9, 0), paciente).contains("mismo horario"));
        assertNull(servicio.reprogramar(p1.idCita(), 2, hoy.plusDays(7), LocalTime.of(10, 0), paciente));
        assertEquals(hoy.plusDays(7), citas.citas.get(p1.idCita()).getFecha());
        assertTrue(eventos.stream().anyMatch(e -> e.getTipo() == TipoEvento.CITA_REPROGRAMADA));
        servicio.reprogramar(p1.idCita(), 2, hoy.plusDays(8), LocalTime.of(10, 0), paciente);
        assertTrue(servicio.reprogramar(p1.idCita(), 2, hoy.plusDays(9), LocalTime.of(10, 0), paciente).contains("recepción"));
        assertNull(servicio.reprogramar(p1.idCita(), 1, hoy.plusDays(9), LocalTime.of(10, 0), recepcion));
        assertEquals(1, citas.citas.get(p1.idCita()).getIdMedico());
        assertNull(servicio.reprogramar(manana.idCita(), 1, miercoles, LocalTime.of(10, 0), recepcion));
        assertNotNull(servicio.reprogramar(999, 1, miercoles, LocalTime.of(11, 0), recepcion));
    }

    @Test
    public void cancelacionConMotivo() {
        CitaService.Resultado manana = reservar(miercoles, 1, 9, recepcion);
        CitaService.Resultado p2 = reservar(hoy.plusDays(3), 2, 9, paciente);
        assertNotNull(servicio.cancelar(manana.idCita(), "Tengo otro compromiso", paciente));
        assertTrue(servicio.cancelar(p2.idCita(), "x", paciente).contains("motivo"));
        assertNull(servicio.cancelar(p2.idCita(), "Tengo otro compromiso", paciente));
        assertTrue(citas.citas.get(p2.idCita()).isCancelada());
        assertNotNull(servicio.cancelar(p2.idCita(), "Tengo otro compromiso", recepcion));
        assertNotNull(servicio.cancelar(manana.idCita(), "Emergencia en quirófano", medico1));
        assertTrue(eventos.stream().anyMatch(e -> e.getTipo() == TipoEvento.CITA_CANCELADA && e.getDetalle().contains("Tengo otro compromiso")));
        assertTrue(reservar(hoy.plusDays(3), 2, 9, recepcion).isExito());
    }

    @Test
    public void llegadaYAtencionDelDia() {
        CitaService.Resultado manana = reservar(miercoles, 1, 10, recepcion);
        CitaService.Resultado deHoy = reservar(hoy, 2, 11, recepcion);
        assertNotNull(servicio.registrarLlegada(deHoy.idCita(), paciente));
        assertTrue(servicio.registrarLlegada(manana.idCita(), recepcion).contains("día de la cita"));
        assertTrue(servicio.registrarAtencion(deHoy.idCita(), false, "", medico2).contains("pasó la hora"));
        assertNull(servicio.registrarLlegada(deHoy.idCita(), recepcion));
        assertTrue(citas.citas.get(deHoy.idCita()).isConfirmada());
        assertNotNull(servicio.registrarAtencion(deHoy.idCita(), true, "", medico1));
        assertNotNull(servicio.registrarAtencion(deHoy.idCita(), true, "x".repeat(501), medico2));
        assertNull(servicio.registrarAtencion(deHoy.idCita(), true, "Control en 1 mes", medico2));
        assertTrue(citas.citas.get(deHoy.idCita()).isAtendida());
        assertNotNull(servicio.cancelar(deHoy.idCita(), "Error de registro", recepcion));
        ahora[0] = LocalDateTime.of(2026, 10, 21, 12, 0);
        assertNull(servicio.registrarAtencion(manana.idCita(), false, "", medico1));
        assertTrue(citas.citas.get(manana.idCita()).isNoAsistio());
    }

    @Test
    public void quienPuedeVerUnaCita() {
        CitaDTO c = citas.citas.get(reservar(hoy.plusDays(2), 1, 9, paciente).idCita());
        assertTrue(CitaService.puedeVer(c, paciente));
        assertFalse(CitaService.puedeVer(c, otroPaciente));
        assertTrue(CitaService.puedeVer(c, recepcion));
        assertTrue(CitaService.puedeVer(c, medico1));
        assertFalse(CitaService.puedeVer(c, medico2));
        assertFalse(CitaService.puedeVer(c, null));
    }

    @Test
    public void resumenYConsultasDelPaciente() {
        reservar(hoy, 2, 11, recepcion);
        reservar(miercoles, 1, 9, recepcion);
        FiltroCitaDTO f = new FiltroCitaDTO(hoy, miercoles, null, null, null, null);
        assertEquals(2, servicio.resumen(f).getProgramadas());
        assertEquals(2, servicio.resumenPorDia(f).size());
        assertEquals(2, servicio.agenda(f).size());
        assertEquals(2, servicio.proximasDelPaciente(7, 5).size());
        assertNotNull(servicio.historialDelPaciente(7, 5));
        assertEquals(Integer.valueOf(7), servicio.pacienteDeUsuario(40).get());
        assertTrue(servicio.paciente(7).isPresent());
        assertTrue(servicio.medicoDeUsuario(30).isPresent());
        assertEquals(24, servicio.horasCancelacion());
        assertTrue(servicio.ultimoDiaReserva().isAfter(hoy));
        assertEquals(hoy, servicio.hoy());
    }

    @Test
    public void preciosDeLaConsultaParaElPaciente() {
        assertTrue(igual(servicio.preciosConsulta(7).get(1).getAPagar(), "80.00"));
        assertTrue(igual(servicio.preciosConsulta(null).get(1).getAPagar(), "80.00"));
    }

    @Test
    public void cobrarUnaCitaAbreLaCuentaConLaConsulta() {
        CitaService.Resultado r = reservar(hoy, 2, 11, recepcion);
        assertFalse(servicio.cobrar(r.idCita(), medico1).isExito());
        CitaService.Cobro cobro = servicio.cobrar(r.idCita(), recepcion);
        assertTrue(cobro.error(), cobro.isExito());
        assertEquals(1, cuentas.cuentas.get(cobro.idCuenta()).getDetalles().size());
        assertEquals(cobro.idCuenta(), servicio.cobrar(r.idCita(), recepcion).idCuenta());
        assertFalse(servicio.cobrar(999, recepcion).isExito());
    }

    @Test
    public void pagoEnLineaSoloDelPacienteDuenoYCitaActiva() {
        CitaService.Resultado r = reservar(hoy.plusDays(2), 1, 9, paciente);
        assertFalse(servicio.prepararPagoEnLinea(r.idCita(), otroPaciente).isExito());
        assertFalse(servicio.prepararPagoEnLinea(r.idCita(), recepcion).isExito());
        assertTrue(servicio.prepararPagoEnLinea(r.idCita(), paciente).isExito());
        servicio.cancelar(r.idCita(), "Tengo otro compromiso", recepcion);
        assertFalse(servicio.prepararPagoEnLinea(r.idCita(), paciente).isExito());
    }
}
