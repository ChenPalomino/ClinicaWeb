package modelo.repositorio;

import configuracion.ConexionBDSingleton;
import modelo.dto.FiltroBusquedaDTO;
import modelo.dto.PacienteDTO;
import modelo.dto.PaginaDTO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * PATRÓN: Repository  ->  IMPLEMENTACIÓN (SQL) de PacienteRepository.
 *
 * Un paciente = una persona (tt_datos_personales, con su correo y celular)
 * + su ficha (tt_paciente: N.º de historia, cobertura, afiliación).
 * Si la persona ya existe en el sistema (por ejemplo, un trabajador de la
 * clínica que se atiende como paciente), se reutilizan sus datos personales.
 */
public class PacienteRepositoryImpl implements PacienteRepository {

    private static final String SELECT_PACIENTE =
          "SELECT p.id_paciente, p.id_datos_personales, p.numero_historia, p.id_cobertura, "
        + "       cb.nombre AS nombre_cobertura, cb.tipo AS tipo_cobertura, p.numero_afiliacion, "
        + "       d.fecha_nacimiento, p.observaciones, p.estado, p.fecha_creacion, p.usuario_registro, "
        + "       d.nombres, d.apellidos, d.dni, d.id_sexo, (d.id_usuario IS NOT NULL) AS tiene_cuenta, "
        + "       c.correo_electronico, t.numero_telefono "
        + "FROM tt_paciente p "
        + "JOIN tt_datos_personales d ON d.id_datos_personales = p.id_datos_personales "
        + "JOIN tm_cobertura cb ON cb.id_cobertura = p.id_cobertura "
        + "LEFT JOIN LATERAL (SELECT correo_electronico FROM tt_correo "
        + "                   WHERE id_datos_personales = d.id_datos_personales AND estado = 'Activo' "
        + "                   ORDER BY id_correo LIMIT 1) c ON TRUE "
        + "LEFT JOIN LATERAL (SELECT numero_telefono FROM tt_telefono "
        + "                   WHERE id_datos_personales = d.id_datos_personales AND estado = 'Activo' "
        + "                   ORDER BY id_telefono LIMIT 1) t ON TRUE ";

    private Connection conexion() throws SQLException {
        return ConexionBDSingleton.getInstancia().getConexion();
    }

    private static PacienteDTO mapear(ResultSet rs) throws SQLException {
        PacienteDTO p = new PacienteDTO();
        p.setIdPaciente(rs.getInt("id_paciente"));
        p.setIdDatosPersonales(rs.getInt("id_datos_personales"));
        p.setNumeroHistoria(rs.getString("numero_historia"));
        p.setIdCobertura(rs.getInt("id_cobertura"));
        p.setNombreCobertura(rs.getString("nombre_cobertura"));
        p.setTipoCobertura(rs.getString("tipo_cobertura"));
        p.setNumeroAfiliacion(rs.getString("numero_afiliacion"));
        p.setFechaNacimiento(RepositorioUtil.fechaONula(rs, "fecha_nacimiento"));
        p.setObservaciones(rs.getString("observaciones"));
        p.setEstado(rs.getString("estado"));
        p.setFechaCreacion(rs.getTimestamp("fecha_creacion"));
        p.setUsuarioRegistro(rs.getString("usuario_registro"));
        p.setNombres(rs.getString("nombres"));
        p.setApellidos(rs.getString("apellidos"));
        p.setDni(rs.getString("dni"));
        p.setIdSexo(RepositorioUtil.enteroONulo(rs, "id_sexo"));
        p.setTieneCuenta(rs.getBoolean("tiene_cuenta"));
        p.setCorreo(rs.getString("correo_electronico"));
        p.setCelular(rs.getString("numero_telefono"));
        return p;
    }

    // ============================== LECTURA ==============================

