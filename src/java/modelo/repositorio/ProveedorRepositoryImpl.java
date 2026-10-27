package modelo.repositorio;

import configuracion.ConexionBDSingleton;
import modelo.dto.FiltroBusquedaDTO;
import modelo.dto.PaginaDTO;
import modelo.dto.ProveedorDTO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** PATRÓN: Repository  ->  IMPLEMENTACIÓN (SQL) de ProveedorRepository. */
public class ProveedorRepositoryImpl implements ProveedorRepository {

    private static final String SELECT_PROVEEDOR =
          "SELECT pr.id_proveedor, pr.ruc, pr.razon_social, pr.nombre_comercial, r.nombre AS rubro, "
        + "       ct.nombre_contacto AS contacto_nombre, ct.telefono, ct.correo_electronico, pr.direccion, pr.estado, "
        + "       pr.fecha_creacion, pr.usuario_registro, "
        + "       (SELECT COUNT(*) FROM vw_producto_servicio ps "
        + "        WHERE ps.id_proveedor = pr.id_proveedor AND ps.estado = 'Activo') AS productos "
        + "FROM tt_proveedor pr "
        + "JOIN tm_rubro r ON r.id_rubro = pr.id_rubro "
        + "LEFT JOIN LATERAL (SELECT nombre_contacto, telefono, correo_electronico FROM tt_proveedor_contacto c "
        + "                   WHERE c.id_proveedor = pr.id_proveedor AND c.estado = 'Activo' "
        + "                   ORDER BY c.id_proveedor_contacto LIMIT 1) ct ON TRUE ";

    private Connection conexion() throws SQLException {
        return ConexionBDSingleton.getInstancia().getConexion();
    }

    private static ProveedorDTO mapear(ResultSet rs) throws SQLException {
        ProveedorDTO p = new ProveedorDTO();
        p.setIdProveedor(rs.getInt("id_proveedor"));
        p.setRuc(rs.getString("ruc"));
        p.setRazonSocial(rs.getString("razon_social"));
        p.setNombreComercial(rs.getString("nombre_comercial"));
        p.setRubro(rs.getString("rubro"));
        p.setContactoNombre(rs.getString("contacto_nombre"));
        p.setTelefono(rs.getString("telefono"));
        p.setCorreo(rs.getString("correo_electronico"));
        p.setDireccion(rs.getString("direccion"));
        p.setEstado(rs.getString("estado"));
        p.setFechaCreacion(rs.getTimestamp("fecha_creacion"));
        p.setUsuarioRegistro(rs.getString("usuario_registro"));
        p.setProductos(rs.getInt("productos"));
        return p;
    }

    @Override
    public PaginaDTO<ProveedorDTO> listar(FiltroBusquedaDTO filtro) {
        StringBuilder where = new StringBuilder("WHERE 1 = 1");
        List<Object> parametros = new ArrayList<>();
        if (filtro.getTexto() != null) {
            where.append(" AND (pr.ruc ILIKE ? OR pr.razon_social ILIKE ? OR pr.nombre_comercial ILIKE ? OR r.nombre ILIKE ?)");
            for (int i = 0; i < 4; i++) {
                parametros.add(filtro.getPatron());
            }
        }
        RepositorioUtil.filtrarEstado(where, "pr", filtro);
        RepositorioUtil.filtrarFechas(where, parametros, "pr.fecha_creacion", filtro.getRango());
        try {
            return RepositorioUtil.paginar(conexion(), SELECT_PROVEEDOR, where.toString(), parametros,
                    "pr.razon_social", filtro, ProveedorRepositoryImpl::mapear);
        } catch (SQLException e) {
            throw new RepositorioException("Error al listar proveedores", e);
        }
    }

    @Override
    public Optional<ProveedorDTO> buscarPorId(Integer id) {
        try {
            return RepositorioUtil.unaFila(conexion(), SELECT_PROVEEDOR + "WHERE pr.id_proveedor = ?", id,
                    ProveedorRepositoryImpl::mapear);
        } catch (SQLException e) {
            throw new RepositorioException("Error al buscar el proveedor", e);
        }
    }

