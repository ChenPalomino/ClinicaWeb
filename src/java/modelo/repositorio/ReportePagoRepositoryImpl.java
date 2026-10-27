package modelo.repositorio;

import configuracion.ConexionBDSingleton;
import modelo.dto.ReportePagoDTO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ReportePagoRepositoryImpl implements ReportePagoRepository {

    private static final String SELECT_REPORTE =
          "SELECT rp.id_reporte_pago, rp.id_cuenta_atencion, ca.numero_cuenta, ca.total AS total_cuenta, ca.estado_cuenta, "
        + "       ca.id_paciente, d.nombres || ' ' || d.apellidos AS paciente, d.dni, rp.id_billetera, b.nombre AS billetera, "
        + "       rp.monto, rp.numero_operacion, rp.celular, rp.nombre_pagador, rp.estado_reporte, rp.motivo_rechazo, "
        + "       ur.nombre_usuario AS usuario_revisa, rp.fecha_revision, rp.id_pago, rp.fecha_creacion, rp.usuario_registro "
        + "FROM tt_reporte_pago rp "
        + "JOIN vw_cuenta_atencion ca ON ca.id_cuenta_atencion = rp.id_cuenta_atencion "
        + "JOIN tt_paciente p ON p.id_paciente = ca.id_paciente "
        + "JOIN tt_datos_personales d ON d.id_datos_personales = p.id_datos_personales "
        + "JOIN tm_billetera b ON b.id_billetera = rp.id_billetera "
        + "LEFT JOIN tt_usuario ur ON ur.id_usuario = rp.id_usuario_revisa ";

    private Connection conexion() throws SQLException {
        return ConexionBDSingleton.getInstancia().getConexion();
    }

    private static ReportePagoDTO mapear(ResultSet rs) throws SQLException {
        ReportePagoDTO r = new ReportePagoDTO();
        r.setIdReportePago(rs.getInt("id_reporte_pago"));
        r.setIdCuentaAtencion(rs.getInt("id_cuenta_atencion"));
        r.setNumeroCuenta(rs.getString("numero_cuenta"));
        r.setTotalCuenta(rs.getBigDecimal("total_cuenta"));
        r.setEstadoCuenta(rs.getString("estado_cuenta"));
        r.setIdPaciente(rs.getInt("id_paciente"));
        r.setNombrePaciente(rs.getString("paciente"));
        r.setDniPaciente(rs.getString("dni"));
        r.setIdBilletera(rs.getInt("id_billetera"));
        r.setNombreBilletera(rs.getString("billetera"));
        r.setMonto(rs.getBigDecimal("monto"));
        r.setNumeroOperacion(rs.getString("numero_operacion"));
        r.setCelular(rs.getString("celular"));
        r.setNombrePagador(rs.getString("nombre_pagador"));
        r.setEstadoReporte(rs.getString("estado_reporte"));
        r.setMotivoRechazo(rs.getString("motivo_rechazo"));
        r.setUsuarioRevisa(rs.getString("usuario_revisa"));
        r.setFechaRevision(rs.getTimestamp("fecha_revision"));
        r.setIdPago(RepositorioUtil.enteroONulo(rs, "id_pago"));
        r.setFechaCreacion(rs.getTimestamp("fecha_creacion"));
        r.setUsuarioRegistro(rs.getString("usuario_registro"));
        return r;
    }

    @Override
    public int registrar(ReportePagoDTO r) {
        String sql = "INSERT INTO tt_reporte_pago (id_cuenta_atencion, id_billetera, monto, numero_operacion, celular, "
                   + "       nombre_pagador, usuario_registro) VALUES (?, ?, ?, ?, ?, ?, ?) RETURNING id_reporte_pago";
        try (PreparedStatement stmt = conexion().prepareStatement(sql)) {
            stmt.setInt(1, r.getIdCuentaAtencion());
            stmt.setInt(2, r.getIdBilletera());
            stmt.setBigDecimal(3, r.getMonto());
            RepositorioUtil.textoONulo(stmt, 4, r.getNumeroOperacion());
            RepositorioUtil.textoONulo(stmt, 5, r.getCelular());
            RepositorioUtil.textoONulo(stmt, 6, r.getNombrePagador());
            stmt.setString(7, r.getUsuarioRegistro());
            try (ResultSet rs = stmt.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            if ("23505".equals(e.getSQLState())) {
                throw new OperacionRechazadaException("Ya informaste un pago para esta cita y la caja lo está revisando.");
            }
            throw new RepositorioException("Error al registrar el pago informado", e);
        }
    }

    @Override
    public Optional<ReportePagoDTO> buscarPorId(int idReporte) {
        try {
            return RepositorioUtil.unaFila(conexion(), SELECT_REPORTE + "WHERE rp.id_reporte_pago = ?", idReporte,
                    ReportePagoRepositoryImpl::mapear);
        } catch (SQLException e) {
            throw new RepositorioException("Error al buscar el pago informado", e);
        }
    }

    @Override
    public Optional<ReportePagoDTO> ultimoDeCuenta(int idCuenta) {
        try {
            return RepositorioUtil.unaFila(conexion(), SELECT_REPORTE
                    + "WHERE rp.id_cuenta_atencion = ? ORDER BY rp.id_reporte_pago DESC LIMIT 1", idCuenta,
                    ReportePagoRepositoryImpl::mapear);
        } catch (SQLException e) {
            throw new RepositorioException("Error al buscar el pago informado", e);
        }
    }

    @Override
    public List<ReportePagoDTO> pendientes(int limite) {
        List<ReportePagoDTO> lista = new ArrayList<>();
        try (PreparedStatement stmt = conexion().prepareStatement(SELECT_REPORTE
                + "WHERE rp.estado_reporte = 'PENDIENTE' ORDER BY rp.fecha_creacion LIMIT ?")) {
            stmt.setInt(1, limite);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapear(rs));
                }
            }
            return lista;
        } catch (SQLException e) {
            throw new RepositorioException("Error al leer los pagos por verificar", e);
        }
    }

    @Override
    public int contarPendientes() {
        try (PreparedStatement stmt = conexion().prepareStatement(
                "SELECT COUNT(*) FROM tt_reporte_pago WHERE estado_reporte = 'PENDIENTE'"); ResultSet rs = stmt.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        } catch (SQLException e) {
            throw new RepositorioException("Error al contar los pagos por verificar", e);
        }
    }

    @Override
    public boolean marcarVerificado(int idReporte, int idUsuario, int idPago) {
        String sql = "UPDATE tt_reporte_pago SET estado_reporte = 'VERIFICADO', id_usuario_revisa = ?, id_pago = ?, "
                   + "       fecha_revision = LOCALTIMESTAMP WHERE id_reporte_pago = ? AND estado_reporte = 'PENDIENTE'";
        try (PreparedStatement stmt = conexion().prepareStatement(sql)) {
            stmt.setInt(1, idUsuario);
            stmt.setInt(2, idPago);
            stmt.setInt(3, idReporte);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RepositorioException("Error al verificar el pago informado", e);
        }
    }

    @Override
    public boolean marcarRechazado(int idReporte, int idUsuario, String motivo) {
        String sql = "UPDATE tt_reporte_pago SET estado_reporte = 'RECHAZADO', id_usuario_revisa = ?, motivo_rechazo = ?, "
                   + "       fecha_revision = LOCALTIMESTAMP WHERE id_reporte_pago = ? AND estado_reporte = 'PENDIENTE'";
        try (PreparedStatement stmt = conexion().prepareStatement(sql)) {
            stmt.setInt(1, idUsuario);
            stmt.setString(2, motivo);
            stmt.setInt(3, idReporte);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RepositorioException("Error al rechazar el pago informado", e);
        }
    }
}
