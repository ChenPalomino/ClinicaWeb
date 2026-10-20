package modelo.citas.vista;

import modelo.citas.FechaTexto;

import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

/**
 * PATRÓN: Strategy  ->  ESTRATEGIA CONCRETA: el mes completo.
 * La vista de mes muestra solo CUÁNTAS citas hay cada día (consulta agrupada,
 * muy liviana); al hacer clic en un día se abre la vista de ese día.
 */
public class VistaMesStrategy implements VistaAgendaStrategy {

    @Override public String getCodigo() { return "mes"; }
    @Override public String getNombre() { return "Mes"; }
    @Override public LocalDate desde(LocalDate ref) { return ref.withDayOfMonth(1); }
    @Override public LocalDate hasta(LocalDate ref) { return ref.with(TemporalAdjusters.lastDayOfMonth()); }
    @Override public LocalDate anterior(LocalDate ref) { return ref.minusMonths(1).withDayOfMonth(1); }
    @Override public LocalDate siguiente(LocalDate ref) { return ref.plusMonths(1).withDayOfMonth(1); }
    @Override public String titulo(LocalDate ref) { return FechaTexto.mesAnio(ref); }
}
