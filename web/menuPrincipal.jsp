<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="com.clinica.modelo.Usuario" %>
<%
    // Validar sesión activa
    Usuario user = (Usuario) session.getAttribute("usuarioLogueado");
    if (user == null) {
        response.sendRedirect("login.jsp");
        return;
    }
%>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <title>Menú Principal - Clínica Modelo San Martín</title>
    <link rel="stylesheet" href="css/estilos.css">
</head>
<body style="flex-direction: column; justify-content: flex-start; padding: 40px;">
    <div style="background: white; padding: 30px; border-radius: 8px; width: 800px; box-shadow: 0 4px 12px rgba(0,0,0,0.1);">
        <h2 style="color: #003366; margin-top: 0;">Bienvenido al Sistema, <%= user.getNombres() %></h2>
        <p><strong>Correo:</strong> <%= user.getCorreo() %> | <strong>Celular:</strong> <%= user.getCelular() %></p>
        <hr style="border: 0; border-top: 1px solid #eee; margin: 20px 0;">
        <h3 style="color: #003366;">Módulos del Sistema (Próximamente)</h3>
        <ul>
            <li>Gestión de Citas Médicas</li>
            <li>Gestión de Clientes y Pacientes</li>
            <li>Facturación y Cobranza en Caja</li>
        </ul>
        <br>
        <a href="LoginServlet?accion=salir" style="background: #cc0000; color: white; padding: 10px 20px; border-radius: 5px; text-decoration: none;">Cerrar Sesión</a>
    </div>
</body>
</html>