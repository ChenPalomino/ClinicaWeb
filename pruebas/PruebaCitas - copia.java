import evento.*;
import modelo.citas.*;
import modelo.citas.vista.*;
import modelo.dto.*;
import modelo.entidades.Usuario;
import modelo.repositorio.*;
import modelo.servicios.CitaService;
import notificacion.*;

import java.time.*;
import java.util.*;

/** Pruebas SIN base de datos del módulo de citas (RNF-03): turnos, reglas, vistas, servicio y notificador. */
public class PruebaCitas {
    static int fallas = 0;
    static void check(String n, boolean ok) { System.out.println((ok ? "OK   " : "FALLA ") + n); if (!ok) fallas++; }

    // ---------- repositorios falsos ----------
    static class MedicosFalsos implements MedicoRepository {
        List<MedicoDTO> lista = new ArrayList<>();
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
    }

    static class CitasFalsas implements CitaRepository {
        Map<Integer, CitaDTO> citas = new LinkedHashMap<>();
        int sec = 0;
        boolean simularCarrera = false;
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

    static class PacientesFalsos implements PacienteRepository {
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

    static MedicoDTO medico(int id, int idUsuario, int esp, int duracion) {
        MedicoDTO m = new MedicoDTO();
        m.setIdMedico(id); m.setIdUsuario(idUsuario); m.setIdEspecialidad(esp); m.setEspecialidad("Medicina General");
        m.setNombres("Marco"); m.setApellidos("Demo " + id); m.setDuracionCita(duracion); m.setCmp("01234" + id); m.setConsultorio("101");
        for (int d = 1; d <= 5; d++) {
            m.getHorarios().add(new HorarioDTO(d, LocalTime.of(8, 0), LocalTime.of(13, 0)));
            m.getHorarios().add(new HorarioDTO(d, LocalTime.of(15, 0), LocalTime.of(19, 0)));
        }
        m.getHorarios().add(new HorarioDTO(6, LocalTime.of(8, 0), LocalTime.of(12, 0)));
        return m;
    }

