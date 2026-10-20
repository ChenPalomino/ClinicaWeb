package notificacion;

import modelo.dto.CitaDTO;

/**
 * RF-09: un medio por el que se avisa al paciente (correo, SMS...).
 * Agregar WhatsApp mañana = crear otra clase que implemente esta interfaz,
 * sin tocar el notificador ni los servicios (abierto/cerrado, RNF-04).
 */
public interface CanalNotificacionCita {

    String getNombre();

    /** @return true si se envió (false si el paciente no tiene ese dato de contacto o el proveedor falló). */
    boolean enviar(CitaDTO cita, MensajeCita mensaje);
}
