<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
    String motivo = request.getParameter("motivo");   // null = 3 intentos | seguridad | reporte
    long minutos = 24 * 60;
    try { minutos = Math.max(1, Long.parseLong(request.getParameter("min"))); } catch (Exception ignorado) { }
    long horas = minutos / 60;
    long resto = minutos % 60;
    String tiempo = (horas > 0 ? horas + " h " : "") + (resto > 0 || horas == 0 ? resto + " min" : "");
%>
<!DOCTYPE html>
<html lang="es">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Cuenta bloqueada - Clínica Modelo San Martín</title>
    <script src="${pageContext.request.contextPath}/js/pestana.js?v=<%= configuracion.VersionAplicacion.RECURSOS %>"></script>
    <link rel="stylesheet" href="${pageContext.request.contextPath}/css/estilos.css?v=<%= configuracion.VersionAplicacion.RECURSOS %>">
</head>
<body>
<div class="main-wrapper" style="width: 520px;">
    <div class="form-panel" style="align-items: center; text-align: center;">
        <div class="bloqueo-icono" aria-hidden="true">&#128274;</div>

    <% if ("reporte".equals(motivo)) { %>
        <h2>Bloqueamos tu cuenta</h2>
        <p class="subtitulo">Gracias por avisarnos. Nadie podrá ingresar a tu cuenta, ni siquiera con la contraseña nueva.</p>
        <div class="alert-info" style="text-align: left;">
            Para recuperarla, acércate a recepción con tu DNI. Te recomendamos también cambiar la
            contraseña de tu correo electrónico.
        </div>
        <a class="boton" href="${pageContext.request.contextPath}/jsp/login.jsp">Ir al inicio</a>

    <% } else if ("seguridad".equals(motivo)) { %>
        <h2>Cuenta bloqueada por seguridad</h2>
        <p class="subtitulo">Esta cuenta fue bloqueada para protegerla y no se puede desbloquear con un código.</p>
        <div class="alert-info" style="text-align: left;">
            Acércate a recepción con tu DNI o comunícate con el administrador del sistema.
        </div>
        <a class="boton" href="${pageContext.request.contextPath}/jsp/login.jsp">Volver al inicio</a>

    <% } else { %>
        <h2>Cuenta bloqueada</h2>
        <p class="subtitulo">Escribiste una contraseña incorrecta 3 veces. Por tu seguridad bloqueamos la cuenta y cerramos la sesión.</p>
        <p style="margin: 6px 0 0; font-size: 13px;">Se desbloqueará sola en</p>
        <p class="bloqueo-tiempo"><%= tiempo %></p>

        <a class="boton" href="${pageContext.request.contextPath}/jsp/recuperar.jsp">Desbloquear ahora con un código</a>
        <a class="boton boton-secundario" href="${pageContext.request.contextPath}/jsp/login.jsp">Volver al inicio</a>
        <p class="ayuda" style="margin-top: 14px;">
            Enviaremos el código al correo de tu cuenta. ¿Sin acceso al correo? Acércate a recepción con tu DNI.
        </p>
    <% } %>
    </div>
</div>
</body>
</html>
