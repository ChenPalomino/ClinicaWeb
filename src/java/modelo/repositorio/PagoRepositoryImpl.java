package modelo.repositorio;

import configuracion.ConexionBDSingleton;
import modelo.cobranza.comprobante.Comprobante;
import modelo.dto.ComprobanteDTO;
import modelo.dto.CuentaAtencionDTO;
import modelo.dto.DetalleCuentaDTO;
import modelo.dto.OpcionDTO;
import modelo.dto.PagoDTO;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * PATRÓN: Repository  ->  IMPLEMENTACIÓN (SQL) de PagoRepository.
 *
 * RNF-08 (integridad): el cobro se hace en UNA transacción sobre una conexión
 * exclusiva. "SELECT ... FOR UPDATE" bloquea la cuenta, así si dos cajeros
 * pulsan "Cobrar" a la vez, el segundo espera y luego ve que ya está pagada.
 * Además, un índice único impide dos pagos aprobados para la misma cuenta.
 */
public class PagoRepositoryImpl implements PagoRepository {

    private static final String INSERT_PAGO =
          "INSERT INTO tt_pago (id_cuenta_atencion, id_metodo_pago, monto, monto_recibido, vuelto, referencia, "
        + "                     estado_pago, motivo_rechazo, id_usuario_cajero, usuario_registro) "
        + "VALUES (?, (SELECT id_metodo_pago FROM tm_metodo_pago WHERE codigo = ? AND estado = 'Activo'), "
        + "        ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id_pago";

    private Connection conexion() throws SQLException {
        return ConexionBDSingleton.getInstancia().getConexion();
    }

