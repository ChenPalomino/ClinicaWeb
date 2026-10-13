package modelo.repositorio;

import configuracion.ConexionBDSingleton;
import modelo.entidades.Rol;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** PATRÓN: Repository  ->  IMPLEMENTACIÓN (SQL) de RolRepository. */
public class RolRepositoryImpl implements RolRepository {

    @Override
    public List<Rol> listarActivos() {
        String sql = "SELECT id_rol, nombre_rol, descripcion FROM tm_rol WHERE estado = 'Activo' ORDER BY id_rol";
        List<Rol> roles = new ArrayList<>();
        try (PreparedStatement stmt = ConexionBDSingleton.getInstancia().getConexion().prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                roles.add(new Rol(rs.getInt("id_rol"), rs.getString("nombre_rol"), rs.getString("descripcion")));
            }
            return roles;
        } catch (SQLException e) {
            throw new RepositorioException("Error al listar roles", e);
        }
    }
}
