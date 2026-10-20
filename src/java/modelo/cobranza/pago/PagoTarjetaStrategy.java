package modelo.cobranza.pago;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * PATRÓN: Strategy  ->  ESTRATEGIA CONCRETA. Tarjeta de débito o crédito.
 *
 * El cobro lo hace el POS del banco; el cajero copia del voucher los últimos
 * 4 dígitos y el código de autorización (6 dígitos). Si el POS rechazó la
 * tarjeta, el voucher trae "000000" y el pago queda registrado como RECHAZADO.
 */
public class PagoTarjetaStrategy implements MetodoPagoStrategy {

    static final String AUTORIZACION_RECHAZADA = "000000";
    private static final Set<String> TIPOS = Set.of("DEBITO", "CREDITO");

    @Override
    public String getCodigo() {
        return "TARJETA";
    }

    @Override
    public Map<String, String> validar(SolicitudPago s) {
        Map<String, String> errores = new LinkedHashMap<>();
        if (!TIPOS.contains(s.getTipoTarjeta())) {
            errores.put("tipoTarjeta", "Elige si la tarjeta es de débito o de crédito.");
        }
        if (!s.getUltimosDigitos().matches("\\d{4}")) {
            errores.put("ultimosDigitos", "Escribe los últimos 4 dígitos de la tarjeta (están en el voucher).");
        }
        if (!s.getCodigoAutorizacion().matches("\\d{6}")) {
            errores.put("codigoAutorizacion", "El código de autorización del voucher tiene 6 dígitos.");
        }
        return errores;
    }

    @Override
    public ResultadoPago procesar(SolicitudPago s) {
        String tipo = "DEBITO".equals(s.getTipoTarjeta()) ? "Débito" : "Crédito";
        String referencia = tipo + " **** " + s.getUltimosDigitos() + " · Aut. " + s.getCodigoAutorizacion();
        if (AUTORIZACION_RECHAZADA.equals(s.getCodigoAutorizacion())) {
            return ResultadoPago.rechazado("El banco no aprobó la tarjeta terminada en " + s.getUltimosDigitos()
                    + ". Pide otra tarjeta o cambia el medio de pago.", referencia);
        }
        return ResultadoPago.aprobado(referencia);
    }
}
