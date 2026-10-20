package modelo.citas.vista;

import modelo.citas.FechaTexto;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

/** PATRÓN: Strategy  ->  ESTRATEGIA CONCRETA: la semana de lunes a domingo que contiene la fecha. */
public class VistaSemanaStrategy implements VistaAgendaStrategy {

    @Override public String getCodigo() { return "semana"; }
    @Override public String getNombre() { return "Semana"; }

    @Override
    public LocalDate desde(LocalDate ref) { return ref.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)); }

    @Override
    public LocalDate hasta(LocalDate ref) { return desde(ref).plusDays(6); }

    @Override public LocalDate anterior(LocalDate ref) { return ref.minusWeeks(1); }
    @Override public LocalDate siguiente(LocalDate ref) { return ref.plusWeeks(1); }

    @Override
    public String titulo(LocalDate ref) {
        LocalDate d = desde(ref);
        LocalDate h = hasta(ref);
        String mesDesde = d.getMonth() == h.getMonth() ? "" : " de " + FechaTexto.mesAnio(d).split(" de ")[0].toLowerCase(FechaTexto.ES);
        return "Semana del " + d.getDayOfMonth() + mesDesde + " al " + h.getDayOfMonth() + " de "
                + FechaTexto.mesAnio(h).toLowerCase(FechaTexto.ES);
    }
}
