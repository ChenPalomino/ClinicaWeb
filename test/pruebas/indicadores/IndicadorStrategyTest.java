package pruebas.indicadores;

import modelo.dto.IndicadorDTO;
import modelo.dto.ParValorDTO;
import modelo.indicadores.IndicadorFactory;
import modelo.indicadores.IndicadorStrategy;
import modelo.indicadores.Periodo;
import modelo.repositorio.IndicadorRepository;
import modelo.servicios.IndicadorService;
import org.junit.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.Assert.*;

/** RF-18, RF-19 y PP-05 · Indicadores del administrador (Strategy + Factory) calculados con datos en memoria. */
public class IndicadorStrategyTest {

    private static final LocalDate HOY = LocalDate.of(2026, 10, 26);

    private static ParValorDTO par(String etiqueta, String v, String v2) {
        return new ParValorDTO(etiqueta, new BigDecimal(v), v2 == null ? null : new BigDecimal(v2));
    }

    /** Repositorio en memoria con datos conocidos; "vacio" simula un periodo sin movimientos. */
    static class DatosFalsos implements IndicadorRepository {
        boolean vacio;
        List<ParValorDTO> o(List<ParValorDTO> l) { return vacio ? List.of() : l; }
        public List<ParValorDTO> ingresosPorDia(LocalDate d, LocalDate h) { return o(List.of(par("2026-10-20", "100.00", "2"), par("2026-10-26", "60.00", "1"))); }
        public List<ParValorDTO> ingresosPorMetodo(LocalDate d, LocalDate h) { return o(List.of(par("Efectivo", "120", "2"), par("Billetera digital", "40", "1"))); }
        public List<ParValorDTO> ingresosPorCobertura(LocalDate d, LocalDate h) { return o(List.of(par("Particular", "80", "0"), par("EPS Pacífico", "24", "56"))); }
        public List<ParValorDTO> masVendidos(LocalDate d, LocalDate h, int l) { return o(List.of(par("Consulta", "160", "2"), par("Hemograma", "35", "1"))); }
        public List<ParValorDTO> citasPorEstado(LocalDate d, LocalDate h) { return o(List.of(par("ATENDIDA", "8", null), par("NO_ASISTIO", "2", null), par("CANCELADA", "2", null), par("RESERVADA", "1", null), par("CONFIRMADA", "1", null), par("OTRO", "0", null))); }
        public List<ParValorDTO> citasPorEspecialidad(LocalDate d, LocalDate h) { return o(List.of(par("Medicina General", "10", "7"), par("Pediatría", "4", "1"))); }
        public List<ParValorDTO> atencionesPorMedico(LocalDate d, LocalDate h, int l) { return o(List.of(par("Demo, Marco", "7", "1"))); }
        public List<ParValorDTO> citasPorHora(LocalDate d, LocalDate h) { return o(List.of(par("08", "3", null), par("10", "6", null), par("15", "2", null))); }
        public List<ParValorDTO> citasPorCanal(LocalDate d, LocalDate h) { return o(List.of(par("RECEPCION", "6", null), par("PORTAL", "4", null))); }
        public List<ParValorDTO> usuariosNuevosPorDia(LocalDate d, LocalDate h) { return o(List.of(par("2026-10-25", "3", null))); }
        public List<ParValorDTO> usuariosActivosPorRol() { return o(List.of(par("Administrador", "1", "0"), par("Paciente", "20", "2"))); }
        public List<ParValorDTO> stockBajo(int l) { return o(List.of(par("Paracetamol", "2", "10"))); }
    }

    private final DatosFalsos datos = new DatosFalsos();
    private final IndicadorService servicio = new IndicadorService(datos);

    private IndicadorDTO calcular(String codigo, String periodo) {
        return servicio.calcular(codigo, periodo, HOY).orElseThrow();
    }

    private static String dato(IndicadorDTO r, String etiqueta) {
        return r.getResumen().stream().filter(d -> d.etiqueta().equals(etiqueta)).findFirst().orElseThrow().valor();
    }

    @Test
    public void elCatalogoTieneDoceIndicadoresConCodigoUnico() {
        List<IndicadorStrategy> todos = servicio.catalogo();
        assertEquals(12, todos.size());
        assertEquals(12, todos.stream().map(IndicadorStrategy::codigo).distinct().count());
        for (IndicadorStrategy i : todos) {
            assertFalse(i.titulo().isBlank());
            assertFalse(i.descripcion().isBlank());
            assertTrue(i.icono().startsWith("fas "));
            assertFalse(i.grupo().isBlank());
        }
        assertTrue(IndicadorFactory.obtener("NO_EXISTE").isEmpty());
        assertTrue(IndicadorFactory.obtener(null).isEmpty());
        assertTrue(servicio.calcular("NO_EXISTE", "30", HOY).isEmpty());
    }

