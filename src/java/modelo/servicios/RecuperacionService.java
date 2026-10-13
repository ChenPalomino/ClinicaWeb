package modelo.servicios;

import modelo.dto.ContactoDTO;
import modelo.dto.TokenRecuperacionDTO;
import modelo.entidades.Usuario;
import modelo.repositorio.UsuarioRepository;
import modelo.repositorio.UsuarioRepositoryImpl;
import seguridad.EncriptadorPassword;
import seguridad.PoliticaPassword;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Lógica de "¿Olvidaste tu contraseña?" y desbloqueo con código al correo.
 *
 * Flujo:
 *  1. solicitarCodigo(): genera un código de 6 dígitos, guarda su HASH
 *     (nunca el código) en tt_reseteo_contrasena con 15 min de vigencia
 *     y lo envía al correo.
 *  2. verificarCodigo(): máximo 5 intentos por código; si es correcto se
 *     invalida (un solo uso).
 *  3. cambiarContrasena(): guarda la nueva contraseña encriptada, desbloquea
 *     la cuenta y envía un aviso con el enlace "No fui yo".
 *  4. reportarCambio(): si el dueño pulsa "No fui yo", la cuenta queda
 *     bloqueada por seguridad hasta que la desbloquee el administrador.
 *
 * Seguridad: si el usuario no existe o no tiene correo, la respuesta es la
 * misma que si existiera, para no revelar qué cuentas existen.
 */
public class RecuperacionService {

    public static final int MINUTOS_CODIGO = 15;
    public static final int MAX_INTENTOS_CODIGO = 5;
    public static final int SEGUNDOS_ENTRE_ENVIOS = 60;
    private static final int MINUTOS_ENLACE_REPORTE = 72 * 60;
    private static final SecureRandom RANDOM = new SecureRandom();

    public enum Solicitud { ENVIADO, ESPERAR, ERROR_ENVIO }

    public enum Verificacion { VALIDO, INCORRECTO, EXPIRADO, SIN_INTENTOS }

    /** Resultado de verificar un código. */
    public static class ResultadoVerificacion {
        private final Verificacion estado;
        private final int idUsuario;
        private final int intentosRestantes;

        ResultadoVerificacion(Verificacion estado, int idUsuario, int intentosRestantes) {
            this.estado = estado;
            this.idUsuario = idUsuario;
            this.intentosRestantes = intentosRestantes;
        }

        public Verificacion getEstado() { return estado; }
        public int getIdUsuario() { return idUsuario; }
        public int getIntentosRestantes() { return intentosRestantes; }
    }

    private final UsuarioRepository usuarioRepository;
    private final NotificacionService notificacionService;

    public RecuperacionService() {
        this(new UsuarioRepositoryImpl(), new NotificacionService());
    }

    public RecuperacionService(UsuarioRepository usuarioRepository, NotificacionService notificacionService) {
        this.usuarioRepository = usuarioRepository;
        this.notificacionService = notificacionService;
    }

    // ------------------------------------------------------------ paso 1
    public Solicitud solicitarCodigo(String usuarioODni) {
        Usuario usuario = usuarioRepository.buscarPorNombreUsuario(usuarioODni);
        if (usuario == null || bloqueadoPorSeguridad(usuario)) {
            return Solicitud.ENVIADO;          // respuesta genérica
        }
        ContactoDTO contacto = usuarioRepository.buscarContacto(usuario.getIdUsuario());
        if (contacto == null) {
            return Solicitud.ENVIADO;          // respuesta genérica
        }

        TokenRecuperacionDTO actual = usuarioRepository.obtenerToken(usuario.getIdUsuario(), TokenRecuperacionDTO.TIPO_CODIGO);
        if (actual != null && actual.isVigente() && actual.getSegundosDesdeCreacion() < SEGUNDOS_ENTRE_ENVIOS) {
            return Solicitud.ESPERAR;          // evita enviar muchos correos seguidos
        }

        String codigo = String.format("%06d", RANDOM.nextInt(1_000_000));
        usuarioRepository.guardarToken(usuario.getIdUsuario(), EncriptadorPassword.encriptar(codigo),
                TokenRecuperacionDTO.TIPO_CODIGO, MINUTOS_CODIGO);

        if (!notificacionService.enviarCodigo(contacto, codigo, MINUTOS_CODIGO)) {
            TokenRecuperacionDTO fallido = usuarioRepository.obtenerToken(usuario.getIdUsuario(), TokenRecuperacionDTO.TIPO_CODIGO);
            if (fallido != null) {
                usuarioRepository.marcarTokenUsado(fallido.getIdReseteo());
            }
            return Solicitud.ERROR_ENVIO;
        }
        return Solicitud.ENVIADO;
    }

