package modelo.citas.vista;

import java.time.LocalDate;

/**
 * PATRÓN: Strategy  ->  INTERFAZ de las vistas de la agenda (día, semana, mes).
 *
 * Cada vista sabe qué rango de fechas consultar, cómo moverse a la anterior o
 * a la siguiente y cómo titularse. El controlador y la JSP trabajan con esta
 * interfaz: agregar una vista nueva (ej. "quincena") es crear una clase, sin
 * tocar las existentes (RNF-04). Además limita lo que se consulta a la BD:
 * nunca más de un mes de citas a la vez (RNF-05).
 */
public interface VistaAgendaStrategy {

    /** "dia", "semana" o "mes" (valor del parámetro "vista"). */
    String getCodigo();

    /** Texto del botón: "Día", "Semana", "Mes". */
    String getNombre();

    LocalDate desde(LocalDate referencia);

    LocalDate hasta(LocalDate referencia);

    LocalDate anterior(LocalDate referencia);

    LocalDate siguiente(LocalDate referencia);

    /** "Martes 20 de octubre de 2026", "Semana del 19 al 25 de octubre", "Octubre de 2026". */
    String titulo(LocalDate referencia);
}
