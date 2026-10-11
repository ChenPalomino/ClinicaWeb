<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ page import="modelo.entidades.Usuario, seguridad.EscapeHtml" %>
<%
    // AutenticacionFilter ya garantiza que hay sesión; esta línea solo obtiene al usuario.
    Usuario user = (Usuario) session.getAttribute("usuarioLogueado");
%>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Menú principal - Clínica Modelo San Martín</title>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/estilos.css">
</head>
<body style="align-items: flex-start; background: #f4f6f9;">
<div style="background: white; padding: 30px; border-radius: 10px; width: 800px; max-width: 100%; box-shadow: 0 4px 12px rgba(0,0,0,0.1);">
    <h2>Bienvenido, <%= EscapeHtml.texto(user.getNombreUsuario()) %></h2>
    <p class="subtitulo">Rol: <strong><%= user.getNombreRol() %></strong></p>
    <hr style="border: 0; border-top: 1px solid #e2e8f0; margin: 20px 0;">
    <h3 style="color: #003366;">Módulos del sistema (en desarrollo)</h3>
    <p style="font-size: 14px; color: #64748b;">En la Etapa 4 este menú se reemplaza por el dashboard adaptado a cada rol.</p>
    <a class="boton" style="width: auto; display: inline-block; padding: 10px 20px; background: #b91c1c;"
       href="${pageContext.request.contextPath}/AutenticacionController?accion=salir">Cerrar sesión</a>
</div>
</body>
</html>
