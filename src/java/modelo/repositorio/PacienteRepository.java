package modelo.repositorio;

import modelo.dto.FiltroBusquedaDTO;
import modelo.dto.PacienteDTO;
import modelo.dto.PaginaDTO;

import java.util.Optional;

/**
 * PATRÓN: Repository  ->  INTERFAZ de pacientes (los "clientes" de la clínica).
 * RF-01 registrar pacientes con DNI, contacto y cobertura; RF-02 buscarlos por DNI o apellidos.
 * Hereda el CRUD genérico y agrega búsqueda, reactivación y validaciones de duplicados.
 */
public interface PacienteRepository extends CrudRepository<PacienteDTO, Integer> {

    /** Búsqueda por DNI, nombres, apellidos o N.º de historia; filtro por cobertura (grupo) y estado. */
    PaginaDTO<PacienteDTO> listar(FiltroBusquedaDTO filtro);

    boolean reactivar(int idPaciente);

    /** N.º de historia del OTRO paciente que ya usa ese DNI, o vacío si está libre. */
    Optional<String> historiaConDni(String dni, int idPacienteExcluir);

    /** true si el correo pertenece a otra persona (otro DNI): paciente, personal o usuario. */
    boolean existeCorreoDeOtraPersona(String correo, String dniPropio);
}
