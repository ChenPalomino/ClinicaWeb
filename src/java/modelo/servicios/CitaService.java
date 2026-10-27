package modelo.servicios;

import configuracion.ConfiguracionClinicaSingleton;
import evento.EventoSubject;
import evento.TipoEvento;
import modelo.citas.CalculadoraTurnos;
import modelo.citas.FechaTexto;
import modelo.citas.ReglasCita;
import modelo.dto.AgendaMedicoDTO;
import modelo.dto.CitaDTO;
import modelo.dto.CuentaAtencionDTO;
import modelo.dto.DiaDisponibleDTO;
import modelo.dto.FiltroCitaDTO;
import modelo.dto.MedicoDTO;
import modelo.dto.OpcionDTO;
import modelo.dto.PacienteDTO;
import modelo.dto.ParametroClinicaDTO;
import modelo.dto.ResumenCitasDTO;
import modelo.dto.TurnoDTO;
import modelo.entidades.Rol;
import modelo.entidades.Usuario;
import modelo.repositorio.CitaRepository;
import modelo.repositorio.CitaRepositoryImpl;
import modelo.repositorio.MedicoRepository;
import modelo.repositorio.MedicoRepositoryImpl;
import modelo.repositorio.OperacionRechazadaException;
import modelo.repositorio.PacienteRepository;
import modelo.repositorio.PacienteRepositoryImpl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Servicio de GESTIÓN DE CITAS (PP-01, RF-04 a RF-10).
 *
 *   - RF-04 disponibilidad: CalculadoraTurnos (horario semanal - turnos ocupados)
 *   - RF-05 reservar: valida, REVALIDA justo antes de guardar y, si otra persona
 *     tomó el turno en ese instante, el índice único de la BD lo rechaza (RNF-08)
 *   - RF-06/07/08/10: cada cambio de estado respeta ReglasCita
 *   - Observer: cada acción publica un evento -> auditoría (RF-21) y avisos al
 *     paciente por correo/SMS (RF-09, NotificadorCitasObserver)
 *
 * Recibe sus dependencias por el constructor: se prueba sin base de datos (RNF-03).
 */
public class CitaService {

    public static final int DIAS_EN_TIRA = 14;
    public static final int MOTIVO_MAXIMO = 250;
    public static final int NOTA_MAXIMA = 500;
    /** Motivos de cancelación para elegir con un clic (Nielsen #6: reconocer antes que recordar). */
    public static final List<String> MOTIVOS_CANCELACION = List.of(
            "Ya no necesito la consulta", "Tengo otro compromiso", "Me siento mejor",
            "Prefiero otro médico u horario", "Problemas de salud o de traslado");

    private final CitaRepository citas;
    private final MedicoRepository medicos;
    private final PacienteRepository pacientes;
    private final Supplier<ParametroClinicaDTO> parametros;
    private final Supplier<LocalDateTime> reloj;
    private final Supplier<CobranzaService> cobranza;

    public CitaService() {
        this(new CitaRepositoryImpl(), new MedicoRepositoryImpl(), new PacienteRepositoryImpl(),
                () -> ConfiguracionClinicaSingleton.getInstancia().getParametros(), LocalDateTime::now, CobranzaService::new);
    }

    public CitaService(CitaRepository citas, MedicoRepository medicos, PacienteRepository pacientes,
                       Supplier<ParametroClinicaDTO> parametros, Supplier<LocalDateTime> reloj,
                       Supplier<CobranzaService> cobranza) {
        this.citas = citas;
        this.medicos = medicos;
        this.pacientes = pacientes;
        this.parametros = parametros;
        this.reloj = reloj;
        this.cobranza = cobranza;
    }

    /** Resultado de reservar: la cita creada o el motivo por el que no se pudo. */
    public record Resultado(int idCita, String codigo, String error) {
        static Resultado fallo(String error) { return new Resultado(0, null, error); }
        public boolean isExito() { return error == null; }
    }

    /** Resultado de "Cobrar" una cita: la cuenta en caja (nueva o la que ya tenía). */
    public record Cobro(int idCuenta, String error, String aviso) {
        public boolean isExito() { return error == null; }
    }

    // ===================== fechas y reglas vigentes =====================

