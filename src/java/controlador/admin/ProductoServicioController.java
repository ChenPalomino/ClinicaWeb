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
import modelo.dto.ProductoServicioDTO;
import modelo.entidades.Usuario;
import modelo.servicios.ProductoServicioService;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

/**
 * Controlador (MVC) del CRUD del TARIFARIO (servicios y productos) — solo Administrador.
 *   GET  /admin/servicios [?accion=nuevo&tipo= | editar&id=]
 *   POST accion=guardar | desactivar | reactivar
 */
@WebServlet(name = "ProductoServicioController", urlPatterns = {"/admin/servicios"})
public class ProductoServicioController extends HttpServlet {

    private static final String RUTA = "/admin/servicios";
    private static final String VISTA_LISTA = "/WEB-INF/vistas/admin/servicios/listar.jsp";
    private static final String VISTA_FORM = "/WEB-INF/vistas/admin/servicios/formulario.jsp";

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        ProductoServicioService servicio = new ProductoServicioService();
        String accion = request.getParameter("accion");
        request.setAttribute("menuActivo", "servicios");
        try {
            if ("nuevo".equals(accion)) {
                ProductoServicioDTO nuevo = new ProductoServicioDTO();
                nuevo.setTipo(request.getParameter("tipo"));
                nuevo.setCodigo(servicio.siguienteCodigo(nuevo.getTipo()));
                if (nuevo.isProducto()) {
                    nuevo.setStock(0);
                    nuevo.setStockMinimo(0);
                }
                // Unidad sugerida: "Servicio" para servicios, "Unidad" para productos
                String unidad = nuevo.isProducto() ? "Unidad" : "Servicio";
                servicio.unidades().stream().filter(u -> u.getNombre().equals(unidad)).findFirst()
                        .ifPresent(u -> nuevo.setIdUnidadMedida(u.getId()));
                mostrarFormulario(request, response, servicio, nuevo, Map.of());
                return;
            }
            if ("editar".equals(accion)) {
                Optional<ProductoServicioDTO> p = servicio.obtener(ControladorUtil.entero(request.getParameter("id"), 0));
                if (p.isEmpty()) {
                    MensajeFlash.error(request.getSession(), "El servicio o producto que buscas no existe.");
                    response.sendRedirect(request.getContextPath() + RUTA);
                    return;
                }
                mostrarFormulario(request, response, servicio, p.get(), Map.of());
                return;
            }
            FiltroBusquedaDTO filtro = new FiltroBusquedaDTO(request.getParameter("q"), request.getParameter("estado"),
                    ControladorUtil.enteroONulo(request.getParameter("categoria")), request.getParameter("tipo"),
                    ControladorUtil.entero(request.getParameter("pagina"), 1));
            request.setAttribute("filtro", filtro);
            request.setAttribute("pagina", servicio.listar(filtro));
            request.setAttribute("categorias", servicio.categorias());
            request.setAttribute("stockBajo", servicio.contarStockBajo());
        } catch (RuntimeException e) {
            System.err.println("ProductoServicioController: " + e.getMessage());
            request.setAttribute("errorDatos", true);
        }
        request.setAttribute("tituloPagina", "Servicios y productos");
        request.getRequestDispatcher(VISTA_LISTA).forward(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setCharacterEncoding("UTF-8");
        HttpSession session = request.getSession();
        Usuario sesion = (Usuario) session.getAttribute("usuarioLogueado");
        ProductoServicioService servicio = new ProductoServicioService();
        String accion = request.getParameter("accion");
        int id = ControladorUtil.entero(request.getParameter("id"), 0);
        String volver = request.getContextPath() + RUTA + ControladorUtil.regresoListado(request);
        try {
            switch (accion == null ? "" : accion) {
                case "guardar": {
                    ProductoServicioDTO p = leerFormulario(request);
                    boolean nuevo = p.isNuevo();
                    Map<String, String> errores = servicio.guardar(p, sesion);
                    if (!errores.isEmpty()) {
                        request.setAttribute("menuActivo", "servicios");
                        mostrarFormulario(request, response, servicio, p, errores);
                        return;
                    }
                    MensajeFlash.exito(session, "«" + p.getNombre() + "» (" + p.getCodigo() + ") "
                            + (nuevo ? "registrado en el tarifario." : "actualizado."));
                    response.sendRedirect(request.getContextPath() + RUTA + "?tipo=" + p.getTipo());
                    return;
                }
                case "desactivar":
                    ControladorUtil.resultado(session, servicio.desactivar(id),
                            "Desactivado: ya no se podrá cobrar en caja. Puedes reactivarlo cuando quieras.");
                    break;
                case "reactivar":
                    ControladorUtil.resultado(session, servicio.reactivar(id), "Reactivado: ya se puede cobrar en caja.");
                    break;
                default:
                    MensajeFlash.error(session, "Acción no reconocida.");
            }
        } catch (RuntimeException e) {
            System.err.println("ProductoServicioController: " + e.getMessage());
            MensajeFlash.error(session, "No se pudo completar la operación. Inténtalo de nuevo en unos minutos.");
        }
        response.sendRedirect(volver);
    }

    private void mostrarFormulario(HttpServletRequest request, HttpServletResponse response, ProductoServicioService servicio,
                                   ProductoServicioDTO p, Map<String, String> errores) throws ServletException, IOException {
        request.setAttribute("itemForm", p);
        request.setAttribute("errores", errores);
        request.setAttribute("categorias", servicio.categorias());
        request.setAttribute("especialidades", servicio.especialidades());
        request.setAttribute("marcas", servicio.marcas());
        request.setAttribute("unidades", servicio.unidades());
        request.setAttribute("proveedores", servicio.proveedores());
        String que = p.isProducto() ? "producto" : "servicio";
        request.setAttribute("tituloPagina", p.isNuevo() ? "Nuevo " + que : "Editar " + que);
        request.getRequestDispatcher(VISTA_FORM).forward(request, response);
    }

    private static ProductoServicioDTO leerFormulario(HttpServletRequest request) {
        ProductoServicioDTO p = new ProductoServicioDTO();
        p.setIdProductoServicio(ControladorUtil.entero(request.getParameter("id"), 0));
        p.setTipo(request.getParameter("tipo"));
        p.setCodigo(request.getParameter("codigo"));
        p.setNombre(request.getParameter("nombre"));
        p.setDescripcion(request.getParameter("descripcion"));
        p.setIdCategoria(ControladorUtil.entero(request.getParameter("idCategoria"), 0));
        p.setIdEspecialidad(ControladorUtil.enteroONulo(request.getParameter("idEspecialidad")));
        p.setIdMarca(ControladorUtil.enteroONulo(request.getParameter("idMarca")));
        p.setIdUnidadMedida(ControladorUtil.entero(request.getParameter("idUnidadMedida"), 0));
        p.setIdProveedor(ControladorUtil.enteroONulo(request.getParameter("idProveedor")));
        p.setPrecio(ControladorUtil.decimal(request.getParameter("precio")));
        p.setAfectoIgv(request.getParameter("afectoIgv") != null);
        p.setStock(ControladorUtil.enteroONulo(request.getParameter("stock")));
        p.setStockMinimo(ControladorUtil.enteroONulo(request.getParameter("stockMinimo")));
        p.setEstado(request.getParameter("estado"));
        return p;
    }
}
