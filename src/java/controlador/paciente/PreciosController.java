package controlador.paciente;

import controlador.ControladorUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import modelo.dto.CoberturaDTO;
import modelo.dto.FiltroBusquedaDTO;
import modelo.dto.ProductoServicioDTO;
import modelo.entidades.Usuario;
import modelo.servicios.CitaService;
import modelo.servicios.CobranzaService;
import modelo.servicios.ProductoServicioService;

import java.io.IOException;
import java.util.stream.Collectors;

@WebServlet(name = "PreciosController", urlPatterns = {"/paciente/precios"})
public class PreciosController extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Usuario usuario = (Usuario) request.getSession().getAttribute("usuarioLogueado");
        try {
            ProductoServicioService tarifario = new ProductoServicioService();
            FiltroBusquedaDTO filtro = new FiltroBusquedaDTO(request.getParameter("q"), "ACTIVO",
                    ControladorUtil.enteroONulo(request.getParameter("categoria")), ProductoServicioDTO.SERVICIO,
                    ControladorUtil.entero(request.getParameter("pagina"), 1));
            request.setAttribute("filtro", filtro);
            request.setAttribute("pagina", tarifario.listar(filtro));
            request.setAttribute("categorias", tarifario.categorias().stream()
                    .filter(c -> ProductoServicioDTO.SERVICIO.equals(c.getExtra())).collect(Collectors.toList()));
            CoberturaDTO cobertura = new CitaService().pacienteDeUsuario(usuario.getIdUsuario())
                    .flatMap(id -> new CobranzaService().coberturaDelPaciente(id)).orElse(null);
            request.setAttribute("cobertura", cobertura);
        } catch (RuntimeException e) {
            System.err.println("PreciosController: " + e.getMessage());
            request.setAttribute("errorDatos", true);
        }
        request.setAttribute("menuActivo", "precios");
        request.setAttribute("tituloPagina", "Precios de nuestros servicios");
        request.getRequestDispatcher("/WEB-INF/vistas/paciente/precios.jsp").forward(request, response);
    }
}
