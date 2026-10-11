package notificacion;

/**
 * PATRÓN: Factory (creacional)
 *
 * Decide QUÉ adaptador de correo usar según la configuración, para que ninguna
 * otra clase tenga que saberlo: - Si existen BREVO_API_KEY y CORREO_REMITENTE
 * -> BrevoCorreoAdapter (correos reales) - Si falta alguna ->
 * ConsolaCorreoAdapter (modo desarrollo)
 *
 * Escribe en la consola (Output) qué modo eligió y, si es el de desarrollo, qué
 * variable falta, para diagnosticar fácilmente.
 */
public final class ServicioCorreoFactory {

    private ServicioCorreoFactory() {
    }

    public static ServicioCorreo crear() {
        String apiKey = limpiar(System.getenv("BREVO_API_KEY"));
        String remitente = limpiar(System.getenv("CORREO_REMITENTE"));

        if (apiKey != null && remitente != null) {
            String nombre = limpiar(System.getenv("CORREO_NOMBRE"));
            System.out.println("ServicioCorreoFactory: enviando correos REALES con Brevo desde " + remitente
                    + " (clave " + apiKey.substring(0, Math.min(12, apiKey.length())) + "...)");
            return new BrevoCorreoAdapter(apiKey, remitente, nombre == null ? "Clínica Modelo San Martín" : nombre);
        }

        System.out.println("ServicioCorreoFactory: MODO DESARROLLO (el correo solo se muestra aquí). Falta: "
                + (apiKey == null ? "BREVO_API_KEY " : "")
                + (remitente == null ? "CORREO_REMITENTE" : ""));
        return new ConsolaCorreoAdapter();
    }

    /**
     * Quita espacios y comillas que a veces se cuelan al copiar y pegar.
     */
    private static String limpiar(String valor) {
        if (valor == null) {
            return null;
        }
        String v = valor.trim();
        if (v.length() >= 2 && v.startsWith("\"") && v.endsWith("\"")) {
            v = v.substring(1, v.length() - 1).trim();
        }
        return v.isEmpty() ? null : v;
    }
}
