package pruebas.cobranza;

import modelo.cobranza.pago.MetodoPagoFactory;
import modelo.cobranza.pago.MetodoPagoStrategy;
import modelo.cobranza.pago.ResultadoPago;
import modelo.cobranza.pago.SolicitudPago;
import org.junit.Test;

import static fakes.Datos.bd;
import static fakes.Datos.igual;
import static org.junit.Assert.*;

/** RF-13 y RF-14 · Strategy de medios de pago: efectivo, tarjeta y billetera digital. */
public class MetodoPagoStrategyTest {

    private final MetodoPagoStrategy efectivo = MetodoPagoFactory.obtener("EFECTIVO");
    private final MetodoPagoStrategy tarjeta = MetodoPagoFactory.obtener("TARJETA");
    private final MetodoPagoStrategy billetera = MetodoPagoFactory.obtener("BILLETERA");

    private static SolicitudPago efectivo(String total, String recibido) {
        SolicitudPago s = new SolicitudPago();
        s.setMonto(bd(total));
        s.setMontoRecibido(recibido == null ? null : bd(recibido));
        return s;
    }

    private static SolicitudPago billetera(String operacion, String celular) {
        SolicitudPago s = new SolicitudPago();
        s.setMonto(bd("80"));
        s.setIdBilletera(1); s.setNombreBilletera("Yape");
        s.setNumeroOperacion(operacion); s.setCelular(celular);
        return s;
    }

    @Test
    public void laFabricaDevuelveCadaEstrategia() {
        assertEquals("EFECTIVO", efectivo.getCodigo());
        assertEquals("TARJETA", tarjeta.getCodigo());
        assertEquals("BILLETERA", billetera.getCodigo());
        assertNull(MetodoPagoFactory.obtener("CHEQUE"));
    }

    @Test
    public void efectivoCalculaElVuelto() {
        SolicitudPago s = efectivo("80.00", "100");
        assertTrue(efectivo.validar(s).isEmpty());
        ResultadoPago r = efectivo.procesar(s);
        assertTrue(r.isAprobado());
        assertTrue(igual(r.getVuelto(), "20.00"));
        assertTrue(igual(r.getMontoRecibido(), "100"));
    }

    @Test
    public void efectivoValidaElMontoRecibido() {
        assertTrue(efectivo.validar(efectivo("80.00", "50")).get("montoRecibido").contains("30.00"));
        assertTrue(efectivo.validar(efectivo("80.00", null)).containsKey("montoRecibido"));
        assertTrue(efectivo.validar(efectivo("80.00", "10.555")).containsKey("montoRecibido"));
        assertTrue(efectivo.validar(efectivo("80.00", "9000")).containsKey("montoRecibido"));
    }

    @Test
    public void tarjetaExigeTipoUltimosDigitosYAutorizacion() {
        SolicitudPago s = new SolicitudPago();
        s.setMonto(bd("80"));
        assertEquals(3, tarjeta.validar(s).size());
        s.setTipoTarjeta("debito"); s.setUltimosDigitos("4321"); s.setCodigoAutorizacion("123456");
        assertTrue(tarjeta.validar(s).isEmpty());
        ResultadoPago r = tarjeta.procesar(s);
        assertTrue(r.isAprobado());
        assertTrue(r.getReferencia().contains("**** 4321"));
    }

    @Test
    public void tarjetaRechazadaPorElBancoSeInforma() {
        SolicitudPago s = new SolicitudPago();
        s.setMonto(bd("80")); s.setTipoTarjeta("CREDITO"); s.setUltimosDigitos("4321"); s.setCodigoAutorizacion("000000");
        ResultadoPago r = tarjeta.procesar(s);
        assertFalse(r.isAprobado());
        assertTrue(r.getMotivoRechazo().contains("4321"));
    }

    @Test
    public void billeteraAceptaOperacionCompletaYOcultaElCelular() {
        SolicitudPago s = billetera("12345678", "987 654 321");
        assertTrue(billetera.validar(s).isEmpty());
        ResultadoPago r = billetera.procesar(s);
        assertTrue(r.isAprobado());
        assertTrue(r.getReferencia().contains("***321"));
        assertFalse(r.getReferencia().contains("987654321"));
    }

    @Test
    public void billeteraAceptaNumeroDeOperacionParcial() {
        assertTrue(billetera.validar(billetera("4521", "")).isEmpty());
        assertTrue(billetera.validar(billetera("123-456", "")).isEmpty());
        assertTrue(billetera.validar(billetera("12", "")).containsKey("numeroOperacion"));
        assertTrue(billetera.validar(billetera("12AB", "")).containsKey("numeroOperacion"));
    }

    @Test
    public void billeteraPideOperacionOCelularYUnaBilleteraElegida() {
        assertTrue(billetera.validar(billetera("", "987654321")).isEmpty());
        assertTrue(billetera.validar(billetera("", "")).containsKey("numeroOperacion"));
        assertTrue(billetera.validar(billetera("", "812345678")).containsKey("celular"));
        SolicitudPago sinApp = billetera("4521", "");
        sinApp.setNombreBilletera("");
        assertTrue(billetera.validar(sinApp).containsKey("billetera"));
        SolicitudPago nombreLargo = billetera("4521", "");
        nombreLargo.setNombrePagador("x".repeat(81));
        assertTrue(billetera.validar(nombreLargo).containsKey("nombrePagador"));
    }

    @Test
    public void billeteraRechazaOperacionInexistente() {
        ResultadoPago r = billetera.procesar(billetera("00000000", "987654321"));
        assertFalse(r.isAprobado());
        assertTrue(r.getMotivoRechazo().contains("Yape"));
    }
}