    public LocalDateTime ahora() { return reloj.get(); }
    public LocalDate hoy() { return reloj.get().toLocalDate(); }
    public int horasCancelacion() { return parametros.get().getHorasCancelacionPaciente(); }
    public LocalDate ultimoDiaReserva() { return hoy().plusDays(parametros.get().getDiasMaximosReserva()); }

    // ===================== RF-04: disponibilidad =====================

    /** Especialidades con al menos un médico que atiende. extra = cantidad de médicos. */
    public List<OpcionDTO> especialidades() { return medicos.especialidadesConMedicos(); }

    public List<MedicoDTO> medicos(Integer idEspecialidad) { return medicos.listarQueAtienden(idEspecialidad); }

    public Map<Integer, modelo.dto.PrecioConsultaDTO> preciosConsulta(Integer idPaciente) {
        modelo.dto.CoberturaDTO cobertura = null;
        CobranzaService caja = idPaciente == null ? null : cobranza.get();
        if (caja != null) {
            cobertura = caja.coberturaDelPaciente(idPaciente).orElse(null);
        }
        Map<Integer, modelo.dto.PrecioConsultaDTO> precios = new java.util.HashMap<>();
        for (Map.Entry<Integer, java.math.BigDecimal> e : medicos.preciosConsulta().entrySet()) {
            precios.put(e.getKey(), modelo.cobranza.CotizadorConsulta.cotizar(e.getValue(), cobertura));
        }
        return precios;
    }

    public Optional<MedicoDTO> medicoDeUsuario(int idUsuario) { return medicos.buscarPorUsuario(idUsuario); }

    /** Médicos considerados: los de la especialidad, o solo uno si se eligió. */
    private List<MedicoDTO> medicosFiltrados(Integer idEspecialidad, Integer idMedico) {
        List<MedicoDTO> lista = medicos.listarQueAtienden(idEspecialidad);
        if (idMedico != null && idMedico > 0) {
            lista = lista.stream().filter(m -> m.getIdMedico() == idMedico).collect(Collectors.toList());
        }
        return lista;
    }

    /** Tira de días (desde "desde", máximo DIAS_EN_TIRA) con cuántos turnos libres hay cada día. */
    public List<DiaDisponibleDTO> dias(Integer idEspecialidad, Integer idMedico, LocalDate desde) {
        List<DiaDisponibleDTO> dias = new ArrayList<>();
        LocalDate inicio = desde == null || desde.isBefore(hoy()) ? hoy() : desde;
        LocalDate fin = inicio.plusDays(DIAS_EN_TIRA - 1L);
        if (fin.isAfter(ultimoDiaReserva())) {
            fin = ultimoDiaReserva();
        }
        List<MedicoDTO> lista = medicosFiltrados(idEspecialidad, idMedico);
        if (lista.isEmpty() || inicio.isAfter(fin)) {
            return dias;
        }
        Map<Integer, Map<LocalDate, Set<LocalTime>>> ocupados = citas.ocupados(ids(lista), inicio, fin);
        for (LocalDate d = inicio; !d.isAfter(fin); d = d.plusDays(1)) {
            int libres = 0;
            boolean atiende = false;
            for (MedicoDTO m : lista) {
                List<TurnoDTO> turnos = CalculadoraTurnos.turnos(m, d, ocupadosDe(ocupados, m.getIdMedico(), d), ahora());
                atiende |= !m.horariosDelDia(d.getDayOfWeek().getValue()).isEmpty();
                libres += (int) turnos.stream().filter(TurnoDTO::isLibre).count();
            }
            dias.add(new DiaDisponibleDTO(d, libres, atiende));
        }
        return dias;
    }

    /** Turnos de un día, agrupados por médico (solo los médicos que atienden ese día). */
    public List<AgendaMedicoDTO> turnos(Integer idEspecialidad, Integer idMedico, LocalDate fecha) {
        List<AgendaMedicoDTO> agenda = new ArrayList<>();
        if (fecha == null || fecha.isBefore(hoy()) || fecha.isAfter(ultimoDiaReserva())) {
            return agenda;
        }
        List<MedicoDTO> lista = medicosFiltrados(idEspecialidad, idMedico);
        if (lista.isEmpty()) {
            return agenda;
        }
        Map<Integer, Map<LocalDate, Set<LocalTime>>> ocupados = citas.ocupados(ids(lista), fecha, fecha);
        for (MedicoDTO m : lista) {
            List<TurnoDTO> turnos = CalculadoraTurnos.turnos(m, fecha, ocupadosDe(ocupados, m.getIdMedico(), fecha), ahora());
            if (!turnos.isEmpty()) {
                agenda.add(new AgendaMedicoDTO(m, turnos));
            }
        }
        return agenda;
    }

