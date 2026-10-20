package modelo.cobranza.pago;

import java.math.BigDecimal;

/**
 * Respuesta de un método de pago (RF-13 / RF-14). Si fue rechazado trae el
 * motivo en palabras para el cajero, que puede reintentar con otro medio.
 */
public final class ResultadoPago {

    private final boolean aprobado;
    private final String motivoRechazo;
    private final String referencia;
    private final BigDecimal montoRecibido;
    private final BigDecimal vuelto;

    private ResultadoPago(boolean aprobado, String motivoRechazo, String referencia,
                          BigDecimal montoRecibido, BigDecimal vuelto) {
        this.aprobado = aprobado;
        this.motivoRechazo = motivoRechazo;
        this.referencia = referencia;
        this.montoRecibido = montoRecibido;
        this.vuelto = vuelto;
    }

    public static ResultadoPago aprobado(String referencia) {
        return new ResultadoPago(true, null, referencia, null, null);
    }

    public static ResultadoPago aprobadoEnEfectivo(BigDecimal recibido, BigDecimal vuelto) {
        return new ResultadoPago(true, null, null, recibido, vuelto);
    }

    public static ResultadoPago rechazado(String motivo, String referencia) {
        return new ResultadoPago(false, motivo, referencia, null, null);
    }

    public boolean isAprobado() { return aprobado; }
    public String getMotivoRechazo() { return motivoRechazo; }
    public String getReferencia() { return referencia; }
    public BigDecimal getMontoRecibido() { return montoRecibido; }
    public BigDecimal getVuelto() { return vuelto; }
}
