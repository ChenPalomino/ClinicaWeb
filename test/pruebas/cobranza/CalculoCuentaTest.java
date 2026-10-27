package pruebas.cobranza;

import modelo.cobranza.CalculadoraCuenta;
import modelo.cobranza.CotizadorConsulta;
import modelo.cobranza.MontoEnLetras;
import modelo.cobranza.TotalesCuenta;
import modelo.cobranza.tarifa.PoliticaTarifariaFactory;
import modelo.dto.CoberturaDTO;
import modelo.dto.DetalleCuentaDTO;
import modelo.dto.PrecioConsultaDTO;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.List;

import static fakes.Datos.bd;
import static fakes.Datos.igual;
import static org.junit.Assert.*;

/** RF-11 · Totales con IGV incluido, monto en letras del comprobante y cotización de la consulta. */
public class CalculoCuentaTest {

    private static DetalleCuentaDTO linea(String importe, boolean igv) {
        DetalleCuentaDTO d = new DetalleCuentaDTO();
        d.setAfectoIgv(igv);
        d.setImporte(bd(importe));
        return d;
    }

    @Test
    public void separaBaseImponibleIgvYExonerado() {
        TotalesCuenta t = CalculadoraCuenta.totalizar(List.of(linea("80.00", true), linea("12.00", false)), bd("18"));
        assertTrue(igual(t.getOpGravada(), "67.80"));
        assertTrue(igual(t.getIgv(), "12.20"));
        assertTrue(igual(t.getOpExonerada(), "12.00"));
        assertTrue(igual(t.getTotal(), "92.00"));
    }

    @Test
    public void cuentaVaciaSumaCero() {
        assertTrue(igual(CalculadoraCuenta.totalizar(List.of(), bd("18")).getTotal(), "0"));
    }

    @Test
    public void elRedondeoNoPierdeCentimos() {
        TotalesCuenta t = CalculadoraCuenta.totalizar(List.of(linea("0.01", true)), bd("18"));
        assertTrue(igual(t.getOpGravada().add(t.getIgv()), "0.01"));
    }

    @Test
    public void tarifarUnaLineaAplicaLaPoliticaDeLaCobertura() {
        DetalleCuentaDTO d = new DetalleCuentaDTO();
        d.setTipo("SERVICIO"); d.setPrecioUnitario(bd("80.00")); d.setCantidad(1); d.setAfectoIgv(true);
        CalculadoraCuenta.tarifar(d, PoliticaTarifariaFactory.obtener("EPS"), bd("70"));
        assertTrue(igual(d.getImporte(), "24.00"));
        assertTrue(igual(d.getMontoAseguradora(), "56.00"));
    }

    @Test
    public void montoEnLetrasParaElComprobante() {
        assertEquals("CERO Y 00/100 SOLES", MontoEnLetras.convertir(BigDecimal.ZERO));
        assertEquals("QUINCE Y 50/100 SOLES", MontoEnLetras.convertir(bd("15.50")));
        assertEquals("VEINTIUNO Y 00/100 SOLES", MontoEnLetras.convertir(bd("21")));
        assertEquals("TREINTA Y UNO Y 00/100 SOLES", MontoEnLetras.convertir(bd("31")));
        assertEquals("CIEN Y 00/100 SOLES", MontoEnLetras.convertir(bd("100")));
        assertEquals("CIENTO UNO Y 00/100 SOLES", MontoEnLetras.convertir(bd("101")));
        assertEquals("DOSCIENTOS CINCUENTA Y 00/100 SOLES", MontoEnLetras.convertir(bd("250")));
        assertEquals("NOVECIENTOS NOVENTA Y NUEVE Y 99/100 SOLES", MontoEnLetras.convertir(bd("999.99")));
        assertEquals("MIL Y 00/100 SOLES", MontoEnLetras.convertir(bd("1000")));
        assertEquals("DOS MIL TRESCIENTOS CUARENTA Y CINCO Y 10/100 SOLES", MontoEnLetras.convertir(bd("2345.10")));
        assertTrue(MontoEnLetras.convertir(bd("1000000")).startsWith("UN MILL"));
        assertTrue(MontoEnLetras.convertir(bd("2000001")).startsWith("DOS MILLONES UNO"));
    }

    @Test
    public void cotizaLaConsultaSegunLaCobertura() {
        PrecioConsultaDTO particular = CotizadorConsulta.cotizar(bd("80.00"), null);
        assertTrue(igual(particular.getAPagar(), "80.00"));
        assertFalse(particular.isConCobertura());
        PrecioConsultaDTO eps = CotizadorConsulta.cotizar(bd("80.00"), new CoberturaDTO(2, "EPS", "EPS", bd("70")));
        assertTrue(igual(eps.getAPagar(), "24.00"));
        assertTrue(igual(eps.getCubre(), "56.00"));
        PrecioConsultaDTO seguro = CotizadorConsulta.cotizar(bd("80"), new CoberturaDTO(3, "Rimac", "SEGURO", bd("80")));
        assertEquals("Rimac cubre S/ 64.00", seguro.getDetalleCobertura());
        PrecioConsultaDTO convenio = CotizadorConsulta.cotizar(bd("100"), new CoberturaDTO(4, "Conv", "CONVENIO", bd("40")));
        assertTrue(convenio.isConvenio());
        assertTrue(igual(convenio.getAPagar(), "60.00"));
        assertTrue(convenio.getDetalleCobertura().startsWith("Descuento"));
    }

    @Test
    public void sinPrecioOCoberturaDesconocida() {
        assertFalse(CotizadorConsulta.cotizar(null, null).isDefinido());
        assertTrue(igual(CotizadorConsulta.cotizar(bd("50"), new CoberturaDTO(9, "X", "OTRA", bd("50"))).getAPagar(), "50.00"));
        assertEquals("1,234.50", PrecioConsultaDTO.soles(bd("1234.5")));
    }
}