    private static List<Integer> ids(List<MedicoDTO> lista) {
        return lista.stream().map(MedicoDTO::getIdMedico).collect(Collectors.toList());
    }

    private static Set<LocalTime> ocupadosDe(Map<Integer, Map<LocalDate, Set<LocalTime>>> mapa, int idMedico, LocalDate d) {
        return mapa.getOrDefault(idMedico, Map.of()).getOrDefault(d, Set.of());
    }

    // ===================== consultas =====================

    public Optional<CitaDTO> obtener(int idCita) { return citas.buscarPorId(idCita); }
    public List<CitaDTO> agenda(FiltroCitaDTO filtro) { return citas.agenda(filtro); }
    public Map<LocalDate, ResumenCitasDTO> resumenPorDia(FiltroCitaDTO filtro) { return citas.resumenPorDia(filtro); }
    public Optional<Integer> pacienteDeUsuario(int idUsuario) { return citas.idPacienteDeUsuario(idUsuario); }
    public Optional<PacienteDTO> paciente(int idPaciente) { return pacientes.buscarPorId(idPaciente); }

    public ResumenCitasDTO resumen(FiltroCitaDTO filtro) {
        ResumenCitasDTO total = new ResumenCitasDTO();
        for (ResumenCitasDTO r : citas.resumenPorDia(filtro).values()) {
            for (String estado : FiltroCitaDTO.ESTADOS) {
                total.sumar(estado, r.get(estado));
            }
        }
        return total;
    }

    public List<CitaDTO> proximasDelPaciente(int idPaciente, int limite) {
        return citas.delPaciente(idPaciente, true, hoy(), limite);
    }

    public List<CitaDTO> historialDelPaciente(int idPaciente, int limite) {
        return citas.delPaciente(idPaciente, false, hoy(), limite);
    }

    /** Quién puede ver una cita: el personal de admisión, su médico y el propio paciente. */
    public static boolean puedeVer(CitaDTO c, Usuario u) {
        if (u == null) {
            return false;
        }
        switch (u.getRolId()) {
            case Rol.ADMINISTRADOR:
            case Rol.RECEPCIONISTA:
                return true;
            case Rol.MEDICO:
                return c.getIdUsuarioMedico() == u.getIdUsuario();
            default:
                return c.getIdUsuarioPaciente() == u.getIdUsuario();
        }
    }

    private static boolean esPersonal(Usuario u) {
        return u.getRolId() == Rol.ADMINISTRADOR || u.getRolId() == Rol.RECEPCIONISTA;
    }

    // ===================== RF-05: reservar =====================

    public Resultado reservar(int idPaciente, int idMedico, LocalDate fecha, LocalTime hora, String motivo, Usuario quien) {
        boolean portal = quien.getRolId() == Rol.PACIENTE;
        String m = motivo == null ? "" : motivo.trim();
        String error = validarSolicitante(portal, quien, idPaciente);
        Optional<PacienteDTO> paciente = pacientes.buscarPorId(idPaciente);
        Optional<MedicoDTO> medico = medicos.buscarPorId(idMedico);
        if (error == null) {
            error = validarDatosReserva(paciente, medico, m);
        }
        if (error == null) {
            error = validarTurno(medico.get(), fecha, hora, idPaciente, 0);
        }
        if (error == null && portal) {
            error = validarLimitePortal(idPaciente);
        }
        if (error != null) {
            return Resultado.fallo(error);
        }
        CitaDTO c = nuevaCita(idPaciente, medico.get(), fecha, hora, m, portal);
        CitaRepository.NuevaCita nueva;
        try {
            nueva = citas.reservar(c, quien.getNombreUsuario());
        } catch (OperacionRechazadaException e) {
            return Resultado.fallo(e.getMessage());
        }
        EventoSubject.getInstancia().publicar(TipoEvento.CITA_RESERVADA, quien, nueva.idCita(),
                nueva.codigo() + " · " + paciente.get().getNombreCompleto() + " · " + medico.get().getEspecialidad()
                + " · " + medico.get().getNombreConTitulo() + " · " + FechaTexto.dmy(fecha) + " " + FechaTexto.horaIso(hora)
                + (portal ? " · desde el portal" : ""));
        return new Resultado(nueva.idCita(), nueva.codigo(), null);
    }

