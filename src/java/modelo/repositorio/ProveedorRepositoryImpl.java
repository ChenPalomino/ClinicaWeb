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
          "SELECT pr.id_proveedor, pr.ruc, pr.razon_social, pr.nombre_comercial, pr.rubro, pr.contacto_nombre, "
        + "       pr.telefono, pr.correo_electronico, pr.direccion, pr.estado, pr.fecha_creacion, pr.usuario_registro, "
        + "       (SELECT COUNT(*) FROM tt_producto_servicio ps "
        + "        WHERE ps.id_proveedor = pr.id_proveedor AND ps.estado = 'Activo') AS productos "
        + "FROM tt_proveedor pr ";

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
            where.append(" AND (pr.ruc ILIKE ? OR pr.razon_social ILIKE ? OR pr.nombre_comercial ILIKE ? OR pr.rubro ILIKE ?)");
            for (int i = 0; i < 4; i++) {
                parametros.add(filtro.getPatron());
            }
        }
        RepositorioUtil.filtrarEstado(where, "pr", filtro);
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
        String sql = "INSERT INTO tt_proveedor (ruc, razon_social, nombre_comercial, rubro, contacto_nombre, telefono, "
                   + "                          correo_electronico, direccion, estado, usuario_registro) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) RETURNING id_proveedor";
        try (PreparedStatement stmt = conexion().prepareStatement(sql)) {
            llenar(stmt, p);
            stmt.setString(10, p.getUsuarioRegistro());
            try (ResultSet rs = stmt.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new RepositorioException("Error al registrar el proveedor", e);
        }
    }

    @Override
    public boolean actualizar(ProveedorDTO p) {
        String sql = "UPDATE tt_proveedor SET ruc = ?, razon_social = ?, nombre_comercial = ?, rubro = ?, "
                   + "       contacto_nombre = ?, telefono = ?, correo_electronico = ?, direccion = ?, estado = ? "
                   + "WHERE id_proveedor = ?";
        try (PreparedStatement stmt = conexion().prepareStatement(sql)) {
            llenar(stmt, p);
            stmt.setInt(10, p.getIdProveedor());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RepositorioException("Error al actualizar el proveedor", e);
        }
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

    /** Los 9 primeros parámetros, iguales en INSERT y UPDATE. */
    private static void llenar(PreparedStatement stmt, ProveedorDTO p) throws SQLException {
        stmt.setString(1, p.getRuc());
        stmt.setString(2, p.getRazonSocial());
        RepositorioUtil.textoONulo(stmt, 3, p.getNombreComercial());
        stmt.setString(4, p.getRubro());
        RepositorioUtil.textoONulo(stmt, 5, p.getContactoNombre());
        RepositorioUtil.textoONulo(stmt, 6, p.getTelefono());
        RepositorioUtil.textoONulo(stmt, 7, p.getCorreo());
        RepositorioUtil.textoONulo(stmt, 8, p.getDireccion());
        stmt.setString(9, p.getEstado());
    }
}
