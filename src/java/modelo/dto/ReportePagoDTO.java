package modelo.dto;

import java.math.BigDecimal;
import java.sql.Timestamp;

public class ReportePagoDTO {

    public static final String PENDIENTE = "PENDIENTE";
    public static final String VERIFICADO = "VERIFICADO";
    public static final String RECHAZADO = "RECHAZADO";

    private int idReportePago;
    private int idCuentaAtencion;
    private String numeroCuenta = "";
    private BigDecimal totalCuenta = BigDecimal.ZERO;
    private String estadoCuenta = "";
    private int idPaciente;
    private String nombrePaciente = "";
    private String dniPaciente = "";
    private int idBilletera;
    private String nombreBilletera = "";
    private BigDecimal monto = BigDecimal.ZERO;
    private String numeroOperacion = "";
    private String celular = "";
    private String nombrePagador = "";
    private String estadoReporte = PENDIENTE;
    private String motivoRechazo = "";
    private String usuarioRevisa = "";
    private Timestamp fechaRevision;
    private Integer idPago;
    private Timestamp fechaCreacion;
    private String usuarioRegistro = "";

    private static String limpio(String v) { return v == null ? "" : v; }

    public boolean isPendiente() { return PENDIENTE.equals(estadoReporte); }
    public boolean isVerificado() { return VERIFICADO.equals(estadoReporte); }
    public boolean isRechazado() { return RECHAZADO.equals(estadoReporte); }
    public boolean isMontoCoincide() { return monto != null && totalCuenta != null && monto.compareTo(totalCuenta) == 0; }

    public String getReferencia() {
        return nombreBilletera + (numeroOperacion.isEmpty() ? "" : " · Op. " + numeroOperacion)
                + (celular.isEmpty() ? "" : " · Cel. " + celular);
    }

    public int getIdReportePago() { return idReportePago; }
    public void setIdReportePago(int v) { idReportePago = v; }
    public int getIdCuentaAtencion() { return idCuentaAtencion; }
    public void setIdCuentaAtencion(int v) { idCuentaAtencion = v; }
    public String getNumeroCuenta() { return numeroCuenta; }
    public void setNumeroCuenta(String v) { numeroCuenta = limpio(v); }
    public BigDecimal getTotalCuenta() { return totalCuenta; }
    public void setTotalCuenta(BigDecimal v) { totalCuenta = v; }
    public String getEstadoCuenta() { return estadoCuenta; }
    public void setEstadoCuenta(String v) { estadoCuenta = limpio(v); }
    public int getIdPaciente() { return idPaciente; }
    public void setIdPaciente(int v) { idPaciente = v; }
    public String getNombrePaciente() { return nombrePaciente; }
    public void setNombrePaciente(String v) { nombrePaciente = limpio(v); }
    public String getDniPaciente() { return dniPaciente; }
    public void setDniPaciente(String v) { dniPaciente = limpio(v); }
    public int getIdBilletera() { return idBilletera; }
    public void setIdBilletera(int v) { idBilletera = v; }
    public String getNombreBilletera() { return nombreBilletera; }
    public void setNombreBilletera(String v) { nombreBilletera = limpio(v); }
    public BigDecimal getMonto() { return monto; }
    public void setMonto(BigDecimal v) { monto = v; }
    public String getNumeroOperacion() { return numeroOperacion; }
    public void setNumeroOperacion(String v) { numeroOperacion = limpio(v); }
    public String getCelular() { return celular; }
    public void setCelular(String v) { celular = limpio(v); }
    public String getNombrePagador() { return nombrePagador; }
    public void setNombrePagador(String v) { nombrePagador = limpio(v); }
    public String getEstadoReporte() { return estadoReporte; }
    public void setEstadoReporte(String v) { estadoReporte = limpio(v); }
    public String getMotivoRechazo() { return motivoRechazo; }
    public void setMotivoRechazo(String v) { motivoRechazo = limpio(v); }
    public String getUsuarioRevisa() { return usuarioRevisa; }
    public void setUsuarioRevisa(String v) { usuarioRevisa = limpio(v); }
    public Timestamp getFechaRevision() { return fechaRevision; }
    public void setFechaRevision(Timestamp v) { fechaRevision = v; }
    public Integer getIdPago() { return idPago; }
    public void setIdPago(Integer v) { idPago = v; }
    public Timestamp getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(Timestamp v) { fechaCreacion = v; }
    public String getUsuarioRegistro() { return usuarioRegistro; }
    public void setUsuarioRegistro(String v) { usuarioRegistro = limpio(v); }
}
