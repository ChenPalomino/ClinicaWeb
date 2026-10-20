package modelo.dto;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.format.TextStyle;
import java.util.Locale;

/** RF-03: un bloque del horario semanal de un médico (ej. lunes de 08:00 a 13:00). */
public class HorarioDTO {

    private final int diaSemana;          // 1 = lunes ... 7 = domingo
    private final LocalTime horaInicio;
    private final LocalTime horaFin;

    public HorarioDTO(int diaSemana, LocalTime horaInicio, LocalTime horaFin) {
        this.diaSemana = diaSemana;
        this.horaInicio = horaInicio;
        this.horaFin = horaFin;
    }

    public int getDiaSemana() { return diaSemana; }
    public LocalTime getHoraInicio() { return horaInicio; }
    public LocalTime getHoraFin() { return horaFin; }

    /** "Lunes", "Martes"... */
    public String getNombreDia() {
        return nombreDia(diaSemana);
    }

    public static String nombreDia(int diaSemana) {
        String n = DayOfWeek.of(diaSemana).getDisplayName(TextStyle.FULL, new Locale("es", "PE"));
        return Character.toUpperCase(n.charAt(0)) + n.substring(1);
    }

    /** true si este bloque se cruza con otro del mismo día. */
    public boolean seCruzaCon(HorarioDTO otro) {
        return diaSemana == otro.diaSemana && horaInicio.isBefore(otro.horaFin) && otro.horaInicio.isBefore(horaFin);
    }
}
