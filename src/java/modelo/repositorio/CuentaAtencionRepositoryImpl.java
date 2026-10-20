package modelo.repositorio;

import configuracion.ConexionBDSingleton;
import modelo.cobranza.TotalesCuenta;
import modelo.dto.CoberturaDTO;
import modelo.dto.CuentaAtencionDTO;
import modelo.dto.DetalleCuentaDTO;
import modelo.dto.FiltroBusquedaDTO;
import modelo.dto.PaginaDTO;
import modelo.dto.PagoDTO;
import modelo.dto.ProductoServicioDTO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/** PATRÓN: Repository  ->  IMPLEMENTACIÓN (SQL) de CuentaAtencionRepository. */
public class CuentaAtencionRepositoryImpl implements CuentaAtencionRepository {

    private static final Set<String> ESTADOS = Set.of(CuentaAtencionDTO.PENDIENTE, CuentaAtencionDTO.PAGADA,
            CuentaAtencionDTO.ANULADA);

    private static final String SELECT_CUENTA =
          "SELECT ca.id_cuenta_atencion, ca.numero_cuenta, ca.id_paciente, ca.id_cobertura, ca.estado_cuenta, "
        + "       ca.op_gravada, ca.op_exonerada, ca.igv, ca.total, ca.monto_aseguradora, ca.monto_descuento, "
        + "       ca.observacion, ca.fecha_creacion, ca.fecha_pago, ca.usuario_registro, ca.motivo_anulacion, "
        + "       ca.fecha_anulacion, ua.nombre_usuario AS usuario_anula, "
        + "       d.nombres || ' ' || d.apellidos AS paciente, d.dni, COALESCE(d.id_usuario, 0) AS id_usuario_paciente, "
        + "       p.numero_historia, cb.nombre AS cobertura, cb.tipo AS tipo_cobertura, cb.porcentaje_cobertura, "
        + "       (SELECT COUNT(*) FROM tt_detalle_cuenta dc WHERE dc.id_cuenta_atencion = ca.id_cuenta_atencion) AS items, "
        + "       cp.id_comprobante, cp.serie, cp.numero "
        + "FROM tt_cuenta_atencion ca "
        + "JOIN tt_paciente p ON p.id_paciente = ca.id_paciente "
        + "JOIN tt_datos_personales d ON d.id_datos_personales = p.id_datos_personales "
        + "JOIN tm_cobertura cb ON cb.id_cobertura = ca.id_cobertura "
        + "LEFT JOIN tt_usuario ua ON ua.id_usuario = ca.id_usuario_anula "
        + "LEFT JOIN LATERAL (SELECT id_comprobante, serie, numero FROM tt_comprobante "
        + "                   WHERE id_cuenta_atencion = ca.id_cuenta_atencion "
        + "                   ORDER BY id_comprobante DESC LIMIT 1) cp ON TRUE ";

    private static final String SELECT_PAGO =
          "SELECT pg.id_pago, pg.id_cuenta_atencion, pg.id_metodo_pago, mp.codigo, mp.nombre AS metodo, pg.monto, "
        + "       pg.monto_recibido, pg.vuelto, pg.referencia, pg.estado_pago, pg.motivo_rechazo, "
        + "       pg.id_usuario_cajero, u.nombre_usuario AS cajero, pg.fecha_creacion "
        + "FROM tt_pago pg "
        + "JOIN tm_metodo_pago mp ON mp.id_metodo_pago = pg.id_metodo_pago "
        + "JOIN tt_usuario u ON u.id_usuario = pg.id_usuario_cajero ";

    private Connection conexion() throws SQLException {
        return ConexionBDSingleton.getInstancia().getConexion();
    }

