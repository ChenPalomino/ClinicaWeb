package seguridad;

import java.util.regex.Pattern;

/**
 * Regla única de contraseñas del sistema. La usan el registro y la
 * recuperación de contraseña, así ambos piden exactamente lo mismo.
 */
public final class PoliticaPassword {

    private static final Pattern REGLA = Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,64}$");

    public static final String MENSAJE =
            "La contraseña necesita mínimo 8 caracteres, una mayúscula, una minúscula y un número.";

    private PoliticaPassword() {
    }

    /** Devuelve null si es válida, o el mensaje de error si no lo es. */
    public static String validar(String password) {
        if (password == null || !REGLA.matcher(password).matches()) {
            return MENSAJE;
        }
        return null;
    }

    public static boolean coinciden(String password, String confirmacion) {
        return password != null && password.equals(confirmacion);
    }
}
