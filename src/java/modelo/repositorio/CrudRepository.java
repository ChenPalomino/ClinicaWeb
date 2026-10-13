package modelo.repositorio;

import java.util.Optional;

/**
 * PATRÓN: Repository  ->  CONTRATO GENÉRICO (CRUD)
 *
 * Define las 4 operaciones básicas que tendrá CUALQUIER repositorio del
 * sistema (usuarios, pacientes, proveedores, productos...):
 *
 *     C - insertar      R - buscarPorId      U - actualizar      D - eliminar
 *
 * T  = tipo de objeto que se guarda (ej. UsuarioDTO)
 * ID = tipo de su identificador (ej. Integer)
 *
 * Cada módulo crea su interfaz extendiendo esta (UsuarioRepository extends
 * CrudRepository<UsuarioDTO, Integer>) y agrega sus consultas propias.
 * En la Etapa 5 se reutiliza para clientes, proveedores y productos.
 */
public interface CrudRepository<T, ID> {

    /** Busca un registro por su id. Optional vacío si no existe. */
    Optional<T> buscarPorId(ID id);

    /** Inserta un registro nuevo y devuelve el id generado. */
    ID insertar(T entidad);

    /** Actualiza un registro existente. true si se modificó. */
    boolean actualizar(T entidad);

    /** Eliminación LÓGICA (estado = 'Inactivo'): el registro no se borra, se conserva el historial. */
    boolean eliminar(ID id);
}
