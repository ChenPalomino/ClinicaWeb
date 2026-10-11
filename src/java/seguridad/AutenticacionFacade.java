package seguridad;

import modelo.entidades.Usuario;
import modelo.repositorio.UsuarioRepositoryImpl;
import modelo.repositorio.UsuarioRepository;
import seguridad.AutenticacionException.Motivo;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * PATRÓN: Facade (estructural)
 * Propósito: ofrecer UN solo método simple, autenticar(), que esconde todos
 * los pasos internos del inicio de sesión:
 *   1. Buscar al usuario (Repository).
 *   2. Revisar si está bloqueado y si ya pasaron las 24 h para desbloquearlo.
 *   3. Verificar la contraseña encriptada (EncriptadorPassword).
 *   4. Contar intentos fallidos y bloquear al llegar a 3.
 *   5. Encriptar contraseñas antiguas que aún estén en texto plano.
 * El controlador no conoce ninguno de estos pasos.
 */
public class AutenticacionFacade {

    public static final int MAX_INTENTOS = 3;
    public static final Duration TIEMPO_BLOQUEO = Duration.ofHours(24);

    private final UsuarioRepository usuarioRepository;

    public AutenticacionFacade() {
        this(new UsuarioRepositoryImpl());
    }

    /** Permite inyectar un repositorio falso en las pruebas unitarias. */
    public AutenticacionFacade(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    public Usuario autenticar(String nombreUsuario, String password) throws AutenticacionException {
        // 1. Buscar
        Usuario usuario = usuarioRepository.buscarPorNombreUsuario(nombreUsuario);
        if (usuario == null) {
            throw new AutenticacionException(Motivo.NO_EXISTE);
        }

        int intentosPrevios = usuario.getIntentosFallidos();

        // 2. ¿Bloqueado?
        if (usuario.isBloqueado()) {
            if (!usuario.isBloqueoPorIntentos()) {
                // Bloqueo por seguridad ("No fui yo") o del administrador: solo el administrador lo levanta
                throw new AutenticacionException(Motivo.BLOQUEADO_SEGURIDAD);
            }
            long minutos = minutosParaDesbloqueo(usuario);
            if (minutos > 0) {
                throw new AutenticacionException(Motivo.BLOQUEADO, MAX_INTENTOS, minutos);
            }
            // Ya pasaron las 24 horas: se desbloquea y se le da 3 intentos nuevos
            usuarioRepository.desbloquearUsuario(usuario.getIdUsuario());
            intentosPrevios = 0;
        }

        // 3. Verificar contraseña
        if (EncriptadorPassword.verificar(password, usuario.getContrasena())) {
            // 5. Migración: si la contraseña estaba en texto plano, se encripta ahora
            if (!EncriptadorPassword.esHash(usuario.getContrasena())) {
                usuarioRepository.actualizarContrasena(usuario.getIdUsuario(), EncriptadorPassword.encriptar(password));
            }
            usuarioRepository.resetearIntentosYActualizarAcceso(usuario.getIdUsuario());
            return usuario;
        }

        // 4. Contraseña incorrecta
        int nuevosIntentos = intentosPrevios + 1;
        if (nuevosIntentos >= MAX_INTENTOS) {
            usuarioRepository.bloquearUsuario(usuario.getIdUsuario(), nuevosIntentos);
            throw new AutenticacionException(Motivo.BLOQUEADO, nuevosIntentos, TIEMPO_BLOQUEO.toMinutes(),
                    usuario.getIdUsuario(), true);
        }
        usuarioRepository.actualizarIntentos(usuario.getIdUsuario(), nuevosIntentos);
        throw new AutenticacionException(Motivo.CREDENCIALES_INVALIDAS, nuevosIntentos, 0);
    }

    /** Minutos que faltan para el desbloqueo automático de 24 horas. */
    private long minutosParaDesbloqueo(Usuario usuario) {
        LocalDateTime desbloqueo = usuario.getFechaBloqueo().toLocalDateTime().plus(TIEMPO_BLOQUEO);
        long minutos = Duration.between(LocalDateTime.now(), desbloqueo).toMinutes();
        if (minutos <= 0 && LocalDateTime.now().isBefore(desbloqueo)) {
            return 1;  // faltan segundos: se muestra "1 minuto"
        }
        return Math.max(minutos, 0);
    }
}
