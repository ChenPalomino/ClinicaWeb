package notificacion;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
 *
 * Además del envío, consulta a Brevo el estado de la cuenta y los últimos
 * eventos de entrega (entregado, bloqueado, rebotado...) para la página de
 * diagnóstico del administrador.
 */
public class BrevoCorreoAdapter implements ServicioCorreo {

    /** Se puede cambiar con BREVO_API_URL solo para pruebas (servidor simulado). */
    private static final String URL_POR_DEFECTO = "https://api.brevo.com/v3";

    private final String apiKey;
    private final String remitente;
    private final String nombreRemitente;
    private final String urlBase;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    public BrevoCorreoAdapter(String apiKey, String remitente, String nombreRemitente) {
        this(apiKey, remitente, nombreRemitente, System.getenv("BREVO_API_URL"));
    }

    BrevoCorreoAdapter(String apiKey, String remitente, String nombreRemitente, String urlBase) {
        this.apiKey = apiKey;
        this.remitente = remitente;
        this.nombreRemitente = nombreRemitente;
        this.urlBase = urlBase == null || urlBase.isBlank() ? URL_POR_DEFECTO : urlBase.replaceAll("/+$", "");
    }

    @Override
    public boolean enviar(String destinatario, String nombreDestinatario, String asunto, String html) {
        return enviarDetallado(destinatario, nombreDestinatario, asunto, html).isAceptado();
    }

