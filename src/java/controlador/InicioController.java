package controlador;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import modelo.entidades.Rol;
import menu.MenuFactory;
import modelo.entidades.Usuario;
import modelo.servicios.UsuarioService;

import java.io.IOException;

/**
 * Controlador (MVC) de la página de inicio del dashboard (/panel).
 * Cada rol ve SU propio inicio: la estrategia del rol (MenuFactory -> MenuStrategy)
 * indica qué vista usar, así este controlador no necesita un if por cada rol.
 * Solo al Administrador se le cargan los indicadores de usuarios.
 */
@WebServlet(name = "InicioController", urlPatterns = {"/panel"})
public class InicioController extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        Usuario usuario = (Usuario) request.getSession().getAttribute("usuarioLogueado");
        if (usuario.getRolId() == Rol.ADMINISTRADOR) {
            try {
                request.setAttribute("estadisticas", new UsuarioService().estadisticas());
            } catch (RuntimeException e) {
                System.err.println("InicioController: " + e.getMessage());
                request.setAttribute("errorDatos", true);
            }
        }
        request.setAttribute("tituloPagina", "Inicio");
        request.setAttribute("menuActivo", "inicio");
        request.getRequestDispatcher(MenuFactory.crear(usuario.getRolId()).getVistaInicio()).forward(request, response);
    }
}
