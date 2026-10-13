package menu;

import java.util.List;

/** PATRÓN: Strategy  ->  ESTRATEGIA CONCRETA para el rol Administrador. */
public class MenuAdministradorStrategy implements MenuStrategy {

    @Override
    public String getNombrePanel() {
        return "Panel de administración";
    }

    @Override
    public String getVistaInicio() {
        return "/WEB-INF/vistas/admin/inicio.jsp";
    }

    @Override
    public List<OpcionMenu> getOpciones() {
        return List.of(
                new OpcionMenu("PRINCIPAL", "Inicio", "fas fa-home", "/panel", "inicio", true),
                new OpcionMenu("ADMINISTRACIÓN", "Usuarios", "fas fa-users-cog", "/admin/usuarios", "usuarios", true),
                OpcionMenu.proximamente("ADMINISTRACIÓN", "Auditoría", "fas fa-clipboard-list"),
                OpcionMenu.proximamente("ADMINISTRACIÓN", "Respaldos", "fas fa-database"),
                new OpcionMenu("CLÍNICA", "Pacientes", "fas fa-user-injured", "/recepcion/pacientes", "pacientes", true),
                new OpcionMenu("CLÍNICA", "Servicios y productos", "fas fa-notes-medical", "/admin/servicios", "servicios", true),
                new OpcionMenu("CLÍNICA", "Proveedores", "fas fa-truck", "/admin/proveedores", "proveedores", true),
                OpcionMenu.proximamente("CLÍNICA", "Citas", "fas fa-calendar-check"),
                OpcionMenu.proximamente("CLÍNICA", "Caja", "fas fa-cash-register"),
                OpcionMenu.proximamente("ANÁLISIS", "Reportes", "fas fa-chart-bar"),
                new OpcionMenu("MI CUENTA", "Mi perfil y ajustes", "fas fa-user-circle", "/perfil", "perfil", true)
        );
    }
}
