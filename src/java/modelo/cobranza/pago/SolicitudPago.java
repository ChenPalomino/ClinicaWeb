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
    private Integer idBilletera;
    private String nombreBilletera = "";
    private String numeroOperacion = "";
    private String celular = "";
    private String nombrePagador = "";
    private Integer idReportePago;

    public Integer getIdReportePago() { return idReportePago; }
    public void setIdReportePago(Integer v) { idReportePago = v; }

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
    public Integer getIdBilletera() { return idBilletera; }
    public void setIdBilletera(Integer v) { idBilletera = v; }
    public String getNombreBilletera() { return nombreBilletera; }
    public void setNombreBilletera(String v) { nombreBilletera = limpio(v); }
    public String getNombrePagador() { return nombrePagador; }
    public void setNombrePagador(String v) { nombrePagador = limpio(v).replaceAll("\\s+", " "); }
    public String getNumeroOperacion() { return numeroOperacion; }
    public void setNumeroOperacion(String v) { numeroOperacion = limpio(v).replaceAll("[\\s-]", ""); }
    public String getCelular() { return celular; }
    public void setCelular(String v) { celular = limpio(v).replace(" ", ""); }
}
