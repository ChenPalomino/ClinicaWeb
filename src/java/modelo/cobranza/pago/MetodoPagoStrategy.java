package modelo.cobranza.pago;

import java.util.Map;

/**
 * PATRÓN: Strategy (de comportamiento)  ->  INTERFAZ DE LA ESTRATEGIA
 *
 * RF-13: cada medio de pago (efectivo, tarjeta, billetera digital) valida
 * y procesa el cobro a su manera. CobranzaService solo llama a validar() y
 * procesar(); agregar un medio nuevo (ej. transferencia) = una clase nueva
 * registrada en MetodoPagoFactory, sin tocar las existentes (RNF-04).
 */
public interface MetodoPagoStrategy {

    /** Código guardado en tm_metodo_pago (EFECTIVO, TARJETA, BILLETERA). */
    String getCodigo();

    /**
     * Revisa que el formulario esté completo. Un dato faltante NO es un pago rechazado:
     * se corrige en el mismo formulario y no se registra nada.
     * @return campo -> mensaje (vacío si todo está bien)
     */
    Map<String, String> validar(SolicitudPago solicitud);

    /** Procesa el cobro (solicitud ya validada). Si no se aprueba, el resultado explica por qué (RF-14). */
    ResultadoPago procesar(SolicitudPago solicitud);
}
