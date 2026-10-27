package pruebas.servicios;

import fakes.CuentasFalsas;
import fakes.PagosFalsos;
import modelo.cobranza.pago.SolicitudPago;
import modelo.dto.CuentaAtencionDTO;
import modelo.dto.DatosClienteDTO;
import modelo.dto.ParametroClinicaDTO;
import modelo.entidades.Usuario;
import modelo.repositorio.OperacionRechazadaException;
import modelo.servicios.CobranzaService;
import org.junit.Before;
import org.junit.Test;

import java.time.LocalDate;

import static fakes.Datos.*;
import static org.junit.Assert.*;

/** RF-11 a RF-16 · Servicio de cobranza con repositorios en memoria (sin base de datos ni interfaz). */
public class CobranzaServiceTest {

    private CuentasFalsas cuentas;
    private PagosFalsos pagos;
    private CobranzaService servicio;
    private final Usuario cajero = usuario("recepcion", 2, 2);
    private final Usuario admin = usuario("admin", 1, 1);
    private DatosClienteDTO boleta;

    @Before
    public void preparar() {
        cuentas = new CuentasFalsas();
        cuentas.items.add(item(1, "Consulta general", "SERVICIO", "80.00", null, true));
        cuentas.items.add(item(2, "Paracetamol", "PRODUCTO", "25.00", 3, true));
        cuentas.items.add(item(3, "Alcohol", "PRODUCTO", "12.00", 10, false));
        cuentas.items.add(item(4, "Vendas", "PRODUCTO", "5.00", 0, true));
        pagos = new PagosFalsos();
        pagos.cuentas = cuentas;
        ParametroClinicaDTO parametros = new ParametroClinicaDTO();
        servicio = new CobranzaService(cuentas, pagos, () -> parametros, () -> LocalDate.of(2026, 10, 13));
        boleta = new DatosClienteDTO();
        boleta.setTipoComprobante("BOLETA"); boleta.setNombre("Paciente Demo"); boleta.setNumeroDocumento("10000004");
    }

    private int cuentaEpsConConsulta() {
        int id = servicio.abrirCuenta(2, "", cajero).idCuenta();
        servicio.agregarItem(id, 1, 1, cajero);
        return id;
    }

    private static SolicitudPago efectivo(String recibido) {
        SolicitudPago s = new SolicitudPago();
        s.setMontoRecibido(bd(recibido));
        return s;
    }

    @Test
    public void abreUnaSolaCuentaPendientePorPaciente() {
        CobranzaService.Apertura a = servicio.abrirCuenta(2, "", cajero);
        assertTrue(a.isExito());
        assertFalse(a.yaExistia());
        CobranzaService.Apertura otra = servicio.abrirCuenta(2, "", cajero);
        assertTrue(otra.yaExistia());
        assertEquals(a.idCuenta(), otra.idCuenta());
        assertFalse(servicio.abrirCuenta(99, "", cajero).isExito());
        assertFalse(servicio.abrirCuenta(1, "x".repeat(251), cajero).isExito());
    }

    @Test
    public void agregaItemsYControlaElStock() {
        int id = cuentaEpsConConsulta();
        assertNull(servicio.agregarItem(id, 2, 2, cajero));
        assertTrue(servicio.agregarItem(id, 2, 2, cajero).contains("Solo quedan 3"));
        assertNotNull(servicio.agregarItem(id, 1, 0, cajero));
        assertNotNull(servicio.agregarItem(id, 77, 1, cajero));
        assertNotNull(servicio.agregarItem(id, 4, 1, cajero));
        CuentaAtencionDTO c = servicio.obtener(id).get();
        assertTrue(igual(c.getTotal(), "39.00"));
        assertTrue(igual(c.getMontoAseguradora(), "91.00"));
    }

    @Test
    public void cambiaCantidadYQuitaLineas() {
        int id = cuentaEpsConConsulta();
        servicio.agregarItem(id, 2, 2, cajero);
        assertNull(servicio.cambiarCantidad(id, 2, 1, cajero));
        assertNull(servicio.cambiarCantidad(id, 2, 0, cajero));
        assertEquals(1, servicio.obtener(id).get().getDetalles().size());
        assertNotNull(servicio.cambiarCantidad(id, 3, 0, cajero));
        assertTrue(igual(servicio.obtener(id).get().getTotal(), "24.00"));
    }

    @Test
    public void erroresDeFormularioNoRegistranNada() {
        int id = cuentaEpsConConsulta();
        CobranzaService.ResultadoCobro r = servicio.cobrar(id, "TARJETA", new SolicitudPago(), boleta, cajero);
        assertFalse(r.errores().isEmpty());
        assertTrue(pagos.rechazos.isEmpty() && pagos.aprobados.isEmpty());
        assertTrue(servicio.cobrar(id, null, new SolicitudPago(), boleta, cajero).errores().containsKey("metodo"));
    }

