package modelo.repositorio;

import configuracion.ConexionBDSingleton;
import modelo.dto.FiltroBusquedaDTO;
import modelo.dto.PaginaDTO;
import modelo.dto.ProductoServicioDTO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** PATRÓN: Repository  ->  IMPLEMENTACIÓN (SQL) de ProductoServicioRepository. */
public class ProductoServicioRepositoryImpl implements ProductoServicioRepository {

    private static final String SELECT_ITEM =
          "SELECT ps.id_producto_servicio, ps.codigo, ps.nombre, ps.descripcion, ps.tipo, ps.id_categoria, "
        + "       c.nombre AS categoria, ps.id_especialidad, e.nombre AS especialidad, ps.id_marca, m.nombre AS marca, "
        + "       ps.id_unidad_medida, um.abreviatura AS unidad, ps.id_proveedor, "
        + "       COALESCE(pr.nombre_comercial, pr.razon_social) AS proveedor, ps.precio, ps.afecto_igv, "
        + "       ps.stock, ps.stock_minimo, ps.estado, ps.fecha_creacion, ps.usuario_registro "
        + "FROM vw_producto_servicio ps "
        + "JOIN tm_categoria c ON c.id_categoria = ps.id_categoria "
        + "JOIN tm_unidad_medida um ON um.id_unidad_medida = ps.id_unidad_medida "
        + "LEFT JOIN tm_especialidad e ON e.id_especialidad = ps.id_especialidad "
        + "LEFT JOIN tm_marca m ON m.id_marca = ps.id_marca "
        + "LEFT JOIN tt_proveedor pr ON pr.id_proveedor = ps.id_proveedor ";

    private Connection conexion() throws SQLException {
        return ConexionBDSingleton.getInstancia().getConexion();
    }

    private static ProductoServicioDTO mapear(ResultSet rs) throws SQLException {
        ProductoServicioDTO p = new ProductoServicioDTO();
        p.setIdProductoServicio(rs.getInt("id_producto_servicio"));
        p.setCodigo(rs.getString("codigo"));
        p.setNombre(rs.getString("nombre"));
        p.setDescripcion(rs.getString("descripcion"));
        p.setTipo(rs.getString("tipo"));
        p.setIdCategoria(rs.getInt("id_categoria"));
        p.setNombreCategoria(rs.getString("categoria"));
        p.setIdEspecialidad(RepositorioUtil.enteroONulo(rs, "id_especialidad"));
        p.setNombreEspecialidad(rs.getString("especialidad"));
        p.setIdMarca(RepositorioUtil.enteroONulo(rs, "id_marca"));
        p.setNombreMarca(rs.getString("marca"));
        p.setIdUnidadMedida(rs.getInt("id_unidad_medida"));
        p.setUnidad(rs.getString("unidad"));
        p.setIdProveedor(RepositorioUtil.enteroONulo(rs, "id_proveedor"));
        p.setNombreProveedor(rs.getString("proveedor"));
        p.setPrecio(rs.getBigDecimal("precio"));
        p.setAfectoIgv(rs.getBoolean("afecto_igv"));
        p.setStock(RepositorioUtil.enteroONulo(rs, "stock"));
        p.setStockMinimo(RepositorioUtil.enteroONulo(rs, "stock_minimo"));
        p.setEstado(rs.getString("estado"));
        p.setFechaCreacion(rs.getTimestamp("fecha_creacion"));
        p.setUsuarioRegistro(rs.getString("usuario_registro"));
        return p;
    }

