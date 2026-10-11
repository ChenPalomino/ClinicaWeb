package seguridad;

import modelo.entidades.Usuario;

/**
 * PATRÓN: Proxy de protección (estructural)
 * Reemplaza a la antigua clase ProxyAcceso.
 *
 * Propósito: se coloca DELANTE del AutenticacionFacade y controla el acceso
 * a él. Antes de dejar pasar la petición:
 *   - Rechaza datos vacíos o demasiado largos (no llegan a la BD).
 *   - Normaliza el usuario (quita espacios).
 *   - Registra el intento (más adelante lo hará AuditoriaLogger en la BD).
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
            return usuario;
        } catch (AutenticacionException e) {
            System.out.println("AutenticacionProxy: acceso denegado a " + usuarioNormalizado + " -> " + e.getMotivo());
            throw e;
        }
    }
}
