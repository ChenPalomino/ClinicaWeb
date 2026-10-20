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
                new OpcionMenu("ADMISIÓN", "Citas", "fas fa-calendar-check", "/recepcion/citas", "citas", true),
                new OpcionMenu("CAJA", "Cobros", "fas fa-cash-register", "/caja/cuentas", "cobros", true),
                new OpcionMenu("CAJA", "Cierre de caja", "fas fa-lock", "/caja/cierre", "cierre", true),
                new OpcionMenu("MI CUENTA", "Mi perfil y ajustes", "fas fa-user-circle", "/perfil", "perfil", true)
        );
    }
}
