package modelo.cobranza;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Convierte un monto a letras como se imprime en boletas y facturas:
 * 1250.50 -> "MIL DOSCIENTOS CINCUENTA Y 50/100 SOLES".
 */
public final class MontoEnLetras {

    private static final String[] UNIDADES = {"", "UNO", "DOS", "TRES", "CUATRO", "CINCO", "SEIS", "SIETE", "OCHO", "NUEVE",
            "DIEZ", "ONCE", "DOCE", "TRECE", "CATORCE", "QUINCE", "DIECISÉIS", "DIECISIETE", "DIECIOCHO", "DIECINUEVE",
            "VEINTE", "VEINTIUNO", "VEINTIDÓS", "VEINTITRÉS", "VEINTICUATRO", "VEINTICINCO", "VEINTISÉIS", "VEINTISIETE",
            "VEINTIOCHO", "VEINTINUEVE"};
    private static final String[] DECENAS = {"", "", "", "TREINTA", "CUARENTA", "CINCUENTA", "SESENTA", "SETENTA",
            "OCHENTA", "NOVENTA"};
    private static final String[] CENTENAS = {"", "CIENTO", "DOSCIENTOS", "TRESCIENTOS", "CUATROCIENTOS", "QUINIENTOS",
            "SEISCIENTOS", "SETECIENTOS", "OCHOCIENTOS", "NOVECIENTOS"};

    private MontoEnLetras() {
    }

    public static String convertir(BigDecimal monto) {
        BigDecimal m = monto.setScale(2, RoundingMode.HALF_UP);
        long entero = m.longValue();
        int centimos = m.subtract(BigDecimal.valueOf(entero)).movePointRight(2).intValue();
        String letras = entero == 0 ? "CERO" : enLetras(entero);
        return letras + " Y " + String.format("%02d", centimos) + "/100 SOLES";
    }

    private static String enLetras(long n) {
        if (n >= 1_000_000) {
            long millones = n / 1_000_000;
            String texto = millones == 1 ? "UN MILLÓN" : apocopar(enLetras(millones)) + " MILLONES";
            return n % 1_000_000 == 0 ? texto : texto + " " + enLetras(n % 1_000_000);
        }
        if (n >= 1000) {
            long miles = n / 1000;
            String texto = miles == 1 ? "MIL" : apocopar(enLetras(miles)) + " MIL";
            return n % 1000 == 0 ? texto : texto + " " + enLetras(n % 1000);
        }
        if (n >= 100) {
            if (n == 100) {
                return "CIEN";
            }
            String texto = CENTENAS[(int) (n / 100)];
            return n % 100 == 0 ? texto : texto + " " + enLetras(n % 100);
        }
        if (n < 30) {
            return UNIDADES[(int) n];
        }
        String texto = DECENAS[(int) (n / 10)];
        return n % 10 == 0 ? texto : texto + " Y " + UNIDADES[(int) (n % 10)];
    }

    /** "VEINTIUNO MIL" -> "VEINTIÚN MIL", "UNO MIL" no ocurre (se trata aparte). */
    private static String apocopar(String texto) {
        if (texto.endsWith("VEINTIUNO")) {
            return texto.substring(0, texto.length() - "VEINTIUNO".length()) + "VEINTIÚN";
        }
        return texto.endsWith("UNO") ? texto.substring(0, texto.length() - 1) : texto;
    }
}