    // ------------------------------------------------------------ paso 2
    public ResultadoVerificacion verificarCodigo(String usuarioODni, String codigo) {
        Usuario usuario = usuarioRepository.buscarPorNombreUsuario(usuarioODni);
        if (usuario == null || bloqueadoPorSeguridad(usuario)) {
            return new ResultadoVerificacion(Verificacion.INCORRECTO, 0, -1);
        }
        int id = usuario.getIdUsuario();
        TokenRecuperacionDTO token = usuarioRepository.obtenerToken(id, TokenRecuperacionDTO.TIPO_CODIGO);
        if (token == null || !token.isVigente()) {
            return new ResultadoVerificacion(Verificacion.EXPIRADO, 0, 0);
        }
        if (token.getIntentos() >= MAX_INTENTOS_CODIGO) {
            return new ResultadoVerificacion(Verificacion.SIN_INTENTOS, 0, 0);
        }

        String limpio = codigo == null ? "" : codigo.replaceAll("\\D", "");
        if (limpio.length() == 6 && EncriptadorPassword.verificar(limpio, token.getHash())) {
            usuarioRepository.marcarTokenUsado(token.getIdReseteo());    // un solo uso
            return new ResultadoVerificacion(Verificacion.VALIDO, id, 0);
        }

        usuarioRepository.incrementarIntentosToken(token.getIdReseteo());
        int restantes = MAX_INTENTOS_CODIGO - (token.getIntentos() + 1);
        return restantes > 0
                ? new ResultadoVerificacion(Verificacion.INCORRECTO, 0, restantes)
                : new ResultadoVerificacion(Verificacion.SIN_INTENTOS, 0, 0);
    }

    // ------------------------------------------------------------ paso 3
    /** @return errores por campo; vacío si se cambió correctamente. */
    public Map<String, String> cambiarContrasena(int idUsuario, String nueva, String confirmacion, String urlBase) {
        Map<String, String> errores = new LinkedHashMap<>();
        String error = PoliticaPassword.validar(nueva);
        if (error != null) {
            errores.put("password", error);
        } else if (!PoliticaPassword.coinciden(nueva, confirmacion)) {
            errores.put("confirmarPassword", "Las contraseñas no coinciden.");
        }
        if (!errores.isEmpty()) {
            return errores;
        }

        usuarioRepository.cambiarContrasenaYDesbloquear(idUsuario, EncriptadorPassword.encriptar(nueva));

        notificarCambioContrasena(idUsuario, urlBase);
        return errores;
    }

    /**
     * Envía el aviso "Tu contraseña fue cambiada" con el enlace "No fui yo".
     * Lo usan la recuperación por código y el cambio desde "Mi perfil".
     */
    public void notificarCambioContrasena(int idUsuario, String urlBase) {
        ContactoDTO contacto = usuarioRepository.buscarContacto(idUsuario);
        if (contacto == null) {
            return;
        }
        // Token aleatorio largo; en la BD se guarda solo su hash
        byte[] bytes = new byte[24];
        RANDOM.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        usuarioRepository.guardarToken(idUsuario, EncriptadorPassword.encriptar(token),
                TokenRecuperacionDTO.TIPO_ALERTA, MINUTOS_ENLACE_REPORTE);
        String enlace = urlBase + "/RecuperacionController?accion=reportar&u=" + idUsuario + "&t=" + token;
        notificacionService.avisarCambioContrasena(contacto, enlace);
    }

    // ------------------------------------------------------- "No fui yo"
    public boolean reportarCambio(int idUsuario, String token) {
        TokenRecuperacionDTO guardado = usuarioRepository.obtenerToken(idUsuario, TokenRecuperacionDTO.TIPO_ALERTA);
        if (guardado == null || !guardado.isVigente() || token == null
                || !EncriptadorPassword.verificar(token, guardado.getHash())) {
            return false;
        }
        usuarioRepository.bloquearPorSeguridad(idUsuario);
        return true;
    }

    // ---------------------------------------------- aviso de bloqueo
    public void avisarBloqueo(int idUsuario, String urlBase) {
        ContactoDTO contacto = usuarioRepository.buscarContacto(idUsuario);
        if (contacto != null) {
            notificacionService.avisarBloqueo(contacto, urlBase + "/jsp/recuperar.jsp");
        }
    }

    /** Bloqueo por "No fui yo" o del administrador: no se puede liberar con código. */
    private static boolean bloqueadoPorSeguridad(Usuario usuario) {
        return usuario.isBloqueado() && !usuario.isBloqueoPorIntentos();
    }
}