    @Override
    public PaginaDTO<PacienteDTO> listar(FiltroBusquedaDTO filtro) {
        StringBuilder where = new StringBuilder("WHERE 1 = 1");
        List<Object> parametros = new ArrayList<>();
        if (filtro.getTexto() != null) {
            // RF-02: por DNI, apellidos (o nombre completo en cualquier orden) o N.º de historia
            where.append(" AND (d.dni ILIKE ? OR p.numero_historia ILIKE ? "
                       + "OR (d.apellidos || ' ' || d.nombres) ILIKE ? OR (d.nombres || ' ' || d.apellidos) ILIKE ?)");
            for (int i = 0; i < 4; i++) {
                parametros.add(filtro.getPatron());
            }
        }
        if (filtro.getGrupo() != null) {
            where.append(" AND p.id_cobertura = ?");
            parametros.add(filtro.getGrupo());
        }
        RepositorioUtil.filtrarEstado(where, "p", filtro);
        RepositorioUtil.filtrarFechas(where, parametros, "p.fecha_creacion", filtro.getRango());
        try {
            return RepositorioUtil.paginar(conexion(), SELECT_PACIENTE, where.toString(), parametros,
                    "d.apellidos, d.nombres", filtro, PacienteRepositoryImpl::mapear);
        } catch (SQLException e) {
            throw new RepositorioException("Error al listar pacientes", e);
        }
    }

    @Override
    public Optional<PacienteDTO> buscarPorId(Integer idPaciente) {
        try {
            return RepositorioUtil.unaFila(conexion(), SELECT_PACIENTE + "WHERE p.id_paciente = ?",
                    idPaciente, PacienteRepositoryImpl::mapear);
        } catch (SQLException e) {
            throw new RepositorioException("Error al buscar el paciente", e);
        }
    }

