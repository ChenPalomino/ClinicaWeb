package modelo.repositorio;

import configuracion.ConexionBDSingleton;
import modelo.dto.ParValorDTO;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class IndicadorRepositoryImpl implements IndicadorRepository {

    private static final String PAGO_VALIDO = "p.estado_pago = 'APROBADO' AND p.fecha_creacion::date BETWEEN ? AND ? ";
    private static final String CITA_EN_RANGO = "c.estado = 'Activo' AND c.fecha BETWEEN ? AND ? ";

    private Connection conexion() throws SQLException {
        return ConexionBDSingleton.getInstancia().getConexion();
    }

    private List<ParValorDTO> consultar(String sql, Object... parametros) {
        List<ParValorDTO> lista = new ArrayList<>();
        try (PreparedStatement stmt = conexion().prepareStatement(sql)) {
            for (int i = 0; i < parametros.length; i++) {
                Object p = parametros[i];
                if (p instanceof LocalDate f) {
                    stmt.setDate(i + 1, Date.valueOf(f));
                } else {
                    stmt.setObject(i + 1, p);
                }
            }
            try (ResultSet rs = stmt.executeQuery()) {
                boolean dos = rs.getMetaData().getColumnCount() > 2;
                while (rs.next()) {
                    lista.add(new ParValorDTO(rs.getString(1), rs.getBigDecimal(2), dos ? rs.getBigDecimal(3) : null));
                }
            }
            return lista;
        } catch (SQLException e) {
            throw new RepositorioException("Error al calcular el indicador", e);
        }
    }

    @Override
    public List<ParValorDTO> ingresosPorDia(LocalDate desde, LocalDate hasta) {
        return consultar("SELECT p.fecha_creacion::date::text, SUM(p.monto), COUNT(*) FROM tt_pago p WHERE " + PAGO_VALIDO
                + "GROUP BY 1 ORDER BY 1", desde, hasta);
    }

    @Override
    public List<ParValorDTO> ingresosPorMetodo(LocalDate desde, LocalDate hasta) {
        return consultar("SELECT mp.nombre, SUM(p.monto), COUNT(*) FROM tt_pago p "
                + "JOIN tm_metodo_pago mp ON mp.id_metodo_pago = p.id_metodo_pago WHERE " + PAGO_VALIDO
                + "GROUP BY mp.nombre ORDER BY 2 DESC", desde, hasta);
    }

    @Override
    public List<ParValorDTO> ingresosPorCobertura(LocalDate desde, LocalDate hasta) {
        return consultar("SELECT cb.nombre, SUM(p.monto), SUM(ca.monto_aseguradora + ca.monto_descuento) FROM tt_pago p "
                + "JOIN tt_cuenta_atencion ca ON ca.id_cuenta_atencion = p.id_cuenta_atencion "
                + "JOIN tm_cobertura cb ON cb.id_cobertura = ca.id_cobertura WHERE " + PAGO_VALIDO
                + "GROUP BY cb.nombre ORDER BY 2 DESC", desde, hasta);
    }

    @Override
    public List<ParValorDTO> masVendidos(LocalDate desde, LocalDate hasta, int limite) {
        return consultar("SELECT d.descripcion, SUM(d.importe), SUM(d.cantidad) FROM vw_detalle_cuenta d "
                + "JOIN tt_cuenta_atencion ca ON ca.id_cuenta_atencion = d.id_cuenta_atencion "
                + "WHERE ca.estado_cuenta = 'PAGADA' AND ca.fecha_pago::date BETWEEN ? AND ? "
                + "GROUP BY d.descripcion ORDER BY 2 DESC, 3 DESC LIMIT ?", desde, hasta, limite);
    }

    @Override
    public List<ParValorDTO> citasPorEstado(LocalDate desde, LocalDate hasta) {
        return consultar("SELECT c.estado_cita, COUNT(*) FROM tt_cita c WHERE " + CITA_EN_RANGO
                + "GROUP BY c.estado_cita ORDER BY 2 DESC", desde, hasta);
    }

    @Override
    public List<ParValorDTO> citasPorEspecialidad(LocalDate desde, LocalDate hasta) {
        return consultar("SELECT e.nombre, COUNT(*) FILTER (WHERE c.estado_cita <> 'CANCELADA'), "
                + "       COUNT(*) FILTER (WHERE c.estado_cita = 'ATENDIDA') FROM tt_cita c "
                + "JOIN tm_especialidad e ON e.id_especialidad = c.id_especialidad WHERE " + CITA_EN_RANGO
                + "GROUP BY e.nombre ORDER BY 2 DESC, 1", desde, hasta);
    }

    @Override
    public List<ParValorDTO> atencionesPorMedico(LocalDate desde, LocalDate hasta, int limite) {
        return consultar("SELECT d.apellidos || ', ' || d.nombres, COUNT(*) FILTER (WHERE c.estado_cita = 'ATENDIDA'), "
                + "       COUNT(*) FILTER (WHERE c.estado_cita = 'NO_ASISTIO') FROM tt_cita c "
                + "JOIN tt_medico m ON m.id_medico = c.id_medico "
                + "JOIN tt_datos_personales d ON d.id_datos_personales = m.id_datos_personales WHERE " + CITA_EN_RANGO
                + "GROUP BY 1 ORDER BY 2 DESC, 1 LIMIT ?", desde, hasta, limite);
    }

    @Override
    public List<ParValorDTO> citasPorHora(LocalDate desde, LocalDate hasta) {
        return consultar("SELECT LPAD(EXTRACT(HOUR FROM c.hora_inicio)::int::text, 2, '0'), COUNT(*) FROM tt_cita c WHERE "
                + CITA_EN_RANGO + "AND c.estado_cita <> 'CANCELADA' GROUP BY 1 ORDER BY 1", desde, hasta);
    }

    @Override
    public List<ParValorDTO> citasPorCanal(LocalDate desde, LocalDate hasta) {
        return consultar("SELECT c.canal, COUNT(*) FROM tt_cita c WHERE " + CITA_EN_RANGO
                + "GROUP BY c.canal ORDER BY 2 DESC", desde, hasta);
    }

    @Override
    public List<ParValorDTO> usuariosNuevosPorDia(LocalDate desde, LocalDate hasta) {
        return consultar("SELECT u.fecha_creacion::date::text, COUNT(*) FROM tt_usuario u "
                + "WHERE u.fecha_creacion::date BETWEEN ? AND ? GROUP BY 1 ORDER BY 1", desde, hasta);
    }

    @Override
    public List<ParValorDTO> usuariosActivosPorRol() {
        return consultar("SELECT r.nombre_rol, COUNT(*) FILTER (WHERE u.estado = 'Activo' AND NOT u.bloqueado), "
                + "       COUNT(*) FILTER (WHERE u.bloqueado) FROM tt_usuario u "
                + "JOIN tm_rol r ON r.id_rol = u.id_rol GROUP BY r.id_rol, r.nombre_rol ORDER BY r.id_rol");
    }

    @Override
    public List<ParValorDTO> stockBajo(int limite) {
        return consultar("SELECT ps.nombre, ps.stock, ps.stock_minimo FROM vw_producto_servicio ps "
                + "WHERE ps.tipo = 'PRODUCTO' AND ps.estado = 'Activo' AND ps.stock <= ps.stock_minimo "
                + "ORDER BY ps.stock - ps.stock_minimo, ps.nombre LIMIT ?", limite);
    }
}
