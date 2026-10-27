package modelo.servicios;

import modelo.dto.ContactoDTO;
import modelo.dto.FotoDTO;
import modelo.dto.PerfilDTO;
import modelo.entidades.Usuario;
import modelo.dto.OpcionDTO;
import modelo.repositorio.CatalogoRepository;
import modelo.repositorio.CatalogoRepositoryImpl;
import modelo.repositorio.PerfilRepository;
import modelo.repositorio.PerfilRepositoryImpl;
import modelo.repositorio.UsuarioRepository;
import modelo.repositorio.UsuarioRepositoryImpl;
import seguridad.EncriptadorPassword;
import seguridad.PoliticaPassword;

import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Lógica de "Mi perfil": cada usuario actualiza SUS propios datos, foto,
 * descripción y contraseña. Valida en el servidor y delega a los Repository.
 */
public class PerfilService {

    private static final Pattern NOMBRE = Pattern.compile("^[A-Za-zÁÉÍÓÚÜÑáéíóúüñ' ]{2,100}$");
    private static final Pattern DNI = Pattern.compile("^\\d{8}$");
    private static final Pattern CORREO = Pattern.compile("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)*\\.[A-Za-z]{2,}$");
    private static final Pattern CELULAR = Pattern.compile("^9\\d{8}$");
    private static final int MAX_DESCRIPCION = 250;
    private static final int MAX_BYTES_FOTO = 600_000;

    private final PerfilRepository perfilRepository;
    private final UsuarioRepository usuarioRepository;
    private final CatalogoRepository catalogoRepository;

    public PerfilService() {
        this(new PerfilRepositoryImpl(), new UsuarioRepositoryImpl(), new CatalogoRepositoryImpl());
    }

    public PerfilService(PerfilRepository perfilRepository, UsuarioRepository usuarioRepository,
                         CatalogoRepository catalogoRepository) {
        this.perfilRepository = perfilRepository;
        this.usuarioRepository = usuarioRepository;
        this.catalogoRepository = catalogoRepository;
    }

    /** Opciones de la lista "Sexo" del formulario. */
    public List<OpcionDTO> sexos() {
        return catalogoRepository.listarSexos();
    }

    public Optional<PerfilDTO> obtener(int idUsuario) {
        return perfilRepository.obtener(idUsuario);
    }

    public Optional<FotoDTO> obtenerFoto(int idUsuario) {
        return perfilRepository.obtenerFoto(idUsuario);
    }

    // --------------------------------------------------------------- datos
    /** @return errores por campo; vacío = guardado. */
    public Map<String, String> actualizarDatos(Usuario sesion, PerfilDTO form) {
        PerfilDTO actual = obtener(sesion.getIdUsuario()).orElseThrow();
        Map<String, String> errores = new LinkedHashMap<>();
        normalizar(form, actual, sesion);
        validarFormato(form, errores);
        validarDuplicados(form, actual, sesion, errores);
        if (!errores.isEmpty()) {
            return errores;
        }
        perfilRepository.actualizarDatos(form, sesion.getNombreUsuario());
        boolean cambioCorreo = !actual.getCorreo().equalsIgnoreCase(form.getCorreo());
        evento.EventoSubject.getInstancia().publicar(evento.TipoEvento.PERFIL_EDITADO, sesion, sesion.getIdUsuario(),
                cambioCorreo ? "Cambió su correo" : null);
        if (cambioCorreo && !actual.getCorreo().isBlank()) {
            new NotificacionService().avisarCambioCorreo(new ContactoDTO(actual.getNombres(), actual.getCorreo()), form.getCorreo());
        }
        return errores;
    }

    /** El DNI identifica a la persona: solo se escribe la primera vez (después lo cambia el administrador). */
    private static void normalizar(PerfilDTO form, PerfilDTO actual, Usuario sesion) {
        form.setIdUsuario(sesion.getIdUsuario());
        form.setNombres(form.getNombres().trim().replaceAll("\\s+", " "));
        form.setApellidos(form.getApellidos().trim().replaceAll("\\s+", " "));
        form.setCorreo(form.getCorreo().trim().toLowerCase());
        form.setCelular(form.getCelular().replaceAll("\\s", ""));
        form.setDescripcion(form.getDescripcion().trim());
        form.setDni(actual.isTieneDatosPersonales() ? actual.getDni() : form.getDni().trim());
    }

    private static void validarFormato(PerfilDTO form, Map<String, String> errores) {
        if (!NOMBRE.matcher(form.getNombres()).matches()) errores.put("nombres", "Escribe tus nombres solo con letras (mínimo 2).");
        if (!NOMBRE.matcher(form.getApellidos()).matches()) errores.put("apellidos", "Escribe tus apellidos solo con letras (mínimo 2).");
        if (!DNI.matcher(form.getDni()).matches()) errores.put("dni", "El DNI debe tener exactamente 8 números.");
        if (form.getCorreo().length() > 100 || !CORREO.matcher(form.getCorreo()).matches()) {
            errores.put("correo", "Escribe un correo válido. Lo usamos para recuperar tu contraseña.");
        }
        if (!form.getCelular().isEmpty() && !CELULAR.matcher(form.getCelular()).matches()) {
            errores.put("celular", "El celular debe tener 9 números y empezar con 9.");
        }
        String errorFecha = Validaciones.fechaNacimiento(form.getFechaNacimiento());
        if (errorFecha != null) errores.put("fechaNacimiento", errorFecha);
        if (form.getDescripcion().length() > MAX_DESCRIPCION) {
            errores.put("descripcion", "Máximo " + MAX_DESCRIPCION + " caracteres.");
        }
    }

