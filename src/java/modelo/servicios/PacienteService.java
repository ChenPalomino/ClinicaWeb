package modelo.servicios;

import evento.EventoSubject;
import evento.TipoEvento;

import modelo.dto.FiltroBusquedaDTO;
import modelo.dto.OpcionDTO;
import modelo.dto.PacienteDTO;
import modelo.dto.PaginaDTO;
import modelo.entidades.Rol;
import modelo.entidades.Usuario;
import modelo.repositorio.CatalogoRepository;
import modelo.repositorio.CatalogoRepositoryImpl;
import modelo.repositorio.PacienteRepository;
import modelo.repositorio.PacienteRepositoryImpl;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Lógica de negocio de PACIENTES (SC-01 Gestión de pacientes; RF-01 y RF-02).
 * Valida en el servidor y delega la persistencia a los Repository (interfaces):
 * por eso se puede probar con JUnit usando repositorios falsos (RNF-03).
 */
public class PacienteService {

    private static final int MAX_OBSERVACIONES = 250;

    private final PacienteRepository pacienteRepository;
    private final CatalogoRepository catalogoRepository;

    public PacienteService() {
        this(new PacienteRepositoryImpl(), new CatalogoRepositoryImpl());
    }

    public PacienteService(PacienteRepository pacienteRepository, CatalogoRepository catalogoRepository) {
        this.pacienteRepository = pacienteRepository;
        this.catalogoRepository = catalogoRepository;
    }

    public PaginaDTO<PacienteDTO> listar(FiltroBusquedaDTO filtro) { return pacienteRepository.listar(filtro); }
    public Optional<PacienteDTO> obtener(int id) { return pacienteRepository.buscarPorId(id); }
    public List<OpcionDTO> coberturas() { return catalogoRepository.listarCoberturas(); }
    public List<OpcionDTO> sexos() { return catalogoRepository.listarSexos(); }

    /**
     * Corregir la IDENTIDAD de un paciente ya registrado (DNI, nombres, apellidos)
     * o darlo de baja es tarea del Administrador. Recepción registra pacientes y
     * actualiza su contacto, cobertura y observaciones (RNF-06: acceso por rol).
     */
    public static boolean puedeCorregirIdentidad(Usuario sesion) {
        return sesion != null && sesion.getRolId() == Rol.ADMINISTRADOR;
    }

    public static boolean puedeDesactivar(Usuario sesion) {
        return puedeCorregirIdentidad(sesion);
    }

    /** @return errores por campo; vacío = guardado (y p.idPaciente queda con el id). */
    public Map<String, String> guardar(PacienteDTO p, Usuario sesion) {
        normalizar(p);
        Map<String, String> errores = new LinkedHashMap<>();
        Optional<PacienteDTO> antes = p.isNuevo() ? Optional.empty() : pacienteRepository.buscarPorId(p.getIdPaciente());
        if (!p.isNuevo() && antes.isEmpty()) {
            errores.put("general", "El paciente ya no existe. Actualiza la lista e inténtalo de nuevo.");
            return errores;
        }
        antes.ifPresent(a -> conservarIdentidad(p, a, sesion));
        errores.putAll(validar(p));
        if (errores.isEmpty()) {
            validarDuplicados(p, errores);
        }
        if (!errores.isEmpty()) {
            return errores;
        }
        p.setUsuarioRegistro(sesion.getNombreUsuario());
        if (p.isNuevo()) {
            p.setIdPaciente(pacienteRepository.insertar(p));
            EventoSubject.getInstancia().publicar(TipoEvento.PACIENTE_REGISTRADO, sesion, p.getIdPaciente(),
                    p.getNombreCompleto() + " · DNI " + p.getDni() + " · " + p.getNombreCobertura());
        } else if (!pacienteRepository.actualizar(p)) {
            errores.put("general", "El paciente ya no existe. Actualiza la lista e inténtalo de nuevo.");
        } else {
            EventoSubject.getInstancia().publicar(TipoEvento.PACIENTE_EDITADO, sesion, p.getIdPaciente(),
                    cambios(antes.get(), p));
        }
        return errores;
    }

    /** Recepción no cambia identidad ni estado aunque modifique el formulario a mano. */
    private static void conservarIdentidad(PacienteDTO p, PacienteDTO antes, Usuario sesion) {
        p.setIdDatosPersonales(antes.getIdDatosPersonales());
        if (puedeCorregirIdentidad(sesion)) {
            return;
        }
        p.setDni(antes.getDni());
        p.setNombres(antes.getNombres());
        p.setApellidos(antes.getApellidos());
        p.setEstado(antes.getEstado());
    }

    private void validarDuplicados(PacienteDTO p, Map<String, String> errores) {
        Optional<String> historia = pacienteRepository.historiaConDni(p.getDni(), p.getIdPaciente());
        if (historia.isPresent()) {
            errores.put("dni", "Ese DNI ya es del paciente con historia " + historia.get() + ". Búscalo en la lista.");
        }
        if (!p.getCorreo().isEmpty() && pacienteRepository.existeCorreoDeOtraPersona(p.getCorreo(), p.getDni())) {
            errores.put("correo", "Ese correo ya está registrado para otra persona.");
        }
    }

    public String desactivar(int id, Usuario sesion) {
        return cambiarEstado(id, sesion, false);
    }

    public String reactivar(int id, Usuario sesion) {
        return cambiarEstado(id, sesion, true);
    }

