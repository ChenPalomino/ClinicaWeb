package modelo.cobranza.tarifa;

import java.math.BigDecimal;

/** PATRÓN: Strategy  ->  ESTRATEGIA CONCRETA. Particular: el paciente paga el precio de lista completo. */
public class TarifaParticularStrategy implements PoliticaTarifariaStrategy {

    @Override
    public String getTipoCobertura() {
        return "PARTICULAR";
    }

    @Override
    public String describir(BigDecimal porcentaje) {
        return "Paciente particular: paga el precio de lista.";
    }

    @Override
    public LineaTarifada tarifar(String tipoItem, BigDecimal precio, int cantidad, BigDecimal porcentaje) {
        return new LineaTarifada(PoliticaTarifariaStrategy.bruto(precio, cantidad), BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO);
    }
}
