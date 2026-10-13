package notificacion;

/**
 * PATRÓN: Adapter  ->  INTERFAZ OBJETIVO ("Target")
 *
 * Es la forma en que NUESTRO sistema quiere enviar correos: un solo método
 * simple. Cada proveedor de correo (Brevo, Gmail, consola de pruebas...)
 * tiene su propia API distinta; un Adapter traduce esta llamada a la API
 * concreta. Así, cambiar de proveedor no obliga a tocar la lógica de negocio.
 */
public interface ServicioCorreo {

    /**
     * Envía un correo HTML.
     * @return true si el proveedor aceptó el correo.
     */
    boolean enviar(String destinatario, String nombreDestinatario, String asunto, String html);

    /**
     * Igual que enviar(), pero con el detalle de lo que respondió el proveedor
     * (para la página de diagnóstico). Los adaptadores sencillos usan esta versión.
     */
    default ResultadoEnvio enviarDetallado(String destinatario, String nombreDestinatario, String asunto, String html) {
        boolean ok = enviar(destinatario, nombreDestinatario, asunto, html);
        return new ResultadoEnvio(ok, 0, ok ? "Enviado." : "No se pudo enviar.");
    }
}
