package modelo.dto;

import java.math.BigDecimal;

/** RF-11: una línea de la cuenta de atención (un servicio o producto). */
public class DetalleCuentaDTO {

    private int idDetalleCuenta;
    private int idProductoServicio;
    private String codigo = "";
    private String descripcion = "";
    private String tipo = "";
    private int cantidad;
    private BigDecimal precioUnitario = BigDecimal.ZERO;
    private boolean afectoIgv = true;
    private BigDecimal porcentajeCubierto = BigDecimal.ZERO;
    private BigDecimal montoAseguradora = BigDecimal.ZERO;
    private BigDecimal montoDescuento = BigDecimal.ZERO;
    private BigDecimal importe = BigDecimal.ZERO;

    public boolean isProducto() { return "PRODUCTO".equals(tipo); }

    /** Precio de lista x cantidad. */
    public BigDecimal getBruto() { return precioUnitario.multiply(BigDecimal.valueOf(cantidad)); }

    public int getIdDetalleCuenta() { return idDetalleCuenta; }
    public void setIdDetalleCuenta(int v) { idDetalleCuenta = v; }
    public int getIdProductoServicio() { return idProductoServicio; }
    public void setIdProductoServicio(int v) { idProductoServicio = v; }
    public String getCodigo() { return codigo; }
    public void setCodigo(String v) { codigo = v == null ? "" : v; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String v) { descripcion = v == null ? "" : v; }
    public String getTipo() { return tipo; }
    public void setTipo(String v) { tipo = v == null ? "" : v; }
    public int getCantidad() { return cantidad; }
    public void setCantidad(int v) { cantidad = v; }
    public BigDecimal getPrecioUnitario() { return precioUnitario; }
    public void setPrecioUnitario(BigDecimal v) { precioUnitario = v; }
    public boolean isAfectoIgv() { return afectoIgv; }
    public void setAfectoIgv(boolean v) { afectoIgv = v; }
    public BigDecimal getPorcentajeCubierto() { return porcentajeCubierto; }
    public void setPorcentajeCubierto(BigDecimal v) { porcentajeCubierto = v; }
    public BigDecimal getMontoAseguradora() { return montoAseguradora; }
    public void setMontoAseguradora(BigDecimal v) { montoAseguradora = v; }
    public BigDecimal getMontoDescuento() { return montoDescuento; }
    public void setMontoDescuento(BigDecimal v) { montoDescuento = v; }
    public BigDecimal getImporte() { return importe; }
    public void setImporte(BigDecimal v) { importe = v; }
}
