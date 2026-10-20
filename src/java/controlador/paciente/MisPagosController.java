package controlador.paciente;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import modelo.entidades.Usuario;
import modelo.servicios.CobranzaService;

import java.io.IOException;

/** Portal del paciente: sus pagos y comprobantes (solo los suyos). Ruta /paciente/* (rol Paciente). */
@WebServlet(name = "MisPagosController", urlPatterns = {"/paciente/pagos"})
public class MisPagosController extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Usuario usuario = (Usuario) request.getSession().getAttribute("usuarioLogueado");
        try {
            request.setAttribute("cuentas", new CobranzaService().pagosDelPaciente(usuario.getIdUsuario()));
        } catch (RuntimeException e) {
            System.err.println("MisPagosController: " + e.getMessage());
            request.setAttribute("errorDatos", true);
        }
        request.setAttribute("menuActivo", "pagos");
        request.setAttribute("tituloPagina", "Mis pagos");
        request.getRequestDispatcher("/WEB-INF/vistas/paciente/pagos.jsp").forward(request, response);
    }
}
