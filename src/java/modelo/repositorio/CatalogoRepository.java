package modelo.repositorio;

import modelo.dto.OpcionDTO;

import java.util.List;

/**
 * PATRÓN: Repository  ->  INTERFAZ de las tablas MAESTRAS (tm_) de solo lectura
 * que llenan las listas desplegables de los formularios.
 */
public interface CatalogoRepository {

    /** extra = tipo (PARTICULAR, EPS, SEGURO, CONVENIO). */
    List<OpcionDTO> listarCoberturas();

    List<OpcionDTO> listarEspecialidades();

    /** extra = tipo de la categoría (SERVICIO o PRODUCTO). */
    List<OpcionDTO> listarCategorias();

    List<OpcionDTO> listarMarcas();

    /** nombre = "Unidad (UND)"; extra = a qué tipo aplica (SERVICIO o PRODUCTO). */
    List<OpcionDTO> listarUnidadesMedida();

    List<OpcionDTO> listarSexos();

    /** Proveedores ACTIVOS para elegir el proveedor habitual de un producto. extra = RUC. */
    List<OpcionDTO> listarProveedoresActivos();
}
