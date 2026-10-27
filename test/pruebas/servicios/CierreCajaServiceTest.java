package pruebas.servicios;

import fakes.CierresFalsos;
import fakes.PagosFalsos;
import modelo.dto.PagoDTO;
import modelo.dto.ResumenCajaDTO;
import modelo.entidades.Usuario;
import modelo.servicios.CierreCajaService;
import org.junit.Before;
import org.junit.Test;

import java.time.LocalDate;

import static fakes.Datos.*;
import static org.junit.Assert.*;

/** RF-17 · Cierre de caja diario: totales por medio de pago y diferencia con lo contado. */
public class CierreCajaServiceTest {

    private final LocalDate hoy = LocalDate.of(2026, 10, 26);
    private final Usuario cajero = usuario("recepcion", 2, 2);
    private final Usuario admin = usuario("admin", 1, 1);
    private PagosFalsos pagos;
    private CierresFalsos cierres;
    private CierreCajaService servicio;

    private static PagoDTO pago(String metodo, String monto, String estado) {
        PagoDTO p = new PagoDTO();
        p.setCodigoMetodo(metodo);
        p.setMonto(bd(monto));
        p.setEstadoPago(estado);
        return p;
    }

    @Before
    public void preparar() {
        pagos = new PagosFalsos();
        pagos.delCajero.add(pago("EFECTIVO", "80.00", "APROBADO"));
        pagos.delCajero.add(pago("EFECTIVO", "24.00", "APROBADO"));
        pagos.delCajero.add(pago("TARJETA", "35.00", "APROBADO"));
        pagos.delCajero.add(pago("BILLETERA", "12.00", "APROBADO"));
        pagos.delCajero.add(pago("TARJETA", "80.00", "RECHAZADO"));
        pagos.delCajero.add(pago("EFECTIVO", "50.00", "ANULADO"));
        cierres = new CierresFalsos();
        servicio = new CierreCajaService(pagos, cierres, () -> hoy);
    }

    @Test
    public void resumeLoCobradoPorMedioDePago() {
        ResumenCajaDTO r = servicio.resumenDeHoy(cajero);
        assertEquals(4, r.getCantidadPagos());
        assertEquals(1, r.getCantidadRechazados());
        assertEquals(1, r.getCantidadAnulados());
        assertTrue(igual(r.getTotalEfectivo(), "104.00"));
        assertTrue(igual(r.getTotalTarjeta(), "35.00"));
        assertTrue(igual(r.getTotalBilletera(), "12.00"));
        assertTrue(igual(r.getTotalGeneral(), "151.00"));
        assertFalse(r.isCerrada());
    }

    @Test
    public void cierraSinDiferencia() {
        assertNull(servicio.cerrar(cajero, bd("104.00"), ""));
        assertEquals(1, cierres.guardados.size());
        assertTrue(igual(cierres.guardados.get(0).getDiferencia(), "0"));
        assertTrue(servicio.resumenDeHoy(cajero).isCerrada());
        assertTrue(servicio.cerrar(cajero, bd("104.00"), "").contains("ya estaba cerrada"));
    }

    @Test
    public void unaDiferenciaExigeExplicacion() {
        assertTrue(servicio.cerrar(cajero, bd("100.00"), "").contains("diferencia"));
        assertNull(servicio.cerrar(cajero, bd("100.00"), "Faltó sencillo para un vuelto"));
        assertTrue(igual(cierres.guardados.get(0).getDiferencia(), "-4.00"));
    }

    @Test
    public void validaElEfectivoContado() {
        assertNotNull(servicio.cerrar(cajero, null, ""));
        assertNotNull(servicio.cerrar(cajero, bd("-1"), ""));
        assertNotNull(servicio.cerrar(cajero, bd("10.555"), ""));
        assertNotNull(servicio.cerrar(cajero, bd("200000"), ""));
        assertNotNull(servicio.cerrar(cajero, bd("104"), "x".repeat(251)));
    }

    @Test
    public void historialSegunRol() {
        servicio.historial(admin);
        assertNull(cierres.historialPedido);
        servicio.historial(cajero);
        assertEquals(Integer.valueOf(2), cierres.historialPedido);
    }
}