    private String validarSolicitante(boolean portal, Usuario quien, int idPaciente) {
        if (!portal && !esPersonal(quien)) {
            return "Tu perfil no puede reservar citas.";
        }
        if (portal && !citas.idPacienteDeUsuario(quien.getIdUsuario()).map(id -> id == idPaciente).orElse(false)) {
            return "Solo puedes reservar citas para ti.";
        }
        return null;
    }

    private static String validarDatosReserva(Optional<PacienteDTO> paciente, Optional<MedicoDTO> medico, String motivo) {
        if (paciente.isEmpty() || !paciente.get().isActivo()) {
            return "Elige un paciente activo.";
        }
        if (motivo.length() > MOTIVO_MAXIMO) {
            return "El motivo de la consulta puede tener hasta " + MOTIVO_MAXIMO + " caracteres.";
        }
        return medico.isEmpty() ? "Elige un médico de la lista." : null;
    }

    private String validarLimitePortal(int idPaciente) {
        if (pendientesDelPaciente(idPaciente, 0) < ReglasCita.MAXIMO_PENDIENTES_PORTAL) {
            return null;
        }
        return "Ya tienes " + ReglasCita.MAXIMO_PENDIENTES_PORTAL + " citas pendientes. Para reservar otra, "
                + "espera a que pase alguna o cancela la que ya no necesites.";
    }

    private static CitaDTO nuevaCita(int idPaciente, MedicoDTO medico, LocalDate fecha, LocalTime hora, String motivo, boolean portal) {
        CitaDTO c = new CitaDTO();
        c.setIdPaciente(idPaciente);
        c.setIdMedico(medico.getIdMedico());
        c.setIdEspecialidad(medico.getIdEspecialidad());
        c.setFecha(fecha);
        c.setHoraInicio(hora);
        c.setHoraFin(hora.plusMinutes(medico.getDuracionCita()));
        c.setMotivo(motivo);
        c.setCanal(portal ? CitaDTO.CANAL_PORTAL : CitaDTO.CANAL_RECEPCION);
        return c;
    }

    /**
     * Validación común de reservar y reprogramar. idCitaExcluir = la cita que se está moviendo (o 0).
     * @return el problema en palabras del usuario, o null si el turno se puede tomar
     */
    String validarTurno(MedicoDTO medico, LocalDate fecha, LocalTime hora, int idPaciente, int idCitaExcluir) {
        String error = validarFecha(medico, fecha, hora);
        if (error != null) {
            return error;
        }
        Optional<TurnoDTO> turno = CalculadoraTurnos.turnoDelHorario(medico, fecha, hora, ahora());
        if (turno.isEmpty()) {
            return medico.getNombreConTitulo() + " no tiene ese horario disponible. Elige uno de los turnos que se muestran.";
        }
        Set<LocalTime> ocupados = ocupadosDe(citas.ocupados(List.of(medico.getIdMedico()), fecha, fecha), medico.getIdMedico(), fecha);
        if (ocupados.contains(hora)) {
            return "Ese horario ya fue tomado. Elige otro, por favor.";
        }
        return validarCruces(medico, fecha, hora, turno.get().getHoraFin(), idPaciente, idCitaExcluir);
    }

    private String validarFecha(MedicoDTO medico, LocalDate fecha, LocalTime hora) {
        if (fecha == null || hora == null) {
            return "Elige el día y la hora de la cita.";
        }
        if (fecha.isBefore(hoy())) {
            return "Esa fecha ya pasó. Elige otro día.";
        }
        if (fecha.isAfter(ultimoDiaReserva())) {
            return "Solo se puede reservar hasta el " + FechaTexto.dmy(ultimoDiaReserva()) + ".";
        }
        return medico.isAtiende() ? null : "Ese médico no está recibiendo citas por ahora.";
    }

