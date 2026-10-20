package modelo.dto;

import java.util.HashMap;
import java.util.Map;

/** Cuántas citas hay de cada estado en un periodo (indicadores del inicio y de la agenda). */
public class ResumenCitasDTO {

    private final Map<String, Integer> porEstado = new HashMap<>();

    public void sumar(String estado, int cantidad) {
        porEstado.merge(estado, cantidad, Integer::sum);
    }

    public int get(String estado) { return porEstado.getOrDefault(estado, 0); }
    public int getReservadas() { return get(CitaDTO.RESERVADA); }
    public int getConfirmadas() { return get(CitaDTO.CONFIRMADA); }
    public int getAtendidas() { return get(CitaDTO.ATENDIDA); }
    public int getNoAsistio() { return get(CitaDTO.NO_ASISTIO); }
    public int getCanceladas() { return get(CitaDTO.CANCELADA); }

    /** Citas que ocupan agenda (todas menos las canceladas). */
    public int getProgramadas() {
        return getReservadas() + getConfirmadas() + getAtendidas() + getNoAsistio();
    }

    /** Aún por atender: reservadas + en sala de espera. */
    public int getPendientes() { return getReservadas() + getConfirmadas(); }
}
