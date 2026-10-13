package modelo.repositorio;

import configuracion.ConexionBDSingleton;
import modelo.dto.FotoDTO;
import modelo.dto.PerfilDTO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

/** PATRÓN: Repository  ->  IMPLEMENTACIÓN (SQL) de PerfilRepository. */
public class PerfilRepositoryImpl implements PerfilRepository {

    private Connection conexion() throws SQLException {
        return ConexionBDSingleton.getInstancia().getConexion();
    }

    @Override
    public Optional<PerfilDTO> obtener(int idUsuario) {
        String sql = "SELECT u.id_usuario, u.nombre_usuario, u.id_rol, u.preferencia_tema, "
                   + "       d.nombres, d.apellidos, d.dni, d.descripcion, c.correo_electronico, t.numero_telefono, "
                   + "       (f.id_datos_personales IS NOT NULL) AS tiene_foto, "
                   + "       COALESCE(EXTRACT(EPOCH FROM f.fecha_actualizacion)::bigint, 0) AS foto_version "
                   + "FROM tt_usuario u "
                   + "LEFT JOIN tt_datos_personales d ON d.id_usuario = u.id_usuario "
                   + "LEFT JOIN LATERAL (SELECT correo_electronico FROM tt_correo "
                   + "                   WHERE id_datos_personales = d.id_datos_personales AND estado = 'Activo' "
                   + "                   ORDER BY id_correo LIMIT 1) c ON TRUE "
                   + "LEFT JOIN LATERAL (SELECT numero_telefono FROM tt_telefono "
                   + "                   WHERE id_datos_personales = d.id_datos_personales AND estado = 'Activo' "
                   + "                   ORDER BY id_telefono LIMIT 1) t ON TRUE "
                   + "LEFT JOIN tt_foto_perfil f ON f.id_datos_personales = d.id_datos_personales "
                   + "WHERE u.id_usuario = ?";
        try (PreparedStatement stmt = conexion().prepareStatement(sql)) {
            stmt.setInt(1, idUsuario);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    return Optional.empty();
                }
                PerfilDTO p = new PerfilDTO();
                p.setIdUsuario(rs.getInt("id_usuario"));
                p.setNombreUsuario(rs.getString("nombre_usuario"));
                p.setIdRol(rs.getInt("id_rol"));
                p.setNombres(rs.getString("nombres"));
                p.setApellidos(rs.getString("apellidos"));
                p.setDni(rs.getString("dni"));
                p.setDescripcion(rs.getString("descripcion"));
                p.setCorreo(rs.getString("correo_electronico"));
                p.setCelular(rs.getString("numero_telefono"));
                p.setTieneFoto(rs.getBoolean("tiene_foto"));
                p.setFotoVersion(rs.getLong("foto_version"));
                p.setTema(rs.getString("preferencia_tema"));
                return Optional.of(p);
            }
        } catch (SQLException e) {
            throw new RepositorioException("Error al leer el perfil", e);
        }
    }

    @Override
    public void actualizarDatos(PerfilDTO p, String usuarioRegistro) {
        try (Connection cn = ConexionBDSingleton.getInstancia().abrirConexionTransaccional()) {
            try {
                int idDatos;
                try (PreparedStatement stmt = cn.prepareStatement(
                        "INSERT INTO tt_datos_personales (id_usuario, nombres, apellidos, dni, descripcion, usuario_registro) "
                      + "VALUES (?, ?, ?, ?, ?, ?) "
                      + "ON CONFLICT (id_usuario) DO UPDATE SET nombres = EXCLUDED.nombres, apellidos = EXCLUDED.apellidos, "
                      + "    dni = EXCLUDED.dni, descripcion = EXCLUDED.descripcion "
                      + "RETURNING id_datos_personales")) {
                    stmt.setInt(1, p.getIdUsuario());
                    stmt.setString(2, p.getNombres());
                    stmt.setString(3, p.getApellidos());
                    stmt.setString(4, p.getDni());
                    stmt.setString(5, p.getDescripcion().isBlank() ? null : p.getDescripcion());
                    stmt.setString(6, usuarioRegistro);
                    try (ResultSet rs = stmt.executeQuery()) {
                        rs.next();
                        idDatos = rs.getInt(1);
                    }
                }
                RepositorioUtil.guardarContacto(cn, "tt_correo", "id_correo", "correo_electronico", "tipo_correo",
                        "Personal", idDatos, p.getCorreo(), usuarioRegistro);
                RepositorioUtil.guardarContacto(cn, "tt_telefono", "id_telefono", "numero_telefono", "tipo_telefono",
                        "Móvil", idDatos, p.getCelular(), usuarioRegistro);
                cn.commit();
            } catch (SQLException e) {
                cn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new RepositorioException("Error al guardar el perfil", e);
        }
    }

    @Override
    public boolean guardarFoto(int idUsuario, byte[] imagen, String tipoMime, String usuarioRegistro) {
        String sql = "INSERT INTO tt_foto_perfil (id_datos_personales, imagen, tipo_mime, usuario_registro) "
                   + "SELECT id_datos_personales, ?, ?, ? FROM tt_datos_personales WHERE id_usuario = ? "
                   + "ON CONFLICT (id_datos_personales) DO UPDATE SET imagen = EXCLUDED.imagen, tipo_mime = EXCLUDED.tipo_mime";
        try (PreparedStatement stmt = conexion().prepareStatement(sql)) {
            stmt.setBytes(1, imagen);
            stmt.setString(2, tipoMime);
            stmt.setString(3, usuarioRegistro);
            stmt.setInt(4, idUsuario);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RepositorioException("Error al guardar la foto", e);
        }
    }

    @Override
    public Optional<FotoDTO> obtenerFoto(int idUsuario) {
        String sql = "SELECT f.imagen, f.tipo_mime FROM tt_foto_perfil f "
                   + "JOIN tt_datos_personales d ON d.id_datos_personales = f.id_datos_personales "
                   + "WHERE d.id_usuario = ? AND f.estado = 'Activo'";
        try (PreparedStatement stmt = conexion().prepareStatement(sql)) {
            stmt.setInt(1, idUsuario);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? Optional.of(new FotoDTO(rs.getBytes("imagen"), rs.getString("tipo_mime"))) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new RepositorioException("Error al leer la foto", e);
        }
    }

    @Override
    public void guardarTema(int idUsuario, String tema) {
        try (PreparedStatement stmt = conexion().prepareStatement(
                "UPDATE tt_usuario SET preferencia_tema = ? WHERE id_usuario = ?")) {
            stmt.setString(1, tema);
            stmt.setInt(2, idUsuario);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RepositorioException("Error al guardar la apariencia", e);
        }
    }

    @Override
    public void eliminarFoto(int idUsuario) {
        String sql = "DELETE FROM tt_foto_perfil WHERE id_datos_personales = "
                   + "(SELECT id_datos_personales FROM tt_datos_personales WHERE id_usuario = ?)";
        try (PreparedStatement stmt = conexion().prepareStatement(sql)) {
            stmt.setInt(1, idUsuario);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RepositorioException("Error al quitar la foto", e);
        }
    }
}
