package notificacion;

import modelo.dto.CitaDTO;

/**
 * RF-09: aviso por SMS (SIMULADO).
 * Un proveedor real de SMS es de pago; esta clase deja listo el punto de
 * conexión: hoy escribe el mensaje en el registro del servidor (log). Para
 * activarlo de verdad solo hay que cambiar el cuerpo de enviar(), sin tocar
 * nada más del sistema.
 */
public class NotificacionSMS implements CanalNotificacionCita {

    @Override
    public String getNombre() { return "SMS"; }

    @Override
    public boolean enviar(CitaDTO cita, MensajeCita mensaje) {
        String numero = cita.getCelularPaciente().replaceAll("[^0-9]", "");
        if (numero.length() < 9) {
            return false;
        }
        String oculto = numero.substring(0, 1) + "******" + numero.substring(numero.length() - 2);
        System.out.println("NotificacionSMS (simulado) -> " + oculto + ": " + mensaje.getTextoCorto());
        return true;
    }
}
