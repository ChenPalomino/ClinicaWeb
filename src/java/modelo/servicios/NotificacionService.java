package modelo.servicios;

import modelo.dto.ContactoDTO;
import notificacion.ServicioCorreo;
import notificacion.ServicioCorreoFactory;
import seguridad.EscapeHtml;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;

/**
 * Arma el contenido de los correos del sistema y los envía usando el
 * ServicioCorreo que entrega la fábrica (Brevo o consola).
 */
public class NotificacionService {

    private static final ZoneId LIMA = ZoneId.of("America/Lima");
    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("dd/MM/yyyy 'a las' HH:mm");

    private final ServicioCorreo correo;

    public NotificacionService() {
        this(ServicioCorreoFactory.crear());
    }

    public NotificacionService(ServicioCorreo correo) {
        this.correo = correo;
    }

    /** Envía el código de 6 dígitos. Es síncrono: hay que saber si salió. */
    public boolean enviarCodigo(ContactoDTO contacto, String codigo, int minutosValidez) {
        String cuerpo = "<p>Hola " + EscapeHtml.texto(contacto.getNombres()) + ",</p>"
                + "<p>Recibimos una solicitud para recuperar el acceso a tu cuenta. Tu código es:</p>"
                + "<p style=\"font-size:30px;font-weight:bold;letter-spacing:6px;color:#002244;margin:18px 0;\">"
                + codigo + "</p>"
                + "<p>Vence en <strong>" + minutosValidez + " minutos</strong> y solo se puede usar una vez.</p>"
                + "<p style=\"color:#64748b;\">Si tú no lo pediste, ignora este correo: tu contraseña no cambiará.</p>";
        return correo.enviar(contacto.getCorreo(), contacto.getNombres(),
                "Tu código de verificación: " + codigo, plantilla("Código de verificación", cuerpo));
    }

    /** Aviso de contraseña cambiada con enlace para reportar si no fue el usuario. */
    public void avisarCambioContrasena(ContactoDTO contacto, String enlaceNoFuiYo) {
        String cuerpo = "<p>Hola " + EscapeHtml.texto(contacto.getNombres()) + ",</p>"
                + "<p>La contraseña de tu cuenta se cambió el <strong>" + ahora() + "</strong>.</p>"
                + "<p>Si fuiste tú, no necesitas hacer nada.</p>"
                + "<p><strong>¿No fuiste tú?</strong> Bloquea tu cuenta de inmediato:</p>"
                + boton(enlaceNoFuiYo, "No fui yo, bloquear mi cuenta", "#b91c1c")
                + "<p style=\"color:#64748b;\">Luego acércate a recepción con tu DNI para recuperar el acceso.</p>";
        enviarEnSegundoPlano(contacto, "Tu contraseña fue cambiada", plantilla("Contraseña actualizada", cuerpo));
    }

    /** Aviso de cuenta bloqueada por 3 intentos fallidos. */
    public void avisarBloqueo(ContactoDTO contacto, String enlaceRecuperar) {
        String cuerpo = "<p>Hola " + EscapeHtml.texto(contacto.getNombres()) + ",</p>"
                + "<p>Tu cuenta se bloqueó el <strong>" + ahora() + "</strong> porque se escribió una "
                + "contraseña incorrecta 3 veces.</p>"
                + "<p>Se desbloqueará sola en 24 horas, o puedes recuperarla ahora con un código:</p>"
                + boton(enlaceRecuperar, "Recuperar mi cuenta", "#003366")
                + "<p style=\"color:#64748b;\">Si no fuiste tú, alguien intentó entrar a tu cuenta. "
                + "Te recomendamos cambiar tu contraseña.</p>";
        enviarEnSegundoPlano(contacto, "Tu cuenta fue bloqueada temporalmente", plantilla("Cuenta bloqueada", cuerpo));
    }

    /** Aviso al correo ANTERIOR cuando el usuario cambia su correo en "Mi perfil". */
    public void avisarCambioCorreo(ContactoDTO anterior, String correoNuevo) {
        String cuerpo = "<p>Hola " + EscapeHtml.texto(anterior.getNombres()) + ",</p>"
                + "<p>El correo de tu cuenta se cambió el <strong>" + ahora() + "</strong> a:</p>"
                + "<p style=\"font-size:16px;\"><strong>" + EscapeHtml.texto(ocultar(correoNuevo)) + "</strong></p>"
                + "<p>Desde ahora los códigos de recuperación llegarán a esa dirección.</p>"
                + "<p style=\"color:#b91c1c;\"><strong>¿No fuiste tú?</strong> Acércate a recepción con tu DNI "
                + "o comunícate con el administrador lo antes posible.</p>";
        enviarEnSegundoPlano(anterior, "Se cambió el correo de tu cuenta", plantilla("Correo actualizado", cuerpo));
    }

    private static String ocultar(String correo) {
        return new ContactoDTO("", correo).getCorreoOculto();
    }

    // ------------------------------------------------------------------

    private void enviarEnSegundoPlano(ContactoDTO contacto, String asunto, String html) {
        CompletableFuture.runAsync(() -> correo.enviar(contacto.getCorreo(), contacto.getNombres(), asunto, html));
    }

    private static String ahora() {
        return ZonedDateTime.now(LIMA).format(FORMATO);
    }

    private static String boton(String url, String texto, String color) {
        return "<p style=\"margin:18px 0;\"><a href=\"" + EscapeHtml.texto(url) + "\" style=\"background:" + color
                + ";color:#ffffff;padding:12px 20px;border-radius:6px;text-decoration:none;font-weight:bold;"
                + "display:inline-block;\">" + texto + "</a></p>";
    }

    private static String plantilla(String titulo, String cuerpo) {
        return "<div style=\"font-family:Segoe UI,Arial,sans-serif;background:#f4f6f9;padding:24px;\">"
                + "<div style=\"max-width:520px;margin:auto;background:#ffffff;border-radius:10px;overflow:hidden;\">"
                + "<div style=\"background:#002244;color:#ffffff;padding:18px 24px;font-size:18px;font-weight:bold;\">"
                + "Clínica Modelo San Martín</div>"
                + "<div style=\"padding:24px;color:#1e293b;font-size:15px;line-height:1.5;\">"
                + "<h2 style=\"margin-top:0;color:#002244;\">" + titulo + "</h2>" + cuerpo + "</div>"
                + "<div style=\"padding:14px 24px;background:#f8fafc;color:#94a3b8;font-size:12px;\">"
                + "Este es un mensaje automático, por favor no respondas.</div></div></div>";
    }
}