    @Override
    public ResultadoEnvio enviarDetallado(String destinatario, String nombreDestinatario, String asunto, String html) {
        String json = "{"
                + "\"sender\":{\"name\":" + json(nombreRemitente) + ",\"email\":" + json(remitente) + "},"
                + "\"to\":[{\"email\":" + json(destinatario) + ",\"name\":" + json(nombreDestinatario) + "}],"
                + "\"subject\":" + json(asunto) + ","
                + "\"htmlContent\":" + json(html)
                + "}";
        String para = ocultar(destinatario);
        if (destinatario != null && destinatario.toLowerCase().endsWith("@clinica.demo")) {
            System.out.println("BrevoCorreoAdapter: ATENCIÓN, " + para + " es un correo de PRUEBA (no existe). "
                    + "Cámbialo por uno real en Mi perfil o en Usuarios para recibir los códigos.");
        }
        try {
            HttpResponse<String> respuesta = http.send(peticion("/smtp/email").header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json)).build(), HttpResponse.BodyHandlers.ofString());
            int codigo = respuesta.statusCode();
            if (codigo >= 200 && codigo < 300) {
                String id = JsonLector.texto(JsonLector.objeto(leerSeguro(respuesta.body())).get("messageId"));
                // System.out: se ve siempre en la pestaña Output de NetBeans
                System.out.println("BrevoCorreoAdapter: Brevo ACEPTÓ el correo para " + para + " (\"" + asunto + "\") "
                        + id + ". Si no llega, revisa en Brevo > Transaccional > Registros el estado de entrega.");
                return new ResultadoEnvio(true, codigo, "Brevo aceptó el correo para " + para + ".");
            }
            String explicacion = explicar(codigo, respuesta.body());
            System.out.println("BrevoCorreoAdapter: ERROR " + codigo + " al enviar a " + para + " -> " + explicacion
                    + " | Respuesta de Brevo: " + respuesta.body());
            return new ResultadoEnvio(false, codigo, explicacion);
        } catch (Exception e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            String explicacion = "No se pudo conectar con Brevo (" + e.getClass().getSimpleName() + ": " + e.getMessage()
                    + "). Revisa tu conexión a internet o si un firewall/antivirus bloquea a Java.";
            System.out.println("BrevoCorreoAdapter: " + explicacion);
            return new ResultadoEnvio(false, 0, explicacion);
        }
    }

    // ------------------------------------------------------------ diagnóstico

    /** Estado de la cuenta Brevo: correo, plan y créditos. Si falla, "error" explica por qué. */
    public Map<String, String> consultarCuenta() {
        Map<String, String> datos = new LinkedHashMap<>();
        try {
            HttpResponse<String> r = http.send(peticion("/account").GET().build(), HttpResponse.BodyHandlers.ofString());
            if (r.statusCode() != 200) {
                datos.put("error", explicar(r.statusCode(), r.body()));
                return datos;
            }
            Map<String, Object> cuenta = JsonLector.objeto(JsonLector.leer(r.body()));
            datos.put("cuenta", JsonLector.texto(cuenta.get("email")));
            datos.put("empresa", JsonLector.texto(cuenta.get("companyName")));
            for (Object plan : JsonLector.lista(cuenta.get("plan"))) {
                Map<String, Object> p = JsonLector.objeto(plan);
                datos.put("plan", JsonLector.texto(p.get("type")));
                datos.put("creditos", JsonLector.texto(p.get("credits")) + " (" + JsonLector.texto(p.get("creditsType")) + ")");
            }
        } catch (Exception e) {
            datos.put("error", "No se pudo conectar con Brevo: " + e.getMessage());
        }
        return datos;
    }

    /** Últimos eventos de entrega (los más recientes primero), opcionalmente de un destinatario. */
    public List<EventoCorreo> ultimosEventos(int cantidad, String destinatario) throws ErrorDiagnostico {
        String url = "/smtp/statistics/events?limit=" + cantidad + "&sort=desc"
                + (destinatario == null || destinatario.isBlank() ? "" : "&email=" + URLEncoder.encode(destinatario.trim(), StandardCharsets.UTF_8));
        try {
            HttpResponse<String> r = http.send(peticion(url).GET().build(), HttpResponse.BodyHandlers.ofString());
            if (r.statusCode() != 200) {
                throw new ErrorDiagnostico(explicar(r.statusCode(), r.body()));
            }
            List<EventoCorreo> eventos = new ArrayList<>();
            for (Object o : JsonLector.lista(JsonLector.objeto(leerSeguro(r.body())).get("events"))) {
                Map<String, Object> e = JsonLector.objeto(o);
                eventos.add(new EventoCorreo(JsonLector.texto(e.get("date")), JsonLector.texto(e.get("email")),
                        JsonLector.texto(e.get("subject")), JsonLector.texto(e.get("event")), JsonLector.texto(e.get("reason"))));
            }
            return eventos;
        } catch (ErrorDiagnostico e) {
            throw e;
        } catch (Exception e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new ErrorDiagnostico("No se pudo conectar con Brevo: " + e.getMessage());
        }
    }

    public String getRemitente() { return remitente; }

    /** "xkeysib-e922…a1b2": suficiente para reconocer la clave sin mostrarla. */
    public String getClaveOculta() {
        return apiKey.length() <= 16 ? "****" : apiKey.substring(0, 12) + "…" + apiKey.substring(apiKey.length() - 4);
    }

    /** Error de la consulta de diagnóstico, con el mensaje ya explicado. */
    public static final class ErrorDiagnostico extends Exception {
        public ErrorDiagnostico(String mensaje) { super(mensaje); }
    }

    // ------------------------------------------------------------ auxiliares

    private HttpRequest.Builder peticion(String ruta) {
        return HttpRequest.newBuilder(URI.create(urlBase + ruta))
                .timeout(Duration.ofSeconds(15))
                .header("api-key", apiKey)
                .header("Accept", "application/json");
    }

    /** Traduce los errores de Brevo a una causa probable y qué hacer. */
    static String explicar(int codigo, String cuerpo) {
        String mensaje = JsonLector.texto(JsonLector.objeto(leerSeguro(cuerpo)).get("message"));
        String m = mensaje.toLowerCase();
        String causa;
        if (codigo == 401 && m.contains("ip")) {
            causa = "Brevo no reconoce la IP de esta computadora. En Brevo > Seguridad > IP autorizadas, "
                    + "autoriza tu IP o desactiva el bloqueo de IP desconocidas.";
        } else if (codigo == 401) {
            causa = "La clave BREVO_API_KEY no es válida (¿la regeneraste o se copió mal?). Crea una nueva en "
                    + "Brevo > SMTP y API > Claves API y actualiza la variable de entorno.";
        } else if (codigo == 400 && m.contains("sender")) {
            causa = "El remitente CORREO_REMITENTE no está verificado en Brevo. Verifícalo en Brevo > Remitentes.";
        } else if (codigo == 402) {
            causa = "La cuenta de Brevo no tiene créditos de envío (plan gratuito: 300 correos al día).";
        } else if (codigo == 403) {
            causa = "Brevo bloqueó o aún no activó la cuenta para envíos transaccionales. Revisa los avisos en tu panel de Brevo.";
        } else if (codigo == 429) {
            causa = "Demasiados envíos seguidos: Brevo limitó la cuenta por unos minutos.";
        } else {
            causa = "Brevo respondió " + codigo + ".";
        }
        return mensaje.isEmpty() ? causa : causa + " (Brevo: \"" + mensaje + "\")";
    }

    private static Object leerSeguro(String json) {
        try {
            return JsonLector.leer(json);
        } catch (RuntimeException e) {
            return null;
        }
    }

    /** "ch***@gmail.com": para los mensajes de la consola. */
    static String ocultar(String correo) {
        if (correo == null || !correo.contains("@")) return String.valueOf(correo);
        int arroba = correo.indexOf('@');
        return correo.substring(0, Math.min(2, arroba)) + "***" + correo.substring(arroba);
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
