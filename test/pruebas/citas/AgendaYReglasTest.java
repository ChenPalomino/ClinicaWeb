package pruebas.citas;

import modelo.citas.CalculadoraTurnos;
import modelo.citas.FechaTexto;
import modelo.citas.ReglasCita;
import modelo.citas.vista.VistaAgendaFactory;
import modelo.citas.vista.VistaAgendaStrategy;
import modelo.citas.vista.VistaDiaStrategy;
import modelo.dto.CitaDTO;
import modelo.dto.HorarioDTO;
import modelo.dto.MedicoDTO;
import modelo.dto.TurnoDTO;
import org.junit.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import static fakes.Datos.medico;
import static org.junit.Assert.*;

/** RF-03, RF-04 y RF-18 · Turnos disponibles, reglas de la cita y vistas de la agenda (Strategy). */
public class AgendaYReglasTest {

    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 10, 20, 10, 15);
    private static final LocalDate HOY = AHORA.toLocalDate();
    private static final LocalDate MIERCOLES = HOY.plusDays(1);
    private static final LocalDate SABADO = LocalDate.of(2026, 10, 24);
    private static final LocalDate DOMINGO = LocalDate.of(2026, 10, 25);
    private final MedicoDTO m1 = medico(1, 30, 1, 30);

    @Test
    public void turnosDeUnDiaConDosBloques() {
        List<TurnoDTO> t = CalculadoraTurnos.turnos(m1, MIERCOLES, Set.of(), AHORA);
        assertEquals(18, t.size());
        assertEquals(LocalTime.of(8, 0), t.get(0).getHoraInicio());
        assertEquals(LocalTime.of(8, 30), t.get(0).getHoraFin());
        assertEquals(LocalTime.of(18, 30), t.get(17).getHoraInicio());
        assertTrue(t.stream().noneMatch(x -> x.getHoraInicio().equals(LocalTime.of(13, 0))));
    }

    @Test
    public void hoyNoOfreceTurnosPasadosYDomingoNoAtiende() {
        assertEquals(LocalTime.of(10, 30), CalculadoraTurnos.turnos(m1, HOY, Set.of(), AHORA).get(0).getHoraInicio());
        assertTrue(CalculadoraTurnos.turnos(m1, DOMINGO, Set.of(), AHORA).isEmpty());
        assertEquals(8, CalculadoraTurnos.turnos(m1, SABADO, Set.of(), AHORA).size());
    }

    @Test
    public void turnoOcupadoNoEstaLibre() {
        Set<LocalTime> ocupados = Set.of(LocalTime.of(9, 0));
        assertTrue(CalculadoraTurnos.turnos(m1, MIERCOLES, ocupados, AHORA).stream()
                .filter(x -> x.getHoraInicio().equals(LocalTime.of(9, 0))).noneMatch(TurnoDTO::isLibre));
        assertEquals(17, CalculadoraTurnos.libres(m1, MIERCOLES, ocupados, AHORA).size());
    }

    @Test
    public void duracionesYHorariosEspeciales() {
        assertEquals(6, CalculadoraTurnos.turnos(medico(9, 99, 1, 45), MIERCOLES, Set.of(), AHORA).stream().filter(TurnoDTO::isManiana).count());
        assertTrue(CalculadoraTurnos.turnoDelHorario(m1, MIERCOLES, LocalTime.of(8, 15), AHORA).isEmpty());
        assertTrue(CalculadoraTurnos.turnoDelHorario(m1, MIERCOLES, LocalTime.of(8, 30), AHORA).isPresent());
        assertTrue(CalculadoraTurnos.dentroDelHorario(m1, MIERCOLES, LocalTime.of(12, 30), LocalTime.of(13, 0)));
        assertFalse(CalculadoraTurnos.dentroDelHorario(m1, MIERCOLES, LocalTime.of(13, 0), LocalTime.of(13, 30)));
        MedicoDTO noche = medico(10, 98, 1, 60);
        noche.getHorarios().clear();
        noche.getHorarios().add(new HorarioDTO(3, LocalTime.of(21, 0), LocalTime.of(23, 59)));
        assertEquals(2, CalculadoraTurnos.turnos(noche, MIERCOLES, Set.of(), AHORA).size());
    }

    @Test
    public void vistasDiaSemanaYMes() {
        VistaAgendaStrategy dia = VistaAgendaFactory.obtener("dia");
        VistaAgendaStrategy sem = VistaAgendaFactory.obtener("semana");
        VistaAgendaStrategy mes = VistaAgendaFactory.obtener("mes");
        assertTrue(VistaAgendaFactory.obtener("xyz") instanceof VistaDiaStrategy);
        assertTrue(VistaAgendaFactory.obtener(null) instanceof VistaDiaStrategy);
        assertEquals(3, VistaAgendaFactory.todas().size());
        assertEquals(LocalDate.of(2026, 10, 19), sem.desde(HOY));
        assertEquals(DOMINGO, sem.hasta(HOY));
        assertEquals(LocalDate.of(2026, 10, 1), mes.desde(HOY));
        assertEquals(LocalDate.of(2026, 10, 31), mes.hasta(HOY));
        assertEquals(HOY, dia.desde(HOY));
        assertEquals(HOY, dia.hasta(HOY));
        assertEquals(MIERCOLES, dia.siguiente(HOY));
        assertEquals(HOY.minusDays(1), dia.anterior(HOY));
        assertEquals(HOY.plusDays(7), sem.siguiente(HOY));
        assertEquals(HOY.minusDays(7), sem.anterior(HOY));
        assertEquals(LocalDate.of(2026, 11, 1), mes.siguiente(HOY));
        assertEquals(LocalDate.of(2026, 9, 1), mes.anterior(HOY));
        assertEquals("Día", dia.getNombre());
        assertEquals("Semana", sem.getNombre());
        assertEquals("Mes", mes.getNombre());
    }

    @Test
    public void titulosDeLasVistas() {
        assertEquals("Martes 20 de octubre de 2026", VistaAgendaFactory.obtener("dia").titulo(HOY));
        assertEquals("Semana del 19 al 25 de octubre de 2026", VistaAgendaFactory.obtener("semana").titulo(HOY));
        assertEquals("Semana del 26 de octubre al 1 de noviembre de 2026", VistaAgendaFactory.obtener("semana").titulo(LocalDate.of(2026, 10, 28)));
        assertEquals("Octubre de 2026", VistaAgendaFactory.obtener("mes").titulo(HOY));
    }

    @Test
    public void textosDeFechaYHora() {
        assertEquals("Hoy", FechaTexto.relativa(HOY, HOY));
        assertEquals("Mañana", FechaTexto.relativa(MIERCOLES, HOY));
        assertEquals("Ayer", FechaTexto.relativa(HOY.minusDays(1), HOY));
        assertEquals("Jueves 22 de octubre", FechaTexto.relativa(HOY.plusDays(2), HOY));
        assertEquals("9:30 a. m.", FechaTexto.hora(LocalTime.of(9, 30)));
        assertEquals("3:00 p. m.", FechaTexto.hora(LocalTime.of(15, 0)));
        assertEquals("12:00 p. m.", FechaTexto.hora(LocalTime.of(12, 0)));
        assertFalse(FechaTexto.relativaCompleta(HOY.plusDays(3), HOY).isBlank());
        assertFalse(FechaTexto.mesCorto(HOY).isBlank());
        assertFalse(FechaTexto.diaCorto(HOY).isBlank());
        assertEquals("Octubre de 2026", FechaTexto.mesAnio(HOY));
        assertEquals("20/10/2026", FechaTexto.dmy(HOY));
        assertEquals("09:30", FechaTexto.horaIso(LocalTime.of(9, 30)));
        assertFalse(FechaTexto.paraFrase(HOY.plusDays(5), HOY).isBlank());
        assertEquals("", FechaTexto.larga(null));
    }

    private static CitaDTO cita(LocalDate fecha, int hora) {
        CitaDTO c = new CitaDTO();
        c.setFecha(fecha); c.setHoraInicio(LocalTime.of(hora, 0)); c.setHoraFin(LocalTime.of(hora, 30));
        return c;
    }

    @Test
    public void elPacienteSoloCambiaConLaAnticipacionMinima() {
        CitaDTO c = cita(MIERCOLES, 9);
        assertFalse(ReglasCita.pacientePuedeCambiar(c, AHORA, 24));
        assertTrue(ReglasCita.pacientePuedeCambiar(c, AHORA, 2));
        assertTrue(ReglasCita.motivoBloqueoPaciente(c, AHORA, 24).contains("recepción"));
        c.setFecha(HOY.plusDays(3));
        assertTrue(ReglasCita.pacientePuedeCambiar(c, AHORA, 24));
        assertNull(ReglasCita.motivoBloqueoPaciente(c, AHORA, 24));
        assertTrue(ReglasCita.pacientePuedeCambiar(c, c.getInicio().minusHours(24), 24));
    }

    @Test
    public void limiteDeReprogramacionesYLlegada() {
        CitaDTO c = cita(HOY.plusDays(3), 9);
        c.setReprogramaciones(2);
        assertFalse(ReglasCita.pacientePuedeReprogramar(c, AHORA, 24));
        assertTrue(ReglasCita.pacientePuedeCambiar(c, AHORA, 24));
        assertTrue(ReglasCita.horasParaLaCita(c, AHORA) > 24);
        c.setEstadoCita(CitaDTO.CONFIRMADA);
        assertFalse(ReglasCita.pacientePuedeCambiar(c, AHORA, 24));
    }

    @Test
    public void reglasDelPersonal() {
        CitaDTO hoy = cita(HOY, 9);
        CitaDTO futura = cita(HOY.plusDays(3), 9);
        assertTrue(ReglasCita.puedeRegistrarLlegada(hoy, HOY));
        assertFalse(ReglasCita.puedeRegistrarLlegada(futura, HOY));
        assertTrue(ReglasCita.puedeMarcarNoAsistio(hoy, AHORA));
        assertFalse(ReglasCita.puedeMarcarNoAsistio(hoy, LocalDateTime.of(HOY, LocalTime.of(8, 0))));
        assertTrue(ReglasCita.puedeMarcarAtendida(hoy, HOY));
        assertFalse(ReglasCita.puedeMarcarAtendida(futura, HOY));
        assertTrue(ReglasCita.puedeCobrar(hoy));
        assertTrue(ReglasCita.personalPuedeCancelar(hoy, HOY));
        assertTrue(ReglasCita.personalPuedeReprogramar(futura, AHORA));
        hoy.setEstadoCita(CitaDTO.CANCELADA);
        assertFalse(ReglasCita.puedeCobrar(hoy));
        assertFalse(ReglasCita.personalPuedeCancelar(hoy, HOY));
    }
}
