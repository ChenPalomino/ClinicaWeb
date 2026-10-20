package modelo.cobranza.tarifa;

import java.math.BigDecimal;

/**
 * PATRÓN: Strategy  ->  ESTRATEGIA CONCRETA.
 * EPS: la EPS paga su porcentaje de servicios y productos; el paciente paga
 * el resto (copago). La parte de la EPS queda registrada para cobrársela a ella.
 */
public class TarifaEpsStrategy implements PoliticaTarifariaStrategy {

    @Override
    public String getTipoCobertura() {
        return "EPS";
    }

    @Override
    public String describir(BigDecimal porcentaje) {
        return "EPS: cubre el " + porcentaje.stripTrailingZeros().toPlainString()
                + " % de servicios y medicamentos; el paciente paga el copago.";
    }

    @Override
    public LineaTarifada tarifar(String tipoItem, BigDecimal precio, int cantidad, BigDecimal porcentaje) {
        BigDecimal bruto = PoliticaTarifariaStrategy.bruto(precio, cantidad);
        return new LineaTarifada(bruto, porcentaje, PoliticaTarifariaStrategy.porcentajeDe(bruto, porcentaje),
                BigDecimal.ZERO);
    }
}
