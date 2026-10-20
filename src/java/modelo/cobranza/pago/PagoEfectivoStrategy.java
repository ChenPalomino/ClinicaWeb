package modelo.cobranza.pago;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.Map;

/** PATRÓN: Strategy  ->  ESTRATEGIA CONCRETA. Efectivo: valida que alcance y calcula el vuelto. */
public class PagoEfectivoStrategy implements MetodoPagoStrategy {

    /** Tope razonable para detectar un monto mal digitado (ej. 8000 en vez de 80.00). */
    static final BigDecimal MAXIMO_RECIBIDO = new BigDecimal("5000");

    @Override
    public String getCodigo() {
        return "EFECTIVO";
    }

    @Override
    public Map<String, String> validar(SolicitudPago s) {
        Map<String, String> errores = new LinkedHashMap<>();
        BigDecimal recibido = s.getMontoRecibido();
        if (recibido == null) {
            errores.put("montoRecibido", "Ingresa con cuánto paga el paciente.");
        } else if (recibido.signum() < 0 || recibido.stripTrailingZeros().scale() > 2) {
            errores.put("montoRecibido", "Escribe un monto válido, con hasta 2 decimales.");
        } else if (recibido.compareTo(MAXIMO_RECIBIDO) > 0) {
            errores.put("montoRecibido", "Revisa el monto: no puede pasar de S/ 5,000.00.");
        } else if (recibido.compareTo(s.getMonto()) < 0) {
            // No es un rechazo: el cajero corrige el monto (o pide el resto) en el mismo formulario
            errores.put("montoRecibido", "Faltan S/ " + s.getMonto().subtract(recibido).setScale(2, RoundingMode.HALF_UP)
                    + " para completar el total.");
        }
        return errores;
    }

    /** El efectivo se recibe en la mano: siempre se aprueba y se calcula el vuelto. */
    @Override
    public ResultadoPago procesar(SolicitudPago s) {
        BigDecimal recibido = s.getMontoRecibido().setScale(2, RoundingMode.HALF_UP);
        return ResultadoPago.aprobadoEnEfectivo(recibido, recibido.subtract(s.getMonto()).setScale(2, RoundingMode.HALF_UP));
    }
}
