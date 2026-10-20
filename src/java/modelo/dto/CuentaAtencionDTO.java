package modelo.dto;

import modelo.cobranza.TotalesCuenta;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/** RF-11: cuenta de atención = CABECERA (paciente, cobertura, totales, estado) + DETALLE (líneas). */
public class CuentaAtencionDTO {

    public static final String PENDIENTE = "PENDIENTE";
    public static final String PAGADA = "PAGADA";
    public static final String ANULADA = "ANULADA";

    private int idCuentaAtencion;
    private String numeroCuenta = "";
    private int idPaciente;
    private String nombrePaciente = "";
    private String dniPaciente = "";
    private String numeroHistoria = "";
    private int idUsuarioPaciente;              // 0 si el paciente no tiene cuenta en el portal
    private int idCobertura;
    private String nombreCobertura = "";
    private String tipoCobertura = "";
    private BigDecimal porcentajeCobertura = BigDecimal.ZERO;
    private String estadoCuenta = PENDIENTE;
    private BigDecimal opGravada = BigDecimal.ZERO;
    private BigDecimal opExonerada = BigDecimal.ZERO;
    private BigDecimal igv = BigDecimal.ZERO;
    private BigDecimal total = BigDecimal.ZERO;
    private BigDecimal montoAseguradora = BigDecimal.ZERO;
    private BigDecimal montoDescuento = BigDecimal.ZERO;
    private String observacion = "";
    private Timestamp fechaCreacion;
    private Timestamp fechaPago;
    private String usuarioRegistro = "";
    private String motivoAnulacion;
    private Timestamp fechaAnulacion;
    private String usuarioAnula;
    private int cantidadItems;
    private List<DetalleCuentaDTO> detalles = new ArrayList<>();
    private PagoDTO pago;                        // pago aprobado (o anulado), si existe
    private List<PagoDTO> intentosRechazados = new ArrayList<>();
    private int idComprobante;
    private String comprobante;                  // B001-00000001

    public boolean isPendiente() { return PENDIENTE.equals(estadoCuenta); }
    public boolean isPagada() { return PAGADA.equals(estadoCuenta); }
    public boolean isAnulada() { return ANULADA.equals(estadoCuenta); }

    /** Precio de lista de todo (antes de cobertura o descuento). */
    public BigDecimal getTotalLista() { return total.add(montoAseguradora).add(montoDescuento); }

    /** Totales de la cabecera tal como están guardados. */
    public TotalesCuenta getTotales() {
        return new TotalesCuenta(opGravada, opExonerada, igv, montoAseguradora, montoDescuento);
    }

    public void aplicarTotales(TotalesCuenta t) {
        opGravada = t.getOpGravada();
        opExonerada = t.getOpExonerada();
        igv = t.getIgv();
        total = t.getTotal();
        montoAseguradora = t.getMontoAseguradora();
        montoDescuento = t.getMontoDescuento();
    }

    public int getIdCuentaAtencion() { return idCuentaAtencion; }
    public void setIdCuentaAtencion(int v) { idCuentaAtencion = v; }
    public String getNumeroCuenta() { return numeroCuenta; }
    public void setNumeroCuenta(String v) { numeroCuenta = v; }
    public int getIdPaciente() { return idPaciente; }
    public void setIdPaciente(int v) { idPaciente = v; }
    public String getNombrePaciente() { return nombrePaciente; }
    public void setNombrePaciente(String v) { nombrePaciente = v; }
    public String getDniPaciente() { return dniPaciente; }
    public void setDniPaciente(String v) { dniPaciente = v; }
    public String getNumeroHistoria() { return numeroHistoria; }
    public void setNumeroHistoria(String v) { numeroHistoria = v; }
    public int getIdUsuarioPaciente() { return idUsuarioPaciente; }
    public void setIdUsuarioPaciente(int v) { idUsuarioPaciente = v; }
    public int getIdCobertura() { return idCobertura; }
    public void setIdCobertura(int v) { idCobertura = v; }
    public String getNombreCobertura() { return nombreCobertura; }
    public void setNombreCobertura(String v) { nombreCobertura = v; }
    public String getTipoCobertura() { return tipoCobertura; }
    public void setTipoCobertura(String v) { tipoCobertura = v; }
    public BigDecimal getPorcentajeCobertura() { return porcentajeCobertura; }
    public void setPorcentajeCobertura(BigDecimal v) { porcentajeCobertura = v; }
    public String getEstadoCuenta() { return estadoCuenta; }
    public void setEstadoCuenta(String v) { estadoCuenta = v; }
    public BigDecimal getOpGravada() { return opGravada; }
    public void setOpGravada(BigDecimal v) { opGravada = v; }
    public BigDecimal getOpExonerada() { return opExonerada; }
    public void setOpExonerada(BigDecimal v) { opExonerada = v; }
    public BigDecimal getIgv() { return igv; }
    public void setIgv(BigDecimal v) { igv = v; }
    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal v) { total = v; }
    public BigDecimal getMontoAseguradora() { return montoAseguradora; }
    public void setMontoAseguradora(BigDecimal v) { montoAseguradora = v; }
    public BigDecimal getMontoDescuento() { return montoDescuento; }
    public void setMontoDescuento(BigDecimal v) { montoDescuento = v; }
    public String getObservacion() { return observacion; }
    public void setObservacion(String v) { observacion = v == null ? "" : v.trim(); }
    public Timestamp getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(Timestamp v) { fechaCreacion = v; }
    public Timestamp getFechaPago() { return fechaPago; }
    public void setFechaPago(Timestamp v) { fechaPago = v; }
    public String getUsuarioRegistro() { return usuarioRegistro; }
    public void setUsuarioRegistro(String v) { usuarioRegistro = v; }
    public String getMotivoAnulacion() { return motivoAnulacion; }
    public void setMotivoAnulacion(String v) { motivoAnulacion = v; }
    public Timestamp getFechaAnulacion() { return fechaAnulacion; }
    public void setFechaAnulacion(Timestamp v) { fechaAnulacion = v; }
    public String getUsuarioAnula() { return usuarioAnula; }
    public void setUsuarioAnula(String v) { usuarioAnula = v; }
    public int getCantidadItems() { return cantidadItems; }
    public void setCantidadItems(int v) { cantidadItems = v; }
    public List<DetalleCuentaDTO> getDetalles() { return detalles; }
    public void setDetalles(List<DetalleCuentaDTO> v) { detalles = v; }
    public PagoDTO getPago() { return pago; }
    public void setPago(PagoDTO v) { pago = v; }
    public List<PagoDTO> getIntentosRechazados() { return intentosRechazados; }
    public void setIntentosRechazados(List<PagoDTO> v) { intentosRechazados = v; }
    public int getIdComprobante() { return idComprobante; }
    public void setIdComprobante(int v) { idComprobante = v; }
    public String getComprobante() { return comprobante; }
    public void setComprobante(String v) { comprobante = v; }
}
