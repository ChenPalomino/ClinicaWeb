package modelo.cobranza.tarifa;

import java.math.BigDecimal;

/**
 * PATRÓN: Strategy  ->  ESTRATEGIA CONCRETA.
 * Convenio empresarial: no hay aseguradora; la empresa negoció un DESCUENTO
 * sobre todo lo que consume el paciente.
 */
public class TarifaConvenioStrategy implements PoliticaTarifariaStrategy {

    @Override
    public String getTipoCobertura() {
        return "CONVENIO";
    }

    @Override
    public String describir(BigDecimal porcentaje) {
        return "Convenio empresarial: " + porcentaje.stripTrailingZeros().toPlainString()
                + " % de descuento en todo.";
    }

    @Override
    public LineaTarifada tarifar(String tipoItem, BigDecimal precio, int cantidad, BigDecimal porcentaje) {
        BigDecimal bruto = PoliticaTarifariaStrategy.bruto(precio, cantidad);
        return new LineaTarifada(bruto, porcentaje, BigDecimal.ZERO,
                PoliticaTarifariaStrategy.porcentajeDe(bruto, porcentaje));
    }
}
