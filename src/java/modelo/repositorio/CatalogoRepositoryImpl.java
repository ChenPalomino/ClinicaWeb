package modelo.repositorio;

import configuracion.ConexionBDSingleton;
import modelo.dto.OpcionDTO;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/** PATRÓN: Repository  ->  IMPLEMENTACIÓN (SQL) de CatalogoRepository. */
public class CatalogoRepositoryImpl implements CatalogoRepository {

    @Override
    public List<OpcionDTO> listarCoberturas() {
        return listar("SELECT id_cobertura, nombre, tipo FROM tm_cobertura WHERE estado = 'Activo' ORDER BY id_cobertura");
    }

    @Override
    public List<OpcionDTO> listarEspecialidades() {
        return listar("SELECT id_especialidad, nombre, NULL FROM tm_especialidad WHERE estado = 'Activo' ORDER BY nombre");
    }

    @Override
    public List<OpcionDTO> listarCategorias() {
        return listar("SELECT id_categoria, nombre, tipo FROM tm_categoria WHERE estado = 'Activo' ORDER BY tipo DESC, nombre");
    }

    @Override
    public List<OpcionDTO> listarMarcas() {
        return listar("SELECT id_marca, nombre, NULL FROM tm_marca WHERE estado = 'Activo' ORDER BY nombre");
    }

    @Override
    public List<OpcionDTO> listarUnidadesMedida() {
        return listar("SELECT id_unidad_medida, nombre || ' (' || abreviatura || ')', aplica_a FROM tm_unidad_medida "
                    + "WHERE estado = 'Activo' ORDER BY aplica_a DESC, id_unidad_medida");
    }

    @Override
    public List<OpcionDTO> listarSexos() {
        return listar("SELECT id_sexo, descripcion, NULL FROM tm_sexo WHERE estado = 'Activo' ORDER BY id_sexo");
    }

    @Override
    public List<OpcionDTO> listarProveedoresActivos() {
        return listar("SELECT id_proveedor, COALESCE(nombre_comercial, razon_social), ruc FROM tt_proveedor "
                    + "WHERE estado = 'Activo' ORDER BY 2");
    }

    /** Todas las consultas devuelven 3 columnas: id, nombre y un dato extra. */
    private List<OpcionDTO> listar(String sql) {
        List<OpcionDTO> lista = new ArrayList<>();
        try (PreparedStatement stmt = ConexionBDSingleton.getInstancia().getConexion().prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                lista.add(new OpcionDTO(rs.getInt(1), rs.getString(2), rs.getString(3)));
            }
            return lista;
        } catch (SQLException e) {
            throw new RepositorioException("Error al leer el catálogo", e);
        }
    }
}
