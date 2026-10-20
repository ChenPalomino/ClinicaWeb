package modelo.cobranza.tarifa;

import java.math.BigDecimal;

/**
 * PATRÓN: Strategy  ->  ESTRATEGIA CONCRETA.
 * Seguro privado: cubre su porcentaje SOLO de los servicios médicos
 * (consultas, análisis, imágenes). Los productos de farmacia no están
 * cubiertos y el paciente los paga completos.
 */
public class TarifaSeguroStrategy implements PoliticaTarifariaStrategy {

    @Override
    public String getTipoCobertura() {
        return "SEGURO";
    }

    @Override
    public String describir(BigDecimal porcentaje) {
        return "Seguro privado: cubre el " + porcentaje.stripTrailingZeros().toPlainString()
                + " % de los servicios; los productos de farmacia se pagan completos.";
    }

    @Override
    public LineaTarifada tarifar(String tipoItem, BigDecimal precio, int cantidad, BigDecimal porcentaje) {
        BigDecimal bruto = PoliticaTarifariaStrategy.bruto(precio, cantidad);
        if (!"SERVICIO".equals(tipoItem)) {
            return new LineaTarifada(bruto, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);
        }
        return new LineaTarifada(bruto, porcentaje, PoliticaTarifariaStrategy.porcentajeDe(bruto, porcentaje),
                BigDecimal.ZERO);
    }
}
