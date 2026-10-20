package modelo.repositorio;

import configuracion.ConexionBDSingleton;
import modelo.dto.HorarioDTO;
import modelo.dto.MedicoDTO;
import modelo.dto.OpcionDTO;
import modelo.entidades.Rol;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** PATRÓN: Repository  ->  IMPLEMENTACIÓN (SQL) de MedicoRepository. */
public class MedicoRepositoryImpl implements MedicoRepository {

    /** Parte desde la CUENTA (rol Médico): así aparecen también los que aún no tienen datos profesionales. */
    private static final String SELECT_MEDICO =
          "SELECT u.id_usuario, u.nombre_usuario, (u.estado = 'Activo') AS usuario_activo, "
        + "       d.nombres, d.apellidos, COALESCE(d.descripcion, '') AS descripcion, "
        + "       (f.id_datos_personales IS NOT NULL) AS tiene_foto, "
        + "       COALESCE(m.id_medico, 0) AS id_medico, COALESCE(m.id_especialidad, 0) AS id_especialidad, "
        + "       COALESCE(e.nombre, '') AS especialidad, COALESCE(m.cmp, '') AS cmp, "
        + "       COALESCE(m.consultorio, '') AS consultorio, COALESCE(m.duracion_cita, 30) AS duracion_cita, "
        + "       COALESCE(m.estado, 'Activo') AS estado "
        + "FROM tt_usuario u "
        + "JOIN tt_datos_personales d ON d.id_usuario = u.id_usuario "
        + "LEFT JOIN tt_foto_perfil f ON f.id_datos_personales = d.id_datos_personales "
        + "LEFT JOIN tt_medico m ON m.id_usuario = u.id_usuario "
        + "LEFT JOIN tm_especialidad e ON e.id_especialidad = m.id_especialidad "
        + "WHERE u.id_rol = " + Rol.MEDICO + " ";

    private Connection conexion() throws SQLException {
        return ConexionBDSingleton.getInstancia().getConexion();
    }

    private static MedicoDTO mapear(ResultSet rs) throws SQLException {
        MedicoDTO m = new MedicoDTO();
        m.setIdUsuario(rs.getInt("id_usuario"));
        m.setNombreUsuario(rs.getString("nombre_usuario"));
        m.setUsuarioActivo(rs.getBoolean("usuario_activo"));
        m.setNombres(rs.getString("nombres"));
        m.setApellidos(rs.getString("apellidos"));
        m.setDescripcion(rs.getString("descripcion"));
        m.setTieneFoto(rs.getBoolean("tiene_foto"));
        m.setIdMedico(rs.getInt("id_medico"));
        m.setIdEspecialidad(rs.getInt("id_especialidad"));
        m.setEspecialidad(rs.getString("especialidad"));
        m.setCmp(rs.getString("cmp"));
        m.setConsultorio(rs.getString("consultorio"));
        m.setDuracionCita(rs.getInt("duracion_cita"));
        m.setEstado(rs.getString("estado"));
        return m;
    }

