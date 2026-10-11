package notificacion;

/**
 * PATRÓN: Adapter  ->  ADAPTADOR DE DESARROLLO
 *
 * No envía nada: escribe el correo en la consola (pestaña Output de
 * NetBeans / log de Tomcat). Se usa automáticamente cuando no está
 * configurada la variable BREVO_API_KEY, para poder probar el flujo de
 * recuperación en local sin una cuenta de correo.
 */
public class ConsolaCorreoAdapter implements ServicioCorreo {

    @Override
    public boolean enviar(String destinatario, String nombreDestinatario, String asunto, String html) {
        String texto = html.replaceAll("<br\\s*/?>", "\n").replaceAll("</p>", "\n").replaceAll("<[^>]+>", "");
        System.out.println("\n================ CORREO (modo desarrollo) ================");
        System.out.println("Para:   " + nombreDestinatario + " <" + destinatario + ">");
        System.out.println("Asunto: " + asunto);
        System.out.println("----------------------------------------------------------");
        System.out.println(texto.trim());
        System.out.println("==========================================================\n");
        return true;
    }
}