    private String validarCruces(MedicoDTO medico, LocalDate fecha, LocalTime hora, LocalTime fin, int idPaciente, int idCitaExcluir) {
        for (CitaDTO otra : citas.activasDelPaciente(idPaciente, fecha)) {
            if (otra.getIdCita() == idCitaExcluir || !otra.getFecha().equals(fecha)) {
                continue;
            }
            if (hora.isBefore(otra.getHoraFin()) && otra.getHoraInicio().isBefore(fin)) {
                return "El paciente ya tiene una cita ese día de " + FechaTexto.hora(otra.getHoraInicio()) + " a "
                        + FechaTexto.hora(otra.getHoraFin()) + " (" + otra.getEspecialidad() + "). Elige otra hora.";
            }
            if (otra.getIdMedico() == medico.getIdMedico()) {
                return "El paciente ya tiene una cita con " + medico.getNombreConTitulo() + " ese mismo día ("
                        + otra.getCodigo() + ").";
            }
        }
        return null;
    }

    private long pendientesDelPaciente(int idPaciente, int idCitaExcluir) {
        return citas.activasDelPaciente(idPaciente, hoy()).stream()
                .filter(c -> c.getIdCita() != idCitaExcluir && c.getInicio().isAfter(ahora())).count();
    }

    /** Cita que el usuario puede modificar (no la del médico ni la de otro paciente). */
    private Optional<CitaDTO> citaModificable(int idCita, Usuario quien) {
        return citas.buscarPorId(idCita).filter(c -> puedeVer(c, quien) && quien.getRolId() != Rol.MEDICO);
    }

    /** Regla de plazo del paciente común a reprogramar y cancelar. */
    private String bloqueoPaciente(CitaDTO c, String porDefecto) {
        if (ReglasCita.pacientePuedeCambiar(c, ahora(), horasCancelacion())) {
            return null;
        }
        String motivo = ReglasCita.motivoBloqueoPaciente(c, ahora(), horasCancelacion());
        return motivo != null ? motivo : porDefecto;
    }

    // ===================== RF-06: reprogramar =====================

    /** @return null si se reprogramó, o el motivo por el que no */
    public String reprogramar(int idCita, int idMedico, LocalDate fecha, LocalTime hora, Usuario quien) {
        Optional<CitaDTO> cita = citaModificable(idCita, quien);
        if (cita.isEmpty()) {
            return "La cita no existe o no puedes modificarla.";
        }
        CitaDTO c = cita.get();
        Optional<MedicoDTO> medico = medicos.buscarPorId(idMedico);
        String error = permisoReprogramar(c, quien);
        if (error == null) {
            error = validarNuevoHorario(c, medico, idMedico, fecha, hora);
        }
        if (error != null) {
            return error;
        }
        boolean hecho;
        try {
            hecho = citas.reprogramar(idCita, idMedico, fecha, hora, hora.plusMinutes(medico.get().getDuracionCita()),
                    quien.getNombreUsuario());
        } catch (OperacionRechazadaException e) {
            return e.getMessage();
        }
        if (!hecho) {
            return "La cita cambió mientras la reprogramabas (quizá ya llegó o se canceló). Revisa su estado.";
        }
        EventoSubject.getInstancia().publicar(TipoEvento.CITA_REPROGRAMADA, quien, idCita,
                c.getCodigo() + " · " + c.getNombrePaciente() + " · de " + FechaTexto.dmy(c.getFecha()) + " "
                + FechaTexto.horaIso(c.getHoraInicio()) + " a " + FechaTexto.dmy(fecha) + " " + FechaTexto.horaIso(hora)
                + (idMedico != c.getIdMedico() ? " con " + medico.get().getNombreConTitulo() : ""));
        return null;
    }

    private String permisoReprogramar(CitaDTO c, Usuario quien) {
        if (quien.getRolId() != Rol.PACIENTE) {
            return ReglasCita.personalPuedeReprogramar(c, ahora()) ? null
                    : "Solo se reprograma una cita reservada que todavía no empieza.";
        }
        String bloqueo = bloqueoPaciente(c, "Esta cita ya no se puede reprogramar.");
        if (bloqueo != null) {
            return bloqueo;
        }
        return ReglasCita.pacientePuedeReprogramar(c, ahora(), horasCancelacion()) ? null
                : "Esta cita ya se reprogramó " + ReglasCita.MAXIMO_REPROGRAMACIONES_PACIENTE
                  + " veces. Para cambiarla otra vez, llama a recepción.";
    }

