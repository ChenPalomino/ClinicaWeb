package modelo.servicios;

import modelo.dto.RegistroPacienteDTO;
import modelo.entidades.Usuario;
import modelo.repositorio.UsuarioRepositoryImpl;
import modelo.repositorio.UsuarioRepository;
import seguridad.EncriptadorPassword;
import seguridad.PoliticaPassword;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Servicio (lógica de negocio) del registro de pacientes.
 *
 * Responsabilidades:
 *  1. Validar en el SERVIDOR los datos (la validación JavaScript se puede
 *     saltar, esta no).
 *  2. Verificar duplicados (DNI, correo).
 *  3. Encriptar la contraseña.
 *  4. Construir el Usuario con el patrón Builder (rol Paciente).
 *  5. Pedir al Repository que guarde todo.
 *
 * Devuelve un mapa campo -> mensaje de error. Si está vacío, se registró.
 */
public class RegistroService {

    private static final Pattern DNI = Pattern.compile("^\\d{8}$");
    private static final Pattern NOMBRE = Pattern.compile("^[A-Za-zÁÉÍÓÚÜÑáéíóúüñ' ]{2,80}$");
    private static final Pattern CELULAR = Pattern.compile("^9\\d{8}$");
    private static final Pattern CORREO = Pattern.compile("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)*\\.[A-Za-z]{2,}$");

    private static final int ROL_PACIENTE = 4;
    private static final int PERMISO_PORTAL_PACIENTE = 4;

    private final UsuarioRepository usuarioRepository;

    public RegistroService() {
        this(new UsuarioRepositoryImpl());
    }

    public RegistroService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    public Map<String, String> registrar(RegistroPacienteDTO dto) {
        Map<String, String> errores = validarFormato(dto);
        if (!errores.isEmpty()) {
            return errores;
        }

        // El DNI es también el nombre de usuario del paciente
        if (usuarioRepository.existeDni(dto.getDni()) || usuarioRepository.existeNombreUsuario(dto.getDni())) {
            errores.put("dni", "Este DNI ya tiene una cuenta. Inicia sesión o recupera tu acceso.");
        }
        if (usuarioRepository.existeCorreo(dto.getCorreo(), dto.getDni())) {
            errores.put("correo", "Este correo ya está registrado.");
        }
        if (!errores.isEmpty()) {
            return errores;
        }

        // PATRÓN BUILDER: construcción del usuario paciente
        Usuario nuevoUsuario = new Usuario.UsuarioBuilder(dto.getDni(), EncriptadorPassword.encriptar(dto.getPassword()))
                .rolId(ROL_PACIENTE)
                .permisoId(PERMISO_PORTAL_PACIENTE)
                .intentosFallidos(0)
                .bloqueado(false)
                .build();

        if (!usuarioRepository.registrarPaciente(nuevoUsuario, dto)) {
            errores.put("general", "No se pudo completar el registro. Inténtalo nuevamente.");
        }
        return errores;
    }

    private Map<String, String> validarFormato(RegistroPacienteDTO dto) {
        Map<String, String> errores = new LinkedHashMap<>();
        if (!DNI.matcher(dto.getDni()).matches()) {
            errores.put("dni", "El DNI debe tener exactamente 8 números.");
        }
        if (!NOMBRE.matcher(dto.getNombres()).matches()) {
            errores.put("nombres", "Escribe tus nombres solo con letras (mínimo 2).");
        }
        if (!NOMBRE.matcher(dto.getApellidos()).matches()) {
            errores.put("apellidos", "Escribe tus apellidos solo con letras (mínimo 2).");
        }
        if (!CELULAR.matcher(dto.getCelular()).matches()) {
            errores.put("celular", "El celular debe tener 9 números y empezar con 9.");
        }
        if (dto.getCorreo().length() > 80 || !CORREO.matcher(dto.getCorreo()).matches()) {
            errores.put("correo", "Escribe un correo válido, por ejemplo nombre@gmail.com.");
        }
        String errorPassword = PoliticaPassword.validar(dto.getPassword());
        if (errorPassword != null) {
            errores.put("password", errorPassword);
        } else if (!PoliticaPassword.coinciden(dto.getPassword(), dto.getConfirmarPassword())) {
            errores.put("confirmarPassword", "Las contraseñas no coinciden.");
        }
        return errores;
    }
}
