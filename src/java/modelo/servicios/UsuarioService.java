package modelo.servicios;

import evento.EventoSubject;
import evento.TipoEvento;

import modelo.dto.EstadisticasUsuariosDTO;
import modelo.dto.FiltroUsuarioDTO;
import modelo.dto.PaginaDTO;
import modelo.dto.UsuarioDTO;
import modelo.entidades.Rol;
import modelo.entidades.Usuario;
import modelo.repositorio.RolRepository;
import modelo.repositorio.RolRepositoryImpl;
import modelo.repositorio.UsuarioRepository;
import modelo.repositorio.UsuarioRepositoryImpl;
import seguridad.EncriptadorPassword;
import seguridad.PoliticaPassword;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Lógica de negocio del CRUD de usuarios (panel del Administrador).
 *
 * El controlador solo recibe y muestra datos; aquí se decide si una
 * operación es válida. Toda la persistencia se delega al Repository
 * (UsuarioRepository), sin escribir SQL en esta clase.
 */
public class UsuarioService {

    private static final Pattern NOMBRE_USUARIO = Pattern.compile("^[A-Za-z0-9._-]{4,30}$");
    private static final Pattern NOMBRE = Pattern.compile("^[A-Za-zÁÉÍÓÚÜÑáéíóúüñ' ]{2,100}$");
    private static final Pattern DNI = Pattern.compile("^\\d{8}$");
    private static final Pattern CORREO = Pattern.compile("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)*\\.[A-Za-z]{2,}$");
    private static final Pattern CELULAR = Pattern.compile("^9\\d{8}$");

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;

    public UsuarioService() {
        this(new UsuarioRepositoryImpl(), new RolRepositoryImpl());
    }