    @Override
    public Optional<String> historiaConDni(String dni, int idPacienteExcluir) {
        String sql = "SELECT p.numero_historia FROM tt_paciente p "
                   + "JOIN tt_datos_personales d ON d.id_datos_personales = p.id_datos_personales "
                   + "WHERE d.dni = ? AND p.id_paciente <> ?";
        try (PreparedStatement stmt = conexion().prepareStatement(sql)) {
            stmt.setString(1, dni);
            stmt.setInt(2, idPacienteExcluir);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? Optional.of(rs.getString(1)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new RepositorioException("Error al verificar el DNI", e);
        }
    }

    @Override
    public boolean existeCorreoDeOtraPersona(String correo, String dniPropio) {
        try {
            return RepositorioUtil.existe(conexion(), "SELECT 1 FROM tt_correo c "
                    + "JOIN tt_datos_personales d ON d.id_datos_personales = c.id_datos_personales "
                    + "WHERE LOWER(c.correo_electronico) = LOWER(?) AND d.dni <> ?", correo, dniPropio);
        } catch (SQLException e) {
            throw new RepositorioException("Error al verificar el correo", e);
        }
    }

    // ============================= ESCRITURA =============================

    /**
     * Registra al paciente en UNA transacción: datos personales (o reutiliza
     * los existentes por DNI) + ficha de paciente + correo + celular.
     */
    @Override
    public Integer insertar(PacienteDTO p) {
        try (Connection cn = ConexionBDSingleton.getInstancia().abrirConexionTransaccional()) {
            try {
                int idDatos = buscarOCrearPersona(cn, p);
                int idPaciente;
                try (PreparedStatement stmt = cn.prepareStatement(
                        "INSERT INTO tt_paciente (id_datos_personales, id_cobertura, numero_afiliacion, "
                      + "                         observaciones, estado, usuario_registro) "
                      + "VALUES (?, ?, ?, ?, ?, ?) RETURNING id_paciente")) {
                    stmt.setInt(1, idDatos);
                    llenarFicha(stmt, 2, p);
                    stmt.setString(5, p.getEstado());
                    stmt.setString(6, p.getUsuarioRegistro());
                    try (ResultSet rs = stmt.executeQuery()) {
                        rs.next();
                        idPaciente = rs.getInt(1);
                    }
                }
                guardarContactos(cn, idDatos, p);
                cn.commit();
                return idPaciente;
            } catch (SQLException e) {
                cn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new RepositorioException("Error al registrar el paciente", e);
        }
    }

    /** Actualiza persona + ficha + contactos en una transacción. */
    @Override
    public boolean actualizar(PacienteDTO p) {
        try (Connection cn = ConexionBDSingleton.getInstancia().abrirConexionTransaccional()) {
            try {
                int idDatos;
                try (PreparedStatement stmt = cn.prepareStatement(
                        "UPDATE tt_paciente SET id_cobertura = ?, numero_afiliacion = ?, observaciones = ?, estado = ? "
                      + "WHERE id_paciente = ? RETURNING id_datos_personales")) {
                    llenarFicha(stmt, 1, p);
                    stmt.setString(4, p.getEstado());
                    stmt.setInt(5, p.getIdPaciente());
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (!rs.next()) {
                            cn.rollback();
                            return false;
                        }
                        idDatos = rs.getInt(1);
                    }
                }
                try (PreparedStatement stmt = cn.prepareStatement(
                        "UPDATE tt_datos_personales SET nombres = ?, apellidos = ?, dni = ?, id_sexo = ?, fecha_nacimiento = ? "
                      + "WHERE id_datos_personales = ?")) {
                    stmt.setString(1, p.getNombres());
                    stmt.setString(2, p.getApellidos());
                    stmt.setString(3, p.getDni());
                    RepositorioUtil.enteroONulo(stmt, 4, p.getIdSexo());
                    RepositorioUtil.fechaONula(stmt, 5, p.getFechaNacimiento());
                    stmt.setInt(6, idDatos);
                    stmt.executeUpdate();
                }
                guardarContactos(cn, idDatos, p);
                cn.commit();
                return true;
            } catch (SQLException e) {
                cn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new RepositorioException("Error al actualizar el paciente", e);
        }
    }

    /** Eliminación lógica: la ficha queda Inactiva; su historial se conserva. */
    @Override
    public boolean eliminar(Integer idPaciente) {
        return cambiarEstado(idPaciente, "Inactivo");
    }

    @Override
    public boolean reactivar(int idPaciente) {
        return cambiarEstado(idPaciente, "Activo");
    }

    // ============================= AUXILIARES =============================

    private boolean cambiarEstado(int idPaciente, String estado) {
        try {
            return RepositorioUtil.cambiarEstado(conexion(), "tt_paciente", "id_paciente", idPaciente, estado);
        } catch (SQLException e) {
            throw new RepositorioException("Error al cambiar el estado del paciente", e);
        }
    }

    /** Devuelve el id de la persona con ese DNI; si no existe, la crea (sin cuenta de acceso). */
    private int buscarOCrearPersona(Connection cn, PacienteDTO p) throws SQLException {
        try (PreparedStatement stmt = cn.prepareStatement(
                "SELECT id_datos_personales FROM tt_datos_personales WHERE dni = ?")) {
            stmt.setString(1, p.getDni());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    int idExistente = rs.getInt(1);
                    // Persona ya registrada: solo se completan los datos que le falten (no se pisan)
                    try (PreparedStatement upd = cn.prepareStatement(
                            "UPDATE tt_datos_personales SET id_sexo = COALESCE(id_sexo, ?), "
                          + "fecha_nacimiento = COALESCE(fecha_nacimiento, ?) WHERE id_datos_personales = ?")) {
                        RepositorioUtil.enteroONulo(upd, 1, p.getIdSexo());
                        RepositorioUtil.fechaONula(upd, 2, p.getFechaNacimiento());
                        upd.setInt(3, idExistente);
                        upd.executeUpdate();
                    }
                    return idExistente;
                }
            }
        }
        try (PreparedStatement stmt = cn.prepareStatement(
                "INSERT INTO tt_datos_personales (nombres, apellidos, dni, id_sexo, fecha_nacimiento, usuario_registro) "
              + "VALUES (?, ?, ?, ?, ?, ?) RETURNING id_datos_personales")) {
            stmt.setString(1, p.getNombres());
            stmt.setString(2, p.getApellidos());
            stmt.setString(3, p.getDni());
            RepositorioUtil.enteroONulo(stmt, 4, p.getIdSexo());
            RepositorioUtil.fechaONula(stmt, 5, p.getFechaNacimiento());
            stmt.setString(6, p.getUsuarioRegistro());
            try (ResultSet rs = stmt.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    /** Cobertura, afiliación y observaciones (3 parámetros desde "desde"). */
    private static void llenarFicha(PreparedStatement stmt, int desde, PacienteDTO p) throws SQLException {
        stmt.setInt(desde, p.getIdCobertura());
        RepositorioUtil.textoONulo(stmt, desde + 1, p.getNumeroAfiliacion());
        RepositorioUtil.textoONulo(stmt, desde + 2, p.getObservaciones());
    }

    private static void guardarContactos(Connection cn, int idDatos, PacienteDTO p) throws SQLException {
        RepositorioUtil.guardarContacto(cn, "tt_correo", "id_correo", "correo_electronico", "tipo_correo",
                "Personal", idDatos, p.getCorreo(), p.getUsuarioRegistro());
        RepositorioUtil.guardarContacto(cn, "tt_telefono", "id_telefono", "numero_telefono", "tipo_telefono",
                "Móvil", idDatos, p.getCelular(), p.getUsuarioRegistro());
    }
}
