package pruebas.servicios;

import fakes.CitasFalsas;
import fakes.CuentasFalsas;
import fakes.MedicosFalsos;
import fakes.PacientesFalsos;
import fakes.PagosFalsos;
import fakes.ReportesFalsos;
import modelo.cobranza.pago.SolicitudPago;
import modelo.dto.ParametroClinicaDTO;
import modelo.dto.ReportePagoDTO;
import modelo.entidades.Usuario;
import modelo.servicios.CitaService;
import modelo.servicios.CobranzaService;
import modelo.servicios.ReportePagoService;
import org.junit.Before;
import org.junit.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;

import static fakes.Datos.*;
import static org.junit.Assert.*;

/** RF-13 · Pago en línea: el paciente informa el pago con su billetera y caja lo verifica o lo rechaza. */
public class ReportePagoServiceTest {

    private final LocalDateTime ahora = LocalDateTime.of(2026, 10, 26, 9, 0);
    private final Usuario paciente = usuario("ana", 40, 4);
    private final Usuario otroPaciente = usuario("luis", 41, 4);
    private final Usuario cajero = usuario("recepcion", 2, 2);
    private CitasFalsas citas;
    private CuentasFalsas cuentas;
    private PagosFalsos pagos;
    private ReportesFalsos reportes;
    private CitaService citaService;
    private ReportePagoService servicio;
    private int idCita;

    @Before
    public void preparar() {
        MedicosFalsos medicos = new MedicosFalsos();
        medicos.lista.add(medico(1, 30, 1, 30));
        citas = new CitasFalsas();
        cuentas = new CuentasFalsas();
        cuentas.items.add(item(1, "Consulta de Medicina General", "SERVICIO", "80.00", null, true));
        pagos = new PagosFalsos();
        pagos.cuentas = cuentas;
        reportes = new ReportesFalsos();
        reportes.cuentas = cuentas;
        ParametroClinicaDTO parametros = new ParametroClinicaDTO();
        CobranzaService caja = new CobranzaService(cuentas, pagos, () -> parametros, ahora::toLocalDate);
        citaService = new CitaService(citas, medicos, new PacientesFalsos(), () -> parametros, () -> ahora, () -> caja);
        servicio = new ReportePagoService(reportes, () -> caja, () -> citaService);
        idCita = citaService.reservar(7, 1, LocalDate.of(2026, 10, 28), LocalTime.of(9, 0), "", paciente).idCita();
    }

    private static SolicitudPago datos(Integer billetera, String operacion, String celular) {
        SolicitudPago s = new SolicitudPago();
        s.setIdBilletera(billetera); s.setNumeroOperacion(operacion); s.setCelular(celular); s.setNombrePagador("Ana T.");
        return s;
    }

    private ReportePagoDTO informado() {
        assertTrue(servicio.informar(idCita, datos(1, "4521", ""), paciente).isEmpty());
        ReportePagoDTO r = reportes.reportes.values().iterator().next();
        r.setDniPaciente("10000004");
        r.setNombrePaciente("Ana Torres");
        return r;
    }

    @Test
    public void prepararMuestraElMontoDeLaCita() {
        ReportePagoService.Preparacion p = servicio.preparar(idCita, paciente);
        assertTrue(p.isExito());
        assertTrue(igual(p.cuenta().getTotal(), "80.00"));
        assertNull(p.ultimoReporte());
        assertFalse(servicio.preparar(idCita, otroPaciente).isExito());
        assertFalse(servicio.preparar(999, paciente).isExito());
    }

    @Test
    public void informarValidaLosDatosDeLaApp() {
        Map<String, String> e = servicio.informar(idCita, datos(null, "", ""), paciente);
        assertTrue(e.containsKey("billetera"));
        assertTrue(e.containsKey("numeroOperacion"));
        assertTrue(servicio.informar(idCita, datos(1, "12", ""), paciente).containsKey("numeroOperacion"));
        assertTrue(servicio.informar(999, datos(1, "4521", ""), paciente).containsKey("general"));
    }

    @Test
    public void unSoloAvisoPendientePorCuenta() {
        informado();
        assertEquals(1, servicio.contarPendientes());
        assertEquals(1, servicio.pendientes().size());
        assertTrue(servicio.informar(idCita, datos(1, "4522", ""), paciente).get("general").contains("revisando"));
        assertTrue(servicio.ultimoDeCuenta(reportes.reportes.get(1).getIdCuentaAtencion()).isPresent());
    }

    @Test
    public void cajaVerificaYEmiteLaBoleta() {
        ReportePagoDTO r = informado();
        CobranzaService.ResultadoCobro resultado = servicio.verificar(r.getIdReportePago(), cajero);
        assertTrue(resultado.isExito());
        assertTrue(resultado.comprobante().startsWith("B001-"));
        assertEquals("4521", pagos.aprobados.get(0).getNumeroOperacion());
        assertEquals("Yape", pagos.aprobados.get(0).getNombreBilletera());
        assertEquals(Integer.valueOf(r.getIdReportePago()), pagos.aprobados.get(0).getIdReportePago());
        assertFalse(servicio.verificar(r.getIdReportePago(), cajero).isExito());
        assertFalse(servicio.preparar(idCita, paciente).isExito());
    }

    @Test
    public void noSeVerificaSiElMontoCambio() {
        ReportePagoDTO r = informado();
        r.setMonto(bd("50.00"));
        assertTrue(servicio.verificar(r.getIdReportePago(), cajero).error().contains("la cuenta suma"));
        assertFalse(servicio.verificar(999, cajero).isExito());
    }

    @Test
    public void noSeVerificaSiLaCuentaYaNoEstaPendiente() {
        ReportePagoDTO r = informado();
        cuentas.cuentas.get(r.getIdCuentaAtencion()).setEstadoCuenta("ANULADA");
        assertTrue(servicio.verificar(r.getIdReportePago(), cajero).error().contains("ya no está por cobrar"));
    }

    @Test
    public void rechazarExigeMotivoYSoloUnaVez() {
        ReportePagoDTO r = informado();
        assertNotNull(servicio.rechazar(r.getIdReportePago(), "no", cajero));
        assertNotNull(servicio.rechazar(r.getIdReportePago(), null, cajero));
        assertNull(servicio.rechazar(r.getIdReportePago(), "No encontramos el pago en la app", cajero));
        assertTrue(r.isRechazado());
        assertNotNull(servicio.rechazar(r.getIdReportePago(), "No encontramos el pago en la app", cajero));
        ReportePagoService.Preparacion p = servicio.preparar(idCita, paciente);
        assertTrue(p.ultimoReporte().isRechazado());
        assertTrue(servicio.informar(idCita, datos(1, "9876", ""), paciente).isEmpty());
    }
}