    @Override
    public Integer insertar(ProveedorDTO p) {
        String sql = "INSERT INTO tt_proveedor (ruc, razon_social, nombre_comercial, id_rubro, direccion, estado, usuario_registro) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?) RETURNING id_proveedor";
        try (Connection cn = ConexionBDSingleton.getInstancia().abrirConexionTransaccional()) {
            try {
                int id;
                try (PreparedStatement stmt = cn.prepareStatement(sql)) {
                    llenar(cn, stmt, p);
                    stmt.setString(7, p.getUsuarioRegistro());
                    try (ResultSet rs = stmt.executeQuery()) {
                        rs.next();
                        id = rs.getInt(1);
                    }
                }
                guardarContacto(cn, id, p);
                cn.commit();
                return id;
            } catch (SQLException | RuntimeException e) {
                cn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new RepositorioException("Error al registrar el proveedor", e);
        }
    }

    @Override
    public boolean actualizar(ProveedorDTO p) {
        String sql = "UPDATE tt_proveedor SET ruc = ?, razon_social = ?, nombre_comercial = ?, id_rubro = ?, direccion = ?, "
                   + "       estado = ? WHERE id_proveedor = ?";
        try (Connection cn = ConexionBDSingleton.getInstancia().abrirConexionTransaccional()) {
            try {
                boolean actualizado;
                try (PreparedStatement stmt = cn.prepareStatement(sql)) {
                    llenar(cn, stmt, p);
                    stmt.setInt(7, p.getIdProveedor());
                    actualizado = stmt.executeUpdate() > 0;
                }
                if (actualizado) {
                    guardarContacto(cn, p.getIdProveedor(), p);
                }
                cn.commit();
                return actualizado;
            } catch (SQLException | RuntimeException e) {
                cn.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new RepositorioException("Error al actualizar el proveedor", e);
        }
    }

    @Override
    public java.util.List<String> rubros() {
        java.util.List<String> lista = new ArrayList<>();
        try (PreparedStatement stmt = conexion().prepareStatement(
                "SELECT nombre FROM tm_rubro WHERE estado = 'Activo' ORDER BY nombre"); ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                lista.add(rs.getString(1));
            }
            return lista;
        } catch (SQLException e) {
            throw new RepositorioException("Error al leer los rubros", e);
        }
    }

    private static int idRubro(Connection cn, String nombre, String usuario) throws SQLException {
        try (PreparedStatement stmt = cn.prepareStatement(
                "INSERT INTO tm_rubro (nombre, usuario_registro) VALUES (?, ?) "
              + "ON CONFLICT (nombre) DO UPDATE SET nombre = EXCLUDED.nombre RETURNING id_rubro")) {
            stmt.setString(1, nombre);
            stmt.setString(2, usuario);
            try (ResultSet rs = stmt.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    private static void guardarContacto(Connection cn, int idProveedor, ProveedorDTO p) throws SQLException {
        boolean vacio = isBlank(p.getContactoNombre()) && isBlank(p.getTelefono()) && isBlank(p.getCorreo());
        int filas;
        try (PreparedStatement stmt = cn.prepareStatement(
                "UPDATE tt_proveedor_contacto SET nombre_contacto = ?, telefono = ?, correo_electronico = ?, estado = ? "
              + "WHERE id_proveedor_contacto = (SELECT id_proveedor_contacto FROM tt_proveedor_contacto "
              + "      WHERE id_proveedor = ? ORDER BY id_proveedor_contacto LIMIT 1)")) {
            RepositorioUtil.textoONulo(stmt, 1, p.getContactoNombre());
            RepositorioUtil.textoONulo(stmt, 2, p.getTelefono());
            RepositorioUtil.textoONulo(stmt, 3, p.getCorreo());
            stmt.setString(4, vacio ? "Inactivo" : "Activo");
            stmt.setInt(5, idProveedor);
            filas = stmt.executeUpdate();
        }
        if (filas == 0 && !vacio) {
            try (PreparedStatement stmt = cn.prepareStatement(
                    "INSERT INTO tt_proveedor_contacto (id_proveedor, nombre_contacto, telefono, correo_electronico, usuario_registro) "
                  + "VALUES (?, ?, ?, ?, ?)")) {
                stmt.setInt(1, idProveedor);
                RepositorioUtil.textoONulo(stmt, 2, p.getContactoNombre());
                RepositorioUtil.textoONulo(stmt, 3, p.getTelefono());
                RepositorioUtil.textoONulo(stmt, 4, p.getCorreo());
                stmt.setString(5, p.getUsuarioRegistro());
                stmt.executeUpdate();
            }
        }
    }

    private static boolean isBlank(String v) {
        return v == null || v.isBlank();
    }

    /** Eliminación lógica: deja de aparecer al elegir proveedor, pero se conserva. */
    @Override
    public boolean eliminar(Integer id) {
        return cambiarEstado(id, "Inactivo");
    }

    @Override
    public boolean reactivar(int id) {
        return cambiarEstado(id, "Activo");
    }

    @Override
    public boolean existeRuc(String ruc, int idProveedorExcluir) {
        try {
            return RepositorioUtil.existe(conexion(), "SELECT 1 FROM tt_proveedor WHERE ruc = ? AND id_proveedor <> ?",
                    ruc, idProveedorExcluir);
        } catch (SQLException e) {
            throw new RepositorioException("Error al verificar el RUC", e);
        }
    }

    private boolean cambiarEstado(int id, String estado) {
        try {
            return RepositorioUtil.cambiarEstado(conexion(), "tt_proveedor", "id_proveedor", id, estado);
        } catch (SQLException e) {
            throw new RepositorioException("Error al cambiar el estado del proveedor", e);
        }
    }

    private static void llenar(Connection cn, PreparedStatement stmt, ProveedorDTO p) throws SQLException {
        stmt.setString(1, p.getRuc());
        stmt.setString(2, p.getRazonSocial());
        RepositorioUtil.textoONulo(stmt, 3, p.getNombreComercial());
        stmt.setInt(4, idRubro(cn, p.getRubro(), p.getUsuarioRegistro()));
        RepositorioUtil.textoONulo(stmt, 5, p.getDireccion());
        stmt.setString(6, p.getEstado());
    }
}
