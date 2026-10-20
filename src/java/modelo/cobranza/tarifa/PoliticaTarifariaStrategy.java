package modelo.cobranza.tarifa;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * PATRÓN: Strategy (de comportamiento)  ->  INTERFAZ DE LA ESTRATEGIA
 *
 * RF-12: el precio que paga el paciente depende de su cobertura. Cada tipo
 * de cobertura (particular, EPS, seguro, convenio) es una estrategia distinta.
 * CobranzaService no tiene ningún "if (tipo == ...)": pide la estrategia a
 * PoliticaTarifariaFactory y la usa. Agregar una cobertura nueva = una clase
 * nueva (principio abierto/cerrado, RNF-04).
 */
public interface PoliticaTarifariaStrategy {

    /** Tipo de cobertura que atiende (PARTICULAR, EPS, SEGURO o CONVENIO). */
    String getTipoCobertura();

    /** Explicación para la pantalla de caja, ej. "La EPS cubre el 70 % y el paciente paga el copago". */
    String describir(BigDecimal porcentaje);

    /**
     * Calcula una línea de la cuenta.
     * @param tipoItem   SERVICIO o PRODUCTO
     * @param precio     precio de lista unitario (con IGV)
     * @param cantidad   1 o más
     * @param porcentaje % de la cobertura del paciente (0 a 100)
     */
    LineaTarifada tarifar(String tipoItem, BigDecimal precio, int cantidad, BigDecimal porcentaje);

    /** Ayuda común: precio x cantidad redondeado a céntimos. */
    static BigDecimal bruto(BigDecimal precio, int cantidad) {
        return precio.multiply(BigDecimal.valueOf(cantidad)).setScale(2, RoundingMode.HALF_UP);
    }

    /** Ayuda común: porcentaje de un monto, redondeado a céntimos. */
    static BigDecimal porcentajeDe(BigDecimal monto, BigDecimal porcentaje) {
        return monto.multiply(porcentaje).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }
}
