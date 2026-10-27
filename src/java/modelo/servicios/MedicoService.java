package modelo.servicios;

import evento.EventoSubject;
import evento.TipoEvento;
import modelo.citas.CalculadoraTurnos;
import modelo.dto.CitaDTO;
import modelo.dto.HorarioDTO;
import modelo.dto.MedicoDTO;
import modelo.dto.OpcionDTO;
import modelo.entidades.Usuario;
import modelo.repositorio.CatalogoRepository;
import modelo.repositorio.CatalogoRepositoryImpl;
import modelo.repositorio.CitaRepository;
import modelo.repositorio.CitaRepositoryImpl;
import modelo.repositorio.MedicoRepository;
import modelo.repositorio.MedicoRepositoryImpl;
import modelo.repositorio.OperacionRechazadaException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * RF-03: el administrador registra los datos profesionales y el horario
 * semanal de cada médico. Con eso el sistema calcula la disponibilidad (RF-04).
 */
public class MedicoService {

    /** Horario de atención posible de la clínica (no se aceptan bloques fuera de este rango). */
    public static final LocalTime APERTURA = LocalTime.of(6, 0);
    public static final LocalTime CIERRE = LocalTime.of(22, 0);
    public static final int MAXIMO_BLOQUES_POR_DIA = 2;

    private final MedicoRepository medicos;
    private final CitaRepository citas;
    private final CatalogoRepository catalogos;
    private final Supplier<LocalDate> hoy;

    public MedicoService() {
        this(new MedicoRepositoryImpl(), new CitaRepositoryImpl(), new CatalogoRepositoryImpl(), LocalDate::now);
    }

    public MedicoService(MedicoRepository medicos, CitaRepository citas, CatalogoRepository catalogos, Supplier<LocalDate> hoy) {
        this.medicos = medicos;
        this.citas = citas;
        this.catalogos = catalogos;
        this.hoy = hoy;
    }

    /** Resultado de guardar: errores por campo, o el aviso de citas que quedaron fuera del nuevo horario. */
    public record Guardado(Map<String, String> errores, int citasFueraDeHorario) {
        public boolean isExito() { return errores.isEmpty(); }
    }

    public List<MedicoDTO> listar() { return medicos.listarTodos(); }
    public List<MedicoDTO> buscar(String texto, Integer idEspecialidad, String estado) { return medicos.buscar(texto, idEspecialidad, estado); }
    public Optional<MedicoDTO> obtenerPorUsuario(int idUsuario) { return medicos.buscarPorUsuario(idUsuario); }
    public List<OpcionDTO> especialidades() { return catalogos.listarEspecialidades(); }

    public Guardado guardar(MedicoDTO form, Usuario admin) {
        Optional<MedicoDTO> actual = medicos.buscarPorUsuario(form.getIdUsuario());
        if (actual.isEmpty()) {
            return new Guardado(Map.of("general", "Esa cuenta no existe o no es de un médico."), 0);
        }
        Map<String, String> errores = validar(form, especialidades());
        if (!errores.isEmpty()) {
            return new Guardado(errores, 0);
        }
        int idMedico;
        try {
            idMedico = medicos.guardar(form, admin.getNombreUsuario());
        } catch (OperacionRechazadaException e) {
            return new Guardado(Map.of("cmp", e.getMessage()), 0);
        }
        form.setIdMedico(idMedico);
        EventoSubject.getInstancia().publicar(TipoEvento.MEDICO_EDITADO, admin, idMedico,
                form.getNombreCompleto() + " · " + nombreEspecialidad(form.getIdEspecialidad()) + " · " + form.getResumenHorario());
        return new Guardado(Map.of(), contarFueraDeHorario(form));
    }

    /** Pausar: el médico deja de aparecer para nuevas reservas (sus citas ya tomadas se conservan). */
    public String cambiarEstado(int idUsuario, boolean activar, Usuario admin) {
        Optional<MedicoDTO> m = medicos.buscarPorUsuario(idUsuario);
        if (m.isEmpty() || !m.get().isConfigurado()) {
            return "Primero registra los datos y el horario del médico.";
        }
        medicos.cambiarEstado(m.get().getIdMedico(), activar ? "Activo" : "Inactivo");
        EventoSubject.getInstancia().publicar(activar ? TipoEvento.MEDICO_REANUDADO : TipoEvento.MEDICO_PAUSADO,
                admin, m.get().getIdMedico(), m.get().getNombreCompleto());
        return null;
    }

    /** Citas futuras ya reservadas que no encajan en el horario nuevo (para avisar al administrador). */
    private int contarFueraDeHorario(MedicoDTO nuevo) {
        int fuera = 0;
        for (CitaDTO c : citas.activasDelMedico(nuevo.getIdMedico(), hoy.get())) {
            if (!CalculadoraTurnos.dentroDelHorario(nuevo, c.getFecha(), c.getHoraInicio(), c.getHoraFin())) {
                fuera++;
            }
        }
        return fuera;
    }

    private String nombreEspecialidad(int id) {
        return especialidades().stream().filter(o -> o.getId() == id).map(OpcionDTO::getNombre).findFirst().orElse("");
    }

    static Map<String, String> validar(MedicoDTO m, List<OpcionDTO> especialidades) {
        Map<String, String> e = new LinkedHashMap<>();
        if (especialidades.stream().noneMatch(o -> o.getId() == m.getIdEspecialidad())) {
            e.put("idEspecialidad", "Elige la especialidad del médico.");
        }
        if (!m.getCmp().matches("[0-9]{4,6}")) {
            e.put("cmp", "Escribe el N.º de colegiatura (CMP): de 4 a 6 dígitos.");
        }
        if (m.getConsultorio().length() < 2 || m.getConsultorio().length() > 30) {
            e.put("consultorio", "Escribe el consultorio (de 2 a 30 caracteres), por ejemplo «Consultorio 101».");
        }
        if (!MedicoDTO.DURACIONES.contains(m.getDuracionCita())) {
            e.put("duracionCita", "Elige una duración de la lista.");
        }
        String horario = validarHorario(m.getHorarios(), m.getDuracionCita());
        if (horario != null) {
            e.put("horario", horario);
        }
        return e;
    }

    /** @return el primer problema del horario, o null si está bien. */
    static String validarHorario(List<HorarioDTO> bloques, int duracion) {
        for (int i = 0; i < bloques.size(); i++) {
            HorarioDTO b = bloques.get(i);
            String dia = b.getNombreDia();
            if (b.getHoraInicio() == null || b.getHoraFin() == null) {
                return dia + ": completa la hora de inicio y la de fin.";
            }
            if (!b.getHoraFin().isAfter(b.getHoraInicio())) {
                return dia + ": la hora de fin debe ser después de la de inicio.";
            }
            if (b.getHoraInicio().isBefore(APERTURA) || b.getHoraFin().isAfter(CIERRE)) {
                return dia + ": la clínica atiende de 06:00 a 22:00.";
            }
            if (b.getHoraInicio().plusMinutes(duracion).isAfter(b.getHoraFin())) {
                return dia + ": el bloque " + b.getHoraInicio() + "-" + b.getHoraFin() + " es más corto que una cita de "
                        + duracion + " minutos.";
            }
            for (int j = i + 1; j < bloques.size(); j++) {
                if (b.seCruzaCon(bloques.get(j))) {
                    return dia + ": los dos bloques se cruzan. Por ejemplo: mañana 08:00-13:00 y tarde 15:00-19:00.";
                }
            }
        }
        return null;
    }
}