    private String validarNuevoHorario(CitaDTO c, Optional<MedicoDTO> medico, int idMedico, LocalDate fecha, LocalTime hora) {
        if (medico.isEmpty() || medico.get().getIdEspecialidad() != c.getIdEspecialidad()) {
            return "Elige un médico de " + c.getEspecialidad() + ".";
        }
        if (idMedico == c.getIdMedico() && c.getFecha().equals(fecha) && c.getHoraInicio().equals(hora)) {
            return "Ese es el mismo horario que ya tiene la cita. Elige otro.";
        }
        return validarTurno(medico.get(), fecha, hora, c.getIdPaciente(), c.getIdCita());
    }

    // ===================== RF-07: cancelar =====================

    public String cancelar(int idCita, String motivo, Usuario quien) {
        Optional<CitaDTO> cita = citaModificable(idCita, quien);
        if (cita.isEmpty()) {
            return "La cita no existe o no puedes cancelarla.";
        }
        CitaDTO c = cita.get();
        String m = motivo == null ? "" : motivo.trim();
        String error = permisoCancelar(c, quien);
        if (error == null && (m.length() < 5 || m.length() > MOTIVO_MAXIMO)) {
            error = "Indica el motivo de la cancelación (de 5 a " + MOTIVO_MAXIMO + " caracteres).";
        }
        if (error != null) {
            return error;
        }
        if (!citas.cancelar(idCita, m, quien.getNombreUsuario())) {
            return "La cita cambió mientras la cancelabas. Revisa su estado.";
        }
        EventoSubject.getInstancia().publicar(TipoEvento.CITA_CANCELADA, quien, idCita,
                c.getCodigo() + " · " + c.getNombrePaciente() + " · " + FechaTexto.dmy(c.getFecha()) + " "
                + FechaTexto.horaIso(c.getHoraInicio()) + " · Motivo: " + m);
        return null;
    }

    private String permisoCancelar(CitaDTO c, Usuario quien) {
        if (quien.getRolId() == Rol.PACIENTE) {
            return bloqueoPaciente(c, "Esta cita ya no se puede cancelar.");
        }
        if (ReglasCita.personalPuedeCancelar(c, hoy())) {
            return null;
        }
        return c.isActiva() ? "Esa cita ya pasó: márcala como «No asistió» desde la agenda del médico."
                            : "Esta cita ya está " + c.getEstadoLegible().toLowerCase() + ".";
    }

    // ===================== RF-08: confirmar asistencia (llegada) =====================

    public String registrarLlegada(int idCita, Usuario quien) {
        if (!esPersonal(quien)) {
            return "Solo recepción registra la llegada del paciente.";
        }
        Optional<CitaDTO> cita = citas.buscarPorId(idCita);
        if (cita.isEmpty()) {
            return "La cita no existe.";
        }
        if (!ReglasCita.puedeRegistrarLlegada(cita.get(), hoy())) {
            return cita.get().isReservada() ? "La llegada solo se registra el día de la cita."
                                            : "Esta cita ya está " + cita.get().getEstadoLegible().toLowerCase() + ".";
        }
        if (!citas.registrarLlegada(idCita, quien.getNombreUsuario())) {
            return "La cita cambió mientras registrabas la llegada. Revisa su estado.";
        }
        EventoSubject.getInstancia().publicar(TipoEvento.CITA_LLEGADA, quien, idCita,
                cita.get().getCodigo() + " · " + cita.get().getNombrePaciente());
        return null;
    }

    // ===================== RF-10: registrar la atención =====================

    public String registrarAtencion(int idCita, boolean atendida, String nota, Usuario quien) {
        Optional<CitaDTO> cita = citas.buscarPorId(idCita).filter(c -> esDeSuAgenda(c, quien));
        if (cita.isEmpty()) {
            return "La cita no existe o no es de tu agenda.";
        }
        CitaDTO c = cita.get();
        String n = nota == null ? "" : nota.trim();
        String error = atendida ? permisoAtendida(c) : permisoNoAsistio(c);
        if (error == null && n.length() > NOTA_MAXIMA) {
            error = "La nota puede tener hasta " + NOTA_MAXIMA + " caracteres.";
        }
        if (error != null) {
            return error;
        }
        String estado = atendida ? CitaDTO.ATENDIDA : CitaDTO.NO_ASISTIO;
        if (!citas.registrarAtencion(idCita, estado, n, quien.getNombreUsuario())) {
            return "La cita cambió mientras la registrabas. Revisa su estado.";
        }
        EventoSubject.getInstancia().publicar(atendida ? TipoEvento.CITA_ATENDIDA : TipoEvento.CITA_NO_ASISTIO, quien, idCita,
                c.getCodigo() + " · " + c.getNombrePaciente());
        return null;
    }

