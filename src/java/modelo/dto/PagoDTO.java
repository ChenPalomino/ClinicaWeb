package modelo.dto;

import java.math.BigDecimal;
import java.sql.Timestamp;

/** RF-13 / RF-14: un intento de pago (aprobado, rechazado o anulado). */
public class PagoDTO {

    public static final String APROBADO = "APROBADO";
    public static final String RECHAZADO = "RECHAZADO";
    public static final String ANULADO = "ANULADO";

    private int idPago;
    private int idCuentaAtencion;
    private String numeroCuenta = "";
    private String nombrePaciente = "";
    private int idMetodoPago;
    private String codigoMetodo = "";
    private String nombreMetodo = "";
    private BigDecimal monto = BigDecimal.ZERO;
    private BigDecimal montoRecibido;
    private BigDecimal vuelto;
    private String referencia;
    private String estadoPago = "";
    private String motivoRechazo;
    private int idUsuarioCajero;
    private String nombreCajero = "";
    private String comprobante;          // B001-00000001 (si se emitió)
    private Timestamp fechaCreacion;

    public boolean isAprobado() { return APROBADO.equals(estadoPago); }
    public boolean isRechazado() { return RECHAZADO.equals(estadoPago); }

    public int getIdPago() { return idPago; }
    public void setIdPago(int v) { idPago = v; }
    public int getIdCuentaAtencion() { return idCuentaAtencion; }
    public void setIdCuentaAtencion(int v) { idCuentaAtencion = v; }
    public String getNumeroCuenta() { return numeroCuenta; }
    public void setNumeroCuenta(String v) { numeroCuenta = v; }
    public String getNombrePaciente() { return nombrePaciente; }
    public void setNombrePaciente(String v) { nombrePaciente = v; }
    public int getIdMetodoPago() { return idMetodoPago; }
    public void setIdMetodoPago(int v) { idMetodoPago = v; }
    public String getCodigoMetodo() { return codigoMetodo; }
    public void setCodigoMetodo(String v) { codigoMetodo = v; }
    public String getNombreMetodo() { return nombreMetodo; }
    public void setNombreMetodo(String v) { nombreMetodo = v; }
    public BigDecimal getMonto() { return monto; }
    public void setMonto(BigDecimal v) { monto = v; }
    public BigDecimal getMontoRecibido() { return montoRecibido; }
    public void setMontoRecibido(BigDecimal v) { montoRecibido = v; }
    public BigDecimal getVuelto() { return vuelto; }
    public void setVuelto(BigDecimal v) { vuelto = v; }
    public String getReferencia() { return referencia; }
    public void setReferencia(String v) { referencia = v; }
    public String getEstadoPago() { return estadoPago; }
    public void setEstadoPago(String v) { estadoPago = v; }
    public String getMotivoRechazo() { return motivoRechazo; }
    public void setMotivoRechazo(String v) { motivoRechazo = v; }
    public int getIdUsuarioCajero() { return idUsuarioCajero; }
    public void setIdUsuarioCajero(int v) { idUsuarioCajero = v; }
    public String getNombreCajero() { return nombreCajero; }
    public void setNombreCajero(String v) { nombreCajero = v; }
    public String getComprobante() { return comprobante; }
    public void setComprobante(String v) { comprobante = v; }
    public Timestamp getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(Timestamp v) { fechaCreacion = v; }
}
