package modelo.repositorio;

import modelo.dto.FiltroBusquedaDTO;
import modelo.dto.PaginaDTO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Operaciones SQL compartidas por varios repositorios (solo dentro del
 * paquete modelo.repositorio). Evita repetir el mismo código en cada Impl.
 */
final class RepositorioUtil {

    /** Convierte la fila actual de un ResultSet en un objeto (lo usa cada Impl con su propio "mapear"). */
    @FunctionalInterface
    interface MapeadorFila<T> {
        T mapear(ResultSet rs) throws SQLException;
    }

    private RepositorioUtil() {
    }

    /**
     * Listado paginado común: cuenta el total y trae solo la página pedida.
     * select = "SELECT ... FROM ... JOIN ..." (sin WHERE)
     * where  = "WHERE ..." armado por cada Impl con parámetros "?"
     */
    static <T> PaginaDTO<T> paginar(Connection cn, String select, String where, List<Object> parametros,
                                    String orden, FiltroBusquedaDTO filtro, MapeadorFila<T> mapeador) throws SQLException {
        int total;
        try (PreparedStatement stmt = cn.prepareStatement("SELECT COUNT(*) FROM (" + select + where + ") x")) {
            asignar(stmt, parametros);
            try (ResultSet rs = stmt.executeQuery()) {
                rs.next();
                total = rs.getInt(1);
            }
        }
        List<T> elementos = new ArrayList<>();
        try (PreparedStatement stmt = cn.prepareStatement(select + where + " ORDER BY " + orden + " LIMIT ? OFFSET ?")) {
            int i = asignar(stmt, parametros);
            stmt.setInt(i++, filtro.getTamanio());
            stmt.setInt(i, filtro.getOffset());
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    elementos.add(mapeador.mapear(rs));
                }
            }
        }
        return new PaginaDTO<>(elementos, total, filtro.getPagina(), filtro.getTamanio());
    }

    /** Una sola fila (o vacío) a partir de una consulta con un id. */
    static <T> Optional<T> unaFila(Connection cn, String sql, int id, MapeadorFila<T> mapeador) throws SQLException {
        try (PreparedStatement stmt = cn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? Optional.of(mapeador.mapear(rs)) : Optional.empty();
            }
        }
    }

    /** Eliminación lógica / reactivación. Los nombres de tabla y columna son constantes del código. */
    static boolean cambiarEstado(Connection cn, String tabla, String columnaId, int id, String estado) throws SQLException {
        try (PreparedStatement stmt = cn.prepareStatement(
                "UPDATE " + tabla + " SET estado = ? WHERE " + columnaId + " = ?")) {
            stmt.setString(1, estado);
            stmt.setInt(2, id);
            return stmt.executeUpdate() > 0;
        }
    }

    /** true si la consulta devuelve al menos una fila. */
    static boolean existe(Connection cn, String sql, Object... parametros) throws SQLException {
        try (PreparedStatement stmt = cn.prepareStatement(sql)) {
            asignar(stmt, List.of(parametros));
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    /** Coloca los parámetros en orden; devuelve el índice del siguiente "?". */
    static int asignar(PreparedStatement stmt, List<Object> parametros) throws SQLException {
        int i = 1;
        for (Object p : parametros) {
            stmt.setObject(i++, p);
        }
        return i;
    }

    /** Agrega al WHERE la condición de estado del filtro (ACTIVO/INACTIVO). */
    static void filtrarEstado(StringBuilder where, String alias, FiltroBusquedaDTO filtro) {
        if ("ACTIVO".equals(filtro.getEstado())) {
            where.append(" AND ").append(alias).append(".estado = 'Activo'");
        } else if ("INACTIVO".equals(filtro.getEstado())) {
            where.append(" AND ").append(alias).append(".estado = 'Inactivo'");
        }
    }

    /** Integer que puede venir NULL de la BD. */
    static Integer enteroONulo(ResultSet rs, String columna) throws SQLException {
        int v = rs.getInt(columna);
        return rs.wasNull() ? null : v;
    }

    /** Coloca un Integer que puede ser null. */
    static void enteroONulo(PreparedStatement stmt, int indice, Integer valor) throws SQLException {
        if (valor == null) {
            stmt.setNull(indice, java.sql.Types.INTEGER);
        } else {
            stmt.setInt(indice, valor);
        }
    }

    /** Texto vacío -> NULL en la BD. */
    static void textoONulo(PreparedStatement stmt, int indice, String valor) throws SQLException {
        if (valor == null || valor.isBlank()) {
            stmt.setNull(indice, java.sql.Types.VARCHAR);
        } else {
            stmt.setString(indice, valor);
        }
    }

    /**
     * Actualiza el primer correo/celular activo de la persona o lo crea si no tiene.
     * Los nombres de tabla/columna son constantes internas (nunca vienen del usuario).
     */
    static void guardarContacto(Connection cn, String tabla, String id, String columna, String columnaTipo,
                                String tipo, int idDatos, String valor, String registro) throws SQLException {
        if (valor == null || valor.isBlank()) {
            return;
        }
        int filas;
        try (PreparedStatement stmt = cn.prepareStatement(
                "UPDATE " + tabla + " SET " + columna + " = ? WHERE " + id + " = ("
              + "  SELECT " + id + " FROM " + tabla + " WHERE id_datos_personales = ? AND estado = 'Activo' "
              + "  ORDER BY " + id + " LIMIT 1)")) {
            stmt.setString(1, valor);
            stmt.setInt(2, idDatos);
            filas = stmt.executeUpdate();
        }
        if (filas == 0) {
            try (PreparedStatement stmt = cn.prepareStatement(
                    "INSERT INTO " + tabla + " (id_datos_personales, " + columna + ", " + columnaTipo
                  + ", usuario_registro) VALUES (?, ?, ?, ?)")) {
                stmt.setInt(1, idDatos);
                stmt.setString(2, valor);
                stmt.setString(3, tipo);
                stmt.setString(4, registro);
                stmt.executeUpdate();
            }
        }
    }
}