    private String cambiarEstado(int id, Usuario sesion, boolean activar) {
        if (!puedeDesactivar(sesion)) {
            return "Solo el administrador puede " + (activar ? "reactivar" : "desactivar") + " pacientes.";
        }
        boolean ok = activar ? pacienteRepository.reactivar(id) : pacienteRepository.eliminar(id);
        if (!ok) {
            return "El paciente no existe.";
        }
        String quien = pacienteRepository.buscarPorId(id).map(PacienteDTO::getNombreCompleto).orElse("#" + id);
        EventoSubject.getInstancia().publicar(activar ? TipoEvento.PACIENTE_REACTIVADO : TipoEvento.PACIENTE_DESACTIVADO,
                sesion, id, quien);
        return null;
    }

    /** "Ana Torres · cobertura: Particular → EPS · celular actualizado" (para la auditoría). */
    static String cambios(PacienteDTO a, PacienteDTO d) {
        StringBuilder c = new StringBuilder(d.getNombreCompleto());
        if (!a.getNombreCompleto().equals(d.getNombreCompleto())) c.append(" · nombre: ").append(a.getNombreCompleto()).append(" → ").append(d.getNombreCompleto());
        if (!a.getDni().equals(d.getDni())) c.append(" · DNI: ").append(a.getDni()).append(" → ").append(d.getDni());
        if (a.getIdCobertura() != d.getIdCobertura()) c.append(" · cobertura: ").append(a.getNombreCobertura()).append(" → ").append(d.getNombreCobertura());
        c.append(cambiosDeContacto(a, d));
        if (!a.getEstado().equals(d.getEstado())) c.append(" · estado: ").append(a.getEstado()).append(" → ").append(d.getEstado());
        return c.toString();
    }

    private static String cambiosDeContacto(PacienteDTO a, PacienteDTO d) {
        StringBuilder c = new StringBuilder();
        if (!a.getCelular().equals(d.getCelular()) && !d.getCelular().isEmpty()) c.append(" · celular actualizado");
        if (!a.getCorreo().equalsIgnoreCase(d.getCorreo()) && !d.getCorreo().isEmpty()) c.append(" · correo actualizado");
        if (!a.getObservaciones().equals(d.getObservaciones())) c.append(" · observaciones actualizadas");
        if (!java.util.Objects.equals(a.getFechaNacimiento(), d.getFechaNacimiento())) c.append(" · fecha de nacimiento");
        return c.toString();
    }

    // ------------------------------------------------------------------ reglas

    private static void normalizar(PacienteDTO p) {
        p.setNombres(Validaciones.normalizar(p.getNombres()));
        p.setApellidos(Validaciones.normalizar(p.getApellidos()));
        p.setDni(Validaciones.soloDigitos(p.getDni()));
        p.setCorreo(p.getCorreo().trim().toLowerCase());
        p.setCelular(Validaciones.soloDigitos(p.getCelular()));
        p.setNumeroAfiliacion(Validaciones.normalizar(p.getNumeroAfiliacion()).toUpperCase());
        p.setObservaciones(p.getObservaciones().trim());
        if (!"Inactivo".equals(p.getEstado())) {
            p.setEstado("Activo");
        }
    }

    /** Reglas puras (sin BD): visibles para las pruebas unitarias. */
    Map<String, String> validar(PacienteDTO p) {
        Map<String, String> errores = new LinkedHashMap<>();
        if (!Validaciones.nombre(p.getNombres())) errores.put("nombres", "Escribe los nombres solo con letras (mínimo 2).");
        if (!Validaciones.nombre(p.getApellidos())) errores.put("apellidos", "Escribe los apellidos solo con letras (mínimo 2).");
        if (!Validaciones.dni(p.getDni())) errores.put("dni", "El DNI debe tener exactamente 8 números.");
        String errorFecha = Validaciones.fechaNacimiento(p.getFechaNacimiento());
        if (errorFecha != null) errores.put("fechaNacimiento", errorFecha);
        validarContacto(p, errores);
        validarCobertura(p, errores);
        if (p.getObservaciones().length() > MAX_OBSERVACIONES) {
            errores.put("observaciones", "Máximo " + MAX_OBSERVACIONES + " caracteres.");
        }
        return errores;
    }

    /** RF-01: al menos un dato de contacto para confirmar y recordar las citas. */
    private static void validarContacto(PacienteDTO p, Map<String, String> errores) {
        if (p.getCorreo().isEmpty() && p.getCelular().isEmpty()) {
            errores.put("celular", "Registra al menos un celular o un correo para avisarle de sus citas.");
        }
        if (!p.getCorreo().isEmpty() && !Validaciones.correo(p.getCorreo())) errores.put("correo", "Escribe un correo válido.");
        if (!p.getCelular().isEmpty() && !Validaciones.celular(p.getCelular())) {
            errores.put("celular", "El celular debe tener 9 números y empezar con 9.");
        }
    }

    /** Si no es particular, se exige el N.º de afiliación (lo pide la aseguradora al cobrar). */
    private void validarCobertura(PacienteDTO p, Map<String, String> errores) {
        Optional<OpcionDTO> cobertura = catalogoRepository.listarCoberturas().stream()
                .filter(c -> c.getId() == p.getIdCobertura()).findFirst();
        if (cobertura.isEmpty()) {
            errores.put("idCobertura", "Elige una cobertura de la lista.");
            return;
        }
        p.setTipoCobertura(cobertura.get().getExtra());
        p.setNombreCobertura(cobertura.get().getNombre());
        boolean particular = "PARTICULAR".equals(cobertura.get().getExtra());
        if (particular) {
            p.setNumeroAfiliacion("");
        } else if (p.getNumeroAfiliacion().isEmpty()) {
            errores.put("numeroAfiliacion", "Para " + cobertura.get().getNombre() + " escribe el N.º de afiliación o póliza.");
        } else if (p.getNumeroAfiliacion().length() > 30) {
            errores.put("numeroAfiliacion", "Máximo 30 caracteres.");
        }
    }
}
