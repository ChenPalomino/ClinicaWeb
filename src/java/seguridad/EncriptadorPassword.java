package seguridad;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Base64;

/**
 * Encriptación (hash) de contraseñas con PBKDF2-HMAC-SHA256.
 *
 * - Viene incluido en Java: no requiere librerías externas.
 * - Usa una "sal" aleatoria por usuario: dos contraseñas iguales generan
 *   hashes distintos.
 * - Es de un solo sentido: no se puede "desencriptar"; para validar se
 *   vuelve a calcular el hash con la misma sal y se comparan.
 *
 * Formato guardado en tt_usuario.contrasena:
 *     pbkdf2$ITERACIONES$SAL_BASE64$HASH_BASE64
 */
public final class EncriptadorPassword {

    private static final String PREFIJO = "pbkdf2";
    private static final String ALGORITMO = "PBKDF2WithHmacSHA256";
    private static final int ITERACIONES = 210_000;
    private static final int BYTES_SAL = 16;
    private static final int BITS_HASH = 256;
    private static final SecureRandom RANDOM = new SecureRandom();

    private EncriptadorPassword() {
    }

    /** Genera el hash que se guarda en la base de datos. */
    public static String encriptar(String password) {
        byte[] sal = new byte[BYTES_SAL];
        RANDOM.nextBytes(sal);
        byte[] hash = pbkdf2(password.toCharArray(), sal, ITERACIONES);
        Base64.Encoder b64 = Base64.getEncoder();
        return PREFIJO + "$" + ITERACIONES + "$" + b64.encodeToString(sal) + "$" + b64.encodeToString(hash);
    }

    /**
     * Verifica la contraseña escrita contra lo guardado en la BD.
     * Si lo guardado aún está en texto plano (usuarios antiguos), lo compara
     * directamente; el Facade se encarga luego de encriptarlo.
     */
    public static boolean verificar(String password, String guardado) {
        if (password == null || guardado == null) {
            return false;
        }
        if (!esHash(guardado)) {
            return MessageDigest.isEqual(password.getBytes(), guardado.getBytes());
        }
        try {
            String[] partes = guardado.split("\\$");
            int iteraciones = Integer.parseInt(partes[1]);
            byte[] sal = Base64.getDecoder().decode(partes[2]);
            byte[] esperado = Base64.getDecoder().decode(partes[3]);
            byte[] calculado = pbkdf2(password.toCharArray(), sal, iteraciones);
            return MessageDigest.isEqual(esperado, calculado);   // comparación en tiempo constante
        } catch (RuntimeException e) {
            return false;
        }
    }

    /** true si el valor guardado ya es un hash generado por esta clase. */
    public static boolean esHash(String guardado) {
        return guardado != null && guardado.startsWith(PREFIJO + "$") && guardado.split("\\$").length == 4;
    }

    private static byte[] pbkdf2(char[] password, byte[] sal, int iteraciones) {
        try {
            KeySpec spec = new PBEKeySpec(password, sal, iteraciones, BITS_HASH);
            return SecretKeyFactory.getInstance(ALGORITMO).generateSecret(spec).getEncoded();
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo encriptar la contraseña", e);
        }
    }

    /** Permite generar un hash desde NetBeans (clic derecho > Run File). */
    public static void main(String[] args) {
        String clave = args.length > 0 ? args[0] : "Clinica2026*";
        System.out.println(encriptar(clave));
    }
}
