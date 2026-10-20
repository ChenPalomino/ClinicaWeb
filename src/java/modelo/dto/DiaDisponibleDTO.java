package modelo.dto;

import java.time.LocalDate;

/** Un día de la tira de fechas del asistente de reserva, con cuántos turnos libres tiene. */
public class DiaDisponibleDTO {

    private final LocalDate fecha;
    private final int libres;
    private final boolean atiende;

    public DiaDisponibleDTO(LocalDate fecha, int libres, boolean atiende) {
        this.fecha = fecha;
        this.libres = libres;
        this.atiende = atiende;
    }

    public LocalDate getFecha() { return fecha; }
    public int getLibres() { return libres; }
    /** false = ningún médico atiende ese día (domingo, por ejemplo). */
    public boolean isAtiende() { return atiende; }
}
