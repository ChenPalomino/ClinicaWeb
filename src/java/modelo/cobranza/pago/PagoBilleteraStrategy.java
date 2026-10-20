package modelo.cobranza.pago;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * PATRÓN: Strategy  ->  ESTRATEGIA CONCRETA. Billetera digital (Yape / Plin).
 *
 * El cajero verifica la constancia en el celular de la clínica y registra el
 * N.º de operación. Si la operación no aparece (la constancia es falsa o no
 * llegó), el cajero escribe ceros y el intento queda registrado como RECHAZADO.
 */
public class PagoBilleteraStrategy implements MetodoPagoStrategy {

    private static final Set<String> APLICACIONES = Set.of("YAPE", "PLIN");

    @Override
    public String getCodigo() {
        return "BILLETERA";
    }

    @Override
    public Map<String, String> validar(SolicitudPago s) {
        Map<String, String> errores = new LinkedHashMap<>();
        if (!APLICACIONES.contains(s.getAplicacion())) {
            errores.put("aplicacion", "Elige Yape o Plin.");
        }
        if (!s.getNumeroOperacion().matches("\\d{6,12}")) {
            errores.put("numeroOperacion", "El N.º de operación tiene entre 6 y 12 dígitos.");
        }
        if (!s.getCelular().matches("9\\d{8}")) {
            errores.put("celular", "El celular tiene 9 dígitos y empieza con 9.");
        }
        return errores;
    }

    @Override
    public ResultadoPago procesar(SolicitudPago s) {
        String app = "YAPE".equals(s.getAplicacion()) ? "Yape" : "Plin";
        String referencia = app + " · Op. " + s.getNumeroOperacion() + " · Cel. ***" + s.getCelular().substring(6);
        if (s.getNumeroOperacion().matches("0+")) {
            return ResultadoPago.rechazado("La operación " + s.getNumeroOperacion() + " no aparece en " + app
                    + " de la clínica. Pide al paciente que muestre la constancia o usa otro medio de pago.", referencia);
        }
        return ResultadoPago.aprobado(referencia);
    }
}
