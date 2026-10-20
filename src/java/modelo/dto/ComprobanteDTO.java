package modelo.dto;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/** Comprobante emitido, listo para mostrarse o imprimirse (representación impresa). */
public class ComprobanteDTO {

    private int idComprobante;
    private int idCuentaAtencion;
    private String numeroCuenta = "";
    private String tipoComprobante = "";
    private String serie = "";
    private int numero;
    private String tipoDocumento = "";
    private String numeroDocumento;
    private String nombreCliente = "";
    private String direccionCliente;
    private BigDecimal opGravada = BigDecimal.ZERO;
    private BigDecimal opExonerada = BigDecimal.ZERO;
    private BigDecimal igv = BigDecimal.ZERO;
    private BigDecimal total = BigDecimal.ZERO;
    private BigDecimal montoAseguradora = BigDecimal.ZERO;
    private BigDecimal montoDescuento = BigDecimal.ZERO;
    private String estadoComprobante = "";
    private Timestamp fechaEmision;
    private String nombrePaciente = "";
    private String numeroHistoria = "";
    private String nombreCobertura = "";
    private int idUsuarioPaciente;
    private String metodoPago = "";
    private String referenciaPago;
    private BigDecimal montoRecibido;
    private BigDecimal vuelto;
    private String cajero = "";
    private List<DetalleCuentaDTO> detalles = new ArrayList<>();

    public boolean isFactura() { return "FACTURA".equals(tipoComprobante); }
    public boolean isAnulado() { return "ANULADO".equals(estadoComprobante); }
    public String getTitulo() { return isFactura() ? "FACTURA ELECTRÓNICA" : "BOLETA DE VENTA ELECTRÓNICA"; }
    public String getNumeroCompleto() { return serie + "-" + String.format("%08d", numero); }

    public int getIdComprobante() { return idComprobante; }
    public void setIdComprobante(int v) { idComprobante = v; }
    public int getIdCuentaAtencion() { return idCuentaAtencion; }
    public void setIdCuentaAtencion(int v) { idCuentaAtencion = v; }
    public String getNumeroCuenta() { return numeroCuenta; }
    public void setNumeroCuenta(String v) { numeroCuenta = v; }
    public String getTipoComprobante() { return tipoComprobante; }
    public void setTipoComprobante(String v) { tipoComprobante = v; }
    public String getSerie() { return serie; }
    public void setSerie(String v) { serie = v; }
    public int getNumero() { return numero; }
    public void setNumero(int v) { numero = v; }
    public String getTipoDocumento() { return tipoDocumento; }
    public void setTipoDocumento(String v) { tipoDocumento = v; }
    public String getNumeroDocumento() { return numeroDocumento; }
    public void setNumeroDocumento(String v) { numeroDocumento = v; }
    public String getNombreCliente() { return nombreCliente; }
    public void setNombreCliente(String v) { nombreCliente = v; }
    public String getDireccionCliente() { return direccionCliente; }
    public void setDireccionCliente(String v) { direccionCliente = v; }
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
    public String getEstadoComprobante() { return estadoComprobante; }
    public void setEstadoComprobante(String v) { estadoComprobante = v; }
    public Timestamp getFechaEmision() { return fechaEmision; }
    public void setFechaEmision(Timestamp v) { fechaEmision = v; }
    public String getNombrePaciente() { return nombrePaciente; }
    public void setNombrePaciente(String v) { nombrePaciente = v; }
    public String getNumeroHistoria() { return numeroHistoria; }
    public void setNumeroHistoria(String v) { numeroHistoria = v; }
    public String getNombreCobertura() { return nombreCobertura; }
    public void setNombreCobertura(String v) { nombreCobertura = v; }
    public int getIdUsuarioPaciente() { return idUsuarioPaciente; }
    public void setIdUsuarioPaciente(int v) { idUsuarioPaciente = v; }
    public String getMetodoPago() { return metodoPago; }
    public void setMetodoPago(String v) { metodoPago = v; }
    public String getReferenciaPago() { return referenciaPago; }
    public void setReferenciaPago(String v) { referenciaPago = v; }
    public BigDecimal getMontoRecibido() { return montoRecibido; }
    public void setMontoRecibido(BigDecimal v) { montoRecibido = v; }
    public BigDecimal getVuelto() { return vuelto; }
    public void setVuelto(BigDecimal v) { vuelto = v; }
    public String getCajero() { return cajero; }
    public void setCajero(String v) { cajero = v; }
    public List<DetalleCuentaDTO> getDetalles() { return detalles; }
    public void setDetalles(List<DetalleCuentaDTO> v) { detalles = v; }
}
