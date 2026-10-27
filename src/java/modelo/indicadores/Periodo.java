package modelo.indicadores;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public record Periodo(LocalDate desde, LocalDate hasta) {

    public static final int MAXIMO_DIAS = 366;
    private static final DateTimeFormatter CORTO = DateTimeFormatter.ofPattern("dd/MM");
    private static final DateTimeFormatter LARGO = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public static Periodo de(String clave, LocalDate hoy) {
        if ("mes".equals(clave)) {
            return new Periodo(hoy.withDayOfMonth(1), hoy);
        }
        if ("mesAnterior".equals(clave)) {
            LocalDate inicio = hoy.minusMonths(1).withDayOfMonth(1);
            return new Periodo(inicio, inicio.withDayOfMonth(inicio.lengthOfMonth()));
        }
        int dias;
        try {
            dias = Integer.parseInt(clave == null ? "30" : clave);
        } catch (NumberFormatException e) {
            dias = 30;
        }
        dias = Math.max(1, Math.min(dias, 365));
        return new Periodo(hoy.minusDays(dias - 1L), hoy);
    }

    public long dias() { return ChronoUnit.DAYS.between(desde, hasta) + 1; }

    public String texto() {
        return desde.equals(hasta) ? desde.format(LARGO) : desde.format(LARGO) + " al " + hasta.format(LARGO);
    }

    public static String corto(LocalDate f) { return f.format(CORTO); }
}
