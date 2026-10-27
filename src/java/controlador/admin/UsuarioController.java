package controlador.admin;

import controlador.MensajeFlash;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import modelo.dto.FiltroUsuarioDTO;
import modelo.dto.UsuarioDTO;
import modelo.entidades.Usuario;
import modelo.servicios.UsuarioService;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

/**
 * Controlador (MVC) del CRUD de usuarios.
 * Paquete controlador.admin: todo lo que solo usa el rol Administrador
 * (lo garantiza AutorizacionFilter en la ruta /admin/*).
 *
 *   GET  /admin/usuarios                    -> listado con búsqueda y filtros
 *   GET  /admin/usuarios?accion=nuevo       -> formulario vacío
 *   GET  /admin/usuarios?accion=editar&id=  -> formulario con datos
 *   POST accion=guardar | desactivar | reactivar | desbloquear | bloquear
 *
 * Después de cada POST se redirige (Post/Redirect/Get) para que al
 * recargar la página no se repita la operación.
 */
@WebServlet(name = "UsuarioController", urlPatterns = {"/admin/usuarios"})
public class UsuarioController extends HttpServlet {

    private static final String VISTA_LISTA = "/WEB-INF/vistas/admin/usuarios/listar.jsp";
    private static final String VISTA_FORM = "/WEB-INF/vistas/admin/usuarios/formulario.jsp";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        UsuarioService servicio = new UsuarioService();
        String accion = request.getParameter("accion");
        request.setAttribute("menuActivo", "usuarios");

        try {
            if ("nuevo".equals(accion)) {
                mostrarFormulario(request, response, servicio, new UsuarioDTO(), Map.of());
                return;
            }
            if ("editar".equals(accion)) {
                Optional<UsuarioDTO> usuario = servicio.obtener(entero(request.getParameter("id"), 0));
                if (usuario.isEmpty()) {
                    MensajeFlash.error(request.getSession(), "El usuario que buscas no existe.");
                    response.sendRedirect(request.getContextPath() + "/admin/usuarios");
                    return;
                }
                mostrarFormulario(request, response, servicio, usuario.get(), Map.of());
                return;
            }

            request.setAttribute("roles", servicio.listarRoles());
            request.setAttribute("tituloPagina", "Usuarios");
            if (!controlador.ControladorUtil.busquedaSolicitada(request)) {
                request.getRequestDispatcher(VISTA_LISTA).forward(request, response);
                return;
            }
            String rol = request.getParameter("rol");
            boolean soloPersonal = "personal".equals(rol);
            FiltroUsuarioDTO filtro = new FiltroUsuarioDTO(
                    request.getParameter("q"),
                    (rol == null || rol.isBlank() || soloPersonal) ? null : entero(rol, 0),
                    request.getParameter("estado"),
                    entero(request.getParameter("pagina"), 1),
                    soloPersonal).conRango(controlador.ControladorUtil.rango(request, "rol", "estado"));
            request.setAttribute("filtro", filtro);
            request.setAttribute("pagina", servicio.listar(filtro));
            request.getRequestDispatcher(VISTA_LISTA).forward(request, response);

        } catch (RuntimeException e) {
            System.err.println("UsuarioController: " + e.getMessage());
            request.setAttribute("tituloPagina", "Usuarios");
            request.setAttribute("errorDatos", true);
            request.getRequestDispatcher(VISTA_LISTA).forward(request, response);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");
        HttpSession session = request.getSession();
        Usuario admin = (Usuario) session.getAttribute("usuarioLogueado");
        UsuarioService servicio = new UsuarioService();
        String accion = request.getParameter("accion");
        int id = entero(request.getParameter("id"), 0);
        String volver = request.getContextPath() + "/admin/usuarios" + regresoListado(request);

        try {
            switch (accion == null ? "" : accion) {
                case "guardar": {
                    UsuarioDTO u = leerFormulario(request);
                    Map<String, String> errores = servicio.guardar(u,
                            request.getParameter("password"), request.getParameter("confirmarPassword"), admin);
                    if (!errores.isEmpty()) {
                        request.setAttribute("menuActivo", "usuarios");
                        mostrarFormulario(request, response, servicio, u, errores);
                        return;
                    }
                    MensajeFlash.exito(session, id == 0
                            ? "Usuario «" + u.getNombreUsuario() + "» creado correctamente."
                            : "Cambios de «" + u.getNombreUsuario() + "» guardados.");
                    response.sendRedirect(request.getContextPath() + "/admin/usuarios?q="
                            + java.net.URLEncoder.encode(u.getNombreUsuario(), java.nio.charset.StandardCharsets.UTF_8));
                    return;
                }
                case "desactivar":
                    resultado(session, servicio.desactivar(id, admin), "Usuario desactivado. Ya no podrá iniciar sesión.");
                    break;
                case "reactivar":
                    resultado(session, servicio.reactivar(id, admin), "Usuario reactivado. Ya puede iniciar sesión.");
                    break;
                case "desbloquear":
                    resultado(session, servicio.desbloquear(id, admin), "Cuenta desbloqueada. Sus intentos fallidos volvieron a cero.");
                    break;
                case "bloquear":
                    resultado(session, servicio.bloquear(id, admin), "Cuenta bloqueada. Solo un administrador podrá desbloquearla.");
                    break;
                default:
                    MensajeFlash.error(session, "Acción no reconocida.");
            }
        } catch (RuntimeException e) {
            System.err.println("UsuarioController: " + e.getMessage());
            MensajeFlash.error(session, "No se pudo completar la operación. Inténtalo de nuevo en unos minutos.");
        }
        response.sendRedirect(volver);
    }

    // ------------------------------------------------------------------

    private void mostrarFormulario(HttpServletRequest request, HttpServletResponse response, UsuarioService servicio,
                                   UsuarioDTO usuario, Map<String, String> errores) throws ServletException, IOException {
        request.setAttribute("usuarioForm", usuario);
        request.setAttribute("errores", errores);
        request.setAttribute("roles", servicio.listarRoles());
        request.setAttribute("tituloPagina", usuario.isNuevo() ? "Nuevo usuario" : "Editar usuario");
        request.getRequestDispatcher(VISTA_FORM).forward(request, response);
    }

    private static UsuarioDTO leerFormulario(HttpServletRequest request) {
        UsuarioDTO u = new UsuarioDTO();
        u.setIdUsuario(entero(request.getParameter("id"), 0));
        u.setNombres(request.getParameter("nombres"));
        u.setApellidos(request.getParameter("apellidos"));
        u.setDni(request.getParameter("dni"));
        u.setCorreo(request.getParameter("correo"));
        u.setCelular(request.getParameter("celular"));
        u.setNombreUsuario(request.getParameter("nombreUsuario"));
        u.setIdRol(entero(request.getParameter("idRol"), 0));
        String estado = request.getParameter("estado");
        u.setEstado(estado == null ? "Activo" : estado);
        return u;
    }

    private static void resultado(HttpSession session, String error, String exito) {
        if (error == null) {
            MensajeFlash.exito(session, exito);
        } else {
            MensajeFlash.error(session, error);
        }
    }

    /** Conserva búsqueda, filtros y página al volver al listado después de una acción. */
    private static String regresoListado(HttpServletRequest request) {
        String q = request.getParameter("regreso");
        return (q == null || q.isBlank() || !q.matches("[A-Za-z0-9=&%+._-]*")) ? "" : "?" + q;
    }

    private static int entero(String valor, int porDefecto) {
        try {
            return Integer.parseInt(valor);
        } catch (NumberFormatException e) {
            return porDefecto;
        }
    }
}