    @Test
    public void periodosDisponibles() {
        assertEquals(new Periodo(HOY.minusDays(29), HOY), Periodo.de("30", HOY));
        assertEquals(7, Periodo.de("7", HOY).dias());
        assertEquals(LocalDate.of(2026, 10, 1), Periodo.de("mes", HOY).desde());
        assertEquals(new Periodo(LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30)), Periodo.de("mesAnterior", HOY));
        assertEquals(30, Periodo.de("abc", HOY).dias());
        assertEquals(30, Periodo.de(null, HOY).dias());
        assertEquals(365, Periodo.de("9999", HOY).dias());
        assertEquals(1, Periodo.de("0", HOY).dias());
        assertEquals("26/10/2026", Periodo.de("1", HOY).texto());
        assertEquals("20/10/2026 al 26/10/2026", Periodo.de("7", HOY).texto());
        assertEquals("Estado actual", IndicadorService.textoPeriodo("STOCK_BAJO", "30", HOY));
        assertEquals("20/10/2026 al 26/10/2026", IndicadorService.textoPeriodo("INGRESOS_DIA", "7", HOY));
    }

    @Test
    public void ingresosPorDiaRellenaLosDiasSinCobrosYAgrupaPorMes() {
        IndicadorDTO r = calcular("INGRESOS_DIA", "7");
        assertEquals(IndicadorDTO.LINEA, r.getTipoGrafico());
        assertEquals(7, r.getEtiquetas().size());
        assertEquals("20/10", r.getEtiquetas().get(0));
        assertEquals(0, r.getSeries().get(0).valores().get(1).signum());
        assertEquals("S/ 160.00", dato(r, "Total cobrado"));
        assertEquals("3", dato(r, "Pagos"));
        assertEquals("S/ 53.33", dato(r, "Ticket promedio"));
        IndicadorDTO anual = calcular("INGRESOS_DIA", "365");
        assertTrue(anual.getEtiquetas().size() <= 13);
        assertFalse(anual.getNota().isBlank());
        assertFalse(anual.isVacio());
    }

    @Test
    public void ingresosPorMetodoYCobertura() {
        IndicadorDTO m = calcular("INGRESOS_METODO", "30");
        assertEquals(IndicadorDTO.DONA, m.getTipoGrafico());
        assertEquals("75 %", dato(m, "Efectivo"));
        IndicadorDTO c = calcular("INGRESOS_COBERTURA", "30");
        assertEquals(2, c.getSeries().size());
        assertEquals("S/ 56.00", dato(c, "Cubierto por terceros"));
        assertEquals("76.9 %", dato(c, "Particulares"));
    }

    @Test
    public void masVendidosYDemanda() {
        IndicadorDTO v = calcular("MAS_VENDIDOS", "30");
        assertEquals(IndicadorDTO.BARRAS_HORIZONTALES, v.getTipoGrafico());
        assertEquals("Consulta", dato(v, "El primero"));
        assertEquals("/admin/servicios", v.getEnlace());
        IndicadorDTO e = calcular("CITAS_ESPECIALIDAD", "30");
        assertEquals("Medicina General", dato(e, "Más pedida"));
        IndicadorDTO a = calcular("ATENCIONES_MEDICO", "30");
        assertEquals("1", dato(a, "Inasistencias"));
    }

    @Test
    public void asistenciaACitasHorasPicoYCanal() {
        IndicadorDTO e = calcular("CITAS_ESTADO", "30");
        assertEquals("80 %", dato(e, "Asistencia"));
        assertEquals("14.3 %", dato(e, "Cancelación"));
        assertTrue(e.getEtiquetas().contains("No asistieron"));
        assertTrue(e.getEtiquetas().contains("OTRO"));
        IndicadorDTO h = calcular("HORAS_PICO", "30");
        assertEquals("10:00", dato(h, "Hora pico"));
        IndicadorDTO c = calcular("CANAL_RESERVA", "30");
        assertEquals("40 %", dato(c, "Desde el portal"));
        assertTrue(c.getEtiquetas().contains("Portal del paciente"));
    }

    @Test
    public void usuariosEInventario() {
        assertEquals("3", dato(calcular("USUARIOS_NUEVOS", "30"), "Nuevos"));
        IndicadorDTO r = calcular("USUARIOS_ROL", "30");
        assertEquals("21", dato(r, "Activos"));
        assertEquals("2", dato(r, "Bloqueados"));
        IndicadorDTO s = calcular("STOCK_BAJO", "30");
        assertEquals("1", dato(s, "Por reponer"));
        assertTrue(s.getNota().isBlank());
    }

    @Test
    public void periodoSinDatosSeMarcaVacio() {
        datos.vacio = true;
        for (IndicadorStrategy i : servicio.catalogo()) {
            IndicadorDTO r = calcular(i.codigo(), "7");
            assertTrue(i.codigo(), r.isVacio());
        }
        assertEquals("—", dato(calcular("CITAS_ESTADO", "7"), "Asistencia"));
        assertFalse(calcular("STOCK_BAJO", "7").getNota().isBlank());
    }
}
