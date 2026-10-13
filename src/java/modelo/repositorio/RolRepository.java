package modelo.repositorio;

import modelo.entidades.Rol;

import java.util.List;

/**
 * PATRÓN: Repository  ->  INTERFAZ de la tabla maestra tm_rol.
 * Se usa para llenar la lista desplegable de roles en el formulario.
 */
public interface RolRepository {

    List<Rol> listarActivos();
}
