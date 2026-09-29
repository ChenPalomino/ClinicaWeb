package com.clinica.controlador;

import com.clinica.dao.UsuarioDAO;
import com.clinica.modelo.Usuario;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;

@WebServlet(name = "LoginServlet", urlPatterns = {"/LoginServlet"})
public class LoginServlet extends HttpServlet {

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");

        String dni = request.getParameter("dni");
        String password = request.getParameter("password");

        UsuarioDAO dao = new UsuarioDAO();
        Usuario usuario = dao.buscarPorDni(dni);

        // MODIFICACIÓN TEMPORAL DE ACCESO DIRECTO PARA TU DESPLIEGUE:
        // Si el usuario existe en la base de datos con ese DNI, lo dejamos entrar automáticamente
        if (usuario != null) {
            HttpSession session = request.getSession();
            session.setAttribute("usuarioLogueado", usuario);
            response.sendRedirect("menuPrincipal.jsp");
        } else {
            response.sendRedirect("login.jsp?error=1");
        }
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        String accion = request.getParameter("accion");
        if ("salir".equals(accion)) {
            HttpSession session = request.getSession(false);
            if (session != null) {
                session.invalidate();
            }
            response.sendRedirect("login.jsp");
        } else {
            response.sendRedirect("login.jsp");
        }
    }
}