    @Override
    public PaginaDTO<ProductoServicioDTO> listar(FiltroBusquedaDTO filtro) {
        StringBuilder where = new StringBuilder("WHERE 1 = 1");
        List<Object> parametros = new ArrayList<>();
        if (filtro.getTexto() != null) {
            where.append(" AND (ps.codigo ILIKE ? OR ps.nombre ILIKE ?)");
            parametros.add(filtro.getPatron());
            parametros.add(filtro.getPatron());
        }
        if (filtro.getTipo() != null) {
            where.append(" AND ps.tipo = ?");
            parametros.add(filtro.getTipo());
        }
        if (filtro.getGrupo() != null) {
            where.append(" AND ps.id_categoria = ?");
            parametros.add(filtro.getGrupo());
        }
        if ("REPONER".equals(filtro.getEstado())) {
            where.append(" AND ps.tipo = 'PRODUCTO' AND ps.estado = 'Activo' AND ps.stock <= COALESCE(ps.stock_minimo, 0)");
        }
        RepositorioUtil.filtrarEstado(where, "ps", filtro);
        RepositorioUtil.filtrarFechas(where, parametros, "ps.fecha_creacion", filtro.getRango());
        try {
            return RepositorioUtil.paginar(conexion(), SELECT_ITEM, where.toString(), parametros,
                    "ps.tipo DESC, ps.codigo", filtro, ProductoServicioRepositoryImpl::mapear);
        } catch (SQLException e) {
            throw new RepositorioException("Error al listar el tarifario", e);
        }
    }

    @Override
    public Optional<ProductoServicioDTO> buscarPorId(Integer id) {
        try {
            return RepositorioUtil.unaFila(conexion(), SELECT_ITEM + "WHERE ps.id_producto_servicio = ?", id,
                    ProductoServicioRepositoryImpl::mapear);
        } catch (SQLException e) {
            throw new RepositorioException("Error al buscar el servicio o producto", e);
        }
    }

