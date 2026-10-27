package modelo.indicadores;

import modelo.dto.ParValorDTO;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

final class Calculo {

    static final int DIAS_DETALLE_DIARIO = 62;
    private static final Locale ES = Locale.forLanguageTag("es-PE");

    private Calculo() { }

    static List<String> etiquetas(List<ParValorDTO> datos) { return datos.stream().map(ParValorDTO::etiqueta).toList(); }
    static List<BigDecimal> valores(List<ParValorDTO> datos) { return datos.stream().map(ParValorDTO::valor).toList(); }
    static List<BigDecimal> valores2(List<ParValorDTO> datos) { return datos.stream().map(ParValorDTO::valor2).toList(); }
    static BigDecimal suma(List<ParValorDTO> datos) { return datos.stream().map(ParValorDTO::valor).reduce(BigDecimal.ZERO, BigDecimal::add); }
    static BigDecimal suma2(List<ParValorDTO> datos) { return datos.stream().map(ParValorDTO::valor2).reduce(BigDecimal.ZERO, BigDecimal::add); }

    static String soles(BigDecimal v) {
        return "S/ " + String.format(Locale.US, "%,.2f", v == null ? BigDecimal.ZERO : v);
    }

    static String entero(BigDecimal v) {
        return String.format(Locale.US, "%,d", v == null ? 0 : v.setScale(0, RoundingMode.HALF_UP).longValue());
    }

    static String porcentaje(BigDecimal parte, BigDecimal total) {
        if (total == null || total.signum() == 0) {
            return "—";
        }
        return parte.multiply(BigDecimal.valueOf(100)).divide(total, 1, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString() + " %";
    }

    static BigDecimal dividir(BigDecimal a, BigDecimal b) {
        return b == null || b.signum() == 0 ? BigDecimal.ZERO : a.divide(b, 2, RoundingMode.HALF_UP);
    }

    static String mes(YearMonth m) {
        return m.getMonth().getDisplayName(TextStyle.SHORT, ES).replace(".", "") + " " + m.getYear();
    }

    /** Serie temporal sin huecos: por día si el periodo es corto, por mes si es largo. Índice 0 = valor, 1 = valor2. */
    static Map<String, BigDecimal[]> porFecha(Periodo p, List<ParValorDTO> datos) {
        boolean diario = p.dias() <= DIAS_DETALLE_DIARIO;
        Function<LocalDate, String> clave = diario ? Periodo::corto : f -> mes(YearMonth.from(f));
        Map<String, BigDecimal[]> mapa = new LinkedHashMap<>();
        for (LocalDate f = p.desde(); !f.isAfter(p.hasta()); f = diario ? f.plusDays(1) : f.plusMonths(1).withDayOfMonth(1)) {
            mapa.put(clave.apply(f), new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
        }
        for (ParValorDTO d : datos) {
            BigDecimal[] celda = mapa.get(clave.apply(LocalDate.parse(d.etiqueta())));
            if (celda != null) {
                celda[0] = celda[0].add(d.valor());
                celda[1] = celda[1].add(d.valor2());
            }
        }
        return mapa;
    }

    static List<BigDecimal> columna(Map<String, BigDecimal[]> mapa, int i) {
        List<BigDecimal> lista = new ArrayList<>();
        mapa.values().forEach(v -> lista.add(v[i]));
        return lista;
    }

    static String estadoCita(String codigo) {
        return switch (codigo) {
            case "RESERVADA" -> "Reservadas";
            case "CONFIRMADA" -> "Llegaron (en espera)";
            case "ATENDIDA" -> "Atendidas";
            case "CANCELADA" -> "Canceladas";
            case "NO_ASISTIO" -> "No asistieron";
            default -> codigo;
        };
    }

    static BigDecimal de(List<ParValorDTO> datos, String etiqueta) {
        return datos.stream().filter(d -> d.etiqueta().equals(etiqueta)).map(ParValorDTO::valor).findFirst().orElse(BigDecimal.ZERO);
    }
}
