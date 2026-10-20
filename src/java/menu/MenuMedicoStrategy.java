package menu;

import java.util.List;

/** PATRÓN: Strategy  ->  ESTRATEGIA CONCRETA para el rol Médico. */
public class MenuMedicoStrategy implements MenuStrategy {

    @Override
    public String getNombrePanel() {
        return "Atención médica";
    }

    @Override
    public String getVistaInicio() {
        return "/WEB-INF/vistas/medico/inicio.jsp";
    }

    @Override
    public List<OpcionMenu> getOpciones() {
        return List.of(
                new OpcionMenu("PRINCIPAL", "Inicio", "fas fa-home", "/panel", "inicio", true),
                new OpcionMenu("ATENCIÓN", "Mi agenda", "fas fa-calendar-day", "/medico/agenda", "agenda", true),
                new OpcionMenu("MI CUENTA", "Mi perfil y ajustes", "fas fa-user-circle", "/perfil", "perfil", true)
        );
    }
}
