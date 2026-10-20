package modelo.citas;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Fechas y horas escritas como las dice la gente (Nielsen #2: lenguaje del
 * usuario): "Hoy", "Mañana", "jueves 22 de octubre", "9:30 a. m.".
 */
public final class FechaTexto {

    public static final Locale ES = new Locale("es", "PE");
    private static final DateTimeFormatter LARGA = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM 'de' yyyy", ES);
    private static final DateTimeFormatter MEDIA = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", ES);
    private static final DateTimeFormatter DIA_SEMANA = DateTimeFormatter.ofPattern("EEE", ES);
    private static final DateTimeFormatter MES = DateTimeFormatter.ofPattern("MMM", ES);
    private static final DateTimeFormatter MES_ANIO = DateTimeFormatter.ofPattern("MMMM 'de' yyyy", ES);
    private static final DateTimeFormatter DMY = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private FechaTexto() {
    }

    /** "martes 20 de octubre de 2026" */
    public static String larga(LocalDate f) { return f == null ? "" : f.format(LARGA); }

    /** "Martes 20 de octubre de 2026" (para títulos) */
    public static String largaTitulo(LocalDate f) { return capital(larga(f)); }

    /** "Hoy", "Mañana", "Ayer" o "jueves 22 de octubre" (+ año si no es el actual). */
    public static String relativa(LocalDate f, LocalDate hoy) {
        if (f == null) return "";
        if (f.equals(hoy)) return "Hoy";
        if (f.equals(hoy.plusDays(1))) return "Mañana";
        if (f.equals(hoy.minusDays(1))) return "Ayer";
        return capital(f.getYear() == hoy.getYear() ? f.format(MEDIA) : f.format(LARGA));
    }

    /** Para frases: "hoy", "mañana" o "el jueves 22 de octubre" (ej. "reservada para mañana"). */
    public static String paraFrase(LocalDate f, LocalDate hoy) {
        String r = relativa(f, hoy);
        return f.equals(hoy) || f.equals(hoy.plusDays(1)) || f.equals(hoy.minusDays(1)) ? r.toLowerCase(ES) : "el " + r.toLowerCase(ES);
    }

    /** "Hoy, martes 20 de octubre" o "Jueves 22 de octubre". */
    public static String relativaCompleta(LocalDate f, LocalDate hoy) {
        String r = relativa(f, hoy);
        return f.equals(hoy) || f.equals(hoy.plusDays(1)) || f.equals(hoy.minusDays(1)) ? r + ", " + f.format(MEDIA) : r;
    }

    /** "mar" */
    public static String diaCorto(LocalDate f) { return capital(f.format(DIA_SEMANA).replace(".", "")); }

    /** "oct" */
    public static String mesCorto(LocalDate f) { return f.format(MES).replace(".", ""); }

    /** "Octubre de 2026" */
    public static String mesAnio(LocalDate f) { return capital(f.format(MES_ANIO)); }

    /** "20/10/2026" */
    public static String dmy(LocalDate f) { return f == null ? "" : f.format(DMY); }

    /** "9:30 a. m." */
    public static String hora(LocalTime h) {
        if (h == null) return "";
        int hh = h.getHour() % 12 == 0 ? 12 : h.getHour() % 12;
        return hh + ":" + String.format("%02d", h.getMinute()) + (h.getHour() < 12 ? " a. m." : " p. m.");
    }

    /** "09:30" para inputs y parámetros. */
    public static String horaIso(LocalTime h) { return h == null ? "" : String.format("%02d:%02d", h.getHour(), h.getMinute()); }

    private static String capital(String s) {
        return s == null || s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
