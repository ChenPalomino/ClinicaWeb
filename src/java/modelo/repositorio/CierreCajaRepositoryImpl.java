package modelo.repositorio;

import configuracion.ConexionBDSingleton;
import modelo.dto.ResumenCajaDTO;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** PATRÓN: Repository  ->  IMPLEMENTACIÓN (SQL) de CierreCajaRepository. */
public class CierreCajaRepositoryImpl implements CierreCajaRepository {

    private static final String SELECT_CIERRE =
          "SELECT cc.id_usuario_cajero, u.nombre_usuario, cc.fecha_caja, cc.cantidad_pagos, cc.total_efectivo, "
        + "       cc.total_tarjeta, cc.total_billetera, cc.efectivo_contado, cc.diferencia, cc.observacion, cc.fecha_creacion "
        + "FROM vw_cierre_caja cc JOIN tt_usuario u ON u.id_usuario = cc.id_usuario_cajero ";

    private Connection conexion() throws SQLException {
        return ConexionBDSingleton.getInstancia().getConexion();
    }

    private static ResumenCajaDTO mapear(ResultSet rs) throws SQLException {
        ResumenCajaDTO r = new ResumenCajaDTO();
        r.setIdUsuarioCajero(rs.getInt("id_usuario_cajero"));
        r.setNombreCajero(rs.getString("nombre_usuario"));
        r.setFecha(rs.getDate("fecha_caja").toLocalDate());
        r.setCantidadPagos(rs.getInt("cantidad_pagos"));
        r.setTotalEfectivo(rs.getBigDecimal("total_efectivo"));
        r.setTotalTarjeta(rs.getBigDecimal("total_tarjeta"));
        r.setTotalBilletera(rs.getBigDecimal("total_billetera"));
        r.setEfectivoContado(rs.getBigDecimal("efectivo_contado"));
        r.setDiferencia(rs.getBigDecimal("diferencia"));
        r.setObservacion(rs.getString("observacion"));
        r.setFechaCierre(rs.getTimestamp("fecha_creacion"));
        r.setCerrada(true);
        return r;
    }

    @Override
    public Optional<ResumenCajaDTO> buscar(int idUsuarioCajero, LocalDate fecha) {
        try (PreparedStatement stmt = conexion().prepareStatement(SELECT_CIERRE
                + "WHERE cc.id_usuario_cajero = ? AND cc.fecha_caja = ?")) {
            stmt.setInt(1, idUsuarioCajero);
            stmt.setDate(2, Date.valueOf(fecha));
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? Optional.of(mapear(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new RepositorioException("Error al buscar el cierre de caja", e);
        }
    }

    @Override
    public void registrar(ResumenCajaDTO c, String usuario) {
        String sql = "INSERT INTO tt_cierre_caja (id_usuario_cajero, fecha_caja, cantidad_pagos, total_efectivo, total_tarjeta, "
                   + "       total_billetera, efectivo_contado, observacion, usuario_registro) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) ON CONFLICT (id_usuario_cajero, fecha_caja) DO NOTHING";
        try (PreparedStatement stmt = conexion().prepareStatement(sql)) {
            stmt.setInt(1, c.getIdUsuarioCajero());
            stmt.setDate(2, Date.valueOf(c.getFecha()));
            stmt.setInt(3, c.getCantidadPagos());
            stmt.setBigDecimal(4, c.getTotalEfectivo());
            stmt.setBigDecimal(5, c.getTotalTarjeta());
            stmt.setBigDecimal(6, c.getTotalBilletera());
            stmt.setBigDecimal(7, c.getEfectivoContado());
            RepositorioUtil.textoONulo(stmt, 8, c.getObservacion());
            stmt.setString(9, usuario);
            if (stmt.executeUpdate() == 0) {
                throw new OperacionRechazadaException("Tu caja de hoy ya estaba cerrada.");
            }
        } catch (SQLException e) {
            throw new RepositorioException("Error al registrar el cierre de caja", e);
        }
    }

    @Override
    public List<ResumenCajaDTO> historial(Integer idUsuarioCajero, int limite) {
        String sql = SELECT_CIERRE + (idUsuarioCajero == null ? "" : "WHERE cc.id_usuario_cajero = ? ")
                   + "ORDER BY cc.fecha_caja DESC, u.nombre_usuario LIMIT ?";
        List<ResumenCajaDTO> lista = new ArrayList<>();
        try (PreparedStatement stmt = conexion().prepareStatement(sql)) {
            int i = 1;
            if (idUsuarioCajero != null) {
                stmt.setInt(i++, idUsuarioCajero);
            }
            stmt.setInt(i, limite);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapear(rs));
                }
            }
            return lista;
        } catch (SQLException e) {
            throw new RepositorioException("Error al leer el historial de cierres", e);
        }
    }
}