    private static CuentaAtencionDTO mapear(ResultSet rs) throws SQLException {
        CuentaAtencionDTO c = new CuentaAtencionDTO();
        c.setIdCuentaAtencion(rs.getInt("id_cuenta_atencion"));
        c.setNumeroCuenta(rs.getString("numero_cuenta"));
        c.setIdPaciente(rs.getInt("id_paciente"));
        c.setIdCobertura(rs.getInt("id_cobertura"));
        c.setEstadoCuenta(rs.getString("estado_cuenta"));
        c.setOpGravada(rs.getBigDecimal("op_gravada"));
        c.setOpExonerada(rs.getBigDecimal("op_exonerada"));
        c.setIgv(rs.getBigDecimal("igv"));
        c.setTotal(rs.getBigDecimal("total"));
        c.setMontoAseguradora(rs.getBigDecimal("monto_aseguradora"));
        c.setMontoDescuento(rs.getBigDecimal("monto_descuento"));
        c.setObservacion(rs.getString("observacion"));
        c.setFechaCreacion(rs.getTimestamp("fecha_creacion"));
        c.setFechaPago(rs.getTimestamp("fecha_pago"));
        c.setUsuarioRegistro(rs.getString("usuario_registro"));
        c.setMotivoAnulacion(rs.getString("motivo_anulacion"));
        c.setFechaAnulacion(rs.getTimestamp("fecha_anulacion"));
        c.setUsuarioAnula(rs.getString("usuario_anula"));
        c.setNombrePaciente(rs.getString("paciente"));
        c.setDniPaciente(rs.getString("dni"));
        c.setIdUsuarioPaciente(rs.getInt("id_usuario_paciente"));
        c.setNumeroHistoria(rs.getString("numero_historia"));
        c.setNombreCobertura(rs.getString("cobertura"));
        c.setTipoCobertura(rs.getString("tipo_cobertura"));
        c.setPorcentajeCobertura(rs.getBigDecimal("porcentaje_cobertura"));
        c.setCantidadItems(rs.getInt("items"));
        c.setIdComprobante(rs.getInt("id_comprobante"));
        String serie = rs.getString("serie");
        if (serie != null) {
            c.setComprobante(serie + "-" + String.format("%08d", rs.getInt("numero")));
        }
        return c;
    }

    static PagoDTO mapearPago(ResultSet rs) throws SQLException {
        PagoDTO p = new PagoDTO();
        p.setIdPago(rs.getInt("id_pago"));
        p.setIdCuentaAtencion(rs.getInt("id_cuenta_atencion"));
        p.setIdMetodoPago(rs.getInt("id_metodo_pago"));
        p.setCodigoMetodo(rs.getString("codigo"));
        p.setNombreMetodo(rs.getString("metodo"));
        p.setMonto(rs.getBigDecimal("monto"));
        p.setMontoRecibido(rs.getBigDecimal("monto_recibido"));
        p.setVuelto(rs.getBigDecimal("vuelto"));
        p.setReferencia(rs.getString("referencia"));
        p.setEstadoPago(rs.getString("estado_pago"));
        p.setMotivoRechazo(rs.getString("motivo_rechazo"));
        p.setIdUsuarioCajero(rs.getInt("id_usuario_cajero"));
        p.setNombreCajero(rs.getString("cajero"));
        p.setFechaCreacion(rs.getTimestamp("fecha_creacion"));
        return p;
    }

    static DetalleCuentaDTO mapearDetalle(ResultSet rs) throws SQLException {
        DetalleCuentaDTO d = new DetalleCuentaDTO();
        d.setIdDetalleCuenta(rs.getInt("id_detalle_cuenta"));
        d.setIdProductoServicio(rs.getInt("id_producto_servicio"));
        d.setCodigo(rs.getString("codigo"));
        d.setDescripcion(rs.getString("descripcion"));
        d.setTipo(rs.getString("tipo"));
        d.setCantidad(rs.getInt("cantidad"));
        d.setPrecioUnitario(rs.getBigDecimal("precio_unitario"));
        d.setAfectoIgv(rs.getBoolean("afecto_igv"));
        d.setPorcentajeCubierto(rs.getBigDecimal("porcentaje_cubierto"));
        d.setMontoAseguradora(rs.getBigDecimal("monto_aseguradora"));
        d.setMontoDescuento(rs.getBigDecimal("monto_descuento"));
        d.setImporte(rs.getBigDecimal("importe"));
        return d;
    }

