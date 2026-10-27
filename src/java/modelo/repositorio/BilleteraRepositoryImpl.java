package modelo.repositorio;

import configuracion.ConexionBDSingleton;
import modelo.dto.BilleteraConfigDTO;
import modelo.dto.OpcionDTO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class BilleteraRepositoryImpl implements BilleteraRepository {

    private Connection conexion() throws SQLException {
        return ConexionBDSingleton.getInstancia().getConexion();
    }

    @Override
    public BilleteraConfigDTO obtenerConfiguracion() {
        String sql = "SELECT billetera_celular, billetera_titular, (qr_imagen IS NOT NULL) AS tiene_qr, "
                   + "       COALESCE(EXTRACT(EPOCH FROM fecha_actualizacion)::bigint, 0) AS version "
                   + "FROM tt_parametro_clinica WHERE id_parametro_clinica = 1";
        try (PreparedStatement stmt = conexion().prepareStatement(sql); ResultSet rs = stmt.executeQuery()) {
            BilleteraConfigDTO c = new BilleteraConfigDTO();
            if (rs.next()) {
                c.setCelular(rs.getString(1));
                c.setTitular(rs.getString(2));
                c.setTieneQr(rs.getBoolean(3));
                c.setVersionQr(rs.getLong(4));
            }
            return c;
        } catch (SQLException e) {
            throw new RepositorioException("Error al leer la configuración de billeteras", e);
        }
    }

    @Override
    public void guardarDatos(String celular, String titular, String usuario) {
        String sql = "UPDATE tt_parametro_clinica SET billetera_celular = ?, billetera_titular = ?, usuario_registro = ? "
                   + "WHERE id_parametro_clinica = 1";
        try (PreparedStatement stmt = conexion().prepareStatement(sql)) {
            RepositorioUtil.textoONulo(stmt, 1, celular);
            RepositorioUtil.textoONulo(stmt, 2, titular);
            stmt.setString(3, usuario);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RepositorioException("Error al guardar los datos de la billetera", e);
        }
    }

    @Override
    public void guardarQr(byte[] imagen, String tipoMime, String usuario) {
        String sql = "UPDATE tt_parametro_clinica SET qr_imagen = ?, qr_tipo_mime = ?, usuario_registro = ? "
                   + "WHERE id_parametro_clinica = 1";
        try (PreparedStatement stmt = conexion().prepareStatement(sql)) {
            stmt.setBytes(1, imagen);
            stmt.setString(2, tipoMime);
            stmt.setString(3, usuario);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RepositorioException("Error al guardar el QR", e);
        }
    }

    @Override
    public void quitarQr(String usuario) {
        String sql = "UPDATE tt_parametro_clinica SET qr_imagen = NULL, qr_tipo_mime = NULL, usuario_registro = ? "
                   + "WHERE id_parametro_clinica = 1";
        try (PreparedStatement stmt = conexion().prepareStatement(sql)) {
            stmt.setString(1, usuario);
            stmt.executeUpdate();
        } catch (SQLException e) {
            throw new RepositorioException("Error al quitar el QR", e);
        }
    }

    @Override
    public Optional<Object[]> obtenerQr() {
        String sql = "SELECT qr_imagen, qr_tipo_mime FROM tt_parametro_clinica WHERE id_parametro_clinica = 1 AND qr_imagen IS NOT NULL";
        try (PreparedStatement stmt = conexion().prepareStatement(sql); ResultSet rs = stmt.executeQuery()) {
            return rs.next() ? Optional.of(new Object[]{rs.getBytes(1), rs.getString(2)}) : Optional.empty();
        } catch (SQLException e) {
            throw new RepositorioException("Error al leer el QR", e);
        }
    }

    @Override
    public List<OpcionDTO> listarTodas() {
        List<OpcionDTO> lista = new ArrayList<>();
        try (PreparedStatement stmt = conexion().prepareStatement(
                "SELECT id_billetera, nombre, estado FROM tm_billetera ORDER BY orden, nombre");
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) {
                lista.add(new OpcionDTO(rs.getInt(1), rs.getString(2), rs.getString(3)));
            }
            return lista;
        } catch (SQLException e) {
            throw new RepositorioException("Error al leer las billeteras", e);
        }
    }

    @Override
    public boolean existeNombre(String nombre) {
        try {
            return RepositorioUtil.existe(conexion(), "SELECT 1 FROM tm_billetera WHERE LOWER(nombre) = LOWER(?)", nombre);
        } catch (SQLException e) {
            throw new RepositorioException("Error al verificar la billetera", e);
        }
    }

    @Override
    public int crear(String nombre, String usuario) {
        String sql = "INSERT INTO tm_billetera (nombre, orden, usuario_registro) "
                   + "VALUES (?, (SELECT COALESCE(MAX(orden), 0) + 1 FROM tm_billetera WHERE orden < 9), ?) RETURNING id_billetera";
        try (PreparedStatement stmt = conexion().prepareStatement(sql)) {
            stmt.setString(1, nombre);
            stmt.setString(2, usuario);
            try (ResultSet rs = stmt.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new RepositorioException("Error al registrar la billetera", e);
        }
    }

    @Override
    public boolean cambiarEstado(int idBilletera, String estado) {
        try {
            return RepositorioUtil.cambiarEstado(conexion(), "tm_billetera", "id_billetera", idBilletera, estado);
        } catch (SQLException e) {
            throw new RepositorioException("Error al cambiar el estado de la billetera", e);
        }
    }
}
