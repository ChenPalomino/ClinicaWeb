package menu;

import modelo.entidades.Rol;

/**
 * PATRÓN: Factory (creacional)  +  PATRÓN: Strategy
 *
 * Recibe el rol del usuario que inició sesión y entrega la ESTRATEGIA de
 * menú que le corresponde. Es el único lugar del sistema que relaciona
 * "rol" con "menú"; la plantilla del dashboard no tiene ningún if por rol.
 */
public final class MenuFactory {

    private MenuFactory() {
    }

    public static MenuStrategy crear(int idRol) {
        switch (idRol) {
            case Rol.ADMINISTRADOR: return new MenuAdministradorStrategy();
            case Rol.RECEPCIONISTA: return new MenuRecepcionistaStrategy();
            case Rol.MEDICO:        return new MenuMedicoStrategy();
            default:                return new MenuPacienteStrategy();   // rol más restringido por defecto
        }
    }
}