    public UsuarioService(UsuarioRepository usuarioRepository, RolRepository rolRepository) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
    }

    // ------------------------------------------------------------- lectura
    public PaginaDTO<UsuarioDTO> listar(FiltroUsuarioDTO filtro) {
        return usuarioRepository.listar(filtro);
    }

    public Optional<UsuarioDTO> obtener(int idUsuario) {
        return usuarioRepository.buscarPorId(idUsuario);
    }

    public List<Rol> listarRoles() {
        return rolRepository.listarActivos();
    }

    public EstadisticasUsuariosDTO estadisticas() {
        return usuarioRepository.obtenerEstadisticas();
    }

    // ------------------------------------------------------------- guardar
    /**
     * Crea (si idUsuario = 0) o actualiza un usuario.
     * @return errores por campo; vacío = guardado correctamente.
     */
    public Map<String, String> guardar(UsuarioDTO u, String password, String confirmacion, Usuario admin) {
        normalizar(u);
        Map<String, String> errores = new LinkedHashMap<>();

        // --- formato ---
        if (!nombreValido(u.getNombres())) errores.put("nombres", "Escribe los nombres solo con letras (mínimo 2).");
        if (!nombreValido(u.getApellidos())) errores.put("apellidos", "Escribe los apellidos solo con letras (mínimo 2).");
        if (!DNI.matcher(u.getDni()).matches()) errores.put("dni", "El DNI debe tener exactamente 8 números.");
        if (u.getCorreo().length() > 100 || !CORREO.matcher(u.getCorreo()).matches()) {
            errores.put("correo", "Escribe un correo válido. Se usa para recuperar la contraseña.");
        }
        if (!u.getCelular().isEmpty() && !CELULAR.matcher(u.getCelular()).matches()) {
            errores.put("celular", "El celular debe tener 9 números y empezar con 9 (o déjalo vacío).");
        }
        if (!NOMBRE_USUARIO.matcher(u.getNombreUsuario()).matches()) {
            errores.put("nombreUsuario", "Entre 4 y 30 caracteres: letras, números, punto, guion o guion bajo. Sin espacios.");
        }
        if (listarRoles().stream().noneMatch(r -> r.getIdRol() == u.getIdRol())) {
            errores.put("idRol", "Selecciona un rol válido.");
        }
        if (!"Activo".equals(u.getEstado()) && !"Inactivo".equals(u.getEstado())) {
            errores.put("estado", "Selecciona un estado válido.");
        }

        // --- contraseña: obligatoria al crear, opcional al editar ---
        boolean cambiaPassword = u.isNuevo() || (password != null && !password.isEmpty());
        if (cambiaPassword) {
            String error = PoliticaPassword.validar(password);
            if (error != null) {
                errores.put("password", error);
            } else if (!PoliticaPassword.coinciden(password, confirmacion)) {
                errores.put("confirmarPassword", "Las contraseñas no coinciden.");
            }
        }

        // --- reglas de negocio: el administrador no puede quitarse acceso a sí mismo ---
        if (!u.isNuevo() && u.getIdUsuario() == admin.getIdUsuario()) {
            if (u.getIdRol() != Rol.ADMINISTRADOR) {
                errores.put("idRol", "No puedes quitarte el rol de Administrador a ti mismo.");
            }
            if (!"Activo".equals(u.getEstado())) {
                errores.put("estado", "No puedes desactivar tu propia cuenta.");
            }
        }

        // --- duplicados (solo si el formato ya es correcto) ---
        if (!errores.containsKey("nombreUsuario")
                && usuarioRepository.existeNombreUsuarioExcepto(u.getNombreUsuario(), u.getIdUsuario())) {
            errores.put("nombreUsuario", "Ese nombre de usuario ya está en uso.");
        }
        if (!errores.containsKey("dni") && usuarioRepository.existeDniExcepto(u.getDni(), u.getIdUsuario())) {
            errores.put("dni", "Ya existe otra persona registrada con ese DNI.");
        }
        if (!errores.containsKey("correo") && usuarioRepository.existeCorreoExcepto(u.getCorreo(), u.getIdUsuario())) {
            errores.put("correo", "Ese correo ya pertenece a otro usuario.");
        }
        if (!errores.isEmpty()) {
            return errores;
        }

        u.setUsuarioRegistro(admin.getNombreUsuario());
        u.setContrasenaHash(cambiaPassword ? EncriptadorPassword.encriptar(password) : null);

        String rol = listarRoles().stream().filter(r -> r.getIdRol() == u.getIdRol()).map(Rol::getNombre).findFirst().orElse("");
        if (u.isNuevo()) {
            u.setIdUsuario(usuarioRepository.insertar(u));
            EventoSubject.getInstancia().publicar(TipoEvento.USUARIO_CREADO, admin, u.getIdUsuario(),
                    "@" + u.getNombreUsuario() + " · " + rol);
            return errores;
        }
        Optional<UsuarioDTO> antes = usuarioRepository.buscarPorId(u.getIdUsuario());
        if (!usuarioRepository.actualizar(u)) {
            errores.put("general", "El usuario ya no existe. Actualiza la lista e inténtalo de nuevo.");
            return errores;
        }
        StringBuilder cambios = new StringBuilder("@" + u.getNombreUsuario());
        antes.ifPresent(a -> {
            if (a.getIdRol() != u.getIdRol()) cambios.append(" · rol: ").append(a.getNombreRol()).append(" → ").append(rol);
            if (!a.getEstado().equals(u.getEstado())) cambios.append(" · estado: ").append(a.getEstado()).append(" → ").append(u.getEstado());
            if (!a.getDni().equals(u.getDni())) cambios.append(" · DNI: ").append(a.getDni()).append(" → ").append(u.getDni());
        });
        if (cambiaPassword) cambios.append(" · contraseña restablecida");
        EventoSubject.getInstancia().publicar(TipoEvento.USUARIO_EDITADO, admin, u.getIdUsuario(), cambios.toString());
        return errores;
    }

    // ------------------------------------------------------ acciones rápidas
    /** @return null si se pudo; si no, el motivo para mostrarlo al usuario. */
    public String desactivar(int idUsuario, Usuario admin) {
        if (idUsuario == admin.getIdUsuario()) {
            return "No puedes desactivar tu propia cuenta.";
        }
        if (!usuarioRepository.eliminar(idUsuario)) {
            return "El usuario no existe.";
        }
        EventoSubject.getInstancia().publicar(TipoEvento.USUARIO_DESACTIVADO, admin, idUsuario, etiqueta(idUsuario));
        return null;
    }

    public String reactivar(int idUsuario, Usuario admin) {
        if (!usuarioRepository.reactivar(idUsuario)) {
            return "El usuario no existe.";
        }
        EventoSubject.getInstancia().publicar(TipoEvento.USUARIO_REACTIVADO, admin, idUsuario, etiqueta(idUsuario));
        return null;
    }

    public String desbloquear(int idUsuario, Usuario admin) {
        usuarioRepository.desbloquearUsuario(idUsuario);
        EventoSubject.getInstancia().publicar(TipoEvento.USUARIO_DESBLOQUEADO, admin, idUsuario, etiqueta(idUsuario));
        return null;
    }

    public String bloquear(int idUsuario, Usuario admin) {
        if (idUsuario == admin.getIdUsuario()) {
            return "No puedes bloquear tu propia cuenta.";
        }
        usuarioRepository.bloquearPorAdministrador(idUsuario);
        EventoSubject.getInstancia().publicar(TipoEvento.USUARIO_BLOQUEADO, admin, idUsuario, etiqueta(idUsuario));
        return null;
    }

    /** "@usuario" para el detalle de la auditoría. */
    private String etiqueta(int idUsuario) {
        return usuarioRepository.buscarPorId(idUsuario).map(u -> "@" + u.getNombreUsuario()).orElse("#" + idUsuario);
    }

    // ------------------------------------------------------------ auxiliares
    private static boolean nombreValido(String valor) {
        return NOMBRE.matcher(valor).matches();
    }

    private static void normalizar(UsuarioDTO u) {
        u.setNombreUsuario(u.getNombreUsuario().trim());
        u.setNombres(u.getNombres().trim().replaceAll("\\s+", " "));
        u.setApellidos(u.getApellidos().trim().replaceAll("\\s+", " "));
        u.setDni(u.getDni().trim());
        u.setCorreo(u.getCorreo().trim().toLowerCase());
        u.setCelular(u.getCelular().replaceAll("\\s", ""));
    }
}