    private void validarDuplicados(PerfilDTO form, PerfilDTO actual, Usuario sesion, Map<String, String> errores) {
        if (!errores.containsKey("correo") && usuarioRepository.existeCorreoExcepto(form.getCorreo(), sesion.getIdUsuario())) {
            errores.put("correo", "Ese correo ya está registrado en otra cuenta.");
        }
        if (!actual.isTieneDatosPersonales() && !errores.containsKey("dni")
                && usuarioRepository.existeDniExcepto(form.getDni(), sesion.getIdUsuario())) {
            errores.put("dni", "Ese DNI ya está registrado en otra cuenta.");
        }
    }

    // ---------------------------------------------------------- contraseña
    public Map<String, String> cambiarContrasena(Usuario sesion, String actual, String nueva, String confirmacion, String urlBase) {
        Map<String, String> errores = new LinkedHashMap<>();
        Usuario guardado = usuarioRepository.buscarPorNombreUsuario(sesion.getNombreUsuario());

        if (guardado == null || actual == null || !EncriptadorPassword.verificar(actual, guardado.getContrasena())) {
            errores.put("actual", "La contraseña actual no es correcta.");
        }
        String error = PoliticaPassword.validar(nueva);
        if (error != null) {
            errores.put("nueva", error);
        } else if (!PoliticaPassword.coinciden(nueva, confirmacion)) {
            errores.put("confirmar", "Las contraseñas no coinciden.");
        } else if (nueva.equals(actual)) {
            errores.put("nueva", "La nueva contraseña debe ser distinta de la actual.");
        }
        if (!errores.isEmpty()) {
            return errores;
        }
        usuarioRepository.actualizarContrasena(sesion.getIdUsuario(), EncriptadorPassword.encriptar(nueva));
        evento.EventoSubject.getInstancia().publicar(evento.TipoEvento.CONTRASENA_CAMBIADA, sesion, sesion.getIdUsuario(),
                "Desde Mi perfil");
        new RecuperacionService().notificarCambioContrasena(sesion.getIdUsuario(), urlBase);
        return errores;
    }

    // ---------------------------------------------------------------- foto
    /**
     * Recibe la foto como "data URL" (data:image/jpeg;base64,....), ya recortada
     * y reducida a 300x300 en el navegador.
     * @return null si se guardó; si no, el motivo.
     */
    public String guardarFoto(Usuario sesion, String dataUrl) {
        if (dataUrl == null || !dataUrl.startsWith("data:image/") || !dataUrl.contains(";base64,")) {
            return "Selecciona una imagen JPG, PNG o WEBP.";
        }
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(dataUrl.substring(dataUrl.indexOf(',') + 1));
        } catch (IllegalArgumentException e) {
            return "La imagen está dañada. Intenta con otra.";
        }
        if (bytes.length > MAX_BYTES_FOTO) {
            return "La imagen es demasiado pesada. Intenta con otra.";
        }
        // No se confía en lo que dice el navegador: se revisa el contenido real del archivo
        String tipo = detectarTipo(bytes);
        if (tipo == null) {
            return "El archivo no es una imagen JPG, PNG o WEBP.";
        }
        if (!perfilRepository.guardarFoto(sesion.getIdUsuario(), bytes, tipo, sesion.getNombreUsuario())) {
            return "Primero completa y guarda tus datos personales.";
        }
        return null;
    }

    /** Guarda la apariencia elegida por el usuario. @return false si el valor no es válido. */
    public boolean cambiarTema(Usuario sesion, String tema) {
        if (!"claro".equals(tema) && !"oscuro".equals(tema) && !"sistema".equals(tema)) {
            return false;
        }
        perfilRepository.guardarTema(sesion.getIdUsuario(), tema);
        return true;
    }

    public void eliminarFoto(Usuario sesion) {
        perfilRepository.eliminarFoto(sesion.getIdUsuario());
    }

    /** Identifica la imagen por sus primeros bytes ("firma" del formato). */
    static String detectarTipo(byte[] b) {
        if (coincide(b, 0, 0xFF, 0xD8, 0xFF)) return "image/jpeg";
        if (b.length > 8 && coincide(b, 0, 0x89, 'P', 'N', 'G')) return "image/png";
        if (coincide(b, 0, 'R', 'I', 'F', 'F') && coincide(b, 8, 'W', 'E', 'B', 'P')) return "image/webp";
        return null;
    }

    private static boolean coincide(byte[] b, int desde, int... firma) {
        if (b.length <= desde + firma.length) {
            return false;
        }
        for (int i = 0; i < firma.length; i++) {
            if ((b[desde + i] & 0xFF) != firma[i]) {
                return false;
            }
        }
        return true;
    }
}
