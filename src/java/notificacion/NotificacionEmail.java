package notificacion;

import modelo.dto.CitaDTO;

/** RF-09: aviso por CORREO, usando el proveedor configurado (Brevo o consola) a través del Adapter. */
public class NotificacionEmail implements CanalNotificacionCita {

    private final ServicioCorreo correo;

    public NotificacionEmail() {
        this(ServicioCorreoFactory.crear());
    }

    public NotificacionEmail(ServicioCorreo correo) {
        this.correo = correo;
    }

    @Override
    public String getNombre() { return "correo"; }

    @Override
    public boolean enviar(CitaDTO cita, MensajeCita mensaje) {
        if (cita.getCorreoPaciente().isBlank()) {
            return false;
        }
        return correo.enviar(cita.getCorreoPaciente(), cita.getNombrePaciente(), mensaje.getAsunto(),
                PlantillaCorreo.envolver(mensaje.getTitulo(), mensaje.getHtml()));
    }
}