    private static boolean esDeSuAgenda(CitaDTO c, Usuario quien) {
        boolean suMedico = quien.getRolId() == Rol.MEDICO && c.getIdUsuarioMedico() == quien.getIdUsuario();
        return suMedico || quien.getRolId() == Rol.ADMINISTRADOR;
    }

    private String permisoAtendida(CitaDTO c) {
        if (ReglasCita.puedeMarcarAtendida(c, hoy())) {
            return null;
        }
        return c.isActiva() ? "Todavía no es el día de esta cita." : "Esta cita ya está " + c.getEstadoLegible().toLowerCase() + ".";
    }

    private String permisoNoAsistio(CitaDTO c) {
        if (ReglasCita.puedeMarcarNoAsistio(c, ahora())) {
            return null;
        }
        return c.isConfirmada() ? "El paciente ya llegó: márcala como atendida."
                                : "Solo se marca «No asistió» cuando ya pasó la hora de la cita.";
    }

    // ===================== Cita -> caja (une PP-01 con PP-03) =====================

    /**
     * Abre (o reutiliza) la cuenta de atención del paciente con la consulta de la especialidad
     * ya cargada, y la deja unida a la cita. Recepción solo tiene que cobrar.
     */
    public Cobro cobrar(int idCita, Usuario quien) {
        if (!esPersonal(quien)) {
            return new Cobro(0, "Solo recepción o el administrador cobran en caja.", null);
        }
        Optional<CitaDTO> cita = citas.buscarPorId(idCita);
        if (cita.isEmpty()) {
            return new Cobro(0, "La cita no existe.", null);
        }
        return abrirCuentaDeCita(cita.get(), quien);
    }

    public Cobro prepararPagoEnLinea(int idCita, Usuario paciente) {
        Optional<CitaDTO> cita = citas.buscarPorId(idCita);
        if (cita.isEmpty() || paciente == null || paciente.getRolId() != Rol.PACIENTE || !puedeVer(cita.get(), paciente)) {
            return new Cobro(0, "La cita no existe.", null);
        }
        if (!cita.get().isActiva()) {
            return new Cobro(0, "Solo se pagan por adelantado las citas reservadas.", null);
        }
        return abrirCuentaDeCita(cita.get(), paciente);
    }

    private Cobro abrirCuentaDeCita(CitaDTO c, Usuario quien) {
        int idCita = c.getIdCita();
        if (c.isTieneCuenta()) {
            return new Cobro(c.getIdCuentaAtencion(), null, null);
        }
        if (!ReglasCita.puedeCobrar(c)) {
            return new Cobro(0, "Una cita " + c.getEstadoLegible().toLowerCase() + " no se cobra.", null);
        }
        CobranzaService caja = cobranza.get();
        CobranzaService.Apertura a = caja.abrirCuenta(c.getIdPaciente(), "Cita " + c.getCodigo() + " · " + c.getEspecialidad(), quien);
        if (!a.isExito()) {
            return new Cobro(0, a.error(), null);
        }
        String aviso = a.yaExistia() ? "El paciente ya tenía una cuenta por cobrar: se usó esa misma." : null;
        Optional<Integer> consulta = medicos.servicioDeConsulta(c.getIdEspecialidad());
        if (consulta.isEmpty()) {
            aviso = "No hay una consulta de " + c.getEspecialidad() + " en el tarifario: agrega el servicio a mano.";
        } else {
            Optional<CuentaAtencionDTO> cuenta = caja.obtener(a.idCuenta());
            boolean yaEsta = cuenta.isPresent() && cuenta.get().getDetalles().stream()
                    .anyMatch(d -> d.getIdProductoServicio() == consulta.get());
            if (!yaEsta) {
                String error = caja.agregarItem(a.idCuenta(), consulta.get(), 1, quien);
                if (error != null) {
                    aviso = error;
                }
            }
        }
        citas.vincularCuenta(idCita, a.idCuenta());
        return new Cobro(a.idCuenta(), null, aviso);
    }
}
