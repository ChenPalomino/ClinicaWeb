package pruebas.cobranza;

import modelo.cobranza.TotalesCuenta;
import modelo.cobranza.comprobante.Boleta;
import modelo.cobranza.comprobante.Comprobante;
import modelo.cobranza.comprobante.ComprobanteFactory;
import modelo.cobranza.comprobante.Factura;
import modelo.dto.DatosClienteDTO;
import modelo.dto.ParametroClinicaDTO;
import org.junit.Test;

import static fakes.Datos.bd;
import static org.junit.Assert.*;

/** RF-15 · Factory Method: la fábrica decide si se emite boleta o factura. */
public class ComprobanteFactoryTest {

    private final ParametroClinicaDTO parametros = new ParametroClinicaDTO();
    private final TotalesCuenta totales = new TotalesCuenta(bd("67.80"), bd("0"), bd("12.20"), bd("0"), bd("0"));

    private static DatosClienteDTO cliente(String tipo, String documento, String nombre, String direccion) {
        DatosClienteDTO c = new DatosClienteDTO();
        c.setTipoComprobante(tipo); c.setNumeroDocumento(documento); c.setNombre(nombre); c.setDireccion(direccion);
        return c;
    }

    @Test
    public void boletaConDniYNumeracion() {
        Comprobante c = ComprobanteFactory.para("BOLETA").emitir(cliente("BOLETA", "10000004", "Ana Pérez", ""), totales, parametros);
        assertTrue(c instanceof Boleta);
        assertEquals("B001", c.getSerie());
        assertEquals("DNI", c.getTipoDocumento());
        c.setNumero(12);
        assertEquals("B001-00000012", c.getNumeroCompleto());
        assertEquals(0, c.getTotal().compareTo(bd("80.00")));
        assertFalse(c.getTitulo().isBlank());
    }

    @Test
    public void boletaPequenaSinDniYBoletaGrandeLoExige() {
        ComprobanteFactory f = ComprobanteFactory.para("BOLETA");
        DatosClienteDTO sinDni = cliente("BOLETA", "", "Ana Pérez", "");
        assertTrue(f.validarCliente(sinDni, totales, parametros).isEmpty());
        assertEquals("-", f.emitir(sinDni, totales, parametros).getTipoDocumento());
        TotalesCuenta grande = new TotalesCuenta(bd("700.00"), bd("0"), bd("126.00"), bd("0"), bd("0"));
        assertTrue(f.validarCliente(sinDni, grande, parametros).containsKey("numeroDocumento"));
    }

    @Test
    public void facturaConRucValidoYDireccion() {
        ComprobanteFactory f = ComprobanteFactory.para("FACTURA");
        Comprobante c = f.emitir(cliente("FACTURA", "20601234565", "Empresa Demo S.A.C.", "Av. Lima 123"), totales, parametros);
        assertTrue(c instanceof Factura);
        assertEquals("F001", c.getSerie());
        assertEquals("RUC", c.getTipoDocumento());
    }

    @Test
    public void facturaValidaRucYDireccion() {
        ComprobanteFactory f = ComprobanteFactory.para("FACTURA");
        assertTrue(f.validarCliente(cliente("FACTURA", "20601234560", "Empresa", "Av. Lima"), totales, parametros).containsKey("numeroDocumento"));
        assertTrue(f.validarCliente(cliente("FACTURA", "20601234565", "Empresa", ""), totales, parametros).containsKey("direccion"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void emitirConDatosInvalidosLanzaExcepcion() {
        ComprobanteFactory.para("FACTURA").emitir(cliente("FACTURA", "123", "", ""), totales, parametros);
    }

    @Test(expected = IllegalArgumentException.class)
    public void tipoDeComprobanteDesconocido() {
        ComprobanteFactory.para("TICKET");
    }
}
