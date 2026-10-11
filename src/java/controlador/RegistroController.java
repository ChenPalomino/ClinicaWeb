package controlador;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import modelo.dto.RegistroPacienteDTO;
import modelo.servicios.RegistroService;

import java.io.IOException;
import java.util.Map;

/**
 * Controlador (MVC) del registro de pacientes.
 * - Éxito: redirige al login con mensaje de bienvenida.
 * - Error: vuelve al formulario CONSERVANDO lo que el usuario escribió
 *   (menos las contraseñas) y mostrando el error junto a cada campo.
 */
@WebServlet(name = "RegistroController", urlPatterns = {"/RegistroController"})
public class RegistroController extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");

        RegistroPacienteDTO dto = new RegistroPacienteDTO(
                request.getParameter("dni"),
                request.getParameter("nombres"),
                request.getParameter("apellidos"),
                request.getParameter("celular"),
                request.getParameter("correo"),
                request.getParameter("password"),
                request.getParameter("confirmarPassword"));

        Map<String, String> errores;
        try {
            errores = new RegistroService().registrar(dto);
        } catch (RuntimeException e) {
            System.err.println("RegistroController: " + e.getMessage());
            errores = Map.of("general", "El sistema no está disponible en este momento. Inténtalo en unos minutos.");
        }

        if (errores.isEmpty()) {
            response.sendRedirect(request.getContextPath() + "/jsp/login.jsp?registro=exito&usuario=" + dto.getDni());
            return;
        }

        request.setAttribute("errores", errores);
        request.setAttribute("datos", dto);
        request.getRequestDispatcher("/jsp/registro.jsp").forward(request, response);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.sendRedirect(request.getContextPath() + "/jsp/registro.jsp");
    }
}
