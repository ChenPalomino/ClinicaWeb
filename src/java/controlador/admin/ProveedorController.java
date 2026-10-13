package controlador.admin;

import controlador.ControladorUtil;
import controlador.MensajeFlash;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import modelo.dto.FiltroBusquedaDTO;
import modelo.dto.ProveedorDTO;
import modelo.entidades.Usuario;
import modelo.servicios.ProveedorService;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

/**
 * Controlador (MVC) del CRUD de PROVEEDORES — solo Administrador (/admin/*).
 *   GET  /admin/proveedores [?accion=nuevo | editar&id=]
 *   POST accion=guardar | desactivar | reactivar
 */
@WebServlet(name = "ProveedorController", urlPatterns = {"/admin/proveedores"})
public class ProveedorController extends HttpServlet {

    private static final String RUTA = "/admin/proveedores";
    private static final String VISTA_LISTA = "/WEB-INF/vistas/admin/proveedores/listar.jsp";
    private static final String VISTA_FORM = "/WEB-INF/vistas/admin/proveedores/formulario.jsp";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        ProveedorService servicio = new ProveedorService();
        String accion = request.getParameter("accion");
        request.setAttribute("menuActivo", "proveedores");
        try {
            if ("nuevo".equals(accion)) {
                mostrarFormulario(request, response, new ProveedorDTO(), Map.of());
                return;
            }
            if ("editar".equals(accion)) {
                Optional<ProveedorDTO> p = servicio.obtener(ControladorUtil.entero(request.getParameter("id"), 0));
                if (p.isEmpty()) {
                    MensajeFlash.error(request.getSession(), "El proveedor que buscas no existe.");
                    response.sendRedirect(request.getContextPath() + RUTA);
                    return;
                }
                mostrarFormulario(request, response, p.get(), Map.of());
                return;
            }
            FiltroBusquedaDTO filtro = new FiltroBusquedaDTO(request.getParameter("q"), request.getParameter("estado"),
                    null, null, ControladorUtil.entero(request.getParameter("pagina"), 1));
            request.setAttribute("filtro", filtro);
            request.setAttribute("pagina", servicio.listar(filtro));
        } catch (RuntimeException e) {
            System.err.println("ProveedorController: " + e.getMessage());
            request.setAttribute("errorDatos", true);
        }
        request.setAttribute("tituloPagina", "Proveedores");
        request.getRequestDispatcher(VISTA_LISTA).forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        HttpSession session = request.getSession();
        Usuario sesion = (Usuario) session.getAttribute("usuarioLogueado");
        ProveedorService servicio = new ProveedorService();
        String accion = request.getParameter("accion");
        int id = ControladorUtil.entero(request.getParameter("id"), 0);
        String volver = request.getContextPath() + RUTA + ControladorUtil.regresoListado(request);
        try {
            switch (accion == null ? "" : accion) {
                case "guardar": {
                    ProveedorDTO p = leerFormulario(request);
                    boolean nuevo = p.isNuevo();
                    Map<String, String> errores = servicio.guardar(p, sesion);
                    if (!errores.isEmpty()) {
                        request.setAttribute("menuActivo", "proveedores");
                        mostrarFormulario(request, response, p, errores);
                        return;
                    }
                    MensajeFlash.exito(session, (nuevo ? "Proveedor «" : "Cambios de «") + p.getNombreMostrar()
                            + (nuevo ? "» registrado correctamente." : "» guardados."));
                    response.sendRedirect(request.getContextPath() + RUTA);
                    return;
                }
                case "desactivar":
                    ControladorUtil.resultado(session, servicio.desactivar(id),
                            "Proveedor desactivado. Ya no aparecerá al elegir proveedor; puedes reactivarlo cuando quieras.");
                    break;
                case "reactivar":
                    ControladorUtil.resultado(session, servicio.reactivar(id), "Proveedor reactivado.");
                    break;
                default:
                    MensajeFlash.error(session, "Acción no reconocida.");
            }
        } catch (RuntimeException e) {
            System.err.println("ProveedorController: " + e.getMessage());
            MensajeFlash.error(session, "No se pudo completar la operación. Inténtalo de nuevo en unos minutos.");
        }
        response.sendRedirect(volver);
    }

    private void mostrarFormulario(HttpServletRequest request, HttpServletResponse response, ProveedorDTO p,
                                   Map<String, String> errores) throws ServletException, IOException {
        request.setAttribute("proveedorForm", p);
        request.setAttribute("errores", errores);
        request.setAttribute("rubros", ProveedorService.RUBROS);
        request.setAttribute("tituloPagina", p.isNuevo() ? "Nuevo proveedor" : "Editar proveedor");
        request.getRequestDispatcher(VISTA_FORM).forward(request, response);
    }

    private static ProveedorDTO leerFormulario(HttpServletRequest request) {
        ProveedorDTO p = new ProveedorDTO();
        p.setIdProveedor(ControladorUtil.entero(request.getParameter("id"), 0));
        p.setRuc(request.getParameter("ruc"));
        p.setRazonSocial(request.getParameter("razonSocial"));
        p.setNombreComercial(request.getParameter("nombreComercial"));
        p.setRubro(request.getParameter("rubro"));
        p.setContactoNombre(request.getParameter("contactoNombre"));
        p.setTelefono(request.getParameter("telefono"));
        p.setCorreo(request.getParameter("correo"));
        p.setDireccion(request.getParameter("direccion"));
        p.setEstado(request.getParameter("estado"));
        return p;
    }
}
