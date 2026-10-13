package modelo.dto;

import java.sql.Timestamp;

/** Una fila del registro de auditoría, lista para mostrarse. */
public class AuditoriaDTO {

    private long idAuditoria;
    private Timestamp fecha;
    private String usuario = "";
    private String accion = "";
    private String descripcion = "";
    private String entidad = "";
    private Integer idEntidad;
    private String detalle = "";
    private String direccionIp = "";

    private static String limpio(String v) { return v == null ? "" : v; }

    public long getIdAuditoria() { return idAuditoria; }
    public void setIdAuditoria(long v) { idAuditoria = v; }
    public Timestamp getFecha() { return fecha; }
    public void setFecha(Timestamp v) { fecha = v; }
    public String getUsuario() { return usuario; }
    public void setUsuario(String v) { usuario = limpio(v); }
    public String getAccion() { return accion; }
    public void setAccion(String v) { accion = limpio(v); }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String v) { descripcion = limpio(v); }
    public String getEntidad() { return entidad; }
    public void setEntidad(String v) { entidad = limpio(v); }
    public Integer getIdEntidad() { return idEntidad; }
    public void setIdEntidad(Integer v) { idEntidad = v; }
    public String getDetalle() { return detalle; }
    public void setDetalle(String v) { detalle = limpio(v); }
    public String getDireccionIp() { return direccionIp; }
    public void setDireccionIp(String v) { direccionIp = limpio(v); }
}
