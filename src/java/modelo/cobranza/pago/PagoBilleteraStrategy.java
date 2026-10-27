package modelo.cobranza.pago;

import java.util.LinkedHashMap;
import java.util.Map;

/** PATRÓN: Strategy -> pago con billetera digital. */
public class PagoBilleteraStrategy implements MetodoPagoStrategy {

    @Override
    public String getCodigo() {
        return "BILLETERA";
    }

    @Override
    public Map<String, String> validar(SolicitudPago s) {
        Map<String, String> errores = new LinkedHashMap<>();
        if (s.getNombreBilletera().isEmpty()) {
            errores.put("billetera", "Elige la billetera con la que pagó.");
        }
        boolean conOperacion = !s.getNumeroOperacion().isEmpty();
        boolean conCelular = !s.getCelular().isEmpty();
        if (conOperacion && !s.getNumeroOperacion().matches("\\d{3,20}")) {
            errores.put("numeroOperacion", "Escribe solo los números de la operación que se vean (al menos 3).");
        }
        if (conCelular && !s.getCelular().matches("9\\d{8}")) {
            errores.put("celular", "El celular tiene 9 dígitos y empieza con 9.");
        }
        if (!conOperacion && !conCelular) {
            errores.put("numeroOperacion", "Escribe el N.º de operación (aunque esté incompleto) o el celular desde el que pagó.");
        }
        if (s.getNombrePagador().length() > 80) {
            errores.put("nombrePagador", "Máximo 80 caracteres.");
        }
        return errores;
    }

    @Override
    public ResultadoPago procesar(SolicitudPago s) {
        String referencia = s.getNombreBilletera()
                + (s.getNumeroOperacion().isEmpty() ? "" : " · Op. " + s.getNumeroOperacion())
                + (s.getCelular().isEmpty() ? "" : " · Cel. ***" + s.getCelular().substring(6));
        if (!s.getNumeroOperacion().isEmpty() && s.getNumeroOperacion().matches("0+")) {
            return ResultadoPago.rechazado("La operación " + s.getNumeroOperacion() + " no aparece en " + s.getNombreBilletera()
                    + " de la clínica. Pide al paciente que muestre la constancia o usa otro medio de pago.", referencia);
        }
        return ResultadoPago.aprobado(referencia);
    }
}
