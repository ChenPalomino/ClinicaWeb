package modelo.citas;

import modelo.dto.HorarioDTO;
import modelo.dto.MedicoDTO;
import modelo.dto.TurnoDTO;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * RF-04: calcula los turnos de un médico en una fecha a partir de su horario
 * semanal y de los turnos ya ocupados. No usa la base de datos (se prueba
 * con JUnit, RNF-03): recibe todo lo que necesita.
 *
 * Ejemplo: horario 08:00-13:00 con citas de 30 min -> 08:00, 08:30 ... 12:30.
 * Los turnos que ya pasaron no se ofrecen.
 */
public final class CalculadoraTurnos {

    private CalculadoraTurnos() {
    }

    /** Todos los turnos FUTUROS del día (libres y ocupados), en orden de hora. */
    public static List<TurnoDTO> turnos(MedicoDTO medico, LocalDate fecha, Collection<LocalTime> ocupados, LocalDateTime ahora) {
        List<TurnoDTO> lista = new ArrayList<>();
        int duracion = Math.max(medico.getDuracionCita(), 5);
        List<HorarioDTO> bloques = new ArrayList<>(medico.horariosDelDia(fecha.getDayOfWeek().getValue()));
        bloques.sort((a, b) -> a.getHoraInicio().compareTo(b.getHoraInicio()));
        for (HorarioDTO bloque : bloques) {
            LocalTime inicio = bloque.getHoraInicio();
            LocalTime fin = inicio.plusMinutes(duracion);
            // fin.isAfter(inicio): evita dar la vuelta a la medianoche
            while (fin.isAfter(inicio) && !fin.isAfter(bloque.getHoraFin())) {
                if (fecha.atTime(inicio).isAfter(ahora)) {
                    lista.add(new TurnoDTO(medico.getIdMedico(), fecha, inicio, fin, !ocupados.contains(inicio)));
                }
                inicio = fin;
                fin = inicio.plusMinutes(duracion);
            }
        }
        return lista;
    }

    /** Solo los libres. */
    public static List<TurnoDTO> libres(MedicoDTO medico, LocalDate fecha, Collection<LocalTime> ocupados, LocalDateTime ahora) {
        List<TurnoDTO> lista = new ArrayList<>();
        for (TurnoDTO t : turnos(medico, fecha, ocupados, ahora)) {
            if (t.isLibre()) {
                lista.add(t);
            }
        }
        return lista;
    }

    /** El turno que empieza a esa hora, si existe en el horario y aún no pasó (sin mirar si está ocupado). */
    public static Optional<TurnoDTO> turnoDelHorario(MedicoDTO medico, LocalDate fecha, LocalTime hora, LocalDateTime ahora) {
        for (TurnoDTO t : turnos(medico, fecha, Set.of(), ahora)) {
            if (t.getHoraInicio().equals(hora)) {
                return Optional.of(t);
            }
        }
        return Optional.empty();
    }

    /** true si la hora cae dentro de algún bloque del horario de ese día (para avisar de citas fuera de horario). */
    public static boolean dentroDelHorario(MedicoDTO medico, LocalDate fecha, LocalTime inicio, LocalTime fin) {
        for (HorarioDTO b : medico.horariosDelDia(fecha.getDayOfWeek().getValue())) {
            if (!inicio.isBefore(b.getHoraInicio()) && !fin.isAfter(b.getHoraFin())) {
                return true;
            }
        }
        return false;
    }
}
