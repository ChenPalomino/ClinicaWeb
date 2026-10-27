package fakes;

import modelo.dto.*;
import modelo.repositorio.*;

import java.time.*;
import java.util.*;

public class MedicosFalsos implements MedicoRepository {
    public List<MedicoDTO> lista = new ArrayList<>();
    public List<MedicoDTO> listarTodos() { return lista; }
    public List<MedicoDTO> listarQueAtienden(Integer esp) {
        List<MedicoDTO> r = new ArrayList<>();
        for (MedicoDTO m : lista) if (m.isAtiende() && (esp == null || m.getIdEspecialidad() == esp)) r.add(m);
        return r;
    }
    public Optional<MedicoDTO> buscarPorUsuario(int u) { return lista.stream().filter(m -> m.getIdUsuario() == u).findFirst(); }
    public Optional<MedicoDTO> buscarPorId(int id) { return lista.stream().filter(m -> m.getIdMedico() == id).findFirst(); }
    public int guardar(MedicoDTO m, String u) { return m.getIdMedico(); }
    public boolean cambiarEstado(int id, String e) { return true; }
    public List<OpcionDTO> especialidadesConMedicos() { return List.of(new OpcionDTO(1, "Medicina General", "2")); }
    public Optional<Integer> servicioDeConsulta(int esp) { return Optional.of(1); }
    public Map<Integer, java.math.BigDecimal> preciosConsulta() { return Map.of(1, new java.math.BigDecimal("80.00")); }
}
