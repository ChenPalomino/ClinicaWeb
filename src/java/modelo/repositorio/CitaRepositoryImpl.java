package modelo.repositorio;

import configuracion.ConexionBDSingleton;
import modelo.dto.CitaDTO;
import modelo.dto.FiltroCitaDTO;
import modelo.dto.ResumenCitasDTO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

/** PATRÓN: Repository  ->  IMPLEMENTACIÓN (SQL) de CitaRepository. */
public class CitaRepositoryImpl implements CitaRepository {

    private static final String SELECT_CITA =
          "SELECT c.id_cita, c.codigo_cita, c.id_paciente, c.id_medico, c.id_especialidad, c.fecha, c.hora_inicio, "
        + "       c.hora_fin, c.motivo, c.estado_cita, c.canal, c.reprogramaciones, c.fecha_llegada, c.fecha_atencion, "
        + "       c.nota_atencion, c.motivo_cancelacion, c.fecha_cancelacion, c.cancelada_por, "
        + "       COALESCE(c.id_cuenta_atencion, 0) AS id_cuenta_atencion, c.fecha_creacion, c.usuario_registro, "
        + "       dp.nombres || ' ' || dp.apellidos AS paciente, dp.dni, p.numero_historia, "
        + "       COALESCE(dp.id_usuario, 0) AS id_usuario_paciente, dp.fecha_nacimiento, "
        + "       COALESCE(p.observaciones, '') AS observaciones, cb.nombre AS cobertura, "
        + "       dm.nombres || ' ' || dm.apellidos AS medico, m.id_usuario AS id_usuario_medico, m.consultorio, "
        + "       e.nombre AS especialidad, COALESCE(ca.estado_cuenta, '') AS estado_cuenta, "
        + "       COALESCE(ca.numero_cuenta, '') AS numero_cuenta "
        + "FROM tt_cita c "
        + "JOIN tt_paciente p ON p.id_paciente = c.id_paciente "
        + "JOIN tt_datos_personales dp ON dp.id_datos_personales = p.id_datos_personales "
        + "JOIN tm_cobertura cb ON cb.id_cobertura = p.id_cobertura "
        + "JOIN tt_medico m ON m.id_medico = c.id_medico "
        + "JOIN tt_datos_personales dm ON dm.id_usuario = m.id_usuario "
        + "JOIN tm_especialidad e ON e.id_especialidad = c.id_especialidad "
        + "LEFT JOIN tt_cuenta_atencion ca ON ca.id_cuenta_atencion = c.id_cuenta_atencion ";

    private static final String ACTIVA = "c.estado_cita IN ('RESERVADA', 'CONFIRMADA')";

    private Connection conexion() throws SQLException {
        return ConexionBDSingleton.getInstancia().getConexion();
    }

    private static CitaDTO mapear(ResultSet rs) throws SQLException {
        CitaDTO c = new CitaDTO();
        c.setIdCita(rs.getInt("id_cita"));
        c.setCodigo(rs.getString("codigo_cita"));
        c.setIdPaciente(rs.getInt("id_paciente"));
        c.setIdMedico(rs.getInt("id_medico"));
        c.setIdEspecialidad(rs.getInt("id_especialidad"));
        c.setFecha(rs.getObject("fecha", LocalDate.class));
        c.setHoraInicio(rs.getObject("hora_inicio", LocalTime.class));
        c.setHoraFin(rs.getObject("hora_fin", LocalTime.class));
        c.setMotivo(rs.getString("motivo"));
        c.setEstadoCita(rs.getString("estado_cita"));
        c.setCanal(rs.getString("canal"));
        c.setReprogramaciones(rs.getInt("reprogramaciones"));
        c.setFechaLlegada(rs.getTimestamp("fecha_llegada"));
        c.setFechaAtencion(rs.getTimestamp("fecha_atencion"));
        c.setNotaAtencion(rs.getString("nota_atencion"));
        c.setMotivoCancelacion(rs.getString("motivo_cancelacion"));
        c.setFechaCancelacion(rs.getTimestamp("fecha_cancelacion"));
        c.setCanceladaPor(rs.getString("cancelada_por"));
        c.setIdCuentaAtencion(rs.getInt("id_cuenta_atencion"));
        c.setFechaCreacion(rs.getTimestamp("fecha_creacion"));
        c.setUsuarioRegistro(rs.getString("usuario_registro"));
        c.setNombrePaciente(rs.getString("paciente"));
        c.setDniPaciente(rs.getString("dni"));
        c.setNumeroHistoria(rs.getString("numero_historia"));
        c.setIdUsuarioPaciente(rs.getInt("id_usuario_paciente"));
        c.setFechaNacimientoPaciente(rs.getObject("fecha_nacimiento", LocalDate.class));
        c.setObservacionesPaciente(rs.getString("observaciones"));
        c.setCoberturaPaciente(rs.getString("cobertura"));
        c.setNombreMedico(rs.getString("medico"));
        c.setIdUsuarioMedico(rs.getInt("id_usuario_medico"));
        c.setConsultorio(rs.getString("consultorio"));
        c.setEspecialidad(rs.getString("especialidad"));
        c.setEstadoCuenta(rs.getString("estado_cuenta"));
        c.setNumeroCuenta(rs.getString("numero_cuenta"));
        return c;
    }

