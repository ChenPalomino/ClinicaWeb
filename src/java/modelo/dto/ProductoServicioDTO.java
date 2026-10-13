package modelo.dto;

import java.math.BigDecimal;
import java.sql.Timestamp;

/** Un ítem del tarifario: un SERVICIO (consulta, análisis...) o un PRODUCTO con stock. */
public class ProductoServicioDTO {

    public static final String SERVICIO = "SERVICIO";
    public static final String PRODUCTO = "PRODUCTO";

    private int idProductoServicio;
    private String codigo = "";
    private String nombre = "";
    private String descripcion = "";
    private String tipo = SERVICIO;
    private int idCategoria;
    private String nombreCategoria = "";
    private Integer idEspecialidad;
    private String nombreEspecialidad = "";
    private Integer idMarca;
    private String nombreMarca = "";
    private int idUnidadMedida;
    private String unidad = "";
    private Integer idProveedor;
    private String nombreProveedor = "";
    private BigDecimal precio;
    private boolean afectoIgv = true;
    private Integer stock;
    private Integer stockMinimo;
    private String estado = "Activo";
    private Timestamp fechaCreacion;
    private String usuarioRegistro;

    public boolean isNuevo() { return idProductoServicio == 0; }
    public boolean isActivo() { return "Activo".equals(estado); }
    public boolean isProducto() { return PRODUCTO.equals(tipo); }
    /** Stock en o por debajo del mínimo: se resalta para reponer. */
    public boolean isStockBajo() { return isProducto() && stock != null && stockMinimo != null && stock <= stockMinimo; }

    private static String limpio(String v) { return v == null ? "" : v; }

    public int getIdProductoServicio() { return idProductoServicio; }
    public void setIdProductoServicio(int v) { idProductoServicio = v; }
    public String getCodigo() { return codigo; }
    public void setCodigo(String v) { codigo = limpio(v); }
    public String getNombre() { return nombre; }
    public void setNombre(String v) { nombre = limpio(v); }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String v) { descripcion = limpio(v); }
    public String getTipo() { return tipo; }
    public void setTipo(String v) { tipo = PRODUCTO.equals(v) ? PRODUCTO : SERVICIO; }
    public int getIdCategoria() { return idCategoria; }
    public void setIdCategoria(int v) { idCategoria = v; }
    public String getNombreCategoria() { return nombreCategoria; }
    public void setNombreCategoria(String v) { nombreCategoria = limpio(v); }
    public Integer getIdEspecialidad() { return idEspecialidad; }
    public void setIdEspecialidad(Integer v) { idEspecialidad = v; }
    public String getNombreEspecialidad() { return nombreEspecialidad; }
    public void setNombreEspecialidad(String v) { nombreEspecialidad = limpio(v); }
    public Integer getIdMarca() { return idMarca; }
    public void setIdMarca(Integer v) { idMarca = v; }
    public String getNombreMarca() { return nombreMarca; }
    public void setNombreMarca(String v) { nombreMarca = limpio(v); }
    public int getIdUnidadMedida() { return idUnidadMedida; }
    public void setIdUnidadMedida(int v) { idUnidadMedida = v; }
    public String getUnidad() { return unidad; }
    public void setUnidad(String v) { unidad = limpio(v); }
    public Integer getIdProveedor() { return idProveedor; }
    public void setIdProveedor(Integer v) { idProveedor = v; }
    public String getNombreProveedor() { return nombreProveedor; }
    public void setNombreProveedor(String v) { nombreProveedor = limpio(v); }
    public BigDecimal getPrecio() { return precio; }
    public void setPrecio(BigDecimal v) { precio = v; }
    public boolean isAfectoIgv() { return afectoIgv; }
    public void setAfectoIgv(boolean v) { afectoIgv = v; }
    public Integer getStock() { return stock; }
    public void setStock(Integer v) { stock = v; }
    public Integer getStockMinimo() { return stockMinimo; }
    public void setStockMinimo(Integer v) { stockMinimo = v; }
    public String getEstado() { return estado; }
    public void setEstado(String v) { estado = limpio(v); }
    public Timestamp getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(Timestamp v) { fechaCreacion = v; }
    public String getUsuarioRegistro() { return usuarioRegistro; }
    public void setUsuarioRegistro(String v) { usuarioRegistro = v; }
}