    /** Detalle de una cuenta (también lo usa la representación impresa del comprobante). */
    static List<DetalleCuentaDTO> detalles(Connection cn, int idCuenta) throws SQLException {
        String sql = "SELECT dc.id_detalle_cuenta, dc.id_producto_servicio, ps.codigo, dc.descripcion, dc.tipo, dc.cantidad, "
                   + "       dc.precio_unitario, dc.afecto_igv, dc.porcentaje_cubierto, dc.monto_aseguradora, "
                   + "       dc.monto_descuento, dc.importe "
                   + "FROM tt_detalle_cuenta dc JOIN tt_producto_servicio ps ON ps.id_producto_servicio = dc.id_producto_servicio "
                   + "WHERE dc.id_cuenta_atencion = ? ORDER BY dc.id_detalle_cuenta";
        List<DetalleCuentaDTO> lista = new ArrayList<>();
        try (PreparedStatement stmt = cn.prepareStatement(sql)) {
            stmt.setInt(1, idCuenta);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapearDetalle(rs));
                }
            }
        }
        return lista;
    }

    @Override
    public PaginaDTO<CuentaAtencionDTO> listar(FiltroBusquedaDTO filtro) {
        StringBuilder where = new StringBuilder("WHERE 1 = 1");
        List<Object> parametros = new ArrayList<>();
        if (filtro.getTexto() != null) {
            where.append(" AND (ca.numero_cuenta ILIKE ? OR d.dni LIKE ? OR (d.nombres || ' ' || d.apellidos) ILIKE ? "
                       + "OR d.apellidos ILIKE ? OR (cp.serie || '-' || LPAD(cp.numero::text, 8, '0')) ILIKE ?)");
            for (int i = 0; i < 5; i++) {
                parametros.add(filtro.getPatron());
            }
        }
        if (filtro.getEstado() != null && ESTADOS.contains(filtro.getEstado())) {
            where.append(" AND ca.estado_cuenta = ?");
            parametros.add(filtro.getEstado());
        }
        if (filtro.getRango().isPorDefecto()) {
            // Rango automático (últimos 30 días): las cuentas POR COBRAR se muestran siempre, aunque sean antiguas
            StringBuilder rango = new StringBuilder("ca.estado_cuenta = 'PENDIENTE'");
            List<Object> parametrosRango = new ArrayList<>();
            StringBuilder fechas = new StringBuilder("1 = 1");
            RepositorioUtil.filtrarFechas(fechas, parametrosRango, "ca.fecha_creacion", filtro.getRango());
            where.append(" AND (").append(rango).append(" OR (").append(fechas).append("))");
            parametros.addAll(parametrosRango);
        } else {
            RepositorioUtil.filtrarFechas(where, parametros, "ca.fecha_creacion", filtro.getRango());
        }
        try {
            return RepositorioUtil.paginar(conexion(), SELECT_CUENTA, where.toString(), parametros,
                    "CASE ca.estado_cuenta WHEN 'PENDIENTE' THEN 0 ELSE 1 END, ca.fecha_creacion DESC", filtro,
                    CuentaAtencionRepositoryImpl::mapear);
        } catch (SQLException e) {
            throw new RepositorioException("Error al listar las cuentas de atención", e);
        }
    }

    @Override
    public Optional<CuentaAtencionDTO> buscarPorId(int idCuenta) {
        try {
            Connection cn = conexion();
            Optional<CuentaAtencionDTO> cuenta = RepositorioUtil.unaFila(cn,
                    SELECT_CUENTA + "WHERE ca.id_cuenta_atencion = ?", idCuenta, CuentaAtencionRepositoryImpl::mapear);
            if (cuenta.isEmpty()) {
                return cuenta;
            }
            CuentaAtencionDTO c = cuenta.get();
            c.setDetalles(detalles(cn, idCuenta));
            try (PreparedStatement stmt = cn.prepareStatement(SELECT_PAGO
                    + "WHERE pg.id_cuenta_atencion = ? ORDER BY pg.id_pago")) {
                stmt.setInt(1, idCuenta);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        PagoDTO p = mapearPago(rs);
                        if (p.isRechazado()) {
                            c.getIntentosRechazados().add(p);
                        } else {
                            c.setPago(p);       // el aprobado (o el anulado si la cuenta se anuló)
                        }
                    }
                }
            }
            return Optional.of(c);
        } catch (SQLException e) {
            throw new RepositorioException("Error al buscar la cuenta de atención", e);
        }
    }

    @Override
    public int crear(int idPaciente, int idCobertura, String observacion, String usuario) {
        String sql = "INSERT INTO tt_cuenta_atencion (id_paciente, id_cobertura, observacion, usuario_registro) "
                   + "VALUES (?, ?, ?, ?) RETURNING id_cuenta_atencion";
        try (PreparedStatement stmt = conexion().prepareStatement(sql)) {
            stmt.setInt(1, idPaciente);
            stmt.setInt(2, idCobertura);
            RepositorioUtil.textoONulo(stmt, 3, observacion);
            stmt.setString(4, usuario);
            try (ResultSet rs = stmt.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new RepositorioException("Error al abrir la cuenta de atención", e);
        }
    }

    @Override
    public Optional<Integer> pendienteDelPaciente(int idPaciente) {
        String sql = "SELECT id_cuenta_atencion FROM tt_cuenta_atencion WHERE id_paciente = ? AND estado_cuenta = 'PENDIENTE' "
                   + "ORDER BY id_cuenta_atencion DESC LIMIT 1";
        try {
            return RepositorioUtil.unaFila(conexion(), sql, idPaciente, rs -> rs.getInt(1));
        } catch (SQLException e) {
            throw new RepositorioException("Error al buscar cuentas pendientes", e);
        }
    }

    @Override
    public boolean guardarDetalles(int idCuenta, List<DetalleCuentaDTO> detalles, TotalesCuenta t, String usuario) {
        try (Connection cn = ConexionBDSingleton.getInstancia().abrirConexionTransaccional()) {
            try {
                // 1. Bloquea la cabecera: nadie puede cobrarla mientras se cambian sus líneas
                try (PreparedStatement stmt = cn.prepareStatement(
                        "SELECT estado_cuenta FROM tt_cuenta_atencion WHERE id_cuenta_atencion = ? FOR UPDATE")) {
                    stmt.setInt(1, idCuenta);
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (!rs.next() || !CuentaAtencionDTO.PENDIENTE.equals(rs.getString(1))) {
                            cn.rollback();
                            return false;
                        }
                    }
                }
                // 2. Reemplaza el detalle (mientras está pendiente es un borrador)
                try (PreparedStatement stmt = cn.prepareStatement("DELETE FROM tt_detalle_cuenta WHERE id_cuenta_atencion = ?")) {
                    stmt.setInt(1, idCuenta);
                    stmt.executeUpdate();
                }
                try (PreparedStatement stmt = cn.prepareStatement(
                        "INSERT INTO tt_detalle_cuenta (id_cuenta_atencion, id_producto_servicio, descripcion, tipo, cantidad, "
                      + "       precio_unitario, afecto_igv, porcentaje_cubierto, monto_aseguradora, monto_descuento, importe, "
                      + "       usuario_registro) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
                    for (DetalleCuentaDTO d : detalles) {
                        stmt.setInt(1, idCuenta);
                        stmt.setInt(2, d.getIdProductoServicio());
                        stmt.setString(3, d.getDescripcion());
                        stmt.setString(4, d.getTipo());
                        stmt.setInt(5, d.getCantidad());
                        stmt.setBigDecimal(6, d.getPrecioUnitario());
                        stmt.setBoolean(7, d.isAfectoIgv());
                        stmt.setBigDecimal(8, d.getPorcentajeCubierto());
                        stmt.setBigDecimal(9, d.getMontoAseguradora());
                        stmt.setBigDecimal(10, d.getMontoDescuento());
                        stmt.setBigDecimal(11, d.getImporte());
                        stmt.setString(12, usuario);
                        stmt.addBatch();
                    }
                    stmt.executeBatch();
                }
                // 3. Totales de la cabecera
                try (PreparedStatement stmt = cn.prepareStatement(
                        "UPDATE tt_cuenta_atencion SET op_gravada = ?, op_exonerada = ?, igv = ?, total = ?, "
                      + "       monto_aseguradora = ?, monto_descuento = ? WHERE id_cuenta_atencion = ?")) {
                    stmt.setBigDecimal(1, t.getOpGravada());
                    stmt.setBigDecimal(2, t.getOpExonerada());
                    stmt.setBigDecimal(3, t.getIgv());
                    stmt.setBigDecimal(4, t.getTotal());
                    stmt.setBigDecimal(5, t.getMontoAseguradora());
                    stmt.setBigDecimal(6, t.getMontoDescuento());
                    stmt.setInt(7, idCuenta);
                    stmt.executeUpdate();
                }
                cn.commit();
                return true;
            } catch (SQLException | RuntimeException e) {
                cn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new RepositorioException("Error al guardar el detalle de la cuenta", e);
        }
    }

    @Override
    public Optional<CoberturaDTO> coberturaDelPaciente(int idPaciente) {
        String sql = "SELECT cb.id_cobertura, cb.nombre, cb.tipo, cb.porcentaje_cobertura "
                   + "FROM tt_paciente p JOIN tm_cobertura cb ON cb.id_cobertura = p.id_cobertura "
                   + "WHERE p.id_paciente = ? AND p.estado = 'Activo'";
        try {
            return RepositorioUtil.unaFila(conexion(), sql, idPaciente, rs -> new CoberturaDTO(rs.getInt(1),
                    rs.getString(2), rs.getString(3), rs.getBigDecimal(4)));
        } catch (SQLException e) {
            throw new RepositorioException("Error al leer la cobertura del paciente", e);
        }
    }

    @Override
    public List<ProductoServicioDTO> itemsVendibles() {
        String sql = "SELECT ps.id_producto_servicio, ps.codigo, ps.nombre, ps.tipo, ps.precio, ps.afecto_igv, ps.stock, "
                   + "       c.nombre AS categoria, um.abreviatura AS unidad "
                   + "FROM tt_producto_servicio ps "
                   + "JOIN tm_categoria c ON c.id_categoria = ps.id_categoria "
                   + "JOIN tm_unidad_medida um ON um.id_unidad_medida = ps.id_unidad_medida "
                   + "WHERE ps.estado = 'Activo' ORDER BY ps.tipo DESC, ps.nombre";
        List<ProductoServicioDTO> lista = new ArrayList<>();
        try (PreparedStatement stmt = conexion().prepareStatement(sql); ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                ProductoServicioDTO p = new ProductoServicioDTO();
                p.setIdProductoServicio(rs.getInt("id_producto_servicio"));
                p.setCodigo(rs.getString("codigo"));
                p.setNombre(rs.getString("nombre"));
                p.setTipo(rs.getString("tipo"));
                p.setPrecio(rs.getBigDecimal("precio"));
                p.setAfectoIgv(rs.getBoolean("afecto_igv"));
                p.setStock(RepositorioUtil.enteroONulo(rs, "stock"));
                p.setNombreCategoria(rs.getString("categoria"));
                p.setUnidad(rs.getString("unidad"));
                p.setEstado("Activo");
                lista.add(p);
            }
            return lista;
        } catch (SQLException e) {
            throw new RepositorioException("Error al leer el tarifario", e);
        }
    }

    @Override
    public List<CuentaAtencionDTO> listarDelUsuarioPaciente(int idUsuario) {
        // Pagadas, y anuladas solo si llegaron a cobrarse (una cuenta anulada sin pago no es un pago del paciente)
        String sql = SELECT_CUENTA + "WHERE d.id_usuario = ? AND (ca.estado_cuenta = 'PAGADA' "
                   + "      OR (ca.estado_cuenta = 'ANULADA' AND cp.id_comprobante IS NOT NULL)) "
                   + "ORDER BY ca.fecha_pago DESC NULLS LAST, ca.id_cuenta_atencion DESC LIMIT 100";
        List<CuentaAtencionDTO> lista = new ArrayList<>();
        try (PreparedStatement stmt = conexion().prepareStatement(sql)) {
            stmt.setInt(1, idUsuario);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapear(rs));
                }
            }
            return lista;
        } catch (SQLException e) {
            throw new RepositorioException("Error al listar los pagos del paciente", e);
        }
    }

    @Override
    public int contarPendientes() {
        try (PreparedStatement stmt = conexion().prepareStatement(
                "SELECT COUNT(*) FROM tt_cuenta_atencion WHERE estado_cuenta = 'PENDIENTE'");
             ResultSet rs = stmt.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        } catch (SQLException e) {
            throw new RepositorioException("Error al contar cuentas pendientes", e);
        }
    }
}