    public static void main(String[] a) {
        // Martes 20/10/2026 10:15 (hora "actual" fija para que la prueba no dependa del reloj)
        LocalDateTime[] ahora = {LocalDateTime.of(2026, 10, 20, 10, 15)};
        LocalDate hoy = ahora[0].toLocalDate();
        LocalDate miercoles = hoy.plusDays(1), domingo = LocalDate.of(2026, 10, 25), sabado = LocalDate.of(2026, 10, 24);

        // ===== CalculadoraTurnos =====
        MedicoDTO m1 = medico(1, 30, 1, 30);
        List<TurnoDTO> t = CalculadoraTurnos.turnos(m1, miercoles, Set.of(), ahora[0]);
        check("miércoles: 10 turnos mañana + 8 tarde = 18", t.size() == 18);
        check("primer turno 08:00-08:30", t.get(0).getHoraInicio().equals(LocalTime.of(8, 0)) && t.get(0).getHoraFin().equals(LocalTime.of(8, 30)));
        check("último turno 18:30", t.get(17).getHoraInicio().equals(LocalTime.of(18, 30)));
        check("no hay turno 13:00 ni 14:30", t.stream().noneMatch(x -> x.getHoraInicio().equals(LocalTime.of(13, 0)) || x.getHoraInicio().equals(LocalTime.of(14, 30))));
        List<TurnoDTO> hoyT = CalculadoraTurnos.turnos(m1, hoy, Set.of(), ahora[0]);
        check("hoy a las 10:15 ya no ofrece 08:00..10:00 (pasados)", hoyT.get(0).getHoraInicio().equals(LocalTime.of(10, 30)));
        check("domingo: no atiende", CalculadoraTurnos.turnos(m1, domingo, Set.of(), ahora[0]).isEmpty());
        check("sábado: 8 turnos (08:00-12:00)", CalculadoraTurnos.turnos(m1, sabado, Set.of(), ahora[0]).size() == 8);
        List<TurnoDTO> conOcupado = CalculadoraTurnos.turnos(m1, miercoles, Set.of(LocalTime.of(9, 0)), ahora[0]);
        check("09:00 ocupado se marca no libre", conOcupado.stream().filter(x -> x.getHoraInicio().equals(LocalTime.of(9, 0))).allMatch(x -> !x.isLibre()));
        check("libres() descuenta el ocupado: 17", CalculadoraTurnos.libres(m1, miercoles, Set.of(LocalTime.of(9, 0)), ahora[0]).size() == 17);
        MedicoDTO m45 = medico(9, 99, 1, 45);
        check("duración 45: bloque 08-13 da 6 turnos (último 12:15? no: 11:45-12:30)",
              CalculadoraTurnos.turnos(m45, miercoles, Set.of(), ahora[0]).stream().filter(TurnoDTO::isManiana).count() == 6);
        check("turnoDelHorario 08:15 no existe (no calza)", CalculadoraTurnos.turnoDelHorario(m1, miercoles, LocalTime.of(8, 15), ahora[0]).isEmpty());
        check("dentroDelHorario 12:30-13:00 sí, 13:00-13:30 no",
              CalculadoraTurnos.dentroDelHorario(m1, miercoles, LocalTime.of(12, 30), LocalTime.of(13, 0))
              && !CalculadoraTurnos.dentroDelHorario(m1, miercoles, LocalTime.of(13, 0), LocalTime.of(13, 30)));
        MedicoDTO noche = medico(10, 98, 1, 60); noche.getHorarios().clear();
        noche.getHorarios().add(new HorarioDTO(3, LocalTime.of(21, 0), LocalTime.of(23, 59)));
        check("bloque nocturno no da la vuelta a la medianoche", CalculadoraTurnos.turnos(noche, miercoles, Set.of(), ahora[0]).size() == 2);

        // ===== Vistas (Strategy) =====
        VistaAgendaStrategy dia = VistaAgendaFactory.obtener("dia"), sem = VistaAgendaFactory.obtener("semana"), mes = VistaAgendaFactory.obtener("mes");
        check("vista desconocida -> día", VistaAgendaFactory.obtener("xyz") instanceof VistaDiaStrategy && VistaAgendaFactory.obtener(null) instanceof VistaDiaStrategy);
        check("semana del martes 20/10: lunes 19 a domingo 25", sem.desde(hoy).equals(LocalDate.of(2026, 10, 19)) && sem.hasta(hoy).equals(domingo));
        check("mes: 01/10 a 31/10", mes.desde(hoy).equals(LocalDate.of(2026, 10, 1)) && mes.hasta(hoy).equals(LocalDate.of(2026, 10, 31)));
        check("navegación: día +1, semana +7, mes siguiente = 01/11",
              dia.siguiente(hoy).equals(miercoles) && sem.siguiente(hoy).equals(hoy.plusDays(7)) && mes.siguiente(hoy).equals(LocalDate.of(2026, 11, 1)));
        check("título día: Martes 20 de octubre de 2026", dia.titulo(hoy).equals("Martes 20 de octubre de 2026"));
        check("título semana: Semana del 19 al 25 de octubre de 2026", sem.titulo(hoy).equals("Semana del 19 al 25 de octubre de 2026"));
        check("título semana entre meses", sem.titulo(LocalDate.of(2026, 10, 28)).equals("Semana del 26 de octubre al 1 de noviembre de 2026"));
        check("título mes: Octubre de 2026", mes.titulo(hoy).equals("Octubre de 2026"));

        // ===== FechaTexto =====
        check("relativa: Hoy / Mañana / Ayer", FechaTexto.relativa(hoy, hoy).equals("Hoy") && FechaTexto.relativa(miercoles, hoy).equals("Mañana")
              && FechaTexto.relativa(hoy.minusDays(1), hoy).equals("Ayer"));
        check("relativa otro día: Jueves 22 de octubre", FechaTexto.relativa(hoy.plusDays(2), hoy).equals("Jueves 22 de octubre"));
        check("hora 12h: 9:30 a. m. / 3:00 p. m. / 12:00 p. m.", FechaTexto.hora(LocalTime.of(9, 30)).equals("9:30 a. m.")
              && FechaTexto.hora(LocalTime.of(15, 0)).equals("3:00 p. m.") && FechaTexto.hora(LocalTime.of(12, 0)).equals("12:00 p. m."));

        // ===== ReglasCita =====
        CitaDTO c = new CitaDTO(); c.setFecha(miercoles); c.setHoraInicio(LocalTime.of(9, 0)); c.setHoraFin(LocalTime.of(9, 30));
        check("faltan 22h45: el paciente YA NO puede cambiar (regla 24 h)", !ReglasCita.pacientePuedeCambiar(c, ahora[0], 24));
        check("con regla de 2 h sí podría", ReglasCita.pacientePuedeCambiar(c, ahora[0], 2));
        check("mensaje explica que llame a recepción", ReglasCita.motivoBloqueoPaciente(c, ahora[0], 24).contains("recepción"));
        c.setFecha(hoy.plusDays(3));
        check("faltan 3 días: sí puede", ReglasCita.pacientePuedeCambiar(c, ahora[0], 24) && ReglasCita.motivoBloqueoPaciente(c, ahora[0], 24) == null);
        check("exactamente 24 h antes: aún puede", ReglasCita.pacientePuedeCambiar(c, c.getInicio().minusHours(24), 24));
        c.setReprogramaciones(2);
        check("ya reprogramó 2 veces: no puede reprogramar, sí cancelar",
              !ReglasCita.pacientePuedeReprogramar(c, ahora[0], 24) && ReglasCita.pacientePuedeCambiar(c, ahora[0], 24));
        c.setEstadoCita(CitaDTO.CONFIRMADA);
        check("con llegada registrada el paciente no cambia nada", !ReglasCita.pacientePuedeCambiar(c, ahora[0], 24));
        CitaDTO h = new CitaDTO(); h.setFecha(hoy); h.setHoraInicio(LocalTime.of(9, 0)); h.setHoraFin(LocalTime.of(9, 30));
        check("llegada solo el mismo día", ReglasCita.puedeRegistrarLlegada(h, hoy) && !ReglasCita.puedeRegistrarLlegada(c, hoy));
        check("no asistió: solo cuando pasó la hora", ReglasCita.puedeMarcarNoAsistio(h, ahora[0])
              && !ReglasCita.puedeMarcarNoAsistio(h, LocalDateTime.of(hoy, LocalTime.of(8, 0))));
        check("atendida: no se marca una cita futura", ReglasCita.puedeMarcarAtendida(h, hoy) && !ReglasCita.puedeMarcarAtendida(c, hoy));
        h.setEstadoCita(CitaDTO.CANCELADA);
        check("cancelada: no se cobra ni se cancela otra vez", !ReglasCita.puedeCobrar(h) && !ReglasCita.personalPuedeCancelar(h, hoy));

        // ===== CitaService (repositorios falsos) =====
        MedicosFalsos medicos = new MedicosFalsos();
        medicos.lista.add(m1);
        MedicoDTO m2 = medico(2, 31, 1, 20); medicos.lista.add(m2);
        MedicoDTO pausado = medico(3, 32, 1, 30); pausado.setEstado("Inactivo"); medicos.lista.add(pausado);
        CitasFalsas citas = new CitasFalsas();
        ParametroClinicaDTO param = new ParametroClinicaDTO();   // 24 h / 60 días
        CitaService s = new CitaService(citas, medicos, new PacientesFalsos(), () -> param, () -> ahora[0], () -> null);
        Usuario recep = new Usuario.UsuarioBuilder("recepcion", "x").idUsuario(2).rolId(2).build();
        Usuario pac = new Usuario.UsuarioBuilder("ana", "x").idUsuario(40).rolId(4).build();
        Usuario otroPac = new Usuario.UsuarioBuilder("luis", "x").idUsuario(41).rolId(4).build();
        Usuario doc1 = new Usuario.UsuarioBuilder("medico", "x").idUsuario(30).rolId(3).build();
        Usuario doc2 = new Usuario.UsuarioBuilder("medico2", "x").idUsuario(31).rolId(3).build();

        List<EventoSistema> eventos = new ArrayList<>();
        EventoObserver captura = eventos::add;
        EventoSubject.getInstancia().suscribir(captura);

        List<DiaDisponibleDTO> dias = s.dias(1, null, hoy);
        check("tira de 14 días desde hoy", dias.size() == 14 && dias.get(0).getFecha().equals(hoy));
        check("domingo sin atención", dias.stream().filter(d -> d.getFecha().equals(domingo)).allMatch(d -> !d.isAtiende() && d.getLibres() == 0));
        check("miércoles: 18 (30 min) + 27 (20 min) = 45 libres (médico pausado no cuenta)",
              dias.stream().filter(d -> d.getFecha().equals(miercoles)).allMatch(d -> d.getLibres() == 45));
        check("turnos del miércoles: 2 médicos", s.turnos(1, null, miercoles).size() == 2);
        check("turnos con médico elegido: solo 1", s.turnos(1, 2, miercoles).size() == 1);
        check("turnos de una fecha pasada: ninguno", s.turnos(1, null, hoy.minusDays(1)).isEmpty());

        CitaService.Resultado r1 = s.reservar(7, 1, miercoles, LocalTime.of(9, 0), "Dolor de cabeza", recep);
        check("recepción reserva: OK con código CT-000001", r1.isExito() && "CT-000001".equals(r1.codigo()));
        check("evento CITA_RESERVADA publicado (auditoría + correo)", eventos.stream().anyMatch(e -> e.getTipo() == TipoEvento.CITA_RESERVADA && e.getIdEntidad() == r1.idCita()));
        check("canal RECEPCION y fin = inicio + 30", citas.citas.get(r1.idCita()).getCanal().equals("RECEPCION")
              && citas.citas.get(r1.idCita()).getHoraFin().equals(LocalTime.of(9, 30)));
        check("mismo turno otra vez: rechazado (ya tomado)", !s.reservar(7, 1, miercoles, LocalTime.of(9, 0), "", recep).isExito());
        CitaService.Resultado choque = s.reservar(7, 2, miercoles, LocalTime.of(9, 0), "", recep);
        check("mismo paciente a la misma hora con otro médico: rechazado", !choque.isExito() && choque.error().contains("ya tiene una cita"));
        check("mismo médico mismo día otra hora: rechazado", !s.reservar(7, 1, miercoles, LocalTime.of(16, 0), "", recep).isExito());
        check("hora que no calza (09:10): rechazado", !s.reservar(7, 2, miercoles, LocalTime.of(9, 10), "", recep).isExito());
        check("domingo: rechazado", !s.reservar(7, 2, domingo, LocalTime.of(9, 0), "", recep).isExito());
        check("fecha pasada: rechazado", s.reservar(7, 2, hoy.minusDays(1), LocalTime.of(9, 0), "", recep).error().contains("pasó"));
        check("más allá de 60 días: rechazado", s.reservar(7, 2, hoy.plusDays(61), LocalTime.of(9, 0), "", recep).error().contains("hasta el"));
        check("hoy a las 08:00 (ya pasó): rechazado", !s.reservar(7, 2, hoy, LocalTime.of(8, 0), "", recep).isExito());
        check("médico pausado: rechazado", s.reservar(7, 3, miercoles, LocalTime.of(10, 0), "", recep).error().contains("no está recibiendo"));
        check("paciente inactivo: rechazado", !s.reservar(8, 2, miercoles, LocalTime.of(10, 0), "", recep).isExito());
        check("motivo de 251 caracteres: rechazado", !s.reservar(7, 2, miercoles, LocalTime.of(11, 0), "x".repeat(251), recep).isExito());
        check("un médico no reserva", !s.reservar(7, 2, miercoles, LocalTime.of(11, 0), "", doc1).isExito());
        check("paciente reserva para OTRO paciente: rechazado", s.reservar(7, 2, miercoles, LocalTime.of(11, 0), "", otroPac).error().contains("para ti"));
        citas.simularCarrera = true;
        CitaService.Resultado carrera = s.reservar(7, 2, hoy.plusDays(2), LocalTime.of(11, 0), "", recep);
        check("RNF-08: si la BD detecta que otro lo tomó, se informa sin error 500", !carrera.isExito() && carrera.error().contains("otra persona"));
        citas.simularCarrera = false;

        // Portal: máximo 3 pendientes
        CitaService.Resultado p1 = s.reservar(7, 2, hoy.plusDays(2), LocalTime.of(9, 0), "", pac);
        CitaService.Resultado p2 = s.reservar(7, 2, hoy.plusDays(3), LocalTime.of(9, 0), "", pac);
        check("paciente reserva desde el portal (canal PORTAL)", p1.isExito() && p2.isExito() && citas.citas.get(p1.idCita()).isDelPortal());
        CitaService.Resultado p4 = s.reservar(7, 2, hoy.plusDays(6), LocalTime.of(9, 0), "", pac);
        check("con 3 pendientes no puede reservar una 4.ª desde el portal", !p4.isExito() && p4.error().contains("3 citas pendientes"));
        check("...pero recepción sí puede", s.reservar(7, 2, hoy.plusDays(6), LocalTime.of(9, 0), "", recep).isExito());

        // Reprogramar
        check("paciente no reprograma la de mañana (menos de 24 h)", s.reprogramar(r1.idCita(), 1, hoy.plusDays(7), LocalTime.of(9, 0), pac).contains("llama a recepción"));
        check("otro paciente no puede tocar la cita", s.reprogramar(p1.idCita(), 2, hoy.plusDays(7), LocalTime.of(10, 0), otroPac) != null);
        check("reprogramar al mismo horario: avisa", s.reprogramar(p1.idCita(), 2, hoy.plusDays(2), LocalTime.of(9, 0), pac).contains("mismo horario"));
        String rp = s.reprogramar(p1.idCita(), 2, hoy.plusDays(7), LocalTime.of(10, 0), pac);
        check("paciente reprograma con más de 24 h: OK", rp == null && citas.citas.get(p1.idCita()).getFecha().equals(hoy.plusDays(7)));
        check("evento CITA_REPROGRAMADA", eventos.stream().anyMatch(e -> e.getTipo() == TipoEvento.CITA_REPROGRAMADA));
        s.reprogramar(p1.idCita(), 2, hoy.plusDays(8), LocalTime.of(10, 0), pac);
        check("tercera reprogramación del paciente: debe ir a recepción", s.reprogramar(p1.idCita(), 2, hoy.plusDays(9), LocalTime.of(10, 0), pac).contains("recepción"));
        check("recepción sí la reprograma, incluso con otro médico de la especialidad",
              s.reprogramar(p1.idCita(), 1, hoy.plusDays(9), LocalTime.of(10, 0), recep) == null && citas.citas.get(p1.idCita()).getIdMedico() == 1);
        check("recepción reprograma la de mañana (menos de 24 h) sin problema", s.reprogramar(r1.idCita(), 1, miercoles, LocalTime.of(10, 0), recep) == null);

        // Cancelar
        check("paciente no cancela la de mañana", s.cancelar(r1.idCita(), "Tengo otro compromiso", pac) != null);
        check("motivo muy corto: rechazado", s.cancelar(p2.idCita(), "x", pac).contains("motivo"));
        check("paciente cancela con más de 24 h: OK", s.cancelar(p2.idCita(), "Tengo otro compromiso", pac) == null && citas.citas.get(p2.idCita()).isCancelada());
        check("cancelar otra vez: avisa", s.cancelar(p2.idCita(), "Tengo otro compromiso", recep) != null);
        check("el médico no cancela citas", s.cancelar(r1.idCita(), "Emergencia en quirófano", doc1) != null);
        check("evento CITA_CANCELADA", eventos.stream().anyMatch(e -> e.getTipo() == TipoEvento.CITA_CANCELADA && e.getDetalle().contains("Tengo otro compromiso")));
        check("el turno cancelado vuelve a estar libre", s.reservar(7, 2, hoy.plusDays(3), LocalTime.of(9, 0), "", recep).isExito());

        // Llegada y atención (cita de HOY)
        CitaService.Resultado hoyR = s.reservar(7, 2, hoy, LocalTime.of(11, 0), "", recep);
        check("reserva para hoy más tarde: OK", hoyR.isExito());
        check("el paciente no registra su propia llegada", s.registrarLlegada(hoyR.idCita(), pac) != null);
        check("llegada de una cita de mañana: rechazada", s.registrarLlegada(r1.idCita(), recep).contains("día de la cita"));
        check("no asistió antes de la hora: rechazado", s.registrarAtencion(hoyR.idCita(), false, "", doc2).contains("pasó la hora"));
        check("recepción registra la llegada", s.registrarLlegada(hoyR.idCita(), recep) == null && citas.citas.get(hoyR.idCita()).isConfirmada());
        check("otro médico no puede atender la cita", s.registrarAtencion(hoyR.idCita(), true, "", doc1) != null);
        check("nota de 501 caracteres: rechazada", s.registrarAtencion(hoyR.idCita(), true, "x".repeat(501), doc2) != null);
        check("su médico la marca atendida con nota", s.registrarAtencion(hoyR.idCita(), true, "Control en 1 mes", doc2) == null
              && citas.citas.get(hoyR.idCita()).isAtendida());
        check("una atendida no se cancela", s.cancelar(hoyR.idCita(), "Error de registro", recep) != null);
        ahora[0] = LocalDateTime.of(2026, 10, 21, 12, 0);    // ya pasó la cita de mañana (10:00)
        check("al día siguiente, la reservada de las 10:00 se marca no asistió", s.registrarAtencion(r1.idCita(), false, "", doc1) == null
              && citas.citas.get(r1.idCita()).isNoAsistio());

        // Permisos de lectura
        CitaDTO de40 = citas.citas.get(p1.idCita());
        check("puedeVer: paciente dueño sí, otro no; recepción sí; médico de la cita sí",
              CitaService.puedeVer(de40, pac) && !CitaService.puedeVer(de40, otroPac) && CitaService.puedeVer(de40, recep)
              && CitaService.puedeVer(de40, doc1) && !CitaService.puedeVer(de40, doc2));
        check("resumen cuenta por estado", s.resumen(new FiltroCitaDTO(hoy, hoy, null, null, null, null)).getAtendidas() == 1);

        // ===== Notificador (Observer) =====
        List<String> enviados = new ArrayList<>();
        CanalNotificacionCita falso = new CanalNotificacionCita() {
            public String getNombre() { return "falso"; }
            public boolean enviar(CitaDTO x, MensajeCita m) { enviados.add(m.getAsunto() + "|" + m.getTextoCorto()); return true; }
        };
        CanalNotificacionCita roto = new CanalNotificacionCita() {
            public String getNombre() { return "roto"; }
            public boolean enviar(CitaDTO x, MensajeCita m) { throw new RuntimeException("proveedor caído"); }
        };
        CitaDTO cc = citas.citas.get(p1.idCita()); cc.setNombreMedico("Marco Demo"); cc.setConsultorio("Consultorio 101");
        NotificadorCitasObserver notificador = new NotificadorCitasObserver(citas, List.of(roto, falso), () -> param, Runnable::run);
        notificador.actualizar(new EventoSistema(TipoEvento.CITA_RESERVADA, 2, "recepcion", p1.idCita(), ""));
        check("reserva: avisa aunque otro canal falle", enviados.size() == 1 && enviados.get(0).startsWith("Cita reservada"));
        check("SMS de máximo 160 caracteres", enviados.get(0).split("\\|")[1].length() <= 160);
        notificador.actualizar(new EventoSistema(TipoEvento.CITA_LLEGADA, 2, "recepcion", p1.idCita(), ""));
        check("la llegada NO genera aviso al paciente", enviados.size() == 1);
        notificador.actualizar(new EventoSistema(TipoEvento.CITA_CANCELADA, 2, "recepcion", p1.idCita(), ""));
        check("cancelación: avisa", enviados.size() == 2 && enviados.get(1).startsWith("Cita cancelada"));
        CitaDTO sinDatos = new CitaDTO();
        check("correo sin dirección: no envía (sin error)", !new NotificacionEmail((d, n, as, html) -> true).enviar(sinDatos, null));
        sinDatos.setCelularPaciente("987 654 321");
        check("SMS simulado con celular: true", new NotificacionSMS().enviar(sinDatos, MensajeCita.de(TipoEvento.CITA_RESERVADA, cc, param, 24, null)));
        String html = MensajeCita.de(TipoEvento.CITA_RESERVADA, cc, param, 24, null).getHtml();
        check("correo escapa HTML del nombre", !html.contains("<script"));

        EventoSubject.getInstancia().desuscribir(captura);
        System.out.println(fallas == 0 ? "\nTODO OK" : "\nFALLAS: " + fallas);
        System.exit(fallas == 0 ? 0 : 1);
    }
}
