package modelo.entidades;

import java.sql.Timestamp;

/**
 * Entidad que representa una fila de la tabla tt_usuario.
 *
 * PATRÓN: Builder (creacional) -> clase interna Usuario.UsuarioBuilder.
 * Propósito: construir el objeto paso a paso, con valores por defecto
 * seguros y sin un constructor de 10 parámetros.
 */
public class Usuario {

    private final int idUsuario;
    private final String nombreUsuario;
    private final String contrasena;      // HASH de la contraseña, nunca el texto plano
    private final int rolId;
    private final int permisoId;
    private final int intentosFallidos;
    private final boolean bloqueado;
    private final Timestamp ultimoAcceso;
    private final Timestamp fechaBloqueo;
    private final String motivoBloqueo;   // INTENTOS | SEGURIDAD | ADMINISTRADOR

    private Usuario(UsuarioBuilder builder) {
        this.idUsuario = builder.idUsuario;
        this.nombreUsuario = builder.nombreUsuario;
        this.contrasena = builder.contrasena;
        this.rolId = builder.rolId;
        this.permisoId = builder.permisoId;
        this.intentosFallidos = builder.intentosFallidos;
        this.bloqueado = builder.bloqueado;
        this.ultimoAcceso = builder.ultimoAcceso;
        this.fechaBloqueo = builder.fechaBloqueo;
        this.motivoBloqueo = builder.motivoBloqueo;
    }

    public int getIdUsuario() { return idUsuario; }
    public String getNombreUsuario() { return nombreUsuario; }
    public String getContrasena() { return contrasena; }
    public int getRolId() { return rolId; }
    public int getPermisoId() { return permisoId; }
    public int getIntentosFallidos() { return intentosFallidos; }
    public boolean isBloqueado() { return bloqueado; }
    public Timestamp getUltimoAcceso() { return ultimoAcceso; }
    public Timestamp getFechaBloqueo() { return fechaBloqueo; }
    public String getMotivoBloqueo() { return motivoBloqueo; }

    /** true si el bloqueo es el temporal por 3 intentos (se libera a las 24 h o con código). */
    public boolean isBloqueoPorIntentos() {
        return bloqueado && "INTENTOS".equals(motivoBloqueo) && fechaBloqueo != null;
    }

    /** Nombre legible del rol (coincide con tm_rol). */
    public String getNombreRol() {
        switch (rolId) {
            case 1: return "Administrador";
            case 2: return "Recepcionista";
            case 3: return "Médico";
            case 4: return "Paciente";
            default: return "Sin rol";
        }
    }

    // =================== PATRÓN BUILDER ===================
    public static class UsuarioBuilder {

        // Obligatorios
        private final String nombreUsuario;
        private final String contrasena;

        // Opcionales con valores por defecto SEGUROS
        private int idUsuario;
        private int rolId = 4;        // 4 = Paciente
        private int permisoId = 4;    // 4 = PORTAL_PACIENTE (antes era 1 = ACCESO_TOTAL)
        private int intentosFallidos = 0;
        private boolean bloqueado = false;
        private Timestamp ultimoAcceso;
        private Timestamp fechaBloqueo;
        private String motivoBloqueo;

        public UsuarioBuilder(String nombreUsuario, String contrasena) {
            this.nombreUsuario = nombreUsuario;
            this.contrasena = contrasena;
        }

        public UsuarioBuilder idUsuario(int idUsuario) { this.idUsuario = idUsuario; return this; }
        public UsuarioBuilder rolId(int rolId) { this.rolId = rolId; return this; }
        public UsuarioBuilder permisoId(int permisoId) { this.permisoId = permisoId; return this; }
        public UsuarioBuilder intentosFallidos(int intentos) { this.intentosFallidos = intentos; return this; }
        public UsuarioBuilder bloqueado(boolean bloqueado) { this.bloqueado = bloqueado; return this; }
        public UsuarioBuilder ultimoAcceso(Timestamp ultimoAcceso) { this.ultimoAcceso = ultimoAcceso; return this; }
        public UsuarioBuilder fechaBloqueo(Timestamp fechaBloqueo) { this.fechaBloqueo = fechaBloqueo; return this; }
        public UsuarioBuilder motivoBloqueo(String motivoBloqueo) { this.motivoBloqueo = motivoBloqueo; return this; }

        public Usuario build() {
            return new Usuario(this);
        }
    }
}