    private List<CitaDTO> lista(String sql, List<Object> parametros) {
        List<CitaDTO> lista = new ArrayList<>();
        try (PreparedStatement stmt = conexion().prepareStatement(sql)) {
            RepositorioUtil.asignar(stmt, parametros);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapear(rs));
                }
            }
            return lista;
        } catch (SQLException e) {
            throw new RepositorioException("Error al leer las citas", e);
        }
    }

    /** WHERE común de la agenda (rango SIEMPRE presente: usa el índice por fecha). */
    private static String where(FiltroCitaDTO f, List<Object> parametros) {
        StringBuilder w = new StringBuilder("WHERE c.fecha BETWEEN ? AND ?");
        parametros.add(f.getDesde());
        parametros.add(f.getHasta());
        if (f.getIdMedico() != null) {
            w.append(" AND c.id_medico = ?");
            parametros.add(f.getIdMedico());
        }
        if (f.getIdEspecialidad() != null) {
            w.append(" AND c.id_especialidad = ?");
            parametros.add(f.getIdEspecialidad());
        }
        if (f.getEstado() != null) {
            w.append(" AND c.estado_cita = ?");
            parametros.add(f.getEstado());
        }
        if (f.getTexto() != null) {
            w.append(" AND (dp.dni LIKE ? OR (dp.nombres || ' ' || dp.apellidos) ILIKE ? OR dp.apellidos ILIKE ? "
                   + "OR p.numero_historia ILIKE ? OR c.codigo_cita ILIKE ?)");
            for (int i = 0; i < 5; i++) {
                parametros.add(f.getPatron());
            }
        }
        return w.toString();
    }

    @Override
    public Optional<CitaDTO> buscarPorId(int idCita) {
        String sql = "SELECT x.*, "
                   + "  (SELECT co.correo_electronico FROM tt_correo co WHERE co.id_datos_personales = x.id_dp "
                   + "     AND co.estado = 'Activo' ORDER BY co.id_correo LIMIT 1) AS correo, "
                   + "  (SELECT t.numero_telefono FROM tt_telefono t WHERE t.id_datos_personales = x.id_dp "
                   + "     AND t.estado = 'Activo' ORDER BY t.id_telefono LIMIT 1) AS celular "
                   + "FROM (" + SELECT_CITA.replace("SELECT c.id_cita,", "SELECT dp.id_datos_personales AS id_dp, c.id_cita,")
                   + "WHERE c.id_cita = ?) x";
        try {
            return RepositorioUtil.unaFila(conexion(), sql, idCita, rs -> {
                CitaDTO c = mapear(rs);
                c.setCorreoPaciente(rs.getString("correo"));
                c.setCelularPaciente(rs.getString("celular"));
                return c;
            });
        } catch (SQLException e) {
            throw new RepositorioException("Error al buscar la cita", e);
        }
    }

    @Override
    public List<CitaDTO> agenda(FiltroCitaDTO filtro) {
        List<Object> parametros = new ArrayList<>();
        String sql = SELECT_CITA + where(filtro, parametros)
                   + " ORDER BY c.fecha, c.hora_inicio, dm.apellidos LIMIT " + FiltroCitaDTO.MAXIMO_FILAS;
        return lista(sql, parametros);
    }

    @Override
    public Map<LocalDate, ResumenCitasDTO> resumenPorDia(FiltroCitaDTO filtro) {
        List<Object> parametros = new ArrayList<>();
        String sql = "SELECT c.fecha, c.estado_cita, COUNT(*) FROM tt_cita c "
                   + "JOIN tt_paciente p ON p.id_paciente = c.id_paciente "
                   + "JOIN tt_datos_personales dp ON dp.id_datos_personales = p.id_datos_personales "
                   + where(filtro, parametros) + " GROUP BY c.fecha, c.estado_cita";
        Map<LocalDate, ResumenCitasDTO> mapa = new TreeMap<>();
        try (PreparedStatement stmt = conexion().prepareStatement(sql)) {
            RepositorioUtil.asignar(stmt, parametros);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    mapa.computeIfAbsent(rs.getObject(1, LocalDate.class), k -> new ResumenCitasDTO())
                        .sumar(rs.getString(2), rs.getInt(3));
                }
            }
            return mapa;
        } catch (SQLException e) {
            throw new RepositorioException("Error al contar las citas", e);
        }
    }

    @Override
    public Map<Integer, Map<LocalDate, Set<LocalTime>>> ocupados(Collection<Integer> idMedicos, LocalDate desde, LocalDate hasta) {
        Map<Integer, Map<LocalDate, Set<LocalTime>>> mapa = new HashMap<>();
        if (idMedicos.isEmpty()) {
            return mapa;
        }
        String sql = "SELECT id_medico, fecha, hora_inicio FROM tt_cita "
                   + "WHERE id_medico = ANY (?) AND fecha BETWEEN ? AND ? AND estado_cita <> 'CANCELADA'";
        try {
            Connection cn = conexion();
            try (PreparedStatement stmt = cn.prepareStatement(sql)) {
                stmt.setArray(1, cn.createArrayOf("integer", idMedicos.toArray()));
                stmt.setObject(2, desde);
                stmt.setObject(3, hasta);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        mapa.computeIfAbsent(rs.getInt(1), k -> new HashMap<>())
                            .computeIfAbsent(rs.getObject(2, LocalDate.class), k -> new HashSet<>())
                            .add(rs.getObject(3, LocalTime.class));
                    }
                }
            }
            return mapa;
        } catch (SQLException e) {
            throw new RepositorioException("Error al leer los turnos ocupados", e);
        }
    }

    @Override
    public List<CitaDTO> activasDelPaciente(int idPaciente, LocalDate desde) {
        return lista(SELECT_CITA + "WHERE c.id_paciente = ? AND c.fecha >= ? AND " + ACTIVA
                + " ORDER BY c.fecha, c.hora_inicio", List.of(idPaciente, desde));
    }

    @Override
    public List<CitaDTO> delPaciente(int idPaciente, boolean proximas, LocalDate hoy, int limite) {
        String sql = proximas
                ? SELECT_CITA + "WHERE c.id_paciente = ? AND c.fecha >= ? AND " + ACTIVA + " ORDER BY c.fecha, c.hora_inicio LIMIT ?"
                : SELECT_CITA + "WHERE c.id_paciente = ? AND (c.fecha < ? OR NOT " + ACTIVA + ") "
                  + "ORDER BY c.fecha DESC, c.hora_inicio DESC LIMIT ?";
        return lista(sql, List.of(idPaciente, hoy, limite));
    }

    @Override
    public List<CitaDTO> activasDelMedico(int idMedico, LocalDate desde) {
        return lista(SELECT_CITA + "WHERE c.id_medico = ? AND c.fecha >= ? AND " + ACTIVA
                + " ORDER BY c.fecha, c.hora_inicio LIMIT " + FiltroCitaDTO.MAXIMO_FILAS, List.of(idMedico, desde));
    }

    @Override
    public Optional<Integer> idPacienteDeUsuario(int idUsuario) {
        String sql = "SELECT p.id_paciente FROM tt_paciente p "
                   + "JOIN tt_datos_personales d ON d.id_datos_personales = p.id_datos_personales "
                   + "WHERE d.id_usuario = ? AND p.estado = 'Activo'";
        try {
            return RepositorioUtil.unaFila(conexion(), sql, idUsuario, rs -> rs.getInt(1));
        } catch (SQLException e) {
            throw new RepositorioException("Error al buscar la ficha del paciente", e);
        }
    }

    /** El índice único rechazó el turno: otra persona lo tomó primero (RNF-08). */
    private static OperacionRechazadaException turnoTomado(SQLException e) {
        String m = String.valueOf(e.getMessage());
        if (m.contains("ux_tt_cita_paciente_horario")) {
            return new OperacionRechazadaException("El paciente ya tiene otra cita a esa misma hora.");
        }
        return new OperacionRechazadaException("Ese horario acaba de ser reservado por otra persona. Elige otro, por favor.");
    }

    @Override
    public NuevaCita reservar(CitaDTO c, String usuario) {
        String sql = "INSERT INTO tt_cita (id_paciente, id_medico, id_especialidad, fecha, hora_inicio, hora_fin, motivo, "
                   + "                     canal, usuario_registro) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) "
                   + "RETURNING id_cita, codigo_cita";
        try (PreparedStatement stmt = conexion().prepareStatement(sql)) {
            stmt.setInt(1, c.getIdPaciente());
            stmt.setInt(2, c.getIdMedico());
            stmt.setInt(3, c.getIdEspecialidad());
            stmt.setObject(4, c.getFecha());
            stmt.setObject(5, c.getHoraInicio());
            stmt.setObject(6, c.getHoraFin());
            RepositorioUtil.textoONulo(stmt, 7, c.getMotivo());
            stmt.setString(8, c.getCanal());
            stmt.setString(9, usuario);
            try (ResultSet rs = stmt.executeQuery()) {
                rs.next();
                return new NuevaCita(rs.getInt(1), rs.getString(2));
            }
        } catch (SQLException e) {
            if ("23505".equals(e.getSQLState())) {
                throw turnoTomado(e);
            }
            throw new RepositorioException("Error al reservar la cita", e);
        }
    }

    @Override
    public boolean reprogramar(int idCita, int idMedico, LocalDate fecha, LocalTime inicio, LocalTime fin, String usuario) {
        String sql = "UPDATE tt_cita SET id_medico = ?, fecha = ?, hora_inicio = ?, hora_fin = ?, "
                   + "       reprogramaciones = reprogramaciones + 1, usuario_registro = ? "
                   + "WHERE id_cita = ? AND estado_cita = 'RESERVADA'";
        try (PreparedStatement stmt = conexion().prepareStatement(sql)) {
            stmt.setInt(1, idMedico);
            stmt.setObject(2, fecha);
            stmt.setObject(3, inicio);
            stmt.setObject(4, fin);
            stmt.setString(5, usuario);
            stmt.setInt(6, idCita);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            if ("23505".equals(e.getSQLState())) {
                throw turnoTomado(e);
            }
            throw new RepositorioException("Error al reprogramar la cita", e);
        }
    }

    private boolean actualizar(String sql, Object... parametros) {
        try (PreparedStatement stmt = conexion().prepareStatement(sql)) {
            RepositorioUtil.asignar(stmt, java.util.Arrays.asList(parametros));   // admite null (nota vacía)
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RepositorioException("Error al actualizar la cita", e);
        }
    }

    @Override
    public boolean cancelar(int idCita, String motivo, String usuario) {
        return actualizar("UPDATE tt_cita c SET estado_cita = 'CANCELADA', motivo_cancelacion = ?, "
                + "fecha_cancelacion = CURRENT_TIMESTAMP, cancelada_por = ? WHERE c.id_cita = ? AND " + ACTIVA,
                motivo, usuario, idCita);
    }

    @Override
    public boolean registrarLlegada(int idCita, String usuario) {
        return actualizar("UPDATE tt_cita SET estado_cita = 'CONFIRMADA', fecha_llegada = CURRENT_TIMESTAMP, "
                + "usuario_registro = ? WHERE id_cita = ? AND estado_cita = 'RESERVADA'", usuario, idCita);
    }

    @Override
    public boolean registrarAtencion(int idCita, String estado, String nota, String usuario) {
        return actualizar("UPDATE tt_cita c SET estado_cita = ?, fecha_atencion = CURRENT_TIMESTAMP, nota_atencion = ?, "
                + "usuario_registro = ? WHERE c.id_cita = ? AND " + ACTIVA,
                estado, nota == null || nota.isBlank() ? null : nota, usuario, idCita);
    }

    @Override
    public boolean vincularCuenta(int idCita, int idCuenta) {
        return actualizar("UPDATE tt_cita SET id_cuenta_atencion = ? WHERE id_cita = ?", idCuenta, idCita);
    }
}
