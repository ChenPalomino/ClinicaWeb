package fakes;

import modelo.dto.EspecialidadDTO;
import modelo.repositorio.EspecialidadRepository;

import java.util.*;
import java.util.stream.Collectors;

public class EspecialidadesFalsas implements EspecialidadRepository {
    public Map<Integer, EspecialidadDTO> datos = new LinkedHashMap<>();
    int sec = 0;

    public List<EspecialidadDTO> listar(String texto, String estado) {
        return datos.values().stream().filter(e -> texto == null || e.getNombre().toLowerCase().contains(texto.toLowerCase())).collect(Collectors.toList());
    }
    public Optional<EspecialidadDTO> buscarPorId(int id) { return Optional.ofNullable(datos.get(id)); }
    public boolean existeNombre(String n, int excluir) {
        return datos.values().stream().anyMatch(e -> e.getNombre().equalsIgnoreCase(n) && e.getIdEspecialidad() != excluir);
    }
    public int insertar(EspecialidadDTO e) { e.setIdEspecialidad(++sec); datos.put(sec, e); return sec; }
    public boolean actualizar(EspecialidadDTO e) {
        EspecialidadDTO actual = datos.get(e.getIdEspecialidad());
        actual.setNombre(e.getNombre());
        actual.setDescripcion(e.getDescripcion());
        actual.setEstado(e.getEstado());
        return true;
    }
    public boolean cambiarEstado(int id, String estado) { datos.get(id).setEstado(estado); return true; }
}
