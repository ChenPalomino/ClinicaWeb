package modelo.dto;

import java.util.List;

/** RF-04: los turnos de UN médico en UN día, para elegir uno al reservar. */
public class AgendaMedicoDTO {

    private final MedicoDTO medico;
    private final List<TurnoDTO> turnos;

    public AgendaMedicoDTO(MedicoDTO medico, List<TurnoDTO> turnos) {
        this.medico = medico;
        this.turnos = turnos;
    }

    public MedicoDTO getMedico() { return medico; }
    public List<TurnoDTO> getTurnos() { return turnos; }

    public long getLibres() {
        return turnos.stream().filter(TurnoDTO::isLibre).count();
    }

    public boolean isAtiendeEseDia() { return !turnos.isEmpty(); }
}
