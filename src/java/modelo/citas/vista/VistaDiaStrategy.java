package modelo.citas.vista;

import modelo.citas.FechaTexto;

import java.time.LocalDate;

/** PATRÓN: Strategy  ->  ESTRATEGIA CONCRETA: la agenda de UN día (vista por defecto). */
public class VistaDiaStrategy implements VistaAgendaStrategy {

    @Override public String getCodigo() { return "dia"; }
    @Override public String getNombre() { return "Día"; }
    @Override public LocalDate desde(LocalDate ref) { return ref; }
    @Override public LocalDate hasta(LocalDate ref) { return ref; }
    @Override public LocalDate anterior(LocalDate ref) { return ref.minusDays(1); }
    @Override public LocalDate siguiente(LocalDate ref) { return ref.plusDays(1); }
    @Override public String titulo(LocalDate ref) { return FechaTexto.largaTitulo(ref); }
}