    @Override
    public List<OpcionDTO> metodosActivos() {
        List<OpcionDTO> lista = new ArrayList<>();
        try (PreparedStatement stmt = conexion().prepareStatement(
                "SELECT id_metodo_pago, nombre, codigo FROM tm_metodo_pago WHERE estado = 'Activo' ORDER BY id_metodo_pago");
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                lista.add(new OpcionDTO(rs.getInt(1), rs.getString(2), rs.getString(3)));
            }
            return lista;
        } catch (SQLException e) {
            throw new RepositorioException("Error al leer los métodos de pago", e);
        }
    }

    private static int insertarPago(Connection cn, PagoDTO p) throws SQLException {
        try (PreparedStatement stmt = cn.prepareStatement(INSERT_PAGO)) {
            stmt.setInt(1, p.getIdCuentaAtencion());
            stmt.setString(2, p.getCodigoMetodo());
            stmt.setBigDecimal(3, p.getMonto());
            stmt.setBigDecimal(4, p.getMontoRecibido());
            stmt.setBigDecimal(5, p.getVuelto());
            RepositorioUtil.textoONulo(stmt, 6, p.getReferencia());
            stmt.setString(7, p.getEstadoPago());
            RepositorioUtil.textoONulo(stmt, 8, p.getMotivoRechazo());
            stmt.setInt(9, p.getIdUsuarioCajero());
            stmt.setString(10, p.getNombreCajero());
            try (ResultSet rs = stmt.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    @Override
    public int registrarRechazo(PagoDTO pago) {
        try {
            return insertarPago(conexion(), pago);
        } catch (SQLException e) {
            throw new RepositorioException("Error al registrar el pago rechazado", e);
        }
    }

    @Override
    public int registrarPagoAprobado(PagoDTO pago, Comprobante c, List<DetalleCuentaDTO> detalles, LocalDate hoy) {
        try (Connection cn = ConexionBDSingleton.getInstancia().abrirConexionTransaccional()) {
            try {
                bloquearCuentaPendiente(cn, pago.getIdCuentaAtencion(), pago.getMonto());
                if (cajaCerrada(cn, pago.getIdUsuarioCajero(), hoy)) {
                    throw new OperacionRechazadaException("Ya cerraste tu caja de hoy: no se pueden registrar más cobros "
                            + "con tu usuario hasta mañana. Pide a otro cajero que lo cobre.");
                }
                descontarStock(cn, detalles);
                int idPago = insertarPago(cn, pago);
                c.setNumero(siguienteNumero(cn, c.getSerie()));
                int idComprobante = insertarComprobante(cn, pago, idPago, c);
                try (PreparedStatement stmt = cn.prepareStatement(
                        "UPDATE tt_cuenta_atencion SET estado_cuenta = 'PAGADA', fecha_pago = LOCALTIMESTAMP "
                      + "WHERE id_cuenta_atencion = ?")) {
                    stmt.setInt(1, pago.getIdCuentaAtencion());
                    stmt.executeUpdate();
                }
                cn.commit();
                return idComprobante;
            } catch (SQLException | RuntimeException e) {
                cn.rollback();          // RNF-08: o se guarda TODO o NADA
                throw e;
            }
        } catch (SQLException e) {
            throw new RepositorioException("Error al registrar el pago", e);
        }
    }

    /** Bloquea la cabecera y comprueba que siga pendiente y con el total que vio el cajero. */
    private static void bloquearCuentaPendiente(Connection cn, int idCuenta, BigDecimal totalEsperado) throws SQLException {
        try (PreparedStatement stmt = cn.prepareStatement(
                "SELECT estado_cuenta, total FROM tt_cuenta_atencion WHERE id_cuenta_atencion = ? FOR UPDATE")) {
            stmt.setInt(1, idCuenta);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    throw new OperacionRechazadaException("La cuenta ya no existe.");
                }
                String estado = rs.getString(1);
                if (CuentaAtencionDTO.PAGADA.equals(estado)) {
                    throw new OperacionRechazadaException("Esta cuenta ya fue pagada (quizá otro cajero la cobró hace un momento). "
                            + "No se hizo un segundo cobro.");
                }
                if (!CuentaAtencionDTO.PENDIENTE.equals(estado)) {
                    throw new OperacionRechazadaException("Esta cuenta fue anulada y ya no se puede cobrar.");
                }
                if (rs.getBigDecimal(2).compareTo(totalEsperado) != 0) {
                    throw new OperacionRechazadaException("El total de la cuenta cambió mientras cobrabas. Revisa el detalle "
                            + "y vuelve a cobrar.");
                }
            }
        }
    }

    private static boolean cajaCerrada(Connection cn, int idUsuario, LocalDate fecha) throws SQLException {
        try (PreparedStatement stmt = cn.prepareStatement(
                "SELECT 1 FROM tt_cierre_caja WHERE id_usuario_cajero = ? AND fecha_caja = ?")) {
            stmt.setInt(1, idUsuario);
            stmt.setDate(2, Date.valueOf(fecha));
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    /** Descuenta el stock de cada producto; si alguno no alcanza, se cancela todo. */
    private static void descontarStock(Connection cn, List<DetalleCuentaDTO> detalles) throws SQLException {
        try (PreparedStatement stmt = cn.prepareStatement(
                "UPDATE tt_producto_servicio SET stock = stock - ? "
              + "WHERE id_producto_servicio = ? AND tipo = 'PRODUCTO' AND stock >= ?")) {
            for (DetalleCuentaDTO d : detalles) {
                if (!d.isProducto()) {
                    continue;
                }
                stmt.setInt(1, d.getCantidad());
                stmt.setInt(2, d.getIdProductoServicio());
                stmt.setInt(3, d.getCantidad());
                if (stmt.executeUpdate() == 0) {
                    throw new OperacionRechazadaException("No hay stock suficiente de «" + d.getDescripcion()
                            + "». Quita el producto o reduce la cantidad; no se cobró nada.");
                }
            }
        }
    }

    /** El UPDATE bloquea la fila de la serie: dos cobros simultáneos nunca reciben el mismo número. */
    private static int siguienteNumero(Connection cn, String serie) throws SQLException {
        try (PreparedStatement stmt = cn.prepareStatement(
                "UPDATE tm_serie_comprobante SET ultimo_numero = ultimo_numero + 1 "
              + "WHERE serie = ? AND estado = 'Activo' RETURNING ultimo_numero")) {
            stmt.setString(1, serie);
            try (ResultSet rs = stmt.executeQuery()) {
                if (!rs.next()) {
                    throw new OperacionRechazadaException("La serie " + serie + " no está activa. Avisa al administrador.");
                }
                return rs.getInt(1);
            }
        }
    }

    private static int insertarComprobante(Connection cn, PagoDTO pago, int idPago, Comprobante c) throws SQLException {
        String sql = "INSERT INTO tt_comprobante (id_cuenta_atencion, id_pago, tipo_comprobante, serie, numero, tipo_documento, "
                   + "       numero_documento, nombre_cliente, direccion_cliente, op_gravada, op_exonerada, igv, total, "
                   + "       usuario_registro) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id_comprobante";
        try (PreparedStatement stmt = cn.prepareStatement(sql)) {
            stmt.setInt(1, pago.getIdCuentaAtencion());
            stmt.setInt(2, idPago);
            stmt.setString(3, c.getTipo());
            stmt.setString(4, c.getSerie());
            stmt.setInt(5, c.getNumero());
            stmt.setString(6, c.getTipoDocumento());
            RepositorioUtil.textoONulo(stmt, 7, c.getNumeroDocumento());
            stmt.setString(8, c.getNombreCliente());
            RepositorioUtil.textoONulo(stmt, 9, c.getDireccionCliente());
            stmt.setBigDecimal(10, c.getOpGravada());
            stmt.setBigDecimal(11, c.getOpExonerada());
            stmt.setBigDecimal(12, c.getIgv());
            stmt.setBigDecimal(13, c.getTotal());
            stmt.setString(14, pago.getNombreCajero());
            try (ResultSet rs = stmt.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    @Override
    public void anular(int idCuenta, int idUsuarioAnula, String motivo) {
        try (Connection cn = ConexionBDSingleton.getInstancia().abrirConexionTransaccional()) {
            try {
                String estado;
                try (PreparedStatement stmt = cn.prepareStatement(
                        "SELECT estado_cuenta FROM tt_cuenta_atencion WHERE id_cuenta_atencion = ? FOR UPDATE")) {
                    stmt.setInt(1, idCuenta);
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (!rs.next()) {
                            throw new OperacionRechazadaException("La cuenta ya no existe.");
                        }
                        estado = rs.getString(1);
                    }
                }
                if (CuentaAtencionDTO.ANULADA.equals(estado)) {
                    throw new OperacionRechazadaException("La cuenta ya estaba anulada.");
                }
                if (CuentaAtencionDTO.PAGADA.equals(estado)) {
                    revertirPago(cn, idCuenta);
                }
                try (PreparedStatement stmt = cn.prepareStatement(
                        "UPDATE tt_cuenta_atencion SET estado_cuenta = 'ANULADA', motivo_anulacion = ?, id_usuario_anula = ?, "
                      + "       fecha_anulacion = LOCALTIMESTAMP WHERE id_cuenta_atencion = ?")) {
                    stmt.setString(1, motivo);
                    stmt.setInt(2, idUsuarioAnula);
                    stmt.setInt(3, idCuenta);
                    stmt.executeUpdate();
                }
                cn.commit();
            } catch (SQLException | RuntimeException e) {
                cn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new RepositorioException("Error al anular la cuenta", e);
        }
    }

    /** Anula pago y comprobante y devuelve el stock. Solo si la caja de ese día sigue abierta. */
    private static void revertirPago(Connection cn, int idCuenta) throws SQLException {
        try (PreparedStatement stmt = cn.prepareStatement(
                "SELECT 1 FROM tt_pago pg JOIN tt_cierre_caja cc ON cc.id_usuario_cajero = pg.id_usuario_cajero "
              + "     AND cc.fecha_caja = pg.fecha_creacion::date "
              + "WHERE pg.id_cuenta_atencion = ? AND pg.estado_pago = 'APROBADO'")) {
            stmt.setInt(1, idCuenta);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    throw new OperacionRechazadaException("La caja en la que se cobró esta cuenta ya se cerró: no se puede anular "
                            + "desde aquí (correspondería una nota de crédito).");
                }
            }
        }
        try (PreparedStatement stmt = cn.prepareStatement(
                "UPDATE tt_producto_servicio ps SET stock = ps.stock + dc.cantidad "
              + "FROM tt_detalle_cuenta dc WHERE dc.id_cuenta_atencion = ? AND dc.tipo = 'PRODUCTO' "
              + "AND ps.id_producto_servicio = dc.id_producto_servicio")) {
            stmt.setInt(1, idCuenta);
            stmt.executeUpdate();
        }
        try (PreparedStatement stmt = cn.prepareStatement(
                "UPDATE tt_pago SET estado_pago = 'ANULADO' WHERE id_cuenta_atencion = ? AND estado_pago = 'APROBADO'")) {
            stmt.setInt(1, idCuenta);
            stmt.executeUpdate();
        }
        try (PreparedStatement stmt = cn.prepareStatement(
                "UPDATE tt_comprobante SET estado_comprobante = 'ANULADO' WHERE id_cuenta_atencion = ?")) {
            stmt.setInt(1, idCuenta);
            stmt.executeUpdate();
        }
    }

    @Override
    public Optional<ComprobanteDTO> buscarComprobante(int idComprobante) {
        String sql = "SELECT cp.id_comprobante, cp.id_cuenta_atencion, ca.numero_cuenta, cp.tipo_comprobante, cp.serie, cp.numero, "
                   + "       cp.tipo_documento, cp.numero_documento, cp.nombre_cliente, cp.direccion_cliente, cp.op_gravada, "
                   + "       cp.op_exonerada, cp.igv, cp.total, cp.estado_comprobante, cp.fecha_creacion, "
                   + "       ca.monto_aseguradora, ca.monto_descuento, d.nombres || ' ' || d.apellidos AS paciente, "
                   + "       p.numero_historia, cb.nombre AS cobertura, COALESCE(d.id_usuario, 0) AS id_usuario_paciente, "
                   + "       mp.nombre AS metodo, pg.referencia, pg.monto_recibido, pg.vuelto, u.nombre_usuario AS cajero "
                   + "FROM tt_comprobante cp "
                   + "JOIN tt_cuenta_atencion ca ON ca.id_cuenta_atencion = cp.id_cuenta_atencion "
                   + "JOIN tt_paciente p ON p.id_paciente = ca.id_paciente "
                   + "JOIN tt_datos_personales d ON d.id_datos_personales = p.id_datos_personales "
                   + "JOIN tm_cobertura cb ON cb.id_cobertura = ca.id_cobertura "
                   + "JOIN tt_pago pg ON pg.id_pago = cp.id_pago "
                   + "JOIN tm_metodo_pago mp ON mp.id_metodo_pago = pg.id_metodo_pago "
                   + "JOIN tt_usuario u ON u.id_usuario = pg.id_usuario_cajero "
                   + "WHERE cp.id_comprobante = ?";
        try {
            Connection cn = conexion();
            Optional<ComprobanteDTO> encontrado = RepositorioUtil.unaFila(cn, sql, idComprobante, rs -> {
                ComprobanteDTO c = new ComprobanteDTO();
                c.setIdComprobante(rs.getInt("id_comprobante"));
                c.setIdCuentaAtencion(rs.getInt("id_cuenta_atencion"));
                c.setNumeroCuenta(rs.getString("numero_cuenta"));
                c.setTipoComprobante(rs.getString("tipo_comprobante"));
                c.setSerie(rs.getString("serie"));
                c.setNumero(rs.getInt("numero"));
                c.setTipoDocumento(rs.getString("tipo_documento"));
                c.setNumeroDocumento(rs.getString("numero_documento"));
                c.setNombreCliente(rs.getString("nombre_cliente"));
                c.setDireccionCliente(rs.getString("direccion_cliente"));
                c.setOpGravada(rs.getBigDecimal("op_gravada"));
                c.setOpExonerada(rs.getBigDecimal("op_exonerada"));
                c.setIgv(rs.getBigDecimal("igv"));
                c.setTotal(rs.getBigDecimal("total"));
                c.setEstadoComprobante(rs.getString("estado_comprobante"));
                c.setFechaEmision(rs.getTimestamp("fecha_creacion"));
                c.setMontoAseguradora(rs.getBigDecimal("monto_aseguradora"));
                c.setMontoDescuento(rs.getBigDecimal("monto_descuento"));
                c.setNombrePaciente(rs.getString("paciente"));
                c.setNumeroHistoria(rs.getString("numero_historia"));
                c.setNombreCobertura(rs.getString("cobertura"));
                c.setIdUsuarioPaciente(rs.getInt("id_usuario_paciente"));
                c.setMetodoPago(rs.getString("metodo"));
                c.setReferenciaPago(rs.getString("referencia"));
                c.setMontoRecibido(rs.getBigDecimal("monto_recibido"));
                c.setVuelto(rs.getBigDecimal("vuelto"));
                c.setCajero(rs.getString("cajero"));
                return c;
            });
            if (encontrado.isPresent()) {
                encontrado.get().setDetalles(CuentaAtencionRepositoryImpl.detalles(cn, encontrado.get().getIdCuentaAtencion()));
            }
            return encontrado;
        } catch (SQLException e) {
            throw new RepositorioException("Error al buscar el comprobante", e);
        }
    }

    @Override
    public List<PagoDTO> pagosDelCajero(int idUsuarioCajero, LocalDate fecha) {
        String sql = "SELECT pg.id_pago, pg.id_cuenta_atencion, pg.id_metodo_pago, mp.codigo, mp.nombre AS metodo, pg.monto, "
                   + "       pg.monto_recibido, pg.vuelto, pg.referencia, pg.estado_pago, pg.motivo_rechazo, "
                   + "       pg.id_usuario_cajero, u.nombre_usuario AS cajero, pg.fecha_creacion, ca.numero_cuenta, "
                   + "       d.nombres || ' ' || d.apellidos AS paciente, cp.serie, cp.numero "
                   + "FROM tt_pago pg "
                   + "JOIN tm_metodo_pago mp ON mp.id_metodo_pago = pg.id_metodo_pago "
                   + "JOIN tt_usuario u ON u.id_usuario = pg.id_usuario_cajero "
                   + "JOIN tt_cuenta_atencion ca ON ca.id_cuenta_atencion = pg.id_cuenta_atencion "
                   + "JOIN tt_paciente p ON p.id_paciente = ca.id_paciente "
                   + "JOIN tt_datos_personales d ON d.id_datos_personales = p.id_datos_personales "
                   + "LEFT JOIN tt_comprobante cp ON cp.id_pago = pg.id_pago "
                   + "WHERE pg.id_usuario_cajero = ? AND pg.fecha_creacion >= ? AND pg.fecha_creacion < ? "
                   + "ORDER BY pg.fecha_creacion";
        List<PagoDTO> lista = new ArrayList<>();
        try (PreparedStatement stmt = conexion().prepareStatement(sql)) {
            stmt.setInt(1, idUsuarioCajero);
            stmt.setDate(2, Date.valueOf(fecha));
            stmt.setDate(3, Date.valueOf(fecha.plusDays(1)));
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    PagoDTO p = CuentaAtencionRepositoryImpl.mapearPago(rs);
                    p.setNumeroCuenta(rs.getString("numero_cuenta"));
                    p.setNombrePaciente(rs.getString("paciente"));
                    if (rs.getString("serie") != null) {
                        p.setComprobante(Comprobante.numeroCompleto(rs.getString("serie"), rs.getInt("numero")));
                    }
                    lista.add(p);
                }
            }
            return lista;
        } catch (SQLException e) {
            throw new RepositorioException("Error al leer los pagos de la caja", e);
        }
    }
}