    @Override
    public Integer insertar(ProductoServicioDTO p) {
        String sql = "INSERT INTO tt_producto_servicio (codigo, nombre, descripcion, id_categoria, id_unidad_medida, precio, "
                   + "       afecto_igv, estado, usuario_registro) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id_producto_servicio";
        try (Connection cn = ConexionBDSingleton.getInstancia().abrirConexionTransaccional()) {
            try {
                int id;
                try (PreparedStatement stmt = cn.prepareStatement(sql)) {
                    llenar(stmt, p);
                    stmt.setString(9, p.getUsuarioRegistro());
                    try (ResultSet rs = stmt.executeQuery()) {
                        rs.next();
                        id = rs.getInt(1);
                    }
                }
                p.setIdProductoServicio(id);
                guardarSubtipo(cn, p);
                cn.commit();
                return id;
            } catch (SQLException | RuntimeException e) {
                cn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new RepositorioException("Error al registrar el servicio o producto", e);
        }
    }

    @Override
    public boolean actualizar(ProductoServicioDTO p) {
        String sql = "UPDATE tt_producto_servicio SET codigo = ?, nombre = ?, descripcion = ?, id_categoria = ?, "
                   + "       id_unidad_medida = ?, precio = ?, afecto_igv = ?, estado = ? WHERE id_producto_servicio = ?";
        try (Connection cn = ConexionBDSingleton.getInstancia().abrirConexionTransaccional()) {
            try {
                boolean actualizado;
                try (PreparedStatement stmt = cn.prepareStatement(sql)) {
                    llenar(stmt, p);
                    stmt.setInt(9, p.getIdProductoServicio());
                    actualizado = stmt.executeUpdate() > 0;
                }
                if (actualizado) {
                    guardarSubtipo(cn, p);
                }
                cn.commit();
                return actualizado;
            } catch (SQLException | RuntimeException e) {
                cn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new RepositorioException("Error al actualizar el servicio o producto", e);
        }
    }

    private static void guardarSubtipo(Connection cn, ProductoServicioDTO p) throws SQLException {
        String otro = p.isProducto() ? "tt_servicio" : "tt_producto";
        try (PreparedStatement stmt = cn.prepareStatement("DELETE FROM " + otro + " WHERE id_producto_servicio = ?")) {
            stmt.setInt(1, p.getIdProductoServicio());
            stmt.executeUpdate();
        }
        if (p.isProducto()) {
            try (PreparedStatement stmt = cn.prepareStatement(
                    "INSERT INTO tt_producto (id_producto_servicio, id_marca, id_proveedor, stock, stock_minimo, usuario_registro) "
                  + "VALUES (?, ?, ?, ?, ?, ?) ON CONFLICT (id_producto_servicio) DO UPDATE SET id_marca = EXCLUDED.id_marca, "
                  + "id_proveedor = EXCLUDED.id_proveedor, stock = EXCLUDED.stock, stock_minimo = EXCLUDED.stock_minimo")) {
                stmt.setInt(1, p.getIdProductoServicio());
                RepositorioUtil.enteroONulo(stmt, 2, p.getIdMarca());
                RepositorioUtil.enteroONulo(stmt, 3, p.getIdProveedor());
                stmt.setInt(4, p.getStock() == null ? 0 : p.getStock());
                stmt.setInt(5, p.getStockMinimo() == null ? 0 : p.getStockMinimo());
                stmt.setString(6, p.getUsuarioRegistro());
                stmt.executeUpdate();
            }
        } else {
            try (PreparedStatement stmt = cn.prepareStatement(
                    "INSERT INTO tt_servicio (id_producto_servicio, id_especialidad, usuario_registro) VALUES (?, ?, ?) "
                  + "ON CONFLICT (id_producto_servicio) DO UPDATE SET id_especialidad = EXCLUDED.id_especialidad")) {
                stmt.setInt(1, p.getIdProductoServicio());
                RepositorioUtil.enteroONulo(stmt, 2, p.getIdEspecialidad());
                stmt.setString(3, p.getUsuarioRegistro());
                stmt.executeUpdate();
            }
        }
    }

    /** Eliminación lógica: deja de ofrecerse en caja, pero las ventas pasadas lo siguen mostrando. */
    @Override
    public boolean eliminar(Integer id) {
        return cambiarEstado(id, "Inactivo");
    }

    @Override
    public boolean reactivar(int id) {
        return cambiarEstado(id, "Activo");
    }

    @Override
    public boolean existeCodigo(String codigo, int idExcluir) {
        try {
            return RepositorioUtil.existe(conexion(),
                    "SELECT 1 FROM tt_producto_servicio WHERE UPPER(codigo) = UPPER(?) AND id_producto_servicio <> ?",
                    codigo, idExcluir);
        } catch (SQLException e) {
            throw new RepositorioException("Error al verificar el código", e);
        }
    }

    @Override
    public String siguienteCodigo(String tipo) {
        String prefijo = ProductoServicioDTO.PRODUCTO.equals(tipo) ? "PRD" : "SRV";
        String sql = "SELECT COALESCE(MAX(CAST(SUBSTRING(codigo FROM 5) AS INTEGER)), 0) + 1 "
                   + "FROM tt_producto_servicio WHERE codigo ~ ?";
        try (PreparedStatement stmt = conexion().prepareStatement(sql)) {
            stmt.setString(1, "^" + prefijo + "-[0-9]{4}$");
            try (ResultSet rs = stmt.executeQuery()) {
                rs.next();
                return String.format("%s-%04d", prefijo, rs.getInt(1));
            }
        } catch (SQLException e) {
            throw new RepositorioException("Error al generar el código", e);
        }
    }

    @Override
    public int contarStockBajo() {
        String sql = "SELECT COUNT(*) FROM vw_producto_servicio "
                   + "WHERE tipo = 'PRODUCTO' AND estado = 'Activo' AND stock <= COALESCE(stock_minimo, 0)";
        try (PreparedStatement stmt = conexion().prepareStatement(sql); ResultSet rs = stmt.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        } catch (SQLException e) {
            throw new RepositorioException("Error al revisar el stock", e);
        }
    }

    private boolean cambiarEstado(int id, String estado) {
        try {
            return RepositorioUtil.cambiarEstado(conexion(), "tt_producto_servicio", "id_producto_servicio", id, estado);
        } catch (SQLException e) {
            throw new RepositorioException("Error al cambiar el estado", e);
        }
    }

    private static void llenar(PreparedStatement stmt, ProductoServicioDTO p) throws SQLException {
        stmt.setString(1, p.getCodigo());
        stmt.setString(2, p.getNombre());
        RepositorioUtil.textoONulo(stmt, 3, p.getDescripcion());
        stmt.setInt(4, p.getIdCategoria());
        stmt.setInt(5, p.getIdUnidadMedida());
        stmt.setBigDecimal(6, p.getPrecio());
        stmt.setBoolean(7, p.isAfectoIgv());
        stmt.setString(8, p.getEstado());
    }
}
