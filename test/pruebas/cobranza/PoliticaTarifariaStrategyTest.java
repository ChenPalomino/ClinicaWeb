package pruebas.cobranza;

import modelo.cobranza.tarifa.LineaTarifada;
import modelo.cobranza.tarifa.PoliticaTarifariaFactory;
import org.junit.Test;

import static fakes.Datos.bd;
import static fakes.Datos.igual;
import static org.junit.Assert.*;

/** RF-12 · Strategy de tarifas: cada cobertura calcula lo que paga el paciente. */
public class PoliticaTarifariaStrategyTest {

    @Test
    public void particularPagaElPrecioCompleto() {
        LineaTarifada l = PoliticaTarifariaFactory.obtener("PARTICULAR").tarifar("SERVICIO", bd("80.00"), 2, bd("0"));
        assertTrue(igual(l.getImporte(), "160.00"));
        assertTrue(igual(l.getMontoAseguradora(), "0"));
    }

    @Test
    public void epsCubreSuPorcentajeYElPacientePagaElCopago() {
        LineaTarifada l = PoliticaTarifariaFactory.obtener("EPS").tarifar("PRODUCTO", bd("25.00"), 1, bd("70"));
        assertTrue(igual(l.getMontoAseguradora(), "17.50"));
        assertTrue(igual(l.getImporte(), "7.50"));
    }

    @Test
    public void seguroCubreServiciosPeroNoProductos() {
        assertTrue(igual(PoliticaTarifariaFactory.obtener("SEGURO").tarifar("SERVICIO", bd("150.00"), 1, bd("80")).getImporte(), "30.00"));
        LineaTarifada producto = PoliticaTarifariaFactory.obtener("SEGURO").tarifar("PRODUCTO", bd("25.00"), 2, bd("80"));
        assertTrue(igual(producto.getImporte(), "50.00"));
        assertTrue(igual(producto.getPorcentajeCubierto(), "0"));
    }

    @Test
    public void convenioAplicaDescuentoRedondeado() {
        LineaTarifada l = PoliticaTarifariaFactory.obtener("CONVENIO").tarifar("SERVICIO", bd("33.33"), 1, bd("40"));
        assertTrue(igual(l.getMontoDescuento(), "13.33"));
        assertTrue(igual(l.getImporte(), "20.00"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void coberturaDesconocidaLanzaExcepcion() {
        PoliticaTarifariaFactory.obtener("OTRA");
    }

    @Test
    public void cadaPoliticaSeDescribeParaElCajero() {
        assertTrue(PoliticaTarifariaFactory.obtener("EPS").describir(bd("70.00")).contains("70 %"));
        for (String tipo : new String[]{"PARTICULAR", "SEGURO", "CONVENIO"}) {
            assertFalse(PoliticaTarifariaFactory.obtener(tipo).describir(bd("40")).isBlank());
        }
    }
}
