package configuracion;

/**
 * Versión visible del sistema y "huella" de los archivos estáticos (CSS/JS).
 *
 * Los navegadores guardan en caché panel.js y panel.css. Si después de una
 * actualización el navegador sigue usando el archivo antiguo, la pantalla se
 * comporta como la versión anterior. Por eso cada enlace a un CSS/JS lleva
 * "?v=" + RECURSOS: el valor cambia en cada despliegue y obliga al navegador
 * a descargar el archivo nuevo.
 */
public final class VersionAplicacion {

    /** Versión que se muestra en el pie de página (ayuda a saber qué versión está corriendo). */
    public static final String VERSION = "Etapa 8.1 · 2026.10.27";

    /** Cambia cada vez que se inicia/despliega la aplicación. */
    public static final String RECURSOS = Long.toString(System.currentTimeMillis(), 36);

    private VersionAplicacion() {
    }
}
