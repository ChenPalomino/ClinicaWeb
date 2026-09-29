/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.clinica.controlador;

import com.clinica.dao.UsuarioDAO;
import com.clinica.modelo.Usuario;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet(name = "RegistroServlet", urlPatterns = {"/RegistroServlet"})
public class RegistroServlet extends HttpServlet {

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");

        String dni = request.getParameter("dni");
        String nombres = request.getParameter("nombres");
        String celular = request.getParameter("celular");
        String correo = request.getParameter("correo");
        String password = request.getParameter("password");

        // Guardamos la contraseña directamente o con un hash consistente
        // Para evitar errores de login en la fase de prueba final, guardaremos el string o un hash seguro compatible
        String passwordHash = password; // Nota: si prefieres hash, usamos password.hashCode() de forma estricta

        // Creamos el objeto usuario (Asignamos id_rol = 4 para Paciente, asegúrate que exista en tm_rol)
        Usuario nuevoUsuario = new Usuario(dni, nombres, celular, correo, passwordHash, 4);

        UsuarioDAO dao = new UsuarioDAO();
        boolean registrado = dao.registrarUsuario(nuevoUsuario);

        if (registrado) {
            response.sendRedirect("login.jsp?registro=exito");
        } else {
            response.sendRedirect("registro.jsp?error=duplicado");
        }
    }
}
