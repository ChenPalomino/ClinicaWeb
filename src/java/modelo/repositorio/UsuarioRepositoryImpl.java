package modelo.repositorio;

import configuracion.ConexionBDSingleton;
import modelo.dto.ContactoDTO;
import modelo.dto.EstadisticasUsuariosDTO;
import modelo.dto.FiltroUsuarioDTO;
import modelo.dto.PaginaDTO;
import modelo.dto.RegistroPacienteDTO;
import modelo.dto.TokenRecuperacionDTO;
import modelo.dto.UsuarioDTO;
import modelo.entidades.Rol;
import modelo.entidades.Usuario;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * PATRÓN: Repository  ->  IMPLEMENTACIÓN (Impl)
 *
 * Implementa la interfaz UsuarioRepository con SQL para PostgreSQL.
 * Es la ÚNICA clase que conoce el SQL de tt_usuario y de las tablas de
 * datos personales. El resto del sistema trabaja con objetos Usuario y DTO,
 * nunca con ResultSet ni con SQL.
 *
 * Usa ConexionBDSingleton para obtener la conexión. La conexión es
 * compartida, por eso aquí solo se cierran los PreparedStatement/ResultSet
 * (try-with-resources) y NUNCA la conexión.
 */
public class UsuarioRepositoryImpl implements UsuarioRepository {

    private Connection conexion() throws SQLException {
        return ConexionBDSingleton.getInstancia().getConexion();
    }

    // ========================= AUTENTICACIÓN =========================

