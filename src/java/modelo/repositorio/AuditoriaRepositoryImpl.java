package modelo.repositorio;

import configuracion.ConexionBDSingleton;
import evento.EventoSistema;
import evento.TipoEvento;
import modelo.dto.AuditoriaDTO;
import modelo.dto.FiltroAuditoriaDTO;
import modelo.dto.PaginaDTO;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/** PATRÓN: Repository  ->  IMPLEMENTACIÓN (SQL) de AuditoriaRepository. */
public class AuditoriaRepositoryImpl implements AuditoriaRepository {

    private static final String SELECT_AUDITORIA =
          "SELECT a.id_auditoria, a.fecha_creacion, a.usuario_registro, a.accion, a.entidad, a.id_entidad, "
        + "       a.detalle, a.direccion_ip "
        + "FROM vw_auditoria a ";

    private Connection conexion() throws SQLException {
        return ConexionBDSingleton.getInstancia().getConexion();
    }

    private static AuditoriaDTO mapear(ResultSet rs) throws SQLException {
        AuditoriaDTO a = new AuditoriaDTO();
        a.setIdAuditoria(rs.getLong("id_auditoria"));
        a.setFecha(rs.getTimestamp("fecha_creacion"));
        a.setUsuario(rs.getString("usuario_registro"));
        a.setAccion(rs.getString("accion"));
        a.setDescripcion(TipoEvento.describir(rs.getString("accion")));
        a.setEntidad(rs.getString("entidad"));
        a.setIdEntidad(RepositorioUtil.enteroONulo(rs, "id_entidad"));
        a.setDetalle(rs.getString("detalle"));
        a.setDireccionIp(rs.getString("direccion_ip"));
        return a;
    }

    @Override
    public void registrar(EventoSistema e) {
        String sql = "INSERT INTO tt_auditoria (id_usuario, accion, id_entidad, detalle, direccion_ip, "
                   + "                          fecha_creacion, usuario_registro) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = conexion().prepareStatement(sql)) {
            RepositorioUtil.enteroONulo(stmt, 1, e.getIdUsuario());
            stmt.setString(2, e.getTipo().name());
            RepositorioUtil.enteroONulo(stmt, 3, e.getIdEntidad());
            RepositorioUtil.textoONulo(stmt, 4, e.getDetalle());
            RepositorioUtil.textoONulo(stmt, 5, e.getDireccionIp());
            stmt.setTimestamp(6, Timestamp.valueOf(e.getFecha()));
            stmt.setString(7, e.getNombreUsuario() == null ? "sistema" : e.getNombreUsuario());
            stmt.executeUpdate();
        } catch (SQLException ex) {
            throw new RepositorioException("Error al registrar la auditoría", ex);
        }
    }

    @Override
    public PaginaDTO<AuditoriaDTO> listar(FiltroAuditoriaDTO f) {
        StringBuilder where = new StringBuilder("WHERE 1 = 1");
        List<Object> parametros = new ArrayList<>();
        if (f.getTexto() != null) {
            where.append(" AND (a.usuario_registro ILIKE ? OR a.detalle ILIKE ? OR a.accion ILIKE ?)");
            parametros.add(f.getPatron());
            parametros.add(f.getPatron());
            parametros.add(f.getPatron().replace(' ', '_'));
        }
        if (f.getEntidad() != null) {
            where.append(" AND a.entidad = ?");
            parametros.add(f.getEntidad());
            if (f.getIdEntidad() != null) {
                where.append(" AND a.id_entidad = ?");
                parametros.add(f.getIdEntidad());
            }
        }
        if (f.getDesde() != null) {
            where.append(" AND a.fecha_creacion >= ?");
            parametros.add(Date.valueOf(f.getDesde()));
        }
        if (f.getHasta() != null) {
            where.append(" AND a.fecha_creacion < ?");                 // hasta el final de ese día
            parametros.add(Date.valueOf(f.getHasta().plusDays(1)));
        }
        try {
            Connection cn = conexion();
            int total;
            try (PreparedStatement stmt = cn.prepareStatement("SELECT COUNT(*) FROM vw_auditoria a " + where)) {
                RepositorioUtil.asignar(stmt, parametros);
                try (ResultSet rs = stmt.executeQuery()) {
                    rs.next();
                    total = rs.getInt(1);
                }
            }
            List<AuditoriaDTO> elementos = new ArrayList<>();
            try (PreparedStatement stmt = cn.prepareStatement(SELECT_AUDITORIA + where
                    + " ORDER BY a.id_auditoria DESC LIMIT ? OFFSET ?")) {
                int i = RepositorioUtil.asignar(stmt, parametros);
                stmt.setInt(i++, f.getTamanio());
                stmt.setInt(i, f.getOffset());
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        elementos.add(mapear(rs));
                    }
                }
            }
            return new PaginaDTO<>(elementos, total, f.getPagina(), f.getTamanio());
        } catch (SQLException e) {
            throw new RepositorioException("Error al leer la auditoría", e);
        }
    }

    @Override
    public void sincronizarAcciones() {
        String sql = "INSERT INTO tm_accion_auditoria (codigo, entidad, descripcion, usuario_registro) VALUES (?, ?, ?, 'sistema') "
                   + "ON CONFLICT (codigo) DO UPDATE SET entidad = EXCLUDED.entidad, descripcion = EXCLUDED.descripcion";
        try (Connection cn = ConexionBDSingleton.getInstancia().abrirConexionTransaccional()) {
            try (PreparedStatement stmt = cn.prepareStatement(sql)) {
                for (TipoEvento t : TipoEvento.values()) {
                    stmt.setString(1, t.name());
                    stmt.setString(2, t.getEntidad());
                    stmt.setString(3, t.getDescripcion());
                    stmt.addBatch();
                }
                stmt.executeBatch();
                cn.commit();
            } catch (SQLException e) {
                cn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new RepositorioException("Error al registrar las acciones de auditoría", e);
        }
    }

    @Override
    public List<AuditoriaDTO> ultimosDe(String entidad, int idEntidad, int cantidad) {
        List<AuditoriaDTO> lista = new ArrayList<>();
        try (PreparedStatement stmt = conexion().prepareStatement(SELECT_AUDITORIA
                + "WHERE a.entidad = ? AND a.id_entidad = ? ORDER BY a.id_auditoria DESC LIMIT ?")) {
            stmt.setString(1, entidad);
            stmt.setInt(2, idEntidad);
            stmt.setInt(3, cantidad);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapear(rs));
                }
            }
            return lista;
        } catch (SQLException e) {
            throw new RepositorioException("Error al leer el historial", e);
        }
    }
}
