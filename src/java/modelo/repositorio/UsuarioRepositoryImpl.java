package modelo.repositorio;

import configuracion.ConexionBDSingleton;
import modelo.dto.ContactoDTO;
import modelo.dto.RegistroPacienteDTO;
import modelo.dto.TokenRecuperacionDTO;
import modelo.entidades.Usuario;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

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
        return existe("SELECT 1 FROM tt_datos_personales WHERE dni = ?", dni);
    }

    @Override
    public boolean existeCorreo(String correo) {
        return existe("SELECT 1 FROM tt_correo WHERE LOWER(correo_electronico) = LOWER(?)", correo);
    }

    /**
     * Registra al paciente en 4 tablas con UNA sola sentencia SQL (CTE).
     * PostgreSQL ejecuta una sentencia de forma atómica: o se insertan las
     * 4 filas o no se inserta ninguna. Así no quedan registros a medias.
     */
    @Override
    public boolean registrarPaciente(Usuario usuario, RegistroPacienteDTO datos) {
        String sql =
              "WITH nuevo_usuario AS ( "
            + "   INSERT INTO tt_usuario (nombre_usuario, contrasena, id_rol, id_permiso, intentos_fallidos, "
            + "                           bloqueado, fecha_creacion, usuario_registro, estado) "
            + "   VALUES (?, ?, ?, ?, 0, FALSE, LOCALTIMESTAMP, 'autoregistro', 'Activo') "
            + "   RETURNING id_usuario), "
            + "nuevos_datos AS ( "
            + "   INSERT INTO tt_datos_personales (id_usuario, nombres, apellidos, dni, fecha_creacion, usuario_registro, estado) "
            + "   SELECT id_usuario, ?, ?, ?, LOCALTIMESTAMP, 'autoregistro', 'Activo' FROM nuevo_usuario "
            + "   RETURNING id_datos_personales), "
            + "nuevo_correo AS ( "
            + "   INSERT INTO tt_correo (id_datos_personales, correo_electronico, tipo_correo, fecha_creacion, usuario_registro, estado) "
            + "   SELECT id_datos_personales, ?, 'Personal', LOCALTIMESTAMP, 'autoregistro', 'Activo' FROM nuevos_datos) "
            + "INSERT INTO tt_telefono (id_datos_personales, numero_telefono, tipo_telefono, fecha_creacion, usuario_registro, estado) "
            + "SELECT id_datos_personales, ?, 'Móvil', LOCALTIMESTAMP, 'autoregistro', 'Activo' FROM nuevos_datos";

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
            return stmt.executeUpdate() > 0;
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