    /**
     * Busca al usuario por su NOMBRE DE USUARIO o por su DNI.
     * Si coincide con ambos (pacientes, cuyo usuario ES su DNI) se prioriza
     * el nombre de usuario.
     */
    @Override
    public Usuario buscarPorNombreUsuario(String nombreUsuarioODni) {
        String sql = "SELECT u.id_usuario, u.nombre_usuario, u.contrasena, u.id_rol, u.id_permiso, "
                   + "       u.intentos_fallidos, u.bloqueado, u.ultimo_acceso, u.fecha_bloqueo, u.motivo_bloqueo "
                   + "FROM tt_usuario u "
                   + "LEFT JOIN tt_datos_personales d ON d.id_usuario = u.id_usuario "
                   + "WHERE (u.nombre_usuario = ? OR d.dni = ?) AND u.estado = 'Activo' "
                   + "ORDER BY (u.nombre_usuario = ?) DESC "
                   + "LIMIT 1";
        try (PreparedStatement stmt = conexion().prepareStatement(sql)) {
            stmt.setString(1, nombreUsuarioODni);
            stmt.setString(2, nombreUsuarioODni);
            stmt.setString(3, nombreUsuarioODni);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new Usuario.UsuarioBuilder(rs.getString("nombre_usuario"), rs.getString("contrasena"))
                            .idUsuario(rs.getInt("id_usuario"))
                            .rolId(rs.getInt("id_rol"))
                            .permisoId(rs.getInt("id_permiso"))
                            .intentosFallidos(rs.getInt("intentos_fallidos"))
                            .bloqueado(rs.getBoolean("bloqueado"))
                            .ultimoAcceso(rs.getTimestamp("ultimo_acceso"))
                            .fechaBloqueo(rs.getTimestamp("fecha_bloqueo"))
                            .motivoBloqueo(rs.getString("motivo_bloqueo"))
                            .build();
                }
                return null;
            }
        } catch (SQLException e) {
            throw new RepositorioException("Error al buscar usuario", e);
        }
    }

    @Override
    public Optional<UsuarioDTO> buscarEstadoAcceso(int idUsuario) {
        try {
            return RepositorioUtil.unaFila(conexion(),
                    "SELECT id_usuario, nombre_usuario, id_rol, estado, bloqueado FROM tt_usuario WHERE id_usuario = ?",
                    idUsuario, rs -> {
                        UsuarioDTO u = new UsuarioDTO();
                        u.setIdUsuario(rs.getInt("id_usuario"));
                        u.setNombreUsuario(rs.getString("nombre_usuario"));
                        u.setIdRol(rs.getInt("id_rol"));
                        u.setEstado(rs.getString("estado"));
                        u.setBloqueado(rs.getBoolean("bloqueado"));
                        return u;
                    });
        } catch (SQLException e) {
            throw new RepositorioException("Error al revisar la cuenta", e);
        }
    }

    @Override
    public void resetearIntentosYActualizarAcceso(int idUsuario) {
        ejecutar("UPDATE tt_usuario SET intentos_fallidos = 0, ultimo_acceso = LOCALTIMESTAMP "
               + "WHERE id_usuario = ?", idUsuario);
    }

    @Override
    public void actualizarIntentos(int idUsuario, int intentos) {
        String sql = "UPDATE tt_usuario SET intentos_fallidos = ? "
                   + "WHERE id_usuario = ?";
        try (PreparedStatement stmt = conexion().prepareStatement(sql)) {
            stmt.setInt(1, intentos);
            stmt.setInt(2, idUsuario);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RepositorioException("Error al actualizar intentos", e);
        }
    }

    @Override
    public void bloquearUsuario(int idUsuario, int intentos) {
        String sql = "UPDATE tt_usuario SET bloqueado = TRUE, intentos_fallidos = ?, "
                   + "fecha_bloqueo = LOCALTIMESTAMP, motivo_bloqueo = 'INTENTOS' WHERE id_usuario = ?";
        try (PreparedStatement stmt = conexion().prepareStatement(sql)) {
            stmt.setInt(1, intentos);
            stmt.setInt(2, idUsuario);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RepositorioException("Error al bloquear usuario", e);
        }
    }

    @Override
    public void desbloquearUsuario(int idUsuario) {
        ejecutar("UPDATE tt_usuario SET bloqueado = FALSE, intentos_fallidos = 0, fecha_bloqueo = NULL, motivo_bloqueo = NULL "
               + "WHERE id_usuario = ?", idUsuario);
    }

    @Override
    public void actualizarContrasena(int idUsuario, String hashContrasena) {
        String sql = "UPDATE tt_usuario SET contrasena = ? WHERE id_usuario = ?";
        try (PreparedStatement stmt = conexion().prepareStatement(sql)) {
            stmt.setString(1, hashContrasena);
            stmt.setInt(2, idUsuario);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RepositorioException("Error al actualizar contraseña", e);
        }
    }

    // ============================ REGISTRO ============================

    @Override
    public boolean existeNombreUsuario(String nombreUsuario) {
        return existe("SELECT 1 FROM tt_usuario WHERE nombre_usuario = ?", nombreUsuario);
    }

    @Override
    public boolean existeDni(String dni) {
        return existe("SELECT 1 FROM tt_datos_personales WHERE dni = ? AND id_usuario IS NOT NULL", dni);
    }

    @Override
    public boolean existeCorreo(String correo, String dniPropio) {
        try {
            return RepositorioUtil.existe(conexion(), "SELECT 1 FROM tt_correo c "
                    + "JOIN tt_datos_personales d ON d.id_datos_personales = c.id_datos_personales "
                    + "WHERE LOWER(c.correo_electronico) = LOWER(?) AND d.dni <> ?", correo, dniPropio);
        } catch (SQLException e) {
            throw new RepositorioException("Error al verificar duplicados", e);
        }
    }

    /**
     * Registra al paciente en 5 tablas con UNA sola sentencia SQL (CTE):
     * cuenta, datos personales, correo, celular y ficha de paciente.
     * PostgreSQL ejecuta una sentencia de forma atómica: o se insertan las
     * 5 filas o no se inserta ninguna. Así no quedan registros a medias.
     */
    @Override
    public boolean registrarPaciente(Usuario usuario, RegistroPacienteDTO datos) {
        String sql =
              "WITH nuevo_usuario AS ( "
            + "   INSERT INTO tt_usuario (nombre_usuario, contrasena, id_rol, id_permiso, intentos_fallidos, "
            + "                           bloqueado, fecha_creacion, usuario_registro, estado) "
            + "   VALUES (?, ?, ?, ?, 0, FALSE, LOCALTIMESTAMP, 'autoregistro', 'Activo') "
            + "   RETURNING id_usuario), "
            + "nuevos_datos AS ( "                // si admisión ya lo registró como paciente, se enlaza la cuenta
            + "   INSERT INTO tt_datos_personales (id_usuario, nombres, apellidos, dni, fecha_creacion, usuario_registro, estado) "
            + "   SELECT id_usuario, ?, ?, ?, LOCALTIMESTAMP, 'autoregistro', 'Activo' FROM nuevo_usuario "
            + "   ON CONFLICT (dni) DO UPDATE SET id_usuario = EXCLUDED.id_usuario "
            + "   WHERE tt_datos_personales.id_usuario IS NULL "
            + "   RETURNING id_datos_personales), "
            + "nuevo_correo AS ( "
            + "   INSERT INTO tt_correo (id_datos_personales, correo_electronico, tipo_correo, fecha_creacion, usuario_registro, estado) "
            + "   SELECT id_datos_personales, ?, 'Personal', LOCALTIMESTAMP, 'autoregistro', 'Activo' FROM nuevos_datos "
            + "   ON CONFLICT DO NOTHING), "
            + "nuevo_telefono AS ( "
            + "   INSERT INTO tt_telefono (id_datos_personales, numero_telefono, tipo_telefono, fecha_creacion, usuario_registro, estado) "
            + "   SELECT id_datos_personales, ?, 'Móvil', LOCALTIMESTAMP, 'autoregistro', 'Activo' FROM nuevos_datos) "
            + "INSERT INTO tt_paciente (id_datos_personales, usuario_registro) "     // ficha de paciente (Etapa 5)
            + "SELECT id_datos_personales, 'autoregistro' FROM nuevos_datos "
            + "ON CONFLICT (id_datos_personales) DO NOTHING";

        try (PreparedStatement stmt = conexion().prepareStatement(sql)) {
            stmt.setString(1, usuario.getNombreUsuario());
            stmt.setString(2, usuario.getContrasena());
            stmt.setInt(3, usuario.getRolId());
            stmt.setInt(4, usuario.getPermisoId());
            stmt.setString(5, datos.getNombres());
            stmt.setString(6, datos.getApellidos());
            stmt.setString(7, datos.getDni());
            stmt.setString(8, datos.getCorreo());
            stmt.setString(9, datos.getCelular());
            stmt.executeUpdate();     // la última sentencia puede insertar 0 filas si ya tenía ficha
            return true;
        } catch (SQLException e) {
            throw new RepositorioException("Error al registrar paciente", e);
        }
    }

    // ===================== RECUPERACIÓN DE CONTRASEÑA =====================

    @Override
    public ContactoDTO buscarContacto(int idUsuario) {
        String sql = "SELECT d.nombres, c.correo_electronico "
                   + "FROM tt_datos_personales d "
                   + "JOIN tt_correo c ON c.id_datos_personales = d.id_datos_personales AND c.estado = 'Activo' "
                   + "WHERE d.id_usuario = ? "
                   + "ORDER BY c.id_correo LIMIT 1";
        try (PreparedStatement stmt = conexion().prepareStatement(sql)) {
            stmt.setInt(1, idUsuario);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? new ContactoDTO(rs.getString("nombres"), rs.getString("correo_electronico")) : null;
            }
        } catch (SQLException e) {
            throw new RepositorioException("Error al buscar el correo del usuario", e);
        }
    }

    @Override
    public void guardarToken(int idUsuario, String hashToken, String tipo, int minutosValidez) {
        // Una sola sentencia: anula los anteriores del mismo tipo y registra el nuevo
        String sql = "WITH anulados AS ( "
                   + "   UPDATE tt_reseteo_contrasena SET usado = TRUE "
                   + "   WHERE id_usuario = ? AND tipo = ? AND usado = FALSE) "
                   + "INSERT INTO tt_reseteo_contrasena (id_usuario, tipo, token_hash, fecha_expiracion, "
                   + "                                   fecha_creacion, usuario_registro) "
                   + "VALUES (?, ?, ?, LOCALTIMESTAMP + make_interval(mins => ?), LOCALTIMESTAMP, 'sistema')";
        try (PreparedStatement stmt = conexion().prepareStatement(sql)) {
            stmt.setInt(1, idUsuario);
            stmt.setString(2, tipo);
            stmt.setInt(3, idUsuario);
            stmt.setString(4, tipo);
            stmt.setString(5, hashToken);
            stmt.setInt(6, minutosValidez);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RepositorioException("Error al guardar el código", e);
        }
    }

    @Override
    public TokenRecuperacionDTO obtenerToken(int idUsuario, String tipo) {
        String sql = "SELECT id_reseteo, token_hash, intentos, "
                   + "       EXTRACT(EPOCH FROM (fecha_expiracion - LOCALTIMESTAMP))::bigint AS restantes, "
                   + "       EXTRACT(EPOCH FROM (LOCALTIMESTAMP - fecha_creacion))::bigint   AS transcurridos "
                   + "FROM tt_reseteo_contrasena "
                   + "WHERE id_usuario = ? AND tipo = ? AND usado = FALSE AND estado = 'Activo' "
                   + "ORDER BY id_reseteo DESC LIMIT 1";
        try (PreparedStatement stmt = conexion().prepareStatement(sql)) {
            stmt.setInt(1, idUsuario);
            stmt.setString(2, tipo);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return new TokenRecuperacionDTO(rs.getInt("id_reseteo"), rs.getString("token_hash"),
                        rs.getInt("intentos"), rs.getLong("restantes"), rs.getLong("transcurridos"));
            }
        } catch (SQLException e) {
            throw new RepositorioException("Error al leer el código", e);
        }
    }

    @Override
    public void incrementarIntentosToken(int idReseteo) {
        ejecutar("UPDATE tt_reseteo_contrasena SET intentos = intentos + 1 WHERE id_reseteo = ?", idReseteo);
    }

    @Override
    public void marcarTokenUsado(int idReseteo) {
        ejecutar("UPDATE tt_reseteo_contrasena SET usado = TRUE WHERE id_reseteo = ?", idReseteo);
    }

    @Override
    public void cambiarContrasenaYDesbloquear(int idUsuario, String hashContrasena) {
        String sql = "WITH anulados AS ( "
                   + "   UPDATE tt_reseteo_contrasena SET usado = TRUE "
                   + "   WHERE id_usuario = ? AND tipo = 'CODIGO' AND usado = FALSE) "
                   + "UPDATE tt_usuario SET contrasena = ?, bloqueado = FALSE, intentos_fallidos = 0, "
                   + "       fecha_bloqueo = NULL, motivo_bloqueo = NULL "
                   + "WHERE id_usuario = ?";
        try (PreparedStatement stmt = conexion().prepareStatement(sql)) {
            stmt.setInt(1, idUsuario);
            stmt.setString(2, hashContrasena);
            stmt.setInt(3, idUsuario);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RepositorioException("Error al cambiar la contraseña", e);
        }
    }

    @Override
    public void bloquearPorSeguridad(int idUsuario) {
        String sql = "WITH anulados AS ( "
                   + "   UPDATE tt_reseteo_contrasena SET usado = TRUE WHERE id_usuario = ? AND usado = FALSE) "
                   + "UPDATE tt_usuario SET bloqueado = TRUE, motivo_bloqueo = 'SEGURIDAD', "
                   + "       fecha_bloqueo = LOCALTIMESTAMP WHERE id_usuario = ?";
        try (PreparedStatement stmt = conexion().prepareStatement(sql)) {
            stmt.setInt(1, idUsuario);
            stmt.setInt(2, idUsuario);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RepositorioException("Error al bloquear la cuenta", e);
        }
    }

    // ===================== CRUD DEL PANEL (Etapa 4) =====================

    /** Columnas comunes del listado y del detalle (cuenta + datos personales + 1 correo + 1 celular). */
    private static final String SELECT_DETALLE =
          "SELECT u.id_usuario, u.nombre_usuario, u.id_rol, r.nombre_rol, u.estado, u.bloqueado, "
        + "       u.motivo_bloqueo, u.intentos_fallidos, u.ultimo_acceso, u.fecha_creacion, u.usuario_registro, "
        + "       d.nombres, d.apellidos, d.dni, c.correo_electronico, t.numero_telefono, "
        + "       (f.id_datos_personales IS NOT NULL) AS tiene_foto, "
        + "       COALESCE(EXTRACT(EPOCH FROM f.fecha_actualizacion)::bigint, 0) AS foto_version "
        + "FROM tt_usuario u "
        + "JOIN tm_rol r ON r.id_rol = u.id_rol "
        + "LEFT JOIN tt_datos_personales d ON d.id_usuario = u.id_usuario "
        + "LEFT JOIN LATERAL (SELECT correo_electronico FROM tt_correo "
        + "                   WHERE id_datos_personales = d.id_datos_personales AND estado = 'Activo' "
        + "                   ORDER BY id_correo LIMIT 1) c ON TRUE "
        + "LEFT JOIN LATERAL (SELECT numero_telefono FROM tt_telefono "
        + "                   WHERE id_datos_personales = d.id_datos_personales AND estado = 'Activo' "
        + "                   ORDER BY id_telefono LIMIT 1) t ON TRUE "
        + "LEFT JOIN tt_foto_perfil f ON f.id_datos_personales = d.id_datos_personales ";

    private static UsuarioDTO mapear(ResultSet rs) throws SQLException {
        UsuarioDTO u = new UsuarioDTO();
        u.setIdUsuario(rs.getInt("id_usuario"));
        u.setNombreUsuario(rs.getString("nombre_usuario"));
        u.setIdRol(rs.getInt("id_rol"));
        u.setNombreRol(rs.getString("nombre_rol"));
        u.setEstado(rs.getString("estado"));
        u.setBloqueado(rs.getBoolean("bloqueado"));
        u.setMotivoBloqueo(rs.getString("motivo_bloqueo"));
        u.setIntentosFallidos(rs.getInt("intentos_fallidos"));
        u.setUltimoAcceso(rs.getTimestamp("ultimo_acceso"));
        u.setFechaCreacion(rs.getTimestamp("fecha_creacion"));
        u.setUsuarioRegistro(rs.getString("usuario_registro"));
        u.setNombres(rs.getString("nombres"));
        u.setApellidos(rs.getString("apellidos"));
        u.setDni(rs.getString("dni"));
        u.setCorreo(rs.getString("correo_electronico"));
        u.setCelular(rs.getString("numero_telefono"));
        u.setTieneFoto(rs.getBoolean("tiene_foto"));
        u.setFotoVersion(rs.getLong("foto_version"));
        return u;
    }

    @Override
    public PaginaDTO<UsuarioDTO> listar(FiltroUsuarioDTO filtro) {
        StringBuilder where = new StringBuilder("WHERE 1 = 1 ");
        List<Object> parametros = new ArrayList<>();

        if (filtro.getTexto() != null) {
            where.append("AND (u.nombre_usuario ILIKE ? OR d.dni ILIKE ? "
                       + "     OR (d.nombres || ' ' || d.apellidos) ILIKE ? OR c.correo_electronico ILIKE ?) ");
            String patron = "%" + filtro.getTexto().replace("%", "").replace("_", "") + "%";
            for (int i = 0; i < 4; i++) {
                parametros.add(patron);
            }
        }
        if (filtro.getIdRol() != null) {
            where.append("AND u.id_rol = ? ");
            parametros.add(filtro.getIdRol());
        }
        if (filtro.isSoloPersonal()) {
            where.append("AND u.id_rol <> 4 ");          // todo el personal: excluye a los pacientes
        }
        if ("ACTIVO".equals(filtro.getEstado())) {
            where.append("AND u.estado = 'Activo' AND u.bloqueado = FALSE ");
        } else if ("INACTIVO".equals(filtro.getEstado())) {
            where.append("AND u.estado = 'Inactivo' ");
        } else if ("BLOQUEADO".equals(filtro.getEstado())) {
            where.append("AND u.bloqueado = TRUE ");
        }

        String sqlTotal = "SELECT COUNT(*) FROM (" + SELECT_DETALLE + where + ") x";
        String sqlPagina = SELECT_DETALLE + where + "ORDER BY u.id_usuario DESC LIMIT ? OFFSET ?";

        try {
            int total;
            try (PreparedStatement stmt = conexion().prepareStatement(sqlTotal)) {
                asignar(stmt, parametros);
                try (ResultSet rs = stmt.executeQuery()) {
                    rs.next();
                    total = rs.getInt(1);
                }
            }
            List<UsuarioDTO> elementos = new ArrayList<>();
            try (PreparedStatement stmt = conexion().prepareStatement(sqlPagina)) {
                int i = asignar(stmt, parametros);
                stmt.setInt(i++, filtro.getTamanio());
                stmt.setInt(i, filtro.getOffset());
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        elementos.add(mapear(rs));
                    }
                }
            }
            return new PaginaDTO<>(elementos, total, filtro.getPagina(), filtro.getTamanio());
        } catch (SQLException e) {
            throw new RepositorioException("Error al listar usuarios", e);
        }
    }

    @Override
    public Optional<UsuarioDTO> buscarPorId(Integer idUsuario) {
        try (PreparedStatement stmt = conexion().prepareStatement(SELECT_DETALLE + "WHERE u.id_usuario = ?")) {
            stmt.setInt(1, idUsuario);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new RepositorioException("Error al buscar el usuario", e);
        }
    }

    /**
     * Crea la cuenta y sus datos personales, correo y celular en UNA
     * transacción: si algo falla, no se guarda nada (rollback).
     */
    @Override
    public Integer insertar(UsuarioDTO u) {
        try (Connection cn = ConexionBDSingleton.getInstancia().abrirConexionTransaccional()) {
            try {
                int idUsuario;
                try (PreparedStatement stmt = cn.prepareStatement(
                        "INSERT INTO tt_usuario (nombre_usuario, contrasena, id_rol, id_permiso, estado, usuario_registro) "
                      + "VALUES (?, ?, ?, ?, ?, ?) RETURNING id_usuario")) {
                    stmt.setString(1, u.getNombreUsuario());
                    stmt.setString(2, u.getContrasenaHash());
                    stmt.setInt(3, u.getIdRol());
                    stmt.setInt(4, u.getIdRol());          // permiso por defecto = el del rol
                    stmt.setString(5, u.getEstado());
                    stmt.setString(6, u.getUsuarioRegistro());
                    try (ResultSet rs = stmt.executeQuery()) {
                        rs.next();
                        idUsuario = rs.getInt(1);
                    }
                }
                guardarDatosPersonales(cn, idUsuario, u);
                cn.commit();
                return idUsuario;
            } catch (SQLException e) {
                cn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new RepositorioException("Error al crear el usuario", e);
        }
    }

    /** Actualiza cuenta + datos personales en una transacción. */
    @Override
    public boolean actualizar(UsuarioDTO u) {
        try (Connection cn = ConexionBDSingleton.getInstancia().abrirConexionTransaccional()) {
            try {
                int filas;
                try (PreparedStatement stmt = cn.prepareStatement(
                        "UPDATE tt_usuario SET nombre_usuario = ?, id_rol = ?, id_permiso = ?, estado = ? "
                      + "WHERE id_usuario = ?")) {
                    stmt.setString(1, u.getNombreUsuario());
                    stmt.setInt(2, u.getIdRol());
                    stmt.setInt(3, u.getIdRol());
                    stmt.setString(4, u.getEstado());
                    stmt.setInt(5, u.getIdUsuario());
                    filas = stmt.executeUpdate();
                }
                if (filas == 0) {
                    cn.rollback();
                    return false;
                }
                if (u.getContrasenaHash() != null) {
                    try (PreparedStatement stmt = cn.prepareStatement(
                            "UPDATE tt_usuario SET contrasena = ? WHERE id_usuario = ?")) {
                        stmt.setString(1, u.getContrasenaHash());
                        stmt.setInt(2, u.getIdUsuario());
                        stmt.executeUpdate();
                    }
                }
                guardarDatosPersonales(cn, u.getIdUsuario(), u);
                cn.commit();
                return true;
            } catch (SQLException e) {
                cn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new RepositorioException("Error al actualizar el usuario", e);
        }
    }

    /** Inserta o actualiza datos personales, correo y celular (dentro de la transacción recibida). */
    private void guardarDatosPersonales(Connection cn, int idUsuario, UsuarioDTO u) throws SQLException {
        int idDatos;
        try (PreparedStatement stmt = cn.prepareStatement(
                "INSERT INTO tt_datos_personales (id_usuario, nombres, apellidos, dni, usuario_registro) "
              + "VALUES (?, ?, ?, ?, ?) "
              + "ON CONFLICT (id_usuario) DO UPDATE SET nombres = EXCLUDED.nombres, "
              + "    apellidos = EXCLUDED.apellidos, dni = EXCLUDED.dni "
              + "RETURNING id_datos_personales")) {
            stmt.setInt(1, idUsuario);
            stmt.setString(2, u.getNombres());
            stmt.setString(3, u.getApellidos());
            stmt.setString(4, u.getDni());
            stmt.setString(5, u.getUsuarioRegistro());
            try (ResultSet rs = stmt.executeQuery()) {
                rs.next();
                idDatos = rs.getInt(1);
            }
        }
        RepositorioUtil.guardarContacto(cn, "tt_correo", "id_correo", "correo_electronico", "tipo_correo", "Personal",
                idDatos, u.getCorreo(), u.getUsuarioRegistro());
        RepositorioUtil.guardarContacto(cn, "tt_telefono", "id_telefono", "numero_telefono", "tipo_telefono", "Móvil",
                idDatos, u.getCelular(), u.getUsuarioRegistro());
        if (u.getIdRol() == Rol.PACIENTE) {
            // Una cuenta de paciente siempre tiene su ficha (N.º de historia, cobertura)
            try (PreparedStatement stmt = cn.prepareStatement(
                    "INSERT INTO tt_paciente (id_datos_personales, usuario_registro) SELECT ?, ? "
                  + "WHERE NOT EXISTS (SELECT 1 FROM tt_paciente WHERE id_datos_personales = ?)")) {
                stmt.setInt(1, idDatos);
                stmt.setString(2, u.getUsuarioRegistro());
                stmt.setInt(3, idDatos);
                stmt.executeUpdate();
            }
        }
    }

    /** Eliminación lógica: la cuenta queda Inactiva y ya no puede iniciar sesión. */
    @Override
    public boolean eliminar(Integer idUsuario) {
        return actualizarFila("UPDATE tt_usuario SET estado = 'Inactivo' WHERE id_usuario = ?", idUsuario);
    }

    @Override
    public boolean reactivar(int idUsuario) {
        return actualizarFila("UPDATE tt_usuario SET estado = 'Activo' WHERE id_usuario = ?", idUsuario);
    }

    @Override
    public void bloquearPorAdministrador(int idUsuario) {
        ejecutar("UPDATE tt_usuario SET bloqueado = TRUE, motivo_bloqueo = 'ADMINISTRADOR', "
               + "fecha_bloqueo = LOCALTIMESTAMP WHERE id_usuario = ?", idUsuario);
    }

    @Override
    public boolean existeNombreUsuarioExcepto(String nombreUsuario, int idExcluir) {
        return existeExcepto("SELECT 1 FROM tt_usuario WHERE LOWER(nombre_usuario) = LOWER(?) AND id_usuario <> ?",
                nombreUsuario, idExcluir);
    }

    @Override
    public boolean existeDniExcepto(String dni, int idExcluir) {
        return existeExcepto("SELECT 1 FROM tt_datos_personales WHERE dni = ? AND COALESCE(id_usuario, 0) <> ?",
                dni, idExcluir);
    }

    @Override
    public boolean existeCorreoExcepto(String correo, int idExcluir) {
        return existeExcepto("SELECT 1 FROM tt_correo c JOIN tt_datos_personales d "
                + "ON d.id_datos_personales = c.id_datos_personales "
                + "WHERE LOWER(c.correo_electronico) = LOWER(?) AND COALESCE(d.id_usuario, 0) <> ?",
                correo, idExcluir);
    }

    @Override
    public List<String> usuariosConCorreoDePrueba() {
        List<String> usuarios = new ArrayList<>();
        try (PreparedStatement stmt = conexion().prepareStatement(
                "SELECT u.nombre_usuario FROM tt_usuario u "
              + "JOIN tt_datos_personales d ON d.id_usuario = u.id_usuario "
              + "JOIN tt_correo c ON c.id_datos_personales = d.id_datos_personales AND c.estado = 'Activo' "
              + "WHERE u.estado = 'Activo' AND LOWER(c.correo_electronico) LIKE '%@clinica.demo' ORDER BY u.id_usuario");
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                usuarios.add(rs.getString(1));
            }
            return usuarios;
        } catch (SQLException e) {
            throw new RepositorioException("Error al revisar los correos", e);
        }
    }

    @Override
    public EstadisticasUsuariosDTO obtenerEstadisticas() {
        String sql = "SELECT COUNT(*), "
                   + "  COUNT(*) FILTER (WHERE estado = 'Activo' AND bloqueado = FALSE), "
                   + "  COUNT(*) FILTER (WHERE estado = 'Inactivo'), "
                   + "  COUNT(*) FILTER (WHERE bloqueado = TRUE), "
                   + "  COUNT(*) FILTER (WHERE id_rol = 4), "
                   + "  COUNT(*) FILTER (WHERE id_rol <> 4) "
                   + "FROM tt_usuario";
        String sqlRoles = "SELECT r.nombre_rol, COUNT(u.id_usuario) FROM tm_rol r "
                        + "LEFT JOIN tt_usuario u ON u.id_rol = r.id_rol AND u.estado = 'Activo' "
                        + "WHERE r.estado = 'Activo' GROUP BY r.id_rol, r.nombre_rol ORDER BY r.id_rol";
        String sqlDias = "SELECT to_char(d, 'DD/MM'), COUNT(u.id_usuario) "
                       + "FROM generate_series(CURRENT_DATE - 13, CURRENT_DATE, INTERVAL '1 day') AS d "
                       + "LEFT JOIN tt_usuario u ON u.fecha_creacion::date = d::date "
                       + "GROUP BY d ORDER BY d";
        try {
            EstadisticasUsuariosDTO est;
            try (PreparedStatement stmt = conexion().prepareStatement(sql); ResultSet rs = stmt.executeQuery()) {
                rs.next();
                est = new EstadisticasUsuariosDTO(rs.getInt(1), rs.getInt(2), rs.getInt(3),
                        rs.getInt(4), rs.getInt(5), rs.getInt(6));
            }
            try (PreparedStatement stmt = conexion().prepareStatement(sqlRoles); ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    est.getRolesEtiquetas().add(rs.getString(1));
                    est.getRolesValores().add(rs.getInt(2));
                }
            }
            try (PreparedStatement stmt = conexion().prepareStatement(sqlDias); ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    est.getDiasEtiquetas().add(rs.getString(1));
                    est.getDiasValores().add(rs.getInt(2));
                }
            }
            return est;
        } catch (SQLException e) {
            throw new RepositorioException("Error al calcular estadísticas", e);
        }
    }

    private int asignar(PreparedStatement stmt, List<Object> parametros) throws SQLException {
        int i = 1;
        for (Object p : parametros) {
            stmt.setObject(i++, p);
        }
        return i;
    }

    private boolean actualizarFila(String sql, int id) {
        try (PreparedStatement stmt = conexion().prepareStatement(sql)) {
            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RepositorioException("Error al actualizar el usuario", e);
        }
    }

    private boolean existeExcepto(String sql, String valor, int idExcluir) {
        try (PreparedStatement stmt = conexion().prepareStatement(sql)) {
            stmt.setString(1, valor);
            stmt.setInt(2, idExcluir);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new RepositorioException("Error al verificar duplicados", e);
        }
    }

    // ============================ AUXILIARES ============================

    private void ejecutar(String sql, int idUsuario) {
        try (PreparedStatement stmt = conexion().prepareStatement(sql)) {
            stmt.setInt(1, idUsuario);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RepositorioException("Error al actualizar usuario", e);
        }
    }

    private boolean existe(String sql, String valor) {
        try (PreparedStatement stmt = conexion().prepareStatement(sql)) {
            stmt.setString(1, valor);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new RepositorioException("Error al verificar duplicados", e);
        }
    }
}