    /** Una sola consulta trae el horario de todos los médicos de la lista (sin N+1 consultas). */
    private void cargarHorarios(Connection cn, List<MedicoDTO> medicos) throws SQLException {
        Map<Integer, MedicoDTO> porId = new LinkedHashMap<>();
        for (MedicoDTO m : medicos) {
            if (m.isConfigurado()) {
                porId.put(m.getIdMedico(), m);
            }
        }
        if (porId.isEmpty()) {
            return;
        }
        String sql = "SELECT id_medico, dia_semana, hora_inicio, hora_fin FROM tt_horario_medico "
                   + "WHERE estado = 'Activo' AND id_medico = ANY (?) ORDER BY dia_semana, hora_inicio";
        try (PreparedStatement stmt = cn.prepareStatement(sql)) {
            stmt.setArray(1, cn.createArrayOf("integer", porId.keySet().toArray()));
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    porId.get(rs.getInt(1)).getHorarios().add(new HorarioDTO(rs.getInt(2),
                            rs.getObject(3, LocalTime.class), rs.getObject(4, LocalTime.class)));
                }
            }
        }
    }

    private List<MedicoDTO> consultar(String where, Object... parametros) {
        try {
            Connection cn = conexion();
            List<MedicoDTO> lista = new ArrayList<>();
            try (PreparedStatement stmt = cn.prepareStatement(SELECT_MEDICO + where)) {
                RepositorioUtil.asignar(stmt, List.of(parametros));
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        lista.add(mapear(rs));
                    }
                }
            }
            cargarHorarios(cn, lista);
            return lista;
        } catch (SQLException e) {
            throw new RepositorioException("Error al leer los médicos", e);
        }
    }

    @Override
    public List<MedicoDTO> listarTodos() {
        return consultar("ORDER BY (m.id_medico IS NULL) DESC, d.apellidos, d.nombres");
    }

    @Override
    public List<MedicoDTO> listarQueAtienden(Integer idEspecialidad) {
        String where = "AND m.estado = 'Activo' AND u.estado = 'Activo' "
                     + "AND EXISTS (SELECT 1 FROM tt_horario_medico h WHERE h.id_medico = m.id_medico AND h.estado = 'Activo') ";
        if (idEspecialidad != null) {
            return consultar(where + "AND m.id_especialidad = ? ORDER BY d.apellidos, d.nombres", idEspecialidad);
        }
        return consultar(where + "ORDER BY e.nombre, d.apellidos, d.nombres");
    }

    @Override
    public Optional<MedicoDTO> buscarPorUsuario(int idUsuario) {
        return consultar("AND u.id_usuario = ?", idUsuario).stream().findFirst();
    }

    @Override
    public Optional<MedicoDTO> buscarPorId(int idMedico) {
        return consultar("AND m.id_medico = ?", idMedico).stream().findFirst();
    }

    @Override
    public int guardar(MedicoDTO medico, String usuario) {
        try (Connection cn = ConexionBDSingleton.getInstancia().abrirConexionTransaccional()) {
            try {
                int idMedico;
                try (PreparedStatement stmt = cn.prepareStatement(
                        "INSERT INTO tt_medico (id_usuario, id_especialidad, cmp, consultorio, duracion_cita, usuario_registro) "
                      + "VALUES (?, ?, ?, ?, ?, ?) "
                      + "ON CONFLICT (id_usuario) DO UPDATE SET id_especialidad = EXCLUDED.id_especialidad, "
                      + "    cmp = EXCLUDED.cmp, consultorio = EXCLUDED.consultorio, duracion_cita = EXCLUDED.duracion_cita, "
                      + "    usuario_registro = EXCLUDED.usuario_registro "
                      + "RETURNING id_medico")) {
                    stmt.setInt(1, medico.getIdUsuario());
                    stmt.setInt(2, medico.getIdEspecialidad());
                    stmt.setString(3, medico.getCmp());
                    stmt.setString(4, medico.getConsultorio());
                    stmt.setInt(5, medico.getDuracionCita());
                    stmt.setString(6, usuario);
                    try (ResultSet rs = stmt.executeQuery()) {
                        rs.next();
                        idMedico = rs.getInt(1);
                    }
                }
                // El horario se reemplaza completo (es una sola "planilla" semanal)
                try (PreparedStatement stmt = cn.prepareStatement("DELETE FROM tt_horario_medico WHERE id_medico = ?")) {
                    stmt.setInt(1, idMedico);
                    stmt.executeUpdate();
                }
                try (PreparedStatement stmt = cn.prepareStatement(
                        "INSERT INTO tt_horario_medico (id_medico, dia_semana, hora_inicio, hora_fin, usuario_registro) "
                      + "VALUES (?, ?, ?, ?, ?)")) {
                    for (HorarioDTO h : medico.getHorarios()) {
                        stmt.setInt(1, idMedico);
                        stmt.setInt(2, h.getDiaSemana());
                        stmt.setObject(3, h.getHoraInicio());
                        stmt.setObject(4, h.getHoraFin());
                        stmt.setString(5, usuario);
                        stmt.addBatch();
                    }
                    stmt.executeBatch();
                }
                cn.commit();
                return idMedico;
            } catch (SQLException e) {
                cn.rollback();
                if ("23505".equals(e.getSQLState()) && String.valueOf(e.getMessage()).contains("cmp")) {
                    throw new OperacionRechazadaException("Ese N.º de CMP ya está registrado para otro médico.");
                }
                throw e;
            } catch (RuntimeException e) {
                cn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new RepositorioException("Error al guardar los datos del médico", e);
        }
    }

    @Override
    public boolean cambiarEstado(int idMedico, String estado) {
        try {
            return RepositorioUtil.cambiarEstado(conexion(), "tt_medico", "id_medico", idMedico, estado);
        } catch (SQLException e) {
            throw new RepositorioException("Error al cambiar el estado del médico", e);
        }
    }

    @Override
    public List<OpcionDTO> especialidadesConMedicos() {
        String sql = "SELECT e.id_especialidad, e.nombre, COUNT(m.id_medico) AS medicos "
                   + "FROM tm_especialidad e "
                   + "JOIN tt_medico m ON m.id_especialidad = e.id_especialidad AND m.estado = 'Activo' "
                   + "JOIN tt_usuario u ON u.id_usuario = m.id_usuario AND u.estado = 'Activo' "
                   + "WHERE e.estado = 'Activo' "
                   + "  AND EXISTS (SELECT 1 FROM tt_horario_medico h WHERE h.id_medico = m.id_medico AND h.estado = 'Activo') "
                   + "GROUP BY e.id_especialidad, e.nombre ORDER BY e.nombre";
        List<OpcionDTO> lista = new ArrayList<>();
        try (PreparedStatement stmt = conexion().prepareStatement(sql); ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                lista.add(new OpcionDTO(rs.getInt(1), rs.getString(2), String.valueOf(rs.getInt(3))));
            }
            return lista;
        } catch (SQLException e) {
            throw new RepositorioException("Error al leer las especialidades", e);
        }
    }

    @Override
    public Optional<Integer> servicioDeConsulta(int idEspecialidad) {
        String sql = "SELECT ps.id_producto_servicio FROM tt_producto_servicio ps "
                   + "JOIN tm_categoria c ON c.id_categoria = ps.id_categoria "
                   + "WHERE ps.id_especialidad = ? AND ps.tipo = 'SERVICIO' AND ps.estado = 'Activo' "
                   + "ORDER BY (c.nombre = 'Consulta médica') DESC, ps.id_producto_servicio LIMIT 1";
        try {
            return RepositorioUtil.unaFila(conexion(), sql, idEspecialidad, rs -> rs.getInt(1));
        } catch (SQLException e) {
            throw new RepositorioException("Error al buscar la consulta en el tarifario", e);
        }
    }
}
