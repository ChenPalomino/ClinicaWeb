package modelo.dto;

/**
 * Datos del usuario que inició sesión, para "Mi perfil" y para mostrar su
 * nombre y foto en el dashboard (en lugar del DNI o del nombre de usuario).
 */
public class PerfilDTO {

    private int idUsuario;
    private String nombreUsuario = "";
    private int idRol;
    private String nombres = "";
    private String apellidos = "";
    private String dni = "";
    private String correo = "";
    private String celular = "";
    private String descripcion = "";
    private boolean tieneFoto;
    private long fotoVersion;
    private String tema = "claro";
    private Integer idSexo;
    private java.time.LocalDate fechaNacimiento;
    private String numeroHistoria = "";   // solo si la persona es paciente
    private String nombreCobertura = "";      // preferencia personal: claro | oscuro | sistema

    /** Nombre para saludar: el primer nombre, o el usuario si aún no tiene datos personales. */
    public String getNombreMostrar() {
        String n = nombres.trim();
        return n.isEmpty() ? nombreUsuario : n.split("\\s+")[0];
    }

    private static final java.util.Set<String> PARTICULAS =
            java.util.Set.of("de", "del", "la", "las", "los", "y", "san", "da", "di", "van", "von");

    /**
     * "Juan Pérez" (primer nombre + primer apellido) para la barra superior.
     * Respeta apellidos compuestos: "María de la Cruz Rojas" -> "María de la Cruz".
     */
    public String getNombreCorto() {
        String a = apellidos.trim();
        if (a.isEmpty()) {
            return getNombreMostrar();
        }
        StringBuilder primero = new StringBuilder();
        for (String palabra : a.split("\\s+")) {
            primero.append(primero.length() == 0 ? "" : " ").append(palabra);
            if (!PARTICULAS.contains(palabra.toLowerCase())) {
                break;
            }
        }
        return getNombreMostrar() + " " + primero;
    }

    public String getNombreCompleto() {
        String c = (nombres + " " + apellidos).trim();
        return c.isEmpty() ? nombreUsuario : c;
    }

    public String getIniciales() {
        String a = nombres.isBlank() ? nombreUsuario : nombres.trim();
        String b = apellidos.trim();
        return ((a.isEmpty() ? "?" : a.substring(0, 1)) + (b.isEmpty() ? "" : b.substring(0, 1))).toUpperCase();
    }

    public boolean isTieneDatosPersonales() { return !dni.isBlank(); }

    // ---------------- getters / setters ----------------
    public int getIdUsuario() { return idUsuario; }
    public void setIdUsuario(int idUsuario) { this.idUsuario = idUsuario; }
    public String getNombreUsuario() { return nombreUsuario; }
    public void setNombreUsuario(String v) { this.nombreUsuario = v == null ? "" : v; }
    public int getIdRol() { return idRol; }
    public void setIdRol(int idRol) { this.idRol = idRol; }
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
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String v) { this.descripcion = v == null ? "" : v; }
    public boolean isTieneFoto() { return tieneFoto; }
    public void setTieneFoto(boolean tieneFoto) { this.tieneFoto = tieneFoto; }
    public long getFotoVersion() { return fotoVersion; }
    public void setFotoVersion(long fotoVersion) { this.fotoVersion = fotoVersion; }
    public String getTema() { return tema; }
    public void setTema(String tema) { this.tema = tema == null ? "claro" : tema; }
    public Integer getIdSexo() { return idSexo; }
    public void setIdSexo(Integer v) { this.idSexo = v; }
    public java.time.LocalDate getFechaNacimiento() { return fechaNacimiento; }
    public void setFechaNacimiento(java.time.LocalDate v) { this.fechaNacimiento = v; }
    public String getNumeroHistoria() { return numeroHistoria; }
    public void setNumeroHistoria(String v) { this.numeroHistoria = v == null ? "" : v; }
    public String getNombreCobertura() { return nombreCobertura; }
    public void setNombreCobertura(String v) { this.nombreCobertura = v == null ? "" : v; }
    public boolean isPaciente() { return !numeroHistoria.isEmpty(); }

    /** Edad en años cumplidos, o null si no registró su fecha de nacimiento. */
    public Integer getEdad() {
        return fechaNacimiento == null ? null : java.time.Period.between(fechaNacimiento, java.time.LocalDate.now()).getYears();
    }
}
