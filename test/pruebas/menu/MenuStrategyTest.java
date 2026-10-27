package pruebas.menu;

import menu.MenuFactory;
import menu.MenuStrategy;
import menu.OpcionMenu;
import modelo.entidades.Rol;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

/** RF-20 · Strategy de menú: cada rol ve solo sus secciones. */
public class MenuStrategyTest {

    private static boolean tiene(MenuStrategy m, String url) {
        return m.getOpciones().stream().anyMatch(o -> url.equals(o.getUrl()));
    }

    @Test
    public void cadaRolTieneSuMenuYSuInicio() {
        for (int rol : new int[]{Rol.ADMINISTRADOR, Rol.RECEPCIONISTA, Rol.MEDICO, Rol.PACIENTE}) {
            MenuStrategy m = MenuFactory.crear(rol);
            assertFalse(m.getNombrePanel().isBlank());
            assertTrue(m.getVistaInicio().endsWith("inicio.jsp"));
            assertFalse(m.getOpciones().isEmpty());
            assertEquals("/panel", m.getOpciones().get(0).getUrl());
        }
    }

    @Test
    public void lasSeccionesRespetanElRol() {
        assertTrue(tiene(MenuFactory.crear(Rol.ADMINISTRADOR), "/admin/billeteras"));
        assertTrue(tiene(MenuFactory.crear(Rol.ADMINISTRADOR), "/admin/especialidades"));
        assertTrue(tiene(MenuFactory.crear(Rol.RECEPCIONISTA), "/caja/cuentas"));
        assertFalse(tiene(MenuFactory.crear(Rol.RECEPCIONISTA), "/admin/usuarios"));
        assertTrue(tiene(MenuFactory.crear(Rol.MEDICO), "/medico/agenda"));
        assertTrue(tiene(MenuFactory.crear(Rol.PACIENTE), "/paciente/precios"));
        assertFalse(tiene(MenuFactory.crear(Rol.PACIENTE), "/caja/cuentas"));
    }

    @Test
    public void rolDesconocidoRecibeElMenuMasRestringido() {
        assertFalse(tiene(MenuFactory.crear(99), "/admin/usuarios"));
        assertTrue(tiene(MenuFactory.crear(99), "/paciente/citas"));
    }

    @Test
    public void opcionProximamente() {
        OpcionMenu o = OpcionMenu.proximamente("ANÁLISIS", "Reportes", "fas fa-chart-bar");
        assertFalse(o.isDisponible());
        assertEquals("ANÁLISIS", o.getSeccion());
        assertEquals("Reportes", o.getTitulo());
        assertEquals("fas fa-chart-bar", o.getIcono());
        List<OpcionMenu> admin = MenuFactory.crear(Rol.ADMINISTRADOR).getOpciones();
        assertTrue(admin.stream().allMatch(x -> x.getClave() != null || !x.isDisponible()));
    }
}
