package fakes;

import modelo.dto.*;
import modelo.repositorio.*;

import java.time.*;
import java.util.*;

/** Agenda en memoria; "simularCarrera" imita la restricción única de la base de datos (RNF-08). */
public class CitasFalsas implements CitaRepository {
    public Map<Integer, CitaDTO> citas = new LinkedHashMap<>();
    public int sec = 0;
    public boolean simularCarrera = false;
    public Optional<CitaDTO> buscarPorId(int id) { return Optional.ofNullable(citas.get(id)); }
    public List<CitaDTO> agenda(FiltroCitaDTO f) { return new ArrayList<>(citas.values()); }
    public Map<LocalDate, ResumenCitasDTO> resumenPorDia(FiltroCitaDTO f) {
        Map<LocalDate, ResumenCitasDTO> m = new TreeMap<>();
        for (CitaDTO c : citas.values()) m.computeIfAbsent(c.getFecha(), k -> new ResumenCitasDTO()).sumar(c.getEstadoCita(), 1);
        return m;
    }
    public Map<Integer, Map<LocalDate, Set<LocalTime>>> ocupados(Collection<Integer> ids, LocalDate d, LocalDate h) {
        Map<Integer, Map<LocalDate, Set<LocalTime>>> m = new HashMap<>();
        for (CitaDTO c : citas.values())
            if (!c.isCancelada() && ids.contains(c.getIdMedico()) && !c.getFecha().isBefore(d) && !c.getFecha().isAfter(h))
                m.computeIfAbsent(c.getIdMedico(), k -> new HashMap<>()).computeIfAbsent(c.getFecha(), k -> new HashSet<>()).add(c.getHoraInicio());
        return m;
    }
    public List<CitaDTO> activasDelPaciente(int p, LocalDate d) {
        List<CitaDTO> r = new ArrayList<>();
        for (CitaDTO c : citas.values()) if (c.getIdPaciente() == p && c.isActiva() && !c.getFecha().isBefore(d)) r.add(c);
        return r;
    }
    public List<CitaDTO> delPaciente(int p, boolean prox, LocalDate hoy, int lim) { return activasDelPaciente(p, hoy); }
    public List<CitaDTO> activasDelMedico(int m, LocalDate d) { return new ArrayList<>(); }
    public Optional<Integer> idPacienteDeUsuario(int u) { return u == 40 ? Optional.of(7) : Optional.empty(); }
    public NuevaCita reservar(CitaDTO c, String u) {
        if (simularCarrera) throw new OperacionRechazadaException("Ese horario acaba de ser reservado por otra persona. Elige otro, por favor.");
        int id = ++sec; c.setIdCita(id); c.setCodigo(String.format("CT-%06d", id)); c.setEstadoCita(CitaDTO.RESERVADA);
        c.setNombrePaciente("Ana Torres"); c.setIdUsuarioPaciente(c.getIdPaciente() == 7 ? 40 : 0);
        c.setIdUsuarioMedico(c.getIdMedico() == 1 ? 30 : 31); c.setEspecialidad("Medicina General");
        citas.put(id, c); return new NuevaCita(id, c.getCodigo());
    }
    public boolean reprogramar(int id, int med, LocalDate f, LocalTime i, LocalTime fin, String u) {
        CitaDTO c = citas.get(id); if (c == null || !c.isReservada()) return false;
        c.setIdMedico(med); c.setIdUsuarioMedico(med == 1 ? 30 : 31); c.setFecha(f); c.setHoraInicio(i); c.setHoraFin(fin); c.setReprogramaciones(c.getReprogramaciones() + 1); return true;
    }
    public boolean cancelar(int id, String m, String u) { CitaDTO c = citas.get(id); if (c == null || !c.isActiva()) return false; c.setEstadoCita(CitaDTO.CANCELADA); c.setMotivoCancelacion(m); return true; }
    public boolean registrarLlegada(int id, String u) { CitaDTO c = citas.get(id); if (!c.isReservada()) return false; c.setEstadoCita(CitaDTO.CONFIRMADA); return true; }
    public boolean registrarAtencion(int id, String e, String n, String u) { CitaDTO c = citas.get(id); if (!c.isActiva()) return false; c.setEstadoCita(e); c.setNotaAtencion(n); return true; }
    public boolean vincularCuenta(int id, int cta) { citas.get(id).setIdCuentaAtencion(cta); return true; }
}
