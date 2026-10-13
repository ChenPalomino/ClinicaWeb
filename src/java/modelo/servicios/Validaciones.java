package modelo.servicios;

import java.util.regex.Pattern;

/**
 * Reglas de validación del lado del SERVIDOR compartidas por todos los
 * servicios (usuarios, perfil, pacientes, proveedores, tarifario).
 * Una sola fuente de verdad: si cambia una regla, cambia en todo el sistema.
 * Son métodos puros (sin BD), por eso se prueban fácilmente con JUnit.
 */
public final class Validaciones {

    private static final Pattern NOMBRE = Pattern.compile("^[A-Za-zÁÉÍÓÚÜÑáéíóúüñ' ]{2,100}$");
    private static final Pattern DNI = Pattern.compile("^\\d{8}$");
    private static final Pattern CORREO = Pattern.compile("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)*\\.[A-Za-z]{2,}$");
    private static final Pattern CELULAR = Pattern.compile("^9\\d{8}$");
    private static final Pattern TELEFONO = Pattern.compile("^\\d{7,9}$");
    private static final Pattern RUC = Pattern.compile("^(10|15|17|20)\\d{9}$");
    private static final int[] PESOS_RUC = {5, 4, 3, 2, 7, 6, 5, 4, 3, 2};

    private Validaciones() {
    }

    public static boolean nombre(String v) { return v != null && NOMBRE.matcher(v).matches(); }
    public static boolean dni(String v) { return v != null && DNI.matcher(v).matches(); }
    public static boolean correo(String v) { return v != null && v.length() <= 100 && CORREO.matcher(v).matches(); }
    public static boolean celular(String v) { return v != null && CELULAR.matcher(v).matches(); }
    /** Fijo (7 dígitos), fijo con código de Lima (9) o celular. */
    public static boolean telefono(String v) { return v != null && TELEFONO.matcher(v).matches(); }

    /**
     * RUC peruano: 11 dígitos, prefijo válido (10, 15, 17, 20) y dígito
     * verificador correcto (algoritmo módulo 11 de SUNAT).
     */
    public static boolean ruc(String v) {
        if (v == null || !RUC.matcher(v).matches()) {
            return false;
        }
        int suma = 0;
        for (int i = 0; i < 10; i++) {
            suma += (v.charAt(i) - '0') * PESOS_RUC[i];
        }
        int resto = 11 - (suma % 11);
        int verificador = resto == 10 ? 0 : (resto == 11 ? 1 : resto);
        return verificador == v.charAt(10) - '0';
    }

    /** Quita espacios repetidos y de los extremos. */
    public static String normalizar(String v) {
        return v == null ? "" : v.trim().replaceAll("\\s+", " ");
    }

    /** Deja solo los dígitos (para DNI, RUC, teléfonos escritos con espacios o guiones). */
    public static String soloDigitos(String v) {
        return v == null ? "" : v.replaceAll("\\D", "");
    }
}
