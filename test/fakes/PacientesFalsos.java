package fakes;

import modelo.dto.*;
import modelo.repositorio.*;

import java.time.*;
import java.util.*;

public class PacientesFalsos implements PacienteRepository {
    public Optional<PacienteDTO> buscarPorId(Integer id) {
        if (id != 7 && id != 8) return Optional.empty();
        PacienteDTO p = new PacienteDTO(); p.setIdPaciente(id); p.setNombres("Ana"); p.setApellidos("Torres");
        if (id == 8) p.setEstado("Inactivo");
        return Optional.of(p);
    }
    public Integer insertar(PacienteDTO e) { return 0; }
    public boolean actualizar(PacienteDTO e) { return false; }
    public boolean eliminar(Integer id) { return false; }
    public PaginaDTO<PacienteDTO> listar(FiltroBusquedaDTO f) { return null; }
    public boolean reactivar(int id) { return false; }
    public Optional<String> historiaConDni(String d, int x) { return Optional.empty(); }
    public boolean existeCorreoDeOtraPersona(String c, String d) { return false; }
}
