package seguridad;

import evento.EventoSistema;
import evento.EventoSubject;
import evento.TipoEvento;

import modelo.entidades.Usuario;

/**
 * PATRÓN: Proxy de protección (estructural)
 * Reemplaza a la antigua clase ProxyAcceso.
 *
 * Propósito: se coloca DELANTE del AutenticacionFacade y controla el acceso
 * a él. Antes de dejar pasar la petición:
 *   - Rechaza datos vacíos o demasiado largos (no llegan a la BD).
 *   - Normaliza el usuario (quita espacios).
 *   - Registra el intento: publica el evento y AuditoriaObserver lo guarda en tt_auditoria.
 * El controlador usa el Proxy igual que usaría el Facade: mismo método.
 */
public class AutenticacionProxy {

    private static final int LONGITUD_MAXIMA = 100;

    private final AutenticacionFacade autenticacionFacade;

    public AutenticacionProxy() {
        this(new AutenticacionFacade());
    }

    public AutenticacionProxy(AutenticacionFacade autenticacionFacade) {
        this.autenticacionFacade = autenticacionFacade;
    }

    public Usuario intentarAcceso(String nombreUsuario, String password) throws AutenticacionException {
        if (nombreUsuario == null || password == null
                || nombreUsuario.isBlank() || password.isEmpty()
                || nombreUsuario.length() > LONGITUD_MAXIMA || password.length() > LONGITUD_MAXIMA) {
            throw new AutenticacionException(AutenticacionException.Motivo.DATOS_INCOMPLETOS);
        }

        String usuarioNormalizado = nombreUsuario.trim();
        System.out.println("AutenticacionProxy: intento de ingreso para el usuario: " + usuarioNormalizado);

        try {
            Usuario usuario = autenticacionFacade.autenticar(usuarioNormalizado, password);
            System.out.println("AutenticacionProxy: acceso concedido a " + usuarioNormalizado);
            EventoSubject.getInstancia().publicar(TipoEvento.INICIO_SESION, usuario, usuario.getIdUsuario(), null);
            return usuario;
        } catch (AutenticacionException e) {
            System.out.println("AutenticacionProxy: acceso denegado a " + usuarioNormalizado + " -> " + e.getMotivo());
            registrarIntentoFallido(usuarioNormalizado, e);
            throw e;
        }
    }

    /** Auditoría de los intentos fallidos sobre cuentas que existen (RNF-10). */
    private static void registrarIntentoFallido(String usuario, AutenticacionException e) {
        boolean contrasenaMala = e.getMotivo() == AutenticacionException.Motivo.CREDENCIALES_INVALIDAS || e.isRecienBloqueado();
        if (e.getIdUsuario() <= 0 || !contrasenaMala) {
            return;     // usuario inexistente o cuenta ya bloqueada: no se guarda (evita llenar la auditoría)
        }
        TipoEvento tipo = e.isRecienBloqueado() ? TipoEvento.CUENTA_BLOQUEADA_INTENTOS : TipoEvento.INICIO_FALLIDO;
        EventoSubject.getInstancia().publicar(new EventoSistema(tipo, e.getIdUsuario(), usuario, e.getIdUsuario(),
                e.isRecienBloqueado() ? "3 contraseñas incorrectas seguidas" : "Intento " + e.getIntentos() + " de 3"));
    }
}
