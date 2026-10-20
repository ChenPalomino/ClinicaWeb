package modelo.dto;

import java.time.LocalDate;
import java.time.LocalTime;

/** RF-04: un turno de la agenda de un médico (ej. 22/10/2026 de 09:30 a 10:00). */
public class TurnoDTO {

    private final int idMedico;
    private final LocalDate fecha;
    private final LocalTime horaInicio;
    private final LocalTime horaFin;
    private final boolean libre;

    public TurnoDTO(int idMedico, LocalDate fecha, LocalTime horaInicio, LocalTime horaFin, boolean libre) {
        this.idMedico = idMedico;
        this.fecha = fecha;
        this.horaInicio = horaInicio;
        this.horaFin = horaFin;
        this.libre = libre;
    }

    public int getIdMedico() { return idMedico; }
    public LocalDate getFecha() { return fecha; }
    public LocalTime getHoraInicio() { return horaInicio; }
    public LocalTime getHoraFin() { return horaFin; }
    public boolean isLibre() { return libre; }
    public boolean isManiana() { return horaInicio.getHour() < 12; }
}
