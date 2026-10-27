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
                new OpcionMenu("MIS CITAS", "Reservar cita", "fas fa-calendar-plus", "/paciente/citas?accion=reservar", "reservar", true),
                new OpcionMenu("MIS CITAS", "Mis citas", "fas fa-calendar-check", "/paciente/citas", "citas", true),
                new OpcionMenu("MIS CITAS", "Precios", "fas fa-tags", "/paciente/precios", "precios", true),
                new OpcionMenu("MIS PAGOS", "Pagos y comprobantes", "fas fa-receipt", "/paciente/pagos", "pagos", true),
                new OpcionMenu("MI CUENTA", "Mi perfil y ajustes", "fas fa-user-circle", "/perfil", "perfil", true)
        );
    }
}
