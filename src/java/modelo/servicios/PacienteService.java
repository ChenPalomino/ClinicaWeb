package modelo.servicios;

import modelo.dto.FiltroBusquedaDTO;
import modelo.dto.OpcionDTO;
import modelo.dto.PacienteDTO;
import modelo.dto.PaginaDTO;
import modelo.entidades.Usuario;
import modelo.repositorio.CatalogoRepository;
import modelo.repositorio.CatalogoRepositoryImpl;
import modelo.repositorio.PacienteRepository;
import modelo.repositorio.PacienteRepositoryImpl;

import java.time.LocalDate;
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
    private static final int EDAD_MAXIMA = 120;

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

    /** @return errores por campo; vacío = guardado (y p.idPaciente queda con el id). */
    public Map<String, String> guardar(PacienteDTO p, Usuario sesion) {
        normalizar(p);
        Map<String, String> errores = validar(p);
        if (errores.isEmpty()) {
            Optional<String> historia = pacienteRepository.historiaConDni(p.getDni(), p.getIdPaciente());
            if (historia.isPresent()) {
                errores.put("dni", "Ese DNI ya es del paciente con historia " + historia.get() + ". Búscalo en la lista.");
            }
            if (!p.getCorreo().isEmpty()
                    && pacienteRepository.existeCorreoDeOtraPersona(p.getCorreo(), p.getDni())) {
                errores.put("correo", "Ese correo ya está registrado para otra persona.");
            }
        }
        if (!errores.isEmpty()) {
            return errores;
        }
        p.setUsuarioRegistro(sesion.getNombreUsuario());
        if (p.isNuevo()) {
            p.setIdPaciente(pacienteRepository.insertar(p));
        } else if (!pacienteRepository.actualizar(p)) {
            errores.put("general", "El paciente ya no existe. Actualiza la lista e inténtalo de nuevo.");
        }
        return errores;
    }

    public String desactivar(int id) {
        return pacienteRepository.eliminar(id) ? null : "El paciente no existe.";
    }

    public String reactivar(int id) {
        return pacienteRepository.reactivar(id) ? null : "El paciente no existe.";
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
        if (p.getFechaNacimiento() != null) {
            LocalDate hoy = LocalDate.now();
            if (p.getFechaNacimiento().isAfter(hoy)) {
                errores.put("fechaNacimiento", "La fecha de nacimiento no puede ser futura.");
            } else if (p.getFechaNacimiento().isBefore(hoy.minusYears(EDAD_MAXIMA))) {
                errores.put("fechaNacimiento", "Revisa el año: la edad no puede pasar de " + EDAD_MAXIMA + " años.");
            }
        }
        // RF-01: al menos un dato de contacto para confirmar y recordar las citas
        if (p.getCorreo().isEmpty() && p.getCelular().isEmpty()) {
            errores.put("celular", "Registra al menos un celular o un correo para avisarle de sus citas.");
        }
        if (!p.getCorreo().isEmpty() && !Validaciones.correo(p.getCorreo())) errores.put("correo", "Escribe un correo válido.");
        if (!p.getCelular().isEmpty() && !Validaciones.celular(p.getCelular())) {
            errores.put("celular", "El celular debe tener 9 números y empezar con 9.");
        }
        validarCobertura(p, errores);
        if (p.getObservaciones().length() > MAX_OBSERVACIONES) {
            errores.put("observaciones", "Máximo " + MAX_OBSERVACIONES + " caracteres.");
        }
        return errores;
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
