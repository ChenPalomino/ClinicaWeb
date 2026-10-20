package modelo.cobranza.pago;

import java.math.BigDecimal;

/**
 * Datos que el cajero ingresa para cobrar. Cada método de pago usa solo los suyos:
 *  - Efectivo:  montoRecibido
 *  - Tarjeta:   tipoTarjeta, ultimosDigitos, codigoAutorizacion (impresos en el voucher del POS)
 *  - Billetera: aplicacion (YAPE/PLIN), numeroOperacion, celular
 * Por seguridad NUNCA se pide ni se guarda el número completo de la tarjeta.
 */
public class SolicitudPago {

    private BigDecimal monto = BigDecimal.ZERO;   // total a cobrar (lo pone el servicio, no el formulario)
    private BigDecimal montoRecibido;
    private String tipoTarjeta = "";
    private String ultimosDigitos = "";
    private String codigoAutorizacion = "";
    private String aplicacion = "";
    private String numeroOperacion = "";
    private String celular = "";

    private static String limpio(String v) { return v == null ? "" : v.trim(); }

    public BigDecimal getMonto() { return monto; }
    public void setMonto(BigDecimal v) { monto = v; }
    public BigDecimal getMontoRecibido() { return montoRecibido; }
    public void setMontoRecibido(BigDecimal v) { montoRecibido = v; }
    public String getTipoTarjeta() { return tipoTarjeta; }
    public void setTipoTarjeta(String v) { tipoTarjeta = limpio(v).toUpperCase(); }
    public String getUltimosDigitos() { return ultimosDigitos; }
    public void setUltimosDigitos(String v) { ultimosDigitos = limpio(v); }
    public String getCodigoAutorizacion() { return codigoAutorizacion; }
    public void setCodigoAutorizacion(String v) { codigoAutorizacion = limpio(v); }
    public String getAplicacion() { return aplicacion; }
    public void setAplicacion(String v) { aplicacion = limpio(v).toUpperCase(); }
    public String getNumeroOperacion() { return numeroOperacion; }
    public void setNumeroOperacion(String v) { numeroOperacion = limpio(v); }
    public String getCelular() { return celular; }
    public void setCelular(String v) { celular = limpio(v).replace(" ", ""); }
}
