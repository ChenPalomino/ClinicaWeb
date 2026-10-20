package modelo.citas;

import modelo.dto.CitaDTO;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Reglas de negocio de las citas, en un solo lugar (sin base de datos: se
 * prueban con JUnit). Los controladores las usan para decidir qué botones
 * mostrar y los servicios para validar: así la pantalla nunca ofrece una
 * acción que luego sería rechazada (prevención de errores, Nielsen #5).
 */
public final class ReglasCita {

    /** El paciente puede reprogramar por su cuenta hasta 2 veces; después, en recepción. */
    public static final int MAXIMO_REPROGRAMACIONES_PACIENTE = 2;
    /** Citas pendientes que un paciente puede tener reservadas desde el portal a la vez. */
    public static final int MAXIMO_PENDIENTES_PORTAL = 3;

    private ReglasCita() {
    }

    /**
     * El PACIENTE puede cancelar o reprogramar desde el portal solo si la cita
     * sigue reservada y faltan al menos "horas" (24 por defecto) para la hora de la cita.
     */
    public static boolean pacientePuedeCambiar(CitaDTO c, LocalDateTime ahora, int horas) {
        return c.isReservada() && !ahora.plusHours(horas).isAfter(c.getInicio());
    }

    public static boolean pacientePuedeReprogramar(CitaDTO c, LocalDateTime ahora, int horas) {
        return pacientePuedeCambiar(c, ahora, horas) && c.getReprogramaciones() < MAXIMO_REPROGRAMACIONES_PACIENTE;
    }

    /** Explicación para el paciente cuando ya no puede cambiar su cita (o null si sí puede). */
    public static String motivoBloqueoPaciente(CitaDTO c, LocalDateTime ahora, int horas) {
        if (!c.isActiva()) {
            return null;
        }
        if (c.isConfirmada()) {
            return "Ya registraste tu llegada en recepción.";
        }
        if (ahora.plusHours(horas).isAfter(c.getInicio())) {
            return "Faltan menos de " + horas + " horas para tu cita: para cancelarla o cambiarla, llama a recepción.";
        }
        return null;
    }

    /** Horas que faltan para la cita (negativo si ya pasó). */
    public static long horasParaLaCita(CitaDTO c, LocalDateTime ahora) {
        return Duration.between(ahora, c.getInicio()).toHours();
    }

    /** RECEPCIÓN reprograma una cita reservada que todavía no empieza. */
    public static boolean personalPuedeReprogramar(CitaDTO c, LocalDateTime ahora) {
        return c.isReservada() && c.getInicio().isAfter(ahora);
    }

    /** RECEPCIÓN cancela una cita activa de hoy o de un día futuro (las pasadas se marcan como "no asistió"). */
    public static boolean personalPuedeCancelar(CitaDTO c, LocalDate hoy) {
        return c.isActiva() && !c.getFecha().isBefore(hoy);
    }

    /** RF-08: la llegada se registra el mismo día de la cita. */
    public static boolean puedeRegistrarLlegada(CitaDTO c, LocalDate hoy) {
        return c.isReservada() && c.getFecha().equals(hoy);
    }

    /** RF-10: el médico marca "atendida" el día de la cita o después (si lo olvidó). */
    public static boolean puedeMarcarAtendida(CitaDTO c, LocalDate hoy) {
        return c.isActiva() && !c.getFecha().isAfter(hoy);
    }

    /** RF-10: "no asistió" solo cuando ya pasó la hora de la cita. */
    public static boolean puedeMarcarNoAsistio(CitaDTO c, LocalDateTime ahora) {
        return c.isReservada() && !ahora.isBefore(c.getInicio());
    }

    /** Se puede cobrar una cita que no fue cancelada ni marcada como no asistida y que aún no tiene cuenta. */
    public static boolean puedeCobrar(CitaDTO c) {
        return (c.isActiva() || c.isAtendida()) && !c.isTieneCuenta();
    }
}
