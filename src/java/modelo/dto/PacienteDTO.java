package modelo.dto;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.Period;

/** Datos del paciente que viajan entre la vista, el servicio y el Repository. */
public class PacienteDTO {

    private int idPaciente;
    private int idDatosPersonales;
    private String numeroHistoria = "";
    private String nombres = "";
    private String apellidos = "";
    private String dni = "";
    private Integer idSexo;
    private LocalDate fechaNacimiento;
    private String correo = "";
    private String celular = "";
    private int idCobertura = 1;
    private String nombreCobertura = "";
    private String tipoCobertura = "PARTICULAR";
    private String numeroAfiliacion = "";
    private String observaciones = "";
    private String estado = "Activo";
    private boolean tieneCuenta;
    private Timestamp fechaCreacion;
    private String usuarioRegistro;

    public boolean isNuevo() { return idPaciente == 0; }
    public boolean isActivo() { return "Activo".equals(estado); }
    public String getNombreCompleto() { return (nombres + " " + apellidos).trim(); }

    /** Edad en años cumplidos, o null si no se registró la fecha de nacimiento. */
    public Integer getEdad() {
        return fechaNacimiento == null ? null : Period.between(fechaNacimiento, LocalDate.now()).getYears();
    }

    public String getIniciales() {
        String n = nombres.isBlank() ? "" : nombres.substring(0, 1);
        String a = apellidos.isBlank() ? "" : apellidos.substring(0, 1);
        return (n + a).toUpperCase();
    }

    private static String limpio(String v) { return v == null ? "" : v; }

    public int getIdPaciente() { return idPaciente; }
    public void setIdPaciente(int v) { idPaciente = v; }
    public int getIdDatosPersonales() { return idDatosPersonales; }
    public void setIdDatosPersonales(int v) { idDatosPersonales = v; }
    public String getNumeroHistoria() { return numeroHistoria; }
    public void setNumeroHistoria(String v) { numeroHistoria = limpio(v); }
    public String getNombres() { return nombres; }
    public void setNombres(String v) { nombres = limpio(v); }
    public String getApellidos() { return apellidos; }
    public void setApellidos(String v) { apellidos = limpio(v); }
    public String getDni() { return dni; }
    public void setDni(String v) { dni = limpio(v); }
    public Integer getIdSexo() { return idSexo; }
    public void setIdSexo(Integer v) { idSexo = v; }
    public LocalDate getFechaNacimiento() { return fechaNacimiento; }
    public void setFechaNacimiento(LocalDate v) { fechaNacimiento = v; }
    public String getCorreo() { return correo; }
    public void setCorreo(String v) { correo = limpio(v); }
    public String getCelular() { return celular; }
    public void setCelular(String v) { celular = limpio(v); }
    public int getIdCobertura() { return idCobertura; }
    public void setIdCobertura(int v) { idCobertura = v; }
    public String getNombreCobertura() { return nombreCobertura; }
    public void setNombreCobertura(String v) { nombreCobertura = limpio(v); }
    public String getTipoCobertura() { return tipoCobertura; }
    public void setTipoCobertura(String v) { tipoCobertura = limpio(v); }
    public String getNumeroAfiliacion() { return numeroAfiliacion; }
    public void setNumeroAfiliacion(String v) { numeroAfiliacion = limpio(v); }
    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String v) { observaciones = limpio(v); }
    public String getEstado() { return estado; }
    public void setEstado(String v) { estado = limpio(v); }
    public boolean isTieneCuenta() { return tieneCuenta; }
    public void setTieneCuenta(boolean v) { tieneCuenta = v; }
    public Timestamp getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(Timestamp v) { fechaCreacion = v; }
    public String getUsuarioRegistro() { return usuarioRegistro; }
    public void setUsuarioRegistro(String v) { usuarioRegistro = v; }
}
