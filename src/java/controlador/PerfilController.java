package controlador;

import configuracion.UrlAplicacion;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import modelo.dto.PerfilDTO;
import modelo.entidades.Usuario;
import modelo.servicios.PerfilService;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

/**
 * Controlador (MVC) de "Mi perfil y ajustes" (/perfil). Disponible para TODOS
 * los roles; cada usuario solo puede ver y cambiar su propia cuenta.
 *
 *   GET  /perfil?tab=datos|foto|seguridad|apariencia
 *   POST accion=datos | foto | quitarFoto | password
 *   POST accion=tema   (lo llama el botón de modo oscuro sin recargar la página)
 */
@WebServlet(name = "PerfilController", urlPatterns = {"/perfil"})
public class PerfilController extends HttpServlet {

    public static final String SESION_PERFIL = "perfil";
    private static final Set<String> PESTANAS = Set.of("datos", "foto", "seguridad", "apariencia");
    private static final String VISTA = "/WEB-INF/vistas/comun/perfil.jsp";

    /** Carga (o recarga) en la sesión el nombre y foto que muestra el dashboard. */
    public static PerfilDTO cargarEnSesion(HttpSession session, int idUsuario) {
        try {
            PerfilDTO perfil = new PerfilService().obtener(idUsuario).orElse(null);
            if (perfil != null) {
                session.setAttribute(SESION_PERFIL, perfil);
            }
            return perfil;
        } catch (RuntimeException e) {
            System.err.println("PerfilController: no se pudo cargar el perfil -> " + e.getMessage());
            return null;
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        Usuario usuario = (Usuario) request.getSession().getAttribute("usuarioLogueado");
        String tab = request.getParameter("tab");
        mostrar(request, response, cargarEnSesion(request.getSession(), usuario.getIdUsuario()),
                tab != null && PESTANAS.contains(tab) ? tab : "datos", Map.of());
    }

    private static void cambiarTema(HttpServletRequest request, HttpServletResponse response, PerfilService servicio, Usuario usuario) {
        try {
            boolean ok = servicio.cambiarTema(usuario, request.getParameter("tema"));
            if (ok) {
                cargarEnSesion(request.getSession(), usuario.getIdUsuario());
            }
            response.setStatus(ok ? HttpServletResponse.SC_NO_CONTENT : HttpServletResponse.SC_BAD_REQUEST);
        } catch (RuntimeException e) {
            System.err.println("PerfilController (tema): " + e.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    /** @return true si se guardó; false si ya se mostró el formulario con errores. */
    private boolean guardarDatos(HttpServletRequest request, HttpServletResponse response, PerfilService servicio, Usuario usuario)
            throws ServletException, IOException {
        PerfilDTO form = new PerfilDTO();
        form.setNombres(request.getParameter("nombres"));
        form.setApellidos(request.getParameter("apellidos"));
        form.setDni(request.getParameter("dni"));
        form.setCorreo(request.getParameter("correo"));
        form.setCelular(request.getParameter("celular"));
        form.setDescripcion(request.getParameter("descripcion"));
        form.setIdSexo(ControladorUtil.enteroONulo(request.getParameter("idSexo")));
        form.setFechaNacimiento(ControladorUtil.fecha(request.getParameter("fechaNacimiento")));
        Map<String, String> errores = servicio.actualizarDatos(usuario, form);
        if (errores.isEmpty()) {
            return true;
        }
        PerfilDTO actual = cargarEnSesion(request.getSession(), usuario.getIdUsuario());
        form.setNombreUsuario(actual.getNombreUsuario());
        form.setTieneFoto(actual.isTieneFoto());
        form.setFotoVersion(actual.getFotoVersion());
        if (actual.isTieneDatosPersonales()) form.setDni(actual.getDni());
        form.setNumeroHistoria(actual.getNumeroHistoria());
        form.setNombreCobertura(actual.getNombreCobertura());
        mostrar(request, response, form, "datos", errores);
        return false;
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        HttpSession session = request.getSession();
        Usuario usuario = (Usuario) session.getAttribute("usuarioLogueado");
        PerfilService servicio = new PerfilService();
        String accion = request.getParameter("accion");
        String volver = request.getContextPath() + "/perfil";

        // Apariencia: llamada en segundo plano (fetch), responde sin redirigir
        if ("tema".equals(accion)) {
            cambiarTema(request, response, servicio, usuario);
            return;
        }

        try {
            switch (accion == null ? "" : accion) {
                case "datos":
                    if (!guardarDatos(request, response, servicio, usuario)) {
                        return;
                    }
                    MensajeFlash.exito(session, "Tus datos se guardaron correctamente.");
                    volver += "?tab=datos";
                    break;
                case "foto":
                    ControladorUtil.resultado(session, servicio.guardarFoto(usuario, request.getParameter("foto")), "Foto de perfil actualizada.");
                    volver += "?tab=foto";
                    break;
                case "quitarFoto":
                    servicio.eliminarFoto(usuario);
                    MensajeFlash.exito(session, "Quitaste tu foto de perfil. Ahora se mostrarán tus iniciales.");
                    volver += "?tab=foto";
                    break;
                case "password": {
                    Map<String, String> errores = servicio.cambiarContrasena(usuario,
                            request.getParameter("actual"), request.getParameter("nueva"),
                            request.getParameter("confirmar"), UrlAplicacion.base(request));
                    if (!errores.isEmpty()) {
                        mostrar(request, response, cargarEnSesion(session, usuario.getIdUsuario()), "seguridad", errores);
                        return;
                    }
                    MensajeFlash.exito(session, "Contraseña actualizada. Te enviamos un aviso a tu correo.");
                    volver += "?tab=seguridad";
                    break;
                }
                default:
                    MensajeFlash.error(session, "Acción no reconocida.");
            }
        } catch (RuntimeException e) {
            System.err.println("PerfilController: " + e.getMessage());
            MensajeFlash.error(session, "No se pudo guardar. Inténtalo de nuevo en unos minutos.");
        }
        cargarEnSesion(session, usuario.getIdUsuario());
        response.sendRedirect(volver);
    }

    private void mostrar(HttpServletRequest request, HttpServletResponse response, PerfilDTO perfil,
                         String tab, Map<String, String> errores) throws ServletException, IOException {
        request.setAttribute("perfilForm", perfil);
        request.setAttribute("tab", tab);
        request.setAttribute("errores", errores);
        try {
            request.setAttribute("sexos", new PerfilService().sexos());
        } catch (RuntimeException e) {
            request.setAttribute("sexos", java.util.List.of());
        }
        request.setAttribute("tituloPagina", "Mi perfil");
        request.setAttribute("menuActivo", "perfil");
        request.getRequestDispatcher(VISTA).forward(request, response);
    }
}
