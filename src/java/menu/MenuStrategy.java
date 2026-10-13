package menu;

import java.util.List;

/**
 * PATRÓN: Strategy (de comportamiento)  ->  INTERFAZ DE LA ESTRATEGIA
 *
 * Cada rol "arma" su menú de forma distinta. En lugar de llenar el JSP de
 * if (rol == 1) ... else if (rol == 2) ..., cada rol tiene su propia clase
 * estrategia con sus opciones. La plantilla del dashboard solo pide
 * "dame las opciones" y las dibuja, sin saber de qué rol se trata.
 *
 * Agregar un rol nuevo = crear una clase nueva, sin tocar las existentes.
 */
public interface MenuStrategy {

    /** Nombre del panel que se muestra arriba del menú (ej. "Panel de administración"). */
    String getNombrePanel();

    /**
     * Página de inicio propia del rol. Cada rol tiene su carpeta de vistas
     * (/WEB-INF/vistas/admin, /recepcion, /medico, /paciente): agregar un rol
     * nuevo es crear su estrategia y su carpeta, sin tocar las demás.
     */
    String getVistaInicio();

    /** Opciones del menú lateral, en orden, agrupadas por sección. */
    List<OpcionMenu> getOpciones();
}
