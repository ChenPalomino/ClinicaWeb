package modelo.dto;

import java.sql.Timestamp;

/** Datos del proveedor (laboratorio, droguería, distribuidor de insumos). */
public class ProveedorDTO {

    private int idProveedor;
    private String ruc = "";
    private String razonSocial = "";
    private String nombreComercial = "";
    private String rubro = "";
    private String contactoNombre = "";
    private String telefono = "";
    private String correo = "";
    private String direccion = "";
    private String estado = "Activo";
    private int productos;              // servicios/productos que lo tienen como proveedor
    private Timestamp fechaCreacion;
    private String usuarioRegistro;

    public boolean isNuevo() { return idProveedor == 0; }
    public boolean isActivo() { return "Activo".equals(estado); }
    /** Nombre corto para listas: el comercial si existe; si no, la razón social. */
    public String getNombreMostrar() { return nombreComercial.isBlank() ? razonSocial : nombreComercial; }

    private static String limpio(String v) { return v == null ? "" : v; }

    public int getIdProveedor() { return idProveedor; }
    public void setIdProveedor(int v) { idProveedor = v; }
    public String getRuc() { return ruc; }
    public void setRuc(String v) { ruc = limpio(v); }
    public String getRazonSocial() { return razonSocial; }
    public void setRazonSocial(String v) { razonSocial = limpio(v); }
    public String getNombreComercial() { return nombreComercial; }
    public void setNombreComercial(String v) { nombreComercial = limpio(v); }
    public String getRubro() { return rubro; }
    public void setRubro(String v) { rubro = limpio(v); }
    public String getContactoNombre() { return contactoNombre; }
    public void setContactoNombre(String v) { contactoNombre = limpio(v); }
    public String getTelefono() { return telefono; }
    public void setTelefono(String v) { telefono = limpio(v); }
    public String getCorreo() { return correo; }
    public void setCorreo(String v) { correo = limpio(v); }
    public String getDireccion() { return direccion; }
    public void setDireccion(String v) { direccion = limpio(v); }
    public String getEstado() { return estado; }
    public void setEstado(String v) { estado = limpio(v); }
    public int getProductos() { return productos; }
    public void setProductos(int v) { productos = v; }
    public Timestamp getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(Timestamp v) { fechaCreacion = v; }
    public String getUsuarioRegistro() { return usuarioRegistro; }
    public void setUsuarioRegistro(String v) { usuarioRegistro = v; }
}
