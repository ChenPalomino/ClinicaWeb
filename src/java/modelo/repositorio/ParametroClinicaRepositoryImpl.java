package modelo.repositorio;

import configuracion.ConexionBDSingleton;
import modelo.dto.ParametroClinicaDTO;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/** PATRÓN: Repository  ->  IMPLEMENTACIÓN (SQL) de ParametroClinicaRepository. */
public class ParametroClinicaRepositoryImpl implements ParametroClinicaRepository {

    @Override
    public ParametroClinicaDTO obtener() {
        String sql = "SELECT ruc, razon_social, nombre_comercial, direccion, telefono, igv_porcentaje, monto_boleta_dni, "
                   + "       horas_cancelacion_paciente, dias_maximos_reserva, fecha_actualizacion, usuario_registro FROM tt_parametro_clinica WHERE id_parametro_clinica = 1";
        try (PreparedStatement stmt = ConexionBDSingleton.getInstancia().getConexion().prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            ParametroClinicaDTO p = new ParametroClinicaDTO();
            if (rs.next()) {
                p.setRuc(rs.getString("ruc"));
                p.setRazonSocial(rs.getString("razon_social"));
                p.setNombreComercial(rs.getString("nombre_comercial"));
                p.setDireccion(rs.getString("direccion"));
                p.setTelefono(rs.getString("telefono"));
                p.setIgvPorcentaje(rs.getBigDecimal("igv_porcentaje"));
                p.setMontoBoletaDni(rs.getBigDecimal("monto_boleta_dni"));
                p.setHorasCancelacionPaciente(rs.getInt("horas_cancelacion_paciente"));
                p.setDiasMaximosReserva(rs.getInt("dias_maximos_reserva"));
                p.setFechaActualizacion(rs.getTimestamp("fecha_actualizacion"));
                p.setUsuarioRegistro(rs.getString("usuario_registro"));
            }
            return p;
        } catch (SQLException e) {
            throw new RepositorioException("Error al leer los parámetros de la clínica", e);
        }
    }

    @Override
    public boolean actualizar(ParametroClinicaDTO p) {
        String sql = "UPDATE tt_parametro_clinica SET ruc = ?, razon_social = ?, nombre_comercial = ?, direccion = ?, "
                   + "       telefono = ?, igv_porcentaje = ?, monto_boleta_dni = ?, "
                   + "       horas_cancelacion_paciente = ?, dias_maximos_reserva = ?, usuario_registro = ? "
                   + "WHERE id_parametro_clinica = 1";
        try (PreparedStatement stmt = ConexionBDSingleton.getInstancia().getConexion().prepareStatement(sql)) {
            stmt.setString(1, p.getRuc());
            stmt.setString(2, p.getRazonSocial());
            stmt.setString(3, p.getNombreComercial());
            stmt.setString(4, p.getDireccion());
            RepositorioUtil.textoONulo(stmt, 5, p.getTelefono());
            stmt.setBigDecimal(6, p.getIgvPorcentaje());
            stmt.setBigDecimal(7, p.getMontoBoletaDni());
            stmt.setInt(8, p.getHorasCancelacionPaciente());
            stmt.setInt(9, p.getDiasMaximosReserva());
            stmt.setString(10, p.getUsuarioRegistro());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RepositorioException("Error al guardar los parámetros de la clínica", e);
        }
    }
}
