package modelo.repositorio;

import configuracion.ConexionBDSingleton;
import modelo.dto.EspecialidadDTO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class EspecialidadRepositoryImpl implements EspecialidadRepository {

    private static final String SELECT_ESPECIALIDAD =
          "SELECT e.id_especialidad, e.nombre, COALESCE(e.descripcion, '') AS descripcion, e.estado, "
        + "       (SELECT COUNT(*) FROM tt_medico m JOIN tt_datos_personales dp ON dp.id_datos_personales = m.id_datos_personales "
        + "         JOIN tt_usuario u ON u.id_usuario = dp.id_usuario "
        + "         WHERE m.id_especialidad = e.id_especialidad AND m.estado = 'Activo' AND u.estado = 'Activo') AS medicos_activos, "
        + "       (SELECT COUNT(*) FROM tt_medico m WHERE m.id_especialidad = e.id_especialidad) AS medicos_total, "
        + "       cs.id_producto_servicio, cs.codigo, cs.precio, e.fecha_creacion, COALESCE(e.usuario_registro, '') AS usuario_registro "
        + "FROM tm_especialidad e "
        + "LEFT JOIN LATERAL (SELECT ps.id_producto_servicio, ps.codigo, ps.precio FROM vw_producto_servicio ps "
        + "                   JOIN tm_categoria c ON c.id_categoria = ps.id_categoria "
        + "                   WHERE ps.id_especialidad = e.id_especialidad AND ps.tipo = 'SERVICIO' AND ps.estado = 'Activo' "
        + "                   ORDER BY (c.nombre = 'Consulta médica') DESC, ps.id_producto_servicio LIMIT 1) cs ON TRUE ";

    private Connection conexion() throws SQLException {
        return ConexionBDSingleton.getInstancia().getConexion();
    }

    private static EspecialidadDTO mapear(ResultSet rs) throws SQLException {
        EspecialidadDTO e = new EspecialidadDTO();
        e.setIdEspecialidad(rs.getInt("id_especialidad"));
        e.setNombre(rs.getString("nombre"));
        e.setDescripcion(rs.getString("descripcion"));
        e.setEstado(rs.getString("estado"));
        e.setMedicosActivos(rs.getInt("medicos_activos"));
        e.setMedicosTotal(rs.getInt("medicos_total"));
        e.setIdServicioConsulta(RepositorioUtil.enteroONulo(rs, "id_producto_servicio"));
        e.setCodigoConsulta(rs.getString("codigo"));
        e.setPrecioConsulta(rs.getBigDecimal("precio"));
        e.setFechaCreacion(rs.getTimestamp("fecha_creacion"));
        e.setUsuarioRegistro(rs.getString("usuario_registro"));
        return e;
    }

    @Override
    public List<EspecialidadDTO> listar(String texto, String estado) {
        StringBuilder where = new StringBuilder("WHERE 1 = 1");
        List<Object> parametros = new ArrayList<>();
        if (texto != null && !texto.isBlank()) {
            where.append(" AND e.nombre ILIKE ?");
            parametros.add("%" + texto.trim().replace("%", "").replace("_", "") + "%");
        }
        if ("ACTIVO".equals(estado)) {
            where.append(" AND e.estado = 'Activo'");
        } else if ("INACTIVO".equals(estado)) {
            where.append(" AND e.estado = 'Inactivo'");
        }
        List<EspecialidadDTO> lista = new ArrayList<>();
        try (PreparedStatement stmt = conexion().prepareStatement(SELECT_ESPECIALIDAD + where + " ORDER BY e.estado, e.nombre")) {
            RepositorioUtil.asignar(stmt, parametros);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapear(rs));
                }
            }
            return lista;
        } catch (SQLException e) {
            throw new RepositorioException("Error al listar las especialidades", e);
        }
    }

    @Override
    public Optional<EspecialidadDTO> buscarPorId(int id) {
        try {
            return RepositorioUtil.unaFila(conexion(), SELECT_ESPECIALIDAD + "WHERE e.id_especialidad = ?", id,
                    EspecialidadRepositoryImpl::mapear);
        } catch (SQLException e) {
            throw new RepositorioException("Error al buscar la especialidad", e);
        }
    }

    @Override
    public boolean existeNombre(String nombre, int idExcluir) {
        try {
            return RepositorioUtil.existe(conexion(),
                    "SELECT 1 FROM tm_especialidad WHERE LOWER(nombre) = LOWER(?) AND id_especialidad <> ?", nombre, idExcluir);
        } catch (SQLException e) {
            throw new RepositorioException("Error al verificar el nombre", e);
        }
    }

    @Override
    public int insertar(EspecialidadDTO e) {
        String sql = "INSERT INTO tm_especialidad (nombre, descripcion, estado, usuario_registro) VALUES (?, ?, ?, ?) "
                   + "RETURNING id_especialidad";
        try (PreparedStatement stmt = conexion().prepareStatement(sql)) {
            stmt.setString(1, e.getNombre());
            RepositorioUtil.textoONulo(stmt, 2, e.getDescripcion());
            stmt.setString(3, e.getEstado());
            stmt.setString(4, e.getUsuarioRegistro());
            try (ResultSet rs = stmt.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        } catch (SQLException ex) {
            throw new RepositorioException("Error al registrar la especialidad", ex);
        }
    }

    @Override
    public boolean actualizar(EspecialidadDTO e) {
        String sql = "UPDATE tm_especialidad SET nombre = ?, descripcion = ?, estado = ? WHERE id_especialidad = ?";
        try (PreparedStatement stmt = conexion().prepareStatement(sql)) {
            stmt.setString(1, e.getNombre());
            RepositorioUtil.textoONulo(stmt, 2, e.getDescripcion());
            stmt.setString(3, e.getEstado());
            stmt.setInt(4, e.getIdEspecialidad());
            return stmt.executeUpdate() > 0;
        } catch (SQLException ex) {
            throw new RepositorioException("Error al actualizar la especialidad", ex);
        }
    }

    @Override
    public boolean cambiarEstado(int id, String estado) {
        try {
            return RepositorioUtil.cambiarEstado(conexion(), "tm_especialidad", "id_especialidad", id, estado);
        } catch (SQLException e) {
            throw new RepositorioException("Error al cambiar el estado de la especialidad", e);
        }
    }
}
