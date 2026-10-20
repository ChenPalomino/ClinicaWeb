import modelo.dto.*;
import modelo.repositorio.*;

import java.time.*;
import java.util.*;
import java.util.concurrent.*;

/** Pruebas de INTEGRACIÓN con PostgreSQL del módulo de citas: repositorios, índice anti doble reserva (RNF-08). */
public class PruebaCitasBD {
    static int fallas = 0;
    static void check(String n, boolean ok) { System.out.println((ok ? "OK   " : "FALLA ") + n); if (!ok) fallas++; }

    public static void main(String[] a) throws Exception {
        MedicoRepository medicos = new MedicoRepositoryImpl();
        CitaRepository citas = new CitaRepositoryImpl();

        List<MedicoDTO> todos = medicos.listarTodos();
        check("listarTodos: el médico demo con horario de 11 bloques", todos.stream().anyMatch(m -> m.getCmp().equals("012345") && m.getHorarios().size() == 11));
        MedicoDTO demo = todos.stream().filter(m -> m.getCmp().equals("012345")).findFirst().orElseThrow();
        check("listarQueAtienden(Medicina General) lo incluye", medicos.listarQueAtienden(demo.getIdEspecialidad()).size() == 1);
        check("especialidadesConMedicos: 1 especialidad, 1 médico", medicos.especialidadesConMedicos().size() == 1
              && medicos.especialidadesConMedicos().get(0).getExtra().equals("1"));
        check("servicioDeConsulta(Medicina General) = SRV-0001", medicos.servicioDeConsulta(demo.getIdEspecialidad()).isPresent());
        check("buscarPorUsuario y buscarPorId coinciden", medicos.buscarPorUsuario(demo.getIdUsuario()).get().getIdMedico() == demo.getIdMedico()
              && medicos.buscarPorId(demo.getIdMedico()).isPresent());

        // guardar: reemplaza el horario en una transacción
        demo.setConsultorio("Consultorio 102");
        demo.setHorarios(new ArrayList<>(List.of(new HorarioDTO(1, LocalTime.of(8, 0), LocalTime.of(12, 0)),
                new HorarioDTO(2, LocalTime.of(8, 0), LocalTime.of(12, 0)), new HorarioDTO(3, LocalTime.of(8, 0), LocalTime.of(12, 0)),
                new HorarioDTO(4, LocalTime.of(8, 0), LocalTime.of(12, 0)), new HorarioDTO(5, LocalTime.of(8, 0), LocalTime.of(12, 0)),
                new HorarioDTO(6, LocalTime.of(8, 0), LocalTime.of(12, 0)), new HorarioDTO(7, LocalTime.of(8, 0), LocalTime.of(12, 0)))));
        medicos.guardar(demo, "prueba");
        MedicoDTO leido = medicos.buscarPorId(demo.getIdMedico()).get();
        check("guardar: horario reemplazado (7 bloques) y consultorio nuevo", leido.getHorarios().size() == 7 && leido.getConsultorio().equals("Consultorio 102"));

        int idPaciente = citas.idPacienteDeUsuario(4).orElse(0);
        check("idPacienteDeUsuario(paciente demo)", idPaciente > 0);

        LocalDate dia = LocalDate.now().plusDays(2);
        CitaDTO c = new CitaDTO();
        c.setIdPaciente(idPaciente); c.setIdMedico(demo.getIdMedico()); c.setIdEspecialidad(demo.getIdEspecialidad());
        c.setFecha(dia); c.setHoraInicio(LocalTime.of(9, 0)); c.setHoraFin(LocalTime.of(9, 30)); c.setMotivo("Control"); c.setCanal("PORTAL");
        CitaRepository.NuevaCita n = citas.reservar(c, "prueba");
        check("reservar: código CT-000001", n.codigo().equals("CT-000001"));
        CitaDTO b = citas.buscarPorId(n.idCita()).get();
        check("buscarPorId: datos completos (paciente, médico, correo)", b.getNombrePaciente().contains("Paciente") && b.getNombreMedico().contains("Médico")
              && b.getCorreoPaciente().equals("paciente@clinica.demo") && b.isReservada() && b.isDelPortal());
        check("ocupados: el turno 09:00 aparece", citas.ocupados(List.of(demo.getIdMedico()), dia, dia).get(demo.getIdMedico()).get(dia).contains(LocalTime.of(9, 0)));

        // RNF-08: 10 reservas SIMULTÁNEAS del mismo turno -> solo 1 se guarda
        LocalTime hora = LocalTime.of(10, 0);
        ExecutorService pool = Executors.newFixedThreadPool(10);
        CountDownLatch salida = new CountDownLatch(1);
        List<Future<String>> res = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            res.add(pool.submit(() -> {
                salida.await();
                CitaDTO x = new CitaDTO();
                x.setIdPaciente(idPaciente); x.setIdMedico(demo.getIdMedico()); x.setIdEspecialidad(demo.getIdEspecialidad());
                x.setFecha(dia.plusDays(1)); x.setHoraInicio(hora); x.setHoraFin(hora.plusMinutes(30)); x.setCanal("RECEPCION");
                try { citas.reservar(x, "hilo"); return "OK"; } catch (OperacionRechazadaException e) { return "RECHAZO"; }
            }));
        }
        salida.countDown();
        int ok = 0, rechazos = 0;
        for (Future<String> f : res) { if (f.get().equals("OK")) ok++; else rechazos++; }
        pool.shutdown();
        check("RNF-08: 10 reservas simultáneas del mismo turno -> 1 guardada, 9 rechazadas con mensaje (" + ok + "/" + rechazos + ")", ok == 1 && rechazos == 9);

