package configuracion;

import modelo.dto.ParametroClinicaDTO;
import modelo.repositorio.ParametroClinicaRepository;
import modelo.repositorio.ParametroClinicaRepositoryImpl;

/**
 * PATRÓN: Singleton (creacional)  ->  configuración de la clínica (RF-22)
 *
 * El IGV, el RUC y la razón social se usan en CADA cuenta y comprobante.
 * En vez de leerlos de la base de datos cada vez, una única instancia los
 * guarda en memoria. Cuando el administrador los cambia, se llama a
 * recargar() y todos los cobros siguientes usan los valores nuevos.
 */
public final class ConfiguracionClinicaSingleton {

    private static volatile ConfiguracionClinicaSingleton instancia;

    private final ParametroClinicaRepository repositorio;
    private volatile ParametroClinicaDTO parametros;

    private ConfiguracionClinicaSingleton(ParametroClinicaRepository repositorio) {
        this.repositorio = repositorio;
    }

    public static ConfiguracionClinicaSingleton getInstancia() {
        if (instancia == null) {
            synchronized (ConfiguracionClinicaSingleton.class) {
                if (instancia == null) {
                    instancia = new ConfiguracionClinicaSingleton(new ParametroClinicaRepositoryImpl());
                }
            }
        }
        return instancia;
    }

    /** Parámetros vigentes (se leen de la BD solo la primera vez). */
    public ParametroClinicaDTO getParametros() {
        ParametroClinicaDTO actual = parametros;
        if (actual == null) {
            synchronized (this) {
                if (parametros == null) {
                    parametros = repositorio.obtener();
                }
                actual = parametros;
            }
        }
        return actual;
    }

    /** Vuelve a leer los parámetros (después de que el administrador los edita). */
    public void recargar() {
        parametros = null;
    }
}
