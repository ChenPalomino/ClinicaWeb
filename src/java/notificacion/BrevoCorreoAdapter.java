package notificacion;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * PATRÓN: Adapter  ->  ADAPTADOR
 *
 * Adapta nuestra interfaz ServicioCorreo a la API REST de Brevo
 * (https://api.brevo.com/v3/smtp/email). Usa HttpClient de Java, así que
 * no necesita librerías externas, y funciona aunque el hosting bloquee SMTP.
 *
 * Configuración (variables de entorno):
 *   BREVO_API_KEY     clave de la API de Brevo
 *   CORREO_REMITENTE  correo verificado en Brevo (ej. clinica.sanmartin@gmail.com)
 *   CORREO_NOMBRE     nombre que verá el paciente (opcional)
 */
public class BrevoCorreoAdapter implements ServicioCorreo {

    private static final String URL_API = "https://api.brevo.com/v3/smtp/email";

    private final String apiKey;
    private final String remitente;
    private final String nombreRemitente;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    public BrevoCorreoAdapter(String apiKey, String remitente, String nombreRemitente) {
        this.apiKey = apiKey;
        this.remitente = remitente;
        this.nombreRemitente = nombreRemitente;
    }

    @Override
    public boolean enviar(String destinatario, String nombreDestinatario, String asunto, String html) {
        String json = "{"
                + "\"sender\":{\"name\":" + json(nombreRemitente) + ",\"email\":" + json(remitente) + "},"
                + "\"to\":[{\"email\":" + json(destinatario) + ",\"name\":" + json(nombreDestinatario) + "}],"
                + "\"subject\":" + json(asunto) + ","
                + "\"htmlContent\":" + json(html)
                + "}";
        HttpRequest peticion = HttpRequest.newBuilder(URI.create(URL_API))
                .timeout(Duration.ofSeconds(15))
                .header("api-key", apiKey)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        try {
            HttpResponse<String> respuesta = http.send(peticion, HttpResponse.BodyHandlers.ofString());
            boolean ok = respuesta.statusCode() >= 200 && respuesta.statusCode() < 300;
            if (!ok) {
                System.err.println("BrevoCorreoAdapter: error " + respuesta.statusCode() + " -> " + respuesta.body());
            }
            return ok;
        } catch (Exception e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            System.err.println("BrevoCorreoAdapter: no se pudo enviar -> " + e.getMessage());
            return false;
        }
    }

    /** Convierte un texto a cadena JSON válida (con comillas y caracteres escapados). */
    private static String json(String texto) {
        if (texto == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder("\"");
        for (char c : texto.toCharArray()) {
            switch (c) {
                case '"': sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        return sb.append('"').toString();
    }
}
