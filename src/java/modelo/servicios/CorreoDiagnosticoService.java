package modelo.servicios;

import modelo.entidades.Usuario;
import modelo.repositorio.UsuarioRepository;
import modelo.repositorio.UsuarioRepositoryImpl;
import notificacion.BrevoCorreoAdapter;
import notificacion.EventoCorreo;
import notificacion.ResultadoEnvio;
import notificacion.ServicioCorreo;
import notificacion.ServicioCorreoFactory;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Diagnóstico del envío de correos (solo Administrador): qué modo está activo,
 * el estado de la cuenta de Brevo, un envío de prueba y los últimos eventos de
 * entrega con su motivo. Sirve para saber POR QUÉ un código no llegó.
 */
public class CorreoDiagnosticoService {

    private final ServicioCorreo correo;
    private final UsuarioRepository usuarioRepository;

    public CorreoDiagnosticoService() {
        this(ServicioCorreoFactory.crear(), new UsuarioRepositoryImpl());
    }

    public CorreoDiagnosticoService(ServicioCorreo correo, UsuarioRepository usuarioRepository) {
        this.correo = correo;
        this.usuarioRepository = usuarioRepository;
    }

    public boolean isModoReal() {
        return correo instanceof BrevoCorreoAdapter;
    }

    /** Configuración visible (sin exponer la clave completa). */
    public Map<String, String> configuracion() {
        Map<String, String> c = new LinkedHashMap<>();
        if (correo instanceof BrevoCorreoAdapter) {
            BrevoCorreoAdapter brevo = (BrevoCorreoAdapter) correo;
            c.put("Modo", "Correos reales con Brevo");
            c.put("Remitente (CORREO_REMITENTE)", brevo.getRemitente());
            c.put("Clave (BREVO_API_KEY)", brevo.getClaveOculta());
        } else {
            c.put("Modo", "Desarrollo: los correos solo se muestran en la consola de NetBeans");
            c.put("Variables que faltan", ServicioCorreoFactory.variablesFaltantes());
        }
        return c;
    }

    public Map<String, String> cuentaBrevo() {
        return isModoReal() ? ((BrevoCorreoAdapter) correo).consultarCuenta() : Map.of();
    }

    public List<EventoCorreo> ultimosEventos(String destinatario) throws BrevoCorreoAdapter.ErrorDiagnostico {
        return isModoReal() ? ((BrevoCorreoAdapter) correo).ultimosEventos(15, destinatario) : List.of();
    }

    public List<String> usuariosConCorreoDePrueba() {
        return usuarioRepository.usuariosConCorreoDePrueba();
    }

    /** Envía un correo de prueba y devuelve exactamente lo que respondió el proveedor. */
    public ResultadoEnvio enviarPrueba(String destinatario, Usuario admin) {
        if (destinatario == null || !Validaciones.correo(destinatario.trim())) {
            return new ResultadoEnvio(false, 0, "Escribe un correo válido para la prueba.");
        }
        String html = "<p>Hola,</p><p>Este es un correo de <strong>prueba</strong> enviado desde ClinicaWeb por @"
                + admin.getNombreUsuario() + ".</p><p>Si lo recibes, el envío de códigos de recuperación funciona.</p>";
        return correo.enviarDetallado(destinatario.trim(), destinatario.trim(), "Prueba de correo - Clínica San Martín", html);
    }
}
