package modelo.dto;

import java.math.BigDecimal;
import java.sql.Timestamp;

public class EspecialidadDTO {

    private int idEspecialidad;
    private String nombre = "";
    private String descripcion = "";
    private String estado = "Activo";
    private int medicosActivos;
    private int medicosTotal;
    private Integer idServicioConsulta;
    private String codigoConsulta = "";
    private BigDecimal precioConsulta;
    private Timestamp fechaCreacion;
    private String usuarioRegistro = "";

    private static String limpio(String v) { return v == null ? "" : v; }

    public boolean isNuevo() { return idEspecialidad == 0; }
    public boolean isActivo() { return "Activo".equals(estado); }
    public boolean isTieneConsulta() { return idServicioConsulta != null; }

    public int getIdEspecialidad() { return idEspecialidad; }
    public void setIdEspecialidad(int v) { this.idEspecialidad = v; }
    public String getNombre() { return nombre; }
    public void setNombre(String v) { this.nombre = limpio(v); }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String v) { this.descripcion = limpio(v); }
    public String getEstado() { return estado; }
    public void setEstado(String v) { this.estado = "Inactivo".equals(v) ? "Inactivo" : "Activo"; }
    public int getMedicosActivos() { return medicosActivos; }
    public void setMedicosActivos(int v) { this.medicosActivos = v; }
    public int getMedicosTotal() { return medicosTotal; }
    public void setMedicosTotal(int v) { this.medicosTotal = v; }
    public Integer getIdServicioConsulta() { return idServicioConsulta; }
    public void setIdServicioConsulta(Integer v) { this.idServicioConsulta = v; }
    public String getCodigoConsulta() { return codigoConsulta; }
    public void setCodigoConsulta(String v) { this.codigoConsulta = limpio(v); }
    public BigDecimal getPrecioConsulta() { return precioConsulta; }
    public void setPrecioConsulta(BigDecimal v) { this.precioConsulta = v; }
    public Timestamp getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(Timestamp v) { this.fechaCreacion = v; }
    public String getUsuarioRegistro() { return usuarioRegistro; }
    public void setUsuarioRegistro(String v) { this.usuarioRegistro = limpio(v); }
}
