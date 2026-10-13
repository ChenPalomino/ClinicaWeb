package modelo.repositorio;

import modelo.dto.FiltroBusquedaDTO;
import modelo.dto.PaginaDTO;
import modelo.dto.ProductoServicioDTO;

/**
 * PATRÓN: Repository  ->  INTERFAZ del tarifario (servicios médicos y productos con stock).
 * Es la "mercadería/servicios" del módulo 6 del curso y la base de la cuenta de atención (Etapa 6).
 */
public interface ProductoServicioRepository extends CrudRepository<ProductoServicioDTO, Integer> {

    /** Búsqueda por código o nombre; filtros por tipo, categoría (grupo) y estado. */
    PaginaDTO<ProductoServicioDTO> listar(FiltroBusquedaDTO filtro);

    boolean reactivar(int idProductoServicio);

    boolean existeCodigo(String codigo, int idExcluir);

    /** Siguiente código libre: SRV-0007 o PRD-0004 según el tipo. */
    String siguienteCodigo(String tipo);

    /** Cantidad de productos activos en o por debajo de su stock mínimo (alerta del inicio). */
    int contarStockBajo();
}
