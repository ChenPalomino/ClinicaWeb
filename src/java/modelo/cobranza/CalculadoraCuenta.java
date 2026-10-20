package modelo.cobranza;

import modelo.cobranza.tarifa.LineaTarifada;
import modelo.cobranza.tarifa.PoliticaTarifariaStrategy;
import modelo.dto.DetalleCuentaDTO;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Cálculos de la cuenta de atención (RF-11 y RF-12). Sin base de datos ni
 * pantallas: se prueba de forma aislada (RNF-03).
 *
 *  - Los precios del tarifario YA incluyen IGV. La base imponible se obtiene
 *    dividiendo entre (1 + IGV) y el IGV es la diferencia, así la suma
 *    base + IGV coincide exactamente con lo que paga el paciente.
 *  - Las líneas no afectas a IGV (exoneradas) suman aparte.
 */
public final class CalculadoraCuenta {

    private static final BigDecimal CIEN = BigDecimal.valueOf(100);

    private CalculadoraCuenta() {
    }

    /** Llena los montos de una línea aplicando la política tarifaria de la cobertura. */
    public static void tarifar(DetalleCuentaDTO linea, PoliticaTarifariaStrategy politica, BigDecimal porcentaje) {
        LineaTarifada t = politica.tarifar(linea.getTipo(), linea.getPrecioUnitario(), linea.getCantidad(), porcentaje);
        linea.setPorcentajeCubierto(t.getPorcentajeCubierto());
        linea.setMontoAseguradora(t.getMontoAseguradora());
        linea.setMontoDescuento(t.getMontoDescuento());
        linea.setImporte(t.getImporte());
    }

    /** Suma las líneas y separa base imponible, IGV y exonerado. */
    public static TotalesCuenta totalizar(List<DetalleCuentaDTO> lineas, BigDecimal igvPorcentaje) {
        BigDecimal gravadoConIgv = BigDecimal.ZERO;
        BigDecimal exonerado = BigDecimal.ZERO;
        BigDecimal aseguradora = BigDecimal.ZERO;
        BigDecimal descuento = BigDecimal.ZERO;
        for (DetalleCuentaDTO l : lineas) {
            if (l.isAfectoIgv()) {
                gravadoConIgv = gravadoConIgv.add(l.getImporte());
            } else {
                exonerado = exonerado.add(l.getImporte());
            }
            aseguradora = aseguradora.add(l.getMontoAseguradora());
            descuento = descuento.add(l.getMontoDescuento());
        }
        BigDecimal factor = BigDecimal.ONE.add(igvPorcentaje.divide(CIEN, 6, RoundingMode.HALF_UP));
        BigDecimal base = gravadoConIgv.divide(factor, 2, RoundingMode.HALF_UP);
        BigDecimal igv = gravadoConIgv.subtract(base).setScale(2, RoundingMode.HALF_UP);
        return new TotalesCuenta(base, exonerado.setScale(2, RoundingMode.HALF_UP), igv,
                aseguradora.setScale(2, RoundingMode.HALF_UP), descuento.setScale(2, RoundingMode.HALF_UP));
    }
}
