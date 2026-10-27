package modelo.repositorio;

import modelo.dto.EspecialidadDTO;

import java.util.List;
import java.util.Optional;

public interface EspecialidadRepository {

    List<EspecialidadDTO> listar(String texto, String estado);

    Optional<EspecialidadDTO> buscarPorId(int id);

    boolean existeNombre(String nombre, int idExcluir);

    int insertar(EspecialidadDTO e);

    boolean actualizar(EspecialidadDTO e);

    boolean cambiarEstado(int id, String estado);
}