    @Test
    public void pagoRechazadoSeRegistraYPermiteReintentar() {
        int id = cuentaEpsConConsulta();
        SolicitudPago t = new SolicitudPago();
        t.setTipoTarjeta("CREDITO"); t.setUltimosDigitos("1111"); t.setCodigoAutorizacion("000000");
        CobranzaService.ResultadoCobro r = servicio.cobrar(id, "TARJETA", t, boleta, cajero);
        assertNotNull(r.rechazo());
        assertEquals("RECHAZADO", pagos.rechazos.get(0).getEstadoPago());
        assertTrue(servicio.obtener(id).get().isPendiente());
        r = servicio.cobrar(id, "EFECTIVO", efectivo("50"), boleta, cajero);
        assertTrue(r.isExito());
        assertEquals("B001-00000001", r.comprobante());
        assertTrue(igual(r.vuelto(), "26.00"));
        assertTrue(igual(pagos.aprobados.get(0).getMonto(), "24.00"));
        assertEquals(2, pagos.aprobados.get(0).getIdUsuarioCajero());
    }

    @Test
    public void cuentaPagadaNoSeModificaNiSeCobraDosVeces() {
        int id = cuentaEpsConConsulta();
        assertTrue(servicio.cobrar(id, "EFECTIVO", efectivo("24"), boleta, cajero).isExito());
        assertNotNull(servicio.agregarItem(id, 1, 1, cajero));
        assertNotNull(servicio.cobrar(id, "EFECTIVO", efectivo("24"), boleta, cajero).error());
    }

    @Test
    public void cobroConBilleteraGuardaLosDatosDeLaApp() {
        int id = cuentaEpsConConsulta();
        SolicitudPago b = new SolicitudPago();
        b.setIdBilletera(2); b.setNumeroOperacion("4521"); b.setNombrePagador("Juan P.");
        CobranzaService.ResultadoCobro r = servicio.cobrar(id, "BILLETERA", b, boleta, cajero);
        assertTrue(r.isExito());
        assertEquals("Plin", pagos.aprobados.get(0).getNombreBilletera());
        assertEquals("4521", pagos.aprobados.get(0).getNumeroOperacion());
        SolicitudPago desconocida = new SolicitudPago();
        desconocida.setIdBilletera(9); desconocida.setNumeroOperacion("4521");
        int otra = servicio.abrirCuenta(1, "", cajero).idCuenta();
        servicio.agregarItem(otra, 1, 1, cajero);
        assertTrue(servicio.cobrar(otra, "BILLETERA", desconocida, boleta, cajero).errores().containsKey("billetera"));
    }

    @Test
    public void conflictoEnLaTransaccionLlegaComoMensaje() {
        int id = servicio.abrirCuenta(1, "", cajero).idCuenta();
        servicio.agregarItem(id, 3, 1, cajero);
        assertTrue(igual(servicio.obtener(id).get().getTotal(), "12.00"));
        assertTrue(igual(servicio.obtener(id).get().getIgv(), "0"));
        pagos.fallar = new OperacionRechazadaException("Esta cuenta ya fue pagada");
        CobranzaService.ResultadoCobro r = servicio.cobrar(id, "EFECTIVO", efectivo("12"), boleta, cajero);
        assertFalse(r.isExito());
        assertTrue(r.error().contains("ya fue pagada"));
    }

    @Test
    public void cuentaVaciaNoSeCobra() {
        int id = servicio.abrirCuenta(4, "", cajero).idCuenta();
        assertTrue(servicio.cobrar(id, "EFECTIVO", efectivo("12"), boleta, cajero).error().contains("al menos"));
    }

    @Test
    public void soloElAdministradorAnulaConMotivo() {
        int id = servicio.abrirCuenta(1, "", cajero).idCuenta();
        servicio.agregarItem(id, 3, 1, cajero);
        assertNotNull(servicio.anular(id, "Paciente se retiró sin atenderse", cajero));
        assertNotNull(servicio.anular(id, "corto", admin));
        assertNull(servicio.anular(id, "Paciente se retiró sin atenderse", admin));
        assertTrue(servicio.obtener(id).get().isAnulada());
        assertNotNull(servicio.anular(id, "Paciente se retiró sin atenderse", admin));
        assertNotNull(servicio.anular(999, "Paciente se retiró sin atenderse", admin));
        assertTrue(CobranzaService.puedeAnular(admin));
        assertFalse(CobranzaService.puedeAnular(cajero));
        assertFalse(CobranzaService.puedeAnular(null));
    }

    @Test
    public void seguroNoCubreProductos() {
        int id = servicio.abrirCuenta(3, "", cajero).idCuenta();
        servicio.agregarItem(id, 1, 1, cajero);
        servicio.agregarItem(id, 2, 1, cajero);
        assertTrue(igual(servicio.obtener(id).get().getTotal(), "41.00"));
        assertFalse(CobranzaService.describirTarifa(servicio.obtener(id).get()).isBlank());
    }

    @Test
    public void buscaEnElTarifarioConMinimoDosLetras() {
        assertTrue(servicio.buscarItems("p").isEmpty());
        assertTrue(servicio.buscarItems(null).isEmpty());
        assertEquals("Paracetamol", servicio.buscarItems("para").get(0).getNombre());
        assertFalse(servicio.itemsFrecuentes().isEmpty());
        assertEquals(2, servicio.billeteras().size());
    }
}
