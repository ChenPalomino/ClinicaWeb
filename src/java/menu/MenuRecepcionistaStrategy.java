package menu;

import java.util.List;

/** PATRÓN: Strategy  ->  ESTRATEGIA CONCRETA para el rol Recepcionista. */
public class MenuRecepcionistaStrategy implements MenuStrategy {

    @Override
    public String getNombrePanel() {
        return "Admisión y caja";
    }

    @Override
    public String getVistaInicio() {
        return "/WEB-INF/vistas/recepcion/inicio.jsp";
    }

    @Override
    public List<OpcionMenu> getOpciones() {
        return List.of(
                new OpcionMenu("PRINCIPAL", "Inicio", "fas fa-home", "/panel", "inicio", true),
                new OpcionMenu("ADMISIÓN", "Pacientes", "fas fa-user-injured", "/recepcion/pacientes", "pacientes", true),
                OpcionMenu.proximamente("ADMISIÓN", "Citas", "fas fa-calendar-check"),
                OpcionMenu.proximamente("CAJA", "Cobros", "fas fa-cash-register"),
                new OpcionMenu("MI CUENTA", "Mi perfil y ajustes", "fas fa-user-circle", "/perfil", "perfil", true)
        );
    }
}
