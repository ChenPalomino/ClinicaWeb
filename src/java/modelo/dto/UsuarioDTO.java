package modelo.dto;

import java.sql.Timestamp;

/**
 * Datos completos de un usuario para el CRUD del panel de administración:
 * cuenta (tt_usuario) + datos personales, correo y celular.
 * Se usa para listar, para mostrar el formulario y para guardar.
 */
public class UsuarioDTO {

    // ---- Cuenta (tt_usuario) ----
    private int idUsuario;
    private String nombreUsuario = "";
    private int idRol = 4;
    private String nombreRol = "";
    private String estado = "Activo";
    private boolean bloqueado;
    private String motivoBloqueo;
    private int intentosFallidos;
    private Timestamp ultimoAcceso;
    private Timestamp fechaCreacion;
    /** Hash de la contraseña. Solo se llena al crear o al restablecer la contraseña. */
    private String contrasenaHash;
    private String usuarioRegistro;

    // ---- Datos personales ----
    private String nombres = "";
    private String apellidos = "";
    private String dni = "";
    private String correo = "";
    private String celular = "";

    // ---- Foto de perfil ----
    private boolean tieneFoto;
    private long fotoVersion;   // cambia cada vez que se actualiza la foto (evita caché vieja)

    public boolean isNuevo() { return idUsuario == 0; }
    public boolean isActivo() { return "Activo".equals(estado); }

    public String getNombreCompleto() {
        String completo = (nombres + " " + apellidos).trim();
        return completo.isEmpty() ? "(sin datos personales)" : completo;
    }

    /** Iniciales para el avatar del listado (ej. "JR"). */
    public String getIniciales() {
        String a = nombres.isBlank() ? nombreUsuario : nombres;
        String b = apellidos.isBlank() ? "" : apellidos;
        return ((a.isEmpty() ? "" : a.substring(0, 1)) + (b.isEmpty() ? "" : b.substring(0, 1))).toUpperCase();
    }

    // ---------------- getters / setters ----------------
    public int getIdUsuario() { return idUsuario; }
    public void setIdUsuario(int idUsuario) { this.idUsuario = idUsuario; }
    public String getNombreUsuario() { return nombreUsuario; }
    public void setNombreUsuario(String v) { this.nombreUsuario = v == null ? "" : v; }
    public int getIdRol() { return idRol; }
    public void setIdRol(int idRol) { this.idRol = idRol; }
    public String getNombreRol() { return nombreRol; }
    public void setNombreRol(String v) { this.nombreRol = v == null ? "" : v; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public boolean isBloqueado() { return bloqueado; }
    public void setBloqueado(boolean bloqueado) { this.bloqueado = bloqueado; }
    public String getMotivoBloqueo() { return motivoBloqueo; }
    public void setMotivoBloqueo(String motivoBloqueo) { this.motivoBloqueo = motivoBloqueo; }
    public int getIntentosFallidos() { return intentosFallidos; }
    public void setIntentosFallidos(int intentosFallidos) { this.intentosFallidos = intentosFallidos; }
    public Timestamp getUltimoAcceso() { return ultimoAcceso; }
    public void setUltimoAcceso(Timestamp ultimoAcceso) { this.ultimoAcceso = ultimoAcceso; }
    public Timestamp getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(Timestamp fechaCreacion) { this.fechaCreacion = fechaCreacion; }
    public String getContrasenaHash() { return contrasenaHash; }
    public void setContrasenaHash(String contrasenaHash) { this.contrasenaHash = contrasenaHash; }
    public String getUsuarioRegistro() { return usuarioRegistro; }
    public void setUsuarioRegistro(String usuarioRegistro) { this.usuarioRegistro = usuarioRegistro; }
    public String getNombres() { return nombres; }
    public void setNombres(String v) { this.nombres = v == null ? "" : v; }
    public String getApellidos() { return apellidos; }
    public void setApellidos(String v) { this.apellidos = v == null ? "" : v; }
    public String getDni() { return dni; }
    public void setDni(String v) { this.dni = v == null ? "" : v; }
    public String getCorreo() { return correo; }
    public void setCorreo(String v) { this.correo = v == null ? "" : v; }
    public String getCelular() { return celular; }
    public void setCelular(String v) { this.celular = v == null ? "" : v; }
    public boolean isTieneFoto() { return tieneFoto; }
    public void setTieneFoto(boolean tieneFoto) { this.tieneFoto = tieneFoto; }
    public long getFotoVersion() { return fotoVersion; }
    public void setFotoVersion(long fotoVersion) { this.fotoVersion = fotoVersion; }
}
