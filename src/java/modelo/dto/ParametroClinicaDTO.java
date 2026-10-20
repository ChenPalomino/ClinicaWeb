package modelo.dto;

import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * RF-22: datos de la clínica que aparecen en los comprobantes, el porcentaje de IGV
 * y las reglas de las citas (anticipación para cancelar y hasta cuándo se puede reservar).
 */
public class ParametroClinicaDTO {

    private String ruc = "";
    private String razonSocial = "";
    private String nombreComercial = "";
    private String direccion = "";
    private String telefono = "";
    private BigDecimal igvPorcentaje = new BigDecimal("18.00");
    private BigDecimal montoBoletaDni = new BigDecimal("700.00");
    private int horasCancelacionPaciente = 24;
    private int diasMaximosReserva = 60;
    private Timestamp fechaActualizacion;
    private String usuarioRegistro;

    private static String limpio(String v) { return v == null ? "" : v.trim(); }

    public String getRuc() { return ruc; }
    public void setRuc(String v) { ruc = limpio(v); }
    public String getRazonSocial() { return razonSocial; }
    public void setRazonSocial(String v) { razonSocial = limpio(v); }
    public String getNombreComercial() { return nombreComercial; }
    public void setNombreComercial(String v) { nombreComercial = limpio(v); }
    public String getDireccion() { return direccion; }
    public void setDireccion(String v) { direccion = limpio(v); }
    public String getTelefono() { return telefono; }
    public void setTelefono(String v) { telefono = limpio(v); }
    public BigDecimal getIgvPorcentaje() { return igvPorcentaje; }
    public void setIgvPorcentaje(BigDecimal v) { igvPorcentaje = v; }
    public BigDecimal getMontoBoletaDni() { return montoBoletaDni; }
    public void setMontoBoletaDni(BigDecimal v) { montoBoletaDni = v; }
    public int getHorasCancelacionPaciente() { return horasCancelacionPaciente; }
    public void setHorasCancelacionPaciente(int v) { horasCancelacionPaciente = v; }
    public int getDiasMaximosReserva() { return diasMaximosReserva; }
    public void setDiasMaximosReserva(int v) { diasMaximosReserva = v; }
    public Timestamp getFechaActualizacion() { return fechaActualizacion; }
    public void setFechaActualizacion(Timestamp v) { fechaActualizacion = v; }
    public String getUsuarioRegistro() { return usuarioRegistro; }
    public void setUsuarioRegistro(String v) { usuarioRegistro = v; }
}
