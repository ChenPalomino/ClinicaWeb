package modelo.repositorio;

import modelo.dto.FiltroBusquedaDTO;
import modelo.dto.PaginaDTO;
import modelo.dto.ProveedorDTO;

/** PATRÓN: Repository  ->  INTERFAZ de proveedores (laboratorios, droguerías, insumos). */
public interface ProveedorRepository extends CrudRepository<ProveedorDTO, Integer> {

    /** Búsqueda por RUC, razón social, nombre comercial o rubro; filtro por estado. */
    PaginaDTO<ProveedorDTO> listar(FiltroBusquedaDTO filtro);

    boolean reactivar(int idProveedor);

    boolean existeRuc(String ruc, int idProveedorExcluir);

    default java.util.List<String> rubros() {
        return modelo.servicios.ProveedorService.RUBROS;
    }
}
