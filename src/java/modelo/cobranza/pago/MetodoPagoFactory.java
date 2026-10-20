package modelo.cobranza.pago;

import java.util.Map;

/** Entrega la estrategia de cada medio de pago según su código (tm_metodo_pago.codigo). */
public final class MetodoPagoFactory {

    private static final Map<String, MetodoPagoStrategy> ESTRATEGIAS = Map.of(
            "EFECTIVO", new PagoEfectivoStrategy(),
            "TARJETA", new PagoTarjetaStrategy(),
            "BILLETERA", new PagoBilleteraStrategy());

    private MetodoPagoFactory() {
    }

    /** @return la estrategia, o null si el código no existe (el servicio lo informa como error del formulario). */
    public static MetodoPagoStrategy obtener(String codigo) {
        return codigo == null ? null : ESTRATEGIAS.get(codigo);
    }
}