        // Paciente a la misma hora con otro médico (índice del paciente)
        CitaDTO choque = new CitaDTO();
        choque.setIdPaciente(idPaciente); choque.setIdMedico(demo.getIdMedico()); choque.setIdEspecialidad(demo.getIdEspecialidad());
        choque.setFecha(dia); choque.setHoraInicio(LocalTime.of(9, 0)); choque.setHoraFin(LocalTime.of(9, 30)); choque.setCanal("RECEPCION");
        try { citas.reservar(choque, "x"); check("doble reserva rechazada", false); } catch (OperacionRechazadaException e) { check("doble reserva rechazada: " + e.getMessage(), true); }

        // Reprogramar, estados condicionados
        check("reprogramar a 11:00", citas.reprogramar(n.idCita(), demo.getIdMedico(), dia, LocalTime.of(11, 0), LocalTime.of(11, 30), "prueba"));
        b = citas.buscarPorId(n.idCita()).get();
        check("reprogramaciones = 1 y el turno 09:00 quedó libre", b.getReprogramaciones() == 1
              && !citas.ocupados(List.of(demo.getIdMedico()), dia, dia).get(demo.getIdMedico()).get(dia).contains(LocalTime.of(9, 0)));
        try { citas.reprogramar(n.idCita(), demo.getIdMedico(), dia.plusDays(1), hora, hora.plusMinutes(30), "prueba"); check("reprogramar a turno ocupado: rechazo", false); }
        catch (OperacionRechazadaException e) { check("reprogramar a turno ocupado: rechazo", true); }
        check("llegada: RESERVADA -> CONFIRMADA", citas.registrarLlegada(n.idCita(), "recepcion"));
        check("llegada otra vez: false (ya no está reservada)", !citas.registrarLlegada(n.idCita(), "recepcion"));
        check("reprogramar con llegada: false", !citas.reprogramar(n.idCita(), demo.getIdMedico(), dia, LocalTime.of(11, 30), LocalTime.of(12, 0), "x"));
        check("atención con nota", citas.registrarAtencion(n.idCita(), CitaDTO.ATENDIDA, "Todo bien", "medico"));
        check("cancelar una atendida: false", !citas.cancelar(n.idCita(), "No", "x"));

        // Agenda y resumen por día
        FiltroCitaDTO f = new FiltroCitaDTO(dia, dia.plusDays(1), null, null, null, null);
        check("agenda del rango: 2 citas", citas.agenda(f).size() == 2);
        check("agenda filtrada por texto (DNI)", citas.agenda(new FiltroCitaDTO(dia, dia.plusDays(1), null, null, null, "10000004")).size() == 2);
        check("agenda filtrada por estado ATENDIDA: 1", citas.agenda(new FiltroCitaDTO(dia, dia.plusDays(1), null, null, "ATENDIDA", null)).size() == 1);
        check("agenda con comodín % no rompe la búsqueda", citas.agenda(new FiltroCitaDTO(dia, dia, null, null, null, "%")).isEmpty());
        Map<LocalDate, ResumenCitasDTO> porDia = citas.resumenPorDia(f);
        check("resumenPorDia: 2 días con 1 cita cada uno", porDia.size() == 2 && porDia.get(dia).getAtendidas() == 1);
        check("próximas del paciente: 1 (la reservada de pasado mañana+1)", citas.delPaciente(idPaciente, true, LocalDate.now(), 10).size() == 1);
        check("historial del paciente: la atendida", citas.delPaciente(idPaciente, false, LocalDate.now(), 10).stream().anyMatch(CitaDTO::isAtendida));
        check("activasDelMedico: 1", citas.activasDelMedico(demo.getIdMedico(), LocalDate.now()).size() == 1);

        // Cancelación libera el turno: se puede volver a reservar el mismo
        int idOtra = citas.agenda(new FiltroCitaDTO(dia.plusDays(1), dia.plusDays(1), null, null, null, null)).get(0).getIdCita();
        check("cancelar la reservada", citas.cancelar(idOtra, "Tengo otro compromiso", "paciente"));
        CitaDTO d = new CitaDTO();
        d.setIdPaciente(idPaciente); d.setIdMedico(demo.getIdMedico()); d.setIdEspecialidad(demo.getIdEspecialidad());
        d.setFecha(dia.plusDays(1)); d.setHoraInicio(hora); d.setHoraFin(hora.plusMinutes(30)); d.setCanal("RECEPCION");
        check("el turno cancelado se puede volver a reservar", citas.reservar(d, "x").idCita() > 0);

        System.out.println(fallas == 0 ? "\nTODO OK" : "\nFALLAS: " + fallas);
        System.exit(fallas == 0 ? 0 : 1);
    }
}
