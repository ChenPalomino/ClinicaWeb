package menu;

import java.util.List;

/** PATRÓN: Strategy  ->  ESTRATEGIA CONCRETA para el rol Paciente. */
public class MenuPacienteStrategy implements MenuStrategy {

    @Override
    public String getNombrePanel() {
        return "Portal del paciente";
    }

    @Override
    public String getVistaInicio() {
        return "/WEB-INF/vistas/paciente/inicio.jsp";
    }

    @Override
    public List<OpcionMenu> getOpciones() {
        return List.of(
                new OpcionMenu("PRINCIPAL", "Inicio", "fas fa-home", "/panel", "inicio", true),
                OpcionMenu.proximamente("MIS CITAS", "Reservar cita", "fas fa-calendar-plus"),
                OpcionMenu.proximamente("MIS CITAS", "Mis citas", "fas fa-calendar-check"),
                new OpcionMenu("MI CUENTA", "Mi perfil y ajustes", "fas fa-user-circle", "/perfil", "perfil", true)
        );
    }
}
